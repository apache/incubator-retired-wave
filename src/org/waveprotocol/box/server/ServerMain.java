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

package org.waveprotocol.box.server;

import com.google.gwt.logging.server.RemoteLoggingServiceImpl;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.apache.commons.configuration.ConfigurationException;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.frontend.ClientFrontend;
import org.waveprotocol.box.server.frontend.ClientFrontendImpl;
import org.waveprotocol.box.server.frontend.WaveClientRpcImpl;
import org.waveprotocol.box.server.frontend.WaveletInfo;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceModule;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.box.server.robots.RobotApiModule;
import org.waveprotocol.box.server.robots.RobotRegistrationServlet;
import org.waveprotocol.box.server.robots.active.ActiveApiServlet;
import org.waveprotocol.box.server.robots.agent.passwd.PasswordAdminRobot;
import org.waveprotocol.box.server.robots.agent.passwd.PasswordRobot;
import org.waveprotocol.box.server.robots.agent.registration.RegistrationRobot;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.box.server.robots.dataapi.DataApiOAuthServlet;
import org.waveprotocol.box.server.robots.dataapi.DataApiServlet;
import org.waveprotocol.box.server.robots.passive.RobotsGateway;
import org.waveprotocol.box.server.rpc.AttachmentServlet;
import org.waveprotocol.box.server.rpc.AuthenticationServlet;
import org.waveprotocol.box.server.rpc.FetchProfilesServlet;
import org.waveprotocol.box.server.rpc.FetchServlet;
import org.waveprotocol.box.server.rpc.GadgetProviderServlet;
import org.waveprotocol.box.server.rpc.NotificationServlet;
import org.waveprotocol.box.server.rpc.SearchServlet;
import org.waveprotocol.box.server.rpc.ServerRpcProvider;
import org.waveprotocol.box.server.rpc.SignOutServlet;
import org.waveprotocol.box.server.rpc.UserRegistrationServlet;
import org.waveprotocol.box.server.rpc.WaveClientServlet;
import org.waveprotocol.box.server.rpc.WaveRefServlet;
import org.waveprotocol.box.server.waveserver.PerUserWaveViewBus;
import org.waveprotocol.box.server.waveserver.PerUserWaveViewDistpatcher;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveIndexer;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletStateException;
import org.waveprotocol.box.server.rpc.AttachmentInfoServlet;
import org.waveprotocol.box.server.rpc.AttachmentServlet;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.federation.FederationSettings;
import org.waveprotocol.wave.federation.FederationTransport;
import org.waveprotocol.wave.federation.noop.NoOpFederationModule;
import org.waveprotocol.wave.federation.xmpp.XmppFederationModule;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.util.settings.SettingsBinder;


import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.box.server.rpc.LocaleServlet;

/**
 * Wave Server entrypoint.
 */
public class ServerMain {

  /**
   * This is the name of the system property used to find the server config file.
   */
  private static final String PROPERTIES_FILE_KEY = "wave.server.config";

  private static final Log LOG = Log.get(ServerMain.class);

  @SuppressWarnings("serial")
  @Singleton
  public static class GadgetProxyServlet extends HttpServlet {

    ProxyServlet.Transparent proxyServlet;

    @Inject
    public GadgetProxyServlet(@Named(CoreSettings.GADGET_SERVER_HOSTNAME) String gadgetServerHostname,
        @Named(CoreSettings.GADGET_SERVER_PORT) int gadgetServerPort){

      LOG.info("Starting GadgetProxyServlet for " + gadgetServerHostname + ":" + gadgetServerPort);
      proxyServlet = new ProxyServlet.Transparent(
          "http://" + gadgetServerHostname + ":" + gadgetServerPort + "/gadgets",
          "/gadgets");      
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
      proxyServlet.init(config);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      proxyServlet.service(req, res);
    }
  }

  public static void main(String... args) {
    try {
      Module coreSettings = SettingsBinder.bindSettings(PROPERTIES_FILE_KEY, CoreSettings.class);
      run(coreSettings);
      return;
    } catch (PersistenceException e) {
      LOG.severe("PersistenceException when running server:", e);
    } catch (ConfigurationException e) {
      LOG.severe("ConfigurationException when running server:", e);
    } catch (WaveServerException e) {
      LOG.severe("WaveServerException when running server:", e);
    }
  }

