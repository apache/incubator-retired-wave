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

package org.waveprotocol.wave.client.widget.button;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for handling multiple orthogonal CSS classnames for an element
 * simultaneously.
 *
 * The motivating case for this class is styles on a button. Buttons might have
 * two CSS class names applied at any given time - one class to represent the
 * button state (NORMAL, HOVER, DOWN) and one to represent the high level
 * 'style' of the button (BLUE_BUTTON, ADD_BUTTON, etc) - and we achieve a
 * different look by defining CSS classes like this:
 *
 * .normal.blue_button { // normal blue button style }
 *
 * .hover.blue_button { // hovered blue button style }
 *
 * With {@link StyleAxis} this would be represented as:
 *
 * StyleAxis buttonState = new StyleAxis(getElement()); StyleAxis buttonStyle =
 * new StyleAxis(getElement());
 *
 * Then we can move along one axis without affecting the other:
 *
 * buttonState.setStyle("normal"); buttonStyle.setStyle("red_button");
 *
 */
public class StyleAxis {
  /**
   * The element to apply styles to.
   */
  private final List<Element> elements = new ArrayList<Element>();

  /**
   * The current style that is applied to the target element.
   */
  private String currentStyle = null;

  /**
   * @param widgets The widgets to apply styles to.
   */
  public StyleAxis(Widget... widgets) {
    for (Widget w : widgets) {
      if (w != null) {
        elements.add(w.getElement());
      }
    }
  }

  /**
   * @param elements The elements to apply styles to.
   */
  public StyleAxis(Element... elements) {
    for (Element e : elements) {
      if (e != null) {
        this.elements.add(e);
      }
    }
  }

  /**
   * Replaces any current style that may be applied with the given style.
   *
   * @param styleName The CSS style to change to, or {@code null} to change to
   *        no style.
   */
  public void setStyle(String styleName) {
    if (!isEquivalentStyle(styleName)) {
      /* Remove the current style if set */
      if (currentStyle != null && currentStyle.trim().length() != 0) {
        for (Element e : elements) {
          e.removeClassName(currentStyle);
        }
      }

      /* Add the new style if set */
      if (styleName != null) {
        for (Element e : elements) {
          e.addClassName(styleName);
        }
      }
      currentStyle = styleName;
    }
  }

  /**
   * Returns true if the provided style is equivalent to the current style.
   *
   * @param styleName The style name to compare to the current style
   */
  private boolean isEquivalentStyle(String styleName) {
    return ((styleName == null && currentStyle == null) || (styleName != null && styleName
        .equals(currentStyle)));
  }
}
