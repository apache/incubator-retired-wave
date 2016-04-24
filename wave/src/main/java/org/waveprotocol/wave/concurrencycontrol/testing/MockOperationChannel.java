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

package org.waveprotocol.wave.concurrencycontrol.testing;

import junit.framework.Assert;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A mock operation channel which allows expectations to be set.
 *
 * @author anorth@google.com (Alex North)
 */
public final class MockOperationChannel implements OperationChannel {

  private static enum Method {
    SEND, RECEIVE, PEEK
  }

  private final Queue<Object[]> expectations = new LinkedList<Object[]>();

  /**
   * Expects a call to send() with a list of operations.
   */
  public void expectSend(WaveletOperation... operations) {
    expectations.add(new Object[] {Method.SEND, operations});
  }

  /**
   * Expects a call to receive(), which will return the provided operation (may
   * be null).
   */
  public void expectReceive(WaveletOperation op) {
    expectations.add(new Object[] {Method.RECEIVE, op});
  }

  /**
   * Expects a call to peek(), which will return the provided operation (may be
   * null).
   */
  public void expectPeek(WaveletOperation op) {
    expectations.add(new Object[] {Method.PEEK, op});
  }

  /**
   * Checks that all expectations have been satisfied.
   */
  public void checkExpectationsSatisfied() {
    Assert.assertTrue("Unsatisfied expectations", expectations.isEmpty());
  }

  @Override
  public void setListener(Listener listener) {
    // Do nothing.
  }

  @Override
  public void send(WaveletOperation... operations) {
    Object[] expected = expectations.remove();
    Assert.assertEquals(expected[0], Method.SEND);
    Assert.assertEquals(expected[1], operations);
  }

  @Override
  public WaveletOperation receive() {
    Object[] expected = expectations.remove();
    Assert.assertEquals(expected[0], Method.RECEIVE);
    return (WaveletOperation) expected[1];
  }

  @Override
  public WaveletOperation peek() {
    Object[] expected = expectations.remove();
    Assert.assertEquals(expected[0], Method.PEEK);
    return (WaveletOperation) expected[1];
  }

  @Override
  public List<HashedVersion> getReconnectVersions() {
    throw new UnsupportedOperationException("Reconnection not supported");
  }

  @Override
  public String getDebugString() {
    return "[Mock operation channel]";
  }
}
