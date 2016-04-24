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

import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Indicates a caller submitted a delta with a mismatched hash.
 *
 */
public class InvalidHashException extends WaveServerException {
  private final HashedVersion expected;
  private final HashedVersion target;

  public InvalidHashException(HashedVersion expectedVersion, HashedVersion targetVersion) {
    // Note: The expected hash is not included in the exception message
    // to avoid revealing it to clients. It's useful to keep a reference
    // here for debugging though.
    super("Mismatched hash at version " + expectedVersion.getVersion() + ": " + targetVersion);
    this.expected = expectedVersion;
    this.target = targetVersion;
  }

  public HashedVersion expectedVersion() {
    return expected;
  }

  public HashedVersion targetVersion() {
    return target;
  }
}
