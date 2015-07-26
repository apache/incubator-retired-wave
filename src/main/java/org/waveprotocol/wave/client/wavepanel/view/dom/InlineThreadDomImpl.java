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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicInlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder;

/**
 * DOM implementation of an inline thread.
 *
 */
public final class InlineThreadDomImpl implements DomView, IntrinsicInlineThreadView {

  private final CollapsibleDomImpl c;

  InlineThreadDomImpl(CollapsibleDomImpl c) {
    this.c = c;
  }

  public static InlineThreadDomImpl of(Element e, CollapsibleBuilder.Css css) {
    return new InlineThreadDomImpl(CollapsibleDomImpl.of(e, css));
  }

  public static InlineThreadDomImpl ofToggle(Element e, CollapsibleBuilder.Css css) {
    return new InlineThreadDomImpl(CollapsibleDomImpl.ofToggle(e, css));
  }

  @Override
  public boolean isCollapsed() {
    return c.isCollapsed();
  }

  @Override
  public void setCollapsed(boolean collapsed) {
    c.setCollapsed(collapsed);
  }

  public void remove() {
    getElement().removeFromParent();
  }

  //
  // DomView nature.
  //

  @Override
  public Element getElement() {
    return c.getElement();
  }

  @Override
  public String getId() {
    return c.getId();
  }

  //
  // Structure.
  //

  Element getBlipContainer() {
    return c.getChrome().getFirstChildElement();
  }
  
  public Element getContinuationIndicator() {
    return c.getChrome().getFirstChildElement().getNextSiblingElement();
  }

  @Override
  public void setTotalBlipCount(int totalBlipCount) {
    c.setTotalBlipCount(totalBlipCount);
  }

  @Override
  public void setUnreadBlipCount(int unreadBlipCount) {
    c.setUnreadBlipCount(unreadBlipCount);
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