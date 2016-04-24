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

import com.google.inject.Inject;
import com.google.wave.api.ParticipantProfile;
import com.typesafe.config.Config;
import org.apache.commons.codec.digest.DigestUtils;
import org.waveprotocol.box.server.robots.operations.FetchProfilesService.ProfilesFetcher;

/**
 * A {@link ProfilesFetcher} implementation that assigns a Gravatar identicon
 * image URL for the user avatar. Users can change the avatar image by going to
 * gravatar.com and adding their wave address to the main profile. It is
 * impossible to create a main profile with wave address since gravatar requires
 * email address verification.
*
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class GravatarProfilesFetcher implements ProfilesFetcher {

  private final static String SECURE_GRAVATAR_URL = "https://secure.gravatar.com/avatar/";
  private final static String NON_SECURE_GRAVATAR_URL = "http://gravatar.com/avatar/";

  private final String gravatarUrl;

  @Inject
  public GravatarProfilesFetcher(Config config) {
    if (config.getBoolean("security.enable_ssl")) {
      gravatarUrl = SECURE_GRAVATAR_URL;
    } else {
      gravatarUrl = NON_SECURE_GRAVATAR_URL;
    }
  }

  /**
   * Returns the Gravatar identicon URL for the given email address.
   */
  public String getImageUrl(String email) {
    // Hexadecimal MD5 hash of the requested user's lowercased email address
    // with all whitespace trimmed.
    String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());
    return gravatarUrl + emailHash + ".jpg?s=100&d=identicon";
  }

  @Override
  public ParticipantProfile fetchProfile(String email) {
    ParticipantProfile pTemp;
    pTemp = ProfilesFetcher.SIMPLE_PROFILES_FETCHER.fetchProfile(email);
    return new ParticipantProfile(email, pTemp.getName(), getImageUrl(email), pTemp.getProfileUrl());
  }
}
