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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.waveprotocol.wave.client.common.util.UserAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps GWT's events class to give prettier access to event properties.
 *
 * NOTE(user): we expect a future version of GWT to do this directly on
 * their Event class, at which point we can probably deprecate this
 * wrapper
 *
 */
@SuppressWarnings({ "serial" })
// TODO(danilatos,user): Remove this class, since a lot of this functionality
//     is exposed by GWT's new event system (i.e. NativeEvent).
public class EventWrapper {

  /**
   * The event we are wrapping
   */
  private final Event event;

  /**
   * Modifier values, greater than 16 bits, to add to the keypresses
   * to distinguish them. First shift
   */
  private final static int SHIFT = 1 << 17;

  /**
   * Alt
   */
  private final static int ALT = 1 << 18;

  /**
   * Ctrl
   */
  private final static int CTRL = 1 << 19;

  /**
   * Meta
   */
  private final static int META = 1 << 20;


  /** (Not defined in GWT's KeyCodes) */
  public final static int KEY_INSERT = 45;

  /**
   * Map of keypresses and modifiers to our KeyCombo enum values.
   * http://www.quirksmode.org/js/keys.html has some useful info about this.
   * TODO(danilatos): Implement an IntMap JSO and use that
   */
  private final static Map<Integer, KeyCombo> keyMap =
    new HashMap<Integer, KeyCombo>() {
    {
      // Tab
      put(9,            KeyCombo.TAB);
      put(9 + SHIFT,    KeyCombo.SHIFT_TAB);

      // Space bar
      put(32,                KeyCombo.SPACE);
      put(32 + SHIFT,        KeyCombo.SHIFT_SPACE);
      put(32 + CTRL,         KeyCombo.CTRL_SPACE);
      put(32 + SHIFT + CTRL, KeyCombo.CTRL_SHIFT_SPACE);

      // Escape
      put(KeyCodes.KEY_ESCAPE, KeyCombo.ESC);

      // Enter
      put(KeyCodes.KEY_ENTER, KeyCombo.ENTER);
      put(KeyCodes.KEY_ENTER + SHIFT, KeyCombo.SHIFT_ENTER);
      put(KeyCodes.KEY_ENTER + CTRL, KeyCombo.CTRL_ENTER);

      // Backspace
      put(KeyCodes.KEY_BACKSPACE, KeyCombo.BACKSPACE);
      put(KeyCodes.KEY_BACKSPACE + SHIFT, KeyCombo.SHIFT_BACKSPACE);

      // Delete
      put(KeyCodes.KEY_DELETE, KeyCombo.DELETE);
      put(KeyCodes.KEY_DELETE + SHIFT, KeyCombo.SHIFT_DELETE);

      // Insert
      put(KEY_INSERT, KeyCombo.INSERT);
      put(KEY_INSERT + SHIFT, KeyCombo.SHIFT_INSERT);
      put(KEY_INSERT + CTRL, KeyCombo.CTRL_INSERT);

      // Ctrl-equals
      put('=' + CTRL, KeyCombo.CTRL_EQUALS);

      // Ctrl-alpha combos
      put('B' + CTRL, KeyCombo.CTRL_B);
      put('D' + CTRL, KeyCombo.CTRL_D);
      put('F' + CTRL, KeyCombo.CTRL_F);
      put('I' + CTRL, KeyCombo.CTRL_I);
      put('G' + CTRL, KeyCombo.CTRL_G);
      put('H' + CTRL, KeyCombo.CTRL_H);
      put('K' + CTRL, KeyCombo.CTRL_K);
      put('O' + CTRL, KeyCombo.CTRL_O);
      put('U' + CTRL, KeyCombo.CTRL_U);
      put('W' + CTRL, KeyCombo.CTRL_W);
      put('A' + CTRL, KeyCombo.CTRL_A);
      put('R' + CTRL, KeyCombo.CTRL_R);
      put('E' + CTRL, KeyCombo.CTRL_E);
      put('L' + CTRL, KeyCombo.CTRL_L);

      put('L' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_L);

      put('F' + CTRL + ALT, KeyCombo.CTRL_ALT_F);
      put('D' + CTRL + ALT, KeyCombo.CTRL_ALT_D);
      put('G' + CTRL + ALT, KeyCombo.CTRL_ALT_G);
      put('S' + CTRL + ALT, KeyCombo.CTRL_ALT_S);

      // Canned responses
      put('!' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_1);
      put('1' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_1);
      put('@' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_2);
      put('2' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_2);
      put('#' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_3);
      put('3' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_3);
      put('%' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_5);
      put('5' + CTRL + SHIFT, KeyCombo.CTRL_SHIFT_5);

      // Allowable ORDER combos
      // ORDER is the Apple key on OS X and the Ctrl key in Windows and Linux.
      // It is the key modifier used to perform actions such as copy/paste,
      // save, print etc.
      // XXX(user): These override the CTRL/META combos! Fix it!
      {
        final int orderKey = UserAgent.isMac() ? META : CTRL;
        put('A' + orderKey, KeyCombo.ORDER_A);
        put('B' + orderKey, KeyCombo.ORDER_B);
        put('C' + orderKey, KeyCombo.ORDER_C);
        put('D' + orderKey, KeyCombo.ORDER_D);
        put('F' + orderKey, KeyCombo.ORDER_F);
        put('G' + orderKey, KeyCombo.ORDER_G);
        put('I' + orderKey, KeyCombo.ORDER_I);
        put('K' + orderKey, KeyCombo.ORDER_K);
        put('L' + orderKey, KeyCombo.ORDER_L);
        put('N' + orderKey, KeyCombo.ORDER_N);
        put('O' + orderKey, KeyCombo.ORDER_O);
        put('P' + orderKey, KeyCombo.ORDER_P);
        put('R' + orderKey, KeyCombo.ORDER_R);
        put('T' + orderKey, KeyCombo.ORDER_T);
        put('U' + orderKey, KeyCombo.ORDER_U);
        put('V' + orderKey, KeyCombo.ORDER_V);
        put('W' + orderKey, KeyCombo.ORDER_W);
        put('X' + orderKey, KeyCombo.ORDER_X);
        put('Z' + orderKey, KeyCombo.ORDER_Z);
        put('Q' + orderKey, KeyCombo.ORDER_Q);
        put('R' + orderKey + SHIFT, KeyCombo.ORDER_SHIFT_R);
        put('V' + orderKey + SHIFT, KeyCombo.ORDER_SHIFT_V);
        put('K' + orderKey + SHIFT, KeyCombo.ORDER_SHIFT_K);
        put('5' + orderKey + SHIFT, KeyCombo.ORDER_SHIFT_5);

        // Plaintext paste in Safari
        put('V' | orderKey | ALT | SHIFT, KeyCombo.ORDER_ALT_SHIFT_V);
      }

      // Key navigation
      put(KeyCodes.KEY_DOWN,        KeyCombo.DOWN);
      put(KeyCodes.KEY_UP,          KeyCombo.UP);
      put(KeyCodes.KEY_LEFT,        KeyCombo.LEFT);
      put(KeyCodes.KEY_RIGHT,       KeyCombo.RIGHT);
      put(KeyCodes.KEY_PAGEUP,      KeyCombo.PAGE_UP);
      put(KeyCodes.KEY_PAGEDOWN,    KeyCombo.PAGE_DOWN);
      put(KeyCodes.KEY_HOME,        KeyCombo.HOME);
      put(KeyCodes.KEY_END,         KeyCombo.END);

      // Safari key navigation
      // TODO(user): consider using deferred binding
      put(63233, KeyCombo.DOWN);
      put(63232, KeyCombo.UP);
      put(63234, KeyCombo.LEFT);
      put(63235, KeyCombo.RIGHT);
      put(63276, KeyCombo.PAGE_UP);
      put(63277, KeyCombo.PAGE_DOWN);
      put(63273, KeyCombo.HOME);
      put(63275, KeyCombo.END);

      // Meta combos
      put(KeyCodes.KEY_LEFT + META, KeyCombo.META_LEFT);
      put(KeyCodes.KEY_RIGHT + META, KeyCombo.META_RIGHT);
      put(KeyCodes.KEY_HOME + META, KeyCombo.META_HOME);
    }
  };

