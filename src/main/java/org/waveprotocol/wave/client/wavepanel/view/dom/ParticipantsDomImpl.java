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

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ParticipantsViewBuilder.Components;

/**
 * The DOM implementation of a participant collection.
 *
 */
public final class ParticipantsDomImpl implements DomView, IntrinsicParticipantsView {

  /** The element to which this view is bound. */
  private final Element self;

  /** The HTML id of {@code self}. */
  private final String id;

  private Element participantContainer;
  private Element simple;

  ParticipantsDomImpl(Element self, String id) {
    this.self = self;
    this.id = id;
  }

  public static ParticipantsDomImpl of(Element e) {
    return new ParticipantsDomImpl(e, e.getId());
  }

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

  Element getParticipantContainer() {
    if (participantContainer == null) {
      participantContainer = DomViewHelper.load(id, Components.CONTAINER);
    }
    return participantContainer;
  }

  Element getSimpleMenu() {
    // The two menu elements are always in the participant container, and should
    // always be at the end.
    if (simple == null) {
      simple = getParticipantContainer().getLastChild().getPreviousSibling().cast();
    }
    return simple;
  }

  void remove() {
    self.removeFromParent();
  }
}
