/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.typesafe.config.Config;
import org.apache.commons.lang.StringUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;
import org.atmosphere.guice.AtmosphereGuiceServlet;
import org.atmosphere.util.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
//import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.servlet.*;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ClientServerExecutor;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereChannel;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereClientInterceptor;
import org.waveprotocol.box.server.util.NetUtils;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import javax.annotation.Nullable;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * ServerRpcProvider can provide instances of type Service over an incoming
 * network socket and service incoming RPCs to these services and their methods.
 *
 *
 */
public class ServerRpcProvider {
  private static final Log LOG = Log.get(ServerRpcProvider.class);
  /**
   * The buffer size is passed to implementations of {@link WaveWebSocketServlet} as init
   * param. It defines the response buffer size.
   */
  private static final int BUFFER_SIZE = 1024 * 1024;

  private final InetSocketAddress[] httpAddresses;
  private final Executor threadPool;
  private final SessionManager sessionManager;
  private final org.eclipse.jetty.server.SessionManager jettySessionManager;
  private Server httpServer = null;
  private final boolean sslEnabled;
  private final String sslKeystorePath;
  private final String sslKeystorePassword;

  // Mapping from incoming protocol buffer type -> specific handler.
  private final Map<Descriptors.Descriptor, RegisteredServiceMethod> registeredServices =
      Maps.newHashMap();

  // List of webApp source directories ("./war", etc)
  private final String[] resourceBases;

  private final String sessionStoreDir;

  /**
   * Internal, static container class for any specific registered service
   * method.
   */
  static class RegisteredServiceMethod {
    final Service service;
    final MethodDescriptor method;

    RegisteredServiceMethod(Service service, MethodDescriptor method) {
      this.service = service;
      this.method = method;
    }
  }

  static class WebSocketConnection extends Connection {
    private final WebSocketChannel socketChannel;

    WebSocketConnection(ParticipantId loggedInUser, ServerRpcProvider provider) {
      super(loggedInUser, provider);
      socketChannel = new WebSocketChannelImpl(this);
      LOG.info("New websocket connection set up for user " + loggedInUser);
      expectMessages(socketChannel);
    }

    @Override
    protected void sendMessage(int sequenceNo, Message message) {
      socketChannel.sendMessage(sequenceNo, message);
    }

    public WebSocketChannel getWebSocketServerChannel() {
      return socketChannel;
    }
  }

  static class AtmosphereConnection extends Connection {

    private final AtmosphereChannel atmosphereChannel;

    public AtmosphereConnection(ParticipantId loggedInUser, ServerRpcProvider provider) {
      super(loggedInUser, provider);

      atmosphereChannel = new AtmosphereChannel(this);
      expectMessages(atmosphereChannel);

    }

    @Override
    protected void sendMessage(int sequenceNo, Message message) {
      atmosphereChannel.sendMessage(sequenceNo, message);
    }

    public AtmosphereChannel getAtmosphereChannel() {
      return atmosphereChannel;
    }


  }



  static abstract class Connection implements ProtoCallback {
    private final Map<Integer, ServerRpcController> activeRpcs =
        new ConcurrentHashMap<>();

    // The logged in user.
    // Note: Due to this bug:
    // http://code.google.com/p/wave-protocol/issues/detail?id=119,
    // the field may be null on first connect and then set later using an RPC.
    private ParticipantId loggedInUser;

    private final ServerRpcProvider provider;

    /**
     * @param loggedInUser The currently logged in user, or null if no user is
     *        logged in.
     * @param provider the provider
     */
    public Connection(ParticipantId loggedInUser, ServerRpcProvider provider) {
      this.loggedInUser = loggedInUser;
      this.provider = provider;
    }

    protected void expectMessages(MessageExpectingChannel channel) {
      synchronized (provider.registeredServices) {
        for (RegisteredServiceMethod serviceMethod : provider.registeredServices.values()) {
          channel.expectMessage(serviceMethod.service.getRequestPrototype(serviceMethod.method));
          LOG.fine("Expecting: " + serviceMethod.method.getFullName());
        }
      }
      channel.expectMessage(Rpc.CancelRpc.getDefaultInstance());
    }

    protected abstract void sendMessage(int sequenceNo, Message message);

