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

package org.waveprotocol.wave.client.editor.content.img;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.LineContainers;

/**
 * Tests that the image doodad renderer is hooked up to the editor and draws images correctly.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class ImgDoodadGwtTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.content.Tests";
  }

  /***/
  public void testImageIsRendered() {
    LineContainers.setTopLevelContainerTagname(Blips.BODY_TAGNAME);
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    LineRendering.registerContainer(Blips.BODY_TAGNAME,
        Editor.ROOT_HANDLER_REGISTRY);
    Editors.initRootRegistries();
    Editor editor = Editors.create();
    editor.init(Editor.ROOT_REGISTRIES, KeyBindingRegistry.NONE, EditorSettings.DEFAULT);

    // seed editor and find image in content:
    ContentSerialisationUtil.setContentString(editor,
        "<body><line/><img src=\"imageSource\"></img></body>");
    ContentElement imgTag = editor.getDocument().getDocumentElement()
        .getFirstChild().getLastChild().getFirstChild().asElement();

    // check image in html:
    Element elt = imgTag.getImplNodelet();
    assertEquals("IMG", elt.getTagName().toUpperCase());
    assertEquals("imageSource", elt.getAttribute("src"));
  }
}
