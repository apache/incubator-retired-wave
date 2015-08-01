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

package org.waveprotocol.box.webclient.profile;

import org.waveprotocol.box.profile.ProfileResponse;

/**
 * Interface that exposes the fetch profile services to the client.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public interface FetchProfilesService {
  
  public interface Callback {
    void onFailure(String message);

    /**
     * Notifies this callback of a successful fetch profiles response.
     *
     * @param profileResponse the response from the server.
     */
    void onSuccess(ProfileResponse profileResponse);
  }

  /**
   * Fetches profiles.
   * 
   * @param callback the callback.
   * @param addresses the profiles to fetch.
   */
  void fetch(Callback callback, String... addresses);
}
