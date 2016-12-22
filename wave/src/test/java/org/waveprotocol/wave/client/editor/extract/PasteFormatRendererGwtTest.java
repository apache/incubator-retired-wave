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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.editor.testing.TestInlineDoodad;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Tests for PasteFormatRenderer
 *
 */

public class PasteFormatRendererGwtTest extends GWTTestCase {

  /** GWT version where invalid closing-br tags appear in innerHTML strings. */
  private final static String INVALID_BR_GWT_VERSION = "2.5.1";

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.extract.Tests";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    LineContainers.setTopLevelContainerTagname("body");
    LineRendering.registerContainer("body", Editor.ROOT_HANDLER_REGISTRY);
    TestInlineDoodad.register(Editor.ROOT_HANDLER_REGISTRY, "input");


    // TODO(user): Doing this is tedious, make this the default.
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
  }

  public void testPlainLineRendering() {
    testHelper("<body><line/>foobar</body>", "foobar<br>");
    testHelper("<body><line/>hello<line/>world</body>", "hello<br>world<br>");
  }

  public void testHeadingRendering() {
    testHelper("<body><line t=\"h1\" />heading<line/>normal</body>",
        "<h1>heading</h1>normal<br>");
  }

  public void testListRendering() {
    testHelper("<body><line t=\"li\"/>in list<line/>outside</body>",
        "<ul><li>in list</li></ul>outside<br>");
    testHelper("<body><line t=\"li\"/>one<line t=\"li\"/>two<line t=\"li\"/>three</body>",
        "<ul><li>one</li><li>two</li><li>three</li></ul>");
    testHelper("<body><line/>before<line t=\"li\"/>one<line t=\"li\"/>two<line/>after</body>",
        "before<br><ul><li>one</li><li>two</li></ul>after<br>");
    testHelper("<body><line t=\"li\"/><input>hello</input> world<line/>after</body>",
    "<ul><li><span contentEditable=\"true\" style=\"white-space: pre-wrap;\" class=\"SWCNL\">hello</span> world</li></ul>after<br>");
  }

  private void testHelper(String content, String expectedResult) {
    EditorImpl editor = (EditorImpl) Editors.create();
    RootPanel.get().add(editor);
    editor.init(Editor.ROOT_REGISTRIES, null, EditorSettings.DEFAULT);
    initWithContent(editor, content);
    String result = renderPasteHtml(editor);
    assertEquals(expectedResult, result);
    RootPanel.get().remove(editor);
  }

  private String renderPasteHtml(Editor editor) {
    ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view =
        editor.getContent().getRenderedView();

    System.out.println("doc: " + XmlStringBuilder.innerXml(view).toString());
    ContentElement topLevelElement = view.getDocumentElement().getFirstChild().asElement();
    assertEquals(LineContainers.topLevelContainerTagname(), topLevelElement.getTagName());
    Point<ContentNode> start = Point.inElement(topLevelElement, topLevelElement.getFirstChild());
    Point<ContentNode> end = Point.<ContentNode> end(topLevelElement);

    Element fragment =
        PasteFormatRenderer.get().renderTree(view, topLevelElement,
            new SelectionMatcher(start, end));
    String html = Element.as(fragment.getFirstChild()).getInnerHTML();

    if (GWT.getVersion().equals(INVALID_BR_GWT_VERSION)) {
      // </br> is invalid HTML.
      html = html.replaceAll("</br>", "");
    }

    return html;
  }

  private void initWithContent(Editor editor, String content) {
    editor.init(Editor.ROOT_REGISTRIES, null, EditorSettings.DEFAULT);
    ContentSerialisationUtil.setContentString(editor, content);
  }
}
