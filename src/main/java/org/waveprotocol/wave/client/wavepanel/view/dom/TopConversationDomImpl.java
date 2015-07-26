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

package org.waveprotocol.wave.client.wavepanel.view.dom;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.dom.full.TopConversationViewBuilder.Components;

/**
 * Dom impl of a conversation view.
 *
 */
public final class TopConversationDomImpl implements DomView {

  /** The DOM element of this view. */
  private final Element self;

  /** The HTML id of {@code self}. */
  private final String id;

  //
  // UI fields for both intrinsic and structural elements.
  // Element references are loaded lazily and cached.
  //

  private Element threadContainer;
  private Element toolbarContainer;

  TopConversationDomImpl(Element e, String id) {
    this.self = e;
    this.id = id;
  }

  public static TopConversationDomImpl of(Element e) {
    return new TopConversationDomImpl(e, e.getId());
  }

  //
  // DomView nature.
  //

  @Override
  public Element getElement() {
    return self;
  }

  @Override
  public String getId() {
    return id;
  }

  //
  // Structure.
  //

  public Element getThreadContainer() {
    if (threadContainer == null) {
      threadContainer = Document.get().getElementById(Components.THREAD_CONTAINER.getDomId(id));
    }
    return threadContainer;
  }

  private Element getToolbarContainer() {
    if (toolbarContainer == null) {
      toolbarContainer = Document.get().getElementById(Components.TOOLBAR_CONTAINER.getDomId(id));
    }
    return toolbarContainer;
  }

  public Element getParticipants() {
    return self.getFirstChildElement();
  }

  public Element getThread() {
    return getThreadContainer().getFirstChildElement();
  }

  public void setToolbar(Element toolbar) {
    getToolbarContainer().setInnerHTML("");
    if (toolbar != null) {
      getToolbarContainer().appendChild(toolbar);
    }
  }

  public void remove() {
    getElement().removeFromParent();
  }

  //
  // Equality.
  //

  @Override
  public boolean equals(Object obj) {
    return DomViewHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return DomViewHelper.hashCode(this);
  }
}
