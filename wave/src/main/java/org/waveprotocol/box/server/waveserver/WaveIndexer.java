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

/**
 * Provides interface for initialization of indexing logic.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public interface WaveIndexer {

  /**
   * Performs index re-making logic for specific implementation of
   * {@link WaveIndexer}. When working with indexed search will do nothing.
   *
   * @throws WaveletStateException if something goes wrong.
   * @throws WaveServerException if something goes wrong.
   */
  void remakeIndex() throws WaveletStateException, WaveServerException;
}
