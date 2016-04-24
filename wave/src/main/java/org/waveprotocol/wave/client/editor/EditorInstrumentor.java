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

package org.waveprotocol.wave.client.editor;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Instrumentation methods for a sink used by an editor to report out statistics to observers.
 * There should be no instances of this which are null - in those cases, use NOOP instead, so
 *   users are always safe to call methods without null checks.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public interface EditorInstrumentor {
  /** Types of actions that this counts. */
  public static enum Action {
    // counts for repairs
    FULL_REPAIR, // entire document repair
    PARTIAL_REPAIR, // partial document repair

    // keypress instrumentation
    SHORTCUT_BOLD,
    SHORTCUT_ITALIC,
    SHORTCUT_UNDERLINE,
    SHORTCUT_TABINDENT,
    SHORTCUT_TABOUTDENT,
    SHORTCUT_TABFIELDS,
    SHORTCUT_OPENNEARBYPOPUP,
    SHORTCUT_HEADINGSTYLE,
    SHORTCUT_ALIGNMENT,
    SHORTCUT_LINETHROUGH,

    // Copy & Paste,
    CLIPBOARD_COPY,
    CLIPBOARD_PASTE_FROM_WAVE,
    CLIPBOARD_PASTE_FROM_OUTSIDE,
    CLIPBOARD_CUT,

    // Undo/Redo
    UNDO,
    REDO,
  }

  /** Types of actions which include a duration that this records. */
  public static enum TimedAction {
    // timings for javascript input
    INPUT_PROCESS, // time to processing an INPUT or DELETE key event
    INPUT_POSTPROCESS, // time for layout/render cycles after the previous action
  }

  /** Indicate whether a particular event should be instrumented. */
  boolean shouldInstrument(SignalEvent e);

  /** Record that a particular action has taken place. */
  void record(Action type);

  /** Report a action's duration to the instrumentation sink. */
  void recordDuration(TimedAction type, double timeMs);

  /**
   * Simple implementation which does nothing at all.
   */
  public static final EditorInstrumentor NOOP = new EditorInstrumentor() {
    @Override
    public void record(Action type) { }
    @Override
    public void recordDuration(TimedAction type, double time) { }
    @Override
    public boolean shouldInstrument(SignalEvent e) { return false; }
  };
}
