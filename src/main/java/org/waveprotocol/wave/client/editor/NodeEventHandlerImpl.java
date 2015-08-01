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

package org.waveprotocol.wave.client.editor;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.HandlerReferenceSet;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.Property;

/**
 * Empty implementation of NodeEventHandler
 *
 */
public class NodeEventHandlerImpl implements NodeEventHandler {
  private static final NodeEventHandler INSTANCE = new NodeEventHandlerImpl();

  // TODO(danilatos): Perhaps make the unregistering automatic?
  // Leaving it explicit for now so people see it in code and think about it.
  public static final class Helper {
    private static final Property<HandlerReferenceSet> HANDLERS = Property.mutable("handlers");

    public static void registerJsHandler(ContentElement element,
        Element nodelet, String event, JavaScriptEventListener handler) {
      HandlerReferenceSet handlers = element.getProperty(HANDLERS);
      if (handlers == null) {
        handlers = new HandlerReferenceSet();
        element.setProperty(HANDLERS, handlers);
      }
      handlers.references.add(DomHelper.registerEventHandler(nodelet, event, handler));
    }

    public static  void removeJsHandlers(ContentElement element) {
      HandlerReferenceSet handlers = element.getProperty(HANDLERS);
      if (handlers != null) {
        handlers.unregister();
        element.setProperty(HANDLERS, null);
      }
    }
  }


  /**
   * Either subclass this to extend add functionality, or use {@link #get()} to
   * get the singleton instance.
   */
  protected NodeEventHandlerImpl() {}

  /** {@inheritDoc} */
  public void onActivated(ContentElement element) {
    // Do nothing by default
  }

  @Override
  public void onDeactivated(ContentElement element) {
    // Do nothing by default
  }

  /**
   * Returns singleton instance of a nop implementation of NodeEventHandler.
   */
  public static NodeEventHandler get() {
    return INSTANCE;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleBackspaceAtBeginning(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleBackspaceNotAtBeginning(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleClick(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleDelete(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleDeleteAtEnd(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleDeleteNotAtEnd(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleEnter(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleLeft(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleLeftAfterNode(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleRight(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleRightAtEnd(ContentElement element, EditorEvent event) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean handleRightBeforeNode(ContentElement element, EditorEvent event) {
    return false;
  }
}
