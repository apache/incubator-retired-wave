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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.util.Serializer;

/**
 * Serializer for PrimitiveThreadStates.
 *
 */
public final class ThreadStateSerializer implements Serializer<PrimitiveThreadState> {

  private static final EnumSerializer<ThreadState> TYPE_SERIALIZER =
      new EnumSerializer<ThreadState>(ThreadState.class);

  @Override
  public PrimitiveThreadState fromString(String s) {
    return fromString(s, null);
  }

  @Override
  public PrimitiveThreadState fromString(String s, PrimitiveThreadState defaultValue) {
    ThreadState state = (s != null ? TYPE_SERIALIZER.fromString(s) : null);
    return state != null ? PrimitiveThreadState.of(state, 0) : defaultValue;
  }

  @Override
  public String toString(PrimitiveThreadState x) {
    return (x != null) ? TYPE_SERIALIZER.toString(x.getState()) : null;
  }
}
