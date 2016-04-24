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

import static org.waveprotocol.wave.model.wave.Constants.NO_VERSION;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.client.ConcurrencyControl;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListenerFactory;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Scheduler;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multiplexes several {@link OperationChannel operation channels} over one
 * {@link ViewChannel view channel}.
 *
 *
 *       |- OperationChannelMultiplexer -----------------------------------------|
 *       |                                                                       |
 *       |  |-Stacklet---------------------------------|                         |
 *       |  | OperationChannel <-> WaveletDeltaChannel |-|                       |
 *   <-> |  |------------------------------------------| |-|   <=> View Channel  | <-> WaveService
 *       |    |------------------------------------------| |                     |
 *       |      |------------------------------------------|                     |
 *       |                                                                       |
 *       |          All exceptions are directed here                             |
 *       |-----------------------------------------------------------------------|
 *
 * Note:
 *
 * All exceptions that are emitted from using the OperationChannel or
 * OperationChannelMultiplexer interfaces are caught in this class.
 * i.e. when the client calls methods from the left part of the diagram.
 *
 * All exceptions generated as a result of handling server messages in ViewChannel
 * are routed here through onException(). i.e. when the WaveService calls methods on
 * the right part of the diagram through call backs.
 *
 * This class is responsible for reporting all the exceptions to the user.
 *
 */
public class OperationChannelMultiplexerImpl implements OperationChannelMultiplexer {
  /**
   * Binds together both ends of a delta channel.
   */
  interface MultiplexedDeltaChannel extends WaveletDeltaChannel, WaveletChannel.Listener {
  }

  /**
   * Factory for creating delta channels.
   */
  interface DeltaChannelFactory {
    /**
     * Creates a delta channel.
     *
     * @param waveletChannel channel through which the delta channel
     *        communicates
     */
    MultiplexedDeltaChannel create(WaveletChannel waveletChannel);
  }

  /**
   * Factory for operation channels.
   */
  interface OperationChannelFactory {
    /**
     * Creates an operation channel.
     *
     * @param deltaChannel channel through which the op channel communicates
     * @param waveletId wavelet id for the new operation channel
     * @param startVersion the version to start from
     * @param accessibility accessibility of the new channel
     * @return a new operation channel.
     */
    InternalOperationChannel create(WaveletDeltaChannel deltaChannel, WaveletId waveletId,
        HashedVersion startVersion, Accessibility accessibility);
  }

  /**
   * A per-wavelet stack above this multiplexer. A stacklet forwards message
   * from the server to a listener at the bottom of the stacklet (a delta
   * channel). When communications fail a stacklet fetches reconnection version
   * from the contained operation channel.
   */
  private static class Stacklet implements WaveletChannel.Listener {
    private final MultiplexedDeltaChannel deltaChannel;
    private final InternalOperationChannel opChannel;
    private boolean firstMessageReceived;
    private boolean dropAdditionalSnapshot;

    /**
     * Creates a stacklet.
     *
     * @param deltaChannel delta channel at the bottom of the stacklet
     * @param opChannel operation channel at the top of the stacklet
     * @param dropSnapshot whether to expect and drop an additional snapshot
     *        after the first message.
     */
    private Stacklet(MultiplexedDeltaChannel deltaChannel, InternalOperationChannel opChannel,
        boolean dropSnapshot) {
      this.deltaChannel = deltaChannel;
      this.opChannel = opChannel;
      this.firstMessageReceived = false;
      this.dropAdditionalSnapshot = dropSnapshot;
    }

    public void onWaveletSnapshot(ObservableWaveletData wavelet,
        HashedVersion lastCommittedVersion, HashedVersion currentVersion)
        throws ChannelException {
      // When a channel is created locally we fake an initial empty
      // snapshot. The server still sends one when it creates the wavelet
      // though, so it's dropped it here if that's expected.
      // See createOperationChannel().
      if (!firstMessageReceived) {
        firstMessageReceived = true;
      } else if (dropAdditionalSnapshot) {
        // TODO(anorth): check the snapshot is as expected, even though
        // it's dropped.
        dropAdditionalSnapshot = false;
        return;
      }

      deltaChannel.onWaveletSnapshot(wavelet, lastCommittedVersion, currentVersion);
    }

