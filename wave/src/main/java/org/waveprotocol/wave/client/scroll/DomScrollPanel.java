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


package org.waveprotocol.wave.client.scroll;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.Measurer;
import org.waveprotocol.wave.client.common.util.MeasurerInstance;

/**
 * Reveals the scrolling capabilities of a DOM element.
 *
 */
public final class DomScrollPanel implements ScrollPanel<Element> {
  private final Element element;
  private final Measurer measurer;

  DomScrollPanel(Element element, Measurer measurer) {
    this.element = element;
    this.measurer = measurer;
  }

  /**
   * Reveals {@code e} as a scroll panel. It is assumed that the given element
   * has appropriate CSS constraints that make it scrollable.
   */
  public static DomScrollPanel create(Element e) {
    return new DomScrollPanel(e, MeasurerInstance.get());
  }

  @Override
  public Extent getViewport() {
    int top = element.getScrollTop();
    return Extent.of(top, top + measurer.height(element));
  }

  @Override
  public Extent getContent() {
    Element first = element.getFirstChildElement();
    Element last = DomHelper.getLastChildElement(element);
    Preconditions.checkNotNull(first, "No content");
    int top = element.getScrollTop();
    return Extent.of(measurer.top(element, first) + top, measurer.bottom(element, last) + top);
  }

  @Override
  public Extent extentOf(Element target) {
    int top = element.getScrollTop();
    return Extent.of(measurer.top(element, target) + top, measurer.bottom(element, target) + top);
  }

  @Override
  public void moveTo(double location) {
    element.setScrollTop((int) location);
  }
}
