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
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Selection implementation that does nothing. This is for user agents where
 * we are not providing proper editor support, such as mobile clients.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SelectionImplDisabled extends SelectionImpl {
  /** {@inheritDoc} */
  @Override
  public void set(Point<Node> point) {
    // Do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void set(Point<Node> start, Point<Node> end) {
    // Do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    // Do nothing
  }

  @Override
  FocusedPointRange<Node> get() {
    return null;
  }

  @Override
  PointRange<Node> getOrdered() {
    return null;
  }

  @Override
  boolean isOrdered() {
    return true;
  }

  @Override
  boolean selectionExists() {
    return false;
  }

  @Override
  void restoreSelection() {
    // Do nothing
  }

  @Override
  void saveSelection() {
    // Do nothing
  }
}