  public static void run(Module coreSettings) throws PersistenceException,
      ConfigurationException, WaveServerException {
    Injector settingsInjector = Guice.createInjector(coreSettings);
    boolean enableFederation = settingsInjector.getInstance(Key.get(Boolean.class,
        Names.named(CoreSettings.ENABLE_FEDERATION)));

    int listenerCount = settingsInjector.getInstance(Key.get(Integer.class,
        Names.named(CoreSettings.LISTENER_EXECUTOR_THREAD_COUNT)));
    int waveletLoadCount = settingsInjector.getInstance(Key.get(Integer.class,
        Names.named(CoreSettings.WAVELET_LOAD_EXECUTOR_THREAD_COUNT)));
    int deltaPersistCount = settingsInjector.getInstance(Key.get(Integer.class,
        Names.named(CoreSettings.DELTA_PERSIST_EXECUTOR_THREAD_COUNT)));
    int storageContinuationCount = settingsInjector.getInstance(Key.get(Integer.class,
        Names.named(CoreSettings.STORAGE_CONTINUATION_EXECUTOR_THREAD_COUNT)));
    int lookupCount = settingsInjector.getInstance(Key.get(Integer.class,
        Names.named(CoreSettings.LOOKUP_EXECUTOR_THREAD_COUNT)));

    if (enableFederation) {
      Module federationSettings =
          SettingsBinder.bindSettings(PROPERTIES_FILE_KEY, FederationSettings.class);
      // This MUST happen first, or bindings will fail if federation is enabled.
      settingsInjector = settingsInjector.createChildInjector(federationSettings);
    }

    Module federationModule = buildFederationModule(settingsInjector, enableFederation);
    PersistenceModule persistenceModule = settingsInjector.getInstance(PersistenceModule.class);
    Module searchModule = settingsInjector.getInstance(SearchModule.class);
    Injector injector =
        settingsInjector.createChildInjector(new ServerModule(enableFederation, listenerCount,
            waveletLoadCount, deltaPersistCount, storageContinuationCount, lookupCount),
            new RobotApiModule(), federationModule, persistenceModule, searchModule);

    ServerRpcProvider server = injector.getInstance(ServerRpcProvider.class);
    WaveBus waveBus = injector.getInstance(WaveBus.class);

    String domain =
      injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN)));
    if (!ParticipantIdUtil.isDomainAddress(ParticipantIdUtil.makeDomainAddress(domain))) {
      throw new WaveServerException("Invalid wave domain: " + domain);
    }

    initializeServer(injector, domain);
    initializeServlets(injector, server);
    initializeRobotAgents(injector, server);
    initializeRobots(injector, waveBus);
    initializeFrontend(injector, server, waveBus);
    initializeFederation(injector);
    initializeSearch(injector, waveBus);

    LOG.info("Starting server");
    server.startWebSocketServer(injector);
  }

  private static Module buildFederationModule(Injector settingsInjector, boolean enableFederation)
      throws ConfigurationException {
    Module federationModule;
    if (enableFederation) {
      federationModule = settingsInjector.getInstance(XmppFederationModule.class);
    } else {
      federationModule = settingsInjector.getInstance(NoOpFederationModule.class);
    }
    return federationModule;
  }

  private static void initializeServer(Injector injector, String waveDomain)
      throws PersistenceException, WaveServerException {
    AccountStore accountStore = injector.getInstance(AccountStore.class);
    accountStore.initializeAccountStore();
    AccountStoreHolder.init(accountStore, waveDomain);

    // Initialize the SignerInfoStore.
    CertPathStore certPathStore = injector.getInstance(CertPathStore.class);
    if (certPathStore instanceof SignerInfoStore) {
      ((SignerInfoStore)certPathStore).initializeSignerInfoStore();
    }

    // Initialize the server.
    WaveletProvider waveServer = injector.getInstance(WaveletProvider.class);
    waveServer.initialize();
  }

  private static void initializeServlets(Injector injector, ServerRpcProvider server) {
    server.addServlet("/gadget/gadgetlist", GadgetProviderServlet.class);

    server.addServlet(AttachmentServlet.ATTACHMENT_URL + "/*", AttachmentServlet.class);
    server.addServlet(AttachmentServlet.THUMBNAIL_URL + "/*", AttachmentServlet.class);
    server.addServlet(AttachmentInfoServlet.ATTACHMENTS_INFO_URL, AttachmentInfoServlet.class);

    server.addServlet(SessionManager.SIGN_IN_URL, AuthenticationServlet.class);
    server.addServlet("/auth/signout", SignOutServlet.class);
    server.addServlet("/auth/register", UserRegistrationServlet.class);

    server.addServlet("/locale/*", LocaleServlet.class);
    server.addServlet("/fetch/*", FetchServlet.class);
    server.addServlet("/search/*", SearchServlet.class);
    server.addServlet("/notification/*", NotificationServlet.class);

    server.addServlet("/robot/dataapi", DataApiServlet.class);
    server.addServlet(DataApiOAuthServlet.DATA_API_OAUTH_PATH + "/*", DataApiOAuthServlet.class);
    server.addServlet("/robot/dataapi/rpc", DataApiServlet.class);
    server.addServlet("/robot/register/*", RobotRegistrationServlet.class);
    server.addServlet("/robot/rpc", ActiveApiServlet.class);
    server.addServlet("/webclient/remote_logging", RemoteLoggingServiceImpl.class);
    server.addServlet("/profile/*", FetchProfilesServlet.class);
    server.addServlet("/waveref/*", WaveRefServlet.class);

    String gadgetHostName =
        injector
            .getInstance(Key.get(String.class, Names.named(CoreSettings.GADGET_SERVER_HOSTNAME)));
    int port =
        injector.getInstance(Key.get(Integer.class, Names.named(CoreSettings.GADGET_SERVER_PORT)));
    Map<String, String> initParams =
        Collections.singletonMap("HostHeader", gadgetHostName + ":" + port);
    server.addServlet("/gadgets/*", GadgetProxyServlet.class, initParams);

    server.addServlet("/", WaveClientServlet.class);
  }

  private static void initializeRobots(Injector injector, WaveBus waveBus) {
    RobotsGateway robotsGateway = injector.getInstance(RobotsGateway.class);
    waveBus.subscribe(robotsGateway);
  }

  private static void initializeRobotAgents(Injector injector, ServerRpcProvider server) {
    server.addServlet(PasswordRobot.ROBOT_URI + "/*", PasswordRobot.class);
    server.addServlet(PasswordAdminRobot.ROBOT_URI + "/*", PasswordAdminRobot.class);
    server.addServlet(WelcomeRobot.ROBOT_URI + "/*", WelcomeRobot.class);
    server.addServlet(RegistrationRobot.ROBOT_URI + "/*", RegistrationRobot.class);
  }

  private static void initializeFrontend(Injector injector, ServerRpcProvider server,
      WaveBus waveBus) throws WaveServerException {
    HashedVersionFactory hashFactory = injector.getInstance(HashedVersionFactory.class);

    WaveletProvider provider = injector.getInstance(WaveletProvider.class);
    WaveletInfo waveletInfo = WaveletInfo.create(hashFactory, provider);
    ClientFrontend frontend =
        ClientFrontendImpl.create(provider, waveBus, waveletInfo);

    ProtocolWaveClientRpc.Interface rpcImpl = WaveClientRpcImpl.create(frontend, false);
    server.registerService(ProtocolWaveClientRpc.newReflectiveService(rpcImpl));
  }

  private static void initializeFederation(Injector injector) {
    FederationTransport federationManager = injector.getInstance(FederationTransport.class);
    federationManager.startFederation();
  }

  private static void initializeSearch(Injector injector, WaveBus waveBus)
      throws WaveletStateException, WaveServerException {
    PerUserWaveViewDistpatcher waveViewDistpatcher =
        injector.getInstance(PerUserWaveViewDistpatcher.class);
    PerUserWaveViewBus.Listener listener = injector.getInstance(PerUserWaveViewBus.Listener.class);
    waveViewDistpatcher.addListener(listener);
    waveBus.subscribe(waveViewDistpatcher);

    WaveIndexer waveIndexer = injector.getInstance(WaveIndexer.class);
    waveIndexer.remakeIndex();
  }
}
