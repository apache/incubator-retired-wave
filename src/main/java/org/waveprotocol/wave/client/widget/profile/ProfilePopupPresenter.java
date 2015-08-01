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

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;


/**
 * Presents a profile into a profile popup view. The presentation is live while
 * the popup is shown (i.e., profile updates are propagated into the view).
 *
 */
public final class ProfilePopupPresenter implements ProfileListener, ProfilePopupView.Listener {
  private final Profile model;
  private final ProfilePopupView view;
  private final ProfileManager events;

  private ProfilePopupPresenter(Profile model, ProfilePopupView view, ProfileManager events) {
    this.model = model;
    this.view = view;
    this.events = events;
  }

  /**
   * Creates a profile popup presenter. This presenter destroys itself and
   * detaches from the view as soon as the popup hides.
   *
   * @param model profile to present
   * @param view view in which to present
   * @param events source of profile events
   * @return a new presenter.
   */
  public static ProfilePopupPresenter create(
      Profile model, ProfilePopupView view, ProfileManager events) {
    ProfilePopupPresenter profileUi = new ProfilePopupPresenter(model, view, events);
    profileUi.init();
    return profileUi;
  }

  private void init() {
    view.init(this);
  }

  private void destroy() {
    view.reset();
  }

  /**
   * Adds a button to the profile card.
   *
   * @param label
   * @param handler
   */
  public void addControl(SafeHtml label, ClickHandler handler) {
    view.addButton(label, handler);
  }

  /**
   * Shows this popup on screen.
   */
  public void show() {
    render();
    view.show();
  }

  private void render() {
    view.setAddress(model.getAddress());
    view.setName(model.getFullName());
    view.setAvatarUrl(model.getImageUrl());
  }

  @Override
  public void onShow() {
    events.addListener(this);
  }

  @Override
  public void onProfileUpdated(Profile profile) {
    if (profile.equals(model)) {
      render();
    }
  }

  @Override
  public void onHide() {
    events.removeListener(this);
    destroy();
  }
}
