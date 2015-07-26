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

package org.waveprotocol.wave.client.doodad.selection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;

/**
 * DOM implementation of a caret view
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class CaretWidget implements CaretView {

  /** Resources for the renderer. */
  private static final CaretMarkerResources RESOURCES = GWT.create(CaretMarkerResources.class);
  public static final CaretMarkerResources.Css CSS = RESOURCES.css();
  static {
    StyleInjector.inject(CSS.getText());
  }

  private final Element caretMarkerElement;
  private final Element compositionStateSpan;
  private final Element outerSpan;
  private final Element innerSpan;
  private final Text nameTextNode;

  /**
   * Build a DOM element for storing a single user selection element.
   */
  public CaretWidget() {
    // TODO(danilatos): UiBinder?
    caretMarkerElement = Document.get().createSpanElement();
    compositionStateSpan = Document.get().createSpanElement();

    caretMarkerElement.appendChild(compositionStateSpan);
    compositionStateSpan.setClassName(CSS.compositionState());

    outerSpan = Document.get().createSpanElement();
    innerSpan = Document.get().createSpanElement();
    caretMarkerElement.appendChild(outerSpan);

    // Text node must come first, or rendering glitches ensue.
    // Also, there is code that depends on it being the first child!
    nameTextNode = Document.get().createTextNode("?");
    outerSpan.appendChild(nameTextNode);
    outerSpan.appendChild(innerSpan);

    outerSpan.setClassName(CSS.outer());
    innerSpan.setClassName(CSS.inner());

    DomHelper.makeUnselectable(caretMarkerElement);
    NodeManager.setTransparency(caretMarkerElement, Skip.DEEP);
  }

  @Override
  public void setName(String name) {
    nameTextNode.setData(name);
  }

  @Override
  public void setCompositionState(String state) {
    compositionStateSpan.setInnerText(state);
  }

  @Override
  public void setColor(RgbColor color) {
    String cssColour = color.getCssColor();
    compositionStateSpan.getStyle().setBorderColor(cssColour);
    outerSpan.getStyle().setBackgroundColor(cssColour);
    innerSpan.getStyle().setBorderColor(cssColour);
  }

  public Element getElement() {
    return caretMarkerElement;
  }
}
