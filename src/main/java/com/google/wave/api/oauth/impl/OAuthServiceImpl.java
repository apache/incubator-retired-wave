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

package com.google.wave.api.oauth.impl;

import com.google.wave.api.Wavelet;
import com.google.wave.api.oauth.LoginFormHandler;
import com.google.wave.api.oauth.OAuthService;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Implements the OAuthService interface. {@link OAuthService}
 * 
 * @author kimwhite@google.com (Kimberly White)
 * @author elizabethford@google.com (Elizabeth Ford)
 */
public class OAuthServiceImpl implements OAuthService {

  /** Writes application logs. */
  private static final Logger LOG = Logger.getLogger(OAuthServiceImpl.class.getName());

  /** Key for OAuth request token query parameter. */
  private static final String TOKEN_KEY = "oauth_token";

  /** Key for OAuth callback url query parameter. */
  private static final String CALLBACK_KEY = "oauth_callback";

  /** Key to force user to enter credentials. */
  private static final String FORCE_LOGIN_KEY = "force_login";

  /** Key for HTTP GET method. */
  private static final String GET = "GET";

  /** Key for HTTP POST method. */
  private static final String POST = "POST";

  /** Key for Datastore object. Consists of wave creator id and wave id. */
  private final String userRecordKey;

  /** OpenAuth client used to negotiate request/access tokens. */
  private final OAuthClient oauthClient;

  /** OpenAuth accessor that stores request/access/secret tokens. */
  private final OAuthAccessor accessor;

  /** Persistence Manager Factory to retrieve and store Datastore objects. */
  private final PersistenceManagerFactory pmf;

