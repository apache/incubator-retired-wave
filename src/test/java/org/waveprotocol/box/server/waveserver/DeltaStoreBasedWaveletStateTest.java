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

import com.google.common.util.concurrent.MoreExecutors;

import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.concurrent.Executor;

/**
 * Runs wavelet state tests with the {@link DeltaStoreBasedWaveletState}.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class DeltaStoreBasedWaveletStateTest extends WaveletStateTestBase {

  private final Executor PERSIST_EXECUTOR = MoreExecutors.sameThreadExecutor();
  private DeltaStore store;

  @Override
  public void setUp() throws Exception {
    store = new MemoryDeltaStore();
    super.setUp();
  }

  @Override
  protected WaveletState createEmptyState(WaveletName name) throws Exception {
    return DeltaStoreBasedWaveletState.create(store.open(name), PERSIST_EXECUTOR);
  }

  @Override
  protected void awaitPersistence() throws Exception {
    // Same-thread executor already completed.
    return;
  }

  // TODO(soren): We need to add tests here that verify interactions with storage.
  // The base tests only test the public interface, not any interactions with the storage system.
}
