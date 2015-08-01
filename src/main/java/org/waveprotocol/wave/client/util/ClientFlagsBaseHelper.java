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


package org.waveprotocol.wave.client.util;

public final class ClientFlagsBaseHelper {

  private final TypedSource source;

  /**
   * This class is package-private and designed to be used by ClientFlags and
   * ClientFlagsBase only.
   *
   * @param source source containing parameters
   */
  ClientFlagsBaseHelper(TypedSource source) {
    this.source = source;
  }

  /**
   * Helper that returns a if a is not null, else returns b.
   * NOTE(user): Does such function already exist?
   */
  private <T> T returnDefaultHelper(T a, T b) {
    return a != null ? a : b;
  }

  /**
   * Get the parameter from source, if null return default value.
   */
  public Boolean getBoolean(String tag, Boolean defaultValue) {
    return returnDefaultHelper(source.getBoolean(tag), defaultValue);
  }

  /**
   * Get the parameter from source, if null return default value.
   */
  public String getString(String tag, String defaultValue) {
    return returnDefaultHelper(source.getString(tag), defaultValue);
  }

  /**
   * Get the parameter from source, if null return default value.
   */
  public Integer getInteger(String tag, Integer defaultValue) {
    return returnDefaultHelper(source.getInteger(tag), defaultValue);
  }

  /**
   * Get the parameter from source, if null return default value.
   */
  public Double getDouble(String tag, Double defaultValue) {
    return returnDefaultHelper(source.getDouble(tag), defaultValue);
  }
}
