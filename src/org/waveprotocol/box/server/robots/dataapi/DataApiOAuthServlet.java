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

package org.waveprotocol.box.server.robots.dataapi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.server.HttpRequestMessage;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.gxp.OAuthAuthorizeTokenPage;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.box.server.gxp.OAuthAuthorizationCodePage;
import org.waveprotocol.wave.model.util.CharBase64;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet responsible for the 3-legged OAuth dance required for the Data api.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings("serial")
@Singleton
public class DataApiOAuthServlet extends HttpServlet {

  public static final String DATA_API_OAUTH_PATH = "/robot/dataapi/oauth";
  private static final Log LOG = Log.get(DataApiOAuthServlet.class);
  private static final String ANONYMOUS_TOKEN = "anonymous";
  private static final String ANONYMOUS_TOKEN_SECRET = "anonymous";
  private static final String HTML_CONTENT_TYPE = "text/html";
  private static final String PLAIN_CONTENT_TYPE = "text/plain";
  private static final int TOKEN_LENGTH = 8;
  private static final int XSRF_TOKEN_TIMEOUT_HOURS = 12;

  private final String requestTokenPath;
  private final String authorizeTokenPath;
  private final String accessTokenPath;
  private final String allTokensPath;
  private final OAuthServiceProvider serviceProvider;
  private final OAuthValidator validator;
  private final DataApiTokenContainer tokenContainer;
  private final SessionManager sessionManager;
  private final TokenGenerator tokenGenerator;
  // TODO(ljvderijk): We should refactor this and use it for our other pages.
  private final ConcurrentMap<ParticipantId, String> xsrfTokens;

