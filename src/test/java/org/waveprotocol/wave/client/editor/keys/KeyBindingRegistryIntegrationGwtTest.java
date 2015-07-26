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

package org.waveprotocol.wave.client.editor.keys;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Panel;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorAction;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupProvider;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;

/**
 * Integration tests to make sure the key bindings passed into an editor are called properly
 * when key events are passed in from outside the editor.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class KeyBindingRegistryIntegrationGwtTest extends GWTTestCase {
  private static final int G_CODE = 'g';
  private int callTracker;

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.keys.Tests";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    EditorTestingUtil.setupTestEnvironment();
  }

  /**
   * Ensure that an action bound to ORDER_G is executed when the appropriate JS keypress
   * event is fired at the editor.
   */
  public void testKeyBinding() {
    KeyBindingRegistry bindings = new KeyBindingRegistry();
    callTracker = 0;
    EditorAction testAction = new EditorAction() {
      public void execute(EditorContext context) {
        callTracker++;
      }
    };
    bindings.registerAction(KeyCombo.ORDER_G, testAction);
    EditorImpl editor = createEditor(bindings);

    // 103 = g, this event = CTRL_G which is bound to ORDER_G by the EventWrapper
    Event rawEvent = Document.get().createKeyPressEvent(
        true, false, false, false, G_CODE, G_CODE).cast();
    editor.onJavaScriptEvent("keypress", rawEvent);
    assertEquals("Callback action not called on registered keypress", callTracker, 1);
  }

  /** Ensure that when other keys are pressed, they are not passed to the action. */
  public void testAlternativeKeyPress() {
    KeyBindingRegistry bindings = new KeyBindingRegistry();
    callTracker = 0;
    EditorAction testAction = new EditorAction() {
      public void execute(EditorContext context) {
        callTracker++;
      }
    };
    bindings.registerAction(KeyCombo.ORDER_G, testAction);
    EditorImpl editor = createEditor(bindings);

    // This event is not ORDER_G, it has other accelerators thrown in.
    Event rawEvent = Document.get().createKeyPressEvent(
        true, true, true, false, G_CODE, G_CODE).cast();
    editor.onJavaScriptEvent("keypress", rawEvent);
    assertEquals("Callback action called on unregistered keypress", callTracker, 0);
  }

  /**
   * Ensure that new keybindings are used after changing them in the editor.
   */
  public void testReregistrationKeyBinding() {
    KeyBindingRegistry bindings = new KeyBindingRegistry();
    callTracker = 0;
    EditorAction testAction = new EditorAction() {
      public void execute(EditorContext context) {
        callTracker++;
      }
    };
    bindings.registerAction(KeyCombo.ORDER_G, testAction);

    EditorImpl editor = createEditor(bindings);
    Event rawEvent = Document.get().createKeyPressEvent(
        true, false, false, false, G_CODE, G_CODE).cast();
    editor.onJavaScriptEvent("keypress", rawEvent);
    // callTracker should be 1 assuming the test above passes

    bindings.removeAction(KeyCombo.ORDER_G);
    initEditor(editor, Editor.ROOT_REGISTRIES, bindings);
    editor.onJavaScriptEvent("keypress", rawEvent);
    assertEquals("Callback action called on deregistered keypress", callTracker, 1);
  }

  /** Util to help construct an editor instance. */
  private EditorImpl createEditor(KeyBindingRegistry keyBinding) {
    EditorStaticDeps.setPopupProvider(new PopupProvider() {
      @Override
      public UniversalPopup createPopup(Element reference, RelativePopupPositioner positioner,
          PopupChrome chrome, boolean autoHide) {
        return new Popup(reference, positioner);
      }
      @Override
      public void setRootPanel(Panel rootPanel) {
        // Not used as we use our own popup implementation.
      }
    });
    Editor editor = Editors.create();
    initEditor(editor, Editor.ROOT_REGISTRIES, keyBinding);
    return (EditorImpl) editor;
  }

  /** Util to help set the editor up with content and selection. */
  private void initEditor(Editor editor,
      Registries registries, KeyBindingRegistry keys) {
    editor.init(registries, keys, EditorSettings.DEFAULT);
    editor.setEditing(true);
    ContentSerialisationUtil.setContentString(editor, "<body><line></line>X</body>");
    editor.getSelectionHelper().setCaret(editor.getDocument().locate(3));
  }
}
