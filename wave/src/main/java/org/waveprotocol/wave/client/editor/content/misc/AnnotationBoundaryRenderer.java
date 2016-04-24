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

package org.waveprotocol.wave.client.editor.content.misc;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.event.EditorEvent;

/**
 * Renderer for the place holder for the blobs of UI added at annotation
 * boundaries
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
class AnnotationBoundaryRenderer implements Renderer {

  /** Event handler */
  public static final NodeEventHandler EVENT_HANDLER = new NodeEventHandlerImpl() {
    @Override
    public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
      return handleBackspaceAtBeginning(element, event);
    }

    @Override
    public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
      return handleDeleteAtEnd(element, event);
    }
  };

  @Override
  public Element createDomImpl(Renderable element) {
    Element nodelet = Document.get().createSpanElement();

    DomHelper.makeUnselectable(nodelet);
    DomHelper.setContentEditable(nodelet, false, false);

    element.setAutoAppendContainer(nodelet);
    return nodelet;
  }
}
