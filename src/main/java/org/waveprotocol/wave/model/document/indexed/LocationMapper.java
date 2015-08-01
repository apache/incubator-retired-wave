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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.util.Point;


/**
 * Provides methods for mapping between nodes and int locations
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <N> Node type
 */
public interface LocationMapper<N> extends SizedObject {

  /**
   * Gets the point in the DOM indexed by a given location.
   *
   * @param location a location.
   * @return the point at the given location.
   */
  Point<N> locate(int location);

  /**
   * Gets the location of a given node in the DOM.
   *
   * @param node a DOM node.
   * @return the location of the given node.
   */
  int getLocation(N node);

  /**
   * Gets the location of a given point in the DOM.
   *
   * @param point a point in the DOM.
   * @return the location of the given point.
   */
  int getLocation(Point<N> point);
}
