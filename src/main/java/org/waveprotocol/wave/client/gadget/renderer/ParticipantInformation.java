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

package org.waveprotocol.wave.client.gadget.renderer;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Overlay and JSON converter class to hold the participant information.
 * This class holds the data to be transferred in JSON form to the Gadget via
 * RPC.
 *
 */
public final class ParticipantInformation extends JavaScriptObject {

  /**
   * External construction is banned.
   */
  protected ParticipantInformation() {
  }

  /**
   * Creates participant information object.
   *
   * @param myId ID of the current user.
   * @param authorId ID of the author of the blip where the Gadget resides.
   * @param participants participants of the blip where the Gadget resides.
   * @param parentUrl parent URL to prepend to the image URL without host.
   * @return constructed participant information object.
   */
  public static ParticipantInformation create(
      String myId, String authorId, List<? extends ParticipantId> participants,
      String parentUrl, ProfileManager profileManger) {
    ParticipantInformation info = createInformationObject(myId, authorId);
    for (ParticipantId participant : participants) {
      info.addParticipant(participant, parentUrl, profileManger);
    }
    return info;
  }

  /**
   * Adds participant information to this object.
   *
   * @param participant participant to add.
   * @param parentUrl parent URL to prepend to the image URL without host.
   */
  public void addParticipant(ParticipantId participant, String parentUrl,
      ProfileManager profileManger) {
    Profile profile = profileManger.getProfile(participant);
    String imageUrl = "";
    String fullName = participant.getAddress();
    if (profile != null) {
      imageUrl = profile.getImageUrl();
      if (!(imageUrl.startsWith("http://") || imageUrl.startsWith("https://") ||
          imageUrl.startsWith("//"))) {
        imageUrl = parentUrl + imageUrl;
      }
    }
    addParticipantInfo(participant.getAddress(),
                       fullName,
                       imageUrl);
  }

  /**
   * Removes participant information from this object.
   *
   * @param participant participant to remove.
   */
  public void removeParticipant(ParticipantId participant) {
    removeParticipantInfo(participant.getAddress());
  }

  /**
   * Returns the ID of the current user stored in the object.
   *
   * @return ID of the current user.
   */
  public native String getMyId() /*-{
    return this['myId'];
  }-*/;

  /**
   * Creates JS object to represent this GWT object.
   *
   * @param myId ID of the current user.
   * @param authorId ID of the author of the blip where the Gadget resides.
   * @return JS object.
   */
  private static native ParticipantInformation createInformationObject(
      String myId, String authorId) /*-{
    return {
      'myId': myId,
      'authorId': authorId,
      'participants': {}
    };
  }-*/;

  private native void addParticipantInfo(String id, String name, String thumbnailUrl) /*-{
    // TODO(user): Add more fields such as presence, profile info, etc.
    var p = {
      'id': id,
      'displayName': name,
      'thumbnailUrl': thumbnailUrl
    };
    this['participants'][id] = p;
  }-*/;

  public native void removeParticipantInfo(String id) /*-{
    delete this['participants'][id];
  }-*/;
}
