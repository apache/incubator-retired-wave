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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler.CaretViewFactory;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;

/**
 * Renderer for the caret marker place holder, and responsible for attachment of
 * a marker into said place holder.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class CaretMarkerRenderer implements Renderer, CaretViewFactory {

  /** Doodad tag information to relate to this renderer. */
  public static final String NS = "l";
  public static final String TAGNAME = "uname";
  public static final String FULL_TAGNAME = NS + ":" + TAGNAME;

  /** Singleton instance: */
  private static final CaretMarkerRenderer instance = new CaretMarkerRenderer();

  public static CaretMarkerRenderer getInstance() {
    return instance;
  }

  private CaretMarkerRenderer() { }

  @Override
  public Element createDomImpl(Renderable element) {
    Element renderSpan = Document.get().createSpanElement();
    DomHelper.makeUnselectable(renderSpan);
    NodeManager.setTransparency(renderSpan, Skip.DEEP);
    return renderSpan;
  }

  @Override
  public CaretView createMarker() {
    return new CaretWidget();
  }

  public void setMarker(Object element, CaretView marker) {
    // Hack for bug 2868754
    Element impl = ((ContentElement) element).getImplNodelet();
    if (impl != null) {
    impl.appendChild(
        ((CaretWidget) marker).getElement());
    }
  }
}
