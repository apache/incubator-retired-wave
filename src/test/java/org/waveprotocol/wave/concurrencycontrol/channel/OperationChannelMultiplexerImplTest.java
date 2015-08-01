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

import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.PrintLogger;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer.KnownWavelet;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl.LoggerContext;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListenerFactory;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.testing.FakeHashedVersionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ImmediateExcecutionScheduler;
import org.waveprotocol.wave.model.util.Scheduler;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test for the multiplexer.
 *
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */

public class OperationChannelMultiplexerImplTest extends TestCase {

  // TODO(anorth): this really tests the whole client end of the stack.
  // Make some simpler tests just for the mux.

  /**
   * Holds information about a single channel from a mux.
   */
  private static final class ConnectionInfo {
    public final WaveletId waveletId;
    public final long initialVersion;
    public final byte[] initialSignature;
    public final ObservableWaveletData snapshot;
    public final HashedVersion initialHashedVersion;

    public ConnectionInfo(WaveletId waveletId, long initialVersion, byte[] initialSignature) {
      this.waveletId = waveletId;
      this.initialVersion = initialVersion;
      this.initialSignature = initialSignature;
      this.snapshot = createSnapshot(waveletId, initialVersion, initialSignature);
      this.initialHashedVersion = HashedVersion.of(initialVersion, initialSignature);
    }
  }

  private static final class ConnectedChannel {
    public final OperationChannel channel;
    public final MockOperationChannelListener listener;

    public ConnectedChannel(OperationChannel channel, MockOperationChannelListener listener) {
      this.channel = channel;
      this.listener = listener;
    }
  }

  private static class FakeScheduler implements Scheduler {
    Scheduler.Command command;

    @Override
    public void reset() {
      // Do nothing
    }

    @Override
    public boolean schedule(Command command) {
      this.command = command;
      return true;
    }
  }

  /** IDs of wavelets for testing. */
  private static final WaveletId WAVELET_ID_1 = WaveletId.of("example.com","w+1234");
  private static final WaveletId WAVELET_ID_2 = WaveletId.of("example.com","w+5678");

  /** ID of wave for testing. */
  private static final WaveId WAVE_ID = WaveId.of("example.com", "waveId_1");

  /** User name for testing. */
  private static final ParticipantId USER_ID = ParticipantId.ofUnsafe("fred@example.com");

  private static final byte[] NOSIG = new byte[0];
  private static final byte[] SIG1 = new byte[] { 1, 1, 1, 1 };
  private static final byte[] SIG2 = new byte[] { 2, 2, 2, 2 };
  private static final byte[] SIG3 = new byte[] { 3, 3, 3, 3 };
  private static final byte[] SIG4 = new byte[] { 4, 4, 4, 4 };
  private static final byte[] SIG5 = new byte[] { 5, 5, 5, 5 };
  private static final byte[] SIG6 = new byte[] { 6, 6, 6, 6 };
  private static final byte[] SIG7 = new byte[] { 7, 7, 7, 7 };


  private static final Map<WaveletId, List<HashedVersion>> NO_KNOWN_WAVELETS =
    Collections.<WaveletId, List<HashedVersion>>emptyMap();

  private static final AbstractLogger logger = new PrintLogger();

  private static final LoggerContext LOGGERS = new LoggerContext(logger, logger, logger, logger);
  private static final DeltaTestUtil testUtil = new DeltaTestUtil(USER_ID);
  private static final ObservableWaveletData.Factory<?> DATA_FACTORY =
      BasicFactories.waveletDataImplFactory();

  private OperationChannelMultiplexerImpl mux;
  private MockViewChannel.Factory viewFactory;
  private MockMuxListener muxListener;

  @Override
  public void setUp() {
    ViewChannelImpl.setMaxViewChannelsPerWave(Integer.MAX_VALUE);
    viewFactory = new MockViewChannel.Factory();
    UnsavedDataListenerFactory fakeListenerFactory = new UnsavedDataListenerFactory() {

      @Override
      public UnsavedDataListener create(WaveletId waveletId) {
        return null;
      }

      @Override
      public void destroy(WaveletId waveletId) {
      }
    };
    mux = new OperationChannelMultiplexerImpl(WAVE_ID, viewFactory, DATA_FACTORY, LOGGERS,
        fakeListenerFactory, new ImmediateExcecutionScheduler(), FakeHashedVersionFactory.INSTANCE);
    muxListener = new MockMuxListener();
  }

