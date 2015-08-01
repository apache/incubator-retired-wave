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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

/**
 * Represents an x, y position relative to an offsetParent.
 *
 * If offsetParent is null, then interpret left and top as absolute positions on
 * the browser.
 *
 */
public final class OffsetPosition {
  public final int left;
  public final int top;
  public final Element offsetParent;

  public OffsetPosition(int left, int top, Element offsetParent) {
    this.left = left;
    this.top = top;
    this.offsetParent = offsetParent;
  }

  /**
   * Create a offset position base on the event's client X, Y. The return offset
   * position is relative to the Document body coordinate.
   *
   * @param event
   */
  public OffsetPosition(Event event) {
    // convert the event's client coordinate system, which is client area base, to the
    // body position coordinate system.

    this.left = event.getClientX() + Window.getScrollLeft() - Document.get().getBodyOffsetLeft();
    this.top = event.getClientY() + Window.getScrollTop() - Document.get().getBodyOffsetTop();

    this.offsetParent = null;
  }

  /**
   * Gets the position of target relative to a specified element.
   * @param target
   * @param relative
   */
  public static OffsetPosition getRelativePosition(OffsetPosition target, Element relative) {
    int parentLeft = 0;
    int parentTop = 0;
    if (target.offsetParent != null) {
      parentLeft = target.offsetParent.getAbsoluteLeft();
      parentTop = target.offsetParent.getAbsoluteTop();
    }
    int left = parentLeft + target.left - relative.getAbsoluteLeft();
    int top =  parentTop + target.top - relative.getAbsoluteTop();
    return new OffsetPosition(left, top, relative);
  }
}
