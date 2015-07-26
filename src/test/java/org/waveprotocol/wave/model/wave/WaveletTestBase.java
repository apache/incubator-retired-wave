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

package org.waveprotocol.wave.model.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Black-box test of the {@link Wavelet} interface.
 *
 * To create a concrete test case, subclass this with an appropriate factory
 * for creating an {@code Wave}.
 *
 * @author zdwang@google.com (David Wang)
 */
public abstract class WaveletTestBase extends TestCase {

  private static String DOCUMENT_NAME = "aDocumentName";
  private static String ANOTHER_DOCUMENT_NAME = "anotherDocumentName";

  private Wavelet target;

  /**
   * Creates a wavelet for testing.
   */
  protected abstract Wavelet createWavelet();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    target = createWavelet();
    // Wavelets must have their creator added as the first op.
    target.addParticipant(target.getCreatorId());
  }

  public void testNewWaveHasCreatorIdButNoParticipants() {
    // We set things up for most tests so that the creator is already added, so
    // need to get a cleaner target wavelet for this test.
    target = createWavelet();
    // Test new wavelet has creator id
    assertNotNull(target.getCreatorId());
    // The creator should NOT automatically be a participant
    assertTrue(target.getParticipantIds().isEmpty());
  }

  public void testAddOneParticipant() {
    ParticipantId fake = new ParticipantId("bill@foo.com");
    target.addParticipant(fake);

    List<ParticipantId> expected = Arrays.asList(target.getCreatorId(), fake);
    assertEquals(expected, CollectionUtils.newArrayList(target.getParticipantIds()));
  }

  public void testParticipantsCollectionIsASet() {
    ParticipantId fake1 = new ParticipantId("joe");
    ParticipantId fake2 = new ParticipantId("bill");
    List<ParticipantId> fakes = CollectionUtils.newArrayList();
    fakes.add(target.getCreatorId());
    fakes.add(fake1);
    fakes.add(fake2);

    target.addParticipant(fake1);
    target.addParticipant(fake2);

    // Check that wavelet contains both fakes, and no others.
    assertEquals(fakes, CollectionUtils.newArrayList(target.getParticipantIds()));

    // Attempt re-add of fake2
    target.addParticipant(fake2);
    assertEquals(fakes, CollectionUtils.newArrayList(target.getParticipantIds()));

    // Attempt re-add of fake1
    target.addParticipant(fake1);
    assertEquals(fakes, CollectionUtils.newArrayList(target.getParticipantIds()));
  }

  public void testAddManyParticipants() {
    Set<ParticipantId> participants = CollectionUtils.newHashSet(target.getCreatorId());

    // Add 100 Participants
    for (int i = 0; i < 10; i++) {
      ParticipantId participant = new ParticipantId("participant" + i + "@foo.com");
      participants.add(participant);
      target.addParticipant(participant);
    }

    assertEquals(participants, target.getParticipantIds());
  }

  public void testCanGetDocuments() {
    MutableDocument<?,?,?> doc = target.getDocument(DOCUMENT_NAME);
    assertNotNull(doc);
    MutableDocument<?,?,?> doc2 = target.getDocument(DOCUMENT_NAME);
    assertEquals(doc, doc2);
    assertEquals(Collections.singleton(DOCUMENT_NAME), target.getDocumentIds());
    doc2 = target.getDocument(ANOTHER_DOCUMENT_NAME);
    assertNotSame(doc, doc2);
    assertEquals(2, target.getDocumentIds().size());
    assertTrue(target.getDocumentIds().contains(DOCUMENT_NAME));
    assertTrue(target.getDocumentIds().contains(ANOTHER_DOCUMENT_NAME));
  }
}
