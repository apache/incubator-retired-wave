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

package org.waveprotocol.wave.client.editor.harness;

import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail;
import org.waveprotocol.wave.client.doodad.attachment.testing.FakeAttachmentsManager;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.form.FormDoodads;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;

import java.util.Map;

/**
 * Utility class that provides a realistic editor for testing.
 *
 * TODO(danilatos/mtsui): Reconcile with TestEditors (separate for open sourcing
 * reasons)
 *
 */
public class RealisticEditor {
  /**
   * Gets a realistic editor for testing.
   */
  public static Editor getRealisticEditor() {
    registerHandlers(Editor.ROOT_REGISTRIES);

    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    Editor editor = Editors.create();
    editor.init(Editor.ROOT_REGISTRIES, KeyBindingRegistry.NONE, EditorSettings.DEFAULT);
    return editor;
  }

  private static void registerHandlers(Registries registries) {
    AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();
    PainterRegistry paintRegistry = registries.getPaintRegistry();
    ElementHandlerRegistry elementHandlerRegistry = registries.getElementHandlerRegistry();

    ImageThumbnail.register(elementHandlerRegistry,
        new FakeAttachmentsManager(), null);

    StyleAnnotationHandler.register(registries);
    DiffAnnotationHandler.register(annotationRegistry, paintRegistry);
    DiffDeleteRenderer.register(elementHandlerRegistry);
    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });
    LineRendering.registerContainer(Blips.BODY_TAGNAME, elementHandlerRegistry);
    FormDoodads.register(Editor.ROOT_HANDLER_REGISTRY);

//    SpellDocument testSpellDoc = SpellDebugHelper.createTestSpellDocument(
//        EditorStaticDeps.logger);
//    SpellAnnotationHandler.register(registries,
//        SpellySettings.DEFAULT, testSpellDoc);
//    SpellSuggestion.register(registries.getElementHandlerRegistry(), testSpellDoc);
  }
}
