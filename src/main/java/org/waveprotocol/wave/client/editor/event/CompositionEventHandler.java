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

package org.waveprotocol.wave.client.editor.event;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.editor.constants.BrowserEvents;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.common.logging.LoggerBundle;

/**
 * Logic dealing with the intricacies of composition events and
 * abstracting the differences between browsers
 *
 * @param <V> Event type pass through
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class CompositionEventHandler<V> {

  public interface CompositionListener<V> {
    void compositionStart(V event);
    void compositionUpdate();
    void compositionEnd();
  }

  private final LoggerBundle logger;
  private final boolean modifiesDomAndFiresTextInputAfterComposition;
  private final TimerService timer;
  private final CompositionListener<V> listener;

  /**
   * It might be worthwhile to increase this delay, because we get lots of 2nd
   * composition cycles on linux, so it might be an improvement to bunch them up.
   */
  final int compositionEndDelay = 0;

  /**
   * True if the "application" is composing (i.e. the state maintained by the
   * listener). Compare with {@link #browserComposing}.
   */
  private boolean appComposing = false;

  /**
   * True if we are in the timer delay after a textInput event (not after just a
   * compositionend event)
   *
   * See notes inside {@link #compositionStart(Object)} for details about why we
   * care.
   */
  @VisibleForTesting boolean delayAfterTextInput = false;

  /**
   * True if we are between a compositionstart...compositionend event sequence.
   * Not true if compositionend has already been received but we haven't notified
   * the listener of the compositionEnd. Therefore this state does not exactly
   * match state being maintained by the listener.
   *
   * Compare with {@link #appComposing}
   */
  private boolean browserComposing = false;

  private final Scheduler.Task endTask = new Scheduler.Task() {
    public void execute() {
      delayAfterTextInput = false;
      if (browserComposing) {
        return;
      }

      assert appComposing == true;
      appComposing = false;
      listener.compositionEnd();
    }
  };

  /**
   *
   * @param modifiesDomAndFiresTextinputAfterComposition use
   *        QuirksConstants.MODIFIES_DOM_AND_FIRES_TEXTINPUT_AFTER_COMPOSITION
   * @param timer
   * @param listener
   */
  public CompositionEventHandler(TimerService timer, CompositionListener<V> listener,
      LoggerBundle logger, boolean modifiesDomAndFiresTextinputAfterComposition) {
    this.timer = timer;
    this.listener = listener;
    this.logger = logger;
    this.modifiesDomAndFiresTextInputAfterComposition =
        modifiesDomAndFiresTextinputAfterComposition;
  }

  /**
   * This method must be called when any "composition" event is received. A composition
   * event is one of the following:
   * - compositionstart
   * - compositionupdate
   * - compositionend
   * - text
   * - textinput
   *
   * @param event Event object for pass through purposes
   * @param typeName Event name
   * @return true if the event should have its default prevented, false otherwise.
   */
  public boolean handleCompositionEvent(V event, String typeName) {
    if (BrowserEvents.COMPOSITIONSTART.equals(typeName)) {
      compositionStart(event);
    } else if (
        BrowserEvents.TEXT.equals(typeName) ||
        BrowserEvents.COMPOSITIONUPDATE.equals(typeName)) {
      compositionUpdate();
    } else if (BrowserEvents.COMPOSITIONEND.equals(typeName)){
      compositionEnd();
    } else if (BrowserEvents.TEXTINPUT.equals(typeName)) {
      textInput();
    } else {
      throw new AssertionError("unreachable");
    }

    return false;
  }

  /**
   * This method is to be called for all non-composition events.
   * Calling is optional, except under the following conditions:
   *
   * {@code modifiesDomAndFiresTextinputAfterComposition} is true AND
   * we are between calls to {@link CompositionListener#compositionStart(Object)}
   *
   * Otherwise it will always be a no-op.
   */
  public void handleOtherEvent() {
    checkAppComposing();

    // Flush if we are outside a composition start...end event sequence,
    // but we haven't yet had our delayed callback to notify the listener
    // of the end.
    if (modifiesDomAndFiresTextInputAfterComposition && !browserComposing) {
      flush();
    } else {
      // do nothing. adding branch for code coverage checking.
      return;
    }
  }

  private void compositionStart(V event) {
    if (browserComposing) {
      logger.error().log("CEH: State was already 'composing' during a compositionstart event!");

      return;
    }

    if (delayAfterTextInput) {
      // We don't want to hit the merge logic below - if we've had a text input, always
      // flush to ensure there's a corresponding compositionEnd, because the browser might
      // be moving on to the next composition phase. But if it's a composition start
      // straight after a composition end, then as of this writing it's safe to have them
      // merged and avoid redundant events.
      assert appComposing();
      flush();
    }

    delayAfterTextInput = false;
    browserComposing = true;

    if (modifiesDomAndFiresTextInputAfterComposition && timer.isScheduled(endTask)) {
      // Got a composition start before our timer fired - just pretend the
      // browser never left composition mode.
      timer.cancel(endTask);
      return;
    }

    assert appComposing == false;
    appComposing = true;
    listener.compositionStart(event);
  }

  private void compositionUpdate() {
    checkAppComposing();

    assert appComposing == true;
    listener.compositionUpdate();
  }

  private void compositionEnd() {
    delayAfterTextInput = false;
    checkAppComposing();

    if (!browserComposing) {
      logger.error().log("CEH: State was not 'composing' during a compositionend event!");
      return;
    }

    browserComposing = false;
    if (modifiesDomAndFiresTextInputAfterComposition) {
      // Browser is known to modify the dom one last time outside
      // a compositionend event - not safe to notify app immediately,
      // notify it later after the dom changes have finished.
      logger.trace().log("ce schedule");
      scheduleEndTask();
    } else {
      // Browser is known not to modify the dom one last time outside
      // a compositionend event - safe to notify app immediately.
      logger.trace().log("ce now");
      assert appComposing == true;
      appComposing = false;
      listener.compositionEnd();
    }
  }

  private void textInput() {
    checkAppComposing();

    if (modifiesDomAndFiresTextInputAfterComposition && appComposing() && !browserComposing) {
      delayAfterTextInput = true;
      scheduleEndTask();
    }
  }

  /** Use this instead of the boolean, to do the assert sanity check */
  private boolean appComposing() {
    checkAppComposing();
    return appComposing;
  }

  @VisibleForTesting void checkAppComposing() {
    // The appComposing variable should be equivalent to the expression on the right
    assert appComposing == (browserComposing || timer.isScheduled(endTask))
        : "appComposing variable does not match inferred state";
  }

  /**
   * End the composition sequence from the listener's perspective, if we have a
   * delayed end scheduled.
   */
  private void flush() {
    assert !browserComposing :
        "flush should not be called during native composition, because it is impossible to flush";
    checkAppComposing();

    if (appComposing()) {
      timer.cancel(endTask);
      endTask.execute();
    } else {
      // do nothing. adding branch for code coverage checking.
      return;
    }
  }

  private void scheduleEndTask() {
    timer.scheduleDelayed(endTask, compositionEndDelay);
  }
}
