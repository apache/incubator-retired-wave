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

package org.waveprotocol.wave.model.document.util;

/**
 * Checked exception thrown by methods that find a node in some context.
 *
 * Such methods usually succeed but rarely fail, and as such it is dangerous simply to return null
 * for when the node is not found, as the corner case might not initially be noticed in calling
 * code. Therefore the use of this checked exception is encouraged.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class NodeNotFoundException extends Exception {
  /***/
  public NodeNotFoundException(String message) {
    super(message);
  }

  /***/
  public NodeNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String toString() {
    return "NodeNotFoundException[" + super.toString() + "]";
  }

  /**
   * Rethrow as a runtime exception, providing a reason why it is an impossible
   * condition, hence warranting promotion from checked to runtime.
   *
   * @param reasonWhyImpossible
   */
  public void throwAsRuntime(String reasonWhyImpossible) {
    throw new RuntimeException(reasonWhyImpossible, this);
  }
}
