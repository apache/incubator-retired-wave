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
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Base implementation of search provider.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public abstract class AbstractSearchProviderImpl implements SearchProvider {
  private static final Log LOG = Log.get(AbstractSearchProviderImpl.class);

  protected final WaveDigester digester;
  protected final ParticipantId sharedDomainParticipantId;
  private final WaveMap waveMap;

  public AbstractSearchProviderImpl(final String waveDomain, WaveDigester digester, WaveMap waveMap) {
    this.digester = digester;
    sharedDomainParticipantId = ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(waveDomain);
    this.waveMap = waveMap;
  }

  protected List<WaveViewData> computeSearchResult(final ParticipantId user, int startAt,
      int numResults, List<WaveViewData> results) {
    int searchResultSize = results.size();
    // Check if we have enough results to return.
    if (searchResultSize < startAt) {
      return Collections.emptyList();
    } else {
      int endAt = Math.min(startAt + numResults, searchResultSize);
      return Lists.newArrayList(results.subList(startAt, endAt));
    }
  }

  // TODO (yurize) : Refactor this method. It does two things: filtering and
  // building waves.
  protected LinkedHashMap<WaveId, WaveViewData> filterWavesViewBySearchCriteria(
      Function<ReadableWaveletData, Boolean> matchesFunction,
      LinkedHashMultimap<WaveId, WaveletId> currentUserWavesView) {
    // Must use a map with stable ordering, since indices are meaningful.
    LinkedHashMap<WaveId, WaveViewData> results = Maps.newLinkedHashMap();

    // Loop over the user waves view.
    for (WaveId waveId : currentUserWavesView.keySet()) {
      Set<WaveletId> waveletIds = currentUserWavesView.get(waveId);
      WaveViewData view = buildWaveViewData(waveId, waveletIds, matchesFunction, waveMap);
      Iterable<? extends ObservableWaveletData> wavelets = view.getWavelets();
      boolean hasConversation = false;
      for (ObservableWaveletData waveletData : wavelets) {
        if (IdUtil.isConversationalId(waveletData.getWaveletId())) {
          hasConversation = true;
          break;
        }
      }
      if ((view != null) && hasConversation) {
        results.put(waveId, view);
      }
    }
    return results;
  }

  public static WaveViewData buildWaveViewData(WaveId waveId, Set<WaveletId> waveletIds,
      Function<ReadableWaveletData, Boolean> matchesFunction, WaveMap waveMap) {

    WaveViewData view = WaveViewDataImpl.create(waveId); // Copy of the wave built up for search hits.
    for (WaveletId waveletId : waveletIds) {
      WaveletContainer waveletContainer = null;
      WaveletName waveletname = WaveletName.of(waveId, waveletId);


      // TODO (Yuri Z.) This loop collects all the wavelets that match the
      // query, so the view is determined by the query. Instead we should
      // look at the user's wave view and determine if the view matches the
      // query.
      try {
        waveletContainer = waveMap.getWavelet(waveletname);
        if ((waveletContainer == null) || !waveletContainer.applyFunction(matchesFunction)) {
          continue;
        }
        // Just keep adding all the relevant wavelets in this wave.
        view.addWavelet(waveletContainer.copyWaveletData());
      } catch (WaveletStateException e) {
        LOG.warning("Failed to access wavelet " + waveletContainer.getWaveletName(), e);
      }
    }
    return view;
  }

  /**
   * Verifies whether the wavelet matches the filter criteria.
   *
   * @param wavelet the wavelet.
   * @param user the logged in user.
   * @param sharedDomainParticipantId the shared domain participant id.
   * @param isAllQuery true if the search results should include shared for this
   *        domain waves.
   */
  protected boolean isWaveletMatchesCriteria(ReadableWaveletData wavelet, ParticipantId user,
      ParticipantId sharedDomainParticipantId, boolean isAllQuery)
          throws WaveletStateException {
    Preconditions.checkNotNull(wavelet);
    // If it is user data wavelet for the user - return true.
    if (IdUtil.isUserDataWavelet(wavelet.getWaveletId()) && wavelet.getCreator().equals(user)) {
      return true;
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
    return true;
  }

  /**
   * Ensures that each wave in the current waves view has the user data wavelet by always adding
   *  it to the view.
   */
  protected void ensureWavesHaveUserDataWavelet(
      LinkedHashMultimap<WaveId, WaveletId> currentUserWavesView, ParticipantId user) {
    WaveletId udw =
        WaveletId.of(user.getDomain(),
            IdUtil.join(IdConstants.USER_DATA_WAVELET_PREFIX, user.getAddress()));
    Set<WaveId> waveIds = currentUserWavesView.keySet();
    for (WaveId waveId : waveIds) {
      Set<WaveletId> waveletIds = currentUserWavesView.get(waveId);
      waveletIds.add(udw);
    }
  }
}
