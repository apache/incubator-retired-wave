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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;
import org.dom4j.Element;
import org.mockito.ArgumentCaptor;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.ProtocolHashedVersionFactory;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationListener.WaveletUpdateCallback;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.WaveletFederationProvider.DeltaSignerInfoResponseListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider.HistoryResponseListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider.PostSignerInfoResponseListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider.SubmitResultListener;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link XmppFederationRemote}.
 *
 * TODO(thorogood,arb): This class actually test round-trips sent from an
 * XmppFederationRemote to a XmppFederationHost.
 *
 * @author arb@google.com (Anthony Baxter)
 * @author thorogood@google.com (Sam Thorogood)
 */

public class XmppFederationRemoteTest extends TestCase {

  private final static String LOCAL_DOMAIN = "acmewave.com";
  private final static String LOCAL_JID = "wave." + LOCAL_DOMAIN;
  private final static String REMOTE_DOMAIN = "initech-corp.com";
  private final static String REMOTE_JID = "wave." + REMOTE_DOMAIN;

  private final static WaveletName REMOTE_WAVELET =
      WaveletName.of(WaveId.of(REMOTE_DOMAIN, "wave"), WaveletId.of(REMOTE_DOMAIN, "wavelet"));
  private final static ProtocolHashedVersion START_VERSION =
      ProtocolHashedVersionFactory.createVersionZero(REMOTE_WAVELET);
  private final static ByteString DELTA_BYTESTRING =
      ByteString.copyFromUtf8("Irrelevant delta bytes");
  private final static ProtocolHashedVersion VERSION_ONE =
      ProtocolHashedVersionFactory.create(DELTA_BYTESTRING, START_VERSION, 1);

  private final static ProtocolSignedDelta DUMMY_SIGNED_DELTA =
      ProtocolSignedDelta.newBuilder().setDelta(ByteString.copyFromUtf8("fake blahblah")).build();

  private final static String TEST_ID = "1-1-sometestID";

  private final static ByteString FAKE_SIGNER_ID = ByteString.copyFromUtf8("Hello Signer!");
  private final static ProtocolSignerInfo FAKE_SIGNER_INFO = ProtocolSignerInfo.newBuilder()
      .setHashAlgorithm(ProtocolSignerInfo.HashAlgorithm.SHA256)
      .setDomain(REMOTE_DOMAIN)
      .addCertificate(ByteString.copyFromUtf8("Test certificate")).build();

  private MockOutgoingPacketTransport transport;
  private WaveletFederationListener.Factory mockUpdateListenerFactory;
  private MockDisco disco;
  private XmppManager manager;

  private WaveletFederationProvider mockProvider;
  private WaveletFederationListener mockUpdateListener;

  // The remote represents the 'caller' for all unit tests in this class.
  private XmppFederationRemote remote;

  // The host represents the 'callee' for all unit tests in this class.
  private XmppFederationHost host;

  private static final String EXPECTED_RECEIPT_MESSAGE =
          "\n<message id=\"" + TEST_ID + "\" to=\"" + REMOTE_JID + "\""
                  + " from=\"" + LOCAL_JID + "\">\n"
                  + "  <received xmlns=\"urn:xmpp:receipts\"/>\n"
                  + "</message>";

  private static final String EXPECTED_SUBMIT_REQUEST;
  private static final String EXPECTED_HISTORY_REQUEST;

