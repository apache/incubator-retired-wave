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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.testing.Factory;
import org.waveprotocol.wave.model.testing.GenericTestBase;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 *
 *         TODO(user): tests for ObservablePrimitiveSupplement
 */
public abstract class PrimitiveSupplementTestBase extends GenericTestBase<PrimitiveSupplement> {

  protected PrimitiveSupplementTestBase(Factory<? extends PrimitiveSupplement> factory) {
    super(factory);
  }

  // These tests are suppressed because the test setup
  // can't approporiately hook up the op-based wavelet as a document
  // listener. Enable when either the op-based wavelet installs itself
  // as a listener or the model test setup is refactored to allow this.
  public void testDefaults() {

    // Default read versions are all NO_VERSION
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    String blip1 = "blip1";
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadParticipantsVersion(wavelet1));
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadWaveletVersion(wavelet1));

    // Default folder collection is.... {} ?
    assertFalse(target.getFolders().iterator().hasNext());

    // Default is no followed-state and no archive (i.e: in inbox).
    assertNull(target.getFollowed());
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getArchiveWaveletVersion(wavelet1));

    // Default is that there is no pending notification.
    assertFalse(target.getPendingNotification());

    // Gadget states are not defined.
    assertTrue(target.getGadgetState("Gadget #1").isEmpty());
  }

  public void testInboxing() {
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    WaveletId wavelet2 = WaveletId.of("google.com", "wavelet2");

    target.archiveAtVersion(wavelet1, 10);
    assertEquals(10, target.getArchiveWaveletVersion(wavelet1));
    target.archiveAtVersion(wavelet2, 5);
    assertEquals(5, target.getArchiveWaveletVersion(wavelet2));

    target.clearArchiveState();
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getArchiveWaveletVersion(wavelet1));
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getArchiveWaveletVersion(wavelet2));
  }

  public void testClearReadState() {
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    WaveletId wavelet2 = WaveletId.of("google.com", "wavelet2");
    String blip1 = "blip1";
    String blip2 = "blip2";

    target.setLastReadBlipVersion(wavelet1, blip1, 23);
    target.setLastReadBlipVersion(wavelet2, blip2, 43);
    target.clearReadState();
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadBlipVersion(wavelet2, blip2));

    target.setLastReadBlipVersion(wavelet1, blip1, 34);
    assertEquals(34, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadBlipVersion(wavelet2, blip2));
  }

  public void testClearBlipReadState() {
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    String blip1 = "blip1";
    String blip2 = "blip2";

    target.setLastReadBlipVersion(wavelet1, blip1, 23);
    target.setLastReadBlipVersion(wavelet1, blip2, 43);
    assertEquals(23, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(43, target.getLastReadBlipVersion(wavelet1, blip2));

    target.clearBlipReadState(wavelet1, blip1);
    assertEquals(PrimitiveSupplement.NO_VERSION, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(43, target.getLastReadBlipVersion(wavelet1, blip2));

    target.setLastReadBlipVersion(wavelet1, blip1, 10);
    assertEquals(10, target.getLastReadBlipVersion(wavelet1, blip1));
  }

  public void testSeenVersions() {
    WaveletId wavelet1 = WaveletId.of("example.com", "wavelet1");
    WaveletId wavelet2 = WaveletId.of("example.com", "wavelet2");
    HashedVersion aSeenVersion = HashedVersion.of(213819238L, new byte[] {1});

    target.setSeenVersion(wavelet1, aSeenVersion);

    assertEquals(aSeenVersion, target.getSeenVersion(wavelet1));
    assertEquals(HashedVersion.unsigned(0), target.getSeenVersion(wavelet2));

    assertEquals(CollectionUtils.immutableSet(wavelet1), target.getSeenWavelets());
  }

  //
  // Since the primitive supplement interface is almost entirely syntactic,
  // there is no
  // non-trivial behaviour to test. So instead of testing specific things (of
  // which there are
  // too many to test), we test some random scenarios.
  //

  public void testRandom() {
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    WaveletId wavelet2 = WaveletId.of("google.com", "wavelet2");
    String blip1 = "blip1";
    String blip2 = "blip2";
    String thread1 = "thread1";
    String thread2 = "thread2";

    target.setLastReadBlipVersion(wavelet1, blip1, 7);
    assertEquals(7, target.getLastReadBlipVersion(wavelet1, blip1));

    target.setLastReadBlipVersion(wavelet1, blip1, 5);
    assertEquals(7, target.getLastReadBlipVersion(wavelet1, blip1));

    target.setLastReadBlipVersion(wavelet1, blip2, 15);
    assertEquals(7, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(15, target.getLastReadBlipVersion(wavelet1, blip2));

    target.setLastReadBlipVersion(wavelet2, blip1, 8);
    assertEquals(7, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(15, target.getLastReadBlipVersion(wavelet1, blip2));
    assertEquals(8, target.getLastReadBlipVersion(wavelet2, blip1));

    target.setLastReadParticipantsVersion(wavelet1, 10);
    assertEquals(10, target.getLastReadParticipantsVersion(wavelet1));

    target.setLastReadWaveletVersion(wavelet2, 5);
    assertEquals(5, target.getLastReadWaveletVersion(wavelet2));

    target.setLastReadBlipVersion(wavelet2, blip1, 13);
    assertEquals(7, target.getLastReadBlipVersion(wavelet1, blip1));
    assertEquals(15, target.getLastReadBlipVersion(wavelet1, blip2));
    assertEquals(13, target.getLastReadBlipVersion(wavelet2, blip1));

    target.addFolder(3);
    assertEquals(Arrays.asList(3), target.getFolders());

    target.addFolder(5);
    assertEquals(Arrays.asList(3, 5), target.getFolders());

    target.addFolder(5);
    assertEquals(Arrays.asList(3, 5), target.getFolders());

    target.removeAllFolders();
    assertEquals(Collections.<Integer> emptyList(), target.getFolders());

    target.addFolder(5);
    assertEquals(Arrays.asList(5), target.getFolders());

    target.setNotifiedVersion(wavelet1, 10);
    target.setNotifiedVersion(wavelet2, 20);
    assertEquals(CollectionUtils.immutableSet(wavelet1, wavelet2), target.getNotifiedWavelets());
    assertEquals(10, target.getNotifiedVersion(wavelet1));
    assertEquals(20, target.getNotifiedVersion(wavelet2));
    assertFalse(target.getPendingNotification());
    HashedVersion aSeenVersion1 = HashedVersion.of(213819238L, new byte[] {10});
    HashedVersion aSeenVersion2 = HashedVersion.of(213819238L, new byte[] {20});
    target.setSeenVersion(wavelet1, aSeenVersion1);
    target.setSeenVersion(wavelet2, aSeenVersion2);
    assertFalse(target.getPendingNotification());

    assertEquals(null, target.getThreadState(wavelet1, thread1));
    assertEquals(null, target.getThreadState(wavelet1, thread2));

    target.setThreadState(wavelet1, thread1, ThreadState.EXPANDED);
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));
    assertEquals(null, target.getThreadState(wavelet2, thread1));

    target.setThreadState(wavelet1, thread1, ThreadState.COLLAPSED);
    assertEquals(ThreadState.COLLAPSED, target.getThreadState(wavelet1, thread1));

    target.setThreadState(wavelet1, thread1, ThreadState.EXPANDED);
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));

    target.setThreadState(wavelet1, thread2, ThreadState.COLLAPSED);
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));
    assertEquals(ThreadState.COLLAPSED, target.getThreadState(wavelet1, thread2));

    target.setThreadState(wavelet2, thread1, ThreadState.COLLAPSED);
    target.setThreadState(wavelet2, thread2, ThreadState.COLLAPSED);
    target.setThreadState(wavelet2, thread2, ThreadState.EXPANDED);
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));
    assertEquals(ThreadState.COLLAPSED, target.getThreadState(wavelet1, thread2));
    assertEquals(ThreadState.COLLAPSED, target.getThreadState(wavelet2, thread1));
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet2, thread2));

    target.setThreadState(wavelet2, thread1, null);
    assertEquals(null, target.getThreadState(wavelet2, thread1));
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet2, thread2));

    target.setThreadState(wavelet1, thread2, null);
    target.setThreadState(wavelet2, thread2, null);
    assertEquals(ThreadState.EXPANDED, target.getThreadState(wavelet1, thread1));
    assertEquals(null, target.getThreadState(wavelet2, thread2));
    assertEquals(null, target.getThreadState(wavelet2, thread1));
  }

  public void testAbuse() {
    WaveletId wavelet1 = WaveletId.of("google.com", "wavelet1");
    final String adder = "evilbob@evil.com";
    WantedEvaluation eval1 =
        new SimpleWantedEvaluation(
            wavelet1, adder, true, 0.1f, 1234L, "agent1", false, "no comment");
    WantedEvaluation eval2 =
        new SimpleWantedEvaluation(
            wavelet1, adder, true, 0.8f, 1256L, "agent2", true, "no comment");
    WantedEvaluation eval3 =
        new SimpleWantedEvaluation(
            wavelet1, adder, false, 0.5f, 1278L, "agent1", false, "no comment");

    Set<WantedEvaluation> all = CollectionUtils.newHashSet(eval1, eval2, eval3);

    // Starts empty
    assertTrue(target.getWantedEvaluations().isEmpty());

    // Add eval1
    target.addWantedEvaluation(eval1);
    assertEquals(1, target.getWantedEvaluations().size());

    // Add eval 2 and 3
    target.addWantedEvaluation(eval2);
    target.addWantedEvaluation(eval3);
    assertEquals(all, target.getWantedEvaluations());

    // Adding eval 1 again should have no actual effect
    target.addWantedEvaluation(eval1);
    assertEquals(all, target.getWantedEvaluations());
  }

  public void testGadgetStates() {
    String gadget1 = "Gadget 1";
    String gadget2 = "Gadget 2";

    String state1 = "State 1";
    String state2 = "State 2";

    assertTrue(target.getGadgetState(gadget1).isEmpty());
    assertTrue(target.getGadgetState(gadget2).isEmpty());

    target.setGadgetState(gadget1, state1, "State 1 in gadget 1");
    assertEquals(1, target.getGadgetState(gadget1).countEntries());
    assertEquals("State 1 in gadget 1", target.getGadgetState(gadget1).get(state1));
    assertTrue(target.getGadgetState(gadget2).isEmpty());

    target.setGadgetState(gadget2, state1, "State 1 in gadget 2");
    assertEquals(1, target.getGadgetState(gadget1).countEntries());
    assertEquals("State 1 in gadget 1", target.getGadgetState(gadget1).get(state1));
    assertEquals(1, target.getGadgetState(gadget2).countEntries());
    assertEquals("State 1 in gadget 2", target.getGadgetState(gadget2).get(state1));

    target.setGadgetState(gadget1, state2, "State 2 in gadget 1");
    target.setGadgetState(gadget2, state2, "State 2 in gadget 2");
    assertEquals(2, target.getGadgetState(gadget1).countEntries());
    assertEquals("State 1 in gadget 1", target.getGadgetState(gadget1).get(state1));
    assertEquals("State 2 in gadget 1", target.getGadgetState(gadget1).get(state2));
    assertEquals(2, target.getGadgetState(gadget2).countEntries());
    assertEquals("State 1 in gadget 2", target.getGadgetState(gadget2).get(state1));
    assertEquals("State 2 in gadget 2", target.getGadgetState(gadget2).get(state2));

    target.setGadgetState(gadget1, state1, null);
    target.setGadgetState(gadget2, state2, null);
    assertEquals(1, target.getGadgetState(gadget1).countEntries());
    assertEquals("State 2 in gadget 1", target.getGadgetState(gadget1).get(state2));
    assertEquals(1, target.getGadgetState(gadget2).countEntries());
    assertEquals("State 1 in gadget 2", target.getGadgetState(gadget2).get(state1));
  }

  private static <T> void assertEquals(Iterable<? extends T> a, Iterable<? extends T> b) {
    Iterator<? extends T> ai = a.iterator();
    Iterator<? extends T> bi = b.iterator();
    while (ai.hasNext() && bi.hasNext()) {
      assertEquals(ai.next(), bi.next());
    }
    assertEquals(ai.hasNext(), bi.hasNext());
  }
}
