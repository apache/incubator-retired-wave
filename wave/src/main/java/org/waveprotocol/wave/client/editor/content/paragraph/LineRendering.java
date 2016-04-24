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

import static org.waveprotocol.wave.model.document.util.LineContainers.PARAGRAPH_FULL_TAGNAME;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.UListElement;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.extract.PasteFormatRenderers;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;


/**
 * Line container and friends
 *
 * Rendering of line tokens within a line container to paragraphs in the HTML.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// Example schema:
// ....
//   <lc>
//     <l t="h3"/>Some heading
//     <l />Some content
//     <l />Some more content
//   </lc>
//
public class LineRendering {

  /**
   * Default renderer that responds to mutation events
   */
  public static final LineContainerParagraphiser DEFAULT_PARAGRAPHISER
      = new LineContainerParagraphiser();

  public static final Renderer DEFAULT_RENDERER = new Renderer() {
    @Override
    public Element createDomImpl(Renderable element) {
      UListElement e = Document.get().createULElement();

      // Be careful if you want to move these into CSS - they might affect rendering
      // of email notifications in gmail. Find a nicer way to deal with this.
      e.getStyle().setPadding(0, Unit.PX);
      e.getStyle().setMargin(0, Unit.PX);
      return element.setAutoAppendContainer(e);
    }
  };


  /**
   * Default handler for user events
   */
  public static final NodeEventHandler DEFAULT_PARAGRAPH_EVENT_HANDLER =
    new LocalParagraphEventHandler();

  /**
   * Registers paragraph handlers for any provided tag names / type attributes.
   */
  public static void registerContainer(String containerTagName,
      final ElementHandlerRegistry registry) {

    LineContainers.registerLineContainerTagname(containerTagName);
    registry.registerMutationHandler(containerTagName, DEFAULT_PARAGRAPHISER);
    registry.registerRenderer(containerTagName, DEFAULT_RENDERER);
    registry.registerNiceHtmlRenderer(containerTagName, PasteFormatRenderers.SHALLOW_CLONE_RENDERER);
  }

  /**
   * Registers paragraph handlers for any provided tag names / type attributes.
   */
  public static void registerLines(
      final ElementHandlerRegistry registry) {
    registerParagraphRenderer(registry, Paragraph.DEFAULT_RENDERER);
    registry.registerEventHandler(PARAGRAPH_FULL_TAGNAME, DEFAULT_PARAGRAPH_EVENT_HANDLER);
    registry.registerNiceHtmlRenderer(PARAGRAPH_FULL_TAGNAME, Paragraph.DEFAULT_NICE_HTML_RENDERER);
    registry.registerRenderingMutationHandler(LineContainers.LINE_TAGNAME,
        DEFAULT_PARAGRAPHISER.getLineHandler());
  }

  public static void registerParagraphRenderer(ElementHandlerRegistry registry,
      RenderingMutationHandler renderer) {
    registry.registerRenderingMutationHandler(PARAGRAPH_FULL_TAGNAME, renderer);
  }

  public static boolean isLineContainerElement(ContentNode node) {
    return LineContainers.isLineContainer(FullContentView.INSTANCE, node);
  }

  public static boolean isLineElement(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, LineContainers.LINE_TAGNAME);
  }

  public static boolean isLineElement(ContentElement element) {
    return LineContainers.LINE_TAGNAME.equals(element.getTagName());
  }

  public static boolean isLocalParagraph(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, PARAGRAPH_FULL_TAGNAME);
  }
}
