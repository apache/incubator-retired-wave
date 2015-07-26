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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.util.DocumentContext;

/**
 * Tentative annotation change listener
 *
 * The changes map more or less to the operations received, with minimal optimisation.
 * Importantly, a large single change that is equivalent to many small changes (for
 * example, if we bold a large region that has every second character already bolded)
 * will still come up as a single large event.
 *
 * TODO(danilatos): Consider using Point rather than int as the location type
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationMutationHandler {

  /**
   * Notification that a change occurred.
   *
   * @param start
   * @param end
   * @param key
   * @param newValue The new value, possibly null
   */
  <N, E extends N, T extends N> void handleAnnotationChange(
      DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue);
}
