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

package org.waveprotocol.wave.communication.json;

/**
 * Helper methods used to convert to and from a Long represented as a pair of
 * (high, low) Integers, since JSON doesn't handle Longs very well.
 *
 */
public class JsonLongHelper {
  private JsonLongHelper() {}

  /**
   * The index in the array for the lower 4 bytes of the long
   */
  public static int LOW_WORD_INDEX = 0;

  /**
   * The index in the array for the higher 4 bytes of the long
   */
  public static int HIGH_WORD_INDEX = 1;

  /**
   * @param value
   * @return the lower word of a long value.
   */
  public static int getLowWord(long value) {
    return (int)(value & 0xFFFFFFFFL);
  }

  /**
   * @param value
   * @return the lower word of a long value.
   */
  public static int getHighWord(long value) {
    return (int)(value >> 32);
  }

  /**
   * Combine the lower word and the higher word into a long
   * @param highWord
   * @param lowWord
   * @return the long of the 2 word combined.
   */
  public static long toLong(int highWord, int lowWord) {
    long value = lowWord;
    // We are avoiding the long shift and the bitwise in favor of the branch because
    // in GWT compiled code, shift and or is really expensive (see
    // @com.google.gwt.lang.LongLib)
    if (!((highWord == 0 && lowWord > 0) || (highWord == -1 && lowWord < 0))) {
      value &= 0xFFFFFFFFL;
      value |= ((long) highWord) << 32;
    }
    return value;
  }
}
