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

package org.waveprotocol.wave.client.widget.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * Standard dialog box with title, message and set of buttons.
 *
 * @author Denis Konovalchik (dyukon@gmail.com)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class DialogBox {

  public static class DialogButton {
    private String title;
    private Command onClick;
    private Button button;

    public DialogButton(String title) {
      this.title = title;
    }

    public DialogButton(String title, Command onClick) {
      this.title = title;
      this.onClick = onClick;
    }

    public void setTitle(String title) {
      this.title = title;
      if (button != null) {
        button.setText(title);
      }
    }

    public void setOnClick(Command onClick) {
      this.onClick = onClick;
    }

    public String getTitle() {
      return title;
    }

    public void execute() {
      if (onClick != null) {
        onClick.execute();
      }
    }

    void link(Button button) {
      this.button = button;
      button.setText(title);
      button.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          execute();
        }
      });
    }
  }

  /**
   * Creates dialog box.
   *
   * @param popup - UniversalPopup on which the dialog is based
   * @param title - title placed in the title bar
   * @param innerWidget - the inner widget of the dialog
   * @param dialogButtons - buttons
   */
  static public void create(UniversalPopup popup, String title, Widget innerWidget, DialogButton[] dialogButtons) {
    // Title
    popup.getTitleBar().setTitleText(title);

    VerticalPanel contents = new VerticalPanel();
    popup.add(contents);

    // Message
    contents.add(innerWidget);

    // Buttons
    HorizontalPanel buttonPanel = new HorizontalPanel();
    for(DialogButton dialogButton : dialogButtons) {
      Button button = new Button(dialogButton.getTitle());
      button.setStyleName(Dialog.getCss().dialogButton());
      buttonPanel.add(button);
      dialogButton.link(button);
    }
    contents.add(buttonPanel);
    buttonPanel.setStyleName(Dialog.getCss().dialogButtonPanel());
    contents.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_RIGHT);
  }
}
