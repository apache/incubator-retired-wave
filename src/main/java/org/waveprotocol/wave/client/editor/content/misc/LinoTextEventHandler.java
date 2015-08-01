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

package org.waveprotocol.wave.client.editor.content.misc;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphEventHandler;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * LinoText (as in Line-of-text) behaviour is similar to paragraph behaviour,
 * but without splitting or joining. It ignores return/enter as well as
 * delete/backspaces at end/beginning. It does not join with other paragraphs.
 *
 * TODO(user): expand canJoin logic so that, e.g., a LinoText followed by a
 * normal paragraph does not join when user hits backspace at beginning of the
 * paragraph.
 *
 * TODO(user): find right way to prevent LinoText from containing non-text.
 * (Font modifiers such as <b> are ok...)
 *
 */
public abstract class LinoTextEventHandler extends ParagraphEventHandler {

  /**
   * Don't do a split, and just return the point we were given, as we ignore
   * newline split attempts
   *
   * {@inheritDoc}
   */
  @Override
  protected Point<ContentNode> maybeSplitForNewline(ContentElement p, ContentPoint splitAt) {
    return splitAt.asPoint();
  }

  /**
   * Cancels backspace at beginning of line.
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleBackspaceAtBeginning(ContentElement p, EditorEvent event) {
    return true;
  }

  /**
   * Cancels delete at end of line.
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleDeleteAtEnd(ContentElement p, EditorEvent event) {
    return true;
  }

}
