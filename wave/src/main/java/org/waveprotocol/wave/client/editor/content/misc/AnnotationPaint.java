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

import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.extract.PasteFormatRenderers;

import java.util.HashMap;
import java.util.Map;

/**
 * Element for rendering bits of annotations
 *
 * TODO(danilatos): Move this file, and {AnnotationBoundary|AnnotationSpread}Renderer to
 * client.doodad.paint once the dependencies from the editor package to these are removed
 * (mainly spelly).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class AnnotationPaint {

  /**
   * Handlers may register callback for browser events over painted regions
   */
  public interface EventHandler {
    void onEvent(ContentElement node, Event event);
  }

  /**
   * Attribute for mapping callback strings.
   */
  public static final String MOUSE_LISTENER_ATTR = "mouseListener";

  /**
   * Handlers may register callback for mutation events over painted regions.
   */
  public interface MutationHandler {
    void onMutation(ContentElement node);
  }

  static final Map<String, MutationHandler> mutationHandlerRegistry =
    new HashMap<String, MutationHandler>();

  /**
   * Attribute for mapping callback strings.
   */
  public static final String MUTATION_LISTENER_ATTR = "mutationListener";

  static final Map<String, EventHandler> eventHandlerRegistry =
      new HashMap<String, EventHandler>();

  /**
   * The link attribute, i.e. an url.
   */
  public static final String LINK_ATTR = "link";

  /**
   * "l" for "local" (as in non-persistent)
   */

  /** Full tag name including namespace for paint nodes */
  public static final String SPREAD_FULL_TAGNAME = "l:s";

  /** Full tag name including namespace for boundary nodes */
  public static final String BOUNDARY_FULL_TAGNAME = "l:b";

  /**
   * Registers subclass with ContentElement
   */
  public static void register(ElementHandlerRegistry registry) {
    AnnotationSpreadRenderer paintRenderer = new AnnotationSpreadRenderer();
    registry.registerRenderingMutationHandler(SPREAD_FULL_TAGNAME, paintRenderer);
    registry.registerNiceHtmlRenderer(SPREAD_FULL_TAGNAME, PasteFormatRenderers.SHALLOW_CLONE_RENDERER);

    AnnotationBoundaryRenderer boundaryRenderer = new AnnotationBoundaryRenderer();
    registry.registerRenderer(BOUNDARY_FULL_TAGNAME, boundaryRenderer);
    registry.registerEventHandler(BOUNDARY_FULL_TAGNAME, AnnotationBoundaryRenderer.EVENT_HANDLER);
  }

  /**
   * Registers a callback, which will be called on the content node if the
   * attribute value for CALLBACK_ATTR corresponds to the key.
   *
   * @param key
   * @param handler
   */
  public static void registerEventHandler(String key, EventHandler handler) {
    eventHandlerRegistry.put(key, handler);
  }

  /**
   * Registers a callback, which will be called on the content node if the
   * attribute value for CALLBACK_ATTR corresponds to the key.
   *
   * @param key
   * @param handler
   */
  public static void setMutationHandler(String key, MutationHandler handler) {
    mutationHandlerRegistry.put(key, handler);
  }

  /**
   * Removes a callback by key.
   *
   * @param key the key callback to remove.
   */
  public static void clearMutationHandler(String key) {
    mutationHandlerRegistry.remove(key);
  }

  private AnnotationPaint() {}
}
