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

package org.waveprotocol.wave.client.doodad.form.events;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.NodeMutationHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.NullRenderer;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/** Useful document elements for representing user events inside the document. */
public final class ContentEvents {

  private static final String TAGNAME = "events";

  /** Register all behaviours for the events elements. */
  public static void register(ElementHandlerRegistry handlerRegistry) {
    handlerRegistry.registerRenderer(TAGNAME, NullRenderer.INSTANCE);
    handlerRegistry.registerEventHandler(TAGNAME, ContentEventsNodeHandler.getInstance());
    handlerRegistry.registerMutationHandler(TAGNAME, ContentEventsNodeMutationHandler.getInstance());

    // Register all {@link Event} subclasses here
    Click.register(handlerRegistry);
  }

  /**
   * @return A content xml string containing an empty events list
   */
  public static XmlStringBuilder constructXml() {
    return XmlStringBuilder.createEmpty().wrap(TAGNAME);
  }

  private ContentEvents() {
  }

  private static class ContentEventsNodeHandler extends NodeEventHandlerImpl {
    private static ContentEventsNodeHandler instance;
    /**
     */
    public static ContentEventsNodeHandler getInstance() {
      if (instance == null) {
        instance = new ContentEventsNodeHandler();
      }
      return instance;
    }

    /**
     * Records a click inside this events node if clicks are enabled
     */
    @Override
    public boolean handleClick(ContentElement element, EditorEvent event) {
      if (isClickingEnabled(element)) {
        recordClick(element);
        return true;
      } else {
        // TODO(user): For now let an additional click clear all events
        clearEvents(element);
        return true;
      }
    }

    /**
     * Records a click event inside the events node
     */
    private void recordClick(ContentElement element) {
      element.getMutableDoc().insertXml(
          Point.<ContentNode>end(element), Click.constructXml());
    }

    /**
     * Clears all events
     */
    private void clearEvents(ContentElement element) {
      element.getMutableDoc().emptyElement(element);
    }
  }

  /**
   * Returns true iff node is a ContentEvents element.
   * @param node
   */
  public static boolean isContentEvents(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, TAGNAME);
  }

  /**
   * @return true if events node will record another click; currently
   *    true when no other click events are presents
   *
   * TODO(user): construct attributes for the <w:events> node that gives control
   *     over which events are recorded, e.g., one per clicker vs. one overall,
   *     only when editor is displaying and so on...
   */
  public static boolean isClickingEnabled(ContentElement element) {
    for (ContentNode child = element.getFirstChild(); child != null;
         child = child.getNextSibling()) {
      if (Click.isClick(child)) {
        return false;
      }
    }
    return true;
  }

  private static class ContentEventsNodeMutationHandler extends
      NodeMutationHandlerImpl<ContentNode, ContentElement> {
    private static ContentEventsNodeMutationHandler instance;

    /**
     */
    public static ContentEventsNodeMutationHandler getInstance() {
      if (instance == null) {
        instance = new ContentEventsNodeMutationHandler();
      }
      return instance;
    }

    @Override
    public void onAddedToParent(ContentElement element, ContentElement oldParent) {
      onContentEventsChanged(element);
      super.onAddedToParent(element, oldParent);
    }

    @Override
    public void onDescendantsMutated(ContentElement element) {
      onContentEventsChanged(element);
      super.onDescendantsMutated(element);
    }

    /**
     * Notifies parent that this events node has changed, if parent implements
     */
    private void onContentEventsChanged(ContentElement element) {
      ContentElement parent = element.getParentElement();
      // TODO(user): Notify parent if parent cares.
    }
  }
}
