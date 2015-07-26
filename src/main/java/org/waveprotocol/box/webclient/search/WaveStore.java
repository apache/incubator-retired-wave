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

package org.waveprotocol.box.webclient.search;

import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Map;

/**
 * Reveals access to a group of open waves.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface WaveStore extends SourcesEvents<WaveStore.Listener> {

  interface Listener {
    void onOpened(WaveContext wave);
    void onClosed(WaveContext wave);
  }

  /**
   * Adds a wave to the store.
   */
  void add(WaveContext wave);

  /**
   * Removes a wave from the store.
   */
  void remove(WaveContext wave);

  /**
   * @return the collection of currently open waves.
   */
  Map<WaveId, WaveContext> getOpenWaves();
}