  static {
    try {
      String uri = XmppUtil.waveletNameCodec.waveletNameToURI(REMOTE_WAVELET);
      EXPECTED_SUBMIT_REQUEST =
          "\n<iq type=\"set\" id=\"" + TEST_ID + "\" from=\"" + LOCAL_JID + "\"" +
                " to=\"" + REMOTE_JID + "\">\n"
          + "  <pubsub xmlns=\"http://jabber.org/protocol/pubsub\">\n"
          + "    <publish node=\"wavelet\">\n"
          + "      <item>\n"
          + "        <submit-request xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\">\n"
          + "          <delta wavelet-name=\"" + uri + "\">" +
                "<![CDATA[" + Base64Util.encode(DUMMY_SIGNED_DELTA) + "]]></delta>\n"
          + "        </submit-request>\n"
          + "      </item>\n"
          + "    </publish>\n"
          + "  </pubsub>\n"
          + "</iq>";

      EXPECTED_HISTORY_REQUEST =
          "\n<iq type=\"get\" id=\"" + TEST_ID + "\" from=\"" + LOCAL_JID + "\"" +
                " to=\"" + REMOTE_JID + "\">\n"
          + "  <pubsub xmlns=\"http://jabber.org/protocol/pubsub\">\n"
          + "    <items node=\"wavelet\">\n"
          + "      <delta-history xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
          + " start-version=\"" + START_VERSION.getVersion() + "\""
          + " start-version-hash=\"" + Base64Util.encode(START_VERSION.getHistoryHash()) + "\""
          + " end-version=\"" + VERSION_ONE.getVersion() + "\""
          + " end-version-hash=\"" + Base64Util.encode(VERSION_ONE.getHistoryHash()) + "\""
          + " wavelet-name=\"" + uri + "\"/>\n"
          + "    </items>\n"
          + "  </pubsub>\n"
          + "</iq>";
    } catch (EncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setUp() {
    XmppUtil.fakeUniqueId = TEST_ID;

    mockProvider = mock(WaveletFederationProvider.class);
    mockUpdateListener = mock(WaveletFederationListener.class);
    mockUpdateListenerFactory = mock(WaveletFederationListener.Factory.class);

    when(mockUpdateListenerFactory.listenerForDomain(eq(REMOTE_DOMAIN)))
        .thenReturn(mockUpdateListener);

    // Create mockDisco. It wants an XmppManager, but we don't need to set it here.
    disco = new MockDisco();

    transport = new MockOutgoingPacketTransport();
    remote = new XmppFederationRemote(mockUpdateListenerFactory, disco,
               ConfigFactory.parseString("federation.xmpp_jid : " + LOCAL_JID));
    host = new XmppFederationHost(mockProvider, disco,
             ConfigFactory.parseString("federation.xmpp_jid : " + REMOTE_JID));
    manager = new XmppManager(host, remote, disco, transport,
                ConfigFactory.parseString("federation.xmpp_jid : " + LOCAL_JID));

    remote.setManager(manager);
  }

  /**
   * Tests that the constructor behaves as expected.
   */
  public void testConstructor() {
    assertEquals(0, transport.packetsSent);
  }

  /**
   * Tests that a submit request from a local wave server is sent out to the
   * foreign federation host, and that the response from it is passed back to
   * the wave server.
   */
  public void testSubmitRequest() {
    int OPS_APPLIED = 1;
    long TIMESTAMP_APPLIED = 123;
    ProtocolHashedVersion APPLIED_AT = ProtocolHashedVersion.newBuilder()
        .setVersion(VERSION_ONE.getVersion() + OPS_APPLIED)
        .setHistoryHash(ByteString.copyFromUtf8("blah")).build();

    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);

    SubmitResultListener listener = mock(SubmitResultListener.class);
    remote.submitRequest(REMOTE_WAVELET, DUMMY_SIGNED_DELTA, listener);
    verifyZeroInteractions(listener);
    assertEquals(1, transport.packetsSent);

    // Validate the outgoing request.
    IQ outgoingRequest = (IQ) transport.packets.poll();
    assertEquals(EXPECTED_SUBMIT_REQUEST, outgoingRequest.toString());

    // Send the outgoing request back to the manager, so it hooks up to the
    // Federation Host.
    manager.receivePacket(outgoingRequest);

    // Provide the remote's host with a dummy answer to verified input.
    ArgumentCaptor<SubmitResultListener> remoteListener =
        ArgumentCaptor.forClass(SubmitResultListener.class);
    verify(mockProvider)
        .submitRequest(eq(REMOTE_WAVELET), eq(DUMMY_SIGNED_DELTA), remoteListener.capture());
    remoteListener.getValue().onSuccess(OPS_APPLIED, APPLIED_AT, TIMESTAMP_APPLIED);

    // Confirm that the packet has been sent back out over the transport.
    assertEquals(2, transport.packetsSent);
    IQ historyResponse = (IQ) transport.packets.poll();
    manager.receivePacket(historyResponse);

    // Confirm that the success is finally delivered to the listener.
    verify(listener, never()).onFailure(any(FederationError.class));
    verify(listener)
        .onSuccess(eq(OPS_APPLIED), any(ProtocolHashedVersion.class), eq(TIMESTAMP_APPLIED));
  }

  /**
   * Tests that that a submit request sent out can properly process a resulting
   * error.
   */
  public void testSubmitRequestError() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);

