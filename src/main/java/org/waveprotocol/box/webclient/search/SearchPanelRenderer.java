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

package org.waveprotocol.box.webclient.search;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.DateUtils;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collection;

/**
 * Renders a digest model into a digest view.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SearchPanelRenderer {
  private final static int MAX_AVATARS = 3;

  /** Profile provider, for avatars. */
  private final ProfileManager profiles;

  public SearchPanelRenderer(ProfileManager profiles) {
    this.profiles = profiles;
  }

  /**
   * Renders a digest model into a digest view.
   */
  public void render(Digest digest, DigestView digestUi) {
    Collection<Profile> avatars = CollectionUtils.createQueue();
    if (digest.getAuthor() != null) {
      avatars.add(profiles.getProfile(digest.getAuthor()));
    }
    for (ParticipantId other : digest.getParticipantsSnippet()) {
      if (avatars.size() < MAX_AVATARS) {
        avatars.add(profiles.getProfile(other));
      } else {
        break;
      }
    }

    digestUi.setAvatars(avatars);
    digestUi.setTitleText(digest.getTitle());
    digestUi.setSnippet(digest.getSnippet());
    digestUi.setMessageCounts(digest.getUnreadCount(), digest.getBlipCount());
    digestUi.setTimestamp(
        DateUtils.getInstance().formatPastDate((long) digest.getLastModifiedTime()));
  }
}
