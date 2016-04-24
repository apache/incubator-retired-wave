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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;


import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.xmpp.MockOutgoingPacketTransport.Router;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test round-trips between two XmppManager instances pointed at each other.
 *
 * This class is not intended to test specific calls; it is primary to test
 * reliable calls made by the manager along with error handling. Any specific
 * call coverage is purely a side-effect of wanting real test data.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */

public class RoundTripTest extends TestCase {

  private static final String SERVER1_DOMAIN = "google.com";
  private static final String SERVER2_DOMAIN = "acmewave.com";

  private static final int PACKET_TIMEOUT = 10;

  private static class ServerInstances {
    final String jid;
    final XmppManager manager;
    final XmppFederationHost host;
    final XmppFederationRemote remote;
    final XmppDisco disco;
    final MockOutgoingPacketTransport transport;

    ServerInstances(String domain, MockOutgoingPacketTransport.Router router) {
      // Mocks.
      host = mock(XmppFederationHost.class);
      remote = mock(XmppFederationRemote.class);
      disco = mock(XmppDisco.class);

      // 'Real' instantiated classes!
      jid = "wave." + domain;
      transport = new MockOutgoingPacketTransport(router);

      final Map<String, Object> props = new HashMap<>();
      props.put("federation.xmpp_disco_successful_expiry", "6s");
      props.put("federation.xmpp_jid", jid);
      manager = new XmppManager(
        host, remote, disco, transport, ConfigFactory.parseMap(props));

      // Verify manager callback.
      verify(host).setManager(eq(manager));
      verify(remote).setManager(eq(manager));
      verify(disco).setManager(eq(manager));
    }
  }