    private ParticipantId authenticate(String token) {
      HttpSession session = provider.sessionManager.getSessionFromToken(token);
      return provider.sessionManager.getLoggedInUser(session);
    }

    @Override
    public void message(final int sequenceNo, Message message) {
      final String messageName = "/" + message.getClass().getSimpleName();
      final Timer profilingTimer = Timing.startRequest(messageName);
      if (message instanceof Rpc.CancelRpc) {
        final ServerRpcController controller = activeRpcs.get(sequenceNo);
        if (controller == null) {
          throw new IllegalStateException("Trying to cancel an RPC that is not active!");
        } else {
          LOG.info("Cancelling open RPC " + sequenceNo);
          controller.cancel();
        }
      } else if (message instanceof ProtocolAuthenticate) {
        // Workaround for bug: http://codereview.waveprotocol.org/224001/

        // When we get this message, either the connection will not be logged in
        // (loggedInUser == null) or the connection will have been authenticated
        // via cookies
        // (in which case loggedInUser must match the authenticated user, and
        // this message has no
        // effect).

        ProtocolAuthenticate authMessage = (ProtocolAuthenticate) message;
        ParticipantId authenticatedAs = authenticate(authMessage.getToken());

        Preconditions.checkArgument(authenticatedAs != null, "Auth token invalid");
        Preconditions.checkState(loggedInUser == null || loggedInUser.equals(authenticatedAs),
            "Session already authenticated as a different user");

        loggedInUser = authenticatedAs;
        LOG.info("Session authenticated as " + loggedInUser);
        sendMessage(sequenceNo, ProtocolAuthenticationResult.getDefaultInstance());
      } else if (provider.registeredServices.containsKey(message.getDescriptorForType())) {
        if (activeRpcs.containsKey(sequenceNo)) {
          throw new IllegalStateException(
              "Can't invoke a new RPC with a sequence number already in use.");
        } else {
          final RegisteredServiceMethod serviceMethod =
              provider.registeredServices.get(message.getDescriptorForType());

          // Create the internal ServerRpcController used to invoke the call.
          final ServerRpcController controller =
              new ServerRpcControllerImpl(message, serviceMethod.service, serviceMethod.method,
                  loggedInUser, new RpcCallback<Message>() {
                    @Override
                    synchronized public void run(Message message) {
                      if (message instanceof Rpc.RpcFinished
                          || !serviceMethod.method.getOptions().getExtension(Rpc.isStreamingRpc)) {
                        // This RPC is over - remove it from the map.
                        boolean failed = message instanceof Rpc.RpcFinished && ((Rpc.RpcFinished) message).getFailed();
                        LOG.fine("RPC " + sequenceNo + " is now finished, failed = " + failed);
                        if (failed) {
                          LOG.info("error = " + ((Rpc.RpcFinished) message).getErrorText());
                        }
                        activeRpcs.remove(sequenceNo);
                      }
                      sendMessage(sequenceNo, message);
                      if (profilingTimer != null) {
                        Timing.stop(profilingTimer);
                      }
                    }
                  });

          // Kick off a new thread specific to this RPC.
          activeRpcs.put(sequenceNo, controller);
          provider.threadPool.execute(controller);
        }
      } else {
        // Sent a message type we understand, but don't expect - erronous case!
        throw new IllegalStateException(
            "Got expected but unknown message  (" + message + ") for sequence: " + sequenceNo);
      }
    }
  }

  /**
   * Construct a new ServerRpcProvider, hosting on the specified
   * WebSocket addresses.
   *
   * Also accepts an ExecutorService for spawning managing threads.
   */
  public ServerRpcProvider(InetSocketAddress[] httpAddresses,
      String[] resourceBases, Executor threadPool, SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager, String sessionStoreDir,
      boolean sslEnabled, String sslKeystorePath, String sslKeystorePassword) {
    this.httpAddresses = httpAddresses;
    this.resourceBases = resourceBases;
    this.threadPool = threadPool;
    this.sessionManager = sessionManager;
    this.jettySessionManager = jettySessionManager;
    this.sessionStoreDir = sessionStoreDir;
    this.sslEnabled = sslEnabled;
    this.sslKeystorePath = sslKeystorePath;
    this.sslKeystorePassword = sslKeystorePassword;
  }

