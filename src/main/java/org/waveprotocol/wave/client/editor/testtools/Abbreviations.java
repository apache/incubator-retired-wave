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

package org.waveprotocol.wave.client.editor.testtools;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a simple abbreviations map
 *
 */
public class Abbreviations {

  /**
   * A map of abbreviations
   */
  private static HashMap<String, String> map =
      new HashMap<String, String>();

  /**
   * Adds an abbreviation
   *
   * @param abbreviation
   * @param expansion
   */
  public void add(String abbreviation, String expansion) {
    map.put(abbreviation, expansion);
  }

  /**
   * Clears abbreviations
   */
  public void clear() {
    map.clear();
  }

  /**
   * @param abbreviated
   * @return abbreviated expanded according to current abbreviations
   */
  public String expand(String abbreviated) {
    String expanded = abbreviated;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      expanded = expanded.replaceAll(
          entry.getKey(), entry.getValue());
    }
    return expanded;
  }

}
