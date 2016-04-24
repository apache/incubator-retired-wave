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

package com.google.wave.api;

import java.util.List;

/**
 * FetchProfilesRequest contain the request for one or more profiles.
 */
public class FetchProfilesRequest {

  private final List<String> participantIds;
  private final String language;

  /**
   * No-args constructor to keep gson happy.
   */
  public FetchProfilesRequest() {
    this(null, null);
  }

  public FetchProfilesRequest(List<String> participantIds) {
    this(participantIds, null);
  }

  public FetchProfilesRequest(List<String> participantIds, String language) {
    this.participantIds = participantIds;
    this.language = language;
  }

  /**
   * @return the requested language of the profile.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @return the participant ids of the requested profile.
   */
  public List<String> getParticipantIds() {
    return participantIds;
  }
}
