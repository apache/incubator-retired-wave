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

import com.google.gwt.dom.client.Element;
import org.waveprotocol.wave.model.document.util.Property;

/**
 * Renderer that creates the DOM implementation for a node.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Renderer {

  /**
   * View of an element consisting of methods relevant to rendering.
   */
  interface Renderable {

    /**
     * @return the element's tag name. This is the only piece of state made
     *         available to the renderer, as it is immutable. If the renderer
     *         relied on any other information such as attributes or children,
     *         then it would be disobeying the contract of creating a *blank*
     *         html implementation, making things less predictable.
     */
    String getTagName();

    /**
     * Optionally call this to specify a default attach point for children. It
     * may be the same as the dom impl nodelet, a descendant of the dom impl
     * nodelet, or null. If null, child html implementations will not be
     * automatically attached during mutations. If not null, it must not contain
     * any children of its own to start with, as they will all be removed.
     *
     * If this method is not called, the value default to null and no automatic
     * behaviour occurs.
     *
     * @param containerNodelet
     * @return its input, for convenience (useful when used as the last line in
     *         {@link Renderer#createDomImpl(Renderable)} where the container
     *         nodelet is the same as the dom impl nodelet)
     */
    // TODO(danilatos): Abolish container nodelet by providing equivalent
    // functionality in a utility implementation of node mutation handler,
    // so that renderers desiring that behaviour may use it instead.
    Element setAutoAppendContainer(Element containerNodelet);

    /**
     * Optionally sets a transient properties on the element.
     *
     * An example use of this method is to assign a reference to a specific
     * widget view interface for use by other handlers.
     *
     * @param <T>
     * @param property
     * @param value
     */
    // TODO(danilatos): A mechanism to allow handlers to clean up after themselves
    <T> void setProperty(Property<T> property, T value);
  }

  /**
   * Creates a blank DOMImpl corresponding this node.
   *
   * @param element the implementor of this method may optionally call some of
   *   the setters
   */
  Element createDomImpl(Renderable element);

}
