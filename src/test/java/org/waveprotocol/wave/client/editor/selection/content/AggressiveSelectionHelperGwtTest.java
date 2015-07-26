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

package org.waveprotocol.wave.client.editor.selection.content;

import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Pretty;

/**
 * Tests for AggressiveSelectionHelper.
 *
 * TODO(user): Test with flushing behaviour as well.
 *
 */

public class AggressiveSelectionHelperGwtTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.selection.Tests";
  }

  private EditorImpl editor;
  private CMutableDocument doc;
  private AggressiveSelectionHelper aggressiveSelectionHelper;

  @Override
  protected void gwtTearDown() throws Exception {
    // Clear everything between each test.
    editor = null;
    doc = null;
    aggressiveSelectionHelper = null;
    super.gwtTearDown();
  }

  private void setupTest(String content) {
    Blips.init();
    LineRendering.registerContainer(Blips.BODY_TAGNAME,
        Editor.ROOT_HANDLER_REGISTRY);
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    editor = (EditorImpl)Editors.create();

    editor.init(Editor.ROOT_REGISTRIES, null, EditorSettings.DEFAULT);
    System.err.println(content);
    ContentSerialisationUtil.setContentString(editor, content);
    doc = editor.mutable();
    aggressiveSelectionHelper = editor.getAggressiveSelectionHelper();
  }


  public void testGetValidSelectionPointIfAvailable() {
    setupTest("<body><line></line></body>");
    Point<ContentNode> point = aggressiveSelectionHelper.getFirstValidSelectionPoint();
    ContentElement localP = doc.getDocumentElement().getFirstChild().getLastChild().asElement();
    assertEquals(Point.end(localP), point);

    setupTest("<body><line></line></body>");
    point = aggressiveSelectionHelper.getLastValidSelectionPoint();
    localP = doc.getDocumentElement().getFirstChild().getLastChild().asElement();
    assertEquals(Point.end(localP), point);
  }

  public void testCreateSelectionPointIfNotFound() {
    setupTest("");
    aggressiveSelectionHelper.getFirstValidSelectionPoint();
    String content = new Pretty<ContentNode>().print(doc).replaceAll(">\\s+<", "><").trim();
    assertEquals("<doc><body><line/></body></doc>", content);

    setupTest("");
    aggressiveSelectionHelper.getLastValidSelectionPoint();
    content = new Pretty<ContentNode>().print(doc).replaceAll(">\\s+<", "><").trim();
    assertEquals("<doc><body><line/></body></doc>", content);
  }

  // NOTE(danilatos): Behaviour has changed such that this test is no longer valid.
  // In any case, no big deal, because schema validation would prevent random elements
  // being siblings of paragraphs anyway.
  public void xtestCreateSelectionPointIfNotFoundWithExtraNodes() {
    /**
    NOTE(patcoleman): actually disabled due to schema checking not allowing this any more.
    seems ok to disable due to comments above.
    setupTest("<a/>");
    Point<ContentNode> point = aggressiveSelectionHelper.getFirstValidSelectionPoint();
    assertEquals("<p></p><a></a>", doc.getDocumentElement().getImplNodelet().getInnerHTML());
    assertEquals(doc.getDocumentElement().getFirstChild(), point.getContainer());
    // Test at either side of the p
    ContentNode p = point.getContainer();
    assertEquals(point, aggressiveSelectionHelper.findOrCreateValidSelectionPoint(Point.inElement(p
        .getParentElement(), p)));
    assertEquals("<p></p><a></a>", doc.getDocumentElement().getImplNodelet().getInnerHTML());
    assertEquals(point, aggressiveSelectionHelper.findOrCreateValidSelectionPoint(Point.inElement(p
        .getParentElement(), p.getNextSibling())));
    assertEquals("<p></p><a></a>", doc.getDocumentElement().getImplNodelet().getInnerHTML());

    setupTest("<a/>");
    point = aggressiveSelectionHelper.getLastValidSelectionPoint();
    assertEquals("<a></a><p></p>", doc.getDocumentElement().getImplNodelet().getInnerHTML());
    assertEquals(doc.getDocumentElement().getLastChild(), point.getContainer());
    // Test at either side of the p
    p = point.getContainer();
    assertEquals(point, aggressiveSelectionHelper.findOrCreateValidSelectionPoint(Point.inElement(p
        .getParentElement(), p)));
    assertEquals("<a></a><p></p>", doc.getDocumentElement().getImplNodelet().getInnerHTML());
    assertEquals(point, aggressiveSelectionHelper.findOrCreateValidSelectionPoint(Point
        .<ContentNode> end(p.getParentElement())));
    assertEquals("<a></a><p></p>", doc.getDocumentElement().getImplNodelet().getInnerHTML());
    */
  }
}
