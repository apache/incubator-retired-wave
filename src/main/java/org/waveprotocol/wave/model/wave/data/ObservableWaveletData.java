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

package org.waveprotocol.wave.model.wave.data;

import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Do NOT extend this interface any further, choose to extend the top level interfaces if you want
 * to mix and match another set of features.
 *
 * This interface is simply a convenience interface that bounds the top level interfaces together
 * so that you don't have to pass 2 interfaces around all the time.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface ObservableWaveletData extends WaveletData, SourcesEvents<WaveletDataListener> {

  //
  // Covariant specialisation of factory type.
  //

  interface Factory<T extends ObservableWaveletData> extends WaveletData.Factory<T> {}
}
