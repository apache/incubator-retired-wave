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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * Create a debug popup for digest panel.
 *
 */
public final class DebugPopupFactory {

  /**
   * Prevents construction.
   */
  private DebugPopupFactory() {
  }

  /**
   * Create a debug popup.
   * @param editorImpl
   */
  public static UniversalPopup create(EditorImpl editorImpl) {
    final DebugDialog debugDialog = new DebugDialog(editorImpl);
    RelativePopupPositioner positioner = new RelativePopupPositioner() {
      public void setPopupPositionAndMakeVisible(Element reference, Element popup) {
        com.google.gwt.dom.client.Style popupStyle = popup.getStyle();
        popupStyle.setTop(50, Unit.PX);
        popupStyle.setLeft(50, Unit.PX);
        popupStyle.setVisibility(Visibility.VISIBLE);
        popupStyle.setPosition(Position.FIXED);
      }
    };
    PopupEventListener listener = new PopupEventListener() {
      public void onHide(PopupEventSourcer source) {
        debugDialog.onHide();
      }

      public void onShow(PopupEventSourcer source) {
        debugDialog.onShow();
      }
    };
    final UniversalPopup popup =
      EditorStaticDeps.createPopup(null, positioner, false, true, debugDialog, listener);
    if (popup.getTitleBar() != null) {
      popup.getTitleBar().setTitleText("Editor Debug");
      popup.getTitleBar().addButton(new Button(" X ", new ClickHandler() {
          public void onClick(ClickEvent event) {
            popup.hide();
          }
        }));
    }
    return popup;
  }
}
