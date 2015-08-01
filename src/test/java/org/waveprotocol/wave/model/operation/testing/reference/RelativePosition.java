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

package org.waveprotocol.wave.model.operation.testing.reference;

/**
 * A tracker that tracks the positions of two cursors relative to each other.
 *
 * @author Alexandre Mah
 */
final class PositionTracker {

  /**
   * The relative position of a cursor relative to another cursor.
   */
  interface RelativePosition {

    /**
     * Increases the relative position of the cursor.
     *
     * @param amount the amount by which to increase the relative position
     */
    void increase(int amount);

    /**
     * @return the relative position
     */
    int get();

  }

  int position = 0;

  /**
   * @return a RelativePosition representing the first cursor's position
   *         relative to the second cursor
   */
  RelativePosition getPosition1() {
    return new RelativePosition() {

      public void increase(int amount) {
        position += amount;
      }

      public int get() {
        return position;
      }

    };
  }

  /**
   * @return a RelativePosition representing the second cursor's position
   *         relative to the first cursor
   */
  RelativePosition getPosition2() {
    return new RelativePosition() {

      public void increase(int amount) {
        position -= amount;
      }

      public int get() {
        return -position;
      }

    };
  }

}
