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

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.Property;

/**
 * Handler for nodes that listen to EditModeChange events.
 *
 *
 */
public class DisplayEditModeHandler {

  /**
   * Interface for listeners to edit mode change events.
   */
  public interface EditModeListener {
    void onEditModeChange(ContentElement element, boolean isEditing);
  }

  public static final Property<EditModeListener> EDIT_MODE_LISTENER_PROP =
      Property.immutable("display_edit_mode");

  private DisplayEditModeHandler() {
  }

  /**
   * Notify the node that the edit mode has changed.
   * @param node
   * @param isEditing
   */
  public static void onEditModeChange(ContentNode node, boolean isEditing) {
    EditModeListener listener = getListener(node);
    if (node.asElement() != null && listener != null) {
      listener.onEditModeChange(node.asElement(), isEditing);
    }
  }

  /**
   * Returns true iff the given node has an edit mode listener.
   * @param node
   */
  public static boolean hasListener(ContentNode node) {
    return getListener(node) != null;
  }

  private static EditModeListener getListener(ContentNode node) {
    ContentElement e = node.asElement();
    return e.asElement() != null ? e.getProperty(EDIT_MODE_LISTENER_PROP) : null;
  }

  /**
   * Sets an EditModeChange listener on this node.
   * @param element
   * @param listener
   */
  public static void setEditModeListener(ContentElement element, EditModeListener listener) {
    element.setProperty(EDIT_MODE_LISTENER_PROP, listener);
  }
}
