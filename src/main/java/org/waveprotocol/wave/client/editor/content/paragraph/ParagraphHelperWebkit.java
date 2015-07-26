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

import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;

/**
 * Quick hack for improving Webkit: don't have a spacer if the paragraph ends in
 * text.
 *
 * Background: We need to aggressively put a BR in, more than Webkit's default
 * of just empty paragraphs, to keep the paragraph open if it just contains an
 * inline element, or if it ends in an inline block element to allow the cursor
 * to go after it. The problem is, if we have a BR at the end of the paragraph
 * above us, and we hit the up arrow key, the cursor always jumps to the end of
 * the line, which is very annoying. This doesn't happen with the down arrow.
 *
 * So a quick fix, until we solve this with a proper selection calculation
 * infrastructure, is to do something that works 95% of the time, when the
 * paragraph ends in text.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ParagraphHelperWebkit extends ParagraphHelperBr {

  @Override
  public void onChildAdded(Node child, Element paragraph) {
    ensureSpacerIfDoesntEndWithText(paragraph);
  }

  @Override
  public void onRemovingChild(Node child, Element paragraph) {
    ensureSpacerIfDoesntEndWithText(paragraph);
  }

  @Override
  public void onRepair(ContentElement paragraph) {
    ensureSpacerIfDoesntEndWithText(paragraph.getImplNodelet());
  }

  private void ensureSpacerIfDoesntEndWithText(Element paragraph) {
    Node last = paragraph.getLastChild();
    BRElement spacer = getSpacer(paragraph);
    if (last == spacer) {
      last = last.getPreviousSibling();
    }
    if (last == null || DomHelper.isElement(last)) {
      paragraph.appendChild(spacer);
    } else {
      spacer.removeFromParent();
    }
  }
}
