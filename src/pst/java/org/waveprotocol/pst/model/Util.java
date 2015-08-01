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


package org.waveprotocol.pst.model;

/**
 * Util methods for model objects.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Util {

  private Util() {
  }

  /**
   * @return the given string, capitalized ("fooBar" = "FooBar")
   */
  public static String capitalize(String s) {
    return s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * @return the given string, uncapitalized ("FooBar" = "fooBar")
   */
  public static String uncapitalize(String s) {
    return s.isEmpty() ? "" : Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }
}
