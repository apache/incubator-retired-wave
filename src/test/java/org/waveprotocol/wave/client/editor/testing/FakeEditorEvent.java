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

package org.waveprotocol.wave.client.editor.testing;

import org.waveprotocol.wave.client.common.util.FakeSignalEvent;
import org.waveprotocol.wave.client.editor.constants.BrowserEvents;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.event.EditorEvent;

/**
 * Use this class to mock events for EditorImpl methods
 *
 */
public class FakeEditorEvent extends FakeSignalEvent implements EditorEvent {

  public static SignalEventFactory<FakeEditorEvent> ED_FACTORY =
    new SignalEventFactory<FakeEditorEvent>() {
      @Override public FakeEditorEvent create() {
        return new FakeEditorEvent();
      }
    };

  /**
   * @param type
   * @return a fake event of the given type
   */
  public static FakeEditorEvent create(String type) {
    return FakeSignalEvent.createEvent(ED_FACTORY, type);
  }


  /**
   * Construct from a KeySignalType and a key code
   */
  public static FakeEditorEvent create(KeySignalType type, int keyCode) {
    return FakeSignalEvent.createKeyPress(ED_FACTORY, type, keyCode, null);
  }

  /**
   * @return A fake paste event
   */
  public static FakeEditorEvent createPasteEvent() {
    return create("paste");
  }

  /**
   * Creates a composition start, some composition updates, and a composition end
   *
   * @param numUpdates
   * @return the events in order
   */
  public static FakeEditorEvent[] compositionSequence(int numUpdates) {
    FakeEditorEvent[] evts = new FakeEditorEvent[numUpdates + 2];

    evts[0] = create(BrowserEvents.COMPOSITIONSTART);
    for (int i = 1; i <= numUpdates; i++) {
      evts[i] = create(BrowserEvents.COMPOSITIONUPDATE);
    }
    evts[numUpdates + 1] = create(BrowserEvents.COMPOSITIONEND);
    return evts;
  }

  private boolean shouldAllowDefault = false;
  private ContentPoint caret;

  @Override
  public void allowBrowserDefault() {
    shouldAllowDefault = true;
  }


  @Override
  public ContentPoint getCaret() {
    return caret;
  }

  @Override
  public void setCaret(ContentPoint caret) {
    this.caret = caret;
  }

  @Override
  public boolean shouldAllowBrowserDefault() {
    return shouldAllowDefault;
  }

}
