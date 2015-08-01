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

package org.waveprotocol.wave.model.schema;

/**
 * Various utilities that can be used by document schemas.
 *
 */
public class SchemaUtils {

  public static final String[] BOOLEAN_VALUES = {"true", "false"};

  public static boolean isValidInteger(String value, int min) {
    try {
      int x = Integer.parseInt(value);
      return x >= min;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isNonNegativeInteger(String value) {
    return isValidInteger(value, 0);
  }

  public static boolean isPositiveInteger(String value) {
    return isValidInteger(value, 1);
  }

  public static boolean isWaveletId(String value) {
    // NOTE(user): This could/should be strengthened to test that value
    // looks like a wavelet id. But for now, the wavelet-id domain type is
    // approximated as just a string.
    return true;
  }

  public static boolean isLong(String value) {
    try {
      Long.parseLong(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isDouble(String value) {
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private SchemaUtils() { }

}