  @Inject
  public DataApiOAuthServlet(@Named("request_token_path") String requestTokenPath,
      @Named("authorize_token_path") String authorizeTokenPath,
      @Named("access_token_path") String accessTokenPath,
      @Named("all_tokens_path") String allTokensPath,
      OAuthServiceProvider serviceProvider,
      OAuthValidator validator, DataApiTokenContainer tokenContainer,
      SessionManager sessionManager, TokenGenerator tokenGenerator) {
    this.requestTokenPath = requestTokenPath;
    this.authorizeTokenPath = authorizeTokenPath;
    this.accessTokenPath = accessTokenPath;
    this.allTokensPath = allTokensPath;
    this.serviceProvider = serviceProvider;
    this.validator = validator;
    this.tokenContainer = tokenContainer;
    this.sessionManager = sessionManager;
    this.tokenGenerator = tokenGenerator;
    this.xsrfTokens =
        CacheBuilder.newBuilder()
          .expireAfterWrite(XSRF_TOKEN_TIMEOUT_HOURS, TimeUnit.HOURS)
          .<ParticipantId, String>build().asMap();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    routeRequest(req, resp);
  }


  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    routeRequest(req, resp);
  }

  /** Routes all requests to the appropriate handler */
  private void routeRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (pathInfo.equals(requestTokenPath)) {
      doRequestToken(req, resp);
    } else if (pathInfo.equals(authorizeTokenPath)) {
      doAuthorizeToken(req, resp);
    } else if (pathInfo.equals(accessTokenPath)) {
      doExchangeToken(req, resp);
    } else if (pathInfo.equals(allTokensPath)) {
      doAllTokens(req, resp);
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Handles the request to get a new unauthorized request token.
   */
  private void doRequestToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());

    // Anyone can generate a request token.
    OAuthConsumer consumer =
        new OAuthConsumer("", ANONYMOUS_TOKEN, ANONYMOUS_TOKEN_SECRET, serviceProvider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    try {
      validator.validateMessage(message, accessor);
    } catch (OAuthException e) {
      LOG.info("The message does not conform to OAuth", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (URISyntaxException e) {
      LOG.info("The message URL is invalid", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    accessor = tokenContainer.generateRequestToken(consumer);

    resp.setContentType(OAuth.FORM_ENCODED);
    ServletOutputStream out = resp.getOutputStream();
    OAuth.formEncode(OAuth.newList(
        OAuth.OAUTH_TOKEN, accessor.requestToken,
        OAuth.OAUTH_TOKEN_SECRET, accessor.tokenSecret,
        OAuth.OAUTH_CALLBACK_CONFIRMED, "true"),
        out);
    out.close();
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Handles the request to authorize a token. Checks if the user is logged in,
   * if not the user is redirected to the login page.
   *
   * <p>
   * If it is a GET request the user will be asked whether permission should be
   * given. For a POST request we will handle the user's decision.
   */
  private void doAuthorizeToken(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // Check if the OAuth parameters are present, even if we don't use them
    // during a GET request.
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());
    try {
      message.requireParameters(OAuth.OAUTH_CALLBACK, OAuth.OAUTH_TOKEN);
    } catch (OAuthProblemException e) {
      LOG.info("Parameter absent", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }

    // Check if the user is logged in, else redirect to login.
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
    if (user == null) {
      resp.sendRedirect(sessionManager.getLoginUrl(
          DATA_API_OAUTH_PATH + authorizeTokenPath + "?" + req.getQueryString()));
      return;
    }

    // Check if the request token is valid, note that this doesn't hold after
    // the call to the container since the token might time out.
    try {
      tokenContainer.getRequestTokenAccessor(message.getToken());
    } catch (OAuthProblemException e) {
      LOG.info("Trying to load a non existing token for authorization", e);
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }

    if (req.getMethod().equals("GET")) {
      doAuthorizeTokenGet(req, resp, user);
    } else if (req.getMethod().equals("POST")) {
      doAuthorizeTokenPost(req, resp, user, message);
    } else {
      throw new IllegalStateException(
          "This method shouldn't be called outside GET or POST requests");
    }
  }

  /**
   * Handles the GET request to authorize a token by displaying the page asking
   * for user's permission.
   *
   * @param req {@link HttpServletRequest} received.
   * @param resp {@link HttpServletResponse} to write the response in.
   * @param user User who wants to authorize the token.
   */
  private void doAuthorizeTokenGet(
      HttpServletRequest req, HttpServletResponse resp, ParticipantId user) throws IOException {
    Preconditions.checkNotNull(user, "User must be supplied");
    // Ask the user for permission.
    OAuthAuthorizeTokenPage.write(resp.getWriter(), new GxpContext(req.getLocale()),
        user.getAddress(), getOrGenerateXsrfToken(user));
    resp.setContentType(HTML_CONTENT_TYPE);
    resp.setStatus(HttpServletResponse.SC_OK);
    return;
  }

  /**
   * Method that handles the POST request for the authorize token page. The
   * token will be rejected if the user pressed the cancel button. Otherwise the
   * token will be authorized.
   *
   * @param req {@link HttpServletRequest} received.
   * @param resp {@link HttpServletResponse} to write the response in.
   * @param user user who is authorizing the token.
   * @param message the {@link OAuthMessage} present in the request.
   */
  private void doAuthorizeTokenPost(
      HttpServletRequest req, HttpServletResponse resp, ParticipantId user, OAuthMessage message)
      throws IOException {
    Preconditions.checkNotNull(user, "User must be supplied");

    // Check the XSRF token.
    if (Strings.isNullOrEmpty(req.getParameter("token"))
        || !req.getParameter("token").equals(xsrfTokens.get(user))) {
      LOG.warning(
          "Request without a valid  xsrf token received from " + req.getRemoteAddr() + " for user "
              + user);
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid XSRF token");
      return;
    }
    // Check whether the user agreed to give access.
    if (req.getParameter("cancel") != null) {
      try {
        tokenContainer.rejectRequestToken(message.getToken());
      } catch (OAuthProblemException e) {
        LOG.info("Rejecting a request token failed", e);
        resp.sendError(e.getHttpStatusCode(), e.getMessage());
        return;
      }
      resp.setContentType(PLAIN_CONTENT_TYPE);
      resp.getWriter().append("No access granted, you can now close this page.");
      resp.setStatus(HttpServletResponse.SC_OK);
      return;
    } else if (req.getParameter("agree") == null) {
      // User did not agree nor disagree, bad request.
      LOG.warning(
          "Bad request when authorzing a token from " + req.getRemoteAddr() + " for user " + user);
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Authorize the token.
    OAuthAccessor accessor;
    try {
      accessor = tokenContainer.authorizeRequestToken(message.getToken(), user);
    } catch (OAuthProblemException e) {
      LOG.info("Authorizing a request token failed", e);
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }

    // Create the callback url and send the user to it
    String callback = message.getParameter(OAuth.OAUTH_CALLBACK);
    callback = OAuth.addParameters(callback, OAuth.OAUTH_TOKEN, accessor.requestToken);
    resp.sendRedirect(callback);
  }

  /**
   * Exchanges an authorized request token with an access token.
   */
  private void doExchangeToken(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());

    String requestToken = message.getToken();
    OAuthAccessor accessor;
    try {
      accessor = tokenContainer.getRequestTokenAccessor(requestToken);
    } catch (OAuthProblemException e) {
      LOG.info("Request token unknown", e);
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }

    try {
      validator.validateMessage(message, accessor);
    } catch (OAuthException e) {
      LOG.info("The message does not conform to OAuth", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (URISyntaxException e) {
      LOG.info("The message URL is invalid", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    OAuthAccessor authorizedAccessor;
    try {
      authorizedAccessor = tokenContainer.generateAccessToken(accessor.requestToken);
    } catch (OAuthProblemException e) {
      LOG.info("Request token unknown", e);
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }

    resp.setContentType(OAuth.FORM_ENCODED);
    ServletOutputStream out = resp.getOutputStream();
    OAuth.formEncode(OAuth.newList(
        OAuth.OAUTH_TOKEN, authorizedAccessor.accessToken,
        OAuth.OAUTH_TOKEN_SECRET, authorizedAccessor.tokenSecret,
        OAuth.OAUTH_CALLBACK_CONFIRMED, "true"),
        out);
    out.close();
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Perform full Auth dance and print tokens.
   */
  private void doAllTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());
    String requestToken = message.getToken();
    if (requestToken == null) {
      OAuthConsumer consumer =
          new OAuthConsumer("", ANONYMOUS_TOKEN, ANONYMOUS_TOKEN_SECRET, serviceProvider);
      OAuthAccessor accessor = tokenContainer.generateRequestToken(consumer);
      String url = accessor.consumer.serviceProvider.userAuthorizationURL
          + "?oauth_token=" + accessor.requestToken + "&oauth_callback="
          + req.getRequestURL().toString() + "&hd=default";
      resp.sendRedirect(url);
    } else {
      OAuthAccessor accessor;
      try {
        accessor = tokenContainer.getRequestTokenAccessor(requestToken);
      } catch (OAuthProblemException e) {
        LOG.info("Request token unknown", e);
        resp.sendError(e.getHttpStatusCode(), e.getMessage());
        return;
      }
      OAuthAccessor authorizedAccessor;
      try {
        authorizedAccessor = tokenContainer.generateAccessToken(accessor.requestToken);
      } catch (OAuthProblemException e) {
        LOG.info("Request token unknown", e);
        resp.sendError(e.getHttpStatusCode(), e.getMessage());
        return;
      }
      String authorizationCode = authorizedAccessor.requestToken + " "
          + authorizedAccessor.accessToken + " " + authorizedAccessor.tokenSecret;
      String base64AuthCode = CharBase64.encode(authorizationCode.getBytes());
      OAuthAuthorizationCodePage.write(resp.getWriter(), new GxpContext(req.getLocale()),
          base64AuthCode);
      resp.setContentType(HTML_CONTENT_TYPE);
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }

  /**
   * Gets or generates an XSRF token for the given user.
   *
   * <p>
   * XSRF is an attack where, for instance, another web site makes the user's
   * browser do a POST request to authorize a request token. Because the user's
   * browser might contain an valid login cookie for "Wave in a Box" this would
   * succeed. By adding a hidden field on each form with a random token and
   * checking the presence of this token the attack can be countered because the
   * attacker does not know the token.
   *
   * @param user the user to generate a token for.
   */
  @VisibleForTesting
  String getOrGenerateXsrfToken(ParticipantId user) {
    String token = tokenGenerator.generateToken(TOKEN_LENGTH);
    String previousToken = xsrfTokens.putIfAbsent(user, token);
    if (previousToken != null) {
      token = previousToken;
    }
    return token;
  }
}