    @Override
    public void onWaveletUpdate(List<TransformedWaveletDelta> deltas,
        HashedVersion lastCommittedVersion, HashedVersion currentVersion)
        throws ChannelException {
      if (!firstMessageReceived) {
        firstMessageReceived = true;
      }

      deltaChannel.onWaveletUpdate(deltas, lastCommittedVersion,
          currentVersion);
    }

    /**
     * Resets this stacklet ready for reconnection.
     */
    public void reset() {
      deltaChannel.reset(opChannel);
      opChannel.reset();
    }

    /**
     * Closes this stacklet permanently.
     */
    public void close() {
      deltaChannel.reset(null);
      opChannel.close();
    }

    public OperationChannel getOperationChannel() {
      return opChannel;
    }

    public boolean isExpectingSnapshot() {
      return dropAdditionalSnapshot;
    }
  }

  /**
   * Holder class for the copious number of loggers.
   */
  public static class LoggerContext {
    public final LoggerBundle ops;
    public final LoggerBundle delta;
    public final LoggerBundle cc;
    public final LoggerBundle view;

    public LoggerContext(LoggerBundle ops, LoggerBundle delta, LoggerBundle cc, LoggerBundle view) {
      this.ops = ops;
      this.delta = delta;
      this.cc = cc;
      this.view = view;
    }
  }

  /** Multiplexer state. */
  private static enum State { NOT_CONNECTED, CONNECTED, RECONNECTING }

  /** Wave id for channels in this mux. */
  private final WaveId waveId;

  /** Multiplexed channels, indexed by wavelet id. */
  private final Map<WaveletId, Stacklet> channels = CollectionUtils.newHashMap();

  /** Factory for creating delta channels. */
  private final DeltaChannelFactory deltaChannelFactory;

  /** Factory for creating operation-channel stacks on top of wave services. */
  private final OperationChannelFactory opChannelFactory;

  /** Factory for creating a view channel */
  private final ViewChannelFactory viewFactory;

  /** Logger. */
  private final LoggerBundle logger;

  /** A stateful manager/factory for unsaved data listeners */
  private final UnsavedDataListenerFactory unsavedDataListenerFactory;

  /** Synthesizer of initial wavelet snapshots for locally-created wavelets. */
  private final ObservableWaveletData.Factory<?> dataFactory;

  /** Produces hashed versions. */
  private final HashedVersionFactory hashFactory;

  /** List of commands to run when the underlying view becomes connected. */
  private final List<Runnable> onConnected = CollectionUtils.newArrayList();

  //
  // Mutable state.
  //

  /** Connection state of the mux. */
  private State state;

  /** Whether the initial open of the mux has finished. */
  private boolean openFinished = false;

  /**
   * Underlying multiplexed view channel; created on reconnection, set null on
   * close.
   */
  private ViewChannel viewChannel;

  /**
   * Tag identifying which view connection is current. Changes on each
   * reconnection.
   */
  private int connectionTag = 0;

  /** Filter specifying wavelets to open. */
  private IdFilter waveletFilter;

  /** Listener for handling new operation channels. */
  private Listener muxListener;

  /** Used to backoff when reconnecting. */
  private final Scheduler scheduler;

  /**
   * Creates factory for building delta channels.
   *
   * @param logger logger to use for created channels
   */
  private static DeltaChannelFactory createDeltaChannelFactory(final LoggerBundle logger) {
    return new DeltaChannelFactory() {
      @Override
      public MultiplexedDeltaChannel create(WaveletChannel waveletChannel) {
        return new WaveletDeltaChannelImpl(waveletChannel, logger);
      }
    };
  }