  private ServerInstances server1;
  private ServerInstances server2;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    server1 = new ServerInstances(SERVER1_DOMAIN, new Router() {
      @Override
      public void route(Packet packet) {
        server2.manager.receivePacket(packet);
      }
    });
    server2 = new ServerInstances(SERVER2_DOMAIN, new Router() {
      @Override
      public void route(Packet packet) {
        server1.manager.receivePacket(packet);
      }
    });
  }

  /**
   * Test the simple case of packet send/receive by sending a malformed request.
   */
  public void testPacketSendMalformedFailure() {
    Packet packet = new IQ();
    packet.setFrom(server1.jid);
    packet.setID("irrelevant");
    packet.setTo(server2.jid);

    PacketCallback callback = mock(PacketCallback.class);

    // Send an outgoing packet from server1 -> server2
    server1.manager.send(packet, callback, PACKET_TIMEOUT);
    assertEquals("First transport should have a single packet pending",
        1, server1.transport.packets.size());
    assertEquals("First transport should have unmodified outgoing packet",
        packet, server1.transport.packets.peek());

    // Confirm that server2 sent back an error
    assertEquals("Second transport should have a single packet pending",
        1, server2.transport.packets.size());
    assertNotNull("Second transport should be an error packet",
        server2.transport.packets.peek().getError());

    // Ensure the error is interpreted correctly and returned to the callback
    ArgumentCaptor<FederationError> errorCaptor = ArgumentCaptor.forClass(FederationError.class);
    verify(callback).error(errorCaptor.capture());
    verify(callback, never()).run(any(Packet.class));
    assertEquals("Invalid packet was sent, error should be BAD_REQUEST",
        FederationError.Code.BAD_REQUEST, errorCaptor.getValue().getErrorCode());
  }

  /**
   * Test the simple case of having a response invoked based entirely on the
   * timeout case.
   */
  public void testPacketTimeout() throws Exception {
    int TIMEOUT_DELAY = 0;
    int TIMEOUT_WAIT = 5;

    // Send a valid packet, so it is received by the remote Disco mock, but not processed.
    IQ packet = new IQ();
    packet.setFrom(server1.jid);
    packet.setID("disco");
    packet.setTo(server2.jid);
    packet.setType(IQ.Type.get);
    packet.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);

    PacketCallback callback = mock(PacketCallback.class);
    final CountDownLatch finished = new CountDownLatch(1);

    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        FederationError error = (FederationError) invocation.getArguments()[0];
        assertEquals(FederationError.Code.REMOTE_SERVER_TIMEOUT, error.getErrorCode());
        finished.countDown();
        return null;
      }
    }).when(callback).error(any(FederationError.class));

    server1.manager.send(packet, callback, TIMEOUT_DELAY);
    assertTrue(finished.await(TIMEOUT_WAIT, TimeUnit.SECONDS));
    verify(callback, never()).run(any(Packet.class));

    // Mockito says never to reset a mock, but we have to as the callback is
    // already used deep in XmppManager.
    Mockito.reset(callback);

    // For fun, process the request by the remote disco and return a response
    // that will never be processed.
    ArgumentCaptor<PacketCallback> server2Callback = ArgumentCaptor.forClass(PacketCallback.class);
    verify(server2.disco).processDiscoItemsGet(eq(packet), server2Callback.capture());
    XmppDisco realDisco = new XmppDisco(MockDisco.config);
    realDisco.setManager(server2.manager);
    realDisco.processDiscoItemsGet(packet, server2Callback.getValue());

    // Confirm disco on server2 has replied with a packet.
    assertEquals(1, server2.transport.packets.size());
    assertEquals(null, server2.transport.packets.peek().getError());

    // Confirm, however, that the packet is dropped by the first manager (no pending call!).
    verifyZeroInteractions(callback);
  }

  /**
   * Test that an arbitrary error response is properly returned when generated
   * by the second server. Also ensure that the second server can't invoke its
   * callback twice.
   */
  public void testErrorResponse() {
    FederationError.Code TEST_CODE = FederationError.Code.NOT_AUTHORIZED;
    PacketError.Condition TEST_CONDITION = PacketError.Condition.not_authorized;

    // Send a valid packet, so it is received by the remote Disco mock, but not
    // explicitly processed.
    IQ packet = new IQ();
    packet.setFrom(server1.jid);
    packet.setID("disco");
    packet.setTo(server2.jid);
    packet.setType(IQ.Type.get);
    packet.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);

    PacketCallback callback = mock(PacketCallback.class);
    server1.manager.send(packet, callback, PACKET_TIMEOUT);

    // Accept the disco request and return TEST_CODE error.
    ArgumentCaptor<PacketCallback> server2Callback = ArgumentCaptor.forClass(PacketCallback.class);
    verify(server2.disco).processDiscoItemsGet(eq(packet), server2Callback.capture());
    server2Callback.getValue().error(FederationErrors.newFederationError(TEST_CODE));

    // Try to then complete the message, but cause an IllegalStateException.
    IQ fakeResponse = IQ.createResultIQ(packet);
    try {
      server2Callback.getValue().run(fakeResponse);
      fail("Should not be able to invoke callback twice");
    } catch (IllegalStateException e) {
      // pass
    }

    // Check the outgoing packet log.
    assertEquals(1, server2.transport.packets.size());
    Packet errorResponse = server2.transport.packets.peek();
    PacketError error = errorResponse.getError();
    assertNotNull(error);
    assertEquals(TEST_CONDITION, error.getCondition());

    // Assert that the error response does *not* include the original packet.
    assertTrue(errorResponse instanceof IQ);
    IQ errorIQ = (IQ) errorResponse;
    assertEquals(null, errorIQ.getChildElement());

    // Confirm that the error is received properly on the first server.
    ArgumentCaptor<FederationError> returnedError = ArgumentCaptor.forClass(FederationError.class);
    verify(callback).error(returnedError.capture());
    verify(callback, never()).run(any(Packet.class));
    assertEquals(TEST_CODE, returnedError.getValue().getErrorCode());

    // If we push the error again, it should be dropped. Note that resetting the
    // callback here is the simplest way to test this, since it is already
    // registered inside the manager.
    reset(callback);
    server1.manager.receivePacket(errorResponse);
    verifyZeroInteractions(callback);
  }

  /**
   * Test that an unhandled error (e.g. <forbidden>) is translated to
   * UNDEFINED_CONDITION before being returned to the mocked callback.
   */
  public void testUnhandledErrorResponse() {
    IQ packet = new IQ();
    packet.setFrom(server1.jid);
    packet.setID("foo");
    packet.setTo(server2.jid);

    // Disable routing so we can intercept the packet.
    server1.transport.router = null;
    PacketCallback callback = mock(PacketCallback.class);
    server1.manager.send(packet, callback, PACKET_TIMEOUT);

    // Generate an explicit error <forbidden>.
    IQ errorPacket = IQ.createResultIQ(packet);
    errorPacket.setError(PacketError.Condition.forbidden);
    server1.manager.receivePacket(errorPacket);

    // Confirm that <forbidden> is transformed to UNDEFINED_CONDITION.
    ArgumentCaptor<FederationError> returnedError = ArgumentCaptor.forClass(FederationError.class);
    verify(callback).error(returnedError.capture());
    verify(callback, never()).run(any(Packet.class));
    assertEquals(FederationError.Code.UNDEFINED_CONDITION, returnedError.getValue().getErrorCode());
  }

  /**
   * Test that packet IDs cannot be re-used while in-flight, and also that may
   * be re-used later.
   */
  public void testReusePacketId() throws Exception {
    int REUSE_FAIL_WAIT = 5;

    IQ packet = new IQ();
    packet.setFrom(server1.jid);
    packet.setID("foo-packet");
    packet.setTo(server2.jid);

    // Disable routing so we can intercept the packet.
    server1.transport.router = null;
    PacketCallback callback = mock(PacketCallback.class);
    server1.manager.send(packet, callback, PACKET_TIMEOUT);
    assertEquals(1, server1.transport.packets.size());
    assertEquals(packet, server1.transport.packets.poll());

    // Try sending another packet with the same ID - must fail (called back in
    // another thread)!
    PacketCallback invalidCallback = mock(PacketCallback.class);
    final CountDownLatch finished = new CountDownLatch(1);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        FederationError error = (FederationError) invocation.getArguments()[0];
        assertEquals(FederationError.Code.UNDEFINED_CONDITION, error.getErrorCode());
        finished.countDown();
        return null;
      }
    }).when(invalidCallback).error(any(FederationError.class));

    server1.manager.send(packet, invalidCallback, PACKET_TIMEOUT);
    assertTrue(finished.await(REUSE_FAIL_WAIT, TimeUnit.SECONDS));
    verify(invalidCallback, never()).run(any(Packet.class));

    // Generate an explicit success response.
    IQ successPacket = IQ.createResultIQ(packet);
    server1.manager.receivePacket(successPacket);
    verify(callback).run(eq(successPacket));
    verify(callback, never()).error(any(FederationError.class));

    // Again, re-use the ID: should succeed since it is cleared from callbacks.
    PacketCallback zeroCallback = mock(PacketCallback.class);
    server1.manager.send(packet, zeroCallback, PACKET_TIMEOUT);
    assertEquals(1, server1.transport.packets.size());
    assertEquals(packet, server1.transport.packets.poll());
    verifyZeroInteractions(zeroCallback);
  }

  /**
   * Test that if (e.g.) an IQ is sent, then an IQ must be returned as a
   * response. If a Message is returned instead, this should invoke an error
   * callback.
   */
  public void testDropInvalidResponseType() throws Exception {
    IQ packet = server1.manager.createRequestIQ(server2.jid);

    // Disable routing so we can intercept the packet.
    server1.transport.router = null;
    PacketCallback callback = mock(PacketCallback.class);
    server1.manager.send(packet, callback, PACKET_TIMEOUT);

    // Generate an explicit Message receipt.
    Message response = new Message();
    response.setTo(packet.getFrom());
    response.setID(packet.getID());
    response.setFrom(packet.getTo());
    response.addChildElement("received", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);
    server1.manager.receivePacket(response);

    // Confirm that an error callback is invoked.
    ArgumentCaptor<FederationError> returnedError = ArgumentCaptor.forClass(FederationError.class);
    verify(callback).error(returnedError.capture());
    verify(callback, never()).run(any(Packet.class));
    assertEquals(FederationError.Code.UNDEFINED_CONDITION, returnedError.getValue().getErrorCode());

    // Confirm that sending a correct response now does nothing.
    reset(callback);
    IQ correctResponse = IQ.createResultIQ(packet);
    server1.manager.receivePacket(correctResponse);
    verifyZeroInteractions(callback);
  }

}
