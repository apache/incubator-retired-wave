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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.autohide.AutoHider;
import org.waveprotocol.wave.client.autohide.AutoHider.KeyBehavior;
import org.waveprotocol.wave.client.autohide.AutoHiderRegistrarHolder;
import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;
import org.waveprotocol.wave.client.scheduler.ScheduleTimer;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

/**
 * Class implementing UniversalPopup for Desktop clients.
 *
 *
 * Class is package private.
 */
class DesktopUniversalPopup extends FlowPanel implements UniversalPopup {
  /** Resources used by DesktopUniversalPopup. */
  interface Resources extends ClientBundle {
    /** CSS class names used by DesktopUniversalPopup */
    interface Css extends CssResource {
      /** style to give popup-behaviour to the popup */
      String popup();

      /** style to apply to the mask */
      String mask();

      /** style to animate fade-in */
      String fadeIn();

      /** style to animate fade-out */
      String fadeOut();
    }

    /** The singleton instance of our CSS resources. */
    static final Resources INSTANCE = GWT.<Resources>create(Resources.class);

    /** css */
    @Source("DesktopUniversalPopup.css")
    Css css();

    static final String FADE_IN_MS = DEFAULT_FADE_IN_DURATION_MS + "ms";
    static final String FADE_OUT_MS = DEFAULT_FADE_OUT_DURATION_MS + "ms";
  }

  /**
   * Inject the stylesheet only once, and only when the class is used.
   */
  static {
    // Injection must be synchronous (not the default behaviour of
    // asynchronous), because positioning the popup involves synchronously
    // measuring its layout properties.
    boolean synchronous = true;
    StyleInjector.inject(Resources.INSTANCE.css().getText(), synchronous);
  }

  /** Time that the fade-in animation should take */
  private static final int DEFAULT_FADE_IN_DURATION_MS = 250;

  /** Time that the fade-out animation should take */
  private static final int DEFAULT_FADE_OUT_DURATION_MS = 350;

  /**
   * Time to wait after the start of fade-out animation before completing DOM
   * removal. Allow enough time for the fade-out animation to complete.
   */
  private static final int DEFAULT_REMOVE_MS = DEFAULT_FADE_OUT_DURATION_MS + 100;

  /** List of popup event listeners */
  private final CopyOnWriteSet<PopupEventListener> listeners = CopyOnWriteSet.create();

  /** Positioner to use when show is called. */
  private final RelativePopupPositioner positioner;

  /**
   * Element relative to which this popup is positioned (may be null).
   *
   * NOTE(hearnden/macpherson): the model to which we want to move is that the
   *   popup DOM is pulled from a pool, and that popup state is supplied at the
   *   point where a popup is shown (rather than instantiating a popup object).
   *   To simplify "singleton" popups, there will be a popup description that
   *   encapsulates the multiple items of state, and that can be supplied to the
   *   new show mechanism.
   *   After that has been implemented, this reference will go away (as will
   *   the positioner above).
   */
  private final Element reference;

  /** Visibility state of this popup */
  private boolean showing = false;

  /** The title bar widget, or null if title bar is not enabled */
  private DesktopTitleBar titleBar;

  /**
   * The PopupChrome which adds a border to this panel.
   */
  private final PopupChrome chrome;

  /** Contains AutoHide logic for this popup. */
  private AutoHider autoHide;

  /** Keep track of whether this popup should actually be auto-hidden. */
  private final boolean shouldAutoHide;

  /** The div that puts a mask over the screen. */
  private DivElement maskDiv;

  /** Whether or not the mask should be shown when the popup is shown. */
  private boolean isMaskEnabled = false;

  /**
   * Create a new DesktopUniversalPopupPanel
   * @param p The positioner to use to determine popup position.
   * @param chrome The chrome for this popup, or null if no chrome is required.
   * @param autoHide If true, clicking outside the popup will cause the popup to hide itself.
   */
  DesktopUniversalPopup(Element reference, RelativePopupPositioner p, PopupChrome chrome,
      boolean autoHide) {
    DebugClassHelper.addDebugClass(getElement(), DEBUG_CLASS);
    this.chrome = chrome;
    this.positioner = p;
    this.reference = reference;
    this.shouldAutoHide = autoHide;

    getElement().setClassName(Resources.INSTANCE.css().popup());

    if (chrome != null) {
      add(chrome.getChrome());
    }
  }


  @Override
  // TODO(user): change this method name to addDebugClass
  public void setDebugClass(String dcName) {
    if (dcName != null) {
      DebugClassHelper.addDebugClass(getElement(), dcName);
    }
  }

