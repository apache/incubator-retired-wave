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


package org.waveprotocol.wave.client.wavepanel.event;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootPanel;
import org.waveprotocol.box.stat.ExecutionTree;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.box.webclient.stat.dialog.StatDialog;

import org.waveprotocol.wave.client.common.util.EventWrapper;
import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.LinkedSequence;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEventImpl;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * Manages keyboard focus among a set of sibling focusables, firing focus and
 * blur events. Managers can be organized into a hierarchy, for modular UI
 * focus.
 * <p>
 * From the focusables in its domain, a focus manager keeps at most one of them
 * selected. Key events to this manager are routed to the selected focusable. As
 * this focus manager gains and loses focus, it fires focus and blur events on
 * the selected focusable.
 * <p>
 * To help explain the need for a hierarchy (rather than a flat domain of
 * focusables), and the difference between selected and focused, consider an
 * application with a windowed UI, with a main window and a chat window. In the
 * main window are multiple tabs, including an explorer tab with a tree control
 * of resources, and an editor tab for an open resource. If focus is in the chat
 * window, and some signal occurs to place application focus on a resource in
 * the tree control (e.g., a click event occurs on it), then that resource
 * demands application focus (using {@link #ensureGlobalFocus()}. This causes it
 * to become the selected resource in the domain of the tree control, the
 * explorer tab becomes the selected tab in the domain of tabs, and the main
 * window becomes the selected window in the domain of windows (stealing
 * selection and focus from the chat window). Additionally, each of those
 * objects becomes focused, which means that key events will be routed to them
 * (top down). If application focus is moved back to the chat window, the
 * explorer tab remains the selected tab in its domain, and the clicked-on
 * resource remains the selected resource in its domain. The selected window
 * transitions from the main window to the chat window.
 * <p>
 * The tree nature of focus managers is to enable modular UIs. The distinction
 * between selected and focused is so that selection can remain persistent
 * despite application focus moving around. The only constraint between
 * selection and focus is that, for the deepest focusable with application
 * focus, all its ancestors (including itself) are both focused and selected.
 *
 */
public final class FocusManager implements Focusable, KeySignalHandler {

  /** Unique top-level focus manager. */
  private final static FocusManager ROOT = new FocusManager(null, true);

  public static void init() {
    DocumentPanel.install(ROOT);
  }

  /**
   * Containing focus manager if there is one. Only {@link #ROOT} is intended to
   * be parent-less.
   */
  private final FocusManager parent;

  /** Focusables in this domain. */
  private final LinkedSequence<Focusable> focusOrder = LinkedSequence.create();

  /** Currently selected focusable. May be null. */
  private Focusable selected;

  /**
   * True iff this focus manager is focused (i.e., is between {@link #onFocus()}
   * and {@link #onBlur()}).
   */
  private boolean focused;

  private FocusManager(FocusManager parent, boolean focused) {
    this.parent = parent;
    this.focused = focused;
  }

  /** @return the root focus manager, that handles key events on the page. */
  public static FocusManager getRoot() {
    return ROOT;
  }

  /**
   * Creates a child focus manager, adding it to this focus manager's domain of
   * focusables.
   *
   * @return the new child manager.
   */
  public FocusManager createChild() {
    FocusManager child = new FocusManager(this, false);
    focusOrder.append(child);
    return child;
  }

  /**
   * Adds a focusable to this domain.
   *
   * @param focusable focusable to add
   */
  public void add(Focusable focusable) {
    focusOrder.append(focusable);
  }

  /**
   * Removes a focusable from this domain.
   *
   * @param focusable focusable to remove
   */
  public void remove(Focusable focusable) {
    focusOrder.remove(focusable);
  }

  /**
   * Moves focus to the next focusable in this domain.
   */
  public void selectNext() {
    select(focusOrder.getNext(selected));
  }

  /**
   * Moves focus to the previous focusable in this domain.
   */
  public void selectPrevious() {
    select(focusOrder.getPrevious(selected));
  }

  /**
   * Ensures that this manager has global focus. All ancestor managers become
   * selected and focused, and this manager's selected focusable gains
   * application focus.
   */
  public void ensureGlobalFocus() {
    if (!focused) {
      if (parent != null) {
        parent.select(this);
        parent.ensureGlobalFocus();
      }
      assert focused;
      // The onFocus handler already propagates the onFocus event to the current
      // focusable.
    }
  }

  /**
   * Sets the selected focusable.
   *
   * @param focusable focusable to select
   */
  public void select(Focusable focusable) {
    Preconditions.checkArgument(focusable == null || focusOrder.contains(focusable));
    if (ValueUtils.equal(selected, focusable)) {
      // No-op.
      return;
    }

    if (focused && selected != null) {
      selected.onBlur();
    }
    selected = focusable;
    if (focused && selected != null) {
      selected.onFocus();
    }
  }

  @Override
  public void onFocus() {
    Preconditions.checkState(!focused);
    focused = true;
    if (selected != null) {
      selected.onFocus();
    }
  }

  @Override
  public void onBlur() {
    Preconditions.checkState(focused);
    if (selected != null) {
      selected.onBlur();
    }
    focused = false;
  }

  @Override
  public boolean onKeySignal(KeyCombo key) {
    return (selected != null) ? selected.onKeySignal(key) : false;
  }

  /**
   * Special panel to grab events for the whole page, and dispatch them to the
   * top-level handlers.
   */
  private final static class DocumentPanel extends ComplexPanel
      implements KeyDownHandler, KeyUpHandler, KeyPressHandler {

    private final KeySignalHandler globalHandler;

    private int statDialogCondition = 0;

    private DocumentPanel(KeySignalHandler handler) {
      this.globalHandler = handler;
    }

    /**
     * Installs a key handler for key events on this window.
     *
     * @param handler handler to receive key events.
     */
    static void install(KeySignalHandler handler) {
      //
      // NOTE: There are three potential candidate elements for sinking keyboard
      // events: the window, the document, and the document body. IE7 does not
      // fire events on the window element, and GWT's RootPanel is already a
      // listener on the body, leaving the document as the only cross-browser
      // whole-window event-sinking 'element'.
      //
      DocumentPanel panel = new DocumentPanel(handler);
      panel.setElement(Document.get().<Element>cast());
      panel.addDomHandler(panel, KeyDownEvent.getType());
      panel.addDomHandler(panel, KeyPressEvent.getType());
      panel.addDomHandler(panel, KeyUpEvent.getType());
      RootPanel.detachOnWindowClose(panel);
      panel.onAttach();
    }

    @Override
    public void onKeyDown(KeyDownEvent event) {
      dispatch(event);
    }

    @Override
    public void onKeyUp(KeyUpEvent event) {
      dispatch(event);
    }

    @Override
    public void onKeyPress(KeyPressEvent event) {
      dispatch(event);
    }

    private void dispatch(KeyEvent<?> event) {
      if (checkStatDialogCondition(event)) {
        StatDialog.show();
      } else {
	Timer timer = null;
        if (Timing.isEnabled()) {
          Timing.enterScope();
          Timing.setScopeValue(ExecutionTree.class, new ExecutionTree());
          timer = Timing.start("Key event dispatch");
        }
        try {
          // Only respond to key events on the body element. Otherwise, the key
          // event was probably targeted to some editable input element, and that
          // should own the events.
          NativeEvent realEvent = event.getNativeEvent();
          Element target = realEvent.getEventTarget().cast();
          if (!"body".equals(target.getTagName().toLowerCase())) {
            return;
          }
          // Test that the event is meaningful (and stop bubbling if it is not).
          SignalEvent signal = SignalEventImpl.create(realEvent.<Event>cast(), true);
          if (signal != null) {
            KeyCombo key = EventWrapper.getKeyCombo(signal);
            if (globalHandler.onKeySignal(key)) {
              event.preventDefault();
            }
          }
        } finally {
          Timing.stop(timer);
          Timing.exitScope();
        }
      }
    }

    // Press and release one by one Ctrl, Alt and Ctrl again to show statistic dialog.
    private final static int statDialogKeyCodeSequence[] = { KeyCodes.KEY_CTRL, KeyCodes.KEY_ALT, KeyCodes.KEY_CTRL };

    private static boolean checkStatDialogCondition(int state, int keyCode, boolean down) {
      return statDialogKeyCodeSequence[state/2] == keyCode && ((state & 1) == 0) == down;
    }

    private boolean checkStatDialogCondition(KeyEvent<?> event) {
      boolean down = event instanceof KeyDownEvent;
      if (checkStatDialogCondition(statDialogCondition, event.getNativeEvent().getKeyCode(), down)) {
        if (statDialogCondition/2 == statDialogKeyCodeSequence.length-1) {
          statDialogCondition = 0;
          return true;
        }
        statDialogCondition++;
      } else if (statDialogCondition != 0) {
        statDialogCondition = checkStatDialogCondition(0, event.getNativeEvent().getKeyCode(), down) ? 1 : 0;
      }
      return false;
    }
  }
}
