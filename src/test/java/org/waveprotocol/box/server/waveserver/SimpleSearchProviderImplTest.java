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

import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gxp.com.google.common.collect.Maps;
import com.google.wave.api.SearchResult;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class SimpleSearchProviderImplTest extends TestCase {

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = WaveId.of(DOMAIN, "abc123");
  private static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "conv+root");
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  private static final ParticipantId USER1 = ParticipantId.ofUnsafe("user1@" + DOMAIN);
  private static final ParticipantId USER2 = ParticipantId.ofUnsafe("user2@" + DOMAIN);
  private static final ParticipantId SHARED_USER = ParticipantId.ofUnsafe("@" + DOMAIN);

  private static final WaveletOperationContext CONTEXT =
      new WaveletOperationContext(USER1, 1234567890, 1);

  private static final HashedVersionFactory V0_HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(new IdURIEncoderDecoder(new JavaUrlCodec()));

  private final HashMultimap<WaveId,WaveletId> wavesViewUser1 = HashMultimap.create();
  private final HashMultimap<WaveId,WaveletId> wavesViewUser2 = HashMultimap.create();
  private final HashMultimap<WaveId,WaveletId> wavesViewUser3 = HashMultimap.create();

  private final Map<ParticipantId, HashMultimap<WaveId,WaveletId>> wavesViews = Maps.newHashMap();

  /** Sorts search result in ascending order by LMT. */
  static final Comparator<SearchResult.Digest> ASCENDING_DATE_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          long lmt0 = arg0.getLastModified();
          long lmt1 = arg1.getLastModified();
          return Long.signum(lmt0 - lmt1);
        }
      };

  /** Sorts search result in descending order by LMT. */
  static final Comparator<SearchResult.Digest> DESCENDING_DATE_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          return -ASCENDING_DATE_COMPARATOR.compare(arg0, arg1);
        }
      };

  /** Sorts search result in ascending order by creation time. */
  static final Comparator<SearchResult.Digest> ASC_CREATED_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          long time0 = arg0.getCreated();
          long time1 = arg1.getCreated();
          return Long.signum(time0 - time1);
        }
      };

  /** Sorts search result in descending order by creation time. */
  static final Comparator<SearchResult.Digest> DESC_CREATED_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          return -ASC_CREATED_COMPARATOR.compare(arg0, arg1);
        }
      };

  /** Sorts search result in ascending order by author. */
  static final Comparator<SearchResult.Digest> ASC_CREATOR_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          ParticipantId author0 = computeAuthor(arg0);
          ParticipantId author1 = computeAuthor(arg1);
          return author0.compareTo(author1);
        }

        private ParticipantId computeAuthor(SearchResult.Digest digest) {
          ParticipantId author;
          author = ParticipantId.ofUnsafe(digest.getParticipants().get(0));
          assert author != null : "Cannot find author for the wave: " + digest.getWaveId();
          return author;
        }
      };

  /** Sorts search result in descending order by author. */
  static final Comparator<SearchResult.Digest> DESC_CREATOR_COMPARATOR =
      new Comparator<SearchResult.Digest>() {
        @Override
        public int compare(SearchResult.Digest arg0, SearchResult.Digest arg1) {
          return -ASC_CREATOR_COMPARATOR.compare(arg0, arg1);
        }
      };

  private WaveletOperation addParticipantToWavelet(ParticipantId user, WaveletName name) {
    addWaveletToUserView(name, user);
    return new AddParticipant(CONTEXT, user);
  }

  @Mock private IdGenerator idGenerator;
  @Mock private WaveletNotificationDispatcher notifiee;
  @Mock private DeltaAndSnapshotStore waveletStore;
  @Mock private RemoteWaveletContainer.Factory remoteWaveletContainerFactory;
  @Mock private PerUserWaveViewProvider waveViewProvider;

  private SearchProvider searchProvider;
  private WaveMap waveMap;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    wavesViews.put(USER1, wavesViewUser1);
    wavesViews.put(USER2, wavesViewUser2);
    wavesViews.put(SHARED_USER, wavesViewUser3);

    when(waveViewProvider.retrievePerUserWaveView(USER1)).thenReturn(wavesViewUser1);
    when(waveViewProvider.retrievePerUserWaveView(USER2)).thenReturn(wavesViewUser2);
    when(waveViewProvider.retrievePerUserWaveView(SHARED_USER)).thenReturn(wavesViewUser3);

    ConversationUtil conversationUtil = new ConversationUtil(idGenerator);
    WaveDigester digester = new WaveDigester(conversationUtil);

    final DeltaStore deltaStore = new MemoryDeltaStore();
    final Executor persistExecutor = MoreExecutors.sameThreadExecutor();
    final Executor storageContinuationExecutor = MoreExecutors.sameThreadExecutor();
    final Executor lookupExecutor = MoreExecutors.sameThreadExecutor();
    LocalWaveletContainer.Factory localWaveletContainerFactory =
        new LocalWaveletContainer.Factory() {
          @Override
          public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
              WaveletName waveletName, String domain) {
            WaveletState waveletState;
            try {
              waveletState = DeltaStoreBasedWaveletState.create(deltaStore.open(waveletName),
                  persistExecutor);
            } catch (PersistenceException e) {
              throw new RuntimeException(e);
            }
            return new LocalWaveletContainerImpl(waveletName, notifiee,
                Futures.immediateFuture(waveletState), DOMAIN, storageContinuationExecutor);
          }
        };

    waveMap =
        new WaveMap(waveletStore, notifiee, localWaveletContainerFactory,
            remoteWaveletContainerFactory, DOMAIN, lookupExecutor);

    searchProvider = new SimpleSearchProviderImpl(DOMAIN, digester, waveMap, waveViewProvider);
  }

  @Override
  protected void tearDown() throws Exception {
    wavesViews.clear();
  }

  public void testSearchEmptyInboxReturnsNothing() {
    SearchResult results = searchProvider.search(USER1, "in:inbox", 0, 20);

    assertEquals(0, results.getNumResults());
  }

  public void testSearchInboxReturnsWaveWithExplicitParticipant() throws Exception {
    submitDeltaToNewWavelet(WAVELET_NAME, USER1, addParticipantToWavelet(USER2, WAVELET_NAME));

    SearchResult results = searchProvider.search(USER2, "in:inbox", 0, 20);

    assertEquals(1, results.getNumResults());
    assertEquals(WAVELET_NAME.waveId.serialise(), results.getDigests().get(0).getWaveId());
  }

  public void testSearchInboxDoesNotReturnWaveWithoutUser() throws Exception {
    submitDeltaToNewWavelet(WAVELET_NAME, USER1, addParticipantToWavelet(USER1, WAVELET_NAME));

    SearchResult results = searchProvider.search(USER2, "in:inbox", 0, 20);
    assertEquals(0, results.getNumResults());
  }

  public void testSearchWaveReturnsWaveWithImplicitParticipant() throws Exception {
    ParticipantId sharedDomainParticipantId =
        ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(DOMAIN);
    WaveletName waveletName =
      WaveletName.of(WaveId.of(DOMAIN, String.valueOf(1)), WAVELET_ID);
    // Implicit participant in this wave.
    submitDeltaToNewWavelet(waveletName, USER1,
        addParticipantToWavelet(sharedDomainParticipantId, waveletName));
    waveletName = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(2)), WAVELET_ID);
    // Explicit participant in this wave.
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER2, waveletName));

    SearchResult results = searchProvider.search(USER2, "", 0, 20);
    // Should return both waves.
    assertEquals(2, results.getNumResults());
  }

  public void testSearchAllReturnsWavesOnlyWithSharedDomainUser() throws Exception {
    WaveletName waveletName =
      WaveletName.of(WaveId.of(DOMAIN, String.valueOf(1)), WAVELET_ID);
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER1, waveletName));
    waveletName = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(2)), WAVELET_ID);
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER2, waveletName));

    SearchResult results = searchProvider.search(USER2, "", 0, 20);
    assertEquals(1, results.getNumResults());
  }

  public void testSearchLimitEnforced() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, "w" + i), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }

    SearchResult results = searchProvider.search(USER1, "in:inbox", 0, 5);

    assertEquals(5, results.getNumResults());
  }

  public void testSearchIndexWorks() throws Exception {
    // For this test, we'll create 10 waves with wave ids "0", "1", ... "9" and then run 10
    // searches using offsets 0..9. The waves we get back can be in any order, but we must get
    // all 10 of the waves back exactly once each from the search query.

    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }

    // The number of times we see each wave when we search
    int[] saw_wave = new int[10];

    for (int i = 0; i < 10; i++) {
      SearchResult results = searchProvider.search(USER1, "in:inbox", i, 1);
      assertEquals(1, results.getNumResults());
      WaveId waveId = WaveId.deserialise(results.getDigests().get(0).getWaveId());
      int index = Integer.parseInt(waveId.getId());
      saw_wave[index]++;
    }

    for (int i = 0; i < 10; i++) {
      // Each wave should appear exactly once in the results
      assertEquals(1, saw_wave[i]);
    }
  }

  public void testSearchOrderByAscWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }
    SearchResult results = searchProvider.search(USER1, "in:inbox orderby:dateasc", 0, 10);
    Ordering<SearchResult.Digest> ascOrdering = Ordering.from(ASCENDING_DATE_COMPARATOR);
    assertTrue(ascOrdering.isOrdered(results.getDigests()));
  }

  public void testSearchOrderByDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }
    SearchResult results = searchProvider.search(USER1, "in:inbox orderby:datedesc", 0, 10);
    Ordering<SearchResult.Digest> descOrdering = Ordering.from(DESCENDING_DATE_COMPARATOR);
    assertTrue(descOrdering.isOrdered(results.getDigests()));
  }

  public void testSearchOrderByCreatedAscWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }
    SearchResult results = searchProvider.search(USER1, "in:inbox orderby:createdasc", 0, 10);
    Ordering<SearchResult.Digest> ascOrdering = Ordering.from(ASC_CREATED_COMPARATOR);
    assertTrue(ascOrdering.isOrdered(results.getDigests()));
  }

  public void testSearchOrderByCreatedDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }
    SearchResult results = searchProvider.search(USER1, "in:inbox orderby:createddesc", 0, 10);
    Ordering<SearchResult.Digest> descOrdering = Ordering.from(DESC_CREATED_COMPARATOR);
    assertTrue(descOrdering.isOrdered(results.getDigests()));
  }

  public void testSearchOrderByAuthorAscWithCompundingWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1, name);
        WaveletOperation op2 = addParticipantToWavelet(USER2, name);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER2, addParticipantToWavelet(USER2, name));
      }
    }
    SearchResult resultsAsc =
        searchProvider.search(USER2, "in:inbox orderby:creatorasc orderby:createddesc", 0, 10);
    assertEquals(10, resultsAsc.getNumResults());
    Ordering<SearchResult.Digest> ascAuthorOrdering = Ordering.from(ASC_CREATOR_COMPARATOR);
    assertTrue(ascAuthorOrdering.isOrdered(resultsAsc.getDigests()));
    Ordering<SearchResult.Digest> descCreatedOrdering = Ordering.from(DESC_CREATED_COMPARATOR);
    // The whole list should not be ordered by creation time.
    assertFalse(descCreatedOrdering.isOrdered(resultsAsc.getDigests()));
    // Each sublist should be ordered by creation time.
    assertTrue(descCreatedOrdering.isOrdered(Lists.newArrayList(resultsAsc.getDigests()).subList(0,
        2)));
    assertTrue(descCreatedOrdering.isOrdered(Lists.newArrayList(resultsAsc.getDigests()).subList(2,
        10)));
  }

  public void testSearchOrderByAuthorDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1, name);
        WaveletOperation op2 = addParticipantToWavelet(USER2, name);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER2, addParticipantToWavelet(USER2, name));
      }
    }
    SearchResult resultsAsc =
        searchProvider.search(USER2, "in:inbox orderby:creatordesc", 0, 10);
    assertEquals(10, resultsAsc.getNumResults());
    Ordering<SearchResult.Digest> descAuthorOrdering = Ordering.from(DESC_CREATOR_COMPARATOR);
    assertTrue(descAuthorOrdering.isOrdered(resultsAsc.getDigests()));
  }

  public void testSearchFilterByWithWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1, name);
        WaveletOperation op2 = addParticipantToWavelet(USER2, name);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
      }
    }
    SearchResult results =
        searchProvider.search(USER1, "in:inbox with:" + USER2.getAddress(), 0, 10);
    assertEquals(2, results.getNumResults());
    results = searchProvider.search(USER1, "in:inbox with:" + USER1.getAddress(), 0, 10);
    assertEquals(10, results.getNumResults());
  }

  /**
   * If query contains invalid search param - it should return empty result.
   */
  public void testInvalidWithSearchParam() throws Exception {
    WaveletName name = WaveletName.of(WAVE_ID, WAVELET_ID);
    submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    SearchResult results =
        searchProvider.search(USER1, "in:inbox with@^^^@:" + USER1.getAddress(), 0, 10);
    assertEquals(0, results.getNumResults());
  }

  public void testInvalidOrderByParam() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
    }
    SearchResult results =
        searchProvider.search(USER1, "in:inbox orderby:createddescCCC", 0, 10);
    assertEquals(0, results.getNumResults());
  }

  public void testSearchFilterByCreatorWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves as creator.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1, name);
        WaveletOperation op2 = addParticipantToWavelet(USER2, name);
        submitDeltaToNewWavelet(name, USER2, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1, name));
      }
    }
    SearchResult results =
        searchProvider.search(USER1, "in:inbox creator:" + USER2.getAddress(), 0, 10);
    assertEquals(2, results.getNumResults());
    results = searchProvider.search(USER1, "in:inbox creator:" + USER1.getAddress(), 0, 10);
    assertEquals(8, results.getNumResults());
    results =
        searchProvider.search(USER1,
            "in:inbox creator:" + USER1.getAddress() + " creator:" + USER2.getAddress(), 0, 10);
    assertEquals(0, results.getNumResults());
  }

  // *** Helpers

  private void submitDeltaToNewWavelet(WaveletName name, ParticipantId user,
      WaveletOperation... ops) throws Exception {

    HashedVersion version = V0_HASH_FACTORY.createVersionZero(name);
    WaveletDelta delta = new WaveletDelta(user, version, Arrays.asList(ops));
    addWaveletToUserView(name, user);


    ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);

    // Submitting the request will require the certificate manager to sign the delta. We'll just
    // leave it unsigned.
    ProtocolSignedDelta signedProtoDelta =
        ProtocolSignedDelta.newBuilder().setDelta(protoDelta.toByteString()).build();

    LocalWaveletContainer wavelet = waveMap.getOrCreateLocalWavelet(name);
    wavelet.submitRequest(name, signedProtoDelta);
  }

  private void addWaveletToUserView(WaveletName name, ParticipantId user) {
    HashMultimap<WaveId,WaveletId> wavesView = wavesViews.get(user);
    if (!wavesView.containsEntry(name.waveId, name.waveletId)) {
      wavesViews.get(user).put(name.waveId, name.waveletId);
    }
  }
}
