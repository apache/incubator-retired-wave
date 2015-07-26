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

package org.waveprotocol.box.server.frontend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link UserManager}.
 */
public class UserManagerTest extends TestCase {
  private static final WaveId W1 = WaveId.of("example.com", "111");
  private static final WaveId W2 = WaveId.of("example.com", "222");

  private static final WaveletId WA = WaveletId.of("example.com", "AAA");
  private static final WaveletId WB = WaveletId.of("example.com", "BBB");

  private static final WaveletName W1A = WaveletName.of(W1, WA);
  private static final WaveletName W2A = WaveletName.of(W2, WA);
  private static final WaveletName W2B = WaveletName.of(W2, WB);

  private static final ParticipantId USER = ParticipantId.ofUnsafe("user@host.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(USER);

  private static final HashedVersion V2 = HashedVersion.unsigned(2);
  private static final HashedVersion V3 = HashedVersion.unsigned(3);

  private static final TransformedWaveletDelta DELTA = UTIL.makeTransformedDelta(0L, V2, 2);
  private static final DeltaSequence DELTAS = DeltaSequence.of(DELTA);

  private UserManager m;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new UserManager();
  }

  // TODO(arb): add tests testing subscriptions with channelIds.

  /**
   * Tests that {@link UserManager#matchSubscriptions(WaveletName)} accurately
   * reflects subscription, independent of whether we actually are on any
   * wavelets.
   */
  public void testMatchSubscriptions() {
    assertEquals(ImmutableList.<OpenListener>of(), m.matchSubscriptions(W1A));

    OpenListener l1 = mock(OpenListener.class, "listener 1");
    OpenListener l2 = mock(OpenListener.class, "listener 2");
    OpenListener l3 = mock(OpenListener.class, "listener 3");
    OpenListener l4 = mock(OpenListener.class, "listener 4");
    OpenListener l5 = mock(OpenListener.class, "listener 5");
    String channelId = "";

    m.subscribe(W2, IdFilter.ofIds(WA), channelId, l1);
    m.subscribe(W2, IdFilters.ALL_IDS, channelId, l2);
    m.subscribe(W1, IdFilter.ofPrefixes("", WA.getId()), channelId, l3);
    m.subscribe(W2, IdFilters.NO_IDS, channelId, l4);
    m.subscribe(W2, IdFilter.ofPrefixes("A", "B"), channelId, l5);

    checkListenersMatchSubscriptions(ImmutableList.of(l1, l2, l5), m.matchSubscriptions(W2A));
    checkListenersMatchSubscriptions(ImmutableList.of(l2, l5), m.matchSubscriptions(W2B));
  }

  /**
   * Method to check whether the given subscriptions contain exactly the expected
   * {@link OpenListener}s.
   *
   * @param expectedListeners the {@link List} of {@link OpenListener}s we are
   *        expecting
   * @param matchedSubscriptions the {@link List} of subscriptions to get the
   *        {@link OpenListener} from
   */
  private void checkListenersMatchSubscriptions(List<OpenListener> expectedListeners,
      List<WaveViewSubscription> matchedSubscriptions) {
    List<OpenListener> actualListeners = new ArrayList<OpenListener>();
    for (WaveViewSubscription subscription : matchedSubscriptions) {
      actualListeners.add(subscription.getOpenListener());
    }
    assertEquals(expectedListeners, actualListeners);
  }

  public void testEmptyDeltaNotReceived() {
    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch", listener);
    m.onUpdate(W1A, DeltaSequence.empty());
    verifyZeroInteractions(listener);
  }

  /**
   * Tests that a single delta update is received by the listener.
   */
  public void testSingleDeltaReceived() {
    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch", listener);
    m.onUpdate(W1A, DELTAS);
    verify(listener).onUpdate(W1A, null, DELTAS, null, null, "ch");
  }

  /**
   * Tests that multiple deltas are received.
   */
  public void testUpdateSeveralDeltas() {
    TransformedWaveletDelta delta2 = UTIL.noOpDelta(V2.getVersion());

    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch1", listener);

    DeltaSequence bothDeltas =  DeltaSequence.of(DELTA, delta2);
    m.onUpdate(W1A, bothDeltas);
    verify(listener).onUpdate(W1A, null, bothDeltas, null, null, "ch1");

    // Also succeeds when sending the two deltas via separate onUpdates()
    DeltaSequence delta2Sequence = DeltaSequence.of(delta2);
    m.subscribe(W2, IdFilters.ALL_IDS, "ch2", listener);
    m.onUpdate(W2A, DELTAS);
    m.onUpdate(W2A, DeltaSequence.of(delta2));
    verify(listener).onUpdate(W2A, null, DELTAS, null, null, "ch2");
    verify(listener).onUpdate(W2A, null, delta2Sequence, null, null, "ch2");
  }

  /**
   * Tests that delta updates are held back while a submit is in flight.
   */
  public void testDeltaHeldBackWhileOutstandingSubmit() {
    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch", listener);

    m.submitRequest("ch", W1A);
    m.onUpdate(W1A, DELTAS);
    verifyZeroInteractions(listener);

    m.submitResponse("ch", W1A, V3); // V3 not the same as update delta.
    verify(listener).onUpdate(W1A, null, DELTAS, null, null, "ch");
  }

  /**
   * Tests that a delta with an end version matching one submitted on this
   * channel is dropped.
   */
  public void testOwnDeltasAreDropped() {
    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch", listener);

    m.submitRequest("ch", W1A);
    m.submitResponse("ch", W1A, V2);
    m.onUpdate(W1A, DELTAS);
    verifyZeroInteractions(listener);
  }

  /**
   * Tests that a a delta with an end version matching one submitted on this
   * channel is dropped even if received before the submit completes.
   */
  public void testOwnDeltaDroppedAfterBeingHeldBack() {
    OpenListener listener = mock(OpenListener.class);
    m.subscribe(W1, IdFilters.ALL_IDS, "ch", listener);

    m.submitRequest("ch", W1A);
    m.onUpdate(W1A, DELTAS);
    m.submitResponse("ch", W1A, V2);
    verifyZeroInteractions(listener);
  }
}
