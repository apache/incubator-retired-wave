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

package org.waveprotocol.wave.model.util;

/**
 * An immutable pair of integers.
 *
 */
public class IntPair {
  private final int first;
  private final int second;

  /**
   * Constructs a pair of integers.
   * @param first
   * @param second
   */
  public IntPair(int first, int second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Returns the first integer in the pair.
   */
  public int getFirst() {
    return first;
  }

  /**
   * Returns the second integer in the pair.
   */
  public int getSecond() {
    return second;
  }
}
