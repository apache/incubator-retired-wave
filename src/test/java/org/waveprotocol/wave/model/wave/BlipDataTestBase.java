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

import org.waveprotocol.wave.model.wave.data.BlipData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test for implementations of the BlipData interface. Subclass and provide
 * a factory for your BlipData implementation.
 *
 * Like {@code WaveletDataTest} these tests should be simple.
 *
 * @author anorth@google.com (Alex North)
 * @see WaveletDataTestBase
 */
public abstract class BlipDataTestBase extends TestCase {

  private BlipData target;

  /**
   * Creates a blip data instance for testing.
   */
  protected abstract BlipData createBlipData();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    target = createBlipData();
  }

  public void testGetWave() {
    assertNotNull(target.getWavelet());
    assertEquals(target.getWavelet().getDocument(target.getId()), target);
  }

  public void testBlipHasId() {
    assertNotNull(target.getId());
  }

  public void testBlipAuthor() {
    assertNotNull(target.getAuthor());
  }

  public void testNewBlipHasNoContributors() {
    assertEquals(0, target.getContributors().size());
  }

  public void testAddContributorsRetreivedInOrder() {
    List<ParticipantId> contributors = Arrays.asList(
        new ParticipantId("fred@gwave.com"),
        new ParticipantId("jane@gwave.com"));
    for (ParticipantId p : contributors) {
      target.addContributor(p);
    }
    List<ParticipantId> result = new ArrayList<ParticipantId>();
    for (ParticipantId p : target.getContributors()) {
      result.add(p);
    }
    assertEquals(contributors, result);
  }

  public void testRemoveContributors() {
    ParticipantId fred = new ParticipantId("fred@gwave.com");
    ParticipantId jane = new ParticipantId("jane@gwave.com");
    target.addContributor(fred);
    target.addContributor(fred); // Added twice, should be completely removed.
    target.addContributor(jane);

    target.removeContributor(fred);
    assertEquals(Collections.singleton(jane), target.getContributors());
    target.removeContributor(jane);
    assertEquals(Collections.emptySet(), target.getContributors());
  }

  public void testRemoveInvalidContributorIgnored() {
    ParticipantId fred = new ParticipantId("fred@gwave.com");
    ParticipantId jane = new ParticipantId("jane@gwave.com");
    target.addContributor(fred);
    target.removeContributor(jane);
    assertEquals(Collections.singleton(fred), target.getContributors());
  }

  public void testSetLastModifiedTime() {
    long lmt = target.getLastModifiedTime() + 42;
    target.setLastModifiedTime(lmt);
    assertEquals(lmt, target.getLastModifiedTime());
  }

  public void testLastModifiedVersion() {
    long lmv = target.getLastModifiedVersion() + 23;
    target.setLastModifiedVersion(lmv);
    assertEquals(lmv, target.getLastModifiedVersion());
  }

  public void testEmptyBlipHasContent() {
    assertNotNull(target.getContent());
  }
}
