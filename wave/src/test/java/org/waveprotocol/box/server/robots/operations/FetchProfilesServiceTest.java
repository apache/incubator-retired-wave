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

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.FetchProfilesRequest;
import com.google.wave.api.FetchProfilesResult;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ParticipantProfile;

import junit.framework.TestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.operations.FetchProfilesService.ProfilesFetcher;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map;

/**
 * Unit tests for {@link FetchProfilesService}.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class FetchProfilesServiceTest extends TestCase {

  private static final String ADDRESS = "john.smith@example.com";
  private static final String NAME = "John Smith";
  private static final String PROFILE_URL = "";
  private static final String IMAGE_URL = "/static/images/unknown.jpg";

  private FetchProfilesService service;
  
  @Mock private OperationRequest operation;
  @Mock private OperationContext context;
  @Mock private ProfilesFetcher fakeProfilesFetcher;

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);
    FetchProfilesRequest request = new FetchProfilesRequest(Lists.newArrayList(ADDRESS));
    when(operation.getParameter(ParamsProperty.FETCH_PROFILES_REQUEST)).thenReturn(request);
    when(fakeProfilesFetcher.fetchProfile(ADDRESS)).thenReturn(
        new ParticipantProfile(ADDRESS, NAME, IMAGE_URL, PROFILE_URL));
    service = new FetchProfilesService(fakeProfilesFetcher);
  }

  public void testFetchProfilesServiceWorks() throws InvalidRequestException {
    service.execute(operation, context, ParticipantId.ofUnsafe(ADDRESS));
    verify(context).constructResponse(eq(operation),
        argThat(matchesFetchResult(ADDRESS, NAME, PROFILE_URL, IMAGE_URL)));
  }
  
  public void testSimpleProfilesFetcherWorks() throws InvalidRequestException {
    service = new FetchProfilesService(FetchProfilesService.ProfilesFetcher.SIMPLE_PROFILES_FETCHER);
    service.execute(operation, context, ParticipantId.ofUnsafe(ADDRESS));
    verify(context).constructResponse(eq(operation),
        argThat(matchesFetchResult(ADDRESS, NAME, PROFILE_URL, IMAGE_URL)));
  }

  // *** Helper
  public Matcher<Map<ParamsProperty, Object>> matchesFetchResult(final String address,
      final String name, final String profileUrl, final String imageUrl) {
    return new BaseMatcher<Map<ParamsProperty, Object>>() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean matches(Object item) {
        Map<ParamsProperty, Object> map = (Map<ParamsProperty, Object>) item;
        assertTrue(map.containsKey(ParamsProperty.FETCH_PROFILES_RESULT));
        Object resultsObj = map.get(ParamsProperty.FETCH_PROFILES_RESULT);
        FetchProfilesResult results = (FetchProfilesResult) resultsObj;
        assertNotNull(results.getProfiles());
        assertEquals(1, results.getProfiles().size());
        ParticipantProfile profile = results.getProfiles().get(0);
        assertEquals(address, profile.getAddress());
        assertEquals(name, profile.getName());
        assertEquals(profileUrl, profile.getProfileUrl());
        assertEquals(imageUrl, profile.getImageUrl());
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Check user profile matches expected data");
      }
    };
  }
}
