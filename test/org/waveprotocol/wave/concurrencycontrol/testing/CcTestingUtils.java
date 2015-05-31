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

package org.waveprotocol.wave.concurrencycontrol.testing;

import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.util.ValueUtils;

public class CcTestingUtils {
  // for testing only -- not overriding .equals() because I don't want to override .hashCode()
  public static boolean deltasAreEqual(WaveletDelta delta1, WaveletDelta delta2) {
    if (delta1 == delta2) return true;
    if (delta1 == null || delta2 == null) return false;
    if (delta1.size() != delta2.size()) return false;
    if (!ValueUtils.equal(delta1.getTargetVersion(), delta2.getTargetVersion())) return false;
    for (int i = 0; i < delta1.size(); ++i) {
      if (!delta1.get(i).equals(delta2.get(i))) {
        return false;
      }
    }
    return true;
  }
}
