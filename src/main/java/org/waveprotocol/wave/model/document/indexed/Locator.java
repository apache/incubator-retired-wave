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

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * A utility class for obtaining locations in an indexed DOM structure.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author alexmah@google.com (Alex Mah)
 */
public class Locator {

  /**
   * Gets the location before a given node.
   *
   * @param <N> The type of DOM nodes.
   * @param doc The indexed document.
   * @param node A node.
   * @return The location before the given node.
   */
  public static <N> int before(ReadableWDocument<N, ?, ?> doc, N node) {
    return doc.getLocation(node);
  }

  /**
   * Gets the location after a given node.
   *
   * @param <N> The type of DOM nodes.
   * @param doc The indexed document.
   * @param node A node.
   * @return The location after the given node.
   */
  public static <N> int after(ReadableWDocument<N, ?, ?> doc, N node) {
    return doc.getLocation(Point.after(doc, node));
  }

  /**
   * Gets the location of the start of a given element.
   *
   * @param <E> The type of DOM Element nodes.
   * @param doc The indexed document.
   * @param element An element.
   * @return The location of the start of the given element.
   */
  public static <N, E extends N> int start(ReadableWDocument<N, E, ?> doc, E element) {
    return doc.getLocation(Point.start(doc, element));
  }

  /**
   * Gets the location of the end of a given element.
   *
   * @param <N> The type of DOM nodes.
   * @param <E> The type of DOM Element nodes.
   * @param doc The indexed document.
   * @param element An element.
   * @return The location of the end of the given element.
   */
  public static <N, E extends N> int end(ReadableWDocument<N, E, ?> doc, E element) {
    return doc.getLocation(Point.<N>end(element));
  }

}
