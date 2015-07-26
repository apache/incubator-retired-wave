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

import org.waveprotocol.wave.client.editor.content.Renderer.Renderable;

/**
 * Restricted view of an element that allows only manipulation of its impl
 * nodelets.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface HasImplNodelets extends Renderable {

  /**
   * Set both the impl nodelet and the container nodelet in one go.
   * @param domImplNodelet
   * @param containerNodelet
   */
  void setImplNodelets(Element domImplNodelet, Element containerNodelet);

  /**
   * Convenience method, equivalent to calling
   * {@link #setImplNodelets(Element, Element)} with the same object for both
   * arguments
   */
  void setBothNodelets(Element implAndContainerNodelet);

  /**
   * @return The top-level wrapped implementation html nodelet. It might be null
   *         if it is not rendered for whatever reason.
   */
  Element getImplNodelet();

  /**
   * Auto-append container nodelet to which the impl nodelets of child model
   * model nodes will be automatically inserted.
   *
   * @return container node
   */
  Element getContainerNodelet();
}
