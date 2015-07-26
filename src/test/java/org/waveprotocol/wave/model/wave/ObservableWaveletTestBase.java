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

import org.waveprotocol.wave.model.testing.FakeWaveletListener;

/**
 * Black-box test of the {@link ObservableWavelet} interface. This only
 * tests parts specific to {@code ObservableWavelet}.
 *
 * To create a concrete test case, subclass this with an appropriate factory for
 * creating an {@code ObservableWavelet}.
 *
 */
public abstract class ObservableWaveletTestBase extends TestCase {
  /** Stub listener to receive events from wavelet being tested. */
  private FakeWaveletListener listener;

  private ObservableWavelet target;

  /**
   * Creates an observable wavelet for testing.
   */
  protected abstract ObservableWavelet createWavelet();

  @Override
  protected void setUp() throws Exception{
    super.setUp();
    target = createWavelet();
    listener = new FakeWaveletListener();
    target.addListener(listener);
  }

  public void testAddedListenersGetEvents() {
    target.addParticipant(new ParticipantId("new guy"));
    assertNotNull(listener.getParticipant());
  }

  public void testRemovedListenersStopGettingEvents() {
    target.removeListener(listener);
    target.addParticipant(new ParticipantId("new guy"));
    assertNull(listener.getParticipant());
  }

  public void testAddParticipantEventsAreFired() {
    ParticipantId p = new ParticipantId("new guy");
    target.addParticipant(p);
    assertEquals(p, listener.getParticipant());
  }

  // TODO(user): Consider the following tests
  // testVersionAndTimestampIsMonotonic
  // testOnlyWaveParticipantsCanContributeToBlips
  // testCanNotSetBlipLastModifiedVersionHigherThanWaveVersion

}
