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

package org.waveprotocol.wave.client.widget.profile;

import com.google.gwt.event.dom.client.ClickHandler;

import org.waveprotocol.wave.client.common.safehtml.SafeHtml;

/**
 * A profile card popup.
 *
 */
public interface ProfilePopupView {

  /**
   * Observer of view events.
   */
  public interface Listener {
    void onShow();

    void onHide();
  }

  /**
   * Binds this view to a listener, until {@link #reset()}.
   */
  void init(Listener listener);

  /**
   * Releases this view from its listener, allowing it to be reused.
   */
  void reset();

  /**
   * Shows the popup.
   */
  void show();

  /**
   * Hides the popup.
   */
  void hide();

  /**
   * Adds a control button to this card.
   *
   * @param label button label
   * @param handler handler for when the button is clicked
   */
  void addButton(SafeHtml label, ClickHandler handler);

  /**
   * Sets the image URL for the profile's avatar.
   *
   * @param url avatar URL
   */
  void setAvatarUrl(String url);

  /**
   * Sets the name on the profile card.
   *
   * @param name name
   */
  void setName(String name);

  /**
   * Sets the address on the profile card.
   *
   * @param address address
   */
  void setAddress(String address);
}
