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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveViewImpl.CcDocumentFactory;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Set;

/**
 * A Wavelet that is based on Cc.
 *
 */
public final class CcBasedWavelet implements ObservableWavelet {

  /**
   * Factory for {@link CcBasedWavelet}s in the same wave.
   */
  static class Factory {
    private final WaveId waveId;
    private final WaveletOperationContext.Factory contextFactory;
    private final ParticipationHelper participationHelper;
    private final CcDocumentFactory<?> docFactory;
    private final FailureHandler failureHandler;
    private final DuplexOpSinkFactory opTaps;

    Factory(WaveId waveId, WaveletOperationContext.Factory contextFactory,
        ParticipationHelper participationHelper, CcDocumentFactory<?> docFactory,
        FailureHandler failureHandler, DuplexOpSinkFactory opTaps) {
      this.waveId = waveId;
      this.contextFactory = contextFactory;
      this.participationHelper = participationHelper;
      this.docFactory = docFactory;
      this.failureHandler = failureHandler;
      this.opTaps = opTaps;
    }

    /**
     * Places a concurrency-control driver and target on top of the new
     * operation channel. All wavelet model objects are created through this
     * method.
     *
     * @param target target to be controller by operations
     * @param channel channel for operation communication
     * @return new wavelet
     */
    CcBasedWavelet create(OperationChannel channel, ObservableWaveletData target) {
      return new CcBasedWavelet(waveId, contextFactory, participationHelper, channel, target,
          docFactory, failureHandler, opTaps);
    }
  }

  /**
   * Handler for failure of the concurrency control channel supporting a
   * wavelet.
   *
   * When a wavelet fails the handler must ensure the wavelet is not further
   * mutated either through application of remote operations or local mutation
   * methods. Side-effect-free queries remain usable.
   */
  interface FailureHandler {
    /**
     * Called when an operation fails to apply to the wavelet.
     *
     * @param failure the exception raised by the wavelet/channel.
     */
    void onWaveletFailed(OperationException failure);
  }

  /**
   * An applier can flush and can consume operations against a target.
   */
  private class OperationApplier implements FlushingOperationSink<WaveletOperation> {
    /** Target being operated. */
    private final ObservableWaveletData target;

    private final WaveletId waveletId;

    /** Attachments hack. */
    private SilentOperationSink<WaveletOperation> extraHandler;

    /** Just for debugging, so we know what ops have been applied before any death. */
    private WaveletOperation debugLastSuccessfulOp = null;

    OperationApplier(ObservableWaveletData target, WaveletId waveletId) {
      this.target = target;
      this.waveletId = waveletId;
    }

    @Override
    public boolean flush(WaveletOperation operation, Runnable resume) {
      Preconditions.checkState(!failed, "CcBasedWavelet operation applier flushed after failure");
      if (operation instanceof WaveletBlipOperation) {
        WaveletBlipOperation waveBlipOp = (WaveletBlipOperation) operation;
        CcDocument doc = docFactory.get(waveletId, waveBlipOp.getBlipId());
        if (doc != null) {
          return doc.flush(resume);
        }
      }
      return true;
    }

    /**
     * HACK(user):
     * Inserts an extra operation handler.  This is only required for
     * attachment handling.
     *
     * @param extraHandler
     */
    void register(SilentOperationSink<WaveletOperation> extraHandler) {
      this.extraHandler = extraHandler;
    }

    @Override
    public void consume(WaveletOperation waveOp) {
      Preconditions.checkState(!failed,
          "CcBasedWavelet operation applier received op after failure");
      try {
        fireOnOpBegin(waveOp.getContext(), waveOp.isWorthyOfAttribution());
        waveOp.apply(target);
        // Hack for attachments
        if (extraHandler != null) {
          extraHandler.consume(waveOp);
        }
        // attachmentsManager.getServerOperationListener().consume(waveOp);
      } catch (OperationException e) {
        // Fail this wavelet permanently
        fail(new OperationException("Operation failed; no recovery possible: " + e.getMessage()
                + "\n[debugLastSuccessfulOp:" + debugLastSuccessfulOp + "]"
                + "\n[waveOp:" + waveOp + "]"
                + "\n[target:" + target + "]", e));
      } finally {
        fireOnOpEnd();
      }
      debugLastSuccessfulOp = waveOp;
    }
  }

  /**
   * Adapts an operation channel, making it look like an operation sink.
   * The only reason a channel is not already a sink is because it has a more
   * general acceptor that takes a varargs parameter.
   */
  private static class ChannelAdapter implements SilentOperationSink<WaveletOperation> {
    private final OperationChannel target;

    public ChannelAdapter(OperationChannel target) {
      this.target = target;
    }

    @Override
    public void consume(WaveletOperation op) {
      try {
        target.send(op);
      } catch (ChannelException e) {
        throw new RuntimeException("Send failed, channel is broken", e);
      }
    }
  }

  private final OperationApplier applier;
  private final ChannelAdapter remote;
  private final OpBasedWavelet wavelet;
  private final CcDocumentFactory<?> docFactory;
  private final CopyOnWriteSet<CcBasedWaveletListener> ccBasedWaveListeners =
    CopyOnWriteSet.create();
  private final FailureHandler failureHandler;
  private final OperationChannel channel;
  private final DuplexOpSink opTap;
  private final OperationSucker driver;

