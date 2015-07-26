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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphHelper;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.Point;

import java.util.Collections;

/**
 * Controller for a little DOM object to contain the cursor during IME
 * composition, to protect it from concurrent mutations to the surrounding DOM.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ImeExtractor {

  private final SpanElement imeContainer = Document.get().createSpanElement();

  private final SpanElement imeInput;

  private Point.El<Node> inContainer;

  private ContentElement wrapper = null;

  private static final String WRAPPER_TAGNAME = "l:ime";

  public static void register(ElementHandlerRegistry registry) {
    registry.registerRenderer(WRAPPER_TAGNAME, new Renderer() {
      @Override
      public Element createDomImpl(Renderable element) {
        return element.setAutoAppendContainer(Document.get().createSpanElement());
      }
    });
  }

  /***/
  public ImeExtractor() {
    NodeManager.setTransparency(imeContainer, Skip.DEEP);
    NodeManager.setMayContainSelectionEvenWhenDeep(imeContainer, true);
    if (QuirksConstants.SUPPORTS_CARET_IN_EMPTY_SPAN) {
      // For browsers that support putting the caret in an empty span,
      // we do just that (it's simpler).
      imeInput = imeContainer;
    } else {
      // For other browsers, we use inline block so we can reuse the
      // paragraph logic to keep the ime extractor span open (i.e. to
      // allow the cursor to live inside it when it contains no text).
      // see #clearContainer()
      imeContainer.getStyle().setDisplay(Display.INLINE_BLOCK);
      DomHelper.setContentEditable(imeContainer, false, false);

      imeInput = Document.get().createSpanElement();
      imeInput.getStyle().setDisplay(Display.INLINE_BLOCK);
      imeInput.getStyle().setProperty("outline", "0");
      DomHelper.setContentEditable(imeInput, true, false);
      NodeManager.setTransparency(imeInput, Skip.DEEP);
      NodeManager.setMayContainSelectionEvenWhenDeep(imeInput, true);

      imeContainer.appendChild(imeInput);
    }
    clearContainer();
  }

  /**
   * @return the current composition text if isActive(), null otherwise.
   */
  public String getContent() {
    return isActive() ? imeContainer.getInnerText() : null;
  }

  /**
   * Activates the IME extractor at the given location.
   *
   * The extraction node will be put in place, and selection moved to it.
   *
   * @param cxt
   * @param location
   */
  public void activate(
      DocumentContext<ContentNode, ContentElement, ContentTextNode> cxt,
      Point<ContentNode> location) {

    clearWrapper(cxt.annotatableContent());

    Point<ContentNode> point = DocHelper.ensureNodeBoundary(
        DocHelper.transparentSlice(location, cxt),
        cxt.annotatableContent(), cxt.textNodeOrganiser());

    // NOTE(danilatos): Needed as a workaround to bug 2152316
    ContentElement container = point.getContainer().asElement();
    ContentNode nodeAfter = point.getNodeAfter();
    if (nodeAfter != null) {
      container = nodeAfter.getParentElement();
    }
    ////

    wrapper = cxt.annotatableContent().transparentCreate(
        WRAPPER_TAGNAME, Collections.<String, String>emptyMap(),
        container, nodeAfter);

    wrapper.getContainerNodelet().appendChild(imeContainer);
    NativeSelectionUtil.setCaret(inContainer);
  }

  /**
   * Removes the IME extractor node.
   * @param doc
   * @return the location where the node resided
   */
  public Point<ContentNode> deactivate(
      LocalDocument<ContentNode, ContentElement, ContentTextNode> doc) {
    Point.El<ContentNode> ret = Point.<ContentNode>inElement(
        doc.getParentElement(wrapper), doc.getNextSibling(wrapper));
    clearWrapper(doc);
    return ret;
  }

  /**
   * @return whether the extractor is actively containing composition events
   */
  public boolean isActive() {
    return wrapper != null;
  }

  private void clearWrapper(LocalDocument<ContentNode, ContentElement, ContentTextNode> doc) {
    if (wrapper != null && wrapper.getParentElement() != null) {
      doc.transparentDeepRemove(wrapper);
    }
    wrapper = null;
    clearContainer();
  }

  private void clearContainer() {
    imeInput.setInnerHTML("");
    if (!QuirksConstants.SUPPORTS_CARET_IN_EMPTY_SPAN) {
      ParagraphHelper.INSTANCE.onEmpty(imeInput);
    }
    inContainer = Point.<Node>inElement(imeInput, imeInput.getFirstChild());
  }

}
