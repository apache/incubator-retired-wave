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
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.concurrencycontrol.testing.CcTestingUtils;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A mock view channel which allows setting of expectations.
 *
 * @author anorth@google.com (Alex North)
 */
public final class MockViewChannel implements ViewChannel {
  /**
   * Factory for mock view channels. Provides access to the created channels.
   */
  public static class Factory implements ViewChannelFactory {
    private final Queue<MockViewChannel> channels = new LinkedList<MockViewChannel>();

    /**
     * Expects a channel creation.
     *
     * @return the channel which will be returned at {@link #create(WaveId)}
     */
    public MockViewChannel expectCreate() {
      MockViewChannel ch = new MockViewChannel();
      channels.add(ch);
      return ch;
    }

    @Override
    public ViewChannel create(WaveId waveId) {
      return channels.remove();
    }
  }

  private enum Method { OPEN, SUBMIT, CLOSE }

  private final Queue<Object[]> expectations = new LinkedList<Object[]>();
  private final Queue<Listener> listeners = new LinkedList<Listener>();
  private final Queue<SubmitCallback> awaitingAck = new LinkedList<SubmitCallback>();

  public void expectOpen(IdFilter waveletFilter,
      Map<WaveletId, List<HashedVersion>> knownWavelets) {
    expectations.add(new Object[] {Method.OPEN, waveletFilter, knownWavelets});
  }

  public void expectSubmitDelta(WaveletId waveletId, WaveletDelta delta) {
    expectations.add(new Object[] {Method.SUBMIT, waveletId, delta});
  }

  public void expectClose() {
    expectations.add(new Object[] {Method.CLOSE});
  }

  public void checkExpectationsSatisified() {
    assertTrue("Unsatisified view channel expectations", expectations.isEmpty());
  }

  @Override
  public void open(Listener viewListener, IdFilter waveletFilter,
      Map<WaveletId, List<HashedVersion>> knownWavelets) {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.OPEN);
    assertEquals(expected[1], waveletFilter);
    assertEquals(expected[2], knownWavelets);
    listeners.add(viewListener);
  }

  @Override
  public void submitDelta(WaveletId waveletId, WaveletDelta delta, SubmitCallback callback) {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.SUBMIT);
    assertEquals(expected[1], waveletId);
    assertTrue(CcTestingUtils.deltasAreEqual((WaveletDelta) expected[2], delta));
    awaitingAck.add(callback);
  }

  @Override
  public void close() {
    Object[] expected = expectations.remove();
    assertEquals(expected[0], Method.CLOSE);
    // Emulate the real view channel in sending close() synchronously.
    for (Listener l : listeners) {
      l.onClosed();
    }
  }

  public Listener takeListener() {
    return listeners.remove();
  }

  public void ackSubmit(int opsApplied, long version, byte[] signature) throws ChannelException {
    SubmitCallback nextCallback = awaitingAck.remove();
    nextCallback.onSuccess(opsApplied, HashedVersion.of(version, signature), ResponseCode.OK, null);
  }

  public void nackSubmit(String reason, long version, byte[] signature) throws ChannelException {
    SubmitCallback nextCallback = awaitingAck.remove();
    nextCallback.onSuccess(0, HashedVersion.of(version, signature), ResponseCode.INTERNAL_ERROR,
        reason);
  }

  @Override
  public String debugGetProfilingInfo(WaveletId waveletId) {
    return null;
  }
}