  public void testMuxOpenOpensView() {
    // Connect to the server.
    MockViewChannel viewChannel = viewFactory.expectCreate();
    viewChannel.expectOpen(IdFilters.ALL_IDS, NO_KNOWN_WAVELETS);
    mux.open(muxListener, IdFilters.ALL_IDS);
    viewChannel.takeListener().onConnected();
    viewChannel.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testReceivedSnapshotOpensChannel() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    muxListener.verifyNoMoreInteractions();
    viewListener.onConnected();

    ConnectedChannel opChannel = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);
    viewChannel.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
    // Also proves that an expected (initial) snapshot doesn't clobber the channel
  }

  public void testReceivedSnapshotClobbersExistingChannel() throws ChannelException {
    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    muxListener.verifyNoMoreInteractions();
    viewListener.onConnected();

    ObservableWaveletData snapshotUpdate = createSnapshot(WAVELET_ID_1, 1, SIG1);
    HashedVersion committed = HashedVersion.unsigned(0);

    viewListener.onSnapshot(WAVELET_ID_1, snapshotUpdate, committed, null);
    OperationChannel ch = muxListener.verifyOperationChannelCreated(snapshotUpdate,
        Accessibility.READ_WRITE);

    // The second snapshot may have a non-zero version.
    committed = HashedVersion.unsigned(1);
    ch = checkSendClobberingSnapshot(viewListener, ch, snapshotUpdate, committed);

    // Repeat the clobbering with a higher version.
    committed = HashedVersion.unsigned(2000);
    checkSendClobberingSnapshot(viewListener, ch, snapshotUpdate, committed);
  }

  public void testReceivedSnapshotClobbersOnlyAppropriateChannel() throws ChannelException {
    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    muxListener.verifyNoMoreInteractions();
    viewListener.onConnected();

    ObservableWaveletData snapshotUpdate = createSnapshot(WAVELET_ID_1, 1, SIG1);
    HashedVersion committed = HashedVersion.unsigned(0);

    viewListener.onSnapshot(WAVELET_ID_1, snapshotUpdate, committed, null);
    OperationChannel ch =
        muxListener.verifyOperationChannelCreated(snapshotUpdate, Accessibility.READ_WRITE);

    // This snapshot should clobber wavelet 1's channel
    committed = HashedVersion.of(1, SIG1);
    checkSendClobberingSnapshot(viewListener, ch, snapshotUpdate, committed);
    muxListener.verifyNoMoreInteractions();

    // This snapshot should only create a new channel and nothing else
    viewListener.onSnapshot(WAVELET_ID_2, snapshotUpdate, committed, null);
    muxListener.verifyOperationChannelCreated(snapshotUpdate, Accessibility.READ_WRITE);
    muxListener.verifyNoMoreInteractions();
  }

  public void testOpsReceivedAndChannelClobbered() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final int serverOps = 1;
    final byte[] finalSignature = SIG2;

    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    viewListener.onConnected();
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);

    // Receive a delta.
    checkReceiveDelta(viewListener, ch.channel, ch.listener, WAVELET_ID_1,
        chInfo.initialVersion, serverOps, finalSignature);
    viewChannel.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();

    // Now receive a snapshot and it should clobber the existing channel
    HashedVersion committed = HashedVersion.of(1000000L, SIG3);
    ObservableWaveletData update = createSnapshot(WAVELET_ID_1, committed.getVersion(),
        committed.getHistoryHash());
    checkSendClobberingSnapshot(viewListener, ch.channel, update, committed);
  }

  public void testOpReceivedOnChannel() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final int serverOps = 1;
    final byte[] finalSignature = SIG2;

    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    viewListener.onConnected();
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);

    // Receive a delta.
    checkReceiveDelta(viewListener, ch.channel, ch.listener, WAVELET_ID_1,
        chInfo.initialVersion, serverOps, finalSignature);
    viewChannel.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testChannelSendSubmitsToView() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel viewChannel = openMux();
    ViewChannel.Listener viewListener = viewChannel.takeListener();
    viewListener.onConnected();

    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);

    // Send an operation and check view submission
    WaveletOperation op = createOp();
    WaveletDelta delta = createDelta(chInfo.initialHashedVersion, op);
    viewChannel.expectSubmitDelta(WAVELET_ID_1, delta);
    ch.channel.send(op);
    viewChannel.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testAckResultsInChannelOp() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final byte[] finalSignature = SIG2;

    MockViewChannel view = openMux();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);

    checkSendDelta(view, ch.channel, chInfo.initialHashedVersion, WAVELET_ID_1);
    checkAckDelta(view, ch.channel, ch.listener, 1, 2, finalSignature);
    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testMultipleChannelsAreIndependent() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final ConnectionInfo chInfo2 = new ConnectionInfo(WAVELET_ID_2, 20, SIG2);

    MockViewChannel view = openMux();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshot.
    ConnectedChannel ch1 = connectChannelSnapshot(viewListener, chInfo1);
    ConnectedChannel ch2 = connectChannelSnapshot(viewListener, chInfo2);
    triggerAndCheckOpenFinished(viewListener);

    // Check channels receive ops independently.
    final int serverOps1 = 5;
    checkReceiveDelta(viewListener, ch1.channel, ch1.listener, WAVELET_ID_1, chInfo1.initialVersion,
        serverOps1, SIG4);
    ch2.listener.checkOpsReceived(0);

    final int serverOps2 = 7;
    checkReceiveDelta(viewListener, ch2.channel, ch2.listener, WAVELET_ID_2, chInfo2.initialVersion,
        serverOps2, SIG5);
    ch1.listener.checkOpsReceived(0);

    // Check channels send ops independently.
    checkSendDelta(view, ch1.channel, HashedVersion.of(chInfo1.initialVersion + serverOps1, SIG4),
        WAVELET_ID_1);
    checkSendDelta(view, ch2.channel, HashedVersion.of(chInfo2.initialVersion + serverOps2, SIG5),
        WAVELET_ID_2);

    // Check acks are received independently.
    final byte[] ackSignature1 = SIG6;
    final byte[] ackSignature2 = SIG7;
    final long ackVersion1 = chInfo1.initialVersion + serverOps1 + 1;
    final long ackVersion2 = chInfo2.initialVersion + serverOps2 + 1;
    checkAckDelta(view, ch1.channel, ch1.listener, 1, ackVersion1, ackSignature1);
    checkAckDelta(view, ch2.channel, ch2.listener, 1, ackVersion2, ackSignature2);
    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testMuxCloseClosesViewAndChannels() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final ConnectionInfo chInfo2 = new ConnectionInfo(WAVELET_ID_2, 20, SIG2);

    MockViewChannel view = openMux();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch1 = connectChannelSnapshot(viewListener, chInfo1);
    ConnectedChannel ch2 = connectChannelSnapshot(viewListener, chInfo2);
    triggerAndCheckOpenFinished(viewListener);

    view.expectClose();
    mux.close();

    // Receive lagging delta from view channel, expect nothing.
    final List<TransformedWaveletDelta> update = createServerDeltaList(1, 1, SIG4);
    viewListener.onUpdate(chInfo1.waveletId, update, null, null);
    ch1.listener.checkOpsReceived(0);

    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testMuxFailCallbackThrowsException() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    OperationChannelMultiplexer.Listener closingListener =
        new OperationChannelMultiplexer.Listener() {
          public void onFailed(CorruptionDetail detail) {
            // Attempt to close the mux *during* the failure callback.
            mux.close();
          }

          public void onOpenFinished() {
          }

          public void onOperationChannelCreated(OperationChannel channel,
              ObservableWaveletData snapshotMetadata,
              Accessibility accessibility) {
          }

          public void onOperationChannelRemoved(
              OperationChannel channel, WaveletId waveletId) {
          }
        };

    MockViewChannel view = viewFactory.expectCreate();
    view.expectOpen(IdFilters.ALL_IDS, NO_KNOWN_WAVELETS);
    mux.open(muxListener, IdFilters.ALL_IDS);

    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshot.
    ConnectedChannel ch1 = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // Fail a channel, which will fail the mux.
    ObservableWaveletData update = createSnapshot(WAVELET_ID_1, 1, SIG1);
    try {
      viewListener.onSnapshot(WAVELET_ID_1, update, null, null);
      fail("Expected exception on bad first message");
    } catch (ChannelException ex) {
      // Expected
    }
  }

  /**
   * Tests that the mux ignores known wavelets that don't match the
   * wavelet filter, hence will never receive updates from the server.
   */
  public void testOpenWithKnownWaveletsIgnoresFilteredWavelets() {
    long knownVersion = 40;
    byte[] knownSig = SIG1;
    IdFilter onlyWavelet1 = IdFilter.ofPrefixes("w+1");

    ObservableWaveletData knownSnapshot1 = createSnapshot(WAVELET_ID_1, knownVersion, knownSig);
    ObservableWaveletData knownSnapshot2 = createSnapshot(WAVELET_ID_2, 0, NOSIG);

    MockViewChannel view = viewFactory.expectCreate();
    Map<WaveletId, List<HashedVersion>> expectedSigs = createKnownVersions(WAVELET_ID_1,
        knownVersion, knownSig);
    view.expectOpen(onlyWavelet1, expectedSigs);
    mux.open(muxListener, onlyWavelet1, Arrays.asList(
        createKnownWavelet(knownSnapshot1, knownVersion, knownSig, Accessibility.READ_WRITE),
        createKnownWavelet(knownSnapshot2, 0, NOSIG, Accessibility.READ_WRITE)));
    view.checkExpectationsSatisified();
  }

  public void testOpenWithKnownWaveletWaitsForReconnection() throws ChannelException {
    long knownVersion = 40;
    byte[] knownSig = SIG1;

    ObservableWaveletData knownSnapshot = createSnapshot(WAVELET_ID_1, knownVersion, knownSig);
    MockViewChannel view = openMuxWithKnownWavelet(knownSnapshot);

    // The channel is "connected" though the underlying view isn't.
    ConnectedChannel ch = expectConnectedChannel(knownSnapshot, Accessibility.READ_WRITE);
    checkOpenFinished();

    // Attempt to send a client op. Submission should be held until the view
    // connects and the channel receives the empty reconnection delta.
    WaveletOperation clientOp = createOp();
    WaveletDelta delta = createDelta(HashedVersion.of(knownVersion, knownSig), clientOp);
    ch.channel.send(clientOp);
    ch.listener.checkOpsReceived(0);

    // Connect the underlying view.
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive the reconnection delta, expect the client delta submission.
    view.expectSubmitDelta(WAVELET_ID_1, delta);
    reconnectChannel(viewListener, WAVELET_ID_1, knownVersion, knownSig);
    // Don't expect this empty delta from the channel.
    ch.listener.checkOpsReceived(0);
    assertNull(ch.channel.receive());

    checkAckDelta(view, ch.channel, ch.listener, 1, knownVersion + 1, SIG2);

    // No snapshot, but check we can receive and send deltas on the channel.
    checkReceiveAndSend(viewListener, view, ch, WAVELET_ID_1, knownVersion + 1);
    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  /**
   * Tests that the mux dies if the server doesn't respond with a reconnection
   * version for every reconnecting wavelet.
   */
  public void testOpenWithKnownWaveletsFailsIfServerDoesntKnow() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final ConnectionInfo chInfo2 = new ConnectionInfo(WAVELET_ID_2, 20, SIG2);

    Map<WaveletId, List<HashedVersion>> expectedSigs = createKnownVersions(WAVELET_ID_1,
        chInfo1.initialVersion, chInfo1.initialSignature, WAVELET_ID_2, chInfo2.initialVersion,
        chInfo2.initialSignature);
    Collection<KnownWavelet> knownWavelets = Arrays.asList(
        createKnownWavelet(chInfo1.snapshot, chInfo1.initialVersion, chInfo1.initialSignature,
            Accessibility.READ_WRITE),
        createKnownWavelet(chInfo2.snapshot, chInfo2.initialVersion, chInfo2.initialSignature,
            Accessibility.READ_WRITE));

    // Connect to the server.
    MockViewChannel view = viewFactory.expectCreate();
    view.expectOpen(IdFilters.ALL_IDS, expectedSigs);
    mux.open(muxListener, IdFilters.ALL_IDS, knownWavelets);

    // The mux appears "connected" though the underlying view isn't.
    ConnectedChannel ch1 = expectConnectedChannel(chInfo1.snapshot, Accessibility.READ_WRITE);
    ConnectedChannel ch2 = expectConnectedChannel(chInfo2.snapshot, Accessibility.READ_WRITE);
    checkOpenFinished();

    // Connect the underlying view.
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive one reconnection delta.
    final List<TransformedWaveletDelta> ch1reconnect = createServerDeltaList(
        chInfo1.initialVersion, 0, chInfo1.initialSignature);
    viewListener.onUpdate(WAVELET_ID_1, ch1reconnect,
        HashedVersion.of(chInfo1.initialVersion, chInfo1.initialSignature), null);

    // Receive openFinished before reconnection delta for second wavelet,
    // expect turbulence.
    try {
      viewListener.onOpenFinished();
      fail("Expected a channel exception");
    } catch (ChannelException ex) {
      // Expected.
    }
  }

  /**
   * Tests that the mux survives if the server doesn't respond with a reconnection
   * version for an inaccessible wavelet.
   */
  public void testOpenWithKnownWaveletsSucceedsIfServerDoesntKnowInaccessibleWavelet()
      throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final ConnectionInfo chInfo2 = new ConnectionInfo(WAVELET_ID_2, 20, SIG2);

    // Only the accessible wavelet will resync.
    Map<WaveletId, List<HashedVersion>> expectedSigs = createKnownVersions(WAVELET_ID_1,
        chInfo1.initialVersion, chInfo1.initialSignature);
    Collection<KnownWavelet> knownWavelets = Arrays.asList(
        createKnownWavelet(chInfo1.snapshot, chInfo1.initialVersion, chInfo1.initialSignature,
            Accessibility.READ_WRITE),
        createKnownWavelet(chInfo2.snapshot, chInfo2.initialVersion, chInfo2.initialSignature,
            Accessibility.INACCESSIBLE));

    // Connect to the server.
    MockViewChannel view = viewFactory.expectCreate();
    view.expectOpen(IdFilters.ALL_IDS, expectedSigs);
    mux.open(muxListener, IdFilters.ALL_IDS, knownWavelets);

    // The mux appears "connected" though the underlying view isn't.
    ConnectedChannel ch1 = expectConnectedChannel(chInfo1.snapshot, Accessibility.READ_WRITE);
    ConnectedChannel ch2 = expectConnectedChannel(chInfo2.snapshot, Accessibility.INACCESSIBLE);
    checkOpenFinished();

    // Connect the underlying view.
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive a reconnection delta for the accessible wavelet.
    final List<TransformedWaveletDelta> ch1reconnect = createServerDeltaList(
        chInfo1.initialVersion, 0, chInfo1.initialSignature);
    viewListener.onUpdate(WAVELET_ID_1, ch1reconnect,
        HashedVersion.of(chInfo1.initialVersion, chInfo1.initialSignature), null);

    // Receive openFinished in the absence of a reconnection delta for the
    // inaccessible wavelet, expect bliss.
    viewListener.onOpenFinished();

    // Check sending ops to the disconnected wavelet fails.
    WaveletOperation op = createOp();
    try {
      ch2.channel.send(op);
      fail("Expected a channel exception");
    } catch (ChannelException expected) {
    }

    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testNewWaveletSuppressesSnapshot() throws ChannelException {
    MockViewChannel view = openMux();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();
    triggerAndCheckOpenFinished(viewListener);

    ConnectedChannel ch = createOperationChannel(WAVELET_ID_1, USER_ID);

    // Send and ack first op.
    checkSendDelta(view, ch.channel, HashedVersion.unsigned(0), WAVELET_ID_1);
    view.checkExpectationsSatisified();
    checkAckDelta(view, ch.channel, ch.listener, 1, 1, SIG1);

    // Drop the empty snapshot sent by the server.
    ObservableWaveletData snapshot = createSnapshot(WAVELET_ID_1, 0, NOSIG);
    HashedVersion committed = HashedVersion.unsigned(0);
    viewListener.onSnapshot(snapshot.getWaveletId(), snapshot, committed, null);
    muxListener.verifyNoMoreInteractions();

    // Now CC should have sent the first client delta so it's acked.
    view.checkExpectationsSatisified();

    checkReceiveAndSend(viewListener, view, ch, WAVELET_ID_1, 1);
    view.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  public void testMuxReconnectsAfterDisconnect() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    final ConnectionInfo chInfo2 = new ConnectionInfo(WAVELET_ID_2, 20, SIG2);

    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch1 = connectChannelSnapshot(viewListener, chInfo1);
    ConnectedChannel ch2 = connectChannelSnapshot(viewListener, chInfo2);
    triggerAndCheckOpenFinished(viewListener);

    view.expectClose();
    MockViewChannel view2 = viewFactory.expectCreate();
    view2.expectOpen(IdFilters.ALL_IDS, createKnownVersions(WAVELET_ID_1, 1, SIG1,
        WAVELET_ID_2, 20, SIG2));
    viewListener.onException(new ChannelException("failed for testing", Recoverable.RECOVERABLE));

    ViewChannel.Listener viewListener2 = reconnectView(view2, chInfo1, chInfo2);
    // Don't expect the mux nor any channel to know about reconnection.
    muxListener.verifyNoMoreInteractions();
    ch1.listener.checkOpsReceived(0);
    ch2.listener.checkOpsReceived(0);

    // Check we can still receive and send deltas.
    checkReceiveAndSend(viewListener2, view2, ch1, WAVELET_ID_1, chInfo1.initialVersion);
    checkReceiveAndSend(viewListener2, view2, ch2, WAVELET_ID_2, chInfo2.initialVersion);

    // If a message is received on the old view it should be ignored
    viewListener.onUpdate(WAVELET_ID_1, createServerDeltaList(1, 1, SIG3), null,
        null);

    view.checkExpectationsSatisified();
    view2.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  private static enum KnownWaveletDisconnectWhen {
    BEFORE_VIEW_CONNECTED,
    AFTER_VIEW_CONNECTED,
    AFTER_RESYNC,
  }

  /**
   * Tests that a mux reconnects with known wavelets if the view channel fails
   * before the channel id message is received.
   */
  public void testMuxReconnectsKnownWaveletBeforeViewConnected() throws ChannelException {
    doTestMuxReconnectsKnownWavelet(KnownWaveletDisconnectWhen.BEFORE_VIEW_CONNECTED);
  }

  /**
   * Tests that a mux reconnects with known wavelets if the view channel fails
   * after the channel id but before the reconnection delta is received.
   */
  public void testMuxReconnectsKnownWaveletAfterViewConnected() throws ChannelException {
    doTestMuxReconnectsKnownWavelet(KnownWaveletDisconnectWhen.AFTER_VIEW_CONNECTED);
  }

  /**
   * Tests that a mux reconnects with known wavelets if the view channel fails
   * after the reconnection delta is received, but before any other deltas.
   */
  public void testMuxReconnectsKnownWaveletAfterResync() throws ChannelException {
    doTestMuxReconnectsKnownWavelet(KnownWaveletDisconnectWhen.AFTER_RESYNC);
  }

  /**
   * Helps test that a mux reconnects with known wavelets if the view channel
   * fails during reconnection.
   */
  private void doTestMuxReconnectsKnownWavelet(KnownWaveletDisconnectWhen when)
      throws ChannelException {
    ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);
    MockViewChannel view = openMuxWithKnownWavelet(chInfo.snapshot);
    ConnectedChannel ch = expectConnectedChannel(chInfo.snapshot, Accessibility.READ_WRITE);
    checkOpenFinished();

    ViewChannel.Listener viewListener = view.takeListener();
    if (when.compareTo(KnownWaveletDisconnectWhen.AFTER_VIEW_CONNECTED) >= 0) {
      viewListener.onConnected();
    }

    if (when.compareTo(KnownWaveletDisconnectWhen.AFTER_RESYNC) >= 0) {
    // Receive reconnection delta and open finished.
      reconnectChannel(viewListener, WAVELET_ID_1, chInfo.initialVersion, chInfo.initialSignature);
      viewListener.onOpenFinished();
      view.checkExpectationsSatisified();
    }

    // Fail view, expect reconnection.
    MockViewChannel view2 = failViewAndExpectReconnection(viewListener, view,
        "View failed after resync message", createKnownVersions(WAVELET_ID_1, 1, SIG1));
    reconnectViewAndCheckEverythingStillWorks(view2, chInfo, ch);
    muxListener.verifyNoMoreInteractions();
  }

  private static enum NewWaveletDisconnectWhen {
    BEFORE_VIEW_CONNECTED,
    AFTER_VIEW_CONNECTED,
    AFTER_SEND_DELTA,
    AFTER_ACK_DELTA,
  }

  /**
   * Tests that a mux reconnects with a new version-zero channel if the view
   * channel fails before it opens.
   */
  public void testMuxReconnectsNewWaveletBeforeViewConnected() throws ChannelException {
    doTestMuxReconnectsNewWavelet(NewWaveletDisconnectWhen.BEFORE_VIEW_CONNECTED);
  }

  /**
   * Tests that the mux reconnects at version zero when the server fails before
   * the client sends any ops on a wavelet.
   */
  public void testMuxReconnectsNewWaveletAfterViewConnected() throws ChannelException {
    doTestMuxReconnectsNewWavelet(NewWaveletDisconnectWhen.AFTER_VIEW_CONNECTED);
  }

  /**
   * Tests that the mux reconnects at version zero when the server fails and
   * loses the client's version-zero delta before acknowledging it, forgetting
   * the wavelet entirely.
   */
  public void testMuxReconnectsNewWaveletAfterFirstSend() throws ChannelException {
    doTestMuxReconnectsNewWavelet(NewWaveletDisconnectWhen.AFTER_SEND_DELTA);
  }

  /**
   * Tests that the mux reconnects at version zero when the server fails and
   * loses the client's version-zero delta, forgetting the wavelet entirely.
   */
  public void testMuxReconnectsNewWaveletWhenServerLosesFirstDelta() throws ChannelException {
    doTestMuxReconnectsNewWavelet(NewWaveletDisconnectWhen.AFTER_ACK_DELTA);
  }

  /**
   * Helps test that the mux reconnects at version zero for a locally-created
   * channel.
   */
  private void doTestMuxReconnectsNewWavelet(NewWaveletDisconnectWhen when)
      throws ChannelException {
    ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 0, NOSIG);
    byte[] deltaSig = SIG1;
    MockViewChannel view = openMux();
    ViewChannel.Listener viewListener = view.takeListener();

    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_VIEW_CONNECTED) >= 0) {
      viewListener.onConnected();
      triggerAndCheckOpenFinished(viewListener);
    }

    ConnectedChannel ch = createOperationChannel(WAVELET_ID_1, USER_ID);

    WaveletDelta clientDelta = null;
    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_SEND_DELTA) >= 0) {
      clientDelta = checkSendDelta(view, ch.channel, chInfo.initialHashedVersion, chInfo.waveletId);
    }

    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_ACK_DELTA) >= 0) {
      checkAckDelta(view,  ch.channel, ch.listener, 1, chInfo.initialVersion + 1, deltaSig);
    }

    // Fail the view.
    Map<WaveletId, List<HashedVersion>> expectedReconnectVersions = CollectionUtils.newHashMap();
    // Zero is always a resync version, even if no delta was submitted.
    expectedReconnectVersions.put(chInfo.waveletId, CollectionUtils.newArrayList(
        HashedVersion.unsigned(0)));
    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_ACK_DELTA) >= 0) {
      expectedReconnectVersions.get(chInfo.waveletId).add(HashedVersion.of(1, deltaSig));
    }
    MockViewChannel view2 = failViewAndExpectReconnection(viewListener, view,
        "View failed for testing", expectedReconnectVersions);

    // Reconnect the view with no channels - the server never knew about or
    // lost the wavelet.
    ViewChannel.Listener viewListener2 = view2.takeListener();
    viewListener2.onConnected();

    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_SEND_DELTA) >= 0) {
      // Expect the client delta to be resubmitted.
      view2.expectSubmitDelta(chInfo.waveletId, clientDelta);
    }
    viewListener2.onOpenFinished();
    view2.checkExpectationsSatisified();

    if (when.compareTo(NewWaveletDisconnectWhen.AFTER_SEND_DELTA) >= 0) {
      // (Re-)ack the resubmitted delta and check the channel works.
      checkAckDelta(view2, ch.channel, ch.listener, 1, chInfo.initialVersion + 1, deltaSig);
      checkReceiveAndSend(viewListener2, view2, ch, chInfo.waveletId, chInfo.initialVersion + 1);
    } else {
      // Check we can send on then channel.
//      checkSendDelta(view2, ch.channel, chInfo.initialVersion, chInfo.waveletId);
      checkReceiveAndSend(viewListener2, view2, ch, chInfo.waveletId, chInfo.initialVersion);
    }
  }

  public void testMuxReconnectUsingScheduler() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    FakeScheduler scheduler = new FakeScheduler();
    mux = new OperationChannelMultiplexerImpl(WAVE_ID, viewFactory, DATA_FACTORY,
        LOGGERS, null, scheduler, FakeHashedVersionFactory.INSTANCE);
    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // Send but don't ack delta.
    WaveletDelta delta =
        checkSendDelta(view, ch.channel, chInfo1.initialHashedVersion, WAVELET_ID_1);

    // No schedule yet
    assertNull(scheduler.command);

    // Reconnect channel.
    view.expectClose();
    viewListener.onClosed();

    // Check we've scheduled something.
    assertNotNull(scheduler.command);

    // scheduler call back
    MockViewChannel view2 = viewFactory.expectCreate();
    view2.expectOpen(IdFilters.ALL_IDS, createKnownVersions(WAVELET_ID_1, 1, SIG1));
    scheduler.command.execute();

    // Signal reconnected
    ViewChannel.Listener viewListener2 = view2.takeListener();
    viewListener2.onConnected();

    // Expect retransmit of the sent delta after reconnect.
    view2.expectSubmitDelta(WAVELET_ID_1, delta);
    reconnectChannel(viewListener2, WAVELET_ID_1, chInfo1.initialVersion, chInfo1.initialSignature);
    viewListener2.onOpenFinished();

  }

  public void testMuxReconnectUsingScheduleResetWithDelta() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    FakeScheduler scheduler = new FakeScheduler();
    mux = new OperationChannelMultiplexerImpl(WAVE_ID, viewFactory, DATA_FACTORY,
        LOGGERS, null, scheduler, FakeHashedVersionFactory.INSTANCE);
    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // No schedule yet
    assertNull(scheduler.command);

    // Reconnect 20 times.
    for (int i = 0; i < 20; i++) {
      // Reconnect channel.
      view.expectClose();
      viewListener.onClosed();

      // scheduler call back
      view = viewFactory.expectCreate();
      view.expectOpen(IdFilters.ALL_IDS, createKnownVersions(WAVELET_ID_1, 1, SIG1));
      assertNotNull(scheduler.command);
      scheduler.command.execute();
      scheduler.command = null;

      viewListener = view.takeListener();
      viewListener.onConnected();

      // Reconnection message
      reconnectChannel(viewListener, WAVELET_ID_1, chInfo1.initialVersion,
          chInfo1.initialSignature);
      viewListener.onOpenFinished();
    }
  }

  public void testMuxReconnectsAfterDisconnectWithOutstandingSubmit() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // Send but don't ack delta.
    WaveletDelta delta =
        checkSendDelta(view, ch.channel, chInfo1.initialHashedVersion, WAVELET_ID_1);

    // Reconnect channel.
    MockViewChannel view2 = failViewAndExpectReconnection(viewListener, view,
        "View failed with outstanding submit", createKnownVersions(WAVELET_ID_1, 1, SIG1));

    // Expect retransmit of the sent delta after reconnect.
    view2.expectSubmitDelta(WAVELET_ID_1, delta);
    ViewChannel.Listener viewListener2 = reconnectView(view2, chInfo1);
    muxListener.verifyNoMoreInteractions(); // No callback on reconnection.

    checkAckDelta(view2, ch.channel, ch.listener, 1, chInfo1.initialVersion + 1, SIG1);
    checkReceiveAndSend(viewListener2, view2, ch, WAVELET_ID_1, chInfo1.initialVersion + 1);

    // If the submit is then acked, it should be ignored.
    view.ackSubmit(1, chInfo1.initialVersion + 1, SIG5);

    view.checkExpectationsSatisified();
    view2.checkExpectationsSatisified();
    ch.listener.checkOpsReceived(0);
    muxListener.verifyNoMoreInteractions();
  }

  public void testMuxReconnectsAfterSubmitFailure() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // Send but don't ack delta.
    WaveletDelta delta =
        checkSendDelta(view, ch.channel, chInfo1.initialHashedVersion, WAVELET_ID_1);

    // Fail the submission, expecting reconnection.
    MockViewChannel view2 = failViewAndExpectReconnection(viewListener, view,
        "View failed with outstanding submit", createKnownVersions(WAVELET_ID_1, 1, SIG1));

    // Expect retransmit of the sent delta after reconnect.
    view2.expectSubmitDelta(WAVELET_ID_1, delta);
    ViewChannel.Listener viewListener2 = reconnectView(view2, chInfo1);
    muxListener.verifyNoMoreInteractions(); // No callback on reconnection.

    checkAckDelta(view2, ch.channel, ch.listener, 1, chInfo1.initialVersion + 1, SIG2);

    // If the first view later disconnects, it should be ignored
    viewListener.onException(new ChannelException("failed for testing", Recoverable.RECOVERABLE));

    checkReceiveAndSend(viewListener2, view2, ch, WAVELET_ID_1, chInfo1.initialVersion + 1);
    view.checkExpectationsSatisified();
    view2.checkExpectationsSatisified();
    ch.listener.checkOpsReceived(0);
    muxListener.verifyNoMoreInteractions();
  }

  public void testMuxFailsAfterChannelCorrupt() throws ChannelException {
    final ConnectionInfo chInfo1 = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo1);
    triggerAndCheckOpenFinished(viewListener);

    // Receive a message that should not be received.
    ObservableWaveletData update = createSnapshot(WAVELET_ID_1, 1, SIG1);
    view.expectClose();

    try {
      viewListener.onSnapshot(WAVELET_ID_1, update, null, null);
      fail("Expected exception corruption");
    } catch (ChannelException ex) {
      // Expected
    }
  }

  public void testMuxReconnectsAgainAfterReconnectFailure() throws ChannelException {
    final ConnectionInfo chInfo = new ConnectionInfo(WAVELET_ID_1, 1, SIG1);

    MockViewChannel view = openMux();
    muxListener.verifyNoMoreInteractions();
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    // Receive initial snapshots.
    ConnectedChannel ch = connectChannelSnapshot(viewListener, chInfo);
    triggerAndCheckOpenFinished(viewListener);

    // Disconnect, expect reconnection.
    view.expectClose();
    MockViewChannel view2 = viewFactory.expectCreate();
    view2.expectOpen(IdFilters.ALL_IDS, createKnownVersions(WAVELET_ID_1, 1, SIG1));
    viewListener.onClosed();
    ViewChannel.Listener viewListener2 = view2.takeListener();

    // Disconnect, expect reconnection again.
    view2.expectClose();
    MockViewChannel view3 = viewFactory.expectCreate();
    view3.expectOpen(IdFilters.ALL_IDS, createKnownVersions(WAVELET_ID_1, 1, SIG1));
    viewListener2.onClosed();

    view2.checkExpectationsSatisified();
    view3.checkExpectationsSatisified();
    muxListener.verifyNoMoreInteractions();
  }

  private static ObservableWaveletData createSnapshot(WaveletId waveletId, final long version,
      final byte[] signature) {
    final HashedVersion hv = HashedVersion.of(version, signature);
    return DATA_FACTORY.create(new EmptyWaveletSnapshot(WAVE_ID, waveletId, USER_ID, hv,
        1273307837000L));
  }

  private static TransformedWaveletDelta createReconnect(WaveId waveId, long connectVersion,
      byte[] connectSignature) {
    TransformedWaveletDelta delta = new TransformedWaveletDelta(USER_ID,
        HashedVersion.of(connectVersion, connectSignature), 0L,
        Collections.<WaveletOperation>emptyList());
    return delta;
  }

  private static KnownWavelet createKnownWavelet(ObservableWaveletData snapshot, long version,
      byte[] signature, Accessibility accessibility) {
    HashedVersion commitVersion = HashedVersion.of(version, signature);
    return new KnownWavelet(snapshot, commitVersion, accessibility);
  }

  private static List<TransformedWaveletDelta> createServerDeltaList(long version, int numOps,
      byte[] signature) {
    TransformedWaveletDelta delta =
        testUtil.makeTransformedDelta(0L, HashedVersion.of(version + numOps, signature), numOps);
    return Collections.singletonList(delta);
  }

  private static NoOp createOp() {
    return testUtil.noOp();
  }

  private static WaveletDelta createDelta(HashedVersion targetVersion, WaveletOperation... ops) {
    return new WaveletDelta(USER_ID, targetVersion, Arrays.asList(ops));
  }

  private static Map<WaveletId, List<HashedVersion>> createKnownVersions(
      WaveletId waveletId, long version, byte[] hash) {
    return CollectionUtils.immutableMap(waveletId, Collections.singletonList(
        HashedVersion.of(version, hash)));
  }

  private static Map<WaveletId, List<HashedVersion>> createKnownVersions(
      WaveletId waveletId1, long version1, byte[] hash1, WaveletId waveletId2,
      long version2, byte[] hash2) {
    assertFalse(waveletId1.equals(waveletId2));
    return CollectionUtils.immutableMap(
        waveletId1, Collections.singletonList(HashedVersion.of(version1, hash1)),
        waveletId2, Collections.singletonList(HashedVersion.of(version2, hash2)));
  }

  private OperationChannel checkSendClobberingSnapshot(ViewChannel.Listener viewListener,
      OperationChannel channelToClobber, ObservableWaveletData snapshotUpdate,
      HashedVersion committed) throws ChannelException {
    viewListener.onSnapshot(WAVELET_ID_1, snapshotUpdate, committed, null);
    muxListener.verifyOperationChannelRemoved(channelToClobber);
    return muxListener.verifyOperationChannelCreated(snapshotUpdate, Accessibility.READ_WRITE);
  }

  private static void checkReceiveDelta(ViewChannel.Listener viewListener,
      OperationChannel opChannel, MockOperationChannelListener opChannelListener,
      WaveletId waveletId, long version, int numOps, byte[] signature) throws ChannelException {
    // Receive delta from view channel, expect ops on op channel.
    final List<TransformedWaveletDelta> update = createServerDeltaList(version, numOps, signature);
    viewListener.onUpdate(waveletId, update, null, null);
    opChannelListener.checkOpsReceived(1);
    opChannelListener.clear();
    for (int i = 0; i < numOps; ++i) {
      assertNotNull(opChannel.receive());
    }
    assertNull(opChannel.receive());
  }

  /**
   * Sends an operation on an operation channel and expects the delta to
   * be submitted to the view.
   */
  private static WaveletDelta checkSendDelta(MockViewChannel viewChannel,
      OperationChannel opChannel, HashedVersion initialVersion, WaveletId expectedWaveletId)
      throws ChannelException {
    WaveletOperation op = createOp();
    WaveletDelta delta = createDelta(initialVersion, op);
    viewChannel.expectSubmitDelta(expectedWaveletId, delta);
    opChannel.send(op);
    return delta;
  }

  /**
   * Acks a delta and checks that the fake version-incrementing op is received
   * from the operation channel.
   */
  private static void checkAckDelta(MockViewChannel viewChannel, OperationChannel opChannel,
      MockOperationChannelListener opChannelListener, int ackedOps, long version, byte[] signature)
      throws ChannelException {
    viewChannel.ackSubmit(ackedOps, version, signature);
    opChannelListener.checkOpsReceived(1);
    opChannelListener.clear();
    assertNotNull(opChannel.receive());
    opChannelListener.checkOpsReceived(0);
    opChannelListener.clear();
  }

  /**
   * Receives a one-op delta and sends/acks another, checking expectations.
   */
  private static void checkReceiveAndSend(ViewChannel.Listener viewListener, MockViewChannel
      viewChannel, ConnectedChannel ch, WaveletId waveletId, long version)
      throws ChannelException {
    checkReceiveDelta(viewListener, ch.channel, ch.listener, waveletId, version, 1, SIG2);
    checkSendDelta(viewChannel, ch.channel, HashedVersion.of(version + 1, SIG2), waveletId);
    checkAckDelta(viewChannel, ch.channel, ch.listener, 1, version + 2, SIG3);
  }

  /**
   * Opens a new mux and returns the created view mock.
   */
  private MockViewChannel openMux() {
    return openMux(IdFilters.ALL_IDS);
  }

  private MockViewChannel openMux(IdFilter idFilter) {
    MockViewChannel viewChannel = viewFactory.expectCreate();
    viewChannel.expectOpen(idFilter, NO_KNOWN_WAVELETS);
    mux.open(muxListener, idFilter);
    return viewChannel;
  }

  /**
   * Opens a new mux with a known wavelet and returns the created view mock.
   */
  private MockViewChannel openMuxWithKnownWavelet(ObservableWaveletData knownSnapshot) {
    long version = knownSnapshot.getVersion();
    byte[] signature = knownSnapshot.getHashedVersion().getHistoryHash();
    MockViewChannel view = viewFactory.expectCreate();
    Map<WaveletId, List<HashedVersion>> expectedSigs = createKnownVersions(WAVELET_ID_1,
        version, signature);
    view.expectOpen(IdFilters.ALL_IDS, expectedSigs);
    mux.open(muxListener, IdFilters.ALL_IDS, Collections.singletonList(createKnownWavelet(
        knownSnapshot, version, signature, Accessibility.READ_WRITE)));
    return view;
  }

  private ConnectedChannel createOperationChannel(WaveletId waveletId, ParticipantId address) {
    mux.createOperationChannel(waveletId, address);
    OperationChannel channel = muxListener.verifyOperationChannelCreated(createSnapshot(
        WAVELET_ID_1, 0, NOSIG), Accessibility.READ_WRITE);
    MockOperationChannelListener channelListener = new MockOperationChannelListener();
    channel.setListener(channelListener);
    return new ConnectedChannel(channel, channelListener);
  }

  private ConnectedChannel connectChannelSnapshot(ViewChannel.Listener viewListener,
      ConnectionInfo info) throws ChannelException {
    return connectChannelSnapshot(viewListener, info.snapshot, info.initialHashedVersion);
  }

  private ConnectedChannel connectChannelSnapshot(ViewChannel.Listener viewListener,
      ObservableWaveletData snapshot, HashedVersion committed)
      throws ChannelException {
    viewListener.onSnapshot(snapshot.getWaveletId(), snapshot, committed, null);
    OperationChannel channel = muxListener.verifyOperationChannelCreated(snapshot,
        Accessibility.READ_WRITE);
    MockOperationChannelListener listener = new MockOperationChannelListener();
    channel.setListener(listener);
    return new ConnectedChannel(channel, listener);
  }

  private ConnectedChannel expectConnectedChannel(ObservableWaveletData knownSnapshot,
      Accessibility accessibility) {
    OperationChannel channel = muxListener.verifyOperationChannelCreated(knownSnapshot,
        accessibility);
    MockOperationChannelListener listener = new MockOperationChannelListener();
    channel.setListener(listener);
    return new ConnectedChannel(channel, listener);
  }

  private void triggerAndCheckOpenFinished(ViewChannel.Listener viewListener)
      throws ChannelException {
    viewListener.onOpenFinished();
    checkOpenFinished();
  }

  private void checkOpenFinished() {
    muxListener.verifyOpenFinished();
  }

  /**
   * Sends a ChannelException to a view listener and expects a new view to be opened
   * to reconnect it.
   *
   * @return the new mock view
   */
  private MockViewChannel failViewAndExpectReconnection(ViewChannel.Listener viewListenerToFail,
      MockViewChannel failingView, String failureReason,
      Map<WaveletId, List<HashedVersion>> expectedReconnectionSigs) {
    failingView.expectClose();
    MockViewChannel newView = viewFactory.expectCreate();
    newView.expectOpen(IdFilters.ALL_IDS, expectedReconnectionSigs);
    viewListenerToFail.onException(new ChannelException(failureReason, Recoverable.RECOVERABLE));
    viewListenerToFail.onClosed();
    failingView.checkExpectationsSatisified();
    return newView;
  }

  /**
   * Reconnects a mux on a view, returning the reconnected view's listener.
   */
  private static ViewChannel.Listener reconnectView(MockViewChannel view,
      ConnectionInfo... channels) throws ChannelException {
    ViewChannel.Listener viewListener = view.takeListener();
    viewListener.onConnected();

    for (ConnectionInfo chInfo : channels) {
      reconnectChannel(viewListener, chInfo.waveletId, chInfo.initialVersion,
          chInfo.initialSignature);
    }
    viewListener.onOpenFinished();
    view.checkExpectationsSatisified();
    return viewListener;
  }

  /**
   * Sends a reconnect delta with current version the same as the reconnection
   * version.
   */
  private static void reconnectChannel(ViewChannel.Listener viewListener, WaveletId waveletId,
      long version, byte[] signature) throws ChannelException {
    reconnectChannel(viewListener, waveletId, version, signature, version, signature);
  }

  private static void reconnectChannel(ViewChannel.Listener viewListener, WaveletId waveletId,
      long connectVersion, byte[] connectSignature, long currentVersion, byte[] currentSignature)
      throws ChannelException {
    TransformedWaveletDelta reconnect = createReconnect(null, connectVersion, connectSignature);
    HashedVersion distinctVersion = HashedVersion.of(currentVersion, currentSignature);
    viewListener.onUpdate(waveletId, Collections.singletonList(reconnect),
        distinctVersion, distinctVersion);
  }

  /**
   * Reconnects a view with a single operation channel and checks that the channel
   * is usable and expectations are satisfied.
   */
  private void reconnectViewAndCheckEverythingStillWorks(MockViewChannel view,
      ConnectionInfo chInfo, ConnectedChannel ch) throws ChannelException {
    // Perform the reconnect.
    ViewChannel.Listener viewListener2 = reconnectView(view, chInfo);
    ch.listener.checkOpsReceived(0);

    // Check everything still works.
    checkReceiveAndSend(viewListener2, view, ch, WAVELET_ID_1, chInfo.initialVersion);
    view.checkExpectationsSatisified();
  }
}
