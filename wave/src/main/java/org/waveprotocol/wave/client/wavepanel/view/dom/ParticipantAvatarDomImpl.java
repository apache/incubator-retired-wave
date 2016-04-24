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
import com.google.gwt.dom.client.ImageElement;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantView;

/**
 * DOM implementation of a participant.
 *
 */
public final class ParticipantAvatarDomImpl implements DomView, IntrinsicParticipantView {
  private final ImageElement self;

  ParticipantAvatarDomImpl(Element self) {
    this.self = self.cast();
  }

  public static ParticipantAvatarDomImpl of(Element e) {
    return new ParticipantAvatarDomImpl(e);
  }

  @Override
  public void setAvatar(String url) {
    self.setSrc(url);
  }

  @Override
  public void setName(String name) {
    self.setTitle(name);
    self.setAlt(name);
  }

  //
  // Structure.
  //

  public void remove() {
    self.removeFromParent();
  }

  //
  // DomView
  //

  @Override
  public Element getElement() {
    return self;
  }

  @Override
  public String getId() {
    return self.getId();
  }

  @Override
  public boolean equals(Object obj) {
    return DomViewHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return DomViewHelper.hashCode(this);
  }
}
