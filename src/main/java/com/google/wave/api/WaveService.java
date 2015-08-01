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

package com.google.wave.api;

import com.google.gson.Gson;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.impl.RawAttachmentData;
import com.google.wave.api.impl.GsonFactory;
import com.google.wave.api.impl.RawDeltasListener;
import com.google.wave.api.impl.WaveletData;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.media.model.AttachmentId;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpClient;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;
import net.oauth.signature.OAuthSignatureMethod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for using OAuth to talk to Wave service.
 */
public class WaveService {

  /** Infinite {@code urlfetch} fetch timeout. */
  public static final int FETCH_INFINITE_TIMEOUT = 0;

  /** Default {@code urlfetch} fetch timeout in ms. */
  public static final int FETCH_DEFAILT_TIMEOUT_IN_MS = 10 * 1000;

  /**
   * Helper class to make outgoing OAuth HTTP requests.
   */
  static class HttpFetcher implements HttpClient {

    private static final String HTTP_POST_METHOD = "POST";
    private static final String HTTP_PUT_METHOD = "PUT";

    private int fetchTimeout = FETCH_DEFAILT_TIMEOUT_IN_MS;

    @Override
    public HttpResponseMessage execute(HttpMessage request, Map<String, Object> stringObjectMap)
        throws IOException {
      String body = readInputStream(request.getBody());
      OutputStreamWriter out = null;
      HttpURLConnection conn = null;
      // Open the connection.
      conn = (HttpURLConnection) request.url.openConnection();
      conn.setReadTimeout(fetchTimeout);
      conn.setRequestMethod(request.method);
      // Add the headers
      if (request.headers != null) {
        for (java.util.Map.Entry<String, String> header : request.headers) {
          conn.setRequestProperty(header.getKey(), header.getValue());
        }
      }

      boolean doOutput =
          body != null && (HTTP_POST_METHOD.equalsIgnoreCase(request.method)
              || HTTP_PUT_METHOD.equalsIgnoreCase(request.method));

      if (doOutput) {
        conn.setDoOutput(true);
      }

      conn.connect();

      if (doOutput) {
        // Send the request body.
        out = new OutputStreamWriter(conn.getOutputStream(), UTF_8);
        try {
          out.write(body);
          out.flush();
        } finally {
          out.close();
        }
      }

      // Return the response stream.
      return new HttpResponse(
          request.method, request.url, conn.getResponseCode(), conn.getInputStream());
    }

    /**
     * Reads the given {@link java.io.InputStream} into a {@link String}
     *
     * @param inputStream the {@link java.io.InputStream} to be read.
     * @return a string content of the {@link java.io.InputStream}.
     * @throws IOException if there is a problem reading the stream.
     */
    static String readInputStream(InputStream inputStream) throws IOException {
      if (inputStream == null) {
        return null;
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder result = new StringBuilder();
      String s;
      while ((s = reader.readLine()) != null) {
        result.append(s);
      }
      return result.toString();
    }

    /**
     * Sets the fetch timeout to a specified timeout, in milliseconds.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param fetchTimeout
     */
    void setTimeout(int fetchTimeout) {
      this.fetchTimeout = fetchTimeout;
    }
  }

  /**
   * A simple implementation of {@link HttpResponseMessage} that gets the
   * response from {@link HttpURLConnection#getInputStream()}.
   */
  static class HttpResponse extends HttpResponseMessage {

    /** The HTTP response code. */
    private final int statusCode;

    /** The response stream. */
    private final InputStream responseStream;

    /**
     * Constructor.
     *
     * @param method the HTTP method, for example, GET or POST.
     * @param url the URL where the response comes from.
     * @param statusCode the HTTP response code.
     * @param responseStream the response stream.
     */
    public HttpResponse(String method, URL url, int statusCode, InputStream responseStream) {
      super(method, url);
      this.statusCode = statusCode;
      this.responseStream = responseStream;
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public InputStream openBody() {
      return responseStream;
    }
  }

  /**
   * Helper class that contains various OAuth credentials.
   */
  static class ConsumerData {

    /** Consumer key used to sign the operations in the active mode. */
    private final String consumerKey;

    /** Consumer secret used to sign the operations in the active mode. */
    private final String consumerSecret;

