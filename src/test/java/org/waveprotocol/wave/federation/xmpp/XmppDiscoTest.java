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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;

import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.joda.time.DateTimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for the {@link XmppDisco} class. Also provides coverage over
 * {@link RemoteDisco} which is used internally by XmppDisco.
 */


public class XmppDiscoTest extends TestCase {
  private static final String LOCAL_DOMAIN = "something.com";
  private static final String LOCAL_JID = "wave." + LOCAL_DOMAIN;
  private static final String REMOTE_DOMAIN = "other.com";
  private static final String REMOTE_JID = "wave." + REMOTE_DOMAIN;

  private static final String DISCO_ITEMS_ID = "disco-items";
  private static final String DISCO_INFO_ID = "disco-info";
  private static final String SERVER_DESCRIPTION = "Wave in a Box";

  // The following JID is intentionally non-Wave.
  private static final String REMOTE_PUBSUB_JID = "pubsub." + REMOTE_DOMAIN;

  private static final String EXPECTED_DISCO_ITEMS_GET =
      "\n<iq type=\"get\" id=\"" + DISCO_ITEMS_ID + "\" to=\"" + REMOTE_DOMAIN + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#items\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_INFO_GET =
      "\n<iq type=\"get\" id=\"" + DISCO_INFO_ID + "\" to=\"" + REMOTE_JID + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#info\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_INFO_GET_PUBSUB =
      "\n<iq type=\"get\" id=\"" + DISCO_INFO_ID + "\" to=\"" + REMOTE_PUBSUB_JID + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#info\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_ITEMS_RESULT =
    "\n<iq type=\"result\" id=\"" + DISCO_ITEMS_ID + "\" from=\"" + LOCAL_JID + "\" "
    + "to=\"" + REMOTE_JID + "\">\n"
    + "  <query xmlns=\"http://jabber.org/protocol/disco#items\"/>\n"
    + "</iq>";

  private static final String EXPECTED_DISCO_INFO_RESULT =
    "\n<iq type=\"result\" id=\""+ DISCO_INFO_ID + "\" from=\"" + LOCAL_JID + "\" "
    + "to=\"" + REMOTE_JID + "\">\n"
    + "  <query xmlns=\"http://jabber.org/protocol/disco#info\">\n"
    + "    <identity category=\"collaboration\" type=\"apache-wave\" "
    + "name=\"" + SERVER_DESCRIPTION + "\"/>\n"
    + "    <feature var=\"http://waveprotocol.org/protocol/0.2/waveserver\"/>\n"
    + "  </query>\n"
    + "</iq>";

  private MockOutgoingPacketTransport transport;
  private XmppManager manager;
  private XmppDisco disco;

  // Explicitly mocked out disco callback usable by individual tests.
  private SuccessFailCallback<String, String> discoCallback;
  private static final int DISCO_FAIL_EXPIRY_SECS = 5 * 60;
  private static final int DISCO_SUCCESS_EXPIRY_SECS = 2 * 60 * 60;

  private final AtomicLong counterStarted;
  private final AtomicLong counterSuccess;
  private final AtomicLong counterFailed;

