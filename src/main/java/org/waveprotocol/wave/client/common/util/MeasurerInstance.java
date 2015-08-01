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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.dom.client.Element;

/**
 * Provides layout properties of elements. In particular, reveals such
 * properties in fractional units, on browsers that have sub-pixel rendering.
 *
 */
public final class MeasurerInstance {
  /** Measuring strategy. */
  private static final Measurer instance =
      UserAgent.isFirefox() ? new BoundingClientRectMeasurer() : new GwtMeasurer();

  /**
   * @return the singleton measuring strategy.
   */
  public static Measurer get() {
    return instance;
  }

  /**
   * GWT's default measuring logic.
   */
  static final class GwtMeasurer implements Measurer {
    @Override
    public double bottom(Element base, Element e) {
      return e.getAbsoluteBottom() - (base != null ? base.getAbsoluteTop() : 0);
    }

    @Override
    public double top(Element base, Element e) {
      return e.getAbsoluteTop() - (base != null ? base.getAbsoluteTop() : 0);
    }

    @Override
    public double left(Element base, Element e) {
      return e.getAbsoluteLeft() - (base != null ? base.getAbsoluteLeft() : 0);
    }

    @Override
    public double height(Element e) {
      return e.getOffsetHeight();
    }

    @Override
    public double offsetTop(Element e) {
      return e.getOffsetTop();
    }

    @Override
    public double offsetBottom(Element e) {
      return e.getOffsetTop() + e.getOffsetHeight();
    }
  }

  /**
   * A Measurer that uses getBoundingClientRect().
   */
  public static final class BoundingClientRectMeasurer implements Measurer {
    @Override
    public native double top(Element base, Element elem) /*-{
      return elem.getBoundingClientRect().top - (base ? base.getBoundingClientRect().top : 0);
    }-*/;

    @Override
    public native double left(Element base, Element elem) /*-{
      return elem.getBoundingClientRect().left - (base ? base.getBoundingClientRect().left : 0);
    }-*/;

    @Override
    public native double bottom(Element base, Element elem) /*-{
      return elem.getBoundingClientRect().bottom - (base ? base.getBoundingClientRect().top : 0);
    }-*/;

    @Override
    public native double height(Element elem) /*-{
      var rect = elem.getBoundingClientRect();
      return rect.bottom - rect.top;
    }-*/;

    @Override
    public double offsetBottom(Element e) {
      return bottom(e.getOffsetParent(), e);
    }

    @Override
    public double offsetTop(Element e) {
      return top(e.getOffsetParent(), e);
    }
  }
}
