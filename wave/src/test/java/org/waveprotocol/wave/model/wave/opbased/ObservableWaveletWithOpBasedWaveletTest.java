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

import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ObservableWaveletTestBase;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

/**
 * A test case that binds the black-box test methods in
 * {@link ObservableWaveletTestBase} with the {@link OpBasedWavelet} implementation,
 * backed by a {@link WaveletDataImpl}.
 *
 */

public final class ObservableWaveletWithOpBasedWaveletTest extends ObservableWaveletTestBase {
  private final FakeWaveView view = BasicFactories.fakeWaveViewBuilder().build();

  @Override
  protected ObservableWavelet createWavelet() {
    return view.create();
  }
}
