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

package org.waveprotocol.box.server.robots;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.robots.operations.FetchProfilesService.ProfilesFetcher;
import org.waveprotocol.box.server.robots.operations.GravatarProfilesFetcher;
import org.waveprotocol.box.server.robots.operations.InitialsProfilesFetcher;

/**
 * Profile Fetcher Module.
 *
 * @author vjrj@apache.org (Vicente J. Ruiz Jurado)
 */
public class ProfileFetcherModule extends AbstractModule {


  private String profileFetcherType;

  @Inject
  public ProfileFetcherModule(Config config) {
    this.profileFetcherType = config.getString("core.profile_fetcher_type");
  }

  @Override
  protected void configure() {
    if ("gravatar".equals(profileFetcherType)) {
      bind(ProfilesFetcher.class).to(GravatarProfilesFetcher.class).in(Singleton.class);
    } else if ("initials".equals(profileFetcherType)) {
      bind(ProfilesFetcher.class).to(InitialsProfilesFetcher.class).in(Singleton.class);
    } else {
      throw new RuntimeException("Unknown profile fetcher type: " + profileFetcherType);
    }
  }
}
