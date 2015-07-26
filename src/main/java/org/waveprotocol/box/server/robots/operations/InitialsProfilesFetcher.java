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

package org.waveprotocol.box.server.robots.operations;

import com.google.wave.api.ParticipantProfile;

import org.waveprotocol.box.server.robots.operations.FetchProfilesService.ProfilesFetcher;

/**
 * A {@link ProfilesFetcher} implementation that assigns a default image URL for
 * the user avatar using it's initial and a random color
 *
 * @author vjrj@apache.org (Vicente J. Ruiz Jurado)
 */
public class InitialsProfilesFetcher implements ProfilesFetcher {

  /**
   * Returns the avatar URL for the given email address.
   */
  public String getImageUrl(String email) {
    return "/iniavatars/100x100/" + email;
  }

  @Override
  public ParticipantProfile fetchProfile(String email) {
    ParticipantProfile pTemp = null;
    pTemp = ProfilesFetcher.SIMPLE_PROFILES_FETCHER.fetchProfile(email);
    ParticipantProfile profile =
        new ParticipantProfile(email, pTemp.getName(), getImageUrl(email), pTemp.getProfileUrl());
    return profile;
  }
}
