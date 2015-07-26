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

package org.waveprotocol.wave.federation.xmpp;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.xmpp.packet.IQ;
import org.joda.time.DateTimeUtils;
import org.waveprotocol.wave.federation.FederationErrors;


import junit.framework.TestCase;

/**
 * Performs naive tests over RemoteDisco. Integration testing is performed in
 * {@link XmppDiscoTest}.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */

public class RemoteDiscoTest extends TestCase {

  private final static String REMOTE_DOMAIN = "acmewave.com";
  private final static String REMOTE_JID = "wave.acmewave.com";
  private static final int SUCCESS_EXPIRY_SECS = 600;
  private static final int FAIL_EXPIRY_SECS = 120;
  private RemoteDisco remoteDisco;
  private SuccessFailCallback<String, String> callback;

  protected void setUp() throws Exception {
    super.setUp();
    XmppManager manager = mock(XmppManager.class);
    when(manager.createRequestIQ(eq(REMOTE_DOMAIN))).thenReturn(new IQ());

    DateTimeUtils.setCurrentMillisFixed(0);
    remoteDisco = new RemoteDisco(manager, REMOTE_DOMAIN, FAIL_EXPIRY_SECS,
                                              SUCCESS_EXPIRY_SECS);
    callback = mockDiscoCallback();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    DateTimeUtils.setCurrentMillisSystem();
  }

  @SuppressWarnings("unchecked")
  private SuccessFailCallback<String, String> mockDiscoCallback() {
    return mock(SuccessFailCallback.class);
  }

  /**
   * Test a RemoteDisco created with a forced success case.
   */
  public void testForcedSuccess() {
    RemoteDisco remoteDisco = new RemoteDisco(REMOTE_DOMAIN, REMOTE_JID, null);

    SuccessFailCallback<String, String> callback = mockDiscoCallback();
    remoteDisco.discoverRemoteJID(callback);
    verify(callback).onSuccess(eq(REMOTE_JID));
    verify(callback, never()).onFailure(anyString());
  }

  /**
   * Test a RemoteDisco created with a forced failure case.
   */
  public void testForcedFailure() {
    RemoteDisco remoteDisco = new RemoteDisco(REMOTE_DOMAIN, null,
        FederationErrors.badRequest("irrelevant"));

    SuccessFailCallback<String, String> callback = mockDiscoCallback();
    remoteDisco.discoverRemoteJID(callback);
    verify(callback, never()).onSuccess(anyString());
    verify(callback).onFailure(anyString());
    callback = mockDiscoCallback();
    remoteDisco.discoverRemoteJID(callback);
    verify(callback, never()).onSuccess(anyString());
    verify(callback).onFailure(anyString());
  }

  /**
   * Tests the disco expiry code for successful disco results.
   */
  public void testTimeToLive() {
    remoteDisco.discoverRemoteJID(callback);
    assertFalse(remoteDisco.ttlExceeded());
    remoteDisco.finish(REMOTE_JID, null);  // successful disco
    assertFalse(remoteDisco.ttlExceeded());
    tick((SUCCESS_EXPIRY_SECS - 1) * 1000); // not quite expired
    assertFalse(remoteDisco.ttlExceeded());
    tick(20 * 1000); // should now be expired
    assertTrue(remoteDisco.ttlExceeded());
  }

  /**
   * Tests the disco expiry code for failed disco results.
   */
  public void testTimeToLiveDiscoFailed() {
    remoteDisco.discoverRemoteJID(callback);
    assertFalse(remoteDisco.ttlExceeded());
    remoteDisco.finish(null, FederationErrors.badRequest("test failure"));  // failed disco
    assertFalse(remoteDisco.ttlExceeded());
    tick((FAIL_EXPIRY_SECS - 1) * 1000); // not quite expired
    assertFalse(remoteDisco.ttlExceeded());
    tick(20 * 1000); // should now be expired
    assertTrue(remoteDisco.ttlExceeded());
  }

  /**
   * Advance the clock.
   *
   * @param millis milliseconds to advance clock
   */
  private void tick(int millis) {
    DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + millis);
  }
}