  /**
   * Factory method. Initializes OAuthServiceProvider with necessary tokens and
   * urls.
   * 
   * @param userRecordKey key consisting of user id and wave id.
   * @param consumerKey service provider OAuth consumer key.
   * @param consumerSecret service provider OAuth consumer secret.
   * @param requestTokenUrl url to get service provider request token.
   * @param authorizeUrl url to service provider authorize page.
   * @param callbackUrl url to callback page.
   * @param accessTokenUrl url to get service provider access token.
   * @return OAuthService instance.
   */
  public static OAuthService newInstance(String userRecordKey, String consumerKey,
      String consumerSecret, String requestTokenUrl, String authorizeUrl, String callbackUrl,
      String accessTokenUrl) {
    OAuthServiceProvider provider =
        new OAuthServiceProvider(requestTokenUrl, authorizeUrl, accessTokenUrl);
    OAuthConsumer consumer = new OAuthConsumer(callbackUrl, consumerKey, consumerSecret, provider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    OAuthClient client = new OAuthClient(new OpenSocialHttpClient());
    PersistenceManagerFactory pmf = SingletonPersistenceManagerFactory.get();
    return new OAuthServiceImpl(accessor, client, pmf, userRecordKey);
  }

  /**
   * Initializes necessary OAuthClient and accessor objects for OAuth handling.
   * 
   * @param accessor Used to store tokesn for OAuth authorization.
   * @param client Handles OAuth authorization.
   * @param pmf Manages datastore fetching and storing.
   * @param recordKey User id for datastore object.
   */
  OAuthServiceImpl(OAuthAccessor accessor, OAuthClient client,
      PersistenceManagerFactory pmf, String recordKey) {
    this.userRecordKey = recordKey;
    this.pmf = pmf;
    this.accessor = accessor;
    this.oauthClient = client;
  }

  @Override
  public boolean checkAuthorization(Wavelet wavelet, LoginFormHandler loginForm) {

    OAuthUser user = retrieveUserProfile();

    // Return true if the user already has an access token.
    if (user != null && user.getAccessToken() != null) {
      return true;
    }

    // If the user doesn't have an access token but already has a request token,
    // exchange the tokens.
    if (user != null && user.getRequestToken() != null) {
      String accessToken = exchangeTokens(user);
      if (accessToken != null) {
        // Yay, we're authorized.
        return true;
      }
    }

    // Need to login.
    String requestToken = getAndStoreRequestToken();
    LOG.info("Request token: " + requestToken);
    buildAuthUrl();
    loginForm.renderLogin(userRecordKey, wavelet);
    return false;
  }

  @Override
  public boolean hasAuthorization() {
    OAuthUser user = retrieveUserProfile();
    return (user != null && user.getAccessToken() != null);
  }

  /**
   * Applies for a request token from the service provider.
   * 
   * @return the request token.
   */
  private String getAndStoreRequestToken() {
    // Get the request token.
    try {
      oauthClient.getRequestToken(accessor);
    } catch (IOException e) {
      LOG.severe("Could not reach service provider to get request token: " + e);
    } catch (OAuthException e) {
      LOG.severe("Unable to fetch request token. Authentication error: " + e);
    } catch (URISyntaxException e) {
      LOG.severe("Unable to fetch request token. Invalid url: " + e);
    }

    // Store request token in Datastore via a tokenData object.
    String requestToken = accessor.requestToken;
    storeUserProfile(new OAuthUser(userRecordKey, requestToken));

    return accessor.requestToken;
  }

  /**
   * Builds the url to authenticate the request token with necessary query
   * parameters and stores in Datastore.
   */
  private void buildAuthUrl() {
    OAuthUser userProfile = retrieveUserProfile();
    OpenSocialUrl url = new OpenSocialUrl(accessor.consumer.serviceProvider.userAuthorizationURL);
    url.addQueryStringParameter(TOKEN_KEY, userProfile.getRequestToken());
    url.addQueryStringParameter(CALLBACK_KEY, accessor.consumer.callbackURL);
    url.addQueryStringParameter(FORCE_LOGIN_KEY, "true");
    userProfile.setAuthUrl(url.toString());
    storeUserProfile(userProfile);
  }

  /**
   * Exchanges a signed request token for an access token from the service
   * provider and stores it in the user profile if access token is successfully
   * acquired.
   * 
   * @return String of the access token, might return null if failed.
   */
  private String exchangeTokens(OAuthUser userProfile) {
    // Re-initialize an accessor with the request token and secret token.
    // Request token needs to be in access token field for exchange to work.
    accessor.accessToken = userProfile.getRequestToken();
    accessor.tokenSecret = accessor.consumer.consumerSecret;

    // Perform the exchange.
    String accessToken = null;
    String tokenSecret = null;
    try {
      OAuthMessage message =
          oauthClient.invoke(accessor, GET, accessor.consumer.serviceProvider.accessTokenURL, null);
      accessToken = message.getToken();
      tokenSecret = message.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    } catch (IOException e) {
      LOG.warning("Failed to retrieve access token: " + e.getMessage());
    } catch (OAuthException e) {
      LOG.warning("Failed to retrieve access token: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    if (accessToken != null) {
      // Store the access token in the user profile object and put profile back
      // in the datastore.
      userProfile.setAccessToken(accessToken);
      userProfile.setTokenSecret(tokenSecret);
      storeUserProfile(userProfile);
    }
    return accessToken;
  }

  @Override
  public String post(String url, Map<String, String> parameters) throws OAuthServiceException {
    return requestResources(url, POST, parameters);
  }

  @Override
  public String get(String url, Map<String, String> parameters) throws OAuthServiceException {
    return requestResources(url, GET, parameters);
  }

  /**
   * Get secure resources from the service provider.
   * 
   * @param url service provider resource url.
   * @param method Http method.
   * @param parameters service provider parameters.
   * @return String of the service provider response message.
   * @throws OAuthServiceException the HTTP response code was not OK
   */
  private String requestResources(String url, String method, Map<String, String> parameters)
      throws OAuthServiceException {

    // Convert parameters to OAuth parameters.
    Collection<OAuth.Parameter> queryParameters = new ArrayList<OAuth.Parameter>();
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      queryParameters.add(new OAuth.Parameter(parameter.getKey(), parameter.getValue()));
    }

    // Set the accessor's access token with the access token in Datastore.
    OAuthUser userProfile = retrieveUserProfile();
    accessor.accessToken = userProfile.getAccessToken();
    accessor.tokenSecret = userProfile.getTokenSecret();

    // Send request and receive response from service provider.
    String messageString = "";
    OAuthMessage message;
    try {
      message = oauthClient.invoke(accessor, method, url, queryParameters);
      messageString = message.readBodyAsString();
    } catch (IOException e) {
      LOG.severe("Response message has no body: " + e);
      throw new OAuthServiceException(e);
    } catch (URISyntaxException e) {
      LOG.severe("Unable to fetch resources. Invalid url: " + e);
      throw new OAuthServiceException(e);
    } catch (OAuthException e){
      throw new OAuthServiceException(e);
    }
    return messageString;
  }

  /**
   * Stores user-specific oauth token information in Datastore.
   * 
   * @param user profile consisting of user's request token, access token, and
   *        consumer secret.
   */
  private void storeUserProfile(OAuthUser user) {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      pm.makePersistent(user);
    } finally {
      pm.close();
    }
  }

  /**
   * Retrieves user's oauth information from Datastore.
   * 
   * @return the user profile (or null if not found).
   */
  private OAuthUser retrieveUserProfile() {
    PersistenceManager pm = pmf.getPersistenceManager();
    OAuthUser userProfile = null;
    try {
      userProfile = pm.getObjectById(OAuthUser.class, userRecordKey);
    } catch (JDOObjectNotFoundException e) {
      LOG.info("Datastore object not yet initialized with key: " + userRecordKey);
    } finally {
      pm.close();
    }
    return userProfile;
  }
}
