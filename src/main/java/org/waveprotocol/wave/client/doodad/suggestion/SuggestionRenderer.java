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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager.HasSuggestions;
import org.waveprotocol.wave.model.document.util.Property;

/**
 * Renderer for Suggestions.
 *
 */
public class SuggestionRenderer implements Renderer {

  public static final Property<HasSuggestions> HAS_SUGGESTIONS_PROP =
      Property.immutable("has-suggestions");
  public static final String FULL_TAGNAME = "l:suggestion";

  SuggestionRenderer() {
  }

  @Override
  public Element createDomImpl(Renderable element) {
    // HACK(danilatos) temp workaround, see todo in SuggestionButton and Suggestion
    // regarding their dep on ContentElement
    ContentElement haxContentElement = (ContentElement) element;

    HasSuggestions suggestions = new Suggestion(haxContentElement,
        haxContentElement.getContext());
    element.setProperty(HAS_SUGGESTIONS_PROP, suggestions);

    SuggestionButton display = new SuggestionButton(haxContentElement);
    // TODO(user): Avoid using non-html tag names
    Element nodelet = Document.get().createElement(FULL_TAGNAME);
    DomHelper.setContentEditable(nodelet, false, false);
    DomHelper.makeUnselectable(nodelet);
    return display.getElement();
  }

  /**
   * Registers self with an element handler registry.
   *
   * @param handlerRegistry the registry to register with.
   */
  void register(ElementHandlerRegistry handlerRegistry) {
    handlerRegistry.registerRenderer(FULL_TAGNAME, this);
  }
}
