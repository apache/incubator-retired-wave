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

import org.waveprotocol.wave.model.util.Serializer;

/**
 * Serializer for wavelet ids to be embedded in strings.
 *
 * @author anorth@google.com (Alex North)
 */
public final class WaveletIdSerializer {

  /**
   * Singleton instance
   */
  public static final Serializer<WaveletId> INSTANCE = new Serializer<WaveletId>() {
    @Override
    public WaveletId fromString(String s) {
      return fromString(s, null);
    }

    /**
     * Deserializes a wavelet id string.
     *
     * @param s serialized wavelet id
     * @return wavelet id represented by {@code s}
     */
    private WaveletId deserialize(String s) {
      try {
        return ModernIdSerialiser.INSTANCE.deserialiseWaveletId(s);
      } catch (InvalidIdException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public WaveletId fromString(String s, WaveletId defaultValue) {
      return (s != null) ? deserialize(s) : defaultValue;
    }

    @Override
    public String toString(WaveletId x) {
      return (x != null) ? ModernIdSerialiser.INSTANCE.serialiseWaveletId(x) : null;
    }
  };

  private WaveletIdSerializer() {}
}
