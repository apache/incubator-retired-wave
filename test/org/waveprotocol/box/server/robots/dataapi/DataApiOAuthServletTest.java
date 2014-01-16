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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;

import junit.framework.TestCase;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.OAuth.Parameter;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Unit tests for the {@link DataApiOAuthServlet}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DataApiOAuthServletTest extends TestCase {

  private static class ServletOutputStreamStub extends ServletOutputStream {

    private final StringWriter stringWriter;
    private boolean closed;

    public ServletOutputStreamStub() {
      stringWriter = new StringWriter();
    }

    @Override
    public void write(int b) {
      stringWriter.write((char) b);
    }

    @Override
    public String toString() {
      return stringWriter.toString();
    }

    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() throws IOException {
      super.close();
      stringWriter.close();
      closed = true;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener wl) {
    }
  }

  private static final String FAKE_TOKEN = "fake_token";
  private static final String REQUEST_TOKEN_PATH = "/request_token";
  private static final String AUTHORIZE_TOKEN_PATH = "/authorize_token";
  private static final String ACCESS_TOKEN_PATH = "/access_token";
  private static final String GET_ALL_TOKENS_PATH = "/get_all_tokens";
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final String CALLBACK_VALUE = "callback";

  private OAuthValidator validator;
  private DataApiTokenContainer tokenContainer;
  private SessionManager sessionManager;
  private HttpServletRequest req;
  private HttpServletResponse resp;
  private DataApiOAuthServlet servlet;
  private ServletOutputStreamStub outputStream;
  private StringWriter outputWriter;
  private OAuthConsumer consumer;

  @Override
  protected void setUp() throws Exception {
    validator = mock(OAuthValidator.class);
    sessionManager = mock(SessionManager.class);

    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    when(tokenGenerator.generateToken(anyInt())).thenReturn(FAKE_TOKEN);
    tokenContainer = new DataApiTokenContainer(tokenGenerator);

    req = mock(HttpServletRequest.class);
    when(req.getRequestURL()).thenReturn(new StringBuffer("www.example.com/robot"));
    when(req.getLocale()).thenReturn(Locale.ENGLISH);
    HttpSession sessionMock = mock(HttpSession.class);
    when(req.getSession()).thenReturn(sessionMock);
    when(req.getSession(anyBoolean())).thenReturn(sessionMock);

    resp = mock(HttpServletResponse.class);
    outputStream = new ServletOutputStreamStub();
    when(resp.getOutputStream()).thenReturn(outputStream);
    outputWriter = new StringWriter();
    when(resp.getWriter()).thenReturn(new PrintWriter(outputWriter));

    OAuthServiceProvider serviceProvider = new OAuthServiceProvider("", "", "");
    consumer = new OAuthConsumer("", "consumerkey", "consumersecret", serviceProvider);

    servlet =
        new DataApiOAuthServlet(REQUEST_TOKEN_PATH,
            AUTHORIZE_TOKEN_PATH, ACCESS_TOKEN_PATH, GET_ALL_TOKENS_PATH,
            serviceProvider, validator, tokenContainer, sessionManager, tokenGenerator);
  }

  @Override
  protected void tearDown() throws Exception {
    outputStream.close();
  }

  public void testDoRequestToken() throws Exception {
    when(req.getPathInfo()).thenReturn(REQUEST_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");

    servlet.doGet(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    verify(validator).validateMessage(any(OAuthMessage.class), any(OAuthAccessor.class));
    assertTrue(outputStream.isClosed());

    // Verify that the output contains a token and token secret.
    String output = outputStream.toString();
    Map<String, String> parameters = toMap(OAuth.decodeForm(output));
    assertTrue("Request token should be present", parameters.containsKey(OAuth.OAUTH_TOKEN));
    assertTrue(
        "Request token secret should be present", parameters.containsKey(OAuth.OAUTH_TOKEN_SECRET));
    OAuthAccessor requestTokenAccessor =
        tokenContainer.getRequestTokenAccessor(parameters.get(OAuth.OAUTH_TOKEN));
    assertNotNull("Container should have stored the token", requestTokenAccessor);
    assertEquals("Correct secret should be returned", requestTokenAccessor.tokenSecret,
        parameters.get(OAuth.OAUTH_TOKEN_SECRET));
  }

  public void testDoRequestTokenUnauthorizedOnOAuthException() throws Exception {
    when(req.getPathInfo()).thenReturn(REQUEST_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");

    doThrow(new OAuthException("")).when(validator).validateMessage(
        any(OAuthMessage.class), any(OAuthAccessor.class));

    servlet.doGet(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  public void testDoRequestTokenUnauthorizedOnURISyntaxException() throws Exception {
    when(req.getPathInfo()).thenReturn(REQUEST_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");

    doThrow(new URISyntaxException("", "")).when(validator).validateMessage(
        any(OAuthMessage.class), any(OAuthAccessor.class));

    servlet.doGet(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  public void testDoAuthorizeTokenGet() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    servlet.doGet(req, resp);

    verify(resp).getWriter();
    assertFalse("Output must have been written", outputWriter.toString().isEmpty());
    verify(resp).setStatus(HttpServletResponse.SC_OK);
  }

  public void testDoAuthorizeTokenBadRequestOnMissingParameters() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");

    // No parameters set.
    servlet.doGet(req, resp);

    verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  public void testDoAuthorizeTokenRedirectsForLogin() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);

    String expectedRedirect = "/auth/login/fake";
    when(sessionManager.getLoginUrl(anyString())).thenReturn(expectedRedirect);

    // No user logged in.
    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(null);

    servlet.doGet(req, resp);

    verify(resp).sendRedirect(expectedRedirect);
  }

  public void testDoAuthorizeTokenUnauthorizedOnWrongToken() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    params.put(OAuth.OAUTH_TOKEN, new String[] {"wrong_token"});
    when(req.getParameterMap()).thenReturn(params);

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    servlet.doGet(req, resp);

    verify(resp).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
  }

  public void testDoAuthorizeTokenPost() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("POST");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);
    String token = servlet.getOrGenerateXsrfToken(ALEX);
    when(req.getParameter("token")).thenReturn(token);
    when(req.getParameter("agree")).thenReturn("yes");

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    servlet.doPost(req, resp);

    verify(resp).sendRedirect(contains(CALLBACK_VALUE));
    String requestToken = params.get(OAuth.OAUTH_TOKEN)[0];
    assertEquals("Token should be authorized by Alex", ALEX,
        tokenContainer.getRequestTokenAccessor(requestToken).getProperty(
            DataApiTokenContainer.USER_PROPERTY_NAME));
  }

  public void testDoAuthorizeTokenPostUnauthorizedOnFailingXsrf() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("POST");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);
    when(req.getParameter("token")).thenReturn("wrong_token");

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    servlet.doPost(req, resp);

    verify(resp).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
  }

  public void testDoAuthorizeTokenPostRejectsToken() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("POST");
    when(req.getParameter("cancel")).thenReturn("yes");
    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);
    String token = servlet.getOrGenerateXsrfToken(ALEX);
    when(req.getParameter("token")).thenReturn(token);

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    try {
      tokenContainer.getRequestTokenAccessor(params.get(OAuth.OAUTH_TOKEN)[0]);
      fail("This token should not be present anymore");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testDoAuthorizeTokenPostBadRequestWhenOmittedPostData() throws Exception {
    when(req.getPathInfo()).thenReturn(AUTHORIZE_TOKEN_PATH);
    when(req.getMethod()).thenReturn("POST");

    Map<String, String[]> params = getDoAuthorizeTokenParams();
    when(req.getParameterMap()).thenReturn(params);
    String token = servlet.getOrGenerateXsrfToken(ALEX);
    when(req.getParameter("token")).thenReturn(token);

    when(sessionManager.getLoggedInUser(any(HttpSession.class))).thenReturn(ALEX);

    // We didn't set the cancel nor agree param, i.e. something is wrong with
    // the form being submitted.
    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  public void testDoExchangeToken() throws Exception {
    when(req.getPathInfo()).thenReturn(ACCESS_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoExchangeTokenParams();
    when(req.getParameterMap()).thenReturn(params);

    servlet.doGet(req, resp);

    verify(validator).validateMessage(any(OAuthMessage.class), any(OAuthAccessor.class));
    verify(resp).setStatus(HttpServletResponse.SC_OK);

    // Verify that the output contains a token and token secret.
    String output = outputStream.toString();
    Map<String, String> parameters = toMap(OAuth.decodeForm(output));
    assertTrue("Access token should be present", parameters.containsKey(OAuth.OAUTH_TOKEN));
    assertTrue(
        "Access token secret should be present", parameters.containsKey(OAuth.OAUTH_TOKEN_SECRET));
    OAuthAccessor accessTokenAccessor =
        tokenContainer.getAccessTokenAccessor(parameters.get(OAuth.OAUTH_TOKEN));
    assertNotNull("Container should have stored the token", accessTokenAccessor);
    assertEquals("Correct secret should be returned", accessTokenAccessor.tokenSecret,
        parameters.get(OAuth.OAUTH_TOKEN_SECRET));
  }

  public void testDoExchangeTokenUnauthorizedOnUnknownToken() throws Exception {
    when(req.getPathInfo()).thenReturn(ACCESS_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoExchangeTokenParams();
    params.put(OAuth.OAUTH_TOKEN, new String[] {"unknown"});
    when(req.getParameterMap()).thenReturn(params);

    servlet.doGet(req, resp);

    verify(resp).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
  }

  public void testDoExchangeTokenUnauthorizedOnOAuthException() throws Exception {
    when(req.getPathInfo()).thenReturn(ACCESS_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");

    Map<String, String[]> params = getDoExchangeTokenParams();
    when(req.getParameterMap()).thenReturn(params);

    doThrow(new OAuthException("")).when(validator).validateMessage(
        any(OAuthMessage.class), any(OAuthAccessor.class));

    servlet.doGet(req, resp);

    verify(validator).validateMessage(any(OAuthMessage.class), any(OAuthAccessor.class));
    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  public void testDoExchangeTokenUnauthorizedOnURISyntaxException() throws Exception {
    when(req.getPathInfo()).thenReturn(ACCESS_TOKEN_PATH);
    when(req.getMethod()).thenReturn("GET");
    Map<String, String[]> params = getDoExchangeTokenParams();
    when(req.getParameterMap()).thenReturn(params);

    doThrow(new URISyntaxException("", "")).when(validator).validateMessage(
        any(OAuthMessage.class), any(OAuthAccessor.class));

    servlet.doGet(req, resp);

    verify(validator).validateMessage(any(OAuthMessage.class), any(OAuthAccessor.class));
    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  /** Sets the list of parameters needed for testing authorizing a request token */
  private Map<String, String[]> getDoAuthorizeTokenParams() {
    OAuthAccessor requestAccessor = tokenContainer.generateRequestToken(consumer);
    Map<String, String[]> params = Maps.newHashMap();
    params.put(OAuth.OAUTH_TOKEN, new String[] {requestAccessor.requestToken});
    params.put(OAuth.OAUTH_CALLBACK, new String[] {CALLBACK_VALUE});
    return params;
  }

  /** Sets the list of parameters needed to test exchanging a request token */
  private Map<String, String[]> getDoExchangeTokenParams() throws Exception {
    OAuthAccessor requestAccessor = tokenContainer.generateRequestToken(consumer);
    tokenContainer.authorizeRequestToken(requestAccessor.requestToken, ALEX);
    Map<String, String[]> params = Maps.newHashMap();
    params.put(OAuth.OAUTH_TOKEN, new String[] {requestAccessor.requestToken});
    return params;
  }

  /**
   * Converts a list of parameters to a map.
   */
  private static Map<String, String> toMap(List<Parameter> params) {
    Map<String, String> map = Maps.newHashMap();
    for (Parameter parameter : params) {
      map.put(parameter.getKey(), parameter.getValue());
    }
    return map;
  }
}
