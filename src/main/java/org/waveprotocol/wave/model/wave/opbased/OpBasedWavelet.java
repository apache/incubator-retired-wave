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

package org.waveprotocol.wave.model.wave.opbased;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletDataListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The collaborative structure of the sinks in a set of wave, blip and document adapters is as
 * follows:
 * <pre>
 *                                 document
 * :OpBasedDocument - - - - - - > :DocumentOperationSink
 *           |
 *           V outputSink
 *           0 fromDocument
 *           |                      blip
 *   :OpBasedBlip - - - - - - - > :BlipData
 *           |
 *           V outputSink
 *           0 fromBlip
 *           |                      wave
 *   :OpBasedWavelet  - - - - - - - > :WaveletData
 *           |
 *           V
 *           0 outputSink
 *           |
 *           V (outgoing client ops)
 *
 * where  -0 x  represents a sink called x
 *       ---->  represents flow of operations
 *       - - >  represents operation application, either full (apply()) or partial (update()).
 * </pre>
 *
 * The operation sinks associated with the OpBasedXXX are only for applying and passing around
 * locally-sourced operations.  The structure of each OpBasedXXX is as follows.   Each OpBasedXXX
 * <em>is</em> sink for operations it produces directly.  These operations are applied to the
 * adapted target, then sent to an output sink:
 * <code>
 *   self.consume(op) =
 *     op.apply(target);
 *     outputSink.consume(op);
 * </code>
 * Each OpBasedXXX with sub-OpBasedXXX has a sink for operations produced by those sub-OpBasedXXX
 * (fromDocument and fromBlip).  Operations received through such a sink are boxed, partially
 * applied to the target (just the update() method, since the sub-operation has already been applied
 * to the sub-component), then sent down to the output sink.
 * <code>
 *   fromSub.consume(op) =
 *     op' = box(op);
 *     op'.update(target);
 *     outputSink.consume(op');
 * </code>
 *
 * Note that operations arriving from elsewhere are applied top-down through the WaveletData
 * implementation, and do not flow through these adapters.  The collaboration between the adapters
 * appears as follows:
 *
 * <pre>
 *                              document
 * :OpBasedDocument      - - - - - - - > :DocumentOperationSink
 *           |                                      A document
 *           V outputSink                           |
 *           0 fromDocument                         |
 *           |                      blip            |
 *   :OpBasedBlip         - - - - - - - >      :BlipData
 *           |                                      A blip
 *           V outputSink                           |
 *           0 fromBlip                             |
 *           |                      wave            |
 *   :OpBasedWavelet         - - - - - - - >      :WaveletData
 *           |                                      |
 *           V                                      |
 *           0 outputSink                           0 localSink
 *           |                                      A
 *           V (outgoing client ops)                |(incoming server ops)
 * </pre>
 *
 */
public class OpBasedWavelet implements ObservableWavelet {

  /**
   * Factory method to create a read-only OpBased-wavelet adapter.
   * Sending any operations will cause an exception.
   *
   * @param wavelet simple data to adapt
   */
  public static OpBasedWavelet createReadOnly(ObservableWaveletData wavelet) {
    return new OpBasedWavelet(wavelet.getWaveId(), wavelet,
        // This doesn't thrown an exception, the sinks will
        new BasicWaveletOperationContextFactory(null),
        ParticipationHelper.READONLY, SilentOperationSink.BLOCKED, SilentOperationSink.BLOCKED);
  }

  /** View context in which this wavelet exists. */
  private final WaveId waveId;

  /** Primitive view of the underlying wave. */
  private final ObservableWaveletData wavelet;

  /** Sink to which produced operations are sent for execution. */
  private final SilentOperationSink<? super WaveletOperation> executor;

  /** Output sink to which produced operations are sent for notification. */
  private final SilentOperationSink<? super WaveletOperation> output;

  /** List of adapt blip */
  private final Map<String, OpBasedBlip> blips = new HashMap<String, OpBasedBlip>();

  /**
   * Sink to which blip adapters should place locally-sourced operations that
   * have already executed. Blip adapters give a wavelet adapter boxed ops,
   * because they know their ids, allowing a wavelet adapter to have a single
   * sink for all blips rather than a sink per blip.
   */
  private final SilentOperationSink<WaveletBlipOperation> fromBlip =
      new SilentOperationSink<WaveletBlipOperation>() {
        /**
         * Implements the strategy for consuming operations sent to this adapter from a blip
         * adapter, after the operation has already been applied locally.
         */
        public void consume(WaveletBlipOperation op) {
          authorise(op);
          // Update the wave, then send out.
          op.update(wavelet);
          output.consume(op);
        }
      };

  /** Helper through which operation contexts are created. */
  private final WaveletOperationContext.Factory contextFactory;

  /** Helper to figure out wavelet participation. */
  private final ParticipationHelper participationHelper;

  private final CopyOnWriteSet<WaveletListener> listeners = CopyOnWriteSet.create();

  /**
   * A WaveletDataListener that forward the event to the listener of this object.
   */
  private final WaveletDataListener eventForwarder = new WaveletDataListener() {
    @Override
    public void onParticipantAdded(WaveletData wavelet, ParticipantId participantId) {
      for (WaveletListener l : listeners) {
        l.onParticipantAdded(OpBasedWavelet.this, participantId);
      }
    }

    @Override
    public void onParticipantRemoved(WaveletData wavelet, ParticipantId participantId) {
      for (WaveletListener l : listeners) {
        l.onParticipantRemoved(OpBasedWavelet.this, participantId);
      }
    }

    @Override
    public void onBlipDataAdded(WaveletData waveletData, BlipData blip) {
      // adapt and fire a event
      OpBasedBlip oblip = adapt(blip);
      for (WaveletListener l : listeners) {
        l.onBlipAdded(OpBasedWavelet.this, oblip);
      }
    }

    @Override
    public void onBlipDataSubmitted(WaveletData waveletData, BlipData blip) {
      // adapt and fire a event
      OpBasedBlip oblip = adapt(blip);
      for (WaveletListener l : listeners) {
        l.onBlipSubmitted(OpBasedWavelet.this, oblip);
      }
    }

    @Override
    public void onLastModifiedTimeChanged(WaveletData waveletData, long oldTime, long newTime) {
      for (WaveletListener l : listeners) {
        l.onLastModifiedTimeChanged(OpBasedWavelet.this, oldTime, newTime);
      }
    }

    @Override
    public void onVersionChanged(WaveletData wavelet, long oldVersion, long newVersion) {
      for (WaveletListener l : listeners) {
        l.onVersionChanged(OpBasedWavelet.this, oldVersion, newVersion);
      }
    }

    @Override
    public void onHashedVersionChanged(WaveletData waveletData,
        HashedVersion oldHashedVersion, HashedVersion newHashedVersion) {
      for (WaveletListener l : listeners) {
        l.onHashedVersionChanged(OpBasedWavelet.this, oldHashedVersion, newHashedVersion);
      }
    }

    @Override
    public void onBlipDataTimestampModified(
        WaveletData waveletData, BlipData b, long oldTime, long newTime) {
      OpBasedBlip oblip = adapt(b);
      for (WaveletListener l : listeners) {
        l.onBlipTimestampModified(OpBasedWavelet.this, oblip, oldTime, newTime);
      }
    }

    @Override
    public void onBlipDataVersionModified(
        WaveletData waveletData, BlipData b, long oldVersion, long newVersion) {
      OpBasedBlip oblip = adapt(b);
      for (WaveletListener l : listeners) {
        l.onBlipVersionModified(OpBasedWavelet.this, oblip, oldVersion, newVersion);
      }
    }

    @Override
    public void onBlipDataContributorAdded(
        WaveletData waveletData, BlipData blip, ParticipantId contributorId) {
      OpBasedBlip oblip = adapt(blip);
      for (WaveletListener l : listeners) {
        l.onBlipContributorAdded(OpBasedWavelet.this, oblip, contributorId);
      }
    }

    @Override
    public void onBlipDataContributorRemoved(
        WaveletData waveletData, BlipData blip, ParticipantId contributorId) {
      OpBasedBlip oblip = adapt(blip);
      for (WaveletListener l : listeners) {
        l.onBlipContributorRemoved(OpBasedWavelet.this, oblip, contributorId);
      }
    }

    @Override
    @Deprecated
    public void onRemoteBlipDataContentModified(WaveletData waveletData, BlipData blip) {
      for (WaveletListener l : listeners) {
        l.onRemoteBlipContentModified(OpBasedWavelet.this, adapt(blip));
      }
    }
  };

  /**
   * Creates a OpBased-wavelet adapter.
   *
   * @param waveId that this wavelet is part of
   * @param wavelet simple data to adapt
   * @param contextFactory factory to produce contexts for new operations
   * @param participationHelper helper to figure out wavelet participation
   * @param executor sink that (only) executes operations locally
   * @param output sink to receive all produced operations after they
   *        have executed
   */
  public OpBasedWavelet(WaveId waveId, ObservableWaveletData wavelet,
      WaveletOperationContext.Factory contextFactory, ParticipationHelper participationHelper,
      SilentOperationSink<? super WaveletOperation> executor,
      SilentOperationSink<? super WaveletOperation> output) {
    this.waveId = waveId;
    this.wavelet = wavelet;
    this.contextFactory = contextFactory;
    this.participationHelper = participationHelper;
    this.executor = executor;
    this.output = output;
    wavelet.addListener(eventForwarder);
  }

  /**
   * Applies the op to the adapted wavelet and then outputs the op for remote
   * notification. Generally {@link #authoriseApplyAndSend(WaveletOperation)}
   * should be called instead.
   *
   * @param op to apply.
   */
  private void applyAndSend(WaveletOperation op) {
    // Put the op on the execution sink.  The sink guarantees that the op has
    // executed by the time consume() returns.
    executor.consume(op);

    // Send to output sink.
    output.consume(op);
  }

  /**
   * Grants authorisation for the given change then applies it locally to the
   * adapted wavelet and outputs the op for remote notification.
   *
   * @param op to authorise and apply.
   */
  private void authoriseApplyAndSend(WaveletOperation op) {
    authorise(op);
    applyAndSend(op);
  }

  /**
   * Gains access for the author of an operation to perform changes if they are
   * not already able to. This should generally be called before the given
   * operation has been applied locally, and must be called before it is sent to
   * the output sink. It acceptable, however, for the given operation to have
   * been applied locally already if it does not make any changes to the
   * participant list.
   *
   * @return the add-participant operation injected as a side-effect to
   *         to authorisation, or null if no operation was injected.
   */
  private AddParticipant authorise(WaveletOperation op) {
    ParticipantId author = op.getContext().getCreator();
    Set<ParticipantId> participantIds = getParticipantIds();
    if (participantIds.contains(author)) {
      // Users on the participant list may submit ops directly.
    } else if (participantIds.isEmpty()) {
      // Model is unaware of how participants are allowed to join a wave when
      // there is no one to authorise them. Assume the op is authorised, leaving
      // it to another part of the system to reject it if necessary.
    } else {
      ParticipantId authoriser = null;
      authoriser = participationHelper.getAuthoriser(author, participantIds);
      if (authoriser != null) {
        AddParticipant authorisation =
            new AddParticipant(contextFactory.createContext(authoriser), author);
        applyAndSend(authorisation);
        return authorisation;
      }
    }
    return null;
  }

  /**
   * Handles an exception that occured from the local application of an operation produced by this
   * adapter.
   *
   * TODO(zdwang): Remove this, it's better to just throw exceptions at the source, the leave the
   * exception policy to the caller.
   */
  void handleException(OperationException e) {
    // TODO(user): implement appropriate policy
    throw new OperationRuntimeException("OpBasedWavelet caught exception: " + e, e);
  }

  /**
   * Delegates to {@link WaveletOperationContext.Factory#createContext()}.
   *
   * This method is also used by collaborating {@link OpBasedBlip} adapters, hence is
   * package-private.
   *
   * @return a new operation context.
   */
  WaveletOperationContext createContext() {
    return contextFactory.createContext();
  }

  /**
   * Adapts a {@link Blip} to a {@link OpBasedBlip}, by creating a
   * {@link OpBasedBlip} that collaborates with this wavelet.
   *
   * This method is also used by collaborating {@link OpBasedBlip}s, hence is
   * package-private.
   *
   * @param blip   primitive blip to wrap
   * @return a OpBased view of the given primitive blip.
   */
  OpBasedBlip adapt(BlipData blip) {
    if (blip == null) {
      return null;
    }
    OpBasedBlip oblip = blips.get(blip.getId());
    if (oblip == null) {
      oblip = new OpBasedBlip(blip, this, fromBlip);
      blips.put(blip.getId(), oblip);
    }
    return oblip;
  }

  @Override
  public Iterable<? extends Blip> getBlips() {
    final List<Blip> blips = new ArrayList<Blip>();
    for (String documentId : wavelet.getDocumentIds()) {
      blips.add(adapt(wavelet.getDocument(documentId)));
    }
    return blips;
  }

  @Override
  public OpBasedBlip getBlip(String blipId) {
    BlipData blipData = wavelet.getDocument(blipId);
    if (blipData != null) {
      return adapt(blipData);
    } else {
      return null;
    }
  }

  @Override
  public OpBasedBlip createBlip(String id) {
    // Optimistically create the blip assuming this author submits the
    // first operation.
    WaveletOperationContext context = createContext();
    BlipData newBlip = wavelet.createDocument(id, context.getCreator(),
        Collections.singleton(context.getCreator()), EmptyDocument.EMPTY_DOCUMENT,
        Constants.NO_TIMESTAMP, Constants.NO_VERSION);
    return adapt(newBlip);
  }

  @Override
  public ObservableDocument getDocument(String docId) {
    Blip blip = getBlip(docId);
    if (blip == null) {
      blip = createBlip(docId);
    }
    Document doc = blip.getContent();
    if (!(doc instanceof ObservableDocument)) {
      Preconditions.illegalArgument("Document \"" + docId + "\" is not observable");
    }
    return (ObservableDocument) doc;
  }

  @Override
  public Set<String> getDocumentIds() {
    return wavelet.getDocumentIds();
  }

  //
  // Mutator to operation translations.
  //

  /**
   * Creates and consumes an {@link AddParticipant} operation for each
   * participant in the set.
   */
  @Override
  public void addParticipantIds(Set<ParticipantId> participants) {
    if (participants != null) {
      for (ParticipantId participant : participants) {
        addParticipant(participant);
      }
    }
  }

  /**
   * Creates and consumes an {@link AddParticipant} operation.
   */
  @Override
  public void addParticipant(ParticipantId participant) {
    if (!wavelet.getParticipants().contains(participant)) {
      // Authrorise and apply/send the op in separate stages to avoid sending
      // duplicate add-participant ops (authorise() may inject one).
      WaveletOperation addOp = new AddParticipant(createContext(), participant);
      WaveletOperation injectedOp = authorise(addOp);
      if (!addOp.equals(injectedOp)) {
        applyAndSend(addOp);
      }
    }
  }

  /**
   * Creates and consumes a {@link RemoveParticipant} operation.
   */
  @Override
  public void removeParticipant(ParticipantId participant) {
    if (wavelet.getParticipants().contains(participant)) {
      authoriseApplyAndSend(new RemoveParticipant(createContext(), participant));
    }
  }

  //
  // Vanilla accessors.
  //

  @Override
  public WaveId getWaveId() {
    return waveId;
  }

  @Override
  public WaveletId getId() {
    return wavelet.getWaveletId();
  }

  @Override
  public long getCreationTime() {
    return wavelet.getCreationTime();
  }

  @Override
  public ParticipantId getCreatorId() {
    return wavelet.getCreator();
  }

  @Override
  public long getLastModifiedTime() {
    return wavelet.getLastModifiedTime();
  }

  @Override
  public Set<ParticipantId> getParticipantIds() {
    return Collections.unmodifiableSet(wavelet.getParticipants());
  }

  @Override
  public long getVersion() {
    return wavelet.getVersion();
  }

  @Override
  public HashedVersion getHashedVersion() {
    return wavelet.getHashedVersion();
  }

  @Override
  public void addListener(WaveletListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(WaveletListener listener) {
    listeners.remove(listener);
  }

  @Override
  public int hashCode() {
    return 37 + wavelet.getWaveletId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof OpBasedWavelet)) {
      return false;
    } else {
      return wavelet.getWaveletId().equals(((OpBasedWavelet) obj).wavelet.getWaveletId());
    }
  }

  @Override
  public String toString() {
    return "OpBasedWavelet { " + wavelet + " }";
  }

  /**
   * Creates and consumes a {@link NoOp} (empty) operation.
   */
  public void touch() {
    authoriseApplyAndSend(new NoOp(createContext()));
  }

  @Override
  public ObservableWaveletData getWaveletData() {
    return wavelet;
  }
}