  /**
   * Constructs a new ServerRpcProvider with a default ExecutorService.
   */
  public ServerRpcProvider(InetSocketAddress[] httpAddresses,
      String[] resourceBases, SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager, String sessionStoreDir,
      boolean sslEnabled, String sslKeystorePath, String sslKeystorePassword,
      Executor executor) {
    this(httpAddresses, resourceBases, executor,
        sessionManager, jettySessionManager, sessionStoreDir, sslEnabled, sslKeystorePath,
        sslKeystorePassword);
  }

  @Inject
  public ServerRpcProvider(Config config,
                           SessionManager sessionManager, org.eclipse.jetty.server.SessionManager jettySessionManager,
                           @ClientServerExecutor Executor executorService) {
    this(parseAddressList(config.getStringList("core.http_frontend_addresses"),
                    config.getString("core.http_websocket_public_address")),
            config.getStringList("core.resource_bases").toArray(new String[0]),
            sessionManager,
            jettySessionManager,
            config.getString("core.sessions_store_directory"),
            config.getBoolean("security.enable_ssl"),
            config.getString("security.ssl_keystore_path"),
            config.getString("security.ssl_keystore_password"),
            executorService);
  }

  public void startWebSocketServer(final Injector injector) {
    httpServer = new Server();

    List<Connector> connectors = getSelectChannelConnectors(httpAddresses);
    if (connectors.isEmpty()) {
      LOG.severe("No valid http end point address provided!");
    }
    for (Connector connector : connectors) {
      httpServer.addConnector(connector);
    }
    final WebAppContext context = new WebAppContext();

    context.setParentLoaderPriority(true);

    if (jettySessionManager != null) {
      // This disables JSessionIDs in URLs redirects
      // see: http://stackoverflow.com/questions/7727534/how-do-you-disable-jsessionid-for-jetty-running-with-the-eclipse-jetty-maven-plu
      // and: http://jira.codehaus.org/browse/JETTY-467?focusedCommentId=114884&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-114884
      jettySessionManager.setSessionIdPathParameterName(null);

      context.getSessionHandler().setSessionManager(jettySessionManager);
    }
    final ResourceCollection resources = new ResourceCollection(resourceBases);
    context.setBaseResource(resources);

    addWebSocketServlets();

    try {

      final ServletModule servletModule = getServletModule();

      ServletContextListener contextListener = new GuiceServletContextListener() {

        private final Injector childInjector = injector.createChildInjector(servletModule);

        @Override
        protected Injector getInjector() {
          return childInjector;
        }
      };

      context.addEventListener(contextListener);
      context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
      context.addFilter(GzipFilter.class, "/webclient/*", EnumSet.allOf(DispatcherType.class));
      httpServer.setHandler(context);

      httpServer.start();
      restoreSessions();

    } catch (Exception e) { // yes, .start() throws "Exception"
      LOG.severe("Fatal error starting http server.", e);
      return;
    }
    LOG.fine("WebSocket server running.");
  }

  private void restoreSessions() {
    try {
      HashSessionManager hashSessionManager = (HashSessionManager) jettySessionManager;
      hashSessionManager.setStoreDirectory(FileUtils.createDirIfNotExists(sessionStoreDir,
          "Session persistence"));
      hashSessionManager.setSavePeriod(60);
      hashSessionManager.restoreSessions();
    } catch (Exception e) {
      LOG.warning("Cannot restore sessions");
    }
  }
  public void addWebSocketServlets() {
    // Servlet where the websocket connection is served from.
    ServletHolder wsholder = addServlet("/socket", WaveWebSocketServlet.class);
    // TODO(zamfi): fix to let messages span frames.
    wsholder.setInitParameter("bufferSize", "" + BUFFER_SIZE);

    // Atmosphere framework. Replacement of Socket.IO
    // See https://issues.apache.org/jira/browse/WAVE-405
    ServletHolder atholder = addServlet("/atmosphere*", AtmosphereGuiceServlet.class);
    // Enable guice. See
    // https://github.com/Atmosphere/atmosphere/wiki/Configuring-Atmosphere%27s-Classes-Creation-and-Injection
    atholder.setInitParameter("org.atmosphere.cpr.objectFactory",
        "org.waveprotocol.box.server.rpc.atmosphere.GuiceAtmosphereFactory");
    atholder.setAsyncSupported(true);
    atholder.setInitOrder(0);

    // Serve the static content and GWT web client with the default servlet
    // (acts like a standard file-based web server).
    addServlet("/static/*", DefaultServlet.class);
    addServlet("/webclient/*", DefaultServlet.class);
  }

