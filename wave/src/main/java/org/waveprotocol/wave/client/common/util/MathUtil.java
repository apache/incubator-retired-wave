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

package org.waveprotocol.wave.client.common.util;

/**
 * General maths utility functions that don't have anywhere else to live.
 *
 */
public final class MathUtil {

  private MathUtil() {
  }

  /**
   * Rounds a double to an int.
   *
   * @param x value to round
   * @return closest int value to x.
   */
  public static int roundToInt(double x) {
    return (int) Math.floor(x + 0.5);
  }

  /**
   * Clips a value within a range.
   *
   * @param lower  lower bound
   * @param upper  upper bound
   * @param x value to clip
   * @return clipped value of {@code x}
   */
  public static int clip(int lower, int upper, int x) {
    return Math.min(Math.max(x, lower), upper);
  }

  /**
   * Clips a value within a range.
   *
   * @param lower  lower bound
   * @param upper  upper bound
   * @param x value to clip
   * @return clipped value of {@code x}
   */
  public static double clip(double lower, double upper, double x) {
    return Math.min(Math.max(x, lower), upper);
  }
}
