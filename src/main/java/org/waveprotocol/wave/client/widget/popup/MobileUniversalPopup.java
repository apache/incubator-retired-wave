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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing UniversalPopup for mobile clients.
 *
 *
 * TODO(user) rename this class SmallScreenUniversalPopup
 */
class MobileUniversalPopup implements UniversalPopup {

  /** Internal class used to implement popups on mobile */
  static final class InternalPopup extends ComplexPanel {
    private final Element container;
    private final Element shadow;
    private final Element content;

    /** Command to be executed when user clicks on shadow. */
    private final Command hideCommand;

    /** Resources used by this widget. */
    interface Resources extends ClientBundle {
      /** CSS */
      @Source("MobileUniversalPopup.css")
      Css css();
    }

    /** CSS for this widget. */
    interface Css extends CssResource {
      String container();
      String shadow();
      String content();
    }

    /** The singleton instance of resources. */
    private static final Resources RESOURCES = GWT.create(Resources.class);
    static {
      StyleInjector.inject(RESOURCES.css().getText());
    }

    /** Helper function to create a DivElement with a given style name */
    private Element createDiv(String style) {
      DivElement x = Document.get().createDivElement();
      x.setClassName(style);
      return (Element) Element.as(x);
    }

    /** Create a new InternalPanel */
    public InternalPopup(Command hideCommand) {
      this.hideCommand = hideCommand;
      container = createDiv(RESOURCES.css().container());
      container.appendChild(shadow = createDiv(RESOURCES.css().shadow()));
      container.appendChild(content = createDiv(RESOURCES.css().content()));
      setElement(container);
      sinkEvents(Event.ONCLICK);
    }

    /** Insert widget as first child */
    public void insertFirst(Widget child) {
      super.insert(child, content, 0, true);
    }

    /** {@inheritDoc} */
    @Override
    public void add(Widget child) {
      super.add(child, content);
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (event.getTypeInt() == Event.ONCLICK && event.getTarget() == shadow) {
        hideCommand.execute();
      } else {
        super.onBrowserEvent(event);
      }
    }
  }

  /** Our instance of InternalPopup */
  private final InternalPopup popup = new InternalPopup(new Command() {
    public void execute() {
      hide();
    }
  });

  /** List of PopupEventListeners */
  private final List<PopupEventListener> listeners = new ArrayList<PopupEventListener>();

  /** The root panel we add to when show is called */
  private final Panel root;

  // TODO(user) replace with a mobile implementation.
  private DesktopTitleBar titleBar;

  // visibility state of this popup.
  private boolean showing = false;

  /**
   * Create a new MobileUniversalPopup.
   * @param root The root panel to which popups will be added when show is called.
   */
  MobileUniversalPopup(Panel root) {
    this.root = root;
    DebugClassHelper.addDebugClass(popup, DEBUG_CLASS);
  }

  /**
   * {@inheritDoc}
   */
  public void hide() {
    showing = true;
    root.remove(popup);
    for (PopupEventListener l : listeners) {
      l.onHide(this);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void show() {
    showing = false;
    root.add(popup);
    for (PopupEventListener l : listeners) {
      l.onShow(this);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void move() {
    // Do nothing
  }

  /**
   * {@inheritDoc}
   */
  public void addPopupEventListener(PopupEventListener listener) {
    listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  public void removePopupEventListener(PopupEventListener listener) {
    listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  public TitleBar getTitleBar() {
    if (titleBar == null) {
      titleBar = new DesktopTitleBar();
      popup.insertFirst(titleBar);
    }
    return titleBar;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isShowing() {
    return showing;
  }

  /**
   * {@inheritDoc}
   */
  public void add(Widget w) {
    popup.add(w);
  }

  /**
   * {@inheritDoc}
   */
  public void clear() {
    popup.clear();
  }

  /**
   * {@inheritDoc}
   */
  public boolean remove(Widget w) {
    return popup.remove(w);
  }

  /**
   * {@inheritDoc}
   */
  public void associateWidget(Widget w) {
    // Ignore on mobiles, where no auto-hiding takes place
  }

  @Override
  public void setMaskEnabled(boolean isMaskEnabled) {
    throw new IllegalStateException("masking is not defined for MobileUniversalPopup.");
  }

  @Override
  // TODO(user): change this method name to addDebugClass
  public void setDebugClass(String dcName) {
    if (dcName != null) {
      DebugClassHelper.addDebugClass(popup.getElement(), dcName);
    }
  }
}
