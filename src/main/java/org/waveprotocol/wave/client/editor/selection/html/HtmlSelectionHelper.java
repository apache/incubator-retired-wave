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
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Simple interface for getting the html selection. Should use this where
 * possible instead of Selection.get() because some implementations might want
 * to do things like return null if the selection is not inside the editor's div
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface HtmlSelectionHelper {

  /**
   * @return Current html selection, or null if none or invalid
   */
  FocusedPointRange<Node> getHtmlSelection();

  /**
   * Same as {@link #getHtmlSelection()}, but as an ordered range
   */
  PointRange<Node> getOrderedHtmlSelection();
}
