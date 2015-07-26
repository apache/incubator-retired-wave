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
import org.waveprotocol.wave.client.editor.content.NiceHtmlRenderer;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.model.util.ChainedData;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.DataDomain;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Registry for different types of handlers for document elements, based
 * on a match condition. Currently just tag name plus special attribute
 * matching is supported.
 *
 * There is a limitation in this approach, namely that handler subclasses
 * are not recognised - so, if you call getHandler to get a handler, you will
 * only get a result when passing in the exact .class with which it was
 * registered. For example, if you subclass Renderer, you'll probably need
 * to register the renderer twice, both with Renderer.class and YourRenderer.class,
 * or do type casting on the return value from Renderer.class.
 *
 * TODO(danilatos): Conceive of a nicer registering mechanism than class plus
 * tagname plus attribute
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ElementHandlerRegistry {

  /**
   * The minimum interface required by the registry to identify an element in
   * order to ascertain look up its handlers.
   */
  public interface HasHandlers {
    String getTagName();
  }

  private static class HandlerData {
    /** Handers for defining renderings for elements. */
    final StringMap<Renderer> renderers
        = CollectionUtils.createStringMap();

    /** Handlers for the events raised by interacting with an element. */
    final StringMap<NodeEventHandler> eventHandlers
        = CollectionUtils.createStringMap();

    /** Handlers for events raised by document tree mutations. */
    final StringMap<NodeMutationHandler<ContentNode, ContentElement>> mutationHandlers
        = CollectionUtils.createStringMap();

    /** Handlers for rendering to nice semantic html mode (rather than what looks good). */
    // NOTE(patcoleman): ideally should be removed from here, nice renderers could be registered
    // with the nice html formatter instead (or inferred from implementing a Renderer subtype).
    final StringMap<NiceHtmlRenderer> niceHtmlRenderers
        = CollectionUtils.createStringMap();

    /** Clears all known handler mappings. */
    void clear() {
      renderers.clear();
      eventHandlers.clear();
      mutationHandlers.clear();
      niceHtmlRenderers.clear();
    }
  }

  private static final DataDomain<HandlerData, HandlerData> handlerDataDomain =
      new DataDomain<HandlerData, HandlerData>() {
        @Override
        public void compose(HandlerData target, HandlerData changes, HandlerData base) {
          target.clear();
          copyInto(target, base);
          copyInto(target, changes);
        }

        private void copyInto(final HandlerData target, HandlerData source) {
          target.renderers.putAll(source.renderers);
          target.eventHandlers.putAll(source.eventHandlers);
          target.mutationHandlers.putAll(source.mutationHandlers);
          target.niceHtmlRenderers.putAll(source.niceHtmlRenderers);
        }

        @Override
        public HandlerData empty() {
          return new HandlerData();
        }

        @Override
        public HandlerData readOnlyView(HandlerData modifiable) {
          return modifiable;
        }
      };

  /**
   * The singleton registry that, as the terminal of the priority chain, is
   * always consulted last by any registry.
   */
  public static final ElementHandlerRegistry ROOT = new ElementHandlerRegistry();

  private final ChainedData<HandlerData, HandlerData> data;

  private ElementHandlerRegistry() {
    data = new ChainedData<HandlerData, HandlerData>(handlerDataDomain);
  }

  /**
   * @param parent  parent registry in the chain
   */
  private ElementHandlerRegistry(ElementHandlerRegistry parent) {
    data = new ChainedData<HandlerData, HandlerData>(parent.data);
  }

  /**
   * @return a chained child
   */
  public ElementHandlerRegistry createExtension() {
    return new ElementHandlerRegistry(this);
  }

  /// Specific handler management

  /** Register a renderer for a given tag name. */
  public void registerRenderer(String tag, Renderer renderer) {
    data.modify().renderers.put(tag, renderer);
  }

  /** Register an event handler for a given tag name. */
  public void registerEventHandler(String tag, NodeEventHandler eventHandler) {
    data.modify().eventHandlers.put(tag, eventHandler);
  }

  /** Register a mutation handler for a given tag name. */
  public void registerMutationHandler(String tag,
      NodeMutationHandler<ContentNode, ContentElement> mutationHandler) {
    data.modify().mutationHandlers.put(tag, mutationHandler);
  }

  /** Register both a renderer and a mutation handler for a given tag name. */
  public void registerRenderingMutationHandler(String tag, RenderingMutationHandler handler) {
    HandlerData mutable = data.modify();
    mutable.renderers.put(tag, handler);
    mutable.mutationHandlers.put(tag, handler);
  }

  /** Register a pretty semantic html renderer for a tag. */
  public void registerNiceHtmlRenderer(String tag, NiceHtmlRenderer renderer) {
    data.modify().niceHtmlRenderers.put(tag, renderer);
  }

  /** Retrieve the renderer instance for an item. */
  public Renderer getRenderer(HasHandlers target) {
    return data.inspect().renderers.get(target.getTagName());
  }

  /** Retrieve the event handler instance for an item. */
  public NodeEventHandler getEventHandler(HasHandlers target) {
    return data.inspect().eventHandlers.get(target.getTagName());
  }

  /** Retrieve the mutation handler instance for an item. */
  public NodeMutationHandler<ContentNode, ContentElement> getMutationHandler(HasHandlers target) {
    return data.inspect().mutationHandlers.get(target.getTagName());
  }

  /** Retrieve the clean html renderer instance for an item. */
  public NiceHtmlRenderer getNiceHtmlRenderer(HasHandlers target) {
    return data.inspect().niceHtmlRenderers.get(target.getTagName());
  }
}
