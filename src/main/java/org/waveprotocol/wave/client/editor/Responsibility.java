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
 * Used to distinguish between locally sourced operations that are a direct
 * result of user action from those that are indirect.
 *
 * For example, typing would normally be considered a direct operation, whereas
 * a background task that annotates the document in response to a user action,
 * even just automatically, would be indirect.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Responsibility {

  /**
   * Identifies the responsibility of currently generated locally sourced
   * operations
   */
  public interface Manager extends Responsibility {
    /**
     * Indicates the start of a sequence of operations directly caused by the
     * user.
     *
     * Indirect and direct sequences may nest within eachother
     */
    void startDirectSequence();

    /**
     * End of sequence started by {@link #startDirectSequence()}
     */
    void endDirectSequence();

    /**
     * Indicates the start of a sequence of operations not directly caused by the
     * user.
     *
     * Indirect and direct sequences may nest within eachother
     */
    void startIndirectSequence();

    /**
     * End of sequence started by {@link #endIndirectSequence()}
     */
    void endIndirectSequence();
  }

  /**
   * @return true if local ops currently being applied are "direct"
   */
  boolean withinDirectSequence();
}