  public XmppDiscoTest() throws ExecutionException {
    counterStarted = XmppDisco.statDiscoStarted.get(REMOTE_DOMAIN);
    counterSuccess = RemoteDisco.statDiscoSuccess.get(REMOTE_DOMAIN);
    counterFailed = RemoteDisco.statDiscoFailed.get(REMOTE_DOMAIN);
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    disco = new XmppDisco(MockDisco.config);
    transport = new MockOutgoingPacketTransport();
    final Map<String, Object> props = new HashMap<>();
    props.put("federation.xmpp_jid", LOCAL_JID);
    manager = new XmppManager(mock(XmppFederationHost.class), mock(XmppFederationRemote.class),
        disco, transport, ConfigFactory.parseMap(props).withFallback(MockDisco.config));
    disco.setManager(manager);
    discoCallback = createMockCallback();

    resetVarz();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    DateTimeUtils.setCurrentMillisSystem();
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server.
   */
  public void testDiscoStart() {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals(1, transport.packets.size());
    Packet packet = transport.packets.poll();
    assertEquals(REMOTE_DOMAIN, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
    checkAndResetStats(1, 0, 0);  // started
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server, and subsequent
   * disco requests are not sent until there is a retransmit timeout.  Also test that the callback
   * is run even after timing out.
   */
  public void testDiscoRetransmitsOnNoReply() {
    int expectedFailures = 0;
    int expectedPackets = 0;

    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    checkAndResetStats(1, 0, 0);  // started

    expectedFailures++;
    expectedPackets++;
    assertEquals("Should have sent disco packet", expectedPackets, transport.packetsSent);

    for (int i = 1; i < RemoteDisco.MAXIMUM_DISCO_ATTEMPTS; i++) {
      manager.causeImmediateTimeout(transport.packets.remove());
      expectedPackets++;
      assertEquals("Should have retried", expectedPackets, transport.packetsSent);

      disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
      disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
      expectedFailures += 2;
      assertEquals("Should not have sent more outgoing packets",
          expectedPackets, transport.packetsSent);

      // Should be no activity on the callback
      verifyZeroInteractions(discoCallback);
    }

    // This final timeout should cause all callbacks to be invoked.
    manager.causeImmediateTimeout(transport.packets.remove());
    verify(discoCallback, times(expectedFailures)).onFailure(anyString());
    verify(discoCallback, never()).onSuccess(anyString());
    checkAndResetStats(0, 0, 1);  // failed

    // The next request should return a cached response.
    SuccessFailCallback<String, String> cachedDiscoCallback = createMockCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cachedDiscoCallback);
    verify(cachedDiscoCallback).onFailure(anyString());
    verify(cachedDiscoCallback, never()).onSuccess(anyString());

    // No more outgoing packets.
    assertEquals("Should not have sent more outgoing packets",
        expectedPackets, transport.packetsSent);
    checkAndResetStats(0, 0, 0);  // no additional varz
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server, and no
   * subsequent disco requests start after we get a successful reply.
   */
  public void testDiscoNoRetransmitsAfterReply() throws ExecutionException {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    checkAndResetStats(1, 0, 0);  // started
    assertEquals("Expected disco packet to be sent", 1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
    assertTrue(disco.isDiscoRequestPending(REMOTE_DOMAIN));

    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);
    discoItemsResult.setID(packet.getID());
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals("Expected disco info get to be sent", 2, transport.packetsSent);
    assertEquals(EXPECTED_DISCO_INFO_GET, transport.lastPacketSent.toString());

    // Check that we haven't yet finished - we should only get up to sending the items request.
    verifyZeroInteractions(discoCallback);
    assertTrue(disco.isDiscoRequestPending(REMOTE_DOMAIN));
    checkAndResetStats(0, 0, 0);  // no additional varz
  }

  /**
   * Tests stage 2 of disco. Inject a disco#items into the disco code, check it
   * calls disco#info on the JID.
   */
  public void testDiscoItemsResult() {
    initiateDiscoRequest();  // sends one packet.
    checkAndResetStats(1, 0, 0);  // started
    // create with wave, no pubsub
    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);

    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(2, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_INFO_GET, packet.toString());
    checkAndResetStats(0, 0, 0);  // no additional varz
  }

  /**
   * Tests stage 3 of disco. Inject a disco#info into the disco code (one that
   * matches wave) and check the callback gets run.
   */
  public void testDiscoInfoResultWave() {
    initiateDiscoRequest();  // sends one packet.
    checkAndResetStats(1, 0, 0);  // started
    // create with wave, no pubsub
    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);
    // Start the process.
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(2, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_INFO_GET, packet.toString());
    // create a wave disco result, inject into disco.
    manager.receivePacket(createDiscoInfo(true /* wave */));
    assertEquals(2, transport.packetsSent);
    verify(discoCallback).onSuccess(eq(REMOTE_JID));
    checkAndResetStats(0, 1, 0);  // success
  }

  /**
   * Tests stage 3 of disco. Inject a disco#info into the disco code (one that
   * doesn't match wave) and check callback gets run with null.
   */
  public void testDiscoInfoResultPubsub() {
    initiateDiscoRequest();  // sends one packet.
    checkAndResetStats(1, 0, 0);  // started
    transport.packets.remove(); // remove packet from queue

    // create with just pubsub
    IQ discoItemsResult = createDiscoItems(false /* not wave */, true /* pubsub */);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(3, transport.packetsSent);

    // Expect a wave request even if we didn't send it (automatic wave request)
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect pubsub packet
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET_PUBSUB, pubsubPacket.toString());

    // Create pubsub response, should not yet invoke callback
    manager.receivePacket(createDiscoInfo(false /* not wave */));
    verifyZeroInteractions(discoCallback);

    // Create response to wave request, with ITEM_NOT_FOUND
    IQ failWaveResponse = IQ.createResultIQ((IQ) wavePacket);
    failWaveResponse.setError(PacketError.Condition.item_not_found);
    manager.receivePacket(failWaveResponse);
    verify(discoCallback).onFailure(anyString());
    checkAndResetStats(0, 0, 1);  // failed

    // No more outgoing packets
    assertEquals(3, transport.packetsSent);
  }

