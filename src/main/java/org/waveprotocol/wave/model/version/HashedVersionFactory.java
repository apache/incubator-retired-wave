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

package org.waveprotocol.wave.model.version;

import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Factory for hashed versions.
 *
 * @author anorth@google.com (Alex North)
 */
public interface HashedVersionFactory {
  /** Creates a hashed version zero for a wavelet. */
  HashedVersion createVersionZero(WaveletName waveletName);

  /**
   * Creates a hashed version after the application of a delta. The new version
   * is targetVersion + opsApplied.
   *
   * @param appliedDeltaBytes byte representation of applied delta
   * @param versionAppliedAt version to which the delta applied
   * @param opsApplied number of ops in the delta
   * @return a hashed version after the delta application
   */
  HashedVersion create(byte[] appliedDeltaBytes, HashedVersion versionAppliedAt, int opsApplied);
}
