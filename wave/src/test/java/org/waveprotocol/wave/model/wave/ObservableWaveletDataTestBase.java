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

import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.testing.FakeWaveletDataListener;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Collections;
import java.util.List;

/**
 * Black-box test of the {@link ObservableWaveletData} interface.  This only
 * tests the parts specific to {@code ObservableWaveData}
 *
 * To create a concrete test case, subclass this with an appropriate factory
 * for creating an {@code ObservableWaveData}.
 *
 */
public abstract class ObservableWaveletDataTestBase extends TestCase {

  /** Stub listener to receive events from wavelet being tested. */
  private FakeWaveletDataListener listener;

  private static final ParticipantId fred = new ParticipantId("fred@gwave.com");
  private static final ParticipantId jane = new ParticipantId("jane@gwave.com");
  private static final List<ParticipantId> noContributors = Collections.emptyList();

  private ObservableWaveletData target;

  /**
   * Creates an observable wavelet data for testing.
   */
  protected abstract ObservableWaveletData createWaveletData();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    target = createWaveletData();
    listener = new FakeWaveletDataListener();
    target.addListener(listener);
  }

  public void testAddedListenersGetEvents() {
    target.addParticipant(fred);
    assertNotNull(listener.getParticipantAdded());
  }

  public void testRemovedListenersStopGettingEvents() {
    target.removeListener(listener);
    target.addParticipant(fred);
    assertNull(listener.getParticipantRemoved());
  }

  public void testVersionChangeEventReceived() {
    long oldVersion = target.getVersion();
    target.setVersion(1234L);
    assertEquals(oldVersion, listener.getOldVersion());
    assertEquals(1234L, listener.getNewVersion());
  }

  public void testHashedVersionChangeEventReceived() {
    HashedVersion oldVersion = target.getHashedVersion();
    HashedVersion newVersion = HashedVersion.of(1234L, new byte[] {4, 4, 4, 4});
    target.setHashedVersion(newVersion);
    assertEquals(oldVersion, listener.getOldHashedVersion());
    assertEquals(newVersion, listener.getNewHashedVersion());
  }

  public void testAddParticipantEventReceived() {
    target.addParticipant(fred);
    assertEquals(fred, listener.getParticipantAdded());
  }

  public void testRemoveParticipantEventReceived() {
    target.addParticipant(fred);
    target.removeParticipant(fred);
    assertEquals(fred, listener.getParticipantRemoved());
  }

  public void testLastModifiedTimeChangedEventReceived() {
    long oldLastModifiedTime = target.getLastModifiedTime();
    target.setLastModifiedTime(45L);
    assertEquals(oldLastModifiedTime, listener.getOldLastModifiedTime());
    assertEquals(45L, listener.getNewLastModifiedTime());
  }

  public void testBlipDataAddedEventReceived() {
    BlipData blip1 = createBlip("b+one");
    assertEquals(blip1, listener.getBlipDataAdded());
    BlipData blip2 = createBlip("b+two");
    assertEquals(blip2, listener.getBlipDataAdded());
  }

  public void testBlipContributorAddedEventReceived() {
    BlipData root = createBlip();
    root.addContributor(jane);
    assertEquals(root, listener.getBlipModified());
    assertEquals(jane, listener.getBlipContributorAdded());
  }

  public void testBlipContributorRemovedEventReceived() {
    BlipData root = createBlip();
    root.addContributor(jane);
    root.removeContributor(jane);
    assertEquals(root, listener.getBlipModified());
    assertEquals(jane, listener.getBlipContributorRemoved());
  }

  public void testBlipTimestampChangedEventReceived() {
    BlipData root = createBlip();
    long oldTimestamp = root.getLastModifiedTime();
    root.setLastModifiedTime(47L);
    assertEquals(root, listener.getBlipModified());
    assertEquals(oldTimestamp, listener.getBlipOldTimestamp());
    assertEquals(47L, listener.getBlipNewTimestamp());
  }

  public void testBlipSubmitEventReceived() {
    BlipData root = createBlip();
    root.submit();
    assertEquals(root, listener.getBlipModified());
  }

  private BlipData createBlip() {
    return createBlip("b+default");
  }

  private BlipData createBlip(String blipId) {
    return target.createDocument(blipId, fred, noContributors, EmptyDocument.EMPTY_DOCUMENT, 0L, 0L);
  }
}