  /**
   * Tests stage 3 of disco. Inject a disco#items into the disco code with
   * pubsub, then wave. Then give it pubsub's disco#info, and check it then
   * sends a disco#info for wave.
   */
  public void testDiscoInfoResultPubsubAndWave() {
    initiateDiscoRequest();  // sends one packet.
    checkAndResetStats(1, 0, 0);  // started

    transport.packets.remove(); // remove packet from queue

    // create with both pubsub and wave
    IQ discoItemsResult = createDiscoItems(true /* wave */, true /* pubsub */);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(3, transport.packetsSent);

    // Expect a wave request
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect pubsub packet
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET_PUBSUB, pubsubPacket.toString());

    // Create pubsub response, should not yet invoke callback
    manager.receivePacket(createDiscoInfo(false /* not wave */));
    verifyZeroInteractions(discoCallback);

    checkAndResetStats(0, 0, 0);  // not finished yet

    // Create response to wave request, with ITEM_NOT_FOUND
    manager.receivePacket(createDiscoInfo(true /* wave */));
    verify(discoCallback).onSuccess(eq(REMOTE_JID));

    checkAndResetStats(0, 1, 0);  // success

    // No more outgoing packets
    assertEquals(3, transport.packetsSent);
  }

  /**
   * Tests that if disco is started for a remote server for which we already
   * have the result, the cached result is just passed to the callback.
   */
  public void testDiscoStartWithCachedResult() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals(0, transport.packetsSent);
    verify(discoCallback).onSuccess(eq(REMOTE_JID));
    checkAndResetStats(0, 0, 0);  // no varz updated
  }

  /**
   * Tests that we return a (useless, empty) IQ for a disco#items.
   */
  public void testDiscoGetDiscoItems() {
    IQ request = createDiscoRequest(XmppNamespace.NAMESPACE_DISCO_ITEMS);
    manager.receivePacket(request);
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_RESULT, packet.toString());
  }

  /**
   * Tests that we return the right wave-identifying IQ for a disco#info.
   */
  public void testDiscoGetDiscoInfo() {
    IQ request = createDiscoRequest(XmppNamespace.NAMESPACE_DISCO_INFO);
    manager.receivePacket(request);
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_INFO_RESULT, packet.toString());
  }

  /**
   * Check the expiry of disco results behaves as expected when successful.
   */
  public void testDiscoCachedResultsExpiryOnSuccess() {
    DateTimeUtils.setCurrentMillisFixed(0);
    SuccessFailCallback<String, String> cb = createMockCallback();
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    checkAndResetStats(1, 0, 0);  // started once only
    assertEquals(1, transport.packetsSent);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(createDiscoItems(true /* wave */, false /* pubsub */));
    assertEquals(2, transport.packetsSent); // original items plus info
    manager.receivePacket(createDiscoInfo(true /* wave */));
    verify(cb).onSuccess(eq(REMOTE_JID));
    verify(cb, never()).onFailure(anyString());
    checkAndResetStats(0, 1, 0); // success

    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    // We shouldn't trigger disco again - we're in an OK state.
    cb = createMockCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    assertEquals(2, transport.packetsSent); // cached result - no more packets sent
    verify(cb).onSuccess(eq(REMOTE_JID));
    verify(cb, never()).onFailure(anyString());
    checkAndResetStats(0, 0, 0); // nothing

    // Time passes...
    tick((DISCO_SUCCESS_EXPIRY_SECS + 1) * 1000);

    cb = createMockCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    assertEquals(3, transport.packetsSent); // 1 more packet - disco restart
    checkAndResetStats(1, 0, 0); // started
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(createDiscoItems(true /* wave */, false /* pubsub */));
    assertEquals(4, transport.packetsSent); // 1 more packet - disco restart info packet
    manager.receivePacket(createDiscoInfo(true /* wave */));
    verify(cb).onSuccess(eq(REMOTE_JID));
    verify(cb, never()).onFailure(anyString());
    checkAndResetStats(0, 1, 0); // success
  }

  /**
   * Check the expiry of disco results behaves as expected when disco fails.
   * We send back wave and pubsub requests, identifying both as not wave. We
   * can't just send back pubsub, as the code in RemoteDisco always asks for
   * wave.foo.
   */
  public void testDiscoCachedResultsExpiryOnFailure() {
    DateTimeUtils.setCurrentMillisFixed(0);
    SuccessFailCallback<String, String> cb = createMockCallback();
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    assertEquals(1, transport.packetsSent);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    checkAndResetStats(1, 0, 0); // started
    manager.receivePacket(createDiscoItems(true /* wave */, true /* pubsub */));
    assertEquals(3, transport.packetsSent); // original items plus info
    manager.receivePacket(createDiscoInfo(false /* pubsub */));
    manager.receivePacket(createBrokenDiscoInfoForWaveJid());
    verify(cb, never()).onSuccess(anyString());
    verify(cb).onFailure(anyString());
    checkAndResetStats(0, 0, 1); // failed

    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    // We shouldn't trigger disco again - we're in a cached state.
    cb = createMockCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    assertEquals(3, transport.packetsSent); // cached result - no more packets sent
    verify(cb, never()).onSuccess(anyString());
    verify(cb).onFailure(anyString());
    checkAndResetStats(0, 0, 0); // nothing

    // Time passes...
    tick((DISCO_FAIL_EXPIRY_SECS + 1) * 1000);

    cb = createMockCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    checkAndResetStats(1, 0, 0); // started
    assertEquals(4, transport.packetsSent); // 1 more packet - disco restart
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(createDiscoItems(true /* wave */, true /* pubsub */));
    assertEquals(6, transport.packetsSent); // 2 more packet - disco restart info packet
    manager.receivePacket(createDiscoInfo(false /* pubsub */));
    manager.receivePacket(createBrokenDiscoInfoForWaveJid());

    verify(cb, never()).onSuccess(anyString());
    verify(cb).onFailure(anyString());
    checkAndResetStats(0, 0, 1); // failed
  }

  /**
   * Tests that if a disco items requests fails due to some error, that we still
   * perform a disco info request on fallback JIDs.
   */
  public void testDiscoItemsFallback() {
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals("Should have sent disco packet", 1, transport.packetsSent);
    checkAndResetStats(1, 0, 0);  // started

    // Generate an error response.
    IQ errorResponse = IQ.createResultIQ((IQ) transport.packets.poll());
    errorResponse.setError(PacketError.Condition.conflict);
    manager.receivePacket(errorResponse);

    // Confirm that two outgoing packets are sent.
    assertEquals(3, transport.packetsSent);

    // Expect a wave request
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect packet targeted at TLD
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(REMOTE_DOMAIN, pubsubPacket.getTo().toBareJID());
    checkAndResetStats(0, 0, 0);  // not finished yet
  }

  /**
   * Tests sending multiple disco requests result in multiple callbacks.
   */
  public void testMultipleDiscoRequestsToSameDomain() {
    final int CALL_COUNT = 10;
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    List<SuccessFailCallback<String, String>> callbacks = Lists.newLinkedList();
    for (int i = 0; i < CALL_COUNT; i++) {
      SuccessFailCallback<String, String> cb = createMockCallback();
      assertTrue(callbacks.add(cb));
      disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    }
    // Expect only one disco request to be sent.
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_DOMAIN, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());

    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(createDiscoItems(true /* wave */, true /* pubsub */));
    manager.receivePacket(createDiscoInfo(true /* wave */));

    for(SuccessFailCallback<String, String> cb : callbacks) {
      verify(cb).onSuccess(eq(REMOTE_JID));
      verify(cb, never()).onFailure(anyString());
    }
  }

  /**
   * Create a disco#info result from the remote server.
   *
   * @param forWaveJID if true, it's for the remote Wave JID, else it's the
   *                   remote pubsub JID.
   * @return the new IQ packet.
   */
  private IQ createDiscoInfo(boolean forWaveJID) {
    IQ response = new IQ(IQ.Type.result);
    response.setTo(LOCAL_JID);
    response.setID(DISCO_INFO_ID);
    Element query = response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);

    if (forWaveJID) {
      response.setFrom(REMOTE_JID);
      query.addElement("identity")
          .addAttribute("category", MockDisco.config.getString("federation.disco_info_category"))
        .addAttribute("type",  MockDisco.config.getString("federation.disco_info_type"))
          .addAttribute("name", SERVER_DESCRIPTION);
      query.addElement("feature")
          .addAttribute("var", XmppNamespace.NAMESPACE_WAVE_SERVER);
    } else {
      response.setFrom(REMOTE_PUBSUB_JID);
      query.addElement("identity")
          .addAttribute("category", "pubsub")
          .addAttribute("type", "whatever")
          .addAttribute("name", "not a wave server");
      query.addElement("feature")
          .addAttribute("var", XmppNamespace.NAMESPACE_PUBSUB);
    }
    return response;
  }

  /**
   * Create a wave.other.com info result that identifies it as non-wave. needed to force
   * failure in the case of the wave.foo fallback.
   * @return the new IQ result packet
   */
  private IQ createBrokenDiscoInfoForWaveJid() {
    IQ response = new IQ(IQ.Type.result);
    response.setTo(LOCAL_JID);
    response.setID(DISCO_INFO_ID);
    Element query = response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);
    response.setFrom(REMOTE_JID);
    query.addElement("identity")
        .addAttribute("category", "pubsub")
        .addAttribute("type", "whatever")
        .addAttribute("name", "not a wave server");
    query.addElement("feature")
        .addAttribute("var", XmppNamespace.NAMESPACE_PUBSUB);
    return response;
  }

  /**
   * Create a disco#items result, with either or both of a pubsub and a wave
   * JID.
   *
   * @param wave   if true, create a wave JID item.
   * @param pubsub if true, create a pubsub JID item.
   * @return the new IQ packet.
   */
  private IQ createDiscoItems(boolean wave, boolean pubsub) {
    IQ discoItemsResult = new IQ(IQ.Type.result);
    discoItemsResult.setFrom(REMOTE_DOMAIN);
    discoItemsResult.setTo(LOCAL_JID);
    discoItemsResult.setID(DISCO_ITEMS_ID);
    Element discoBody =
        discoItemsResult.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);
    if (wave) {
      discoBody.addElement("item").addAttribute("jid", REMOTE_JID);
    }
    if (pubsub) {
      discoBody.addElement("item").addAttribute("jid", REMOTE_PUBSUB_JID);
    }
    return discoItemsResult;
  }

  /**
   * Create a disco#info or disco#items query.
   *
   * @param namespace the namespace of the query - disco#info or disco#items
   * @return the new IQ packet
   */
  private IQ createDiscoRequest(String namespace) {
    IQ request = new IQ(IQ.Type.get);
    switch (namespace) {
      case XmppNamespace.NAMESPACE_DISCO_ITEMS:
        request.setID(DISCO_ITEMS_ID);
        break;
      case XmppNamespace.NAMESPACE_DISCO_INFO:
        request.setID(DISCO_INFO_ID);
        break;
      default:
        throw new IllegalArgumentException();
    }
    request.setTo(LOCAL_JID);
    request.setFrom(REMOTE_JID);
    request.setChildElement("query", namespace);
    return request;
  }

  @SuppressWarnings("unchecked")
  private SuccessFailCallback<String, String> createMockCallback() {
    return mock(SuccessFailCallback.class);
  }

  private void checkAndResetStats(int started, int success, int failed) {
    assertEquals("start counter", started, counterStarted.getAndSet(0));
    assertEquals("success counter", success, counterSuccess.getAndSet(0));
    assertEquals("failed counter", failed, counterFailed.getAndSet(0));
  }

   private void resetVarz() {
    counterStarted.getAndSet(0);
    counterSuccess.getAndSet(0);
    counterFailed.getAndSet(0);
  }

  /**
   * Advance the clock.
   *
   * @param millis milliseconds to advance clock
   */
  private void tick(int millis) {
    DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + millis);
  }

  /**
   * Initiate a simple disco request to REMOTE_DOMAIN.
   */
  private void initiateDiscoRequest() {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals("Disco packet should have been sent", 1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
  }
}
