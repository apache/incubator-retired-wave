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

package org.waveprotocol.wave.client.editor.debug;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorUpdateEventImpl;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.event.EditorEventHandler;
import org.waveprotocol.wave.model.document.util.DocProviders;

import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

/**
 * Controls a selection of debug options that manipulate editor behaviour,
 * and can be changed from a debug panel.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class DebugOptions {
  private final HorizontalPanel panel = new HorizontalPanel();
  private final VerticalPanel optionsPanel = new VerticalPanel();
  private final VerticalPanel updateEventsPanel = new VerticalPanel();
  private final EditorImpl editor;

  /**
   * Constructs the options and builds a widget to display them
   * @param editorImpl The editor the options are for
   */
  DebugOptions(final EditorImpl editorImpl) {
    this.editor = editorImpl;
    final CursorDisplay cursorDisplay = new CursorDisplay(editorImpl);

    addCheckBox("Editor on", !editorImpl.debugIsDisabled(),
        new ValueChangeHandler<Boolean>() {
          public void onValueChange(ValueChangeEvent<Boolean> event) {
            editorImpl.debugSetDisabled(!event.getValue());
          }
        });

    addCheckBox("Receive/send ops", editorImpl.debugIsConnected(),
        new ValueChangeHandler<Boolean>() {
          @Override
          public void onValueChange(ValueChangeEvent<Boolean> event) {
            editorImpl.debugConnectOpSinks(event.getValue());
          }
        });

    addCheckBox("Cancel unsafe combos", EditorEventHandler.getCancelUnsafeCombos(),
        new ValueChangeHandler<Boolean>() {
          public void onValueChange(ValueChangeEvent<Boolean> event) {
            EditorEventHandler.setCancelUnsafeCombos(event.getValue());
          }
        });

    addCheckBox("Show xy-selection cursor", cursorDisplay.getEnabled(),
        new ValueChangeHandler<Boolean>() {
          public void onValueChange(ValueChangeEvent<Boolean> event) {
            cursorDisplay.setEnabled(event.getValue());
          }
        });

    addCheckBox("Check local ops", ContentDocument.validateLocalOps,
        new ValueChangeHandler<Boolean>() {
          @Override
          public void onValueChange(ValueChangeEvent<Boolean> event) {
            ContentDocument.validateLocalOps = event.getValue();
          }
        });

    optionsPanel.add(new HTML("<br/>Permitted update event listeners:"));
    optionsPanel.add(updateEventsPanel);
    updateEventsPanel.getElement().getStyle().setPaddingLeft(10, Unit.PX);

    panel.add(optionsPanel);

    VerticalPanel rhs = new VerticalPanel();
    rhs.add(new HTML("XML to insert at current cursor location.<br/>" +
        "Ensure there is no extra whitespace anywhere between your tags!"));
    final TextArea contentInput = new TextArea();
    contentInput.setVisibleLines(10);
    contentInput.setCharacterWidth(40);
    rhs.add(contentInput);
    final Label errorLabel = new Label("");
    Button insertXmlButton = new Button("Insert");
    insertXmlButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        editor.focus(true);
        ContentRange selection = editor.getSelectionHelper().getOrderedSelectionPoints();
        if (selection == null) {
          errorLabel.setText("Don't know where to insert this");
          return;
        }

        XmlStringBuilder xml;
        try {
           xml = XmlStringBuilder.innerXml(DocProviders.POJO.parse(contentInput.getText()));
        } catch (RuntimeException e) {
          errorLabel.setText("Ill formed XML");
          return;
        }

        try {
          editor.getDocument().insertXml(selection.getSecond(), xml);
          errorLabel.setText("");
        } catch (RuntimeException e) {
          errorLabel.setText("Invalid XML: " + e.getMessage());
        }
      }
    });
    rhs.add(errorLabel);
    rhs.add(insertXmlButton);
    panel.add(rhs);
  }

  public void refresh() {
    final EditorUpdateEventImpl updates = editor.debugGetUpdateEventImpl();
    updateEventsPanel.clear();
    updates.debugGetAllUpdateEventNames().each(new Proc() {
      @Override
      public void apply(final String element) {
        addCheckBox(updateEventsPanel, element,
            !updates.debugGetSuppressedUpdateEventNames().contains(element),
            new ValueChangeHandler<Boolean>() {
              @Override
              public void onValueChange(ValueChangeEvent<Boolean> event) {
                updates.debugSuppressUpdateEvent(element, !event.getValue());
              }
            });
      }
    });
  }

  public Widget getWidget() {
    return panel;
  }

  private void addCheckBox(String caption, boolean initValue,
      ValueChangeHandler<Boolean> handler) {
    addCheckBox(optionsPanel, caption, initValue, handler);
  }

  private void addCheckBox(Panel panel, String caption, boolean initValue,
      ValueChangeHandler<Boolean> handler) {
    CheckBox box = new CheckBox(caption);
    box.setValue(initValue);
    box.addValueChangeHandler(handler);
    panel.add(box);
  }
}
