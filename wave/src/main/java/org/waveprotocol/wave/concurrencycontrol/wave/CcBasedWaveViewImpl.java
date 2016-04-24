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

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.channel.Accessibility;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWavelet.FailureHandler;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.WaveViewListener;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;

import java.util.Map;

/**
 * An implementation of a wave view based on wavelets that sit on top of a
 * concurrency-control stack.
 *
 */
public final class CcBasedWaveViewImpl implements CcBasedWaveView {

  /**
   * Handler for the view becoming disconnected.
   *
   * @see WaveDisconnectedHandler
   */
  public interface DisconnectedHandler {
    /**
     * Called when this wave becomes permanently disconnected
     */
    void onWaveDisconnected(CorruptionDetail detail);
  }

  /**
   * A factory for flushable documents, that also remembers documents it has
   * created.
   */
  public interface CcDocumentFactory<D extends CcDocument> extends DocumentFactory<D> {
    /**
     * Gets the document that was created for a particular document identifier.
     *
     * @param waveletId wavelet in which the document exists
     * @param blipId blip in which the document exists
     * @return the document that was previously created for blip {@code blipId}
     *         in wavelet {@code waveletId}, or {@code null}.
     */
    D get(WaveletId waveletId, String blipId);
  }

  /**
   * Listens to a mux, placing wavelet models on top of operation channels
   * that show up.  Such wavelets are then passed to a listener.
   */
  private static class MuxListener implements OperationChannelMultiplexer.Listener {
    /**
     * Interface through which this listener notifies something of new wavelets
     * that show up on the mux.
     */
    interface WaveletListener {
      void onWaveletAdded(CcBasedWavelet w);
      void onWaveletRemoved(CcBasedWavelet w);
    }

    /**
     * Interface through which mux listeners are created.
     */
    interface Factory {
      MuxListener create(OpenListener openListener);
    }

    private final CcBasedWavelet.Factory waveletFactory;
    private final DisconnectedHandler disconnectedHandler;
    private final WaveletListener listener;
    private final OpenListener openListener;
    private final Map<WaveletId, CcBasedWavelet> wavelets = CollectionUtils.newHashMap();
    private final TerminalWavelets terminalWavelets;

    MuxListener(CcBasedWavelet.Factory waveletFactory, DisconnectedHandler disconnectedHandler,
        WaveletListener listener, OpenListener openListener,
        TerminalWavelets terminalWavelets) {
      this.waveletFactory = waveletFactory;
      this.disconnectedHandler = disconnectedHandler;
      this.listener = listener;
      this.openListener = openListener;
      this.terminalWavelets = terminalWavelets;
    }

    @Override
    public void onOperationChannelCreated(OperationChannel channel, ObservableWaveletData snapshot,
        Accessibility accessibility) {
      // New wavelet has come into existence. Build a model on top.
      CcBasedWavelet wavelet = waveletFactory.create(channel, snapshot);

      // Keep this wavelet around for future changes.
      wavelets.put(wavelet.getId(), wavelet);

      // Mark inaccessible if necessary.
      if (accessibility == Accessibility.INACCESSIBLE) {
        terminalWavelets.markTerminal(wavelet);
      }

      listener.onWaveletAdded(wavelet);
    }

    @Override
    public void onOperationChannelRemoved(OperationChannel channel, WaveletId waveletId) {
      CcBasedWavelet wavelet = wavelets.remove(waveletId);
      terminalWavelets.clearTerminal(wavelet);
      listener.onWaveletRemoved(wavelet);
    }

    @Override
    public void onFailed(CorruptionDetail detail) {
      if (disconnectedHandler != null) {
        disconnectedHandler.onWaveDisconnected(detail);
      }
    }

    @Override
    public void onOpenFinished() {
      openListener.onOpenFinished();
    }
  }

  /**
   * Tracks terminal wavelets across all wavelet channels.
   * {@see CcBasedWaveView#isTerminal(Wavelet)}.
   */
  private static class TerminalWavelets {
    private final IdentitySet<Wavelet> terminalWavelets = CollectionUtils.createIdentitySet();

    /**
     * Marks a wavelet as terminal.
     */
    public void markTerminal(Wavelet wavelet) {
      terminalWavelets.add(wavelet);
    }

    /**
     * Clears the terminated status of a wavelet.
     */
    public void clearTerminal(Wavelet wavelet) {
      terminalWavelets.remove(wavelet);
    }

    /**
     * @return whether a given wavelet is terminal
     */
    public boolean isTerminal(Wavelet wavelet) {
      return terminalWavelets.contains(wavelet);
    }
  }

  private final WaveViewImpl<CcBasedWavelet> view;
  private final OperationChannelMultiplexer pipe;
  private final IdFilter waveletFilter;
  private final MuxListener.Factory muxListenerFactory;
  private final TerminalWavelets terminalWavelets;

