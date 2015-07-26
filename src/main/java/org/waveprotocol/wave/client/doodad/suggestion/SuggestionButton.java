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

package org.waveprotocol.wave.client.doodad.suggestion;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager.HasSuggestions;
import org.waveprotocol.wave.client.widget.button.ButtonFactory;
import org.waveprotocol.wave.client.widget.button.ClickButton.ClickButtonListener;
import org.waveprotocol.wave.client.widget.button.icon.IconButtonTemplate.IconButtonStyle;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;

/**
 * Suggestion (lightbulb) button.
 *
 */
public class SuggestionButton {

  // @NotInternationalized
  private static final String TOOLTIP = "Suggestion";

  // TODO(user): Remove the dep on content element from this class by registering
  // the click handler in the context of the activate method of a node event handler,
  // which has access to the ContentElement.
  public SuggestionButton(final ContentElement element) {
    Widget clickButton =
        ButtonFactory.createIconClickButton(IconButtonStyle.LIGHTBULB, TOOLTIP,
        new ClickButtonListener() {
          @Override
          public void onClick() {
            HasSuggestions suggestion =
                element.getProperty(SuggestionRenderer.HAS_SUGGESTIONS_PROP);
            element.getSuggestionsManager().showSuggestionsFor(suggestion);
          }
        });
    clickButton.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
    // Logically attach it to the root panel to listen to events, but then
    // manually move the dom to the desired location.
    RootPanel.get().add(clickButton);
    this.element = clickButton.getElement();
    NodeManager.setTransparency(this.element, Skip.DEEP);
    DomHelper.makeUnselectable(this.element);
    this.element.setAttribute("contentEditable", "false");
  }

  private final Element element;

  public Element getElement() {
    return element;
  }
}