  /**
   * Creates a factory for building operation channels on a wave.
   *
   * @param waveId wave id
   * @param unsavedDataListenerFactory factory for unsaved data listeners
   * @param loggers logger bundle
   * @return a new operation channel factory
   */
  private static OperationChannelFactory createOperationChannelFactory(final WaveId waveId,
      final UnsavedDataListenerFactory unsavedDataListenerFactory, final LoggerContext loggers) {
    return new OperationChannelFactory() {
      @Override
      public InternalOperationChannel create(WaveletDeltaChannel deltaChannel, WaveletId waveletId,
          HashedVersion startVersion, Accessibility accessibility) {
        ConcurrencyControl cc = new ConcurrencyControl(loggers.cc, startVersion);
        if (unsavedDataListenerFactory != null) {
          cc.setUnsavedDataListener(unsavedDataListenerFactory.create(waveletId));
        }
        return new OperationChannelImpl(loggers.ops, deltaChannel, cc, accessibility);
      }
    };
  }

  /**
   * Creates a multiplexer.
   *
   * WARNING: the scheduler should provide back-off. Providing a scheduler which
   * executes immediately or does not back off may cause denial-of-service-like
   * reconnection attempts against the servers. Use something like
   * {@link FuzzingBackOffScheduler}.
   *
   * @param waveId wave id to open
   * @param viewFactory factory for opening view channels
   * @param dataFactory factory for making snapshots of empty wavelets
   * @param loggers log targets
   * @param unsavedDataListenerFactory a factory for adding listeners
   * @param scheduler scheduler for reconnection
   * @param hashFactory factory for hashed versions
   */
  public OperationChannelMultiplexerImpl(WaveId waveId, ViewChannelFactory viewFactory,
      ObservableWaveletData.Factory<?> dataFactory, LoggerContext loggers,
      UnsavedDataListenerFactory unsavedDataListenerFactory, Scheduler scheduler,
      HashedVersionFactory hashFactory) {
    // Construct default dependency implementations, based on given arguments.
    this(waveId,
        createDeltaChannelFactory(loggers.delta),
        createOperationChannelFactory(waveId, unsavedDataListenerFactory, loggers),
        viewFactory, dataFactory, scheduler, loggers.view, unsavedDataListenerFactory,
        hashFactory);
    Preconditions.checkNotNull(dataFactory, "null dataFactory");
  }

  /**
   * Creates a multiplexer (direct dependency arguments only). Exposed as
   * package-private for testing.
   *
   * @param opChannelFactory factory for creating operation-channel stacks
   * @param channelFactory factory for creating the underlying view channel
   * @param dataFactory factory for creating wavelet snapshots
   * @param scheduler used to back off when reconnecting. assumed not null.
   * @param logger log target
   * @param unsavedDataListenerFactory
   * @param hashFactory factory for hashed versions
   */
  OperationChannelMultiplexerImpl(
      WaveId waveId, DeltaChannelFactory deltaChannelFactory,
      OperationChannelFactory opChannelFactory,
      ViewChannelFactory channelFactory, ObservableWaveletData.Factory<?> dataFactory,
      Scheduler scheduler, LoggerBundle logger,
      UnsavedDataListenerFactory unsavedDataListenerFactory,
      HashedVersionFactory hashFactory) {
    this.waveId = waveId;
    this.deltaChannelFactory = deltaChannelFactory;
    this.opChannelFactory = opChannelFactory;
    this.viewFactory = channelFactory;
    this.dataFactory = dataFactory;
    this.logger = logger;
    this.unsavedDataListenerFactory = unsavedDataListenerFactory;
    this.state = State.NOT_CONNECTED;
    this.scheduler = scheduler;
    this.hashFactory = hashFactory;
  }

