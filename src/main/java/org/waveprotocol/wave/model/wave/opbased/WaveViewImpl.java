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

import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveViewListener;
import org.waveprotocol.wave.model.wave.Wavelet;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of a wave view, parameterized by a factory for creating
 * individual wavelets in this view.
 *
 * @param <T> Type of wavelets in this view
 */
public final class WaveViewImpl<T extends ObservableWavelet> implements ObservableWaveView {

  /**
   * Factory for creating new wavelets.
   *
   * @param <T> Wavelet type created by this factory
   */
  public interface WaveletFactory<T extends ObservableWavelet> {
    T create(WaveId waveId, WaveletId waveletId, ParticipantId creator);
  }

  /**
   * Available strategies for configuring wavelets created by this view.
   * {@link #ADD_CREATOR} is the default.
   */
  public enum WaveletConfigurator {
    /**
     * Adds the creator as a participant. Recommended mode, due to the
     * underlying access-control semantics.
     */
    ADD_CREATOR {
      @Override
      void configure(Wavelet wavelet) {
        wavelet.addParticipant(wavelet.getCreatorId());
      }
    },

    /**
     * Does nothing. Only to be used by smart agents who know about view-server
     * access-control mechanisms.
     */
    NONE {
      @Override
      void configure(Wavelet wwavelet) {
        // Do nothing
      }
    },

    /**
     * Throws an exception, to indicate that this view is not expected to create
     * wavelets.
     */
    ERROR {
      @Override
      void configure(Wavelet wavelet) {
        throw new IllegalStateException("Unexpected configuration request on this view");
      }
    };

    /**
     * Performs some initial actions on a wavelet created by this view.
     *
     * @param wavelet  new wavelet
     */
    abstract void configure(Wavelet wavelet);
  }

  //
  // Internal state.
  //

  /** Wave id. */
  private final WaveId waveId;

  /** Wavelets in thie view. */
  private final Map<WaveletId, T> wavelets = new HashMap<WaveletId, T>();

  private final CopyOnWriteSet<WaveViewListener> listeners = CopyOnWriteSet.create();

  //
  // Helpers and parameterization.
  //

  private final ParticipantId viewer;
  private final IdGenerator idGenerator;
  private final WaveletConfigurator configurator;
  private final WaveletFactory<? extends T> factory;

  // TODO(anorth/hearnden): Move known ids into an application-level object.
  private final WaveletId rootId;
  private final WaveletId userDataId;

  /**
   * Creates a wave view (dependency injection constructor).
   */
  WaveViewImpl(WaveletFactory<? extends T> factory, WaveId waveId,
      IdGenerator idGenerator, ParticipantId viewer, WaveletConfigurator configurator) {
    this.waveId = waveId;
    this.factory = factory;
    this.viewer = viewer;
    this.idGenerator = idGenerator;
    this.configurator = configurator;
    this.rootId = idGenerator.buildConversationRootWaveletId(waveId);
    this.userDataId = idGenerator.newUserDataWaveletId(viewer.getAddress());
  }

  public static <T extends ObservableWavelet> WaveViewImpl<T> create(
      WaveletFactory<? extends T> factory, WaveId waveId, IdGenerator idGenerator,
      ParticipantId viewer, WaveletConfigurator configurator) {
    return new WaveViewImpl<T>(factory, waveId, idGenerator, viewer, configurator);
  }

  //
  // Services provided by this implementation.
  //

  public void addWavelet(T wavelet) {
    Preconditions.checkArgument(!wavelets.containsKey(wavelet.getId()),
        "Added multiple wavelets with same id: " + wavelet);
    wavelets.put(wavelet.getId(), wavelet);
    triggerOnWaveletAdded(wavelet);
  }

  /**
   * This method takes the upper bound of T (ObservableWavelet) rather than
   * T in order to preserve the substitutability principle. It is the same
   * reason that {@code Collection<T>#remove()} takes Object, rather than T.
   *
   * @param wavelet The wavelet to remove.
   */
  public void removeWavelet(ObservableWavelet wavelet) {
    Preconditions.checkArgument(wavelets.containsKey(wavelet.getId()),
        "Removed a wavelet that doesn't exist: " + wavelet);
    wavelets.remove(wavelet.getId());
    triggerOnWaveletRemoved(wavelet);
  }

  public T createWavelet(WaveletId id) {
    T wavelet = factory.create(waveId, id, viewer);
    configurator.configure(wavelet);
    addWavelet(wavelet);
    return wavelet;
  }

  //
  // WaveView implementation:
  //

  @Override
  public T createRoot() {
    try {
      return createWavelet(rootId);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Attempted to create duplicate root wavelet", e);
    }
  }

  @Override
  public T createWavelet() {
    // Synthesize new id, and create a new channel for it.
    return createWavelet(idGenerator.newConversationWaveletId());
  }

  @Override
  public T createUserData() {
    return createWavelet(userDataId);
  }

  @Override
  public T getWavelet(WaveletId waveletId) {
    return wavelets.get(waveletId);
  }

  @Override
  public T getRoot() {
    return getWavelet(rootId);
  }

  @Override
  public T getUserData() {
    return getWavelet(userDataId);
  }

  @Override
  public Iterable<? extends T> getWavelets() {
    return wavelets.values();
  }

  @Override
  public WaveId getWaveId() {
    return waveId;
  }

  //
  // Observable extension:
  //

  @Override
  public void addListener(WaveViewListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(WaveViewListener listener) {
    listeners.remove(listener);
  }

  private void triggerOnWaveletAdded(ObservableWavelet wavelet) {
    for (WaveViewListener listener : listeners) {
      listener.onWaveletAdded(wavelet);
    }
  }

  private void triggerOnWaveletRemoved(ObservableWavelet wavelet) {
    for (WaveViewListener listener : listeners) {
      listener.onWaveletRemoved(wavelet);
    }
  }
}
