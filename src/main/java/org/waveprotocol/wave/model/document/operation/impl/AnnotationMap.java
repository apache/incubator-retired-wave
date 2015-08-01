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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.util.StateMap;

// TODO: Should this be in a different package? It's not related to
// operations.
/**
 * Implementations must be immutable.
 */
public interface AnnotationMap extends StateMap {

  /**
   * Returns a new map, updated with the differences
   * @param mutation the mutation must be compatible with the current map
   * @return a new immutable map
   */
  public AnnotationMap updateWith(AnnotationsUpdate mutation);

  /**
   * Same as {@link #updateWith(AnnotationsUpdate)}, but the mutation does
   * not have to be compatible. This is useful when the mutation may
   * already be known to be incompatible, but we wish still to perform it.
   *
   * This is useful for validity checking code, for example. In general,
   * {@link #updateWith(AnnotationsUpdate)} should be used instead.
   *
   * @param mutation does not have to be compatible
   * @return a new updated map, based on the key to new-value pairs, ignoring
   *   the old values.
   */
  public AnnotationMap updateWithNoCompatibilityCheck(AnnotationsUpdate mutation);
}