  @Override
  public void open(Listener listener, IdFilter waveletFilter,
      Collection<KnownWavelet> knownWavelets) {
    this.muxListener = listener;
    this.waveletFilter = waveletFilter;

    try {
      if (!knownWavelets.isEmpty()) {
        for (KnownWavelet knownWavelet : knownWavelets) {
          Preconditions.checkNotNull(knownWavelet.snapshot, "Snapshot has no wavelet");
          Preconditions.checkNotNull(knownWavelet.committedVersion,
              "Known wavelet has null committed version");
          boolean dropAdditionalSnapshot = false;
          addOperationChannel(knownWavelet.snapshot.getWaveletId(), knownWavelet.snapshot,
              knownWavelet.committedVersion, knownWavelet.accessibility, dropAdditionalSnapshot);
        }
        // consider the wave as if open has finished.
        maybeOpenFinished();
      }

      Map<WaveletId, List<HashedVersion>> knownSignatures = signaturesFromWavelets(knownWavelets);
      connect(knownSignatures);
    } catch (ChannelException e) {
      shutdown("Multiplexer open failed.", e);
    }
  }

  @Override
  public void open(Listener listener, IdFilter waveletFilter) {
    open(listener, waveletFilter, Collections.<KnownWavelet>emptyList());
  }

  @Override
  public void close() {
    shutdown(ResponseCode.OK, "View closed.", null);
  }

  @Override
  public void createOperationChannel(WaveletId waveletId, ParticipantId creator) {
    if (channels.containsKey(waveletId)) {
      Preconditions.illegalArgument("Operation channel already exists for: " + waveletId);
    }

    // Create the new channel, and fake an initial snapshot.
    // TODO(anorth): inject a clock for providing timestamps.
    HashedVersion v0 = hashFactory.createVersionZero(WaveletName.of(waveId, waveletId));
    final ObservableWaveletData emptySnapshot =
        dataFactory.create(
            new EmptyWaveletSnapshot(waveId, waveletId, creator, v0, System.currentTimeMillis()));

    try {
      boolean dropAdditionalSnapshot = true;
      addOperationChannel(waveletId, emptySnapshot, v0, Accessibility.READ_WRITE,
          dropAdditionalSnapshot);
    } catch (ChannelException e) {
      shutdown("Creating operation channel failed.", e);
    }
  }

