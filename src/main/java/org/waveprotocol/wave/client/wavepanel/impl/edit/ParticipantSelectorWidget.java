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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.TitleBar;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashSet;
import java.util.Set;

/**
 * Selector for participants to add to a newly created wave. Allowing an user
 * to create a new wave with participants from another wave.
 *
 * @author wavegrove@gmail.com
 */
public class ParticipantSelectorWidget extends Composite implements
    ParticipantWidget.Listener {
  public interface Listener {
    void onSelect(Set<ParticipantId> participants);
    void onCancel();
  }

  interface Binder extends UiBinder<ImplPanel, ParticipantSelectorWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField ImplPanel self;
  @UiField DockLayoutPanel dockPanel;
  @UiField Button createButton;
  @UiField Button cancelButton;
  @UiField Button selectAllButton;
  @UiField Button deselectAllButton;
  @UiField FlowPanel options;

  private Listener listener;
  private Set<ParticipantId> participants;
  private ParticipantId user;
  private ProfileManager profiles;

  public ParticipantSelectorWidget() {
    initWidget(self = BINDER.createAndBindUi(this));
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Shows in a popup, and returns the popup.
   * @param user the logged in user. The popup does not show it but makes sure it is
   *             in the participant set returned
   */
  public UniversalPopup showInPopup(ParticipantId user, Set<ParticipantId> participants,
      ProfileManager profiles) {
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    UniversalPopup popup = PopupFactory.createPopup(
        null, new CenterPopupPositioner(), chrome, true);

    TitleBar titleBar = popup.getTitleBar();
    titleBar.setTitleText("Select participants");
    popup.add(ParticipantSelectorWidget.this);

    this.user = user;
    this.participants = new HashSet<ParticipantId>(participants);
    this.profiles = profiles;

    // If there is only one participant, create the wave without showing the popup
    if (participants.size() == 1) {
      if (listener != null) {
        listener.onSelect(participants);
      }
      return popup;
    }

    createParticipantList(participants);
    popup.show();
    setFocusAndHeight();

    return popup;
  }

  private void setFocusAndHeight() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        createButton.setFocus(true);
        dockPanel
            .setHeight((ParticipantSelectorWidget.this.getParent()
                .getElement().getOffsetHeight()) + "px");
      }
    });
  }

  private void createParticipantList(Set<ParticipantId> participants) {
    options.clear();
    for (ParticipantId participant : participants) {
      if (!participant.equals(user)) {
        ParticipantWidget participantWidget = new ParticipantWidget();
        participantWidget.setParticipant(participant);
        participantWidget.setImage(profiles.getProfile(participant).getImageUrl());
        participantWidget.setListener(this);
        options.add(participantWidget);
      }
    }
  }

  @UiHandler("createButton")
  void onClickCreateButton(ClickEvent event) {
    if (listener != null) {
      listener.onSelect(participants);
    }
  }

  @UiHandler("cancelButton")
  void onClickCancelButton(ClickEvent event) {
    if (listener != null) {
      listener.onCancel();
    }
  }

  @UiHandler("selectAllButton")
  void onSelectAllClicked(ClickEvent event) {
    for (int i = 0; i < options.getWidgetCount(); i++) {
      ((ParticipantWidget)options.getWidget(i)).mark();
    }
  }

  @UiHandler("deselectAllButton")
  void onDeselectAllClicked(ClickEvent event) {
    for (int i = 0; i < options.getWidgetCount(); i++) {
      ((ParticipantWidget)options.getWidget(i)).unMark();
    }
  }

  @Override
  public void onMark(ParticipantId participant) {
    if (!participants.contains(participant)) {
      participants.add(participant);
    }
  }

  @Override
  public void onUnMark(ParticipantId participant) {
    participants.remove(participant);
  }
}
