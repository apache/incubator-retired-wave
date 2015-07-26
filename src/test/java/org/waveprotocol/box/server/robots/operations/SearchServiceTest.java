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
import static org.waveprotocol.box.server.util.testing.TestingConstants.OTHER_PARTICIPANT;
import static org.waveprotocol.box.server.util.testing.TestingConstants.PARTICIPANT;
import static org.waveprotocol.box.server.util.testing.TestingConstants.WAVE_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;

import junit.framework.TestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.waveserver.SearchProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link SearchService}.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SearchServiceTest extends TestCase {
  private static final ParticipantId USER = ParticipantId.ofUnsafe("me@example.com");
  private static final WaveletId CONVERSATION_WAVELET_ID =
      WaveletId.of("example.com", "conv+root");

  private SearchService service;

  @Mock private SearchProvider searchProvider;
  @Mock private OperationRequest operation;
  @Mock private OperationContext context;
  
  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);
    
    when(operation.getParameter(ParamsProperty.QUERY)).thenReturn("in:inbox");
   
    service = new SearchService(searchProvider);
  }

  public void testSearchWrapsSearchProvidersResult() throws InvalidRequestException {
    String title = "title";
    SearchResult.Digest digest =
        new Digest(title, "", WAVE_ID.serialise(), ImmutableList.of(PARTICIPANT.getAddress(),
            OTHER_PARTICIPANT.getAddress()), -1L, -1L, 1, 1);
    String query = "in:inbox";
    SearchResult searchResult = new SearchResult(query);
    searchResult.addDigest(digest);
    when(searchProvider.search(USER, query, 0, 10)).thenReturn(searchResult);
    service.execute(operation, context, USER);

    verify(context).constructResponse(
        eq(operation),
        argThat(matchesSearchResult("in:inbox", WAVE_ID, "title", PARTICIPANT, ImmutableSet.of(
            PARTICIPANT, OTHER_PARTICIPANT), 1, 1)));
  }

  // Note: this is really just testing that the SearchService does not over-step
  // its role and do additional filtering beyond that done by the search
  // provider. If the search provider starts filtering empty waves, then this
  // test is not exactly reproducing expected behavior.
  public void testResultsAreWhatTheSearchProviderSaysIncludingEmptyWaves() throws Exception {
    TestingWaveletData data =
      new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, false);
    final Collection<WaveViewData> providerResults = Arrays.asList(data.copyViewData());

    String query = "in:inbox";
    SearchResult.Digest digest =
        new Digest("", "", WAVE_ID.serialise(), ImmutableList.of(PARTICIPANT.getAddress()), -1L,
            -1L, 1, 1);
    SearchResult searchResult = new SearchResult(query);
    searchResult.addDigest(digest);
    when(searchProvider.search(USER, query, 0, 10)).thenReturn(searchResult);
    service.execute(operation, context, USER);

    verify(context).constructResponse(
        eq(operation), argThat(new BaseMatcher<Map<ParamsProperty, Object>>() {
          @SuppressWarnings("unchecked")
          @Override
          public boolean matches(Object item) {
            Map<ParamsProperty, Object> map = (Map<ParamsProperty, Object>) item;
            assertTrue(map.containsKey(ParamsProperty.SEARCH_RESULTS));

            Object resultsObj = map.get(ParamsProperty.SEARCH_RESULTS);
            SearchResult results = (SearchResult) resultsObj;

            assertEquals(providerResults.size(), results.getNumResults());
            assertEquals(providerResults.size(), results.getDigests().size());

            return true;
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("Check digests match expected data");
          }
        }));
  }

  public void testDefaultFieldsMatchSpec() throws InvalidRequestException {
    String query = "in:inbox";
    when(searchProvider.search(USER, query, 0, 10)).thenReturn(new SearchResult(query));
    service.execute(operation, context, USER);

    verify(searchProvider).search(USER, query, 0, 10);
  }

  public void testSearchThrowsOnMissingQueryParameter() {
    when(operation.getParameter(ParamsProperty.QUERY)).thenReturn(null);
    try {
      service.execute(operation, context, USER);
      fail("Should have thrown an invalid request exception");
    } catch (InvalidRequestException e) {
      // pass.
    }
  }

  // *** Helpers

  public Matcher<Map<ParamsProperty, Object>> matchesSearchResult(final String query,
      final WaveId waveId, final String title, final ParticipantId author,
      final Set<ParticipantId> participants, final int unreadCount, final int blipCount) {
    return new BaseMatcher<Map<ParamsProperty, Object>>() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean matches(Object item) {
        Map<ParamsProperty, Object> map = (Map<ParamsProperty, Object>) item;
        assertTrue(map.containsKey(ParamsProperty.SEARCH_RESULTS));

        Object resultsObj = map.get(ParamsProperty.SEARCH_RESULTS);
        SearchResult results = (SearchResult) resultsObj;

        assertEquals(query, results.getQuery());
        assertEquals(1, results.getNumResults());

        Digest digest = results.getDigests().get(0);
        assertEquals(title, digest.getTitle());
        assertEquals(ApiIdSerializer.instance().serialiseWaveId(waveId), digest.getWaveId());

        Builder<ParticipantId> participantIds = ImmutableSet.builder();
        for (String name : digest.getParticipants()) {
          participantIds.add(ParticipantId.ofUnsafe(name));
        }
        assertEquals(participants, participantIds.build());

        assertEquals(unreadCount, digest.getUnreadCount());
        assertEquals(blipCount, digest.getBlipCount());
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Check digests match expected data");
      }
    };
  }
}