  /**
   * @return true is even is a submit
   */
  public boolean isSubmit() {
    return event != null && isKeyEvent() && getKeyCombo() == KeyCombo.SHIFT_ENTER;
  }

  /**
   * Constructor
   *
   * @param event
   */
  public EventWrapper(Event event) {
    this.event = event;
  }

  /**
   * @return True if alt key was pressed
   */
  public boolean getAltKey() {
    return DOM.eventGetAltKey(event);
  }

  /**
   * @return The mouse buttons that were depressed as a bit-field, defined
   *    by {@link Event#BUTTON_LEFT}, {@link Event#BUTTON_MIDDLE}, and
   *    {@link Event#BUTTON_RIGHT}
   */
  public int getButton() {
    return DOM.eventGetButton(event);
  }

  /**
   * @return {@link #getKeyCode()} cast to char
   */
  public char getCharCode() {
    return (char) getKeyCode();
  }

  /**
   * @return The mouse x-position within the browser window's client area.
   */
  public int getClientX() {
    return DOM.eventGetClientX(event);
  }

  /**
   * @return The mouse y-position within the browser window's client area.
   */
  public int getClientY() {
    return DOM.eventGetClientY(event);
  }

  /**
   * @return True if ctrl key was pressed
   */
  public boolean getCtrlKey() {
    return DOM.eventGetCtrlKey(event);
  }

