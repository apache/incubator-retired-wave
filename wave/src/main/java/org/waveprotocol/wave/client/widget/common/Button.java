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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;

public class Button extends Label {

  /**
   * Flag if button is enabled
   */
  protected boolean enabled = false;

  /**
   * Overrides the same method as per Label. Updates the CSS style on the parent
   * DIV, and then passes on control to the superclass implementation.
   *
   * @see com.google.gwt.user.client.ui.Label#onBrowserEvent(com.google.gwt.user.client.Event)
   */
  @Override
  public void onBrowserEvent(Event event) {

    switch (event.getTypeInt()) {
    case Event.ONMOUSEDOWN:
      me.replaceClassName(ButtonStyle.mouseUp, ButtonStyle.mouseDown);
      if (cancelMouseDownBubble) {
        event.stopPropagation();
      }
      if (preventMouseDownDefault) {
        event.preventDefault();
      }
      break;
    case Event.ONMOUSEUP:
      me.replaceClassName(ButtonStyle.mouseDown, ButtonStyle.mouseUp);
      break;
    case Event.ONMOUSEOVER:
      me.replaceClassName(ButtonStyle.mouseOut, ButtonStyle.mouseOver);
      break;
    case Event.ONMOUSEOUT:
      me.replaceClassName(ButtonStyle.mouseOver, ButtonStyle.mouseOut);
      break;
    }

    // Call to super to handle all event generation.
    super.onBrowserEvent(event);
  }

  /**
   * UIElement version of widget
   */
  protected Element me = null;

  /**
   * UIElement in center of table for labeled buttons
   */
  protected Element c = null;

  /**
   * Constructs a simple CSS button around a single div. Use
   * this for iconic buttons sans label
   *
   * @param styleName A style name to append to the button.
   * @param title A title to give to the button.
   * @param handler A click handler for the CSSButton.
   */
  public Button(String styleName, String title,
          final ClickHandler handler) {
    me = getElement();
    setStyleName(ButtonStyle.button);
    addStyleName(styleName);
    addStyleName(ButtonStyle.mouseUp);
    addStyleName(ButtonStyle.mouseOut);
    setEnabled(true);
    if (handler != null) {
      addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent e) {
          if (enabled) {
            handler.onClick(e);
          }
        }
      });
    }
    setTitle(title);
  }

  /**
   * Constructs a not-so-simple CSS button around a div with a three-<td>
   * table. Use this for buttons with labels
   *
   * @param styleName A style name to append to the button.
   * @param title A title to give to the button.
   * @param handler A click handler for the CSSButton.
   * @param label The text label for the button
   */
  public Button(String styleName, String title,
          ClickHandler handler, String label) {
    this(styleName, title, handler);
    // TODO(user): Use CssResource's @sprite.
    addStyleName(ButtonStyle.labeledButton);
    me.setInnerHTML(
        "<table cellSpacing='0'><tbody><tr>" +
        "<td><div class='" + ButtonStyle.left + "'></div></td>" +
        "<td class='" + ButtonStyle.center + " " + ButtonStyle.xRepeat + "'></td>" +
        "<td><div class='" + ButtonStyle.right + "'></div></td>" +
        "</tr></tbody></table>");
    // TODO(user): Declarative UI
    c = me.getChild(0).<Element>cast().getChild(0).<Element>cast()
      .getChild(0).<Element>cast().getChild(1).cast();
    c.setInnerHTML(label);
  }

  /**
   * Flag that mouse down events should not bubble.
   *
   * @see #setCancelMouseDownBubble
   */
  private boolean cancelMouseDownBubble = false;

  /**
   * Flags that button should eat mouse downs (as in canceling the
   * event's bubbling). Use this, e.g., on buttons
   * that results in panel focus going away from panel containing the
   * button. That way, if user click the button on a blurred panel,
   * the panel doesn't get focus in the short time between mousedown
   * and click. For example: close button on conversation panel.
   */
  public void setCancelMouseDownBubble() {
    cancelMouseDownBubble = true;
  }

  /**
   * Flag that mouse down events should not perform default action.
   *
   * @see #setPreventMouseDownDefault
   */
  private boolean preventMouseDownDefault = false;

  /**
   * Flags that button should eat mouse downs' default action. Use
   * this on buttons with an onclick handler depending on the current
   * selection to avoid that the mousedown destroys that selection.
   * For example, the reply button on blip widgets
   */
  public void setPreventMouseDownDefault() {
    preventMouseDownDefault = true;
  }

  /**
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (enabled) {
      me.replaceClassName(ButtonStyle.disabled, ButtonStyle.enabled);
    } else {
      me.replaceClassName(ButtonStyle.enabled, ButtonStyle.disabled);
    }
  }

  /**
   * @return True if button is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}
