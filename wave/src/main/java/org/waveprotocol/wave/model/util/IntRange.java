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
 * An integer range.
 *
 * A pair of integers with additional constraint that start must be less than or
 * equal to end.
 *
 */
public final class IntRange extends IntPair {

  /**
   * Constructs an integer range.
   *
   * @param start start of the range.
   * @param end end of the range, must be greater than or equal to start.
   */
  public IntRange(int start, int end) {
    super(start, end);
    Preconditions.checkArgument(start <= end, "Start of range must be <= end");
  }

  public IntRange(int collapsedAt) {
    super(collapsedAt, collapsedAt);
  }
}
