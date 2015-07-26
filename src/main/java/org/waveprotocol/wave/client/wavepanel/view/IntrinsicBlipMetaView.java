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

package org.waveprotocol.wave.client.wavepanel.view;

import java.util.Set;


/**
 * Reveals the primitive state in the metadata section of a blip.
 *
 * @see BlipMetaView for structural state.
 */
public interface IntrinsicBlipMetaView {

  enum MenuOption {
    /** Option to reply to this blip. */
    REPLY,
    /** Option to delete this blip. */
    DELETE,
    /** Option to link to this blip. */
    LINK,
    /** Option to edit this blip. */
    EDIT,
    /** Option to finish edit this blip. */
    EDIT_DONE,;
  }

  /**
   * Sets the avatar image.
   */
  void setAvatar(String imageUrl);

  /**
   * Sets the last modified time.
   */
  void setTime(String time);

  /**
   * Sets the line of text at the top of this blip.
   */
  void setMetaline(String metaline);

  /**
   * Sets the read state.
   *
   * @param read true for read, false for unread.
   */
  void setRead(boolean read);

  /**
   * Enables a set of menu options. Only enabled options are shown.
   *
   * @param options options to be enabled
   */
  void enable(Set<MenuOption> options);

  /**
   * Disables a set of menu options. Only enabled options are shown.
   *
   * @param options options to be disabled
   */
  void disable(Set<MenuOption> options);

  /**
   * Selects (shown as depressed) a menu item.
   *
   * @param option option to select
   */
  void select(MenuOption option);

  /**
   * De-selects a menu item.
   *
   * @param option option to de-select
   */
  void deselect(MenuOption option);
}
