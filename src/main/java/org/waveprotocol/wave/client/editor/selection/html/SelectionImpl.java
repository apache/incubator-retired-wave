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

import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Standard (Safari, Firefox) browser-specific selection implementation
 */
abstract class SelectionImpl {

  /**
   * Shorthand selection debug logger
   */
  static LoggerBundle logger = NativeSelectionUtil.LOG;

  /**
   * Fast implementation to check if there is a selection or not.
   * @return true if there is a selection
   */
  abstract boolean selectionExists();

  /**
   * @return Current selection
   */
  abstract FocusedPointRange<Node> get();

  /**
   * @return Current selection
   */
  abstract PointRange<Node> getOrdered();

  /**
   * @return true if the selection is ordered
   */
  abstract boolean isOrdered();

  /**
   * Sets selection
   * @param anchor
   * @param focus
   */
  abstract void set(Point<Node> anchor, Point<Node> focus);

  /**
   * Sets selection
   *
   * @param point
   */
  abstract void set(Point<Node> focus);

  /**
   * Clears the selection
   */
  abstract void clear();

  /**
   * Saves the selection internally in a manner optimised for each browser
   */
  abstract void saveSelection();

  /**
   * Restores the selection saved with {@link #saveSelection()}
   *
   * Behaviour is undefined if the DOM has been changed since the selection
   * was saved.
   */
  abstract void restoreSelection();
}