  /**
   * @return The current event that is being fired. The current event is only
   * available within the lifetime of the onBrowserEvent function. Once the
   * onBrowserEvent method returns, the current event is reset to null.
   */
  public static EventWrapper getCurrentEvent() {
    Event current = DOM.eventGetCurrentEvent();
    return current != null ?
        new EventWrapper(DOM.eventGetCurrentEvent()) : null;
  }

  /**
   * @return The event's current target element. This is the element
   * whose listener fired last, not the element which fired the event
   * initially.
   */
  public Element getCurrentTarget() {
    return DOM.eventGetCurrentTarget(event);
  }

  /**
   * @return The event
   */
  public Event getEvent() {
    return event;
  }

  /**
   * @return The element from which the mouse pointer was moved
   *    (only valid for {@link Event#ONMOUSEOVER}).
   */
  public static Element getFromElement(Event event) {
    return DOM.eventGetFromElement(event);
  }

  /**
   * @return The key code associated with this event. For
   * {@link Event#ONKEYPRESS}, the Unicode value of the character generated.
   * For {@link Event#ONKEYDOWN} and {@link Event#ONKEYUP}, the code
   * associated with the physical key.
   */
  public int getKeyCode() {
    return getKeyCode(event);
  }

  /**
   * Wrapper for GWT's get[Key Char]Code() that conflates the two values. If
   * there is no keyCode present, it returns charCode instead. This matches the
   * values in {{@link #keyMap} above.
   */
  public static int getKeyCode(Event evt) {
    int keyCode = evt.getKeyCode();
    if (keyCode == 0) {
      keyCode = evt.getCharCode();
    }
    return keyCode;
  }

  /**
   * Semi-deprecated (will be deprecated once event signal has a mechanism for
   * fast switching).
   * @return An encoding of the event's keycode and modifiers
   */
  public KeyCombo getKeyCombo() {
    return getKeyCombo(getKeyCode(), getCtrlKey(), getShiftKey(), getAltKey(), getMetaKey());
  }

  public static KeyCombo getKeyCombo(SignalEvent signal) {
    return getKeyCombo(signal.getKeyCode(), signal.getCtrlKey(),
        signal.getShiftKey(), signal.getAltKey(), signal.getMetaKey());
  }

