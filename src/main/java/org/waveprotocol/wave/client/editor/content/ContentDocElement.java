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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.event.EditorEvent;

/**
 * Root document element. Provides some event handlers.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class ContentDocElement {

  public static final String DEFAULT_TAGNAME = "doc";

  public static void register(ElementHandlerRegistry registry, String tagName) {
    registry.registerRenderer(tagName, new Renderer() {
      @Override
      public Element createDomImpl(final Renderable element) {
        return element.setAutoAppendContainer(Document.get().createDivElement());
      }
    });

    registry.registerEventHandler(tagName, new NodeEventHandlerImpl() {
      @Override
      public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event) {
        return true;
      }

      @Override
      public boolean handleRightAtEnd(ContentElement element, EditorEvent event) {
        return true;
      }

      @Override
      public boolean handleDeleteAtEnd(ContentElement element, EditorEvent event) {
        return true;
      }

      @Override
      public boolean handleBackspaceAtBeginning(ContentElement element, EditorEvent event) {
        return true;
      }
    });
  }

  private ContentDocElement() {}
}
