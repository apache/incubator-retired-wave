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

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;

/**
 * DOM implementation of an anchor view.
 *
 */
public final class AnchorDomImpl implements DomView {
  private final Element self;

  private AnchorDomImpl(Element e) {
    this.self = e;
  }

  public static AnchorDomImpl of(Element e) {
    return new AnchorDomImpl(e);
  }

  @Override
  public Element getElement() {
    return self;
  }

  @Override
  public String getId() {
    return self.getId();
  }

  public void setChild(Element e) {
    Preconditions.checkArgument(e != null);
    Preconditions.checkArgument(e.getParentElement() == null);
    Preconditions.checkState(self.getFirstChildElement() == null);
    self.appendChild(e);
  }

  public void removeChild(Element e) {
    Preconditions.checkArgument(e != null && e.getParentElement() == self);
    e.removeFromParent();
  }

  public Element getChild() {
    // Do not cache this.
    return self.getFirstChildElement();
  }

  //
  // Anchors are somewhat special, because if they are placed inline in editor
  // content then they can be removed from the DOM without their consent. Since
  // removing an anchor may require a consequent update to some state in the
  // containing view (e.g., the blip-meta's inline-locator list), a
  // post-detachment notification is too late to use the usual mechanism of
  // locating a view's containing view (DOM traversal). Therefore, when
  // attaching an anchor to a containing view, its DOM id is preserved, so that
  // after involuntary detachment, the appropriate notification can be sent to
  // the containing view without needing DOM traversal.
  //

  /**
   * Sets the DOM id of this anchor's containing view. This id is still
   * available, via {@link #getParentId()}, even after this anchor has been
   * detached from the DOM.
   *
   * @param id parent id, or {@code null} to clear
   * @throws IllegalArgumentException if the nullity of {@code id} and the
   *         nullity of an existing parent id are equal.
   */
  public void setParentId(String id) {
    // Only null -> non-null and non-null -> null transitions are permitted.
    // Other transitions are signs of bugs.
    if (self.hasAttribute("pid")) {
      Preconditions.checkArgument(id == null);
      self.removeAttribute("pid");
    } else {
      Preconditions.checkArgument(id != null && !id.isEmpty());
      self.setAttribute("pid", id);
    }
  }

  /**
   * @return the value previously recorded by {@link #setParentId(String)}, or
   *         {@code null} if no value has been recorded.
   */
  public String getParentId() {
    return self.hasAttribute("pid") ? self.getAttribute("pid") : null;
  }
}