  /**
   * @return the key-combo representation of the key event.
   */
  public static KeyCombo getKeyCombo(Event evt) {
    return getKeyCombo(getKeyCode(evt), DOM.eventGetCtrlKey(evt),
        DOM.eventGetShiftKey(evt), DOM.eventGetAltKey(evt), DOM.eventGetMetaKey(evt));
  }

  private static KeyCombo getKeyCombo(int keyCode, boolean ctrl, boolean shift, boolean alt,
      boolean meta) {
    int gwtCode = keyCode;
    gwtCode +=
          (ctrl ? CTRL : 0)
        + (shift ? SHIFT : 0)
        + (alt ? ALT : 0) +
        + (meta ? META : 0);
    if (!keyMap.containsKey(gwtCode) && keyCode >= 'a' && keyCode <= 'z') {
      // HACK(danilatos): make it work cross-browser with the event signal updates.
      // get rid of this class soon.
      gwtCode += 'A' - 'a';
    }
    return keyMap.containsKey(gwtCode) ? keyMap.get(gwtCode) : KeyCombo.OTHER;
  }

  /**
   * Converts the parameters to {@link KeyCodes} events to a KeyCombo.
   *
   * @return the key-combo representation of the key event.
   */
  public static KeyCombo getKeyCombo(char keyCode, int modifiers) {
    int gwtCode = keyCode
        + (((modifiers & KeyCodes.KEY_CTRL) != 0) ? CTRL : 0)
        + (((modifiers & KeyCodes.KEY_SHIFT) != 0) ? SHIFT : 0)
        + (((modifiers & KeyCodes.KEY_ALT) != 0) ? ALT : 0);
    return keyMap.containsKey(gwtCode) ? keyMap.get(gwtCode) : KeyCombo.OTHER;
  }

  /**
   * @return True if the metakey was pressed
   */
  public boolean getMetaKey() {
    return DOM.eventGetMetaKey(event);
  }

  /**
   * @return The velocity of the mouse wheel associated with the event
   * along the Y axis.
   */
  public static int getMouseWheelVelocityY(Event event) {
    return DOM.eventGetMouseWheelVelocityY(event);
  }

  /**
   * @return True if this was an auto-repeat event
   */
  public boolean getRepeat() {
    return DOM.eventGetRepeat(event);
  }

  /**
   * @return The mouse x-position on the user's display
   */
  public int getScreenX() {
    return DOM.eventGetScreenX(event);
  }

  /**
   * @return The mouse y-position on the user's display
   */
  public int getScreenY() {
    return DOM.eventGetScreenY(event);
  }

  /**
   * @return True if the shift key was pressed
   */
  public boolean getShiftKey() {
    return DOM.eventGetShiftKey(event);
  }

  /**
   * @return The element that was the actual target of the event.
   */
  public Element getTarget() {
    return DOM.eventGetTarget(event);
  }

  /**
   * @return The element to which the mouse pointer was moved
   * (only valid for {@link Event#ONMOUSEOUT}).
   */
  public static Element getToElement(Event event) {
    return DOM.eventGetToElement(event);
  }

  /**
   * @return The event's type
   */
  public int getType() {
    return DOM.eventGetType(event);
  }

  /**
   * @return true if event type is a focus event
   */
  public boolean isFocusEvent() {
    return (getType() & Event.FOCUSEVENTS) != 0;
  }

  /**
   * @return true if event type is a key event
   */
  public boolean isKeyEvent() {
    return (getType() & Event.KEYEVENTS) != 0;
  }

  /**
   * @return true if event type is a mouse event
   */
  public boolean isMouseEvent() {
    return (getType() & Event.MOUSEEVENTS) != 0;
  }

  /**
   * @return The event's type string
   */
  public String getTypeString() {
    return DOM.eventGetTypeString(event);
  }

  /**
   * Prevents default for current event
   */
  public static void preventCurrentEventDefault() {
    getCurrentEvent().event.preventDefault();
  }

