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


package org.waveprotocol.wave.client.wavepanel.impl.edit;

import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;

/**
 * Defines the UI actions that can be performed as part of the editing feature.
 * This includes editing, replying, and deleting blips in a conversation.
 *
 */
public interface Actions {
  enum Action {
    /** Starts an edit session on the focused blip. */
    EDIT_BLIP,
    /** Creates a reply thread on the focused blip. */
    REPLY_TO_BLIP,
    /** Continues the thread of the focused blip. */
    CONTINUE_THREAD,
    /** Deletes the focused blip. */
    DELETE_BLIP,
    /** Deletes the thread of the focused blip. */
    DELETE_THREAD,
  }

  /**
   * Starts editing a blip.
   */
  void startEditing(BlipView blipUi);

  /**
   * Stops editing a blip.
   */
  void stopEditing();

  /**
   * Replies to a blip.
   */
  void reply(BlipView blipUi);

  /**
   * Adds a continuation to a thread.
   */
  void addContinuation(ThreadView threadUi);

  /**
   * Deletes a blip.
   */
  void delete(BlipView blipUi);

  /**
   * Deletes a thread.
   */
  void delete(ThreadView threadUi);

  /**
   * Pops up a link info for the blip.
   */
  void popupLink(BlipView blipUi);
}