  @Override
  public void hide() {
    // nothing to do if we are already invisible
    if (!showing) {
      return;
    }

    final Element clone = (Element)getElement().cloneNode(true);
    RootPanel.getBodyElement().appendChild(clone);
    showing = false;
    if (isMaskEnabled) {
      setMaskVisible(false);
    }
    RootPanel.get().remove(DesktopUniversalPopup.this);
    if (shouldAutoHide) {
      deregisterAutoHider();
    }

    // trigger fade-out
    clone.removeClassName(Resources.INSTANCE.css().fadeIn());
    clone.addClassName(Resources.INSTANCE.css().fadeOut());
    clone.getOffsetWidth(); // Force update
    clone.getStyle().setOpacity(0.0);


    // schedule removal of clone from DOM once animation complete
    new ScheduleTimer() {
      @Override
      public void run() {
        clone.removeFromParent();
      }
    }.schedule(DEFAULT_REMOVE_MS);

    // fire popup event listeners
    for (PopupEventListener listener : listeners) {
      listener.onHide(DesktopUniversalPopup.this);
    }
  }

  @Override
  public void show() {
    // nothing to do if we are already visible.
    if (showing) {
      return;
    }

    // we are invisbile, need to set up the popup
    getElement().getStyle().setVisibility(Visibility.HIDDEN);
    getElement().getStyle().setOpacity(0.0);

    if (isMaskEnabled) {
      setMaskVisible(true);
    }
    RootPanel.get().add(this);
    if (shouldAutoHide) {
      registerAutoHider();
    }
    if (positioner != null) {
      position();
    } else {
      getElement().getStyle().setVisibility(Visibility.VISIBLE);
    }
    for (PopupEventListener listener : listeners) {
      listener.onShow(this);
    }

    // trigger the fade-in animation
    getElement().removeClassName(Resources.INSTANCE.css().fadeOut());
    getElement().addClassName(Resources.INSTANCE.css().fadeIn());
    getOffsetWidth();  // force update
    getElement().getStyle().setOpacity(1.0);

    // change state to appearing
    showing = true;
  }

  /**
   * @param isVisible Whether or not the mask should be visible on the screen.
   */
  private void setMaskVisible(boolean isVisible) {
    if (isVisible) {
      RootPanel.get().getElement().appendChild(getOrCreateMask());
    } else {
      RootPanel.get().getElement().removeChild(getOrCreateMask());
    }
  }

  /**
   * Creates the div for the mask if it doesn't already exist.
   */
  private Element getOrCreateMask() {
    if (maskDiv == null) {
      maskDiv = Document.get().createDivElement();
      maskDiv.setClassName(Resources.INSTANCE.css().mask());
    }
    return maskDiv;
  }

  @Override
  public void move() {
    position();
  }

  @Override
  public void clear() {
    super.clear();
    if (chrome != null) {
      add(chrome.getChrome());
    }
  }

  @Override
  public void addPopupEventListener(PopupEventListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removePopupEventListener(PopupEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    // We don't want anything outside the popup to receive events
    event.stopPropagation();
  }

  private void registerAutoHider() {
    // NOTE(patcoleman): assumes hiding on both escape and outside click.
    //   This could later be extended to set either hiding separately.
    maybeCreateAutoHider();
    AutoHiderRegistrarHolder.get().registerAutoHider(autoHide);
  }

  private void deregisterAutoHider() {
    AutoHiderRegistrarHolder.get().deregisterAutoHider(autoHide);
  }

  @Override
  public TitleBar getTitleBar() {
    if (titleBar == null) {
      titleBar = new DesktopTitleBar();
      insert(titleBar, 0);
      if (chrome != null) {
        chrome.enableTitleBar();
      }
    }
    return titleBar;
  }

  @Override
  public boolean isShowing() {
    return showing;
  }

  @Override
  public void associateWidget(Widget w) {
    maybeCreateAutoHider();
    autoHide.ignoreHideClickFor(w.getElement()); // add another widget to the 'inside' list
  }

  /**
   * Positions and displays this popup.
   */
  private void position() {
    positioner.setPopupPositionAndMakeVisible(reference, getElement());
  }

  /** Utility to lazily set up the autohider (but not register it). */
  private void maybeCreateAutoHider() {
    if (autoHide == null) {
      if (isMaskEnabled) {
        autoHide = new AutoHider(this, false, false, false, KeyBehavior.DO_NOT_HIDE_ON_ANY_KEY);
        autoHide.ignoreHideClickFor(maskDiv);
      } else {
        autoHide = new AutoHider(this, true, true, true, KeyBehavior.HIDE_ON_ESCAPE);
      }
      autoHide.ignoreHideClickFor(getElement()); // your own element is inside
    }
  }

  @Override
  public void setMaskEnabled(boolean isMaskEnabled) {
    this.isMaskEnabled = isMaskEnabled;
  }
}
