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


package org.waveprotocol.wave.client.paging;

/**
 * Defines the intersection relationship between two regions.
 *
 */
public enum OverlapKind {
  /** a1 &lt; a2 &lt; b1 &lt; b2 */
  FULLY_BEFORE,
  /** b1 &lt; b2 &lt; a1 &lt; a2 */
  FULLY_AFTER,
  /** a1 &lt; b1 &lt; a2 &lt; b2 */
  INTERSECTS_BEFORE,
  /** b1 &lt; a1 &lt; b2 &lt; a2 */
  INTERSECTS_AFTER,
  /** a1 &lt; b1 &lt; b2 &lt; a2 */
  ENCLOSES,
  /** b1 &lt; a1 &lt; a2 &lt; b2 */
  ENCLOSED;

  /**
   * Compares two regions.
   */
  public static OverlapKind compare(Region a, Region b) {
    // [ ... ] = a
    // < ... > = b
    if (a.getEnd() < b.getStart()) {
      // ... [ ... ] ... < ... > ...
      return OverlapKind.FULLY_BEFORE;
    } else if (b.getEnd() < a.getStart()) {
      // ... < ... > ... [ ... ] ...
      return OverlapKind.FULLY_AFTER;
    } else if (b.getStart() < a.getStart()) {
      if (b.getEnd() < a.getEnd()) {
        // ... < ... [ ... > ... ] ...
        return OverlapKind.INTERSECTS_AFTER;
      } else {
        // ... < ... [ ... ] ... > ...
        return OverlapKind.ENCLOSED;
      }
    } else {
      if (a.getEnd() < b.getEnd()) {
        // ... [ ... < ... ] ... > ...
        return OverlapKind.INTERSECTS_BEFORE;
      } else {
        // ... [ ... < ... > ... ] ...
        return OverlapKind.ENCLOSES;
      }
    }
  }
}
