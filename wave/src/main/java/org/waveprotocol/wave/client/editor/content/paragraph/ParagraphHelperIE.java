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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.model.document.util.Point;


/**
 * IE specifics for {@link ParagraphHelper}
 *
 * Unlike Webkit and Gecko, IE does not add regular DOM to
 * a paragraph to render it correctly in an editor, but rather
 * sets some otherwise inaccessible flag on the <p> element.
 * Consider this editable div:
 *
 *  <div contentEditable><p></p></div>
 *
 * You can test that the <p> has the magic flag: if set, then
 * the div's innerHTML is "<p>&nbsp;</p>". Otherwise it is "<p></p>".
 * In both cases, the <p> element itself has an outerHTML of "<p></p>"
 * and reports having no children. That is, we need to inspect
 * the innerHTML of the paragraphs editable parent in order to
 * check if the flag is set on the parent; the paragraph itself
 * doesn't tell us the difference.
 *
 * Note that adding a text node with a single &nbsp; char is *not*
 * the same as setting the magic flag. In particular adding a
 * real &nbsp; makes it possible to move the caret before +
 * after the char, which is not the case for an empty paragraph
 * with the magic flag set.
 *
 * Deep cloning a <p> preserves the magic flag, even if the node
 * being cloned is not attached to the document.
 *
 * The flag appears to work for all block elements, including
 * custom ones like <title style="display:block">. (However
 * the test above appears only to work for non-custom HTML block
 * elements.)
 *
 * IE's native editor:
 *
 * -- Sets the flag when creating new, empty paragraphs.
 * -- Sets the flag when the user deletes the last char in the paragraph.
 * -- Unsets the flag when the user types in and empty paragraph (or at
 *   least we have found no evidence the flag is still there).
 * -- Unsets the flag when our code appendChilds a node to an empty
 *   paragraph (or at least we have found no evidence the flag is still there).
 *
 * but IE:
 *
 * -- Does *not* set the flag when we removeChild the last node
 *   from a paragraph.
 * -- Does *not* set the flag when we createElement an empty paragraph
 *   and insert it into the editor.
 *
 * Thus, we need only add helper code to set the flag on empty
 * paragraphs our code creates.
 *
 */
public class ParagraphHelperIE extends ParagraphHelper {

  /**
   * We need these only for the magic flag test in
   * {@link #assertHealthy(ContentElement)}
   */
  private static Element testMarker = null;
  private static Element testDiv = null;

  @Override
  public Node getEndingNodelet(Element paragraph) {
    return null;
  }


  @Override
  public void onEmpty(Element nodelet) {
    openEmptyParagraph(nodelet);
  }

  /**
   * Conditionally check if we need to do the magic trick
   */
  @Override
  public void onRepair(ContentElement paragraph) {
    if (paragraph.getFirstChild() == null) {
      onEmpty(paragraph.getImplNodelet());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertHealthy(ContentElement paragraph) {
    ContentView renderedContent = paragraph.getRenderedContentView();
    if (renderedContent.getFirstChild(paragraph) == null) {
      Element nodelet = paragraph.getImplNodelet();
      assert 0 == nodelet.getChildCount() : "Empty Paragraph nodelet should have no chldren";
      // Magic flag test
      // TODO(user): can we find a test for this also for the <title> element?
      // NOTE(user): temporarily disabling this, as it messes with setting
      // carets in the paragraph
      if (false) {
        if (testDiv == null) {
          // Construct test elements
          testMarker = Document.get().createDivElement();
          testDiv = Document.get().createDivElement();
          testDiv.setAttribute("contentEditable", "true");
        }
        // Stick nodelet in test editable div, grab the div's inner html,
        // and place nodelet back where it came from. We need this because
        // only the innerHTML of an editable parent of the paragraph
        // differs based on the flag we are testing.
        nodelet.getParentNode().insertBefore(testMarker, nodelet);
        testDiv.appendChild(nodelet);
        String innerHTML = testDiv.getInnerHTML().toLowerCase();
        testMarker.getParentNode().insertBefore(nodelet, testMarker);
        testMarker.removeFromParent();
        // Now test the innerHTML we got above
        assert "<p>&nbsp;</p>".equals(innerHTML) :
            "Empty Paragraph nodelet should have magic flag set";
      }
    }
    super.assertHealthy(paragraph);
  }

  /**
   * IE-specific trick to keep an empty paragraph "open" (i.e. prevent it from
   * collapsing to zero height). See class javadoc for details.
   *
   * Since we know of no direct way to set the magic flag, we mimic the user
   * deleting the last char.
   *
   * TODO(user): can we get away with only doing this when creating new, empty
   * paragraphs? It seems our own emptying should already set the magic flag,
   * but for some reason it doesn't seem to happen...
   *
   * NB(user): The flag only gets set when the nodelet is attached to the
   * editor. See also ImplementorImpl.Helper#beforeImplementation(Element)
   */
  public static void openEmptyParagraph(Element nodelet) {
    // Add + delete an arbitrary char from the paragraph
    // TODO(user): why does this throw exception in <caption> elements?
    try {
      Point<Node> point = IeNodeletHelper.beforeImplementation(nodelet);
      nodelet.setInnerHTML("x");
      nodelet.setInnerHTML("");
      IeNodeletHelper.afterImplementation(nodelet, point);
    } catch (JavaScriptException t) {}
  }
}
