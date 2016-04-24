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

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.selection.content.SelectionUtil;

/**
 * An element that gets deleted by backspace and delete key events in the
 * expected manner.
 *
 * It may be useful as a base class for other handlers as well.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ChunkyElementHandler extends NodeEventHandlerImpl {
  /**
   * Instance, exposed for delegation if needed.
   */
  public static final ChunkyElementHandler INSTANCE = new ChunkyElementHandler();

  /** Register a self-deleting element */
  public static void register(String tagName, final ElementHandlerRegistry registry) {
    registry.registerEventHandler(tagName, INSTANCE);
  }

  @Override
  public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
    element.getMutableDoc().deleteNode(element);
    return true;
  }


  @Override
  public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
    element.getMutableDoc().deleteNode(element);
    return true;
  }

  @Override
  public boolean handleLeftAfterNode(ContentElement element, EditorEvent event) {
    SelectionUtil.placeCaretBeforeElement(element.getSelectionHelper(), element);
    return true;
  }

  @Override
  public boolean handleRightBeforeNode(ContentElement element, EditorEvent event) {
    SelectionUtil.placeCaretAfterElement(element.getSelectionHelper(), element);
    return true;
  }
}
