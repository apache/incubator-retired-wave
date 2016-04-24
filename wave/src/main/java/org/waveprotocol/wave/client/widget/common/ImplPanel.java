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

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.WidgetCollection;

import org.waveprotocol.wave.client.common.util.LogicalPanel;

/**
 * A generic panel for use by UiBinder templates, for widgets that require more
 * powerful control over their DOM and/or child lifecycle.
 *
 * Like {@link HTMLPanel}, its state can be defined in UiBinder using regular
 * HTML, allowing child widgets to be attached statically at arbitrary
 * locations. Unlike HTMLPanel, however, it also exposes the ability to attach
 * child widgets <em>dynamically</em> at arbitrary locations. It also exposes
 * the ability to adopt widgets already in the DOM (and, symmetrically, to
 * orphan them).
 *
 */
public class ImplPanel extends HTMLPanel
    implements
    LogicalPanel,
    HasClickHandlers,
    HasDoubleClickHandlers,
    HasMouseDownHandlers {

  /**
   * Creates an panel with some initial inner-HTML.
   *
   * @param html initial inner contents
   */
  public ImplPanel(String html) {
    super(html);
  }

  /**
   * Adds a widget to this panel.
   */
  @Override
  public void add(Widget child) {
    add(child, getElement());
  }

  // Expose proteced method.
  @Override
  public void add(Widget child, com.google.gwt.user.client.Element container) {
    super.add(child, container);
  }

  /**
   * Adds a widget, inserting it as the first child in the DOM structure.
   *
   * @param child widget to add
   */
  public void insertFirst(Widget child) {
    insertBefore(child, getElement(), getElement().getFirstChildElement());
  }

  @Override
  protected void insert(Widget child, com.google.gwt.user.client.Element container, int beforeIndex,
      boolean domInsert) {
    // This method is intentionally suppressed, because ComplexPanel's insert()
    // method operates under the assumption that the only HTML contents of this
    // panel's element are the elements of this panel's child widgets (i.e., no
    // HTML decorations or chrome elements). This is not the desired behaviour
    // for ImplPanel.
    throw new UnsupportedOperationException("Index-based insertion is not supported by ImplPanel");
  }

  /**
   * Inserts a widget into this panel, attaching its HTML to a specified
   * location within this panel's HTML.
   * <p>
   * Note that in order for this panel to have arbitrary HTML decorations,
   * rather than none at all, this panel must not care about the logical order
   * of its child widgets (an inherited restriction from ComplexPanel, which
   * assumes logical child index == physical DOM index).
   * <p>
   * Assumes (but does not check) that {@code container} is a descendant of this
   * widget's element, and that {@code reference} is a direct child of {@code
   * container}.
   *
   * @param child
   * @param container
   * @param reference
   */
  public void insertBefore(Widget child, Element container, Element reference) {
    // The implementation below is identical to add(), except the physical DOM
    // insertion is positional.

    // Detach new child.
    child.removeFromParent();

    // Logical attach.
    getChildren().add(child);

    // Physical attach.
    container.insertBefore(child.getElement(), reference);

    // Adopt.
    adopt(child);
  }

  /**
   * Inserts a widget into this panel, attaching its HTML to a specified
   * location within this panel's HTML.
   * <p>
   * Note that in order for this panel to have arbitrary HTML decorations,
   * rather than none at all, this panel must not care about the logical order
   * of its child widgets (an inherited restriction from ComplexPanel, which
   * assumes logical child index == physical DOM index).
   * <p>
   * Assumes (but does not check) that {@code container} is a descendant of this
   * widget's element, and that {@code reference} is a direct child of {@code
   * container}.
   *
   * @param child
   * @param container
   * @param reference
   */
  public void insertAfter(Widget child, Element container, Element reference) {
    // The implementation below is identical to add(), except the physical DOM
    // insertion is positional.

    // Detach new child.
    child.removeFromParent();

    // Logical attach.
    getChildren().add(child);

    // Physical attach.
    if (reference == null) {
      container.insertFirst(child.getElement());
    } else {
      container.insertAfter(child.getElement(), reference);
    }

    // Adopt.
    adopt(child);
  }

  @Override
  public void doAdopt(Widget child) {
    Preconditions.checkArgument(child != null && child.getParent() == null, "Not an orphan");
    getChildren().add(child);
    adopt(child);
  }

  @Override
  public void doOrphan(Widget child) {
    Preconditions.checkArgument(child != null && child.getParent() == this, "Not a child");
    orphan(child);
    getChildren().remove(child);
  }

  /**
   * Narrows an object to a widget, if it is a child of this panel. Otherwise,
   * throws an exception.
   *
   * @param o object to narrow
   * @return {@code o} as a widget
   * @throws IllegalArgumentException if {@code o} is not a child of this panel.
   */
  public Widget narrowChild(Object o) {
    // Note: it is very important that this method can be implemented WITHOUT
    // casting.  This implementation uses casting as an optimization.  However,
    // a non-casting version is provided in comments. Anyone touching this
    // method must ensure that a non-casting possibility exists.
    //
    // for (Widget child : getChildren()) {
    //   if (child == o) {
    //     return child;
    //   }
    // }
    // return null;
    //
    Widget w = o instanceof Widget ? (Widget) o : null;
    if (w != null && w.getParent() == this) {
      return w;
    } else {
      throw new IllegalArgumentException("Not a child");
    }
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  @Override
  public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
    return addDomHandler(handler, DoubleClickEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(handler, MouseDownEvent.getType());
  }

  @Override
  public WidgetCollection getChildren() {
    return super.getChildren();
  }
}
