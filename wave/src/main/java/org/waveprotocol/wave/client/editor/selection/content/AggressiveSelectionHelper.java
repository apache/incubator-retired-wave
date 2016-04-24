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

package org.waveprotocol.wave.client.editor.selection.content;

import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.client.editor.content.paragraph.Line;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * A selection helper that tries to correct the selection whenever it is found
 * to be in an invalid location (even when reading it). Corrections may have
 * side effects such as generating and applying operations.
 *
 * If in doubt, use the passive selection helper.
 *
 * @see PassiveSelectionHelper
 * @see SelectionHelper
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class AggressiveSelectionHelper extends PassiveSelectionHelper {

  private final CMutableDocument mutableDocument;

  /**
   * @param bundle
   * @param htmlHelper
   */
  public AggressiveSelectionHelper(HtmlSelectionHelper htmlHelper, NodeManager nodeManager,
      ContentView renderedContentView, LocationMapper<ContentNode> locationMapper,
      CMutableDocument mutableDocument) {
    super(htmlHelper, nodeManager, renderedContentView, locationMapper);

    this.mutableDocument = mutableDocument;
  }

  @Override
  protected ContentElement maybePlaceMissingCursorContainer(Point.El<ContentNode> at) {
    logger.error().log("PROBLEM: Had to create a line container to accommodate the cursor!");

    ContentElement line = null;
    if (!LineContainers.isLineContainer(FullContentView.INSTANCE, at.getContainer())) {
      // create line container and line atomically:
      line = LineContainers.appendLine(mutableDocument, null, Attributes.EMPTY_MAP);
    } else {
      line = LineContainers.appendLine(mutableDocument, (ContentElement) at.getContainer(), null);
    }
    // return the local p element as the target for the cursor:
    ContentElement inserted = Line.fromLineElement(line).getParagraph();
    needsCorrection = true;
    return inserted;
  }

  @Override
  protected Point<ContentNode> nodeletPointToWrapperPointAttempt2(Point<Node> nodelet)
      throws HtmlInserted, HtmlMissing {
    // Try again after a flush.
    // The normal case when this happens, is on the second character of a typing sequence
    // when node == a text node with no corresponding content text node.
    // This catch should remain here as a failsafe, but it would also be nice to improve
    // the code in nodeManager to deal with that scenario and return a Point.El.
    flushForUnextractedText();
    needsCorrection = true;
    return nodeManager.nodeletPointToWrapperPoint(nodelet);
  }

  protected abstract void flushForUnextractedText();
}
