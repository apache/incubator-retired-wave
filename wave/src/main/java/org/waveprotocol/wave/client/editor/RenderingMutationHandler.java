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

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Renderer;

import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience base class for renderers that respond to mutation events
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class RenderingMutationHandler
  extends NodeMutationHandlerImpl<ContentNode, ContentElement>
  implements Renderer {

  /**
   * Convenience method that calls
   * {@link #onAttributeModified(ContentElement, String, String, String)} for
   * each existing attribute on the element, with {@code null} for the old value
   *
   * @param element
   */
  public static void fanoutAttrs(final NodeMutationHandler<ContentNode, ContentElement> handler,
      final ContentElement element) {
    element.getAttributes().each(new ProcV<String>() {
      @Override
      public void apply(String key, String item) {
        handler.onAttributeModified(element, key, null, item);
      }
    });
  }

  /**
   * Takes a snapshot of the given element's children (to be robust against
   * mutations while fanning out) and calls
   * {@link #onChildAdded(ContentElement, ContentNode)} for each child.
   *
   * @param handler
   * @param element
   */
  public static void fanoutChildren(final NodeMutationHandler<ContentNode, ContentElement> handler,
      final ContentElement element) {
    List<ContentNode> children = new ArrayList<ContentNode>();
    for (ContentNode node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      children.add(node);
    }
    if (children.isEmpty()) {
      handler.onEmptied(element);
    }
    for (ContentNode child : children) {
      handler.onChildAdded(element, child);
      child.onAddedToParent(null);
    }
  }

  /**
   * Calls {@link #fanoutAttrs(NodeMutationHandler, ContentElement)} with this.
   */
  // NOTE(danilatos): Should this method be called by default? Most doodads seem to want it.
  // Calling it by default would be inconsistent with respect to other methods though.
  protected final void fanoutAttrs(ContentElement element) {
    fanoutAttrs(this, element);
  }

  /**
   * Calls {@link #fanoutChildren(NodeMutationHandler, ContentElement)} with this.
   */
  protected final void fanoutChildren(ContentElement element) {
    fanoutChildren(this, element);
  }
}
