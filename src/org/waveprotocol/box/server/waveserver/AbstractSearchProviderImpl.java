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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

  protected Collection<WaveViewData> computeSearchResult(final ParticipantId user, int startAt,
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

  protected Map<WaveId, WaveViewData> filterWavesViewBySearchCriteria(
      Function<ReadableWaveletData, Boolean> matchesFunction,
      Multimap<WaveId, WaveletId> currentUserWavesView) {
    // Must use a map with stable ordering, since indices are meaningful.
    Map<WaveId, WaveViewData> results = Maps.newLinkedHashMap();

    // Loop over the user waves view.
    for (WaveId waveId : currentUserWavesView.keySet()) {
      Collection<WaveletId> waveletIds = currentUserWavesView.get(waveId);
      WaveViewData view = buildWaveViewData(waveId, waveletIds, matchesFunction, waveMap);
      if (view != null) {
        results.put(waveId, view);
      }
    }
    return results;
  }

  public static WaveViewData buildWaveViewData(WaveId waveId, Collection<WaveletId> waveletIds,
      Function<ReadableWaveletData, Boolean> matchesFunction, WaveMap waveMap) {

    WaveViewData view = null; // Copy of the wave built up for search hits.
    for (WaveletId waveletId : waveletIds) {
      WaveletContainer waveletContainer = null;
      WaveletName waveletname = WaveletName.of(waveId, waveletId);

      // TODO (alown): Find some way to use isLocalWavelet to do this properly!
      try {
        if (LOG.isFineLoggable()) {
          LOG.fine("Trying as a remote wavelet");
        }
        waveletContainer = waveMap.getRemoteWavelet(waveletname);
      } catch (WaveletStateException e) {
        LOG.severe(String.format("Failed to get remote wavelet %s", waveletname.toString()), e);
      } catch (NullPointerException e) {
        // This is a fairly normal case of it being a local-only wave.
        // Yet this only seems to appear in the test suite.
        // Continuing is completely harmless here.
        LOG.info(
            String.format("%s is definitely not a remote wavelet. (Null key)",
                waveletname.toString()), e);
      }

      if (waveletContainer == null) {
        try {
          if (LOG.isFineLoggable()) {
            LOG.fine("Trying as a local wavelet");
          }
          waveletContainer = waveMap.getLocalWavelet(waveletname);
        } catch (WaveletStateException e) {
          LOG.severe(String.format("Failed to get local wavelet %s", waveletname.toString()), e);
        }
      }

      // TODO (Yuri Z.) This loop collects all the wavelets that match the
      // query, so the view is determined by the query. Instead we should
      // look at the user's wave view and determine if the view matches the
      // query.
      try {
        if (waveletContainer == null || !waveletContainer.applyFunction(matchesFunction)) {
          LOG.fine("----doesn't match: " + waveletContainer);
          continue;
        }
        if (view == null) {
          view = WaveViewDataImpl.create(waveId);
        }
        // Just keep adding all the relevant wavelets in this wave.
        view.addWavelet(waveletContainer.copyWaveletData());
      } catch (WaveletStateException e) {
        LOG.warning("Failed to access wavelet " + waveletContainer.getWaveletName(), e);
      }
    }
    return view;
  }
}
