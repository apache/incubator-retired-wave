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

package org.waveprotocol.wave.client.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Exposes the location of the native browser selection, with respect to a
 * document. Selections that span documents are ignored.
 */
public final class HtmlSelectionHelperImpl implements HtmlSelectionHelper {

  /** Document area that constrains the selection. */
  private final Element doc;

  public HtmlSelectionHelperImpl(Element doc) {
    this.doc = doc;
  }

  @Override
  public FocusedPointRange<Node> getHtmlSelection() {
    FocusedPointRange<Node> selection = NativeSelectionUtil.get();
    // NOTE(user): We also need to check that the selection is not inside a
    // child editor, if it is the selection with respect to this editor is null.
    // TODO(user, danilatos): There is a more general problem of selection
    // inside textboxes, i.e. search gadget that needs to be addressed.
    if (selection == null || !isFullyInside(selection, doc) || isInChildEditor(selection)) {
      return null;
    }
    return selection;
  }

  private boolean isInChildEditor(FocusedPointRange<Node> selection) {
    return selection.isCollapsed() ? isInChildEditor(selection.getAnchor()) :
        isInChildEditor(selection.getAnchor()) || isInChildEditor(selection.getFocus());
  }

  private boolean isInChildEditor(Point<Node> point) {
    // The editable doc is marked by an attribute EDITABLE_DOC_MARKER, if
    // an element is found with that attribute, and is not the element of this
    // editor's doc element, then it must be a child's doc element.
    Node n = point.getContainer();
    Element e = DomHelper.isTextNode(n) ? n.getParentElement() : (Element) n;
    while (e != null && e != doc) {
      if (e.hasAttribute(EditorImpl.EDITABLE_DOC_MARKER)) {
        return true;
      }
      e = e.getParentElement();
    }
    return false;
  }

  /**
   * Tests if the given selection is fully inside the given node.
   *
   * @param selection
   * @param node
   * @return true if the given selection is fully inside the given node.
   */
  private boolean isFullyInside(FocusedPointRange<Node> selection, Node node) {
    return node.isOrHasChild(selection.getAnchor().getContainer())
        && node.isOrHasChild(selection.getFocus().getContainer());
  }

  @Override
  public PointRange<Node> getOrderedHtmlSelection() {
    // TODO(danilatos): Optimise
    if (getHtmlSelection() == null) {
      return null;
    }
    return NativeSelectionUtil.getOrdered();
  }
}
