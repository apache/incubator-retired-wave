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

package org.waveprotocol.wave.client.doodad.diff;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Renderer;

import java.util.List;

/**
 * Renders diff content for things that have been removed since last view.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author patcoleman@google.com (Pat Coleman)
 */
public class DiffDeleteRenderer implements Renderer {
  /** Singleton instance: */
  private static DiffDeleteRenderer instance;
  public static DiffDeleteRenderer getInstance() {
    if (instance == null) {
      instance = new DiffDeleteRenderer();
    }
    return instance;
  }

  /** Tag details: */
  public static final String FULL_TAGNAME = "l:diffdel";

  /**
   * Registers subclass with ContentElement
   */
  public static void register(ElementHandlerRegistry handlerRegistry) {
    // register the annotation handler:
    handlerRegistry.registerRenderer(FULL_TAGNAME, getInstance());
  }

  /** Create a renderer, taking an object to use to manage properties. */
  private DiffDeleteRenderer() {
  }

  /** Adds internal html of deleted contents to a diff element. */
  public void setInnards(ContentElement element, List<Element> deletedStuff) {
    Element nodelet = element.getContainerNodelet(); // first find DOM element
    for (Element e : deletedStuff) {
      nodelet.appendChild(e);
    }
  }

  @Override
  public Element createDomImpl(Renderable element) {
    Element nodelet = Document.get().createSpanElement();
    DomHelper.makeUnselectable(nodelet);
    DomHelper.setContentEditable(nodelet, false, true);
    return element.setAutoAppendContainer(nodelet);
  }
}
