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

package org.waveprotocol.wave.model.wave.undo;

import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

/**
 * Utility methods.
 *
 */
final class OpUtils {
  private OpUtils() {
  }

  /**
   * Returns true iff the wavelet operation is considered a no-op as far as undo
   * is concerned.
   *
   * @param op
   */
  static boolean isNoop(WaveletOperation op) {
    if (op instanceof VersionUpdateOp || op instanceof NoOp) {
      return true;
    } else if (op instanceof AddParticipant || op instanceof RemoveParticipant
        || op instanceof WaveletBlipOperation) {
      return false;
    } else {
      // This happens when the op parameter is a non-core subclass of
      // WaveletOperation, not supported by wave.
      throw new RuntimeException("Unhandled op:" + op.getClass());
    }
  }
}
