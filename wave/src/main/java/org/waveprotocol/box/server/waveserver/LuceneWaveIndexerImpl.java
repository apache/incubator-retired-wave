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

package org.waveprotocol.box.server.waveserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.waveprotocol.box.server.waveserver.PerUserWaveViewBus.Listener;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
@Singleton
public class LuceneWaveIndexerImpl extends AbstractWaveIndexer {

  private final PerUserWaveViewBus.Listener listener;

  @Inject
  public LuceneWaveIndexerImpl(WaveMap waveMap, WaveletProvider waveletProvider, Listener listener) {
    super(waveMap, waveletProvider);
    this.listener = listener;
  }

  @Override
  protected void processWavelet(WaveletName waveletName) {
    listener.onWaveInit(waveletName);
  }

  @Override
  protected void postIndexHook() {
    try {
      getWaveMap().unloadAllWavelets();
    } catch (WaveletStateException e) {
      throw new IndexException("Problem encountered while cleaning up", e);
    }
  }
}