    /** The URL that handles the JSON-RPC request in the active mode. */
    private final String rpcServerUrl;

    /** Whether this session is user authenticated */
    private final boolean userAuthenticated;

    /** The OAuth Accessor contains authentication data used to make requests */
    private final OAuthAccessor accessor;

    /**
     * Constructor.
     *
     * @param consumerKey the consumer key.
     * @param consumerSecret the consumer secret
     * @param rpcServerUrl the URL of the JSON-RPC request handler
     */
    public ConsumerData(String consumerKey, String consumerSecret, String rpcServerUrl) {
      String consumerKeyPrefix = "";
      // NOTE(ljvderijk): Present for backwards capability.
      if (RPC_URL.equals(rpcServerUrl) || SANDBOX_RPC_URL.equals(rpcServerUrl)) {
        consumerKeyPrefix = "google.com:";
      }
      this.consumerKey = consumerKeyPrefix + consumerKey;
      this.consumerSecret = consumerSecret;
      this.rpcServerUrl = rpcServerUrl;

      userAuthenticated = false;
      OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, null);
      consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
      accessor = new OAuthAccessor(consumer);
    }

    public ConsumerData(OAuthAccessor accessor, String rpcServerUrl) {
      this.consumerKey = accessor.consumer.consumerKey;
      this.consumerSecret = accessor.consumer.consumerSecret;
      this.accessor = accessor;
      this.rpcServerUrl = rpcServerUrl;
      userAuthenticated = true;
    }

    /**
     * @return the consumer key used to sign the operations in the active mode.
     */
    public String getConsumerKey() {
      return consumerKey;
    }

    /**
     * @return the consumer secret used to sign the operations in the active mode.
     */
    public String getConsumerSecret() {
      return consumerSecret;
    }

    /**
     * @return the URL of the JSON-RPC request handler.
     */
    public String getRpcServerUrl() {
      return rpcServerUrl;
    }

    public boolean isUserAuthenticated() {
      return userAuthenticated;
    }

    public OAuthAccessor getAccessor() {
      return accessor;
    }

  }

  /** The wire protocol version. */
  public static final ProtocolVersion PROTOCOL_VERSION = ProtocolVersion.DEFAULT;

  private static final String JSON_MIME_TYPE = "application/json; charset=utf-8";
  private static final String OAUTH_BODY_HASH = "oauth_body_hash";
  private static final String POST = "POST";
  private static final String SHA_1 = "SHA-1";
  private static final String UTF_8 = "UTF-8";

  /** Wave RPC URLs. */
  public static final String RPC_URL = "https://www-opensocial.googleusercontent.com/api/rpc";
  public static final String SANDBOX_RPC_URL =
      "https://www-opensocial-sandbox.googleusercontent.com/api/rpc";

  private static final Logger LOG = Logger.getLogger(WaveService.class.getName());

  /** Namespace to prefix all active api operation calls. */
  private static final String OPERATION_NAMESPACE = "wave";

  /** Serializer to serialize events and operations in active mode. */
  private static final Gson SERIALIZER = new GsonFactory().create(OPERATION_NAMESPACE);

  /** OAuth request validator. */
  private static final OAuthValidator VALIDATOR = new SimpleOAuthValidator();

  /** A map of RPC server URL to its consumer data object. */
  private final Map<String, ConsumerData> consumerDataMap = new HashMap<String, ConsumerData>();

  /** A version number. */
  private final String version;

  /** A utility to make HTTP requests. */
  private final HttpFetcher httpFetcher;

  /**
   * Constructor.
   */
  public WaveService() {
    this(new HttpFetcher(), null);
  }

  /**
   * Constructor.
   *
   * @param version the version number.
   */
  public WaveService(String version) {
    this(new HttpFetcher(), version);
  }

  /**
   * Constructor.
   *
   * @param httpFetcher the fetcher to make HTTP calls.
   * @param version the version number.
   */
  public WaveService(HttpFetcher httpFetcher, String version) {
    this.httpFetcher = httpFetcher;
    this.version = version;
  }

