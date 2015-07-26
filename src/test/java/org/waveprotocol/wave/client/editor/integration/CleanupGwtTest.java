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

package org.waveprotocol.wave.client.editor.integration;

import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.PainterRegistryImpl;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.RegistriesImpl;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.document.util.DocHelper;

/**
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class CleanupGwtTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.integration.Tests";
  }

  public void testAnnotationPainterDies() {
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });
    EditorTestingUtil.setupTestEnvironment();

    FakeTimerService timerService = new FakeTimerService();
    PainterRegistryImpl paintRegistry = new PainterRegistryImpl(
            AnnotationPaint.SPREAD_FULL_TAGNAME, AnnotationPaint.BOUNDARY_FULL_TAGNAME,
            new AnnotationPainter(timerService));

    Registries registries =
        new RegistriesImpl(Editor.ROOT_HANDLER_REGISTRY,
            Editor.ROOT_ANNOTATION_REGISTRY, paintRegistry);

    StyleAnnotationHandler.register(registries);

    Editor editor = Editors.create();
    editor.init(registries, KeyBindingRegistry.NONE, EditorSettings.DEFAULT);

    // Now the actual test

    ContentSerialisationUtil.setContentString(editor, "<body><line/>abc</body>");
    CMutableDocument doc = editor.getDocument();
    doc.setAnnotation(3, 4, "style/color", "red");

    editor.getContent().debugCheckHealthy2();
    timerService.tick(1000);
    ContentView fullDoc = ((EditorImpl) editor).getContent().getFullContentView();
    assertNotNull(
        DocHelper.getElementWithTagName(fullDoc, AnnotationPaint.SPREAD_FULL_TAGNAME));
    doc.setAnnotation(3, 4, "style/color", null);
    timerService.tick(1000);
    assertNull(DocHelper.getElementWithTagName(fullDoc, AnnotationPaint.SPREAD_FULL_TAGNAME));

    doc.setAnnotation(3, 5, "style/color", "red");
    editor.removeContentAndUnrender();
    editor.reset();
    timerService.tick(1000);
    assertNull(DocHelper.getElementWithTagName(fullDoc, AnnotationPaint.SPREAD_FULL_TAGNAME));

  }
}
