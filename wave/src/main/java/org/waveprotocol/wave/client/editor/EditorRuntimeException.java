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

package org.waveprotocol.wave.client.editor;

/**
 * Exceptions from Editor
 *
 */
@SuppressWarnings("serial")
public class EditorRuntimeException extends RuntimeException {

  /**
   * @param reason Human readable reason for exception
   */
  public EditorRuntimeException(String reason) {
    super(reason);
  }

  /**
   * @param reason Human readable reason for exception
   * @param object A toString will be appended to reason
   */
  public EditorRuntimeException(String reason, Object object) {
    super(reason + ": " + object);
  }

  /**
   * @param reason Human readable reason for exception
   * @param object1 A toString will be appended to reason
   * @param object2 A toString will be appended to reason
   */
  public EditorRuntimeException(String reason, Object object1, Object object2) {
    super(reason + ": " + object1 + " " + object2);
  }

}