  /**
   * Sets the OAuth related properties, including the consumer key and secret
   * that are used to sign the outgoing operations.
   *
   * <p>
   * This version of the method is for 2-legged OAuth, where the robot is not
   * acting on behalf of a user.
   *
   * <p>
   * For the rpcServerUrl you can use:
   * <ul>
   * <li>https://www-opensocial.googleusercontent.com/api/rpc - for wave
   * preview.
   * <li>
   * https://www-opensocial-sandbox.googleusercontent.com/api/rpc - for wave
   * sandbox.
   * </ul>
   *
   * @param consumerKey the consumer key.
   * @param consumerSecret the consumer secret.
   * @param rpcServerUrl the URL of the server that serves the JSON-RPC request.
   */
  public void setupOAuth(String consumerKey, String consumerSecret, String rpcServerUrl) {
    if (consumerKey == null || consumerSecret == null || rpcServerUrl == null) {
      throw new IllegalArgumentException(
          "Consumer Key, Consumer Secret and RPCServerURL " + "have to be non-null");
    }
    consumerDataMap.put(rpcServerUrl, new ConsumerData(consumerKey, consumerSecret, rpcServerUrl));
  }

  /**
   * Sets the OAuth related properties that are used to sign the outgoing
   * operations for 3-legged OAuth.
   *
   * <p>
   * Performing the OAuth dance is not part of this interface - once you've done
   * the dance, pass the constructed accessor and rpc endpoint into this method.
   *
   * <p>
   * Ensure that the endpoint URL you pass in matches exactly the URL used to
   * request an access token (including https vs http).
   *
   *  For the rpcServerUrl you can use:
   * <ul>
   * <li>https://www-opensocial.googleusercontent.com/api/rpc - for wave
   * preview.
   * <li>
   * https://www-opensocial-sandbox.googleusercontent.com/api/rpc - for wave
   * sandbox.
   * </ul>
   *
   * @param accessor the {@code OAuthAccessor} with access token and secret
   * @param rpcServerUrl the endpoint URL of the server that serves the JSON-RPC
   *        request.
   */
  public void setupOAuth(OAuthAccessor accessor, String rpcServerUrl) {
    if (accessor == null || rpcServerUrl == null) {
      throw new IllegalArgumentException("Accessor and RPCServerURL have to be non-null");
    }
    consumerDataMap.put(rpcServerUrl, new ConsumerData(accessor, rpcServerUrl));
  }

  /**
   * Validates the incoming HTTP request.
   *
   * @param requestUrl the URL of the request.
   * @param jsonBody the request body to be validated.
   * @param rpcServerUrl the RPC server URL.
   *
   * @throws OAuthException if it can't validate the request.
   */
  public void validateOAuthRequest(
      String requestUrl, Map<String, String[]> requestParams, String jsonBody, String rpcServerUrl)
      throws OAuthException {
    ConsumerData consumerData = consumerDataMap.get(rpcServerUrl);
    if (consumerData == null) {
      throw new IllegalArgumentException(
          "There is no consumer key and secret associated " + "with the given RPC URL "
              + rpcServerUrl);
    }

    List<OAuth.Parameter> params = new ArrayList<OAuth.Parameter>();
    for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
      for (String value : entry.getValue()) {
        params.add(new OAuth.Parameter(entry.getKey(), value));
      }
    }
    OAuthMessage message = new OAuthMessage(POST, requestUrl, params);

