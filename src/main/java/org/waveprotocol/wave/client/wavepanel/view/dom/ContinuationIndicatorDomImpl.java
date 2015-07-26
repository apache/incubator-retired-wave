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
import com.google.gwt.dom.client.Style.Display;

import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicContinuationIndicatorView;

/**
 * Dom impl of an inline continuation indicator.
 */
public final class ContinuationIndicatorDomImpl implements DomView, 
    IntrinsicContinuationIndicatorView {
  
  /**
   * Handles structural queries on menu-item views.
   *
   * @param <I> intrinsic indicator implementation
   */
  public interface Helper<I> {
    InlineThreadView getParent(I impl);
  }
  
  /** The DOM element of this view. */
  private final Element self;

  /** The HTML id of {@code self}. */
  private final String id;

  ContinuationIndicatorDomImpl(Element e, String id) {
    this.self = e;
    this.id = id;
  }

  public static ContinuationIndicatorDomImpl of(Element e) {
    return new ContinuationIndicatorDomImpl(e, e.getId());
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
 
  @Override
  public void enable() {
    self.getStyle().clearDisplay();
  }

  @Override
  public void disable() {
    self.getStyle().setDisplay(Display.NONE);
  }
  
  public void remove() {
    getElement().removeFromParent();
  }
}
