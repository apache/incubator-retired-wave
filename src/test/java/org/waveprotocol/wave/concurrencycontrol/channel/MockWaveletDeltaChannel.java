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

package org.waveprotocol.wave.concurrencycontrol.channel;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.testing.CcTestingUtils;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A mock WaveletDeltaChannel.
 *
 * @author anorth@google.com (Alex North)
 */
public class MockWaveletDeltaChannel implements WaveletDeltaChannel, WaveletChannel.Listener {

  private static enum Method { RESET, SEND, WAVELET_UPDATE, WAVELET_SNAPSHOT }

  private final Queue<Object[]> expectations = new LinkedList<Object[]>();

  public void expectReset(Receiver r) {
    expectations.add(new Object[] {Method.RESET, r});
  }

  public void expectSend(WaveletDelta delta) {
    expectations.add(new Object[] {Method.SEND, delta});
  }

  public void checkExpectationsSatisfied() {
    assertTrue("Unsatisfied delta channel expectations", expectations.isEmpty());
  }

  @Override
  public void reset(Receiver receiver) {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.RESET);
    assertEquals(expected[1], receiver);
  }

  @Override
  public void send(Transmitter transmitter) {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.SEND);
    assertTrue(expected[1] + " vs. " + transmitter.takeMessage().getDelta(),
        CcTestingUtils.deltasAreEqual((WaveletDelta) expected[1],
        transmitter.takeMessage().getDelta()));
  }

  @Override
  public void onWaveletSnapshot(ObservableWaveletData snapshot,
      HashedVersion lastCommittedVersion, HashedVersion currentSignedVersion)
      throws ChannelException {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.WAVELET_SNAPSHOT);
    assertEquals(expected[1], snapshot);
    assertEquals(expected[2], lastCommittedVersion);
    assertEquals(expected[3], currentSignedVersion);
  }

  @Override
  public void onWaveletUpdate(List<TransformedWaveletDelta> deltas,
      HashedVersion lastCommittedVersion, HashedVersion currentSignedVersion) {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.WAVELET_UPDATE);
    assertEquals(expected[1], deltas);
    assertEquals(expected[2], lastCommittedVersion);
    assertEquals(expected[3], currentSignedVersion);
  }

  @Override
  public String debugGetProfilingInfo() {
    return null;
  }

}
