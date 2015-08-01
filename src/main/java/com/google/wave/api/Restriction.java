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

package com.google.wave.api;

/**
 * A class that represents a property filter for element, that can be used when
 * when searching for an element inside a blip.
 */
public class Restriction {

  /** The key of the property. */
  private final String key;

  /** The value of the property. */
  private final String value;

  /**
   * Creates an instance of {@link Restriction} for a property with the given
   * key and value.
   *
   * @param key the key of the property to restrict.
   * @param value the value of the property to restrict.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction of(String key, String value) {
    return new Restriction(key, value);
  }

  /**
   * Constructor.
   *
   * @param key the key of the property to restrict.
   * @param value the value of the property to restrict.
   */
  private Restriction(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * @return the key of the property to restrict.
   */
  public String getKey() {
    return key;
  }

  /**
   * @return the value of the property to restrict.
   */
  public String getValue() {
    return value;
  }
}
