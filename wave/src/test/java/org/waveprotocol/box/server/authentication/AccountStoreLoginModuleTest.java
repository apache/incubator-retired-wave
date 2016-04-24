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

package org.waveprotocol.box.server.authentication;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 *
 */
public class AccountStoreLoginModuleTest extends TestCase {
  private class FakeCallbackHandler implements CallbackHandler {
    final String address, password;

    public FakeCallbackHandler(String address, String password) {
      this.address = address;
      this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
      for (Callback c : callbacks) {
        if (c instanceof NameCallback) {
          ((NameCallback) c).setName(address);
        } else if (c instanceof PasswordCallback) {
          ((PasswordCallback) c).setPassword(password.toCharArray());
        } else {
          throw new UnsupportedCallbackException(c);
        }
      }
    }
  }

  @Override
  protected void setUp() throws Exception {
    AccountStore store = new MemoryStore();
    store.putAccount(new HumanAccountDataImpl(
        ParticipantId.ofUnsafe("haspwd@example.com"), new PasswordDigest("pwd".toCharArray())));
    store.putAccount(new HumanAccountDataImpl(ParticipantId.ofUnsafe("nopwd@example.com")));
    AccountStoreHolder.init(store, "example.com");
  }

  @Override
  protected void tearDown() {
    AccountStoreHolder.resetForTesting();
  }

  private LoginContext makeLoginContext(String address, String password) throws LoginException {
    return new LoginContext("Wave", new Subject(), new FakeCallbackHandler(address, password),
        AuthTestUtil.makeConfiguration());
  }

  private static void assertLoginFails(LoginContext context) {
    try {
      context.login();
      fail("Login succeeded unexpectedly");
    } catch (LoginException e) {
      // Pass.
    }
  }

  public void testIncorrectPasswordThrowsLoginException() throws Exception {
    LoginContext context = makeLoginContext("haspwd@example.com", "wrongpassword");
    assertLoginFails(context);

    // Make sure the subject doesn't have any principals set.
    assertEquals(0, context.getSubject().getPrincipals(ParticipantPrincipal.class).size());
  }

  public void testCorrectPasswordConfiguresSubject() throws Exception {
    LoginContext context = makeLoginContext("haspwd@example.com", "pwd");
    context.login();
    Subject subject = context.getSubject();
    ParticipantPrincipal p = subject.getPrincipals(ParticipantPrincipal.class).iterator().next();
    assertEquals("haspwd@example.com", p.getName());

    context.logout();
    assertEquals(0, subject.getPrincipals(ParticipantPrincipal.class).size());
  }

  public void testMissingDomainIsAddedAutomatically() throws Exception {
    LoginContext context = makeLoginContext("haspwd", "pwd");
    context.login();
    Subject subject = context.getSubject();
    ParticipantPrincipal p = subject.getPrincipals(ParticipantPrincipal.class).iterator().next();
    assertEquals("haspwd@example.com", p.getName());
  }

  public void testUserWithNoPasswordCannotLogin() throws Exception {
    LoginContext context = makeLoginContext("nopwd@example.com", "");
    assertLoginFails(context);
    assertEquals(0, context.getSubject().getPrincipals(ParticipantPrincipal.class).size());
  }
}
