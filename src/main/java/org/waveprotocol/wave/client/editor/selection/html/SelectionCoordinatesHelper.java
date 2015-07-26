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

package org.waveprotocol.wave.client.editor.selection.html;

import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.model.util.IntRange;

/**
 * Mechanism for obtaining the x, y offset of the current selection.
 *
 */
interface SelectionCoordinatesHelper {

  /**
   * Gets the position of the nearest element of the selection. If the selection
   * is a textnode, it will return the position of its parent, otherwise it will
   * return its own position.
   */
  OffsetPosition getNearestElementPosition();

  /**
   * Gets the position of the current selection focus.  This is the position where
   * the selection was started, for mouse selections the position corresponding to
   * the mouse down.
   */
  OffsetPosition getFocusPosition();

  /**
   * Gets the position of the current selection anchor.  This is the position where
   * the selection ends, for mouse selections the position corresponding to the
   * mouse up.
   */
  OffsetPosition getAnchorPosition();

  /**
   * Gets the absolute y-bounds of the cursor position.
   */
  IntRange getFocusBounds();
}
