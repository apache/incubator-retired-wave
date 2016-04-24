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

import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.load;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.TOTAL_BLIPS_ATTRIBUTE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.UNREAD_BLIPS_ATTRIBUTE;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.RootThreadViewBuilder.Components;

/**
 * Dom impl of a thread view.
 *
 */
public class RootThreadDomImpl implements DomView, IntrinsicThreadView {

  /** The DOM element of this view. */
  private final Element self;
  

  /** The HTML id of {@code self}. */
  private final String id;
  
  //
  // UI fields for both intrinsic and structural elements.
  // Element references are loaded lazily and cached.
  //

  private Element blips;

  RootThreadDomImpl(Element e, String id) {
    this.self = e;
    this.id = id;
  }

  public static RootThreadDomImpl of(Element e) {
    return new RootThreadDomImpl(e, e.getId());
  }

  //
  // Structure exposed for external control.
  //

  public Element getBlipContainer() {
    if (blips == null) {
      blips = load(id, Components.BLIPS);
    }
    return blips;
  }
  
  public Element getIndicator() {
    return self.getFirstChildElement().getNextSiblingElement();
  }
  
  public void remove() {
    getElement().removeFromParent();
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

  @Override
  public void setTotalBlipCount(int totalBlipCount) {
    self.setAttribute(TOTAL_BLIPS_ATTRIBUTE, "" + totalBlipCount);
  }
  
  @Override
  public void setUnreadBlipCount( int unreadBlipCount ) {
    self.setAttribute(UNREAD_BLIPS_ATTRIBUTE, "" + unreadBlipCount);
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