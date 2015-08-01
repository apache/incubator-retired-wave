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

import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.getLastChildElement;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicInlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder;

/**
 * DOM implementation of an inline thread.
 *
 */
public final class InlineConversationDomImpl implements DomView, IntrinsicInlineConversationView {

  private final CollapsibleDomImpl c;

  InlineConversationDomImpl(CollapsibleDomImpl c) {
    this.c = c;
  }

  public static InlineConversationDomImpl of(Element e, CollapsibleBuilder.Css css) {
    return new InlineConversationDomImpl(CollapsibleDomImpl.of(e, css));
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

  Element getRootThread() {
    return getLastChildElement(c.getChrome());
  }

  Element getParticipants() {
    return c.getChrome().getFirstChildElement();
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