    SubmitResultListener listener = mock(SubmitResultListener.class);
    remote.submitRequest(REMOTE_WAVELET, DUMMY_SIGNED_DELTA, listener);

    verifyZeroInteractions(listener);
    assertEquals(1, transport.packetsSent);

    // Validate the outgoing request.
    IQ outgoingRequest = (IQ) transport.packets.poll();
    assertEquals(EXPECTED_SUBMIT_REQUEST, outgoingRequest.toString());

    // Return a confusing error response (<registration-required>).
    IQ errorResponse = IQ.createResultIQ(outgoingRequest);
    errorResponse.setError(PacketError.Condition.registration_required);
    manager.receivePacket(errorResponse);

    // Confirm error is passed through to the callback.
    ArgumentCaptor<FederationError> error = ArgumentCaptor.forClass(FederationError.class);
    verify(listener).onFailure(error.capture());
    verify(listener, never())
        .onSuccess(anyInt(), any(ProtocolHashedVersion.class), anyLong());
    assertEquals(FederationError.Code.UNDEFINED_CONDITION, error.getValue().getErrorCode());
  }

  /**
   * Tests that a submit request doesn't fall over if disco fails, but instead
   * passes an error back to the wave server.
   */
  public void testSubmitRequestDiscoFailed() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, null);

    SubmitResultListener listener = mock(SubmitResultListener.class);
    ProtocolSignedDelta signedDelta =
        ProtocolSignedDelta.newBuilder().setDelta(ByteString.copyFromUtf8("fake")).build();
    remote.submitRequest(REMOTE_WAVELET, signedDelta, listener);
    verify(listener).onFailure(any(FederationError.class));
    verify(listener, never())
        .onSuccess(anyInt(), any(ProtocolHashedVersion.class), anyLong());
  }

  /**
   * Tests that a history request from a local wave server is sent out to the
   * foreign federation host, and that the response from it is passed back to
   * the wave server.
   */
  public void testHistoryRequest() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);

    // Send the outgoing request. Assert that a packet is sent and that no
    // callbacks have been invoked.
    HistoryResponseListener listener = mock(HistoryResponseListener.class);
    remote.requestHistory(REMOTE_WAVELET, REMOTE_DOMAIN, START_VERSION, VERSION_ONE, -1, listener);
    verifyZeroInteractions(listener);
    assertEquals(1, transport.packetsSent);

    // Validate the outgoing request.
    IQ outgoingRequest = (IQ) transport.packets.poll();
    assertEquals(EXPECTED_HISTORY_REQUEST, outgoingRequest.toString());

    // Send the outgoing request back to the manager, so it hooks up to the
    // Federation Host.
    manager.receivePacket(outgoingRequest);

    ArgumentCaptor<HistoryResponseListener> remoteListener =
        ArgumentCaptor.forClass(HistoryResponseListener.class);
    // TODO(thorogood): Note that the caller's JID is not the domain we expect
    // here - it is not actually the domain of the requester!
    verify(mockProvider).requestHistory(eq(REMOTE_WAVELET), eq(LOCAL_JID), eq(START_VERSION),
        eq(VERSION_ONE), anyInt(), remoteListener.capture());
    remoteListener.getValue().onSuccess(ImmutableList.of(DELTA_BYTESTRING), VERSION_ONE, 0);

    // Confirm that the packet has been sent back out over the transport.
    assertEquals(2, transport.packetsSent);
    IQ historyResponse = (IQ) transport.packets.poll();
    manager.receivePacket(historyResponse);

    // Confirm that the success is finally delivered to the listener.
    ArgumentCaptor<ProtocolHashedVersion> commitVersion =
      ArgumentCaptor.forClass(ProtocolHashedVersion.class);
    verify(listener, never()).onFailure(any(FederationError.class));
    verify(listener).onSuccess(eq(ImmutableList.of(DELTA_BYTESTRING)),
        commitVersion.capture(), anyInt());

    // Confirm that the returned commit notice matches the expected value.
    // TODO(thorogood): We don't transfer the history hash over the wire.
    assertEquals(VERSION_ONE.getVersion(), commitVersion.getValue().getVersion());
    assertEquals(ByteString.EMPTY, commitVersion.getValue().getHistoryHash());
  }

  /**
   * Helper method wrapping an unchecked mock conversion.
   */
  @SuppressWarnings("unchecked")
  private static List<ByteString> anyListByteString() {
    return any(List.class);
  }

  /**
   * Tests that a submit request doesn't fall over if disco fails, but instead
   * passes an error back to the wave server.
   */
  public void testHistoryRequestDiscoFailed() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, null);

    HistoryResponseListener listener = mock(HistoryResponseListener.class);
    ProtocolSignedDelta signedDelta =
        ProtocolSignedDelta.newBuilder().setDelta(ByteString.copyFromUtf8("fake")).build();
    remote.requestHistory(REMOTE_WAVELET, REMOTE_DOMAIN, START_VERSION, VERSION_ONE, -1, listener);
    verify(listener).onFailure(any(FederationError.class));
    verify(listener, never())
        .onSuccess(anyListByteString(), any(ProtocolHashedVersion.class), anyLong());
  }

  /**
   * Test a successful get signer.
   */
  public void testGetSigner() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);

    // Send the outgoing request. Assert that a packet is sent and that no
    // callbacks have been invoked.
    DeltaSignerInfoResponseListener listener = mock(DeltaSignerInfoResponseListener.class);
    remote.getDeltaSignerInfo(FAKE_SIGNER_ID, REMOTE_WAVELET, VERSION_ONE, listener);
    verifyZeroInteractions(listener);
    assertEquals(1, transport.packetsSent);

    // Validate the outgoing request.
    IQ outgoingRequest = (IQ) transport.packets.poll();
    //assertEquals(EXPECTED_HISTORY_REQUEST, outgoingRequest.toString());

    // Send the outgoing request back to the manager, so it hooks up to the
    // Federation Host.
    manager.receivePacket(outgoingRequest);

    ArgumentCaptor<DeltaSignerInfoResponseListener> remoteListener =
        ArgumentCaptor.forClass(DeltaSignerInfoResponseListener.class);
    verify(mockProvider).getDeltaSignerInfo(eq(FAKE_SIGNER_ID), eq(REMOTE_WAVELET), eq(VERSION_ONE),
        remoteListener.capture());
    remoteListener.getValue().onSuccess(FAKE_SIGNER_INFO);

    // Confirm that the packet has been sent back out over the transport.
    assertEquals(2, transport.packetsSent);
    IQ historyResponse = (IQ) transport.packets.poll();
    manager.receivePacket(historyResponse);

    // Confirm that the success is finally delivered to the listener.
    verify(listener, never()).onFailure(any(FederationError.class));
    verify(listener).onSuccess(eq(FAKE_SIGNER_INFO));
  }

  /**
   * Test a successful post signer.
   */
  public void testPostSigner() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);

    // Send the outgoing request. Assert that a packet is sent and that no
    // callbacks have been invoked.
    PostSignerInfoResponseListener listener = mock(PostSignerInfoResponseListener.class);
    remote.postSignerInfo(REMOTE_DOMAIN, FAKE_SIGNER_INFO, listener);
    verifyZeroInteractions(listener);
    assertEquals(1, transport.packetsSent);

    // Validate the outgoing request.
    IQ outgoingRequest = (IQ) transport.packets.poll();
    //assertEquals(EXPECTED_HISTORY_REQUEST, outgoingRequest.toString());

    // Send the outgoing request back to the manager, so it hooks up to the
    // Federation Host.
    manager.receivePacket(outgoingRequest);

    ArgumentCaptor<PostSignerInfoResponseListener> remoteListener =
        ArgumentCaptor.forClass(PostSignerInfoResponseListener.class);
    verify(mockProvider).postSignerInfo(eq(REMOTE_DOMAIN), eq(FAKE_SIGNER_INFO),
        remoteListener.capture());
    remoteListener.getValue().onSuccess();

    // Confirm that the packet has been sent back out over the transport.
    assertEquals(2, transport.packetsSent);
    IQ historyResponse = (IQ) transport.packets.poll();
    manager.receivePacket(historyResponse);

    // Confirm that the success is finally delivered to the listener.
    verify(listener, never()).onFailure(any(FederationError.class));
    verify(listener).onSuccess();
  }

  /**
   * Tests an update message containing both a delta and commit notice from a
   * foreign federation host is correctly decoded and passed to the Update
   * Listener Factory, and a response is sent as requested.
   */
  public void testUpdate() throws EncodingException {
    Message updateMessage = new Message();
    Element waveletUpdate = addWaveletUpdate(updateMessage, true); // request receipt
    waveletUpdate.addElement("applied-delta").addCDATA(Base64Util.encode(DELTA_BYTESTRING));
    waveletUpdate.addElement("commit-notice")
        .addAttribute("version", String.valueOf(VERSION_ONE.getVersion()))
        .addAttribute("history-hash", Base64Util.encode(VERSION_ONE.getHistoryHash()));

    manager.receivePacket(updateMessage);

    ArgumentCaptor<WaveletUpdateCallback> deltaCallback =
        ArgumentCaptor.forClass(WaveletUpdateCallback.class);
    List<ByteString> expected = ImmutableList.of(DELTA_BYTESTRING);
    verify(mockUpdateListener).waveletDeltaUpdate(eq(REMOTE_WAVELET), eq(expected),
        deltaCallback.capture());

    deltaCallback.getValue().onSuccess();
    assertEquals(0, transport.packetsSent); // Callback has only been invoked once.

    ArgumentCaptor<WaveletUpdateCallback> commitCallback =
      ArgumentCaptor.forClass(WaveletUpdateCallback.class);
    verify(mockUpdateListener).waveletCommitUpdate(eq(REMOTE_WAVELET), eq(VERSION_ONE),
        commitCallback.capture());

    commitCallback.getValue().onSuccess();
    assertEquals(1, transport.packetsSent); // Callback has been invoked twice, expect receipt.
    assertEquals(EXPECTED_RECEIPT_MESSAGE, transport.lastPacketSent.toString());
  }

  /**
   * Test that a single update message, where a receipt is not requested, is
   * correctly received and processed.
   */
  public void testUpdateNoReceipt() throws EncodingException {
    Message updateMessage = new Message();
    Element waveletUpdate = addWaveletUpdate(updateMessage, false);
    waveletUpdate.addElement("applied-delta").addCDATA(Base64Util.encode(DELTA_BYTESTRING));

    manager.receivePacket(updateMessage);

    ArgumentCaptor<WaveletUpdateCallback> deltaCallback =
        ArgumentCaptor.forClass(WaveletUpdateCallback.class);
    List<ByteString> expected = ImmutableList.of(DELTA_BYTESTRING);
    verify(mockUpdateListener).waveletDeltaUpdate(eq(REMOTE_WAVELET), eq(expected),
        deltaCallback.capture());

    deltaCallback.getValue().onSuccess();
    assertEquals(0, transport.packetsSent); // Do not expect a callback.
  }

  /**
   * Add a single wavelet-update message to the given Message. Should (probably)
   * not be called twice on the same Message.
   */
  private Element addWaveletUpdate(Message updateMessage, boolean requestReceipt)
      throws EncodingException {
    updateMessage.setFrom(REMOTE_JID);
    updateMessage.setTo(LOCAL_JID);
    updateMessage.setID(TEST_ID);
    if (requestReceipt) {
      updateMessage.addChildElement("request", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);
    }
    Element event = updateMessage.addChildElement("event", XmppNamespace.NAMESPACE_PUBSUB_EVENT);
    Element waveletUpdate =
        event.addElement("items").addElement("item").addElement("wavelet-update");
    waveletUpdate.addAttribute("wavelet-name",
        XmppUtil.waveletNameCodec.waveletNameToURI(REMOTE_WAVELET));
    return waveletUpdate;
  }
}