  private boolean failed = false;

  CcBasedWavelet(WaveId waveId, WaveletOperationContext.Factory contextFactory,
      ParticipationHelper participationHelper, OperationChannel channel,
      ObservableWaveletData target, CcDocumentFactory<?> docFactory,
      FailureHandler failureHandler, DuplexOpSinkFactory opTaps) {
    this.docFactory = docFactory;
    this.channel = channel;
    this.failureHandler = failureHandler;

    Preconditions.checkNotNull(opTaps, "OpTaps cannot be null");

    // Sink through which all operations are sent to remote state
    remote = new ChannelAdapter(channel);

    // Sink through which all operations are applied to local state
    applier = new OperationApplier(target, target.getWaveletId());

    opTap = opTaps.create(target.getWaveletId(), applier, remote);

    // Create operation adapter, which translates semantic methods into
    // operations, applying them to the target as well as sending them down
    // the operation channel. Local operation failure fails this wavelet
    // permanently.
    wavelet = new OpBasedWavelet(waveId, target, contextFactory, participationHelper,
        opTap.incoming(), opTap.outgoing());

    // Create operation sucker, which sucks operations out of the channel and
    // applies them to the CC target.
    driver = new OperationSucker(channel, opTap.incoming());
    channel.setListener(driver);
  }

  /**
   * Add an OpBasedWaveletListener.
   */
  public void addCcBasedWaveletListener(
          org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveletListener listener) {
    ccBasedWaveListeners.add(listener);
  }

  /**
   * Remove an OpBasedWaveletListener.
   */
  public void removeCcBasedWaveletListener(CcBasedWaveletListener listener) {
    ccBasedWaveListeners.remove(listener);
  }

  /**
   * @return the debug string of cc stack
   */
  public String getCcDebugString() {
    return channel.getDebugString();
  }

  /**
   * @return the underlying wavelet.
   */
  public OpBasedWavelet getOpBasedWavelet() {
    return wavelet;
  }

  private void fireOnOpBegin(WaveletOperationContext context, boolean isWorthyOfContribution) {
    for (CcBasedWaveletListener l : ccBasedWaveListeners) {
      l.onOpBegin(context.getCreator(), context.getTimestamp(), isWorthyOfContribution);
    }
  }

  private void fireOnOpEnd() {
    for (CcBasedWaveletListener l : ccBasedWaveListeners) {
      l.onOpEnd();
    }
  }

  /**
   * Fails this wavelet, removing listeners and notifying the wavelet's failure
   * handler that this wavelet is unusable.
   *
   * @param failure exception causing the failure
   */
  private void fail(OperationException failure) {
    if (!failed) {
      failed = true;
      driver.shutdown();
      ccBasedWaveListeners.clear();
      // Currently this will cause the wave to disconnect.
      failureHandler.onWaveletFailed(failure);
    }
  }

  /**
   * @throws IllegalStateException if this wavelet has failed.
   */
  private void checkNotFailed() {
    if (failed) {
      throw new IllegalStateException("CcBasedWavelet used after failure");
    }
  }

  //
  // Delegates to OpBasedWavelet
  //

  @Override
  public void addParticipantIds(Set<ParticipantId> participants) {
    checkNotFailed();
    wavelet.addParticipantIds(participants);
  }

  @Override
  public void addParticipant(ParticipantId participant) {
    checkNotFailed();
    wavelet.addParticipant(participant);
  }

  @Override
  public Iterable<? extends Blip> getBlips() {
    return wavelet.getBlips();
  }

  @Override
  public Blip getBlip(String blipId) {
    return wavelet.getBlip(blipId);
  }

  @Override
  public Blip createBlip(String id) {
    return wavelet.createBlip(id);
  }

  @Override
  public long getCreationTime() {
    return wavelet.getCreationTime();
  }

  @Override
  public ParticipantId getCreatorId() {
    return wavelet.getCreatorId();
  }

  @Override
  public ObservableDocument getDocument(String documentName) {
    return wavelet.getDocument(documentName);
  }

  @Override
  public Set<String> getDocumentIds() {
    return wavelet.getDocumentIds();
  }

  @Override
  public WaveletId getId() {
    return wavelet.getId();
  }

  @Override
  public long getLastModifiedTime() {
    return wavelet.getLastModifiedTime();
  }

  @Override
  public Set<ParticipantId> getParticipantIds() {
    return wavelet.getParticipantIds();
  }

  @Override
  public long getVersion() {
    return wavelet.getVersion();
  }

  @Override
  public HashedVersion getHashedVersion() {
    return wavelet.getHashedVersion();
  }

  @Deprecated
  @Override
  public WaveId getWaveId() {
    return wavelet.getWaveId();
  }

  @Override
  public void addListener(WaveletListener listener) {
    wavelet.addListener(listener);
  }

  @Override
  public void removeListener(WaveletListener listener) {
    wavelet.removeListener(listener);
  }

  @Override
  public void removeParticipant(ParticipantId participant) {
    checkNotFailed();
    wavelet.removeParticipant(participant);
  }

  @Override
  public String toString() {
    return wavelet.toString();
  }

  @Override
  public ObservableWaveletData getWaveletData() {
    return wavelet.getWaveletData();
  }
}
