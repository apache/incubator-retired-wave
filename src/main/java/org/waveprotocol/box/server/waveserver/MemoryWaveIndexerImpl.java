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

import org.waveprotocol.wave.model.id.WaveletName;


/**
 * Implements the waves view initialization for memory based waves view
 * provider.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@Singleton
public class MemoryWaveIndexerImpl extends AbstractWaveIndexer {

  @Inject
  public MemoryWaveIndexerImpl(WaveMap waveMap, WaveletProvider waveletProvider) {
    super(waveMap, waveletProvider);
  }

  @Override
  protected void processWavelet(WaveletName waveletName) {
    // No op.
  }

  @Override
  protected void postIndexHook() {
    // No op.
  }
}