  public ServletModule getServletModule() {

    return new ServletModule() {
      @Override
      protected void configureServlets() {
        // We add servlets here to override the DefaultServlet automatic registered by WebAppContext
        // in path "/" with our WaveClientServlet. Any other way to do this?
        // Related question (unanswered 08-Apr-2011)
        // http://web.archiveorange.com/archive/v/d0LdlXf1kN0OXyPNyQZp
        for (Pair<String, ServletHolder> servlet : servletRegistry) {
          String url = servlet.getFirst();
          @SuppressWarnings("unchecked")
          Class<HttpServlet> clazz = (Class<HttpServlet>) servlet.getSecond().getHeldClass();
          Map<String,String> params = servlet.getSecond().getInitParameters();
          serve(url).with(clazz,params);
          bind(clazz).in(Singleton.class);
        }
        for (Pair<String, Class<? extends Filter>> filter : filterRegistry) {
          filter(filter.first).through(filter.second);
        }
      }
    };
  }

  private static InetSocketAddress[] parseAddressList(List<String> addressList, String websocketAddress) {
    if (addressList == null || addressList.size() == 0) {
      return new InetSocketAddress[0];
    } else {
      Set<InetSocketAddress> addresses = Sets.newHashSet();
      // We add the websocketAddress as another listening address.
      ArrayList<String> mergedAddressList = new ArrayList<>(addressList);
      if (!StringUtils.isEmpty(websocketAddress)) {
        mergedAddressList.add(websocketAddress);
      }
      for (String str : mergedAddressList) {
        if (str.length() == 0) {
          LOG.warning("Encountered empty address in http addresses list.");
        } else {
          try {
            InetSocketAddress address = NetUtils.parseHttpAddress(str);
            if (!addresses.contains(address)) {
              addresses.add(address);
            } else {
              LOG.warning(
                  "Ignoring duplicate address in http addresses list: Duplicate entry '" + str
                      + "' resolved to " + address.getAddress().getHostAddress());
            }
          } catch (IOException e) {
            LOG.severe("Unable to process address " + str, e);
          }
        }
      }
      return addresses.toArray(new InetSocketAddress[addresses.size()]);
    }
  }

  /**
   * @return a list of {@link SelectChannelConnector} each bound to a host:port
   *         pair form the list addresses.
   */
  private List<Connector> getSelectChannelConnectors(
      InetSocketAddress[] httpAddresses) {
    List<Connector> list = Lists.newArrayList();
    String[] excludeCiphers = {"SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                               "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
                               "SSL_DHE_RSA_WITH_DES_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                               "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA"};
    SslContextFactory sslContextFactory = null;

    if (sslEnabled) {
      Preconditions.checkState(sslKeystorePath != null && !sslKeystorePath.isEmpty(),
          "SSL Keystore path left blank");
      Preconditions.checkState(sslKeystorePassword != null && !sslKeystorePassword.isEmpty(),
          "SSL Keystore password left blank");

      sslContextFactory = new SslContextFactory(sslKeystorePath);
      sslContextFactory.setKeyStorePassword(sslKeystorePassword);
      sslContextFactory.setRenegotiationAllowed(false);
      sslContextFactory.setExcludeCipherSuites(excludeCiphers);

      // Note: we only actually needed client auth for AuthenticationServlet.
      // Using Need instead of Want prevents web-sockets from working on
      // Chrome.
      sslContextFactory.setWantClientAuth(true);
    }

    for (InetSocketAddress address : httpAddresses) {
      ServerConnector connector;
      if (sslEnabled) {
        connector = new ServerConnector(httpServer, sslContextFactory);
      } else {
        connector = new ServerConnector(httpServer);
      }
      connector.setHost(address.getAddress().getHostAddress());
      connector.setPort(address.getPort());
      connector.setIdleTimeout(0);
      list.add(connector);
    }

    return list;
  }

  @SuppressWarnings("serial")
  @Singleton
  public static class WaveWebSocketServlet extends WebSocketServlet {

    final ServerRpcProvider provider;
    final int websocketMaxIdleTime;
    final int websocketMaxMessageSize;

    @Inject
    public WaveWebSocketServlet(ServerRpcProvider provider, Config config) {
      super();
      this.provider = provider;
      this.websocketMaxIdleTime = config.getInt("network.websocket_max_idle_time");
      this.websocketMaxMessageSize = config.getInt("network.websocket_max_message_size");
    }

    @SuppressWarnings("cast")
    @Override
    public void configure(WebSocketServletFactory factory) {
      if (websocketMaxIdleTime == 0) {
        // Jetty does not allow to set infinite timeout.
        factory.getPolicy().setIdleTimeout(Integer.MAX_VALUE);
      } else {
        factory.getPolicy().setIdleTimeout(websocketMaxIdleTime);
      }
      factory.getPolicy().setMaxTextMessageSize(websocketMaxMessageSize * 1024 * 1024);
      factory.setCreator(new WebSocketCreator() {
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
          ParticipantId loggedInUser =
              provider.sessionManager.getLoggedInUser(req.getSession());

          return new WebSocketConnection(loggedInUser, provider).getWebSocketServerChannel();
        }
      });
    }
  }

