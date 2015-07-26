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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;


/**
 * Shared {@link ParagraphHelper} code for browsers using <br/> elements
 * as spacers. Note that the exact behaviour of the spacers once added
 * is determined by further deferred binding of this class for Safari vs.
 * Firefox.
 *
 */
public class ParagraphHelperBr extends ParagraphHelper {

  private static final String BR_REF = NodeManager.getNextMarkerName("br");

  /**
   * @param n Node to test, or null
   * @return true if n is a spacer <br/>, WHETHER OR NOT we created it
   *   or the browser's native editor created it
   */
  protected boolean isSpacer(Node n) {
    if (n == null) {
      return false;
    }
    if (DomHelper.isTextNode(n)) {
      return false;
    }
    Element e = n.cast();
    return e.getTagName().toLowerCase().equals("br") && !NodeManager.hasBackReference(e);
  }

  @Override
  public Node getEndingNodelet(Element paragraph) {
    Node maybeSpacer = paragraph.getLastChild();
    return isSpacer(maybeSpacer) ? maybeSpacer : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEmpty(Element paragraph) {
    appendSpacer(paragraph);
  }

  /**
   * Appends a spacer <br sp='t'/> element to paragraph
   * Each paragraph owns a spacer which gets reused, so it
   * is safe to call this method multiple times.
   *
   * @param paragraph
   */
  protected void appendSpacer(Element paragraph) {
    Element spacer = getSpacer(paragraph);
    paragraph.appendChild(spacer);
  }

  /**
   * Get the spacer for the given paragraph.
   * Lazily creates & registers one if not present.
   * If there's one that the browser created, registers it as our own.
   * If the browser put a different one in to the one that we were already
   * using, replace ours with the browser's.
   * @param paragraph
   * @return The spacer
   */
  protected BRElement getSpacer(Element paragraph) {
    Node last = paragraph.getLastChild();
    BRElement spacer = paragraph.getPropertyJSO(BR_REF).cast();
    if (spacer == null) {
      // Register our spacer, using one the browser put in if present
      spacer = isSpacer(last) ? last.<BRElement>cast() : Document.get().createBRElement();
      setupSpacer(paragraph, spacer);
    } else if (isSpacer(last) && last != spacer) {
      // The browser put a different one in by itself, so let's use that one
      if (spacer.hasParentElement()) {
        spacer.removeFromParent();
      }
      spacer = last.<BRElement>cast();
      setupSpacer(paragraph, spacer);
    }
    return spacer;
  }

  /**
   * Setup the given br element as the paragraph's spacer
   * @param paragraph
   * @param spacer
   */
  private void setupSpacer(Element paragraph, BRElement spacer) {
    NodeManager.setTransparency(spacer, Skip.DEEP);
    paragraph.setPropertyJSO(BR_REF, spacer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertHealthy(ContentElement paragraph) {
//TODO(danilatos): Bring this check back. Something doesn't look right about it atm,
//need to think about it.
//    if (paragraph.getImplNodelet().getChildCount() == 0) {
//      Element nodelet = paragraph.getImplNodelet();
//      Assert.assertTrue(
//          "Empty Paragraph must have <br/> spacer",
//          isSpacer(nodelet.getFirstChild()));
//      Assert.assertTrue(
//          "Empty Paragraph must have only <br/> spacer",
//          nodelet.getChildCount() == 1);
//    }
    super.assertHealthy(paragraph);
  }

}
