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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentView;


/**
 * Used for Gecko and Webkit
 *
 * Firefox, like Safari, uses a regular <br/> element as spacer.
 *
 * Firefox's native editor always preserves a br at the end of
 * each line as a spacer.
 *
 * Safari's native editor removes the spacer br by default for
 * non-empty lines, however this causes glitches. Given that it
 * works just to manually force it to always keep the br, and
 * that this fixes the glitches (to do with non-empty content
 * that still collapses the paragraph, such as a nested empty
 * span), we use this behaviour for Safari as well.
 *
 * Firefox's native editor:
 *
 * -- Adds the <br/> when creating new, empty paragraphs.
 * -- Adds the <br/> when the user deletes the last char in the paragraph.
 * -- Adds the <br/> when the user types a trailing space
 * -- Removes the <br/> when the user hits return at the end of a
 *    non-empty paragraph
 *
 * but Firefox:
 *
 * -- Does *not* add the <br/> when we removeChild the last node from
 *   a paragraph.
 * -- Does *not* add the <br/> when we createElement an empty paragraph
 *   and insert it into the editor.
 * -- Does *not* add the <br> is we insert text with a trailing space into
 *   the end of the paragraph
 *
 * Our code inserts and moves spacer <br/> elements to always have the
 * <br/> element at the end of the paragraph. (Note that we handle return
 * ourselves, so Firefox does not get a chance to remove the <br/> on return.)
 *
 */
public class ParagraphHelperAlwaysBr extends ParagraphHelperBr {

  /**
   * {@inheritDoc}
   */
  @Override
  public void onChildAdded(Node child, Element paragraph) {
    appendSpacer(paragraph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemovingChild(Node child, Element paragraph) {
    appendSpacer(paragraph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRepair(ContentElement paragraph) {
    appendSpacer(paragraph.getImplNodelet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertHealthy(ContentElement paragraph) {
    ContentView renderedContent = paragraph.getRenderedContentView();
    if (renderedContent.getFirstChild(paragraph) != null) {
      Element nodelet = paragraph.getImplNodelet();
      Node last = nodelet.getLastChild();
      assert isSpacer(last) : "Last nodelet child should be spacer";
      Node child = nodelet.getFirstChild();
      while (child != null && !child.equals(last)) {
        assert !isSpacer(child) : "Only last nodelet child should be spacer";
        child = child.getNextSibling();
      }
    }
    super.assertHealthy(paragraph);
  }
}
