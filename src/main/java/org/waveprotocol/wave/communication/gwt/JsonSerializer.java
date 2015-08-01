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

package org.waveprotocol.wave.communication.gwt;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * An interface that is responsible for converting a javascript object to/from a
 * JSON string.
 *
 */
public interface JsonSerializer {
  /**
   * Serialize a javascript object into a JSON string
   * @param obj the JSO to serialize, this object must be simple and doesn't
   *     contains any loop.
   * @return JSON string
   */
  String serialize(JavaScriptObject obj);

  /**
   * Parse a json string into a JavaScriptObject
   *
   * @param str JSON string to parse
   * @return the JavaScriptObject contains the data in the json string.
   */
  JavaScriptObject parse(String str);
}
