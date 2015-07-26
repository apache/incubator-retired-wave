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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.servlet.http.HttpSession;

/**
 * Unit tests for {@link SessionManagerImpl}.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SessionManagerTest extends TestCase {
  @Mock private org.eclipse.jetty.server.SessionManager jettySessionManager;

  private SessionManager sessionManager;
  private HumanAccountData account;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    AccountStore store = new MemoryStore();
    account = new HumanAccountDataImpl(ParticipantId.ofUnsafe("tubes@example.com"));
    store.putAccount(account);
    sessionManager = new SessionManagerImpl(store, jettySessionManager);
  }

  public void testSessionFetchesAddress() {
    HttpSession session = mock(HttpSession.class);
    ParticipantId id = ParticipantId.ofUnsafe("tubes@example.com");
    when(session.getAttribute("user")).thenReturn(id);

    assertEquals(id, sessionManager.getLoggedInUser(session));
    assertSame(account, sessionManager.getLoggedInAccount(session));
  }

  public void testUnknownUserReturnsNull() {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(ParticipantId.ofUnsafe("missing@example.com"));

    assertNull(sessionManager.getLoggedInAccount(session));
  }

  public void testNullSessionReturnsNull() {
    assertNull(sessionManager.getLoggedInUser(null));
    assertNull(sessionManager.getLoggedInAccount(null));
  }

  public void testGetLoginUrlWithNoArgument() {
    assertEquals(SessionManager.SIGN_IN_URL, sessionManager.getLoginUrl(null));
  }

  public void testGetLoginUrlWithSimpleRedirect() {
    assertEquals(SessionManager.SIGN_IN_URL + "?r=/some/other/url",
        sessionManager.getLoginUrl("/some/other/url"));
  }

  public void testGetLoginUrlEncodesQueryParameters() {
    String url = "/abc123?nested=query&string";
    String encoded_url = "/abc123?nested%3Dquery%26string";
    assertEquals(
        SessionManager.SIGN_IN_URL + "?r=" + encoded_url, sessionManager.getLoginUrl(url));
  }

  public void testGetSessionFromToken() {
    HttpSession session = mock(HttpSession.class);
    Mockito.when(jettySessionManager.getHttpSession("abc123")).thenReturn(session);
    assertSame(session, sessionManager.getSessionFromToken("abc123"));
  }

  public void testGetSessionFromUnknownToken() {
    HttpSession session = mock(HttpSession.class);
    Mockito.when(jettySessionManager.getHttpSession("abc123")).thenReturn(null);
    assertNull(sessionManager.getSessionFromToken("abc123"));
  }
}
