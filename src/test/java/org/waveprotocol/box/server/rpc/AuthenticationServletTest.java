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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.AuthTestUtil;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.PercentEscaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AuthenticationServletTest extends TestCase {
  private static final ParticipantId USER = ParticipantId.ofUnsafe("frodo@example.com");

  private AuthenticationServlet servlet;

  @Mock private HttpServletRequest req;
  @Mock private HttpServletResponse resp;
  @Mock private HttpSession session;
  @Mock private SessionManager manager;
  @Mock private WelcomeRobot welcomeBot;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    AccountStore store = new MemoryStore();
    HumanAccountData account =
        new HumanAccountDataImpl(USER, new PasswordDigest("password".toCharArray()));
    store.putAccount(account);

    Config config = ConfigFactory.parseMap(ImmutableMap.<String, Object>of(
      "administration.disable_registration", false,
      "administration.analytics_account", "UA-someid",
      "security.enable_clientauth", false,
      "security.clientauth_cert_domain", "",
      "administration.disable_loginpage", false)
    );

    servlet = new AuthenticationServlet(store, AuthTestUtil.makeConfiguration(),
        manager, "examPLe.com", config, welcomeBot);
    AccountStoreHolder.init(store, "eXaMple.com");
  }

  @Override
  protected void tearDown() throws Exception {
    AccountStoreHolder.resetForTesting();
  }

  public void testGetReturnsSomething() throws IOException {
    when(req.getSession(false)).thenReturn(null);

    PrintWriter writer = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(writer);
    when(req.getLocale()).thenReturn(Locale.ENGLISH);

    servlet.doGet(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
  }

  public void testGetRedirects() throws IOException {
    String location = "/abc123?nested=query&string";
    when(req.getSession(false)).thenReturn(session);
    when(manager.getLoggedInUser(session)).thenReturn(USER);
    configureRedirectString(location);

    servlet.doGet(req, resp);
    verify(resp).sendRedirect(location);
  }

  public void testValidLoginWorks() throws IOException {
    attemptLogin("frodo@example.com", "password", true);
    verify(resp).sendRedirect("/");
  }

  public void testUserWithNoDomainGetsDomainAutomaticallyAdded() throws Exception {
    attemptLogin("frodo", "password", true);
    verify(resp).sendRedirect("/");
  }

  public void testLoginRedirects() throws IOException {
    String redirect = "/abc123?nested=query&string";
    configureRedirectString(redirect);
    attemptLogin("frodo@example.com", "password", true);

    verify(resp).sendRedirect(redirect);
  }

  public void testLoginDoesNotRedirectToRemoteSite() throws IOException {
    configureRedirectString("http://example.com/other/site");
    attemptLogin("frodo@example.com", "password", true);

    verify(resp, never()).sendRedirect(anyString());
  }

  public void testIncorrectPasswordReturns403() throws IOException {
    attemptLogin("frodo@example.com", "incorrect", false);

    verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(session, never()).setAttribute(eq("user"), anyString());
  }

  public void testInvalidUsernameReturns403() throws IOException {
    attemptLogin("madeup@example.com", "incorrect", false);

    verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(session, never()).setAttribute(eq("address"), anyString());
  }

  // *** Utility methods

  private void configureRedirectString(String location) {
    PercentEscaper escaper =
        new PercentEscaper(PercentEscaper.SAFEQUERYSTRINGCHARS_URLENCODER, false);
    String queryStr = "r=" + escaper.escape(location);
    when(req.getQueryString()).thenReturn(queryStr);
  }

  private void attemptLogin(String address, String password, boolean expectSuccess) throws IOException {
    // The query string is escaped.
    PercentEscaper escaper = new PercentEscaper(PercentEscaper.SAFECHARS_URLENCODER, true);
    String data =
        "address=" + escaper.escape(address) + "&" + "password=" + escaper.escape(password);

    Reader reader = new StringReader(data);
    when(req.getReader()).thenReturn(new BufferedReader(reader));
    PrintWriter writer = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(writer);
    when(req.getSession(false)).thenReturn(null);
    when(req.getSession(true)).thenReturn(session);
    when(req.getLocale()).thenReturn(Locale.ENGLISH);

    // Servlet control flow forces us to set these return values first and
    // verify the logged in user was set afterwards.
    if (expectSuccess) {
      when(manager.getLoggedInUser(Mockito.any(HttpSession.class))).thenReturn(USER);
      when(session.getAttribute("user")).thenReturn(USER);
    }
    servlet.doPost(req, resp);
    if (expectSuccess) {
      verify(manager).setLoggedInUser(session, USER);
    }
  }
}
