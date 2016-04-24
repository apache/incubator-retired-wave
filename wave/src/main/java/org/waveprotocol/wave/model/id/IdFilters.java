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

package org.waveprotocol.wave.model.id;

import java.util.Collections;

/**
 * This class contains a few static IdFilter instances and methods.
 *
 */
public class IdFilters {

  /** An id filter that accepts all ids. */
  public static final IdFilter ALL_IDS = IdFilter.of(
      Collections.<WaveletId>emptySet(), Collections.singleton(""));

  /** An IdFilter that accepts no ids. */
  public static final IdFilter NO_IDS = IdFilter.of(
      Collections.<WaveletId>emptySet(), Collections.<String>emptySet());

  /** An id filter that accepts only conversation wavelets. */
  public static final IdFilter CONVERSATION_WAVELET_IDS = IdFilter.ofPrefixes(
      IdUtil.CONVERSATION_WAVELET_PREFIX);

  protected IdFilters() {
    // hidden constructor
  }
}
