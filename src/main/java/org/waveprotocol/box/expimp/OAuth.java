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
package org.waveprotocol.box.expimp;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

import org.apache.commons.codec.binary.Base64;
import com.google.wave.api.WaveService;

/**
 * OAuth authorization through Robot and Data API.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class OAuth {
  private static final String REQUEST_URL_POSTFIX = "/robot/dataapi/oauth/OAuthGetRequestToken";
  private static final String AUTH_URL_POSTFIX = "/robot/dataapi/oauth/OAuthAuthorizeToken";
  private static final String ACCESS_URL_POSTFIX = "/robot/dataapi/oauth/OAuthGetAccessToken";
  private static final String GET_ALL_TOKENS_URL_POSTFIX = "/robot/dataapi/oauth/OAuthGetAllTokens";

  private static final String DATA_API_RPC_URL_POSTFIX = "/robot/dataapi/rpc";
  private static final String ROBOT_RPC_URL_POSTFIX = "/robot/rpc";

  private static final String THREE_LEGGED_API_CONSUMER_KEY = "anonymous";
  private static final String THREE_LEGGED_API_CONSUMER_SECRET = "anonymous";

  private final String serverUrl;

  public OAuth(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  /**
   * Performs 2-legged OAuth authorization through Robot API.
   *
   * @param service wave service.
   * @param consumerKey robot consumer key.
   * @param consumerSecret robot consumer secret.
   */
  public String twoLeggedOAuth(WaveService service, String consumerKey, String consumerSecret) {
    String rpcServerUrl = serverUrl + ROBOT_RPC_URL_POSTFIX;
    service.setupOAuth(consumerKey, consumerSecret, rpcServerUrl);
    return rpcServerUrl;
  }

  /**
   * Performs 3-legged OAuth authorization through Data API.
   *
   * @param service wave service.
   */
  public String threeLeggedOAuth(WaveService service) throws IOException {
    Console.println("Paste this URL in your browser:\n" + serverUrl + GET_ALL_TOKENS_URL_POSTFIX);
    Console.println("Type the code you received here: ");
    String authorizationCode = new String(
        Base64.decodeBase64(Console.readLine().getBytes("UTF-8")), "UTF-8");

    StringTokenizer st = new StringTokenizer(authorizationCode);

    String requestToken = st.nextToken();
    String accessToken = st.nextToken();
    String tokenSecret = st.nextToken();

    String requestUrl = serverUrl + REQUEST_URL_POSTFIX;
    String authUrl = serverUrl + AUTH_URL_POSTFIX;
    String accessUrl = serverUrl + ACCESS_URL_POSTFIX;

    OAuthServiceProvider provider = new OAuthServiceProvider(requestUrl
        + "?scope=" + URLEncoder.encode("", "utf-8"), authUrl, accessUrl);
    OAuthConsumer consumer = new OAuthConsumer("", THREE_LEGGED_API_CONSUMER_KEY,
        THREE_LEGGED_API_CONSUMER_SECRET, provider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    accessor.requestToken = requestToken;
    accessor.accessToken = accessToken;
    accessor.tokenSecret = tokenSecret;

    String rpcServerUrl = serverUrl + DATA_API_RPC_URL_POSTFIX;
    service.setupOAuth(accessor, rpcServerUrl);
    return rpcServerUrl;
  }
}