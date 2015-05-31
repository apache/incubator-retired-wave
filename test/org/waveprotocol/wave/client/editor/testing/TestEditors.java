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

package org.waveprotocol.wave.client.editor.testing;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.LineContainers;

/**
 * Utility to set up basic editors for integration testing.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class TestEditors {
  /**
   * Gets a realistic editor for testing.
   */
  public static Editor getMinimalEditor() {
    registerHandlers(Editor.ROOT_REGISTRIES);

    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    Editor editor = Editors.create();
    editor.init(Editor.ROOT_REGISTRIES, KeyBindingRegistry.NONE, EditorSettings.DEFAULT);
    editor.setEditing(true);
    return editor;
  }

  private static void registerHandlers(Registries registries) {
    AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();
    PainterRegistry paintRegistry = registries.getPaintRegistry();
    ElementHandlerRegistry elementHandlerRegistry = registries.getElementHandlerRegistry();

    LineContainers.setTopLevelContainerTagname(Blips.BODY_TAGNAME);
    LineRendering.registerContainer(Blips.BODY_TAGNAME, elementHandlerRegistry);
    TestInlineDoodad.register(elementHandlerRegistry);
    StyleAnnotationHandler.register(registries);
  }

  /** For testing purposes only. */
  public static ContentDocument createTestDocument() {
    ContentDocument doc = new ContentDocument(DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    Registries registries = Editor.ROOT_REGISTRIES.createExtension();
    for (String t : new String[] {"q", "a", "b", "c", "x"}) {
      final String tag = t;
      registries.getElementHandlerRegistry().registerRenderer(tag,
          new Renderer() {
            @Override
            public Element createDomImpl(Renderable element) {
              return element.setAutoAppendContainer(Document.get().createElement(tag));
            }
          });
    }
    doc.setRegistries(registries);
    Editor editor = getMinimalEditor();
    editor.setContent(doc);
    return doc;
  }
}