  /**
   * Creates a view channel listener. The listener will forward messages to
   * stacklets while {@link #connectionTag} has the value it had at creation
   * time. When a channel (re)connects the tag changes.
   *
   * @param expectedWavelets wavelets and reconnection versions we expect to
   *        receive a message for before
   *        {@link ViewChannel.Listener#onOpenFinished()}
   */
  private ViewChannel.Listener createViewListener(
      final Map<WaveletId, List<HashedVersion>> expectedWavelets) {
    final int expectedTag = connectionTag;
    return new ViewChannel.Listener() {
      /**
       * Wavelets for which we have not yet seen a message, or null after
       * onOpenFinished.
       */
      Set<WaveletId> missingWavelets = CollectionUtils.newHashSet(expectedWavelets.keySet());

      @Override
      public void onSnapshot(WaveletId waveletId, ObservableWaveletData wavelet,
          HashedVersion lastCommittedVersion, HashedVersion currentVersion)
          throws ChannelException {
        if (connectionTag == expectedTag) {
          removeMissingWavelet(waveletId);
          try {
            // Forward message to the appropriate stacklet, creating it if
            // needed.
            Stacklet stacklet = channels.get(waveletId);
            boolean dropAdditionalSnapshot = false;
            // TODO(anorth): Do better than guessing at accessibility here.
            if (stacklet == null) {
              createStacklet(waveletId, wavelet, Accessibility.READ_WRITE,
                  dropAdditionalSnapshot);
              stacklet = channels.get(waveletId);
            } else if (!stacklet.isExpectingSnapshot()) {
              // Replace the existing stacklet by first removing the wavelet
              // and then adding the newly connected one.
              channels.remove(waveletId);
              unsavedDataListenerFactory.destroy(waveletId);
              muxListener.onOperationChannelRemoved(stacklet.getOperationChannel(), waveletId);
              createStacklet(waveletId, wavelet, Accessibility.READ_WRITE,
                  dropAdditionalSnapshot);
              stacklet = channels.get(waveletId);
            }
            stacklet.onWaveletSnapshot(wavelet, lastCommittedVersion, currentVersion);
          } catch (ChannelException e) {
            throw exceptionWithContext(e, waveletId);
          }
        }
      }

      @Override
      public void onUpdate(WaveletId waveletId, List<TransformedWaveletDelta> deltas,
          HashedVersion lastCommittedVersion, HashedVersion currentVersion)
          throws ChannelException {
        if (connectionTag == expectedTag) {
          removeMissingWavelet(waveletId);
          maybeResetScheduler(deltas);
          try {
            Stacklet stacklet = channels.get(waveletId);
            if (stacklet == null) {
              //TODO(user): Figure out the right exception to throw here.
              throw new IllegalStateException("Received deltas with no stacklet present!");
            }
            stacklet.onWaveletUpdate(deltas, lastCommittedVersion, currentVersion);
          } catch (ChannelException e) {
            throw exceptionWithContext(e, waveletId);
          }
        } else {
          logger.trace().log("Mux dropping update from defunct view");
        }
      }

      @Override
      public void onOpenFinished() throws ChannelException {
        if (connectionTag == expectedTag) {
          if (missingWavelets == null) {
            // TODO(anorth): Add an error code for a protocol error and use
            // it here.
            throw new ChannelException(ResponseCode.INTERNAL_ERROR,
                "Multiplexer received openFinished twice", null, Recoverable.NOT_RECOVERABLE,
                waveId, null);
          }

          // If a missing wavelet could be reconnected at version zero then
          // fake the resync message here. The server no longer knows about
          // the wavelet so we should resubmit changes from version zero.
          Iterator<WaveletId> itr = missingWavelets.iterator();
          while (itr.hasNext()) {
            WaveletId maybeMissing = itr.next();
            List<HashedVersion> resyncVersions = expectedWavelets.get(maybeMissing);
            Preconditions.checkState(!resyncVersions.isEmpty(),
                "Empty resync versions for wavelet " + maybeMissing);
            if (resyncVersions.get(0).getVersion() == 0) {
              Stacklet stacklet = channels.get(maybeMissing);
              if (stacklet == null) {
                Preconditions.illegalState("Resync wavelet has no stacklet. Channels: "
                    + channels.keySet() + ", resync: " + expectedWavelets.keySet());
              }
              WaveletName wavelet = WaveletName.of(waveId, maybeMissing);
              List<TransformedWaveletDelta> resyncDeltaList = createVersionZeroResync(wavelet);
              HashedVersion v0 = hashFactory.createVersionZero(wavelet);
              stacklet.onWaveletUpdate(resyncDeltaList, v0, v0);
              itr.remove();
            }
          }

          // Check we received a message for each expected wavelet.
          if (!missingWavelets.isEmpty()) {
            throw new ChannelException(ResponseCode.NOT_AUTHORIZED,
                "Server didn't acknowledge known wavelets; perhaps access has been lost: "
                    + missingWavelets, null, Recoverable.NOT_RECOVERABLE, waveId, null);
          }
          missingWavelets = null;
          maybeOpenFinished();
        } else {
          logger.trace().log("Mux dropping openFinished from defunct view");
        }
      }

      @Override
      public void onConnected() {
        if (connectionTag == expectedTag) {
          OperationChannelMultiplexerImpl.this.onConnected();
        } else {
          logger.trace().log("Mux dropping onConnected from defunct view");
        }
      }

      @Override
      public void onClosed() {
        if (connectionTag == expectedTag) {
          reconnect(null);
        } else {
          logger.trace().log("Mux dropping onClosed from defunct view");
        }
      }

      @Override
      public void onException(ChannelException e) {
        if (connectionTag == expectedTag) {
          onChannelException(e);
        } else {
          logger.trace().log("Mux dropping failure from defunct view");
        }
      }

      /**
       * Adds a wavelet id to the set of seen ids if they are being tracked.
       */
      private void removeMissingWavelet(WaveletId id) {
        if (missingWavelets != null) {
          missingWavelets.remove(id);
        }
      }

      /**
       * Resets the reconnection scheduler if a message indicates
       * the connection is somewhat ok.
       */
      private void maybeResetScheduler(List<TransformedWaveletDelta> deltas) {
        // The connection is probably ok if we receive a delta. A snapshot
        // is not sufficient since some are locally generated. The delta need
        // not have ops; a reconnection delta is enough.
        if ((deltas.size() > 0)) {
          scheduler.reset();
        }
      }
    };
  }

