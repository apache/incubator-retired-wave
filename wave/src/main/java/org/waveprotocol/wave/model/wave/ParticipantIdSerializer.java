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
package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.util.Serializer;

/**
 * A serializer for ParticipantId objects. Stores them as the address.
 *
 */
public final class ParticipantIdSerializer {

  public static final Serializer<ParticipantId> INSTANCE = new Serializer<ParticipantId>() {
    @Override
    public ParticipantId fromString(String s) {
      // NOTE(user): We don't have a good general way of handling malformed
      // data :-(
      return s == null ? null : ParticipantId.ofUnsafe(s);
    }

    @Override
    public ParticipantId fromString(String s, ParticipantId defaultValue) {
      if (s != null) {
        return fromString(s);
      }
      return defaultValue;
    }

    @Override
    public String toString(ParticipantId x) {
      return x == null ? null : x.getAddress();
    }
  };

  private ParticipantIdSerializer() {
  }
}
