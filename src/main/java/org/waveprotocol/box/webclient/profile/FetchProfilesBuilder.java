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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;

import org.waveprotocol.box.profile.ProfileRequest;
import org.waveprotocol.box.profile.jso.ProfileRequestJsoImpl;
import org.waveprotocol.box.profile.jso.ProfileResponseJsoImpl;
import org.waveprotocol.box.webclient.profile.FetchProfilesService.Callback;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileImpl;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;

import java.util.Arrays;

/**
 * Helper class to fetch profiles.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class FetchProfilesBuilder {
  private static final LoggerBundle LOG = new DomLogger("FetchProfilesBuilder");

  /** The base profile URL. */
  private static final String SEARCH_URL_BASE = "/profile";

  /** Holds profile request data. */
  private ProfileRequest profileRequest;

  private FetchProfilesBuilder() {
  }

  /** Static factory method */
  public static FetchProfilesBuilder create() {
    return new FetchProfilesBuilder();
  }

  public FetchProfilesBuilder newFetchProfilesRequest() {
    profileRequest = ProfileRequestJsoImpl.create();
    return this;
  }

  public FetchProfilesBuilder setAddresses(String... addresses) {
    profileRequest.addAllAddresses(Arrays.asList(addresses));
    return this;
  }

  public FetchProfilesBuilder setProfilesManager(
      AbstractProfileManager<ProfileImpl> profileManager) {
    return this;
  }

  public void fetchProfiles(final Callback callback) {
    Preconditions.checkState(profileRequest != null);
    Preconditions.checkState(profileRequest.getAddresses() != null);

    String url = getUrl(profileRequest);
    LOG.trace().log(
        "Fetching profiles for: [" + Joiner.on(",").join(profileRequest.getAddresses()) + "]");

    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
    requestBuilder.setCallback(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        LOG.trace().log("Profile response received: ", response.getText());
        if (response.getStatusCode() != Response.SC_OK) {
          callback.onFailure("Got back status code " + response.getStatusCode());
        } else if (!response.getHeader("Content-Type").startsWith("application/json")) {
          callback.onFailure("Profile service did not return json");
        } else {
          ProfileResponseJsoImpl profileResponse;
          try {
            profileResponse = JsonMessage.parse(response.getText());
          } catch (JsonException e) {
            callback.onFailure(e.getMessage());
            return;
          }
          callback.onSuccess(profileResponse);
        }
      }

      @Override
      public void onError(Request request, Throwable e) {
        callback.onFailure(e.getMessage());
      }
    });

    try {
      requestBuilder.send();
    } catch (RequestException e) {
      LOG.error().log(e.getMessage());
    }
  }

  private static String getUrl(ProfileRequest profileRequest) {
    String params = "?addresses=" + Joiner.on(",").join(profileRequest.getAddresses());
    return SEARCH_URL_BASE + "/" + URL.encode(params);
  }

  @Override
  public String toString() {
    return "[Fetch profiles: " + Joiner.on(",").join(profileRequest.getAddresses()) + "]";
  }
}
