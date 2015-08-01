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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class UserRegistrationServletTest extends TestCase {
  private final AccountData account = new HumanAccountDataImpl(
      ParticipantId.ofUnsafe("frodo@example.com"), new PasswordDigest("password".toCharArray()));
  private AccountStore store;

  @Mock private HttpServletRequest req;
  @Mock private HttpServletResponse resp;

  @Mock private WelcomeRobot welcomeBot;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    store = new MemoryStore();
    store.putAccount(account);

  }

  public void testRegisterNewUserEnabled() throws Exception {
    attemptToRegister(req, resp, "foo@example.com", "internet", false);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    ParticipantId participantId = ParticipantId.ofUnsafe("foo@example.com");
    AccountData account = store.getAccount(participantId);
    assertNotNull(account);
    assertTrue(account.asHuman().getPasswordDigest().verify("internet".toCharArray()));
    verify(welcomeBot).greet(eq(participantId));
  }

  public void testRegisterNewUserDisabled() throws Exception {
    attemptToRegister(req, resp, "foo@example.com", "internet", true);

    verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
    ParticipantId participantId = ParticipantId.ofUnsafe("foo@example.com");
    AccountData account = store.getAccount(participantId);
    assertNull(account);
  }

  public void testDomainInsertedAutomatically() throws Exception {
    attemptToRegister(req, resp, "sam", "fdsa", false);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    assertNotNull(store.getAccount(ParticipantId.ofUnsafe("sam@example.com")));
  }

  public void testRegisterExistingUserThrowsError() throws Exception {
    attemptToRegister(req, resp, "frodo@example.com", "asdf", false);

    verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);

    // ... and it should have left the account store unchanged.
    assertSame(account, store.getAccount(account.getId()));
  }

  public void testRegisterUserAtForeignDomainThrowsError() throws Exception {
    attemptToRegister(req, resp, "bilbo@example2.com", "fdsa", false);

    verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
    assertNull(store.getAccount(ParticipantId.ofUnsafe("bilbo@example2.com")));
  }

  public void testUsernameTrimmed() throws Exception {
    attemptToRegister(req, resp, " ben@example.com ", "beetleguice", false);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    assertNotNull(store.getAccount(ParticipantId.ofUnsafe("ben@example.com")));
  }

  public void testNullPasswordWorks() throws Exception {
    attemptToRegister(req, resp, "zd@example.com", null, false);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    AccountData account = store.getAccount(ParticipantId.ofUnsafe("zd@example.com"));
    assertNotNull(account);
    assertTrue(account.asHuman().getPasswordDigest().verify("".toCharArray()));
  }

  public void attemptToRegister(
      HttpServletRequest req, HttpServletResponse resp, String address,
      String password, boolean disabledRegistration) throws IOException {

    Config config1 = ConfigFactory.parseMap(ImmutableMap.<String, Object>of(
      "administration.disable_registration", false,
      "administration.analytics_account", "UA-someid")
    );
    UserRegistrationServlet enabledServlet =
        new UserRegistrationServlet(store, "example.com", config1, welcomeBot);

    Config config2 = ConfigFactory.parseMap(ImmutableMap.<String, Object>of(
      "administration.disable_registration", true,
      "administration.analytics_account", "UA-someid")
    );
    UserRegistrationServlet disabledServlet =
        new UserRegistrationServlet(store, "example.com", config2, welcomeBot);

    when(req.getParameter("address")).thenReturn(address);
    when(req.getParameter("password")).thenReturn(password);
    when(req.getLocale()).thenReturn(Locale.ENGLISH);
    PrintWriter writer = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(writer);

    if (disabledRegistration) {
      disabledServlet.doPost(req, resp);
    } else {
      enabledServlet.doPost(req, resp);
    }

    verify(writer, atLeastOnce()).append(anyString());
  }
}
