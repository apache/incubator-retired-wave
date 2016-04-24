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

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Instances of this class encapsulate the event to signal mapping logic for a
 * specific environment (os/browser).
 *
 * Contains as much of the signal event logic as possible in a POJO testable
 * manner.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class SignalKeyLogic {

  /**
   * For webkit + IE
   * I think also all browsers on windows?
   */
  public static final int IME_CODE = 229;

  private static final String DELETE_KEY_IDENTIFIER = "U+007F";

  //TODO(danilatos): Use int map
  private static final Set<Integer> NAVIGATION_KEYS = new HashSet<Integer>();
  private static final StringMap<Integer> NAVIGATION_KEY_IDENTIFIERS =
      CollectionUtils.createStringMap();
  static {

    NAVIGATION_KEY_IDENTIFIERS.put("Left", KeyCodes.KEY_LEFT);
    NAVIGATION_KEY_IDENTIFIERS.put("Right", KeyCodes.KEY_RIGHT);
    NAVIGATION_KEY_IDENTIFIERS.put("Up", KeyCodes.KEY_UP);
    NAVIGATION_KEY_IDENTIFIERS.put("Down", KeyCodes.KEY_DOWN);
    NAVIGATION_KEY_IDENTIFIERS.put("PageUp", KeyCodes.KEY_PAGEUP);
    NAVIGATION_KEY_IDENTIFIERS.put("PageDown", KeyCodes.KEY_PAGEDOWN);
    NAVIGATION_KEY_IDENTIFIERS.put("Home", KeyCodes.KEY_HOME);
    NAVIGATION_KEY_IDENTIFIERS.put("End", KeyCodes.KEY_END);

    NAVIGATION_KEY_IDENTIFIERS.each(new ProcV<Integer>() {
      public void apply(String key, Integer keyCode) {
        NAVIGATION_KEYS.add(keyCode);
      }
    });
  }

  public enum UserAgentType {
    WEBKIT,
    GECKO,
    IE
  }

  public enum OperatingSystem {
    WINDOWS,
    MAC,
    LINUX
  }

  @VisibleForTesting
  public static class Result {
    @VisibleForTesting
    public int keyCode;
    // Sentinal by default for testing purposes
    @VisibleForTesting
    public KeySignalType type = KeySignalType.SENTINAL;
  }

  private final UserAgentType userAgent;
  private final boolean commandIsCtrl;


  // Hack, get rid of this
  final boolean commandComboDoesntGiveKeypress;

  /**
   * @param userAgent
   * @param os Operating system
   */
  public SignalKeyLogic(UserAgentType userAgent, OperatingSystem os,
      boolean commandComboDoesntGiveKeypress) {
    this.userAgent = userAgent;
    this.commandComboDoesntGiveKeypress = commandComboDoesntGiveKeypress;
    commandIsCtrl = os != OperatingSystem.MAC;
  }

  public boolean commandIsCtrl() {
    return commandIsCtrl;
  }

  public void computeKeySignalType(
      Result result,
      String typeName,
      int keyCode, int which, String keyIdentifier,
      boolean metaKey, boolean ctrlKey, boolean altKey, boolean shiftKey) {

    boolean ret = true;

    int typeInt;
    if ("keydown".equals(typeName)) {
      typeInt = Event.ONKEYDOWN;
    } else if ("keypress".equals(typeName)) {
      typeInt = Event.ONKEYPRESS;
    } else if ("keyup".equals(typeName)) {
      result.type = null;
      return;
    } else {
      throw new AssertionError("Non-key-event passed to computeKeySignalType");
    }

    KeySignalType type;

    int computedKeyCode = which != 0 ? which : keyCode;

    if (computedKeyCode == 10) {
      computedKeyCode = KeyCodes.KEY_ENTER;
    }

    // For non-firefox browsers, we only get keydown events for IME, no keypress
    boolean isIME = computedKeyCode == IME_CODE;

    boolean commandKey = commandIsCtrl ? ctrlKey : metaKey;

    switch (userAgent) {
      case WEBKIT:
        // This is a bit tricky because there are significant differences
        // between safari 3.0 and safari 3.1...

        // We could probably actually almost use the same code that we use for IE
        // for safari 3.1, because with 3.1 the webkit folks made a big shift to
        // get the events to be in line with IE for compatibility. 3.0 events
        // are a lot more similar to FF, but different enough to need special
        // handling. However, it seems that using more advanced features like
        // keyIdentifier for safaris is probably better and more future-proof,
        // as well as being compatible between the two, so for now we're not
        // using IE logic for safari 3.1

        // Weird special large keycode numbers for safari 3.0, where it gives
        // us keypress events (though they happen after the dom is changed,
        // for some things like delete. So not too useful). The number
        // 63200 is known as the cutoff mark.
        if (typeInt == Event.ONKEYDOWN && computedKeyCode > 63200) {
          result.type = null;
          return;
        } else if (typeInt == Event.ONKEYPRESS) {
          // Skip keypress for tab and escape, because they are the only non-input keys
          // that don't have keycodes above 63200. This is to prevent them from being treated
          // as INPUT in the || = keypress below. See (X) below
          if (computedKeyCode == KeyCodes.KEY_ESCAPE
              || computedKeyCode == KeyCodes.KEY_TAB) {
            result.type = null;
            return;
          }
        }

        // boolean isPossiblyCtrlInput = typeInt == Event.ONKEYDOWN && ret.getCtrlKey();
        boolean isActuallyCtrlInput = false;

        boolean startsWithUPlus = keyIdentifier != null && keyIdentifier.startsWith("U+");

        // Need to use identifier for the delete key because the keycode conflicts
        // with the keycode for the full stop.
        if (isIME) {
          // If is IME, override the logic below - we get keyIdentifiers for IME events,
          // but those are basically useless as the event is basically still an IME input
          // event (e.g. keyIdentifier might say "Up", but it's certainly not navigation,
          // it's just the user selecting from the IME dialog).
          type = KeySignalType.INPUT;
        } else if (DELETE_KEY_IDENTIFIER.equals(keyIdentifier) ||
            computedKeyCode == KeyCodes.KEY_BACKSPACE) {

          type = KeySignalType.DELETE;
        } else if (NAVIGATION_KEY_IDENTIFIERS.containsKey(keyIdentifier)) {
          type = KeySignalType.NAVIGATION;
        // Escape, backspace and context-menu-key (U+0010) are, to my knowledge,
        // the only non-navigation keys that
        // have a "U+..." keyIdentifier, so we handle them explicitly.
        // (Backspace was handled earlier).
        } else if (computedKeyCode == KeyCodes.KEY_ESCAPE || "U+0010".equals(keyIdentifier)) {
          type = KeySignalType.NOEFFECT;
        } else if (
            computedKeyCode < 63200 && // if it's not a safari 3.0 non-input key (See (X) above)
            (typeInt == Event.ONKEYPRESS ||  // if it's a regular keypress
                  startsWithUPlus || computedKeyCode == KeyCodes.KEY_ENTER)) {
          type = KeySignalType.INPUT;
          isActuallyCtrlInput = ctrlKey
              || (commandComboDoesntGiveKeypress && commandKey);
        } else {
          type = KeySignalType.NOEFFECT;
        }

        // Maybe nullify it with the same logic as IE, EXCEPT for the special
        // Ctrl Input webkit behaviour, and IME for windows
        if (isActuallyCtrlInput) {
          if (computedKeyCode == KeyCodes.KEY_ENTER) {
            ret = typeInt == Event.ONKEYDOWN;
          }
          // HACK(danilatos): Don't actually nullify isActuallyCtrlInput for key press.
          // We get that for AltGr combos on non-mac computers.
        } else if (isIME || keyCode == KeyCodes.KEY_TAB) {
          ret = typeInt == Event.ONKEYDOWN;
        } else {
          ret = maybeNullWebkitIE(ret, typeInt, type);
        }
        if (!ret) {
          result.type = null;
          return;
        }
        break;
      case GECKO:
        boolean hasKeyCodeButNotWhich = keyCode != 0 && which == 0;

        // Firefox is easy for deciding signal events, because it issues a keypress for
        // whenever we would want a signal. So we can basically ignore all keydown events.
        // It also, on all OSes, does any default action AFTER the keypress (even for
        // things like Ctrl/Meta+C, etc). So keypress is perfect for us.
        // Ctrl+Space is an exception, where we don't get a keypress
        // Firefox also gives us keypress events even for Windows IME input
        if (ctrlKey && !altKey && !shiftKey && computedKeyCode == ' ') {
          if (typeInt != Event.ONKEYDOWN) {
            result.type = null;
            return;
          }
        } else if (typeInt == Event.ONKEYDOWN) {
          result.type = null;
          return;
        }

        // Backspace fails the !hasKeyCodeButNotWhich test, so check it explicitly first
        if (computedKeyCode == KeyCodes.KEY_BACKSPACE) {
          type = KeySignalType.DELETE;
        // This 'keyCode' but not 'which' works very nicely for catching normal typing input keys,
        // the only 'exceptions' I've seen so far are bksp & enter which have both
        } else if (!hasKeyCodeButNotWhich || computedKeyCode == KeyCodes.KEY_ENTER
            || computedKeyCode == KeyCodes.KEY_TAB) {
          type = KeySignalType.INPUT;
        } else if (computedKeyCode == KeyCodes.KEY_DELETE) {
          type = KeySignalType.DELETE;
        } else if (NAVIGATION_KEYS.contains(computedKeyCode)) {
          type = KeySignalType.NAVIGATION;
        } else {
          type = KeySignalType.NOEFFECT;
        }

        break;
      case IE:

        // Unfortunately IE gives us the least information, so there are no nifty tricks.
        // So we pretty much need to use some educated guessing based on key codes.
        // Experimentation page to the rescue.

        boolean isKeydownForInputKey = isInputKeyCodeIE(computedKeyCode);

        // IE has some strange behaviour with modifiers and whether or not there will
        // be a keypress. Ctrl kills the keypress, unless shift is also held.
        // Meta doesn't kill it. Alt always kills the keypress, overriding other rules.
        boolean hasModifiersThatResultInNoKeyPress =
          altKey || (ctrlKey && !shiftKey);

        if (typeInt == Event.ONKEYDOWN) {
          if (isKeydownForInputKey) {
            type = KeySignalType.INPUT;
          } else if (computedKeyCode == KeyCodes.KEY_BACKSPACE ||
              computedKeyCode == KeyCodes.KEY_DELETE) {
            type = KeySignalType.DELETE;
          } else if (NAVIGATION_KEYS.contains(computedKeyCode)) {
            type = KeySignalType.NAVIGATION;
          } else {
            type = KeySignalType.NOEFFECT;
          }
        } else {
          // Escape is the only non-input thing that has a keypress event
          if (computedKeyCode == KeyCodes.KEY_ESCAPE) {
            result.type = null;
            return;
          }
          assert typeInt == Event.ONKEYPRESS;
          // I think the guessCommandFromModifiers() check here isn't needed,
          // but i feel safer putting it in.
          type = KeySignalType.INPUT;
        }

        if (hasModifiersThatResultInNoKeyPress || isIME || computedKeyCode == KeyCodes.KEY_TAB) {
          ret = typeInt == Event.ONKEYDOWN ? ret : false;
        } else {
          ret = maybeNullWebkitIE(ret, typeInt, type);
        }
        if (!ret) {
          result.type = null;
          return;
        }
        break;
      default:
        throw new UnsupportedOperationException("Unhandled user agent");
    }

    if (ret) {
      result.type = type;
      result.keyCode = computedKeyCode;
    } else {
      result.type = null;
      return;
    }
  }

  private static final boolean isInputKeyCodeIE(int keyCode) {
    /*
    DATA
    ----
    For KEYDOWN:

    "Input"
    48-57 (numbers)
    65-90 (a-z)
    96-111 (Numpad digits & other keys, with numlock off. with numlock on, they
      behave like their corresponding keys on the rest of the keyboard)
    186-192 219-222 (random non-alphanumeric next to letters on RHS + backtick)
    229 Code that the input has passed to an IME

    Non-"input"
    < 48 ('0')
    91-93 (Left & Right Win keys, ContextMenu key)
    112-123 (F1-F12)
    144-5 (NUMLOCK,SCROLL LOCK)

    For KEYPRESS: only "input" things get this event! yay! not even backspace!
    Well, one exception: ESCAPE
    */
    // boundaries in keycode ranges where the keycode for a keydown is for an input
    // key. at "ON" it is, starting from the number going up, and the opposite for "OFF".
    final int A_ON = 48;
    final int B_OFF = 91;
    final int C_ON = 96;
    final int D_OFF = 112;
    final int E_ON = 186;

    return
      (keyCode == 9 || keyCode == 32 || keyCode == 13) || // And tab, enter & spacebar, of course!
      (keyCode >= A_ON && keyCode < B_OFF) ||
      (keyCode >= C_ON && keyCode < D_OFF) ||
      (keyCode >= E_ON);
  }

  /**
   * Common logic between Webkit and IE for deciding whether we want the keydown
   * or the keypress
   */
  private static boolean maybeNullWebkitIE(boolean ret, int typeInt,
      KeySignalType type) {
    // Use keydown as the signal for everything except input.
    // This is because the mutation always happens after the keypress for
    // input (this is especially important for chrome,
    // which interleaves deferred commands between keydown and keypress).
    //
    // For everything else, keypress is redundant with keydown, and also, the resulting default
    // dom mutation (if any) often happens after the keydown but before the keypress in webkit.
    // Also, if the 'Command' key is held for chrome/safari etc, we want to get the keydown
    // event, NOT the keypress event, for everything because of things like ctrl+c etc.
    // where sometimes it'll happen just after the keydown, or sometimes we just won't
    // get a keypress at all
    if (typeInt == (type == KeySignalType.INPUT ? Event.ONKEYDOWN : Event.ONKEYPRESS)) {
      return false;
    }

    return ret;
  }
}