  /**
   * Creates a stacklet and (optionally) initialises it with a snapshot.
   *
   * @param waveletId the wavelet id of the channel to create
   * @param snapshot the wavelet container for the new channel
   * @param committedVersion the committed version for the new channel
   * @param accessibility accessibility the user currently has to the wavelet
   * @param initialiseLocalChannel whether to send the snapshot through the
   *        stacklet, in which case it should expect and drop an additional
   *        snapshot from the network
   */
  private void addOperationChannel(final WaveletId waveletId,
      ObservableWaveletData snapshot, HashedVersion committedVersion,
      Accessibility accessibility, boolean initialiseLocalChannel) throws ChannelException {
    final Stacklet stacklet =
        createStacklet(waveletId, snapshot, accessibility, initialiseLocalChannel);
    if (initialiseLocalChannel) {
      final HashedVersion currentVersion = snapshot.getHashedVersion();
      initialiseLocallyCreatedStacklet(stacklet, waveletId, snapshot, committedVersion,
          currentVersion);
    }
  }

  /**
   * This is an ugly work-around the lack of ability to add channels to a view
   * in the view service API. We need to send some message through the stacklet
   * so it's connected but the server can't send us any message until we submit
   * the first delta, which requires a connected stacklet...
   */
  private void initialiseLocallyCreatedStacklet(final Stacklet stacklet, final WaveletId waveletId,
      final ObservableWaveletData snapshot, final HashedVersion committedVersion,
      final HashedVersion currentVersion)
      throws ChannelException {
    if (state == State.CONNECTED) {
      try {
        stacklet.onWaveletSnapshot(snapshot, committedVersion, currentVersion);
      } catch (ChannelException e) {
        throw exceptionWithContext(e, waveletId);
      }
    } else {
      // Delay connecting the stacklet until the underlying view is connected.
      onConnected.add(new Runnable() {
        public void run() {
          try {
            stacklet.onWaveletSnapshot(snapshot, committedVersion, currentVersion);
          } catch (ChannelException e) {
            shutdown("Fake snapshot for wavelet channel " + waveId + "/" + waveletId + "failed",
                exceptionWithContext(e, waveletId));
          }
        }
      });
    }
  }

  /**
   * Adds a new operation-channel stacklet to this multiplexer and notifies the
   * listener of the new channel's creation.
   *
   * @param waveletId id of the concurrency domain for the new channel
   * @param snapshot wavelet initial state snapshot
   * @param accessibility accessibility of the stacklet; if not
   *        {@link Accessibility#READ_WRITE} then
   *        the stacklet will fail on send
   * @param dropSnapshot whether to expect and drop an additional snapshot from
   *        the view
   */
  private Stacklet createStacklet(final WaveletId waveletId, ObservableWaveletData snapshot,
      Accessibility accessibility, boolean dropSnapshot) {
    if (channels.containsKey(waveletId)) {
      Preconditions.illegalArgument("Cannot create duplicate channel for wavelet: " + waveId + "/"
          + waveletId);
    }
    WaveletChannel waveletChannel = createWaveletChannel(waveletId);
    MultiplexedDeltaChannel deltaChannel = deltaChannelFactory.create(waveletChannel);
    InternalOperationChannel opChannel = opChannelFactory.create(deltaChannel, waveletId,
        snapshot.getHashedVersion(), accessibility);
    Stacklet stacklet = new Stacklet(deltaChannel, opChannel, dropSnapshot);
    stacklet.reset();
    channels.put(waveletId, stacklet);

    if (muxListener != null) {
      muxListener.onOperationChannelCreated(stacklet.getOperationChannel(), snapshot,
          accessibility);
    }

    return stacklet;
  }

