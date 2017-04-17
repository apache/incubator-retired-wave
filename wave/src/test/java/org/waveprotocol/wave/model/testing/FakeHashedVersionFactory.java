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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;

/**
 * A hashed version factory which generates unsigned versions.
 *
 * @author anorth@google.com (Alex North)
 */
public final class FakeHashedVersionFactory implements HashedVersionFactory {

  public static final HashedVersionFactory INSTANCE = new FakeHashedVersionFactory();

  @Override
  public HashedVersion createVersionZero(WaveletName waveletName) {
    return HashedVersion.unsigned(0);
  }

  @Override
  public HashedVersion create(byte[] appliedDeltaBytes, HashedVersion versionAppliedAt,
      int opsApplied) {
    return HashedVersion.unsigned(versionAppliedAt.getVersion() + opsApplied);
  }
}