  /**
   * Manange atmosphere connections and dispatch messages to
   * wave channels.
   *
   * @author pablojan@gmail.com <Pablo Ojanguren>
   *
   */
  @Singleton
  @AtmosphereHandlerService(path = "/atmosphere",
      interceptors = {AtmosphereClientInterceptor.class},
      broadcasterCache = UUIDBroadcasterCache.class)
  public static class WaveAtmosphereService implements AtmosphereHandler {


    private static final Log LOG = Log.get(WaveAtmosphereService.class);

    private static final String WAVE_CHANNEL_ATTRIBUTE = "WAVE_CHANNEL_ATTRIBUTE";
    private static final String MSG_SEPARATOR = "|";
    private static final String MSG_CHARSET = "UTF-8";

    @Inject
    public ServerRpcProvider provider;


    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {

      AtmosphereResourceSession resourceSession =
          AtmosphereResourceSessionFactory.getDefault().getSession(resource);

      AtmosphereChannel resourceChannel =
          resourceSession.getAttribute(WAVE_CHANNEL_ATTRIBUTE, AtmosphereChannel.class);

      if (resourceChannel == null) {

        ParticipantId loggedInUser =
            provider.sessionManager.getLoggedInUser(resource.getRequest().getSession(false));

        AtmosphereConnection connection = new AtmosphereConnection(loggedInUser, provider);
        resourceChannel = connection.getAtmosphereChannel();
        resourceSession.setAttribute(WAVE_CHANNEL_ATTRIBUTE, resourceChannel);
        resourceChannel.onConnect(resource);
      }

      resource.setBroadcaster(resourceChannel.getBroadcaster()); // on every
                                                                 // request

      if (resource.getRequest().getMethod().equalsIgnoreCase("GET")) {

        resource.suspend();

      }


      if (resource.getRequest().getMethod().equalsIgnoreCase("POST")) {

        StringBuilder b = IOUtils.readEntirely(resource);
        resourceChannel.onMessage(b.toString());

      }

    }


    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {


      AtmosphereResponse response = event.getResource().getResponse();
      AtmosphereResource resource = event.getResource();

      if (event.isSuspended()) {

        // Set content type before do response.getWriter()
        // http://docs.oracle.com/javaee/5/api/javax/servlet/ServletResponse.html#setContentType(java.lang.String)
        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");


        if (event.getMessage().getClass().isArray()) {

          LOG.fine("SEND MESSAGE ARRAY " + event.getMessage().toString());

          List<Object> list = Collections.singletonList(event.getMessage());

          response.getOutputStream().write(MSG_SEPARATOR.getBytes(MSG_CHARSET));
          for (Object object : list) {
            String message = (String) object;
            message += MSG_SEPARATOR;
            response.getOutputStream().write(message.getBytes(MSG_CHARSET));
          }

        } else if (event.getMessage() instanceof List) {

          LOG.fine("SEND MESSAGE LIST " + event.getMessage().toString());

          @SuppressWarnings("unchecked")
          List<Object> list = List.class.cast(event.getMessage());

          response.getOutputStream().write(MSG_SEPARATOR.getBytes(MSG_CHARSET));
          for (Object object : list) {
            String message = (String) object;
            message += MSG_SEPARATOR;
            response.getOutputStream().write(message.getBytes(MSG_CHARSET));
          }

        } else if (event.getMessage() instanceof String) {

          LOG.fine("SEND MESSAGE " + event.getMessage().toString());

          String message = (String) event.getMessage();
          response.getOutputStream().write(message.getBytes(MSG_CHARSET));
        }



        try {

          response.flushBuffer();

          switch (resource.transport()) {
            case JSONP:
            case LONG_POLLING:
              event.getResource().resume();
              break;
            case WEBSOCKET:
            case STREAMING:
            case SSE:
              response.getOutputStream().flush();
              break;
            default:
              LOG.info("Unknown transport");
              break;
          }
        } catch (IOException e) {
          LOG.info("Error resuming resource response", e);
        }


      } else if (event.isResuming()) {

        LOG.fine("RESUMING");

      } else if (event.isResumedOnTimeout()) {

        LOG.fine("RESUMED ON TIMEOUT");

      } else if (event.isClosedByApplication() || event.isClosedByClient()) {

        LOG.fine("CONNECTION CLOSED");

        AtmosphereResourceSession resourceSession =
            AtmosphereResourceSessionFactory.getDefault().getSession(resource);

        AtmosphereChannel resourceChannel =
            resourceSession.getAttribute(WAVE_CHANNEL_ATTRIBUTE, AtmosphereChannel.class);

        if (resourceChannel != null) {
          resourceChannel.onDisconnect();
        }
      }
    }

