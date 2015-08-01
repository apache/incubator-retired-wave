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

import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.model.document.util.Point;


/**
 * Higher-level handler for editor events.
 *
 * An application can implement this interface for custom handling of behaviour.
 *
 */
public interface EditorEventsSubHandler {
  /**
   * Handler for commands that affect the current block
   */
  public boolean handleBlockLevelCommands(EditorEvent event, ContentRange selection);

  /**
   * Handler for key combos on a range selection
   * @param selection
   */
  public boolean handleRangeKeyCombo(EditorEvent event, ContentRange selection);

  /**
   * Handler for key combos on a collapsed selection
   */
  boolean handleCollapsedKeyCombo(EditorEvent event, Point<ContentNode> caret);

  /**
   * Handler for selection-independent commands
   */
  boolean handleCommand(EditorEvent event);

  /**
   * Handler for dom mutation events
   * @param event
   */
  void handleDomMutation(SignalEvent event);

  /**
   * Handle cut event.
   * @param event
   */
  boolean handleCut(EditorEvent event);

  /**
   * Handle paste event.
   * @param event
   */
  boolean handlePaste(EditorEvent event);

  /**
   * Handle copy event
   * @param event
   */
  boolean handleCopy(EditorEvent event);
}
