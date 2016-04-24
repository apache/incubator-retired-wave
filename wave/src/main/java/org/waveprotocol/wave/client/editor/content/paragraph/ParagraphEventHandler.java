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

package org.waveprotocol.wave.client.editor.content.paragraph;

import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Default user event handler for paragraphs
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ParagraphEventHandler extends NodeEventHandlerImpl {

  /**
   * Handles an enter from inside the paragraph, by splitting the
   * paragraph in two, the second with a <p> tag.
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleEnter(ContentElement p, EditorEvent event) {
    maybeSplitForNewline(p, event.getCaret());
    return true;
  }

  /**
   * @see #maybeSplitForNewline(ContentElement, Point)
   */
  protected Point<ContentNode> maybeSplitForNewline(ContentElement p, ContentPoint splitAt) {
    return maybeSplitForNewline(p, splitAt.asPoint());
  }

  /**
   * Helper method that does the work with mutable document
   * @param splitAt
   * @return the point where the split occurred
   */
  protected Point<ContentNode> maybeSplitForNewline(ContentElement p, Point<ContentNode> splitAt) {
    throw new UnsupportedOperationException("No more paragraphs");
  }

  /**
   * Determine whether a particular key/value attribute combination should be copied
   * to the newly created paragraph when a newline is triggered.
   *
   * @param key Key of the attribute
   * @param value Value for the attribute
   * @return Whether the attribute should be kept
   */
  static boolean attributeKeptOnNewline(String key, String value) {
    // TODO(patcoleman): clean up to keep whitelisted combos in a set / map?

    // don't keep anything but bullet points
    if (Paragraph.SUBTYPE_ATTR.equals(key)) {
      if (Paragraph.LIST_TYPE.equals(value)) {
        return true;
      }
      return false;
    }

    return true;
  }

  /**
   * @return true if the two given nodes may join as paragraphs
   */
  protected boolean canJoin(ContentNode first, ContentNode second) {
    return LineRendering.isLocalParagraph(first) && LineRendering.isLocalParagraph(second);
  }

  /**
   * Compute maximum join level of two adjacent elements by asking
   * the relevant ContentNode.canJoin questions
   *
   * @param left
   * @param right
   * @return maximum legal join level of first and second
   *
   */
  private int maxJoinLevels(ContentNode left, ContentNode right) {
    int level = 0;
    while (left != null && right != null &&
        canJoin(left, right)) {
      level++;
      right = left.getMutableDoc().getFirstChild(right);
      left = left.getMutableDoc().getLastChild(left);
    }
    return level;
  }

  static void indent(ContentElement p, int delta) {
    int indent = Math.max(0, Paragraph.getIndent(p) + delta);
    p.getMutableDoc().setElementAttribute(p, Paragraph.INDENT_ATTR,
        indent == 0 ? null : indent + "");
  }

  /**
   * Joins this with previousSibling if appropriate after a backspace at beginning
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleBackspaceAtBeginning(ContentElement p, EditorEvent event) {
    throw new UnsupportedOperationException("No more paragraphs");
  }

  /**
   * Joins this with nextSibling if appropriate after a delete at end
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleDeleteAtEnd(ContentElement p, EditorEvent event) {
    throw new UnsupportedOperationException("No more paragraphs");
  }


  /**
   * @return Behaviour of this "paragraph"
   */
  static ParagraphBehaviour getBehaviour(ContentElement p) {
    return ParagraphBehaviour.of(p.getAttribute(Paragraph.SUBTYPE_ATTR));
  }

}
