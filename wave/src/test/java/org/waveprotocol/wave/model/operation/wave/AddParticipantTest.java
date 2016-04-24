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

package org.waveprotocol.wave.model.operation.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link AddParticipant} and {@link RemoveParticipant}.
 *
 * TODO(user): rename to AddRemoveParticipantTest
 *
 * @author zdwang@google.com (David Wang)
 */

public class AddParticipantTest extends TestCase {
  private static final ParticipantId CREATOR = new ParticipantId("abc@example.com");
  private static final ParticipantId ANOTHER = new ParticipantId("def@example.com");
  private static final ParticipantId ATHIRD = new ParticipantId("xyz@example.com");

  private WaveletData wavelet;

  @Override
  public void setUp() {
    WaveId waveId = WaveId.of("example.com", "c+123");
    WaveletId waveletId = WaveletId.of("example.com", IdConstants.CONVERSATION_ROOT_WAVELET);
    wavelet = new WaveletDataImpl(waveletId, CREATOR, 0L, 0L,
        HashedVersion.unsigned(0), 0L, waveId,
        BasicFactories.pluggableMutableDocumentFactory());
  }

  /**
   * Test Adding participants using operations are ok.
   * @throws OperationException Not supposed to happen.
   */
  public void testAddManyParticipants() throws OperationException {
    // Check empty participants
    Set<ParticipantId> participants = new LinkedHashSet<ParticipantId>();
    assertEquals(participants, wavelet.getParticipants());

    AddParticipant op = new AddParticipant(createContext(), CREATOR);
    op.apply(wavelet);
    participants.add(CREATOR);

    // Check participant list
    assertEquals(participants, wavelet.getParticipants());

    // Add lots of participants
    for (int i = 0; i < 10; i++) {
      ParticipantId p = new ParticipantId("abc" + i + "@example.com");
      op = new AddParticipant(createContext(), p);
      participants.add(p);
      op.apply(wavelet);
    }

    // Check participant list
    assertEquals(participants, wavelet.getParticipants());
  }

  public void testCannotAddSameParticipantTwice() throws OperationException {
    new AddParticipant(createContext(), CREATOR, 0).apply(wavelet);
    verifyCurrentParticipants(CREATOR);
    try {
      new AddParticipant(createContext(), CREATOR, 0).apply(wavelet);
      fail("Cannot add a participant twice");
    } catch (OperationException expected) {
    }
    new AddParticipant(createContext(), ANOTHER, 0).apply(wavelet); // adding another is ok
  }

  public void testReverseOfAddParticipantIsRemoveParticipant() throws OperationException {
    assertEquals(
        Arrays.<WaveletOperation>asList(new RemoveParticipant(createContext(), CREATOR)),
        new AddParticipant(createContext(), CREATOR, 0).applyAndReturnReverse(wavelet));
  }

  public void testCannotRemoveNonParticipant() throws OperationException {
    try {
      new RemoveParticipant(createContext(), CREATOR).apply(wavelet);
      fail("Cannot remove a participant when there are none");
    } catch (OperationException expected) {
    }
    new AddParticipant(createContext(), CREATOR, 0).apply(wavelet);
    new RemoveParticipant(createContext(), CREATOR).apply(wavelet); // now it's ok to remove
    verifyCurrentParticipants();
    try {
      new RemoveParticipant(createContext(), CREATOR).apply(wavelet);
      fail("Cannot remove a participant twice in a row");
    } catch (OperationException expected) {
    }
  }

  public void testReverseOfRemoveParticipantIsAddParticipantWithPosition()
      throws OperationException {
    // Build participant list with 3 participants.
    List<ParticipantId> participants = Arrays.<ParticipantId>asList(CREATOR, ANOTHER, ATHIRD);
    for (ParticipantId p : participants) {
      wavelet.addParticipant(p);
    }
    assertEquals(participants, currentParticipantList());

    // The reverse of removing any of the participants is an AddParticipant with the
    // correct position which, when applied, rolls back the participant list.
    for (int i = 0; i < participants.size(); i++) {
      ParticipantId p = participants.get(i);
      List<? extends WaveletOperation> reverse =
          new RemoveParticipant(createContext(), p).applyAndReturnReverse(wavelet);
      assertEquals(
        Arrays.<WaveletOperation>asList(new AddParticipant(createContext(), p, i)),
        reverse);
      reverse.get(0).apply(wavelet);
      assertEquals(participants, currentParticipantList());
    }
  }

  /**
   * Creates a dummy context for an operation.
   */
  private static WaveletOperationContext createContext() {
    return new WaveletOperationContext(CREATOR, -1L, 1L);
  }

  private List<ParticipantId> currentParticipantList() {
    return new ArrayList<ParticipantId>(wavelet.getParticipants());
  }

  private void verifyCurrentParticipants(ParticipantId... participants) {
    assertEquals(Arrays.asList(participants), currentParticipantList());
  }
}