  /**
   * Creates a wave view. The new view needs to be {@link #open(OpenListener)
   * opened} before use.
   *
   * @return a new wave view.
   */
  public static CcBasedWaveView create(CcDocumentFactory<?> docFactory, SchemaProvider schemas,
      WaveId waveId, ParticipantId userId,
      final OperationChannelMultiplexer pipe, IdFilter waveletFilter,
      IdGenerator idGenerator, final LoggerBundle logger,
      WaveletOperationContext.Factory contextFactory, ParticipationHelper participationHelper,
      final DisconnectedHandler disconnectedHandler, WaveletConfigurator configurator,
      DuplexOpSinkFactory tapsFactory) {
    // Local class to overcome the impedance mismatch between:
    // *) the view implementation's policy of notifying listeners as soons as a
    // new wavelet shows up;
    // *) the mux's model of creating a new wavelet by creating a new operation
    // channel (which makes it look like a new wavelet has just showed up); and
    // *) the need to delay notifying listeners of new wavelets until after the
    // configurator has had a chance to prepare it.
    class LocalWaveletHolder {
      private boolean isExpecting;
      private CcBasedWavelet w;

      void expect() {
        isExpecting = true;
      }

      boolean isExpecting() {
        return isExpecting;
      }

      void push(CcBasedWavelet w) {
        this.w = w;
        isExpecting = false;
      }

      CcBasedWavelet pop() {
        CcBasedWavelet toReturn = w;
        w = null;
        return toReturn;
      }
    }

    // Handle wavelet failure by closing the mux (ensuring no more ops are
    // received) and disconnecting the wave (ensuring no more local
    // modifications are made).
    FailureHandler failureHandler = new FailureHandler() {
      @Override
      public void onWaveletFailed(OperationException failure) {
        logger.error().log("CcBasedWavelet failed permanently", failure);
        disconnectedHandler.onWaveDisconnected(
            new CorruptionDetail(failure.isSchemaViolation()
                ? ResponseCode.SCHEMA_VIOLATION : ResponseCode.INVALID_OPERATION,
                "CcBasedWavelet failed",
                failure));
        pipe.close();
      }
    };

    final CcBasedWavelet.Factory waveletFactory =
        new CcBasedWavelet.Factory(waveId, contextFactory, participationHelper, docFactory,
            failureHandler, tapsFactory);

    final LocalWaveletHolder holder = new LocalWaveletHolder();
    WaveletFactory<CcBasedWavelet> factory = new WaveletFactory<CcBasedWavelet>() {
      @Override
      public CcBasedWavelet create(WaveId waveId, WaveletId waveletId, ParticipantId creator) {
        // For wavelets created by this view instance, it is undesirable to
        // notify listeners of the new wavelet here; it is better to notify
        // listeners right before the exit of createWavelet(), AFTER the
        // WaveletConfigurator has initialized the new wavelet (e.g., in case
        // the creating account needs to be added as a participant). This is to
        // prevent callbacks from making mutations to the wavelet before it has
        // been configured.
        holder.expect();
        assert holder.isExpecting();
        pipe.createOperationChannel(waveletId, creator);
        // This should have created a new wavelet through the
        // onOperationChannelCreated callback.
        assert !holder.isExpecting();
        return holder.pop();
      }
    };

    final WaveViewImpl<CcBasedWavelet> view =
        WaveViewImpl.create(factory, waveId, idGenerator, userId, configurator);

    final MuxListener.WaveletListener waveletListener = new MuxListener.WaveletListener() {
      @Override
      public void onWaveletAdded(CcBasedWavelet wavelet) {
        if (view.getWavelet(wavelet.getId()) != null) {
          logger.error().log(
              "Ignoring new channel for existing wavelet " + wavelet.getId() + " in wave "
              + view.getWaveId());
        } else {
          if (holder.isExpecting()) {
            holder.push(wavelet);
          } else {
            view.addWavelet(wavelet);
          }
        }
      }

      @Override
      public void onWaveletRemoved(CcBasedWavelet wavelet) {
        view.removeWavelet(wavelet);
      }
    };

    final TerminalWavelets accessibilityTracker = new TerminalWavelets();

    MuxListener.Factory muxListenerFactory = new MuxListener.Factory() {
      @Override
      public MuxListener create(OpenListener openListener) {
        return new MuxListener(waveletFactory, disconnectedHandler, waveletListener,
            openListener, accessibilityTracker);
      }
    };

    return new CcBasedWaveViewImpl(
        pipe, waveletFilter, view, muxListenerFactory, accessibilityTracker);
  }

  /**
   * Creates a wave view (dependency injection constructor).
   */
  private CcBasedWaveViewImpl(OperationChannelMultiplexer pipe, IdFilter waveletFilter,
      WaveViewImpl<CcBasedWavelet> view, MuxListener.Factory muxListenerFactory,
      TerminalWavelets terminalWavelets) {
    this.pipe = pipe;
    this.waveletFilter = waveletFilter;
    this.view = view;
    this.muxListenerFactory = muxListenerFactory;
    this.terminalWavelets = terminalWavelets;
  }

  //
  // Channel lifecycle.
  //

  public void open(OpenListener openListener) {
    pipe.open(muxListenerFactory.create(openListener), waveletFilter);
  }

  public void close() {
    pipe.close();
  }

  //
  // Accessibility.
  //

  @Override
  public boolean isTerminal(Wavelet wavelet) {
    return terminalWavelets.isTerminal(wavelet);
  }

  //
  // WaveView implementation.
  //

  @Override
  public CcBasedWavelet createRoot() {
    return view.createRoot();
  }

  @Override
  public CcBasedWavelet createWavelet() {
    return view.createWavelet();
  }

  @Override
  public CcBasedWavelet createUserData() {
    return view.createUserData();
  }

  @Override
  public CcBasedWavelet getWavelet(WaveletId waveletId) {
    return view.getWavelet(waveletId);
  }

  @Override
  public CcBasedWavelet getRoot() {
    return view.getRoot();
  }

  @Override
  public CcBasedWavelet getUserData() {
    return view.getUserData();
  }

  @Override
  public Iterable<? extends CcBasedWavelet> getWavelets() {
    return view.getWavelets();
  }

  @Override
  public WaveId getWaveId() {
    return view.getWaveId();
  }

  //
  // Observable extension.
  //

  @Override
  public void addListener(WaveViewListener listener) {
    view.addListener(listener);
  }

  @Override
  public void removeListener(WaveViewListener listener) {
    view.removeListener(listener);
  }
}
