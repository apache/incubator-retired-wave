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

import org.waveprotocol.wave.model.util.Pair;

/**
 * Holds the state that records a user action of collapsing or expanding a
 * thread. Specifically, a pair of the state into which the thread was put, and
 * the wavelet version at which it was put into that state.
 *
 */
public final class PrimitiveThreadState extends Pair<ThreadState, Integer> implements
    Comparable<PrimitiveThreadState> {

  private PrimitiveThreadState(ThreadState state, Integer version) {
    super(state, version);
  }

  public static PrimitiveThreadState of(ThreadState state, Integer version) {
    return new PrimitiveThreadState(state, version);
  }

  public static PrimitiveThreadState expanded(int version) {
    return new PrimitiveThreadState(ThreadState.EXPANDED, version);
  }

  public static PrimitiveThreadState collapsed(int version) {
    return new PrimitiveThreadState(ThreadState.COLLAPSED, version);
  }

  public ThreadState getState() {
    return getFirst();
  }

  public Integer getVersion() {
    return getSecond();
  }

  @Override
  public int compareTo(PrimitiveThreadState o) {
    Integer thisVersion = getVersion();
    Integer otherVersion = o.getVersion();

    if (thisVersion == null && otherVersion == null) {
      return 0;
    } else if (thisVersion == null) {
      // Other is better than this one.
      return 1;
    } else if (otherVersion == null) {
      // Other is worse than this one.
      return -1;
    } else {
      return thisVersion.compareTo(otherVersion);
    }
  }
}