  /**
   * Executes any pending commands in the {@link #onConnected} queue.
   */
  private void onConnected() {
    state = State.CONNECTED;
    // Connect all channels created before now.
    for (Runnable command : onConnected) {
      command.run();
    }
    onConnected.clear();
  }

  /**
   * Handles failure of the view channel or an operation channel.
   *
   * @param e The exception that caused the channel to fail.
   */
  private void onChannelException(ChannelException e) {
    if (e.getRecoverable() != Recoverable.RECOVERABLE) {
      shutdown(e.getResponseCode(), "Channel Exception", e);
    } else {
      reconnect(e);
    }
  }

  private void connect(Map<WaveletId, List<HashedVersion>> knownWavelets) {
    Preconditions.checkState(state != State.CONNECTED, "Cannot connect already-connected channel");
    checkConnectVersions(knownWavelets);
    logger.trace().log("Multiplexer reconnecting wave " + waveId);
    viewChannel = viewFactory.create(waveId);
    viewChannel.open(createViewListener(knownWavelets), waveletFilter, knownWavelets);
  }

  /**
   * Checks that reconnect versions are strictly increasing and removes any
   * that are not accepted by the connection's wavelet filter.
   */
  private void checkConnectVersions(Map<WaveletId, List<HashedVersion>> knownWavelets) {
    Iterator<Map.Entry<WaveletId, List<HashedVersion>>> itr =
        knownWavelets.entrySet().iterator();
    while (itr.hasNext()) {
      Map.Entry<WaveletId, List<HashedVersion>> entry = itr.next();
      WaveletId id = entry.getKey();
      if (IdFilter.accepts(waveletFilter, id)) {
        long prevVersion = NO_VERSION;
        for (HashedVersion v : entry.getValue()) {
          if ((prevVersion != NO_VERSION) && (v.getVersion() <= prevVersion)) {
            throw new IllegalArgumentException("Invalid reconnect versions for " + waveId
                + id + ": " + entry.getValue());
          }
          prevVersion = v.getVersion();
        }
      } else {
        // TODO(anorth): throw an IllegalArgumentException here after fixing
        // all callers to avoid this.
        logger.error().log(
            "Mux for " + waveId + " dropping resync versions for filtered wavelet " + id
                + ", filter " + waveletFilter);
        itr.remove();
      }
    }
  }

  /**
   * Terminates all stacklets then reconnects with the known versions
   * provided by them.
   * @param exception The exception that caused the reconnection
   */
  private void reconnect(ChannelException exception) {
    logger.trace().logLazyObjects("Multiplexer disconnected in state ", state , ", reconnecting.");
    state = State.RECONNECTING;

    // NOTE(zdwang): don't clear this as we'll lose wavelets if we've never
    // been connected. This is a reminder.
    // onConnected.clear();

    // Reset each stacklet, collecting the reconnect versions.
    final Map<WaveletId, List<HashedVersion>> knownWavelets = CollectionUtils.newHashMap();
    for (final WaveletId wavelet : channels.keySet()) {
      final Stacklet stacklet = channels.get(wavelet);
      stacklet.reset();
      knownWavelets.put(wavelet, stacklet.getOperationChannel().getReconnectVersions());
    }

    // Close the view channel and ignore future messages from it.
    connectionTag++;
    viewChannel.close();

    // Run the connect part in the scheduler
    scheduler.schedule(new Scheduler.Command() {
      int tag = connectionTag;
      @Override
      public void execute() {
        if (tag == connectionTag) {
          // Reconnect by creating another view channel.
          connect(knownWavelets);
        }
      }
    });
  }

