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
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentView;


/**
 * NOTE(danilatos): This is probably no longer needed - delete
 * after 2009/11/01 if no issues have come up
 *
 * Safari (or rather webkit) specifics for {@link ParagraphHelper}
 *
 * Safari, like Firefox, uses a regular <br/> element as spacer. Unlike
 * Firefox, the <br/> spacer need and should only be present when the
 * paragraph is empty.
 *
 * Safari's native editor:
 *
 * -- Adds the <br/> when creating new, empty paragraphs.
 * -- Adds the <br/> when the user deletes the last char in the paragraph.
 * -- Removes the <br/> when the user types in and empty paragraph.
 *
 * but Safari:
 *
 * -- Does *not* remove the <br/> when our code appendChilds a node
 *   to an empty paragraph
 * -- Does *not* add the <br/> when we removeChild the last node from
 *   a paragraph.
 * -- Does *not* add the <br/> when we createElement an empty paragraph
 *   and insert it into the editor.
 *
 * In addition, Safari appears to have a bug when the paragraph contains
 * inline-block elements, such as image thumbnails and chars. For example,
 * when the user  deletes the last char after an image thumbnail in an
 * otherwise empty paragraph, Safari add a spacer <br/> after the thumbnail,
 * which immediately places the caret on the next line.
 *
 * Our code inserts and removes spacer <br/> elements to ensure
 * it is only there when the paragraph is empty.
 *
 */
public class ParagraphHelperEmptyLineBr extends ParagraphHelperBr {

  /**
   * {@inheritDoc}
   */
  @Override
  public void onChildAdded(Node child, Element paragraph) {
    Element spacer = getSpacer(paragraph);
    if (spacer.hasParentElement()) {
      spacer.removeFromParent();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemovingChild(Node child, Element paragraph) {
    Node first = paragraph.getFirstChild();
    BRElement spacer = getSpacer(paragraph);
    if (first == null) {
      appendSpacer(paragraph);
    } else if (first != spacer) {
      spacer.removeFromParent();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRepair(ContentElement paragraph) {
    if (paragraph.getFirstChild() == null) {
      appendSpacer(paragraph.getImplNodelet());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertHealthy(ContentElement paragraph) {
    ContentView renderedContent = paragraph.getRenderedContentView();
    if (renderedContent.getFirstChild(paragraph) != null) {
      Node child =
        paragraph.getImplNodelet().getFirstChild();
      while (child != null) {
        assert !isSpacer(child) : "Non-empty paragraph should not have spacer";
        child = child.getNextSibling();
      }
    }
    super.assertHealthy(paragraph);
  }

}
