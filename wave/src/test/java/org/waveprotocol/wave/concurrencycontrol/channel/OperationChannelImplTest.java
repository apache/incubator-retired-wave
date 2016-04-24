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

package org.waveprotocol.wave.concurrencycontrol.channel;


import junit.framework.TestCase;

import org.waveprotocol.wave.common.logging.PrintLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.concurrencycontrol.client.ConcurrencyControl;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Tests OperationChannelImpl by mocking the underlying WaveletDeltaChannel.
 *
 * TODO(anorth): this is really tests for the channel+CC combination. Create
 * some simpler tests with a fake CC too.
 *
 */

public class OperationChannelImplTest extends TestCase {

  ///////////////////// PRIVATES //////////////////////

  private static final ParticipantId USER_ID = ParticipantId.ofUnsafe("test@example.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(USER_ID);
  private static final byte[] SIG1 = new byte[] { 1, 1, 1, 1 };
  private static final byte[] SIG2 = new byte[] { 2, 2, 2, 2 };
  private static final byte[] SIG3 = new byte[] { 3, 3, 3, 3 };
  private static final byte[] SIG4 = new byte[] { 4, 4, 4, 4 };
  private static final byte[] SIG5 = new byte[] { 5, 5, 5, 5 };

  private MockWaveletDeltaChannel deltaChannel;
  private ConcurrencyControl cc;
  private OperationChannelImpl operationChannel;
  private final MockOperationChannelListener listener = new MockOperationChannelListener();

  ///////////////////// SETUP //////////////////////

  private static PrintLogger opLogger = new PrintLogger();
  private static PrintLogger ccLogger = new PrintLogger();

  static {
    // We should not be logging fatals in CC.
    opLogger.setAllowedMinLogLevel(Level.ERROR);
    ccLogger.setAllowedMinLogLevel(Level.ERROR);
  }

  @Override
  public void tearDown() {
    listener.clear();
  }

  ///////////////////// UTILITY METHODS FOR TESTS //////////////////////

  /**
   * Returns a Delta with the requested number of ops, version number
   * and signature.
   */
  private TransformedWaveletDelta createRandomTransformedDelta(int targetVersion, int opCount,
      byte[] hash) {
    return UTIL.makeTransformedDelta(0L, HashedVersion.of(targetVersion + opCount, hash), opCount);
  }

  /**
   * Connects the channel at the provided version.
   */
  private void connectChannel(long version, byte[] signature) throws ChannelException {
    final HashedVersion signatureInfo = HashedVersion.of(version, signature);
    deltaChannel = new MockWaveletDeltaChannel();
    cc = new ConcurrencyControl(ccLogger, signatureInfo);
    operationChannel = new OperationChannelImpl(opLogger, deltaChannel, cc,
        Accessibility.READ_WRITE);
    operationChannel.setListener(listener);
    operationChannel.onConnection(signatureInfo, signatureInfo);
  }

  /**
   * Reconnects the channel at the provided version, which is also
   * the current version.
   */
  private void reconnectChannel(long connectVersion, byte[] connectSignature)
      throws ChannelException {
    reconnectChannel(connectVersion, connectSignature, connectVersion, connectSignature, null);
  }

  /**
   * Fails and reconnects the channel at the provided connection version and
   * current version. The channel is expected to provide a distinct version
   * matching the connect version. If expectRetransmission is not null expects
   * the channel to attempt to send that message on reconnection.
   */
  private void reconnectChannel(long connectVersion, byte[] connectSignature,
      long currentVersion, byte[] currentSignature, WaveletDelta expectRetransmission)
      throws ChannelException {
    final HashedVersion connectHashedVersion = HashedVersion.of(connectVersion, connectSignature);
    final HashedVersion currentHashedVersion = HashedVersion.of(currentVersion, currentSignature);

    // Simulate failure elsewhere.
    operationChannel.reset();

    // Check reconnect versions provided by the channel include the
    // version we'll reconnect at.
    List<HashedVersion> reconnectVersions = operationChannel.getReconnectVersions();
    assertTrue(reconnectVersions.size() > 0);
    boolean matchedSignature = false;
    for (HashedVersion rcv : reconnectVersions) {
      if (connectVersion == rcv.getVersion() && connectSignature.equals(rcv.getHistoryHash())) {
        matchedSignature = true;
      }
    }
    assertTrue("No matching signature provided", matchedSignature);

    // Simulate reconnection reconnection message from delta channel.
    if (expectRetransmission != null) {
      deltaChannel.expectSend(expectRetransmission);
    }
    operationChannel.onConnection(connectHashedVersion, currentHashedVersion);
  }

  private WaveletDelta sendAndCheckRandomOp(OperationChannelImpl opChannel,
      long currentVersion, byte[] signature) throws ChannelException {
    WaveletDelta delta = UTIL.makeDelta(HashedVersion.of(currentVersion, signature), 0L, 1);

    deltaChannel.expectSend(delta);
    opChannel.send(new WaveletOperation[] {delta.get(0)});
    return delta;
  }

  private void checkExpectationsSatisfied() {
    deltaChannel.checkExpectationsSatisfied();
  }


  ///////////////////// TESTS //////////////////////

  /**
   * Test a simple connect.
   */
  public void testConnectSuccess() throws ChannelException {
    connectChannel(10, SIG1);
    listener.checkOpsReceived(0);
    checkExpectationsSatisfied();
  }

  /**
   * Test a simple connect then successful disconnection.
   */
  public void testConnectThenDisconnectSuccess() throws ChannelException {
    connectChannel(10, SIG1);
    listener.checkOpsReceived(0);

    // Disconnect
    operationChannel.reset();
    checkExpectationsSatisfied();
  }

  /**
   * Test simple reconnect.
   */
   public void testSimpleReconnect() throws ChannelException {
     connectChannel(10, SIG1);
     listener.checkOpsReceived(0);

     // Now disconnect server-side, forcing a reconnect.
     reconnectChannel(10, SIG1);

    // No new messages.
    listener.checkOpsReceived(0);
    checkExpectationsSatisfied();
   }

  /**
   * Test receiving a simple delta that was committed.
   */
  public void testSimpleReceive() throws ChannelException {
    final int initialVersion = 42;
    connectChannel(initialVersion, SIG1);

    operationChannel.onDelta(createRandomTransformedDelta(initialVersion, 1, SIG2));
    listener.checkOpsReceived(1);
    listener.clear();
    assertNotNull(operationChannel.receive());
    listener.checkOpsReceived(0);

    // TODO(anorth): test OnCommit by mocking CC
    operationChannel.onCommit(initialVersion + 1);
    checkExpectationsSatisfied();
  }

  public void testSimpleSendAndAck() throws Exception {
    final int initialVersion = 42;
    connectChannel(initialVersion, SIG1);

    sendAndCheckRandomOp(operationChannel, initialVersion, SIG1);
    listener.clear();

    HashedVersion signature = HashedVersion.of(43, SIG2);
    operationChannel.onAck(1, signature);

    // Listener should receive a version update op.
    listener.checkOpsReceived(1);
    WaveletOperation op = operationChannel.receive();
    WaveletOperationContext context = op.getContext();
    assertEquals(1, context.getVersionIncrement());
    assertEquals(signature, context.getHashedVersion());
  }

  /**
   * Test simple NACK and onDelta.
   * Nack is not supposed to happen, so it should be death.
   */
  public void testSimpleNackAndReceive() throws Exception {
    final int initialVersion = 42;
    connectChannel(initialVersion, SIG1);

    sendAndCheckRandomOp(operationChannel, initialVersion, SIG1);
    listener.clear();

    try {
      operationChannel.onNack(ResponseCode.OK, null, 43);
      fail("Should have thrown ChannelException");
    } catch (ChannelException expected) {
    }

    checkExpectationsSatisfied();
  }

  public void testSendToInaccessibleChanneFails() throws ChannelException {
    final HashedVersion connectSig = HashedVersion.unsigned(0);
    deltaChannel = new MockWaveletDeltaChannel();
    cc = new ConcurrencyControl(ccLogger, connectSig);
    operationChannel = new OperationChannelImpl(opLogger, deltaChannel, cc,
        Accessibility.READ_ONLY);
    operationChannel.setListener(listener);
    operationChannel.onConnection(connectSig, connectSig);

    try {
      sendAndCheckRandomOp(operationChannel, connectSig.getVersion(), connectSig.getHistoryHash());
      fail("Expected a channel exception");
    } catch (ChannelException expected) {
    }
  }

  public void testReconnectAfterCommit() throws Exception {
    final int initialVersion = 42;
    connectChannel(initialVersion, SIG1);

    sendAndCheckRandomOp(operationChannel, 42, SIG1);
    operationChannel.onAck(1, HashedVersion.of(43, SIG2));
    // Take the fake op resulting from ack that updates version info.
    listener.checkOpsReceived(1);
    assertNotNull(operationChannel.receive());
    listener.clear();

    operationChannel.onCommit(43); // Causes cc to remove old operations from memory
    reconnectChannel(43, SIG2);

    // Receive next delta.
    TransformedWaveletDelta delta = createRandomTransformedDelta(43, 1, SIG3);
    operationChannel.onDelta(delta);
    listener.checkOpsReceived(1);
    assertNotNull(operationChannel.receive());
    assertNull(operationChannel.receive());
    checkExpectationsSatisfied();
  }

  /**
   * Test reconnect where there was an unACKed delta during the failure which
   * was not received by the server, expect the client to immediately resend the
   * delta.
   */
  public void testReconnectWithPendingAckNotRecievedByServer() throws Exception {
    final int initialVersion = 42;
    final byte[] ackSignature = SIG2;
    connectChannel(initialVersion, SIG1);

    WaveletDelta delta = sendAndCheckRandomOp(operationChannel, 42, SIG1);

    // The server's version is still 42 when it responds. Expect a retransmission.
    reconnectChannel(initialVersion, SIG1, initialVersion, SIG1, delta);

    // Now ack.
    listener.checkOpsReceived(0);
    operationChannel.onAck(1, HashedVersion.of(43, ackSignature));

    // There should be a fake op resulting from ack that updates version info
    listener.checkOpsReceived(1);
    checkExpectationsSatisfied();
  }

  /**
   * Test reconnect where there was an unACKed delta during the failure which
   * was received and ACKed by the server, but this was unseen by the client. On
   * reconnect, the server responds with that delta and a new one, the client
   * should receive only the new one.
   */
  public void testReconnectWithPendingAckAndNewDeltaSentByServer() throws Exception {
    final int initialVersion = 42;
    final byte[] initialSignature = SIG1;
    connectChannel(initialVersion, initialSignature);

    WaveletDelta delta = sendAndCheckRandomOp(operationChannel, 42, SIG1);

    // Reconnect at the version before that delta, but with a later
    // current version including it.
    final long afterVersion = 44;
    final byte[] afterSignature = SIG3;
    reconnectChannel(initialVersion, initialSignature, afterVersion, afterSignature, null);

    // Receive delta that is the client's own un-acked op.
    TransformedWaveletDelta serverDelta =
        TransformedWaveletDelta.cloneOperations(HashedVersion.of(43, SIG2), 0L, delta);
    operationChannel.onDelta(serverDelta);

    // It should implicitly cause an ack, so read that fake op
    // to update version info, plus a VersionUpdateOp.
    listener.checkOpsReceived(2);
    assertNotNull(operationChannel.receive());
    listener.clear();

    // Receive another op
    TransformedWaveletDelta rdelta = createRandomTransformedDelta(43, 1, SIG4);
    operationChannel.onDelta(rdelta);
    // Recovery should be complete since the client has caught up with the
    // server, and there should be one readable delta.
    listener.checkOpsReceived(1);
    assertNotNull(operationChannel.receive());
    checkExpectationsSatisfied();
  }

  /**
   * Test reconnect where there was an ACKed and an unACKed delta, the server
   * loses state and accepted a delta from another client assigning the same
   * version number. Expect failure.
   */
  public void testReconnectFailureWithAckAndPendingAckWithCollidingVersionNumber()
      throws Exception {
    final int initialVersion = 42;
    final byte[] initialSignature = SIG1;
    final byte[] ackSignature = SIG2;
    connectChannel(initialVersion, initialSignature);

    // Send an op and ack it.
    sendAndCheckRandomOp(operationChannel, 42, SIG1);
    operationChannel.onAck(1, HashedVersion.of(43, ackSignature));
    // Take the fake op resulting from ack that updates version info.
    listener.checkOpsReceived(1);
    assertNotNull(operationChannel.receive());
    listener.clear();

    // Send an op, then fail & reconnect.
    // Note(anorth): this makes no sense. Why would the server reconnect
    // at 43 with the same acked signature if it lost state.
    sendAndCheckRandomOp(operationChannel, 43, ackSignature);
    reconnectChannel(43, ackSignature, 47, SIG3, null);

    // Receive a different delta to the first one the client sent.
    try {
      operationChannel.onDelta(createRandomTransformedDelta(42, 5, SIG4));
      fail("Should have thrown ChannelException");
    } catch (ChannelException expected) {
    }

    checkExpectationsSatisfied();
  }

  /**
   * Tests reconnection where the server does not provide a matching
   * signature. Expect failure.
   */
  public void testReconnectionFailure() throws ChannelException {
    final int initialVersion = 42;
    final byte[] initialSignature = SIG1;
    connectChannel(initialVersion, initialSignature);

    // Simulate failure
    operationChannel.reset();

    // Server presents unknown reconnect version
    final HashedVersion reconnectVersion = HashedVersion.of(66, SIG2);

    try {
      operationChannel.onConnection(reconnectVersion, reconnectVersion);
      fail("Should have thrown ChannelException");
    } catch (ChannelException expected) {
    }
  }

  /**
   * Test reconnect where client sends a delta to the server and the server
   * forgets that delta on reconnection. Then check life can continue as normal
   * after reconnection.
   */
  public void testReconnectWherePendingAckCollisionCanBeRecovered() throws Exception {
    final int initialVersion = 42;
    final byte[] initialSignature = SIG1;
    connectChannel(initialVersion, initialSignature);

    // Receive a delta to resets the recovery mechanism so the next
    // recovery will be at version 43.
    final byte[] signature43 = SIG2;
    operationChannel.onDelta(createRandomTransformedDelta(42, 1, signature43));
    assertNotNull(operationChannel.receive());
    listener.clear();

    // Send a delta.
    sendAndCheckRandomOp(operationChannel, 43, signature43);

    // Reconnect at 43.
    reconnectChannel(43, signature43, 44, SIG3, null);

    // Now receive a different delta to the un-acked delta1.
    TransformedWaveletDelta delta2 = createRandomTransformedDelta(43, 1, SIG4);
    operationChannel.onDelta(delta2);
    listener.checkOpsReceived(1);
    assertNotNull(operationChannel.receive());
    listener.clear();

    // Push another delta to the channel, but don't expect a delta channel
    // send yet.
    WaveletDelta delta3 = UTIL.makeDelta(HashedVersion.of(45, SIG5), 0L, 1);
    operationChannel.send(new WaveletOperation[] { delta3.get(0) });
    listener.checkOpsReceived(0);

    // Ack the first op, and expect a send for the pending op3.
    deltaChannel.expectSend(delta3);
    operationChannel.onAck(1, HashedVersion.of(45, SIG5));
    checkExpectationsSatisfied();
  }

  /**
   * Check that the client sends the correct last known signatures.
   */
  public void testReconnectWhereAnAckWasNotCommittedCanBeRecovered() throws Exception {
    final int initialVersion = 42;
    connectChannel(initialVersion, SIG1);

    // Send op and expect an Ack.
    byte[] signature43 = SIG2;
    byte[] signature44 = SIG3;
    sendAndCheckRandomOp(operationChannel, 42, SIG1);
    operationChannel.onAck(1, HashedVersion.of(43, signature43));
    // Send another op and expect an Ack.
    WaveletDelta delta2 = sendAndCheckRandomOp(operationChannel, 43, signature43);
    operationChannel.onAck(1, HashedVersion.of(44, signature44));
    // send commit for first Ack.
    operationChannel.onCommit(43);

    // Check that we reconnect, the server went down and has a LCV of 43.
    // Channel will resend delta2.
    reconnectChannel(43, signature43, 43, signature43, delta2);
    deltaChannel.checkExpectationsSatisfied();
    checkExpectationsSatisfied();
  }

// TODO(zdwang): The test below is too complicated to read now. My head hurts. Fix later, or remove
// as thorough test of recovery is already done in OT3Test
//
//  private class Pair<A, B> {
//    A first;
//    B second;
//    Pair (A first, B second) {
//      this.first = first;
//      this.second = second;
//    }
//  }
//
//  /**
//   * Method used by tests for recovery where we have sent a number of deltas
//   * all of which have been ACKed, then reconnect.
//   * We expect retransmission of all those deltas that the server has
//   * received/not forgotten, the client retransmits the rest.
//   *
//   * @param initialServerVersion version the server starts on.
//   * @param ackedClientOps number of deltas sent by client & ACKed by server.
//   * @param numberOfDeltasRememberedByServer number of deltas that the server remembers
//            after crash.
//   * @param disconnectDuringStartup forces client to disconnect during recovery when in start up.
//   * @param disconnectWhileRetransmitting forces client to disconnect while retransmitting
//   *        previously ACKed deltas.
//   * @param mergeStartupDeltas allows the server may merge adjacent deltas.
//   * @param useCommitDuringStartup makes the server indicate a commit during the startup.
//   * @param useCommitWhileRetransmitting makes the server indicate a commit while retransmitting
//   *        previously ACKed deltas.
//   */
//  private void reconnectWithRetransmissionOfPreviouslyAckedDeltas(
//      int initialServerVersion,
//      int ackedClientOps,
//      int numberOfDeltasRememberedByServer,
//      boolean disconnectDuringStartup,
//      boolean disconnectWhileRetransmitting,
//      boolean mergeStartupDeltas,
//      boolean useCommitDuringStartup,
//      boolean useCommitWhileRetransmitting) {
//
//    assert numberOfDeltasRememberedByServer >= 0 &&
//        numberOfDeltasRememberedByServer <= ackedClientOps;
//
//    int reconnectVersion = initialServerVersion;
//    int reconnectSignature = randomInt();
//    WaveletDeltaChannel.Receiver oldReceiver = utilCheckAndCompleteConnect(
//        operationChannel.getDeltaChannel(), sig(reconnectVersion, reconnectSignature));
//    int version = reconnectVersion;
//    // Store each op together with a signature value.
//    ArrayList<Pair<WaveOpMessage, Integer>> savedInitialOps =
//      new ArrayList<Pair<WaveOpMessage, Integer>>();
//    for (int i = 0; i < ackedClientOps; i++) {
//      int signature = randomInt();
//      // TODO(jochen): once we have better control over how ops are collated into deltas, do more
//      // than one op per delta.
//      savedInitialOps.add(new Pair<WaveOpMessage, Integer>(
//          utilSendAndCheckRndOp(operationChannel, version), signature));
//
//      version += 1;  // Update to be the version AFTER the sent delta is applied.
//      oldReceiver.onAck(1, sig(version, signature));
//    }
//    if (numberOfDeltasRememberedByServer > 0) {
//      reconnectSignature = savedInitialOps.get(numberOfDeltasRememberedByServer - 1).second;
//    }
//
//    int postStartUpServerVersion = initialServerVersion + numberOfDeltasRememberedByServer;
//    int finalServerVersion = initialServerVersion + ackedClientOps;
//    do {
//      // Make a local copy used for retransmission.
//      ArrayList<Pair<WaveOpMessage, Integer>> initialOps =
//        new ArrayList<Pair<WaveOpMessage, Integer>>(savedInitialOps);
//
//      listener.clear();
//      callback.clear();
//      // The server may have remembered some deltas.
//      WaveletDeltaChannel.Receiver receiver = utilDisconnectReconnect(oldReceiver,
//          sig(reconnectVersion, reconnectSignature),
//          sig(postStartUpServerVersion, reconnectSignature));
//
//      MockWaveletDeltaChannel deltaChannel = operationChannel.getDeltaChannel();
//
//      // The server sends the deltas it remembered, optionally merging them.
//      for (int receiveVersion = reconnectVersion; receiveVersion < postStartUpServerVersion;) {
//        int signature = -1;
//        ArrayList<WaveOpMessage> ops = new ArrayList<WaveOpMessage>();
//        for (int i = 0; i < (mergeStartupDeltas ? 2 : 1) && receiveVersion + ops.size() <
//            postStartUpServerVersion; ++i) {
//          Pair<WaveOpMessage, Integer> opSignaturePair = initialOps.remove(0);
//          ops.add(opSignaturePair.first);
//          signature = opSignaturePair.second;  // Always use the last signature.
//        }
//        assert ops.size() > 0;
//        WaveDeltaMessage delta = utilCreateDeltaMessage(receiveVersion, signature, 0L, ops);
//        receiver.onDelta(delta);
//
//        callback.assertCounts(0, 0);
//        assertNull(operationChannel.receive());
//        receiveVersion += delta.getOpListSize();
//
//        if (useCommitDuringStartup) {
//          reconnectVersion = receiveVersion;
//          reconnectSignature = signature;
//          receiver.onCommit(reconnectVersion);
//          for (int i = 0; i < delta.getOpListSize(); i++) {
//            savedInitialOps.remove(0);  // On reconnect, the ops so far will not be sent.
//          }
//        }
//
//        if (disconnectDuringStartup) {
//          continue;  // break out of loop
//        }
//      }
//      if (disconnectDuringStartup) {
//        disconnectDuringStartup = false;
//        // Check to make sure that if we break out at startup, that we still check there
//        // was a send
//        // if we expected one.
//        if (postStartUpServerVersion < finalServerVersion) {
//          deltaChannel.checkSend();
//        }
//        continue;  // back to top.
//      }
//
//      // The client retransmits the remainder, so ACK each one.
//      for (int rexmitVersion = postStartUpServerVersion; rexmitVersion < finalServerVersion;
//          ++rexmitVersion) {
//        utilCheckOpsSent(operationChannel, rexmitVersion, initialOps.get(0).first);
//
//        receiver.onAck(1, sig(rexmitVersion + 1, randomInt()));  // ACK the op.
//        if (useCommitWhileRetransmitting) {
//          reconnectVersion = rexmitVersion + 1;
//          receiver.onCommit(reconnectVersion);
//          reconnectSignature = initialOps.get(0).second;
//          savedInitialOps.remove(0);  // When we try to reconnect,
//                                      // the ops so far will not be sent.
//        }
//        initialOps.remove(0);
//
//        if (disconnectWhileRetransmitting) {
//          continue;  // break out of loop
//        }
//      }
//      deltaChannel.allChecked();
//      if (disconnectWhileRetransmitting) {
//        disconnectWhileRetransmitting = false;
//        continue;  // back to top.
//      }
//      // The condition below is a dummy, both should be false by the time we get here. It saves
//      // me from using break here.
//    } while(disconnectWhileRetransmitting || disconnectDuringStartup);
//    operationChannel.getDeltaChannel().allChecked();
//  }
//
//
//  // Work around the test harness problem of timing out a test running too long. The hack is to
//  // test the last boolean variant in a separate test to prevent the test thread
//  // from timing out :(
//  // Any other suggestions for fixing this ?
//  /**
//   * Drive variants of the reconnect and retransmission test.
//   */
//  public void reconnectWithRetranmissionOfPreviouslyAckedDeltasWithVariations(boolean lastBool) {
//    final int maxNumberRemembered = 8;
//    // Test the range of numberRemembered deltas by the server against the disconnect
//    // combinations.
//    for (int boolVariants = 0; boolVariants < 16; boolVariants++) {
//      for (int numberRemembered = 0; numberRemembered <= maxNumberRemembered; numberRemembered +=
//          4) {
//        reconnectWithRetransmissionOfPreviouslyAckedDeltas(40,
//            maxNumberRemembered, numberRemembered, (boolVariants & 1) > 0,
//            (boolVariants & 2) > 0, (boolVariants & 4) > 0,
//            (boolVariants & 8) > 0, lastBool);
//        tearDown();
//        setUp();
//      }
//    }
//    // Check the spurious connect we did in the trailing setUp()
//    operationChannel.getDeltaChannel().checkConnect();
//  }
//
//  /**
//   * Force GWT to run these as two separate tests.
//   */
//  public void testReconnectWithRetranmissionOfPreviouslyAckedDeltasA() {
//    reconnectWithRetranmissionOfPreviouslyAckedDeltasWithVariations(false);
//  }
//
//  public void testReconnectWithRetranmissionOfPreviouslyAckedDeltasB() {
//    reconnectWithRetranmissionOfPreviouslyAckedDeltasWithVariations(true);
//  }
}
