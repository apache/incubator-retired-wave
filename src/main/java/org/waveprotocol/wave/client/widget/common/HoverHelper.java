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

package org.waveprotocol.wave.client.widget.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;

/**
 * Helper class for guaranteeing enter and leave mouse events.
 *
 * On some browsers (at least Firefox), the mouse over and out events around DOM
 * that is both mutating and whose rendering is changing do not necessarily come
 * out symmetrically. One scenario seems to be if the mouse is over an element
 * that gets removed from the DOM (e.g., over an editor button that removes
 * itself when clicked on), no mouse-out event gets generated for that event,
 * and thus the containing elements don't know that the mouse is now somewhere
 * else.
 *
 * This class explicitly maintains a reference to the last widget that grabbed
 * hover, so that when a widget grabs hover, and the previously hovered-on
 * widget did not get a mouseOut, it still gets programmatically de-hovered.
 *
 */
public final class HoverHelper {
  /**
   * Something that can be hovered on.
   */
  public interface Hoverable {
    /**
     * Notifies this hoverable that the mouse is now hovering on it.
     */
    void onMouseEnter();

    /**
     * Notifies this hoverable that the mouse is no longer hovering on it.
     */
    void onMouseLeave();

    /**
     * Tests if an element is a descendant of this hoverable.
     *
     * @param e element to test
     * @return {@code true} if {@code e} is a descendant of this hoverable.
     */
    boolean isOrHasChild(Element e);

    /**
     * Adds handlers to this hoverable.
     *
     * @param mouseOverHandler a mouse-over handler
     * @param mouseOutHandler a mouse-out handler
     */
    void addHandlers(MouseOverHandler mouseOverHandler, MouseOutHandler mouseOutHandler);
  }

  /**
   * Common global instance of HoverHelper.
   */
  private static final HoverHelper HOVER_HELPER = new HoverHelper();

  /**
   * Last hoverable to get a mouseOver.
   */
  private Hoverable last;

  @VisibleForTesting
  HoverHelper() {
  }

  public static HoverHelper getInstance() {
    return HOVER_HELPER;
  }

  /**
   * Installs event handlers on a hoverable to track hovering programmatically.
   *
   * @param hoverable  hoverable to track
   */
  public void setup(final Hoverable hoverable) {
    //
    // Install mouse-over and mouse-out handlers.
    //
    // The handlers always cancel bubbling, in case hoverables are nested
    // (otherwise, less-specific top-level elements would grab hover from more
    // specific nested elements).
    //
    // This is not so nice, because mouse over/out handlers shouldn't really
    // cancel bubbling in order to cooperate (e.g., imagine if some random
    // element deep in the DOM stopped a hoverable from finding out about
    // mouse-overs...).
    //
    hoverable.addHandlers(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        enter(hoverable);
        event.stopPropagation();
      }
    }, new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        EventTarget to = event.getRelatedTarget();
        // Blur, unless we're moving into a descendant.
        if (!(to != null && hoverable.isOrHasChild(Element.as(to)))) {
          exit(hoverable);
        }
        event.stopPropagation();
      }
    });
  }

  /**
   * Moves hover status to a hoverable.
   *
   * @param hoverable  hoverable to which browser hover has moved
   */
  @VisibleForTesting
  void enter(Hoverable hoverable) {
    // Mouse Enter
    if (last != hoverable) {
      if (last != null) {
        last.onMouseLeave();
      }
      last = hoverable;
      last.onMouseEnter();
    }
  }

  /**
   * Removes hover status from a hoverable.
   *
   * @param hoverable  hoverable away from which browser hover has moved
   */
  @VisibleForTesting
  void exit(Hoverable hoverable) {
    // Mouse Leave
    if (last != hoverable) {
      // Mouse has left something that didn't know it was hovered on.
      // This branch does not occur in normal operation. It has only been
      // observed to occur when something like Firebug Inspect is mucking
      // around with hovering.
      return;
    }

    last.onMouseLeave();
    last = null;
  }
}
