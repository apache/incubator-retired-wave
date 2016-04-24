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

package org.waveprotocol.box.webclient.client;

import com.google.common.base.Preconditions;

import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import java.util.Collections;
import java.util.Map;

/**
 * A trivial implementation of a wave store.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SimpleWaveStore implements WaveStore {

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();
  private final Map<WaveId, WaveContext> waves = CollectionUtils.newHashMap();
  private final Map<WaveId, WaveContext> wavesView = Collections.unmodifiableMap(waves);

  /**
   * Creates a wave store.
   */
  public SimpleWaveStore() {
  }

  @Override
  public void add(WaveContext wave) {
    WaveId id = wave.getWave().getWaveId();
    Preconditions.checkArgument(!waves.containsKey(id));
    waves.put(id, wave);
    fireOnWaveOpened(wave);
  }

  @Override
  public void remove(WaveContext wave) {
    WaveId id = wave.getWave().getWaveId();
    Preconditions.checkArgument(waves.get(id) == wave);
    waves.remove(id);
    fireOnWaveClosed(wave);
  }

  @Override
  public Map<WaveId, WaveContext> getOpenWaves() {
    return wavesView;
  }

  //
  // Events
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnWaveOpened(WaveContext wave) {
    for (Listener listener : listeners) {
      listener.onOpened(wave);
    }
  }

  private void fireOnWaveClosed(WaveContext wave) {
    for (Listener listener : listeners) {
      listener.onClosed(wave);
    }
  }
}