  /**
   * @return A string describing which modifier keys were pressed,
   *        and whether this was a repeat event,  e.g., " shift ctrl"
   */
  @SuppressWarnings("deprecation")
  public static String modifiers(Event event) {
    // repeat is deprecated, but useful for debugging
    return (event.getAltKey() ? " alt" : "")
    + (event.getShiftKey() ? " shift" : "")
    + (event.getCtrlKey() ? " ctrl" : "")
    + (event.getMetaKey() ? " meta" : "")
    + ((event.getTypeInt() == Event.ONKEYDOWN) && event.getRepeat() ? " repeat" : "");
  }

  /**
   * @return A string describing which mouse buttons were pressed,
   *        e.g., " left"
   */
  private static String mouseButtons(Event event) {
    if (event.getButton() == -1) {
      return "";
    } else {
      return ((event.getButton() & Event.BUTTON_LEFT) != 0 ? " left" : "")
          + ((event.getButton() & Event.BUTTON_MIDDLE) != 0 ? " middle" : "")
          + ((event.getButton() & Event.BUTTON_RIGHT) != 0 ? " right" : "");
    }
  }

  /**
   * @return String describing the event's key code, e.g.,
   *    " 64 'a'"
   */
  private static String key(Event event) {
    return " " + event.getKeyCode() + " '" + (char) event.getKeyCode() + "'";
  }

  /**
   * @return A string describing the client x,y position,
   *        e.g., " (100, 100)"
   */
  private static String mousePoint(Event event) {
    return " (" + event.getClientX() + ", " + event.getClientY() + ")";
  }

  @Override
  public String toString() {
    return asString(event);
  }

  public static String asString(Event event) {
    // Start with the event type string
    String string = DOM.eventGetTypeString(event);
    // Next type-specific fields
    switch (event.getTypeInt()) {
    case Event.ONKEYPRESS:
    case Event.ONKEYUP:
    case Event.ONKEYDOWN:
      string += key(event) + modifiers(event);
      break;
    case Event.ONCLICK:
    case Event.ONDBLCLICK:
    case Event.ONMOUSEMOVE:
      string += mousePoint(event) + modifiers(event);
      break;
    case Event.ONMOUSEDOWN:
    case Event.ONMOUSEUP:
      string += mousePoint(event) + mouseButtons(event) + modifiers(event);
      break;
    case Event.ONMOUSEOUT:
      string += mousePoint(event) + modifiers(event) + " to: " + getToElement(event);
      break;
    case Event.ONMOUSEOVER:
      string += mousePoint(event) + modifiers(event) + " from: " + getFromElement(event);
      break;
    case Event.ONMOUSEWHEEL:
      string += " " + getMouseWheelVelocityY(event) + mousePoint(event) + modifiers(event);
      break;
    case Event.ONFOCUS:
    case Event.ONBLUR:
    case Event.ONCHANGE:
    case Event.ONERROR:
    case Event.ONLOAD:
    case Event.ONLOSECAPTURE:
    case Event.ONSCROLL:
      break;
    }
    return string;
  }

  /**
   * In safari, there is X velocity and Y velocity.  GWT code return the combination of the 2.
   * This code only return the Y velocity.
   * @return The y velocity of the mouse event and only the y velocity.
   */
  public static int getMouseWheelVelocityYOnly(Event event) {
    if (!UserAgent.isSafari()) {
      return event.getMouseWheelVelocityY();
    } else {
      return nativeGetMouseWheelVelocityYOnly(event);
    }
  }

  private static native int nativeGetMouseWheelVelocityYOnly(Event evt) /*-{
    // wheelDeltaY is not standard and only available in newer safari
    if (evt.wheelDeltaY == undefined) {
      // The following line is copied from DOMImplSafari.getMouseWheelVelocityY
      return Math.round(-evt.wheelDelta / 40) || 0;
    } else {
      return Math.round(-evt.wheelDeltaY / 40) || 0;
    }
  }-*/;
}
