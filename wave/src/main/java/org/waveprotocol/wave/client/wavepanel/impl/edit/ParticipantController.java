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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.events.ClientEvents;
import org.waveprotocol.wave.client.events.WaveCreationEvent;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.WaveClickHandler;
import org.waveprotocol.wave.client.wavepanel.impl.edit.i18n.ParticipantMessages;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.client.widget.profile.ProfilePopupPresenter;
import org.waveprotocol.wave.client.widget.profile.ProfilePopupView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Installs the add/remove participant controls.
 *
 */
public final class ParticipantController {
  private final DomAsViewProvider views;
  private final ModelAsViewProvider models;
  private final ProfileManager profiles;
  private final String localDomain;
  private final ParticipantId user;
  private final ParticipantMessages messages;
  private UniversalPopup popup = null;

  /**
   * @param localDomain nullable. if provided, automatic suffixing will occur.
   * @param user the logged in user
   */
  ParticipantController(
      DomAsViewProvider views, ModelAsViewProvider models, ProfileManager profiles,
      String localDomain, ParticipantId user, ParticipantMessages messages) {
    this.views = views;
    this.models = models;
    this.profiles = profiles;
    this.localDomain = localDomain;
    this.user = user;
    this.messages = messages;
  }

  /**
   * Builds and installs the participant control feature.
   * @param user the logged in user
   */
  public static void install(WavePanel panel, ModelAsViewProvider models, ProfileManager profiles,
      String localDomain, ParticipantId user, ParticipantMessages messages) {
    ParticipantController controller =
        new ParticipantController(panel.getViewProvider(), models, profiles,
          localDomain, user, messages);
    controller.install(panel.getHandlers());
  }

  private void install(EventHandlerRegistry handlers) {
    handlers.registerClickHandler(TypeCodes.kind(Type.ADD_PARTICIPANT), new WaveClickHandler() {
      @Override
      public boolean onClick(ClickEvent event, Element context) {
        handleAddButtonClicked(context);
        return true;
      }
    });
    handlers.registerClickHandler(TypeCodes.kind(Type.NEW_WAVE_WITH_PARTICIPANTS),
      new WaveClickHandler() {
        @Override
        public boolean onClick(ClickEvent event, Element context) {
          handleNewWaveWithParticipantsButtonClicked(context);
          return true;
        }
      });
    handlers.registerClickHandler(TypeCodes.kind(Type.PARTICIPANT), new WaveClickHandler() {
      @Override
      public boolean onClick(ClickEvent event, Element context) {
        handleParticipantClicked(context);
        return true;
      }
    });
  }

  /**
   * Constructs a list of {@link ParticipantId} with the supplied string with comma
   * separated participant addresses. The method will only succeed if all addresses
   * is valid.
   *
   * @param localDomain if provided, automatic suffixing will occur.
   * @param addresses string with comma separated participant addresses
   * @return the array of {@link ParticipantId} instances constructed using the given
   *         addresses string
   * @throws InvalidParticipantAddress if at least one of the addresses failed validation.
   */
  public static ParticipantId[] buildParticipantList(
      @Nullable String localDomain, String addresses) throws InvalidParticipantAddress {
    Preconditions.checkNotNull(addresses, "Expected non-null address");

    String[] addressList = addresses.split(",");
    ParticipantId[] participants = new ParticipantId[addressList.length];

    for (int i = 0; i < addressList.length; i++) {
      String address = addressList[i].trim();

      if (localDomain != null) {
        if (!address.isEmpty() && address.indexOf("@") == -1) {
          // If no domain was specified, assume that the participant is from the local domain.
          address = address + "@" + localDomain;
        } else if (address.equals("@")) {
          // "@" is a shortcut for the shared domain participant.
          address = address + localDomain;
        }
      }

      // Will throw InvalidParticipantAddress if address is not valid
      participants[i] = ParticipantId.of(address);
    }
    return participants;
  }

  /**
   * Creates a new wave with the participants of the current wave. Showing
   * a popup dialog where the user can chose to deselect users that should not
   * be participants in the new wave
   */
  private void handleNewWaveWithParticipantsButtonClicked(Element context) {
    ParticipantsView participantsUi = views.fromNewWaveWithParticipantsButton(context);
    ParticipantSelectorWidget selector = new ParticipantSelectorWidget();
    popup = null;
    selector.setListener(new ParticipantSelectorWidget.Listener() {
      @Override
      public void onSelect(Set<ParticipantId> participants) {
        if (popup != null) {
          popup.hide();
        }
        ClientEvents.get().fireEvent(
            new WaveCreationEvent(participants));
      }

      @Override
      public void onCancel() {
        popup.hide();
      }
    });
    popup = selector.showInPopup(user,
        models.getParticipants(participantsUi).getParticipantIds(), profiles);
  }

  /**
   * Shows an add-participant popup.
   */
  private void handleAddButtonClicked(Element context) {
    String addressString = Window.prompt("Add a participant(s) (separate with comma ','): ", "");
    if (addressString == null) {
      return;
    }

    ParticipantId[] participants;

    try {
      participants = buildParticipantList(localDomain, addressString);
    } catch (InvalidParticipantAddress e) {
      Window.alert(e.getMessage());
      return;
    }

    ParticipantsView participantsUi = views.fromAddButton(context);
    Conversation conversation = models.getParticipants(participantsUi);
    for (ParticipantId participant : participants) {
      conversation.addParticipant(participant);
    }
  }

  /**
   * Shows a participation popup for the clicked participant.
   */
  private void handleParticipantClicked(Element context) {
    ParticipantView participantView = views.asParticipant(context);
    final Pair<Conversation, ParticipantId> participation = models.getParticipant(participantView);
    Profile profile = profiles.getProfile(participation.second);

    // Summon a popup view from a participant, and attach profile-popup logic to
    // it.
    final ProfilePopupView profileView = participantView.showParticipation();
    ProfilePopupPresenter profileUi = ProfilePopupPresenter.create(profile, profileView, profiles);
    profileUi.addControl(EscapeUtils.fromSafeConstant(messages.remove()), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        participation.first.removeParticipant(participation.second);
        // The presenter is configured to destroy itself on view hide.
        profileView.hide();
      }
    });
    profileUi.show();
  }
}
