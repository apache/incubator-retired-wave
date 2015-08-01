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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.dom.client.Element;

/**
 * Provides layout properties of elements. In particular, reveals such
 * properties in fractional units, on browsers that have sub-pixel rendering.
 *
 */
public interface Measurer {
  /**
   * Gets the position of the top of an element's offset box, either relative to
   * the top of another element, or relative to the viewport.
   *
   * @param base element whose top defines the origin, or {@code null} for the
   *        top of the viewport being the origin
   * @param e element to measure
   * @return the top position of {@code e}, relative to the top of {@code base}.
   */
  double top(Element base, Element e);

  /**
   * Gets the position of the left of an element's offset box, either relative to
   * the left of another element, or relative to the viewport.
   *
   * @param base element whose top defines the origin, or {@code null} for the
   *        left of the viewport being the origin
   * @param e element to measure
   * @return the left position of {@code e}, relative to the left of {@code base}.
   */
  double left(Element base, Element e);

  /**
   * Gets the position of the bottom of an element's offset box, either relative
   * to the top of another element, or relative to the viewport.
   *
   * @param base element whose top defines the origin, or {@code null} for the
   *        top of the viewport being the origin
   * @param e element to measure
   * @return the bottom position of {@code e}, relative to the top of {@code
   *         base}.
   */
  double bottom(Element base, Element e);

  /** @return the (offset) height of an element. */
  double height(Element e);

  /** @return the top of an element relative to its offset parent. */
  double offsetTop(Element e);

  /** @return the bottom of an element relative to its offset parent. */
  double offsetBottom(Element e);
}