  /**
   * Shuts down this multiplexer permanently.
   *
   * @param reasonCode code representing failure reason. If the value is not
   *    {@code ResponseCode.OK} then the listener will be notified of connection failure.
   * @param description reason for failure
   * @param exception any exception that caused the shutdown.
   */
  private void shutdown(ResponseCode reasonCode, String description, Throwable exception) {
    if (description == null) {
      description = "(No error description provided)";
    }

    boolean notifyFailure = (reasonCode != ResponseCode.OK);

    // We are telling the user through UI that the wave is corrupt, so we must also report it
    // to the server.
    if (notifyFailure) {
      if (exception == null) {
        logger.error().log(description);
      } else {
        logger.error().log(description, exception);
      }
    }

    if (viewChannel != null) {
      // Ignore future messages.
      connectionTag++;
      state = State.NOT_CONNECTED;

      for (Stacklet stacklet : channels.values()) {
        stacklet.close();
      }
      channels.clear();
      viewChannel.close();
      viewChannel = null;
      if (muxListener != null && notifyFailure) {
        muxListener.onFailed(new CorruptionDetail(reasonCode, description, exception));
      }
      muxListener = null;
    }
  }

  /**
   * Shuts down this multiplexer permanently after an exception.
   */
  private void shutdown(String message, ChannelException e) {
    shutdown(e.getResponseCode(), message, e);
  }

  /**
   * Creates a wavelet channel for submissions against a wavelet.
   *
   * @param waveletId wavelet id for the channel
   */
  private WaveletChannel createWaveletChannel(final WaveletId waveletId) {
    return new WaveletChannel() {
      @Override
      public void submit(WaveletDelta delta, final SubmitCallback callback) {
        viewChannel.submitDelta(waveletId, delta, callback);
      }

      @Override
      public String debugGetProfilingInfo() {
        return viewChannel.debugGetProfilingInfo(waveletId);
      }
    };
  }

  private void maybeOpenFinished() {
    // Forward message to the mux's open listener.
    if (!openFinished) {
      openFinished = true;
      muxListener.onOpenFinished();
    }
  }

  /**
   * Wraps a channel exception in another providing wave and wavelet id context.
   */
  private ChannelException exceptionWithContext(ChannelException e, WaveletId waveletId) {
    return new ChannelException(e.getResponseCode(), "Nested ChannelException", e,
        e.getRecoverable(), waveId, waveletId);
  }

  /**
   * Constructs a maps of list of wavelet signatures from a collection of
   * wavelet snapshots.
   *
   * Package-private for testing.
   */
  static Map<WaveletId, List<HashedVersion>> signaturesFromWavelets(
      Collection<KnownWavelet> knownWavelets) {
    Map<WaveletId, List<HashedVersion>> signatures =
      new HashMap<WaveletId, List<HashedVersion>>();
    for (KnownWavelet knownWavelet : knownWavelets) {
      if (knownWavelet.accessibility.isReadable()) {
        ObservableWaveletData snapshot = knownWavelet.snapshot;
        WaveletId waveletId = snapshot.getWaveletId();
        List<HashedVersion> sigs = Collections.singletonList(snapshot.getHashedVersion());
        signatures.put(waveletId, sigs);
      }
    }
    return signatures;
  }

  /**
   * Creates a container message mimicking a resync message for a wavelet at
   * version zero.
   */
  private List<TransformedWaveletDelta> createVersionZeroResync(WaveletName wavelet) {
    return Collections.singletonList(new TransformedWaveletDelta((ParticipantId) null,
        hashFactory.createVersionZero(wavelet), 0L, Collections.<WaveletOperation> emptyList()));
  }
}
