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

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.SearchResult;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.QueryHelper.InvalidQueryException;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Search provider that reads user specific info from user data wavelet.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class SimpleSearchProviderImpl extends AbstractSearchProviderImpl {

  private static final Log LOG = Log.get(SimpleSearchProviderImpl.class);

  private final PerUserWaveViewProvider waveViewProvider;

  @Inject
  public SimpleSearchProviderImpl(@Named(CoreSettings.WAVE_SERVER_DOMAIN) final String waveDomain,
      WaveDigester digester, final WaveMap waveMap, PerUserWaveViewProvider userWaveViewProvider) {
    super(waveDomain, digester, waveMap);
    this.waveViewProvider = userWaveViewProvider;
  }

  @Override
  public SearchResult search(final ParticipantId user, String query, int startAt,
      int numResults) {
    LOG.fine("Search query '" + query + "' from user: " + user + " [" + startAt + ", "
        + (startAt + numResults - 1) + "]");
    Map<TokenQueryType, Set<String>> queryParams = null;
    try {
      queryParams = QueryHelper.parseQuery(query);
    } catch (InvalidQueryException e1) {
      // Invalid query param - stop and return empty search results.
      LOG.warning("Invalid Query. " + e1.getMessage());
      return digester.generateSearchResult(user, query, null);
    }
    // Maybe should be changed in case other folders in addition to 'inbox' are
    // added.
    final boolean isAllQuery = !queryParams.containsKey(TokenQueryType.IN);

    final List<ParticipantId> withParticipantIds;
    final List<ParticipantId> creatorParticipantIds;
    try {
      String localDomain = user.getDomain();
      // Build and validate.
      withParticipantIds =
          QueryHelper.buildValidatedParticipantIds(queryParams, TokenQueryType.WITH,
              localDomain);
      creatorParticipantIds =
          QueryHelper.buildValidatedParticipantIds(queryParams, TokenQueryType.CREATOR,
              localDomain);
    } catch (InvalidParticipantAddress e) {
      // Invalid address - stop and return empty search results.
      LOG.warning("Invalid participantId: " + e.getAddress() + " in query: " + query);
      return digester.generateSearchResult(user, query, null);
    }

    Multimap<WaveId, WaveletId> currentUserWavesView =  createWavesViewToFilter(user, isAllQuery);
    Function<ReadableWaveletData, Boolean> filterWaveletsFunction =
        createFilterWaveletsFunction(user, isAllQuery, withParticipantIds, creatorParticipantIds);

    List<WaveViewData> results =
        Lists.newArrayList(filterWavesViewBySearchCriteria(filterWaveletsFunction,
            currentUserWavesView).values());
    List<WaveViewData> sortedResults = sort(queryParams, results);

    Collection<WaveViewData> searchResult =
        computeSearchResult(user, startAt, numResults, sortedResults);
    LOG.info("Search response to '" + query + "': " + searchResult.size() + " results, user: "
        + user);
    return digester.generateSearchResult(user, query, searchResult);
  }

  private Multimap<WaveId, WaveletId> createWavesViewToFilter(final ParticipantId user,
      final boolean isAllQuery) {
    Multimap<WaveId, WaveletId> currentUserWavesView;
    currentUserWavesView = HashMultimap.create();
    currentUserWavesView.putAll(waveViewProvider.retrievePerUserWaveView(user));
    if (isAllQuery) {
      // If it is the "all" query - we need to include also waves view of the
      // shared domain participant.
      currentUserWavesView.putAll(waveViewProvider.retrievePerUserWaveView(sharedDomainParticipantId));
    }

    if(LOG.isFineLoggable()) {
      for (Map.Entry<WaveId, WaveletId> e : currentUserWavesView.entries()) {
        LOG.fine("unfiltered view contains: " + e.getKey() + " " + e.getValue());
      }
    }

    return currentUserWavesView;
  }

  private Function<ReadableWaveletData, Boolean> createFilterWaveletsFunction(final ParticipantId user,
      final boolean isAllQuery, final List<ParticipantId> withParticipantIds,
      final List<ParticipantId> creatorParticipantIds) {
    // A function to be applied by the WaveletContainer.
    Function<ReadableWaveletData, Boolean> matchesFunction =
        new Function<ReadableWaveletData, Boolean>() {

          @Override
          public Boolean apply(ReadableWaveletData wavelet) {
            try {
              return isWaveletMatchesCriteria(wavelet, user, sharedDomainParticipantId, withParticipantIds,
                  creatorParticipantIds, isAllQuery);
            } catch (WaveletStateException e) {
              LOG.warning(
                  "Failed to access wavelet "
                      + WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId()), e);
              return false;
            }
          }
        };
    return matchesFunction;
  }

  /**
   * Verifies whether the wavelet matches the filter criteria.
   *
   * @param wavelet the wavelet.
   * @param user the logged in user.
   * @param sharedDomainParticipantId the shared domain participant id.
   * @param withList the list of participants to be used in 'with' filter.
   * @param creatorList the list of participants to be used in 'creator' filter.
   * @param isAllQuery true if the search results should include shared for this
   *        domain waves.
   */
  private boolean isWaveletMatchesCriteria(ReadableWaveletData wavelet, ParticipantId user,
      ParticipantId sharedDomainParticipantId, List<ParticipantId> withList,
      List<ParticipantId> creatorList, boolean isAllQuery) throws WaveletStateException {
    // If it is user data wavelet for the user - return true.
    if (IdUtil.isUserDataWavelet(wavelet.getWaveletId()) && wavelet.getCreator().equals(user)) {
      return true;
    }
    // Filter by creator. This is the fastest check so we perform it first.
    for (ParticipantId creator : creatorList) {
      if (!creator.equals(wavelet.getCreator())) {
        // Skip.
        return false;
      }
    }
    // The wavelet should have logged in user as participant for 'in:inbox'
    // query.
    if (!isAllQuery && !wavelet.getParticipants().contains(user)) {
      return false;
    }
    // Or if it is an 'all' query - then either logged in user or shared domain
    // participant should be present in the wave.
    if (isAllQuery
        && !WaveletDataUtil.checkAccessPermission(wavelet, user, sharedDomainParticipantId)) {
      return false;
    }
    // If not returned 'false' above - then logged in user is either
    // explicit or implicit participant and therefore has access permission.

    // Now filter by 'with'.
    for (ParticipantId otherUser : withList) {
      if (!wavelet.getParticipants().contains(otherUser)) {
        // Skip.
        return false;
      }
    }
    return true;
  }

  private List<WaveViewData> sort(Map<TokenQueryType, Set<String>> queryParams,
      List<WaveViewData> results) {
    return QueryHelper.computeSorter(queryParams).sortedCopy(results);
  }
}
