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

package org.waveprotocol.box.server.frontend;


import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * A wavelet snapshot with committed version.
 *
 * @author anorth@google.com (Alex North)
*/
public final class CommittedWaveletSnapshot {
  public final ReadableWaveletData snapshot;
  public final HashedVersion committedVersion;

  public CommittedWaveletSnapshot(ReadableWaveletData snapshot,
      HashedVersion committedVersion) {
    Preconditions.checkNotNull(snapshot);
    Preconditions.checkNotNull(committedVersion);
    this.snapshot = snapshot;
    this.committedVersion = committedVersion;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj != null && getClass() == obj.getClass()) {
      CommittedWaveletSnapshot other = (CommittedWaveletSnapshot) obj;
      return committedVersion.equals(other.committedVersion) && snapshot.equals(other.snapshot);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((committedVersion == null) ? 0 : committedVersion.hashCode());
    result = prime * result + ((snapshot == null) ? 0 : snapshot.hashCode());
    return result;
  }
}
