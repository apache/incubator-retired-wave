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

import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.util.Serializer;

/**
 * A serializer for wavelet hashed versions. Represents them as
 * pair-encoded strings: [version]:[hash base64]
 *
 * @author anorth@google.com (Alex North)
 */
public final class HashedVersionSerializer {
  private static final byte[] EMPTY = new byte[0];

  /**
   * Singleton instance
   */
  public static final Serializer<HashedVersion> INSTANCE = new Serializer<HashedVersion>() {
    @Override
    public HashedVersion fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public HashedVersion fromString(String s, HashedVersion defaultValue) {
      if (null == s) {
        return defaultValue;
      }

      String[] pair = s.split(":", 2);
      long v = Long.parseLong(pair[0]);
      byte[] h = EMPTY;
      if (pair.length == 2) {
        try {
          h = CharBase64.decode(pair[1]);
        } catch (Base64DecoderException e) {
          throw new IllegalArgumentException("Invalid base64 hash: '" + pair[1] + "'");
        }
      }
      return HashedVersion.of(v, h);
    }

    @Override
    public String toString(HashedVersion x) {
      return (x != null) ?  (x.getVersion() + ":" + CharBase64.encode(x.getHistoryHash())) : null;
    }
  };

  private HashedVersionSerializer() {}
}