    // Compute and check the hash of the body.
    try {
      MessageDigest md = MessageDigest.getInstance(SHA_1);
      byte[] hash = md.digest(jsonBody.getBytes(UTF_8));
      String encodedHash = new String(Base64.encodeBase64(hash, false), UTF_8);
      if (!encodedHash.equals(message.getParameter(OAUTH_BODY_HASH))) {
        throw new IllegalArgumentException(
            "Body hash does not match. Expected: " + encodedHash + ", provided: "
                + message.getParameter(OAUTH_BODY_HASH));
      }

      OAuthAccessor accessor = consumerData.getAccessor();
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Signature base string: " + OAuthSignatureMethod.getBaseString(message));
      }
      VALIDATOR.validateMessage(message, accessor);
    } catch (NoSuchAlgorithmException e) {
      throw new OAuthException("Error validating OAuth request", e);
    } catch (URISyntaxException e) {
      throw new OAuthException("Error validating OAuth request", e);
    } catch (OAuthException e) {
      throw new OAuthException("Error validating OAuth request", e);
    } catch (IOException e) {
      throw new OAuthException("Error validating OAuth request", e);
    }
  }

  /**
   * Submits the pending operations associated with this {@link Wavelet}.
   *
   * @param wavelet the bundle that contains the operations to be submitted.
   * @param rpcServerUrl the active gateway to send the operations to.
   * @return a list of {@link JsonRpcResponse} that represents the responses
   *         from the server for all operations that were submitted.
   *
   * @throws IllegalStateException if this method is called prior to setting the
   *         proper consumer key, secret, and handler URL.
   * @throws IOException if there is a problem submitting the operations.
   */
  public List<JsonRpcResponse> submit(Wavelet wavelet, String rpcServerUrl) throws IOException {
    List<JsonRpcResponse> responses = makeRpc(wavelet.getOperationQueue(), rpcServerUrl);
    wavelet.getOperationQueue().clear();
    return responses;
  }

  /**
   * Returns an empty/blind stub of a wavelet with the given wave id and wavelet
   * id.
   *
   * <p>
   * Call this method if you would like to apply wavelet-only operations
   * without fetching the wave first.
   *
   * The returned wavelet has its own {@link OperationQueue}. It is the
   * responsibility of the caller to make sure this wavelet gets submitted to
   * the server, either by calling {@link AbstractRobot#submit(Wavelet, String)}
   * or by calling {@link Wavelet#submitWith(Wavelet)} on the new wavelet, to
   * join its queue with another wavelet, for example, the event wavelet.
   *
   * @param waveId the id of the wave.
   * @param waveletId the id of the wavelet.
   * @return a stub of a wavelet.
   */
  public Wavelet blindWavelet(WaveId waveId, WaveletId waveletId) {
    return blindWavelet(waveId, waveletId, null);
  }

  /**
   * @see #blindWavelet(WaveId, WaveletId)
   *
   * @param proxyForId the proxying information that should be set on the
   *        operation queue. Please note that this parameter should be properly
   *        encoded to ensure that the resulting participant id is valid (see
   *        {@link Util#checkIsValidProxyForId(String)} for more details).
   */
  public Wavelet blindWavelet(WaveId waveId, WaveletId waveletId, String proxyForId) {
    return blindWavelet(waveId, waveletId, proxyForId, new HashMap<String, Blip>());
  }

  /**
   * @see #blindWavelet(WaveId, WaveletId, String)
   *
   * @param blips a collection of blips that belong to this wavelet.
   */
  public Wavelet blindWavelet(
      WaveId waveId, WaveletId waveletId, String proxyForId, Map<String, Blip> blips) {
    return blindWavelet(waveId, waveletId, proxyForId, blips, new HashMap<String, BlipThread>());
  }

  /**
   * @see #blindWavelet(WaveId, WaveletId, String, Map)
   *
   * @param threads a collection of threads that belong to this wavelet.
   */
  public Wavelet blindWavelet(WaveId waveId, WaveletId waveletId, String proxyForId,
      Map<String, Blip> blips, Map<String, BlipThread> threads) {
    Util.checkIsValidProxyForId(proxyForId);
    Map<String, String> roles = new HashMap<String, String>();
    return new Wavelet(waveId, waveletId, null,
        new BlipThread("", -1, new ArrayList<String>(), blips), Collections.<String>emptySet(),
        roles, blips, threads, new OperationQueue(proxyForId));
  }

  /**
   * Creates a new wave with a list of participants on it.
   *
   * The root wavelet of the new wave is returned with its own
   * {@link OperationQueue}. It is the responsibility of the caller to make sure
   * this wavelet gets submitted to the server, either by calling
   * {@link AbstractRobot#submit(Wavelet, String)} or by calling
   * {@link Wavelet#submitWith(Wavelet)} on the new wavelet.
   *
   * @param domain the domain to create the wavelet on. In general, this should
   *        correspond to the domain of the incoming event wavelet, except when
   *        the robot is calling this method outside of an event or when the
   *        server is handling multiple domains.
   * @param participants the initial participants on the wave. The robot, as the
   *        creator of the wave, will be added by default. The order of the
   *        participants will be preserved.
   */
  public Wavelet newWave(String domain, Set<String> participants) {
    return newWave(domain, participants, null);
  }

  /**
   * @see #newWave(String, Set)
   *
   * @param proxyForId the proxy id that should be used to create the new wave.
   *        If specified, the creator of the wave would be
   *        robotid+<proxyForId>@appspot.com. Please note that this parameter
   *        should be properly encoded to ensure that the resulting participant
   *        id is valid (see {@link Util#checkIsValidProxyForId(String)} for
   *        more details).
   */
  public Wavelet newWave(String domain, Set<String> participants, String proxyForId) {
    return newWave(domain, participants, "", proxyForId);
  }

  /**
   * @see #newWave(String, Set, String)
   *
   * @param msg the message that will be passed back to the robot when
   *        WAVELET_CREATED event is fired as a result of this operation.
   */
  public Wavelet newWave(String domain, Set<String> participants, String msg, String proxyForId) {
    Util.checkIsValidProxyForId(proxyForId);
    return new OperationQueue(proxyForId).createWavelet(domain, participants, msg);
  }

  /**
   * @see #newWave(String, Set, String, String)
   *
   * @param rpcServerUrl if specified, this operation will be submitted
   *        immediately to this active gateway, that will return immediately the
   *        actual wave id, the id of the root wavelet, and id of the root blip.
   *
   * @throws IOException if there is a problem submitting the operation to the
   *         server, when {@code submit} is {@code true}.
   * @throws InvalidIdException
   */
  public Wavelet newWave(
      String domain, Set<String> participants, String msg, String proxyForId, String rpcServerUrl)
      throws IOException, InvalidIdException {
    Util.checkIsValidProxyForId(proxyForId);
    OperationQueue opQueue = new OperationQueue(proxyForId);
    Wavelet newWavelet = opQueue.createWavelet(domain, participants, msg);

    if (rpcServerUrl != null && !rpcServerUrl.isEmpty()) {
      // Get the response for the robot.fetchWavelet() operation, which is the
      // second operation, since makeRpc prepends the robot.notify() operation.
      JsonRpcResponse response = this.submit(newWavelet, rpcServerUrl).get(1);
      if (response.isError()) {
        throw new IOException(response.getErrorMessage());
      }
      WaveId waveId = ApiIdSerializer.instance().deserialiseWaveId(
          (String) response.getData().get(ParamsProperty.WAVE_ID));
      WaveletId waveletId = ApiIdSerializer.instance().deserialiseWaveletId(
          (String) response.getData().get(ParamsProperty.WAVELET_ID));
      String rootBlipId = (String) response.getData().get(ParamsProperty.BLIP_ID);

      Map<String, Blip> blips = new HashMap<String, Blip>();
      Map<String, BlipThread> threads = new HashMap<String, BlipThread>();
      Map<String, String> roles = new HashMap<String, String>();

      List<String> blipIds = new ArrayList<String>();
      blipIds.add(rootBlipId);
      BlipThread rootThread = new BlipThread("", -1, blipIds, blips);

      newWavelet = new Wavelet(waveId, waveletId, rootBlipId, rootThread, participants,
          roles, blips, threads, opQueue);
      blips.put(rootBlipId, new Blip(rootBlipId, "", null, "", newWavelet));
    }
    return newWavelet;
  }

  /**
   * Requests SearchResult for a query.
   *
   * @param query the query to execute.
   * @param index the index from which to return results.
   * @param numresults the number of results to return.
   * @param rpcServerUrl the active gateway.
   *
   * @throws IOException if remote server returns error.
   */
  public SearchResult search(String query, Integer index, Integer numResults, String rpcServerUrl)
      throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.search(query, index, numResults);
    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    return (SearchResult) response.get(ParamsProperty.SEARCH_RESULTS);
  }

  /**
   * Fetches a wavelet using the active API.
   *
   *  The returned wavelet contains a snapshot of the state of the wavelet at
   * that point. It can be used to modify the wavelet, but the wavelet might
   * change in between, so treat carefully.
   *
   * Also, the returned wavelet has its own {@link OperationQueue}. It is the
   * responsibility of the caller to make sure this wavelet gets submitted to
   * the server, either by calling {@link AbstractRobot#submit(Wavelet, String)}
   * or by calling {@link Wavelet#submitWith(Wavelet)} on the new wavelet.
   *
   * @param waveId the id of the wave to fetch.
   * @param waveletId the id of the wavelet to fetch.
   * @param rpcServerUrl the active gateway that is used to fetch the wavelet.
   *
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public Wavelet fetchWavelet(WaveId waveId, WaveletId waveletId, String rpcServerUrl)
      throws IOException {
    return fetchWavelet(waveId, waveletId, null, rpcServerUrl);
  }

  /**
   * @see #fetchWavelet(WaveId, WaveletId, String)
   *
   * @param proxyForId the proxy id that should be used to fetch this wavelet.
   *        Please note that this parameter should be properly encoded to ensure
   *        that the resulting participant id is valid (see
   *        {@link Util#checkIsValidProxyForId(String)} for more details).
   */
  public Wavelet fetchWavelet(
      WaveId waveId, WaveletId waveletId, String proxyForId, String rpcServerUrl)
      throws IOException {
    Util.checkIsValidProxyForId(proxyForId);
    OperationQueue opQueue = new OperationQueue(proxyForId);
    opQueue.fetchWavelet(waveId, waveletId);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);

    // Deserialize wavelet.
    opQueue.clear();
    WaveletData waveletData = (WaveletData) response.get(ParamsProperty.WAVELET_DATA);
    Map<String, Blip> blips = new HashMap<String, Blip>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();
    Wavelet wavelet = Wavelet.deserialize(opQueue, blips, threads, waveletData);

    // Deserialize threads.
    @SuppressWarnings("unchecked")
    Map<String, BlipThread> tempThreads =
        (Map<String, BlipThread>) response.get(ParamsProperty.THREADS);
    for (Map.Entry<String, BlipThread> entry : tempThreads.entrySet()) {
      BlipThread thread = entry.getValue();
      threads.put(entry.getKey(),
          new BlipThread(thread.getId(), thread.getLocation(), thread.getBlipIds(), blips));
    }

    // Deserialize blips.
    @SuppressWarnings("unchecked")
    Map<String, BlipData> blipDatas =
        (Map<String, BlipData>) response.get(ParamsProperty.BLIPS);
    for (Map.Entry<String, BlipData> entry : blipDatas.entrySet()) {
      blips.put(entry.getKey(), Blip.deserialize(opQueue, wavelet, entry.getValue()));
    }

    return wavelet;
  }

  /**
   * Retrieves wavelets ids of the specified wave.
   *
   * @param waveId the id of the wave.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @return list of wavelets ids.
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public List<WaveletId> retrieveWaveletIds(WaveId waveId, String rpcServerUrl)
      throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.retrieveWaveletIds(waveId);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    @SuppressWarnings("unchecked")
    List<WaveletId> list = (List<WaveletId>)response.get(ParamsProperty.WAVELET_IDS);
    return list;
  }

  /**
   * Exports wavelet deltas history.
   *
   * @param waveId the id of the wave to export.
   * @param waveletId the id of the wavelet to export.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @return WaveletSnapshot in Json.
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public String exportRawSnapshot(WaveId waveId, WaveletId waveletId, String rpcServerUrl) throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.exportSnapshot(waveId, waveletId);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    return (String)response.get(ParamsProperty.RAW_SNAPSHOT);
  }

  /**
   * Exports wavelet deltas history.
   *
   * @param waveId the id of the wave to export.
   * @param waveletId the id of the wavelet to export.
   * @param fromVersion start ProtocolHashedVersion.
   * @param toVersion end ProtocolHashedVersion.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @return history of deltas.
   * @throws IOException if there is a problem fetching the deltas.
   */
  public void exportRawDeltas(WaveId waveId, WaveletId waveletId,
      byte[] fromVersion, byte[] toVersion, String rpcServerUrl,
      RawDeltasListener listener) throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.exportRawDeltas(waveId, waveletId, fromVersion, toVersion);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    @SuppressWarnings("unchecked")
    List<byte[]> rawHistory = (List<byte[]>)response.get(ParamsProperty.RAW_DELTAS);
    byte[] rawTargetVersion = (byte[])response.get(ParamsProperty.TARGET_VERSION);
    listener.onSuccess(rawHistory, rawTargetVersion);
  }

  /**
   * Exports attachment.
   *
   * @param attachmentId the id of attachment.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @return the data of attachment.
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public RawAttachmentData exportAttachment(AttachmentId attachmentId,
      String rpcServerUrl) throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.exportAttachment(attachmentId);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    return (RawAttachmentData)response.get(ParamsProperty.ATTACHMENT_DATA);
  }

  /**
   * Imports deltas to wavelet.
   *
   * @param waveId the id of the wave to import.
   * @param waveletId the id of the wavelet to import.
   * @param history the history of deltas.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @return the version from which importing started.
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public long importRawDeltas(WaveId waveId, WaveletId waveletId,
      List<byte[]> history, String rpcServerUrl) throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.importRawDeltas(waveId, waveletId, history);

    Map<ParamsProperty, Object> response = makeSingleOperationRpc(opQueue, rpcServerUrl);
    return (Long)response.get(ParamsProperty.IMPORTED_FROM_VERSION);
  }

  /**
   * Imports attachment.
   *
   * @param waveId the id of the wave to import.
   * @param waveletId the id of the wavelet to import.
   * @param attachmentId the id of attachment.
   * @param attachmentData the data of attachment.
   * @param rpcServerUrl the URL of the JSON-RPC request handler.
   * @throws IOException if there is a problem fetching the wavelet.
   */
  public void importAttachment(WaveId waveId, WaveletId waveletId, AttachmentId attachmentId,
      RawAttachmentData attachmentData, String rpcServerUrl) throws IOException {
    OperationQueue opQueue = new OperationQueue();
    opQueue.importAttachment(waveId, waveletId, attachmentId, attachmentData);

    makeSingleOperationRpc(opQueue, rpcServerUrl);
  }

  /**
   * @return the map of consumer key and secret.
   */
  protected Map<String, ConsumerData> getConsumerDataMap() {
    return consumerDataMap;
  }

  /**
   * @return {@code true} if this service object contains a consumer key and
   *         secret for the given RPC server URL.
   */
  protected boolean hasConsumerData(String rpcServerUrl) {
    return consumerDataMap.containsKey(rpcServerUrl);
  }

  /**
   * Submits the given operation.
   *
   * @param opQueue the operation queue with operation to be submitted.
   * @param rpcServerUrl the active gateway to send the operations to.
   * @return the data of response.
   * @throws IllegalStateException if this method is called prior to setting the
   *         proper consumer key, secret, and handler URL.
   * @throws IOException if there is a problem submitting the operations, or error response.
   */
  private Map<ParamsProperty, Object> makeSingleOperationRpc(OperationQueue opQueue, String rpcServerUrl)
      throws IOException {
    JsonRpcResponse response = makeRpc(opQueue, rpcServerUrl).get(0);
    if (response.isError()) {
      throw new IOException(response.getErrorMessage());
    }
    return response.getData();
  }

  /**
   * Submits the given operations.
   *
   * @param opQueue the operation queue to be submitted.
   * @param rpcServerUrl the active gateway to send the operations to.
   * @return a list of {@link JsonRpcResponse} that represents the responses
   *         from the server for all operations that were submitted.
   *
   * @throws IllegalStateException if this method is called prior to setting the
   *         proper consumer key, secret, and handler URL.
   * @throws IOException if there is a problem submitting the operations.
   */
  private List<JsonRpcResponse> makeRpc(OperationQueue opQueue, String rpcServerUrl)
      throws IOException {
    if (rpcServerUrl == null) {
      throw new IllegalStateException("RPC Server URL is not set up.");
    }

    ConsumerData consumerDataObj = consumerDataMap.get(rpcServerUrl);
    if (consumerDataObj == null) {
      throw new IllegalStateException("Consumer key, consumer secret, and  JSON-RPC server URL "
          + "have to be set first, by calling AbstractRobot.setupOAuth(), before invoking "
          + "AbstractRobot.submit().");
    }

    opQueue.notifyRobotInformation(PROTOCOL_VERSION, version);
    String json =
        SERIALIZER.toJson(opQueue.getPendingOperations(), GsonFactory.OPERATION_REQUEST_LIST_TYPE);

    try {
      InputStream bodyStream;
      InputStream responseStream;
      try {
        bodyStream = new ByteArrayInputStream(json.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e);
      }
      if (!consumerDataObj.isUserAuthenticated()) {
        String url = createOAuthUrlString(
            json, consumerDataObj.getRpcServerUrl(), consumerDataObj.getAccessor());
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("JSON request to be sent: " + json);
        }
        HttpMessage request = new HttpMessage("POST", new URL(url), bodyStream);
        request.headers.add(
            new SimpleEntry<String, String>(HttpMessage.CONTENT_TYPE, JSON_MIME_TYPE));
        request.headers.add(new SimpleEntry<String, String>("oauth_version", "1.0"));
        responseStream =
            httpFetcher.execute(request, Collections.<String, Object>emptyMap()).getBody();
      } else {
        OAuthAccessor accessor = consumerDataObj.getAccessor();
        OAuthMessage message = accessor.newRequestMessage("POST", rpcServerUrl, null, bodyStream);
        message.getHeaders().add(
            new SimpleEntry<String, String>(HttpMessage.CONTENT_TYPE, JSON_MIME_TYPE));
        message.getHeaders().add(new SimpleEntry<String, String>("oauth_version", "1.0"));
        OAuthClient client = new OAuthClient(httpFetcher);
        responseStream = client.invoke(message, net.oauth.ParameterStyle.BODY).getBodyAsStream();
      }

      String responseString = HttpFetcher.readInputStream(responseStream);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Response returned: " + responseString);
      }

      List<JsonRpcResponse> responses = null;
      if (responseString.startsWith("[")) {
        responses = SERIALIZER.fromJson(responseString, GsonFactory.JSON_RPC_RESPONSE_LIST_TYPE);
      } else {
        responses = new ArrayList<JsonRpcResponse>(1);
        responses.add(SERIALIZER.fromJson(responseString, JsonRpcResponse.class));
      }
      responses.remove(0); // removes response to the notify operation.
      return responses;
    } catch (OAuthException e) {
      LOG.warning("OAuthException when constructing the OAuth parameters: " + e);
      throw new IOException(e);
    } catch (URISyntaxException e) {
      LOG.warning("URISyntaxException when constructing the OAuth parameters: " + e);
      throw new IOException(e);
    }
  }

  /**
   * Creates a URL that contains the necessary OAuth query parameters for the
   * given JSON string.
   *
   * The required OAuth parameters are:
   * <ul>
   * <li>oauth_body_hash</li>
   * <li>oauth_consumer_key</li>
   * <li>oauth_signature_method</li>
   * <li>oauth_timestamp</li>
   * <li>oauth_nonce</li>
   * <li>oauth_version</li>
   * <li>oauth_signature</li>
   * </ul>
   *
   * @param jsonBody the JSON string to construct the URL from.
   * @param rpcServerUrl the URL of the handler that services the JSON-RPC
   *        request.
   * @param accessor the OAuth accessor used to create the signed string.
   * @return a URL for the given JSON string, and the required OAuth parameters.
   */
  public static String createOAuthUrlString(
      String jsonBody, String rpcServerUrl, OAuthAccessor accessor)
      throws IOException, URISyntaxException, OAuthException {
    OAuthMessage message =
        new OAuthMessage(POST, rpcServerUrl, Collections.<SimpleEntry<String, String>>emptyList());

    // Compute the hash of the body.
    byte[] rawBody = jsonBody.getBytes(UTF_8);
    byte[] hash = DigestUtils.sha(rawBody);
    byte[] encodedHash = Base64.encodeBase64(hash);
    message.addParameter(OAUTH_BODY_HASH, new String(encodedHash, UTF_8));

    // Add other parameters.

    message.addRequiredParameters(accessor);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Signature base string: " + OAuthSignatureMethod.getBaseString(message));
    }

    // Construct the resulting URL.
    StringBuilder sb = new StringBuilder(rpcServerUrl);
    char connector = '?';
    for (Map.Entry<String, String> p : message.getParameters()) {
      if (!p.getKey().equals(jsonBody)) {
        sb.append(connector);
        sb.append(URLEncoder.encode(p.getKey(), UTF_8));
        sb.append('=');
        sb.append(URLEncoder.encode(p.getValue(), UTF_8));
        connector = '&';
      }
    }
    return sb.toString();
  }

  /**
   * Sets the fetch timeout to a specified timeout, in milliseconds.
   * A timeout of zero is interpreted as an infinite timeout.
   *
   * @param fetchTimeout
   */
  public void setFetchFimeout(int fetchTimeout) {
    httpFetcher.setTimeout(fetchTimeout);
  }
}
