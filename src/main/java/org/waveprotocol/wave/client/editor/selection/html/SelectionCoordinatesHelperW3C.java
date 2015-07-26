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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.model.util.IntRange;

/**
 * Standard browser specific implementation of SelectionCoordinatesHelper.
 *
 */
class SelectionCoordinatesHelperW3C implements SelectionCoordinatesHelper {
  private static final Element SPAN = Document.get().createSpanElement();
  /**
   * Notify the mutationListener before/after transient dom mutation.
   */
  private final NativeSelectionUtil.MutationListener mutationListener;

  SelectionCoordinatesHelperW3C(NativeSelectionUtil.MutationListener mutationListener) {
    this.mutationListener = mutationListener;
  }

  @Override
  public OffsetPosition getNearestElementPosition() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionUnsafe();
    return selection == null ? null :
        getNearestElementPosition(selection.focusNode(), selection.focusOffset());
  }

  private OffsetPosition getNearestElementPosition(Node focusNode, int focusOffset) {
    Node startContainer;
    if (focusNode == null) {
      return null;
    }

    Element e =
      DomHelper.isTextNode(focusNode) ? focusNode.getParentElement() : focusNode.<Element>cast();
    return e == null ? null
        : new OffsetPosition(e.getOffsetLeft(), e.getOffsetTop(), e.getOffsetParent());
  }

  @Override
  public OffsetPosition getFocusPosition() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    // NOTE(patcoleman): selection can be unknown here or in unreadable shadow nodes.
    return selection == null ? null : getPosition(selection.focusNode(), selection.focusOffset());
  }

  @Override
  public OffsetPosition getAnchorPosition() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    // NOTE(patcoleman): selection can be unknown here or in unreadable shadow nodes.
    return selection == null ? null : getPosition(selection.anchorNode(), selection.anchorOffset());
  }

  private OffsetPosition getPosition(Node focusNode, int focusOffset) {
    if (focusNode == null || focusNode.getParentElement() == null) {
      // Return null if cannot get selection, or selection is inside an
      // "unattached" node.
      return null;
    }

    if (DomHelper.isTextNode(focusNode)) {
      // We don't want to split the existing child text node, so we just add
      // duplicate the string up to the offset.
      Node txt =
          Document.get().createTextNode(
              focusNode.getNodeValue().substring(0, focusOffset));
      try {
        mutationListener.startTransientMutations();
        focusNode.getParentNode().insertBefore(txt, focusNode);
        txt.getParentNode().insertAfter(SPAN, txt);
        OffsetPosition ret =
            new OffsetPosition(SPAN.getOffsetLeft(), SPAN.getOffsetTop(), SPAN.getOffsetParent());

        return ret;
      } finally {
        SPAN.removeFromParent();
        txt.removeFromParent();
        mutationListener.endTransientMutations();
      }
    } else {
      Element e = focusNode.cast();
      return new OffsetPosition(e.getOffsetLeft(), e.getOffsetTop(), e.getOffsetParent());
    }
  }

  @Override
  public IntRange getFocusBounds() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    // NOTE(patcoleman): selection can be unknown here or in unreadable shadow nodes.
    return selection == null ? null : getBounds(selection.focusNode(), selection.focusOffset());
  }

  private IntRange getBounds(Node node, int offset) {
    if (node == null || node.getParentElement() == null) {
      // Return null if cannot get selection, or selection is inside an
      // "unattached" node.
      return null;
    }

    if (DomHelper.isTextNode(node)) {
      Element parentElement = node.getParentElement();
      return new IntRange(parentElement.getAbsoluteTop(), parentElement.getAbsoluteBottom());
    } else {
      Element e = node.<Element>cast();
      return new IntRange(e.getAbsoluteTop(), e.getAbsoluteBottom());
    }
  }
}
