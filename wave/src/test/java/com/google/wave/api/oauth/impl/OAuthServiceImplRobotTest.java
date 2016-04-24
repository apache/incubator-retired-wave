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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.wave.api.oauth.LoginFormHandler;

import junit.framework.TestCase;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Unit tests for {@link OAuthServiceImpl}.
 */
public class OAuthServiceImplRobotTest extends TestCase {

  /** Mock datastore key string to be used in these tests. */
  public static final String USER_RECORD_KEY = "JUNIT_TEST_KEY";

  /** Mock request token to be used in these tests. */
  public static final String REQUEST_TOKEN_STRING = "JUNIT_TEST_REQUEST_TOKEN";

  /** Request token Url. */
  public static final String REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";

  /** Access token Url. */
  public static final String ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";

  /** User authorization Url. */
  public static final String AUTHORIZE_URL = "http://twitter.com/oauth/authenticate";

  /** URL that the service provider will redirect to after the user logs in. */
  private static final String CALLBACK_URL = "http://oauth-robot-test.appspot.com/callback";

  /**
   * The consumer secret used to authenticate using OAuth. Unique to this
   * registered app.
   */
  public static final String CONSUMER_SECRET = "wWHuuhRa8fhfBRTG3eQjPYVqXW77OV0O2TxPR01c";

  /**
   * The consumer key used to authenticate using OAuth. Unique to this
   * registered app.
   */
  public static final String CONSUMER_KEY = "LmMi37TI9VPb7ERkUHUmzw";

  /** Url for requesting a Twitter user's timeline. */
  public static final String FRIENDS_TIMELINE_URL =
      "http://twitter.com/statuses/friends_timeline.json";

  /**
   * OAuth client that performs OAuth exchanges for secure access to protected
   * resources.
   */
  private OAuthServiceImpl oauthService;

  private OAuthAccessor buildAccessor(String consumerKey,
    String consumerSecret, String requestTokenUrl, String authorizeUrl,
    String callbackUrl, String accessTokenUrl) {
    OAuthServiceProvider provider =
      new OAuthServiceProvider(requestTokenUrl, authorizeUrl, accessTokenUrl);
    OAuthConsumer consumer =
      new OAuthConsumer(callbackUrl, consumerKey, consumerSecret, provider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    return accessor;
  }

  /**
   * Tests the case when the user has not started the authorization process (no
   * request token).
   */
  public final void testCheckAuthorizationNoRequestToken() {
    // Setup.
    LoginFormHandler loginForm = mock(LoginFormHandler.class);
    OAuthClient client = mock(OAuthClient.class);
    PersistenceManager pm = mock(PersistenceManager.class);
    PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);

    OAuthAccessor accessor = buildAccessor(CONSUMER_KEY, CONSUMER_SECRET,
        REQUEST_TOKEN_URL, AUTHORIZE_URL, CALLBACK_URL, ACCESS_TOKEN_URL);
    accessor.requestToken = REQUEST_TOKEN_STRING;
    oauthService = new OAuthServiceImpl(accessor, client, pmf,
        USER_RECORD_KEY);
    OAuthUser userWithRequestToken = new OAuthUser(USER_RECORD_KEY, REQUEST_TOKEN_STRING);

    // Expectations.
    when(pmf.getPersistenceManager()).thenReturn(pm);
    when(pm.getObjectById(OAuthUser.class, USER_RECORD_KEY)).thenReturn(null, userWithRequestToken,
        userWithRequestToken);

    assertFalse(oauthService.checkAuthorization(null, loginForm));

    String authUrl = userWithRequestToken.getAuthUrl();
    try {
      new URL(authUrl);
    } catch (MalformedURLException e) {
      fail("Malformed authUrl");
    }

    assertTrue(Pattern.matches(".+(oauth_token){1}.+", authUrl));
    assertTrue(Pattern.matches(".+(oauth_callback){1}.+", authUrl));
  }
}