    @Override
    public void destroy() {
      // Nothing to do

    }


  }

  /**
   * Returns the socket the WebSocket server is listening on.
   */
  public SocketAddress getWebSocketAddress() {
    if (httpServer == null) {
      return null;
    } else {
      ServerConnector c = (ServerConnector)httpServer.getConnectors()[0];
      return new InetSocketAddress(c.getHost(), c.getLocalPort());
    }
  }

  /**
   * Stops this server.
   */
  public void stopServer() throws IOException {
    try {
      httpServer.stop(); // yes, .stop() throws "Exception"
    } catch (Exception e) {
      LOG.warning("Fatal error stopping http server.", e);
    }
    LOG.fine("server shutdown.");
  }

  /**
   * Register all methods provided by the given service type.
   */
  public void registerService(Service service) {
    synchronized (registeredServices) {
      for (MethodDescriptor methodDescriptor : service.getDescriptorForType().getMethods()) {
        registeredServices.put(methodDescriptor.getInputType(),
            new RegisteredServiceMethod(service, methodDescriptor));
      }
    }
  }

  /**
   * List of servlets
   */
  List<Pair<String, ServletHolder>> servletRegistry = Lists.newArrayList();

  /**
   * List of filters
   */
  List<Pair<String, Class<? extends Filter>>> filterRegistry = Lists.newArrayList();

  /**
   * Add a servlet to the servlet registry. This servlet will be attached to the
   * specified URL pattern when the server is started up.
   *
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   * @param servlet the servlet class to bind to the specified paths.
   * @param initParams the map with init params, can be null or empty.
   * @return the {@link ServletHolder} that holds the servlet.
   */
  public ServletHolder addServlet(String urlPattern, Class<? extends HttpServlet> servlet,
      @Nullable Map<String, String> initParams) {
    ServletHolder servletHolder = new ServletHolder(servlet);
    if (initParams != null) {
      servletHolder.setInitParameters(initParams);
    }
    servletRegistry.add(Pair.of(urlPattern, servletHolder));
    return servletHolder;
  }

  /**
   * Add a servlet to the servlet registry. This servlet will be attached to the
   * specified URL pattern when the server is started up.
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   * @param servlet the servlet class to bind to the specified paths.
   * @return the {@link ServletHolder} that holds the servlet.
   */
  public ServletHolder addServlet(String urlPattern, Class<? extends HttpServlet> servlet) {
    return addServlet(urlPattern, servlet, null);
  }

  /**
   * Add a filter to the filter registry. This filter will be attached to the
   * specified URL pattern when the server is started up.
   *
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   *
   */
  public void addFilter(String urlPattern, Class<? extends Filter> filter) {
    filterRegistry.add(new Pair<String, Class<? extends Filter>>(urlPattern, filter));
  }
}
