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

package org.waveprotocol.wave.client.common.util;

/**
 * Base View interface.
 *
 * @param <L> listener interface for this view.
 */
public interface View<L> {

  interface Factory<V extends View<?>> extends org.waveprotocol.wave.client.common.util.Factory<V> {}

  /**
   * Initializes this view. The view is considered to be used until
   * {@link #reset()}.
   *
   * @param listener listener for events broadcast by this view
   */
  void init(L listener);

  /**
   * Releases this view from being used. It is up to each implementation type to
   * define if this view is reusable after this method is called.
   */
  void reset();
}
