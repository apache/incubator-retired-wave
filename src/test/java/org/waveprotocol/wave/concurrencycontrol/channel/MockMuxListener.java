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

import junit.framework.Assert;

import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Queue;

/**
 * The mock lister on the OperationChannelMultiplexer
 *
 * @author zdwang@google.com (David Wang)
 */
public class MockMuxListener implements OperationChannelMultiplexer.Listener {
  private enum MethodCall {
    ON_OPERATION_CHANNEL_CREATED,
    ON_OPERATION_CHANNEL_REMOVED,
    ON_OPEN_FINISHED,
    ON_FAILED
  }

  public class MethodCallContext {
    final MockMuxListener.MethodCall method;
    final OperationChannel channel;
    final ObservableWaveletData snapshot;
    final Accessibility accessibility;

    public MethodCallContext(MockMuxListener.MethodCall method, OperationChannel channel,
        ObservableWaveletData snapshot, Accessibility accessibility) {
      this.method = method;
      this.channel = channel;
      this.snapshot = snapshot;
      this.accessibility = accessibility;
    }

    public MethodCallContext(MockMuxListener.MethodCall method) {
      this(method, null, null, null);
    }

    @Override
    public String toString() {
      return method.toString();
    }
  }

  Queue<MethodCallContext> methodCalls = CollectionUtils.newLinkedList();

  @Override
  public void onOperationChannelCreated(OperationChannel channel, ObservableWaveletData snapshot,
      Accessibility accessibility) {
    methodCalls.add(
        new MethodCallContext(MethodCall.ON_OPERATION_CHANNEL_CREATED, channel,
            snapshot, accessibility));
  }

  @Override
  public void onOperationChannelRemoved(
      OperationChannel channel, WaveletId waveletId) {
    methodCalls.add(
        new MethodCallContext(MethodCall.ON_OPERATION_CHANNEL_REMOVED, channel, null, null));
  }

  @Override
  public void onOpenFinished() {
    methodCalls.add(new MethodCallContext(MethodCall.ON_OPEN_FINISHED));
  }

  @Override
  public void onFailed(CorruptionDetail detail) {
    methodCalls.add(new MethodCallContext(MethodCall.ON_FAILED));
  }

  public void verifyNoMoreInteractions() {
    Assert.assertEquals("Unexpected method calls: " + methodCalls, 0, methodCalls.size());
  }

  public OperationChannel verifyOperationChannelCreated(ObservableWaveletData snapshot,
      Accessibility accessibility) {
    MethodCallContext context = methodCalls.remove();
    assertEquals(MethodCall.ON_OPERATION_CHANNEL_CREATED, context.method);
    // TODO(anorth): check the snapshot with equals() once a clock is injected
    // into the mux for timestamps.
    assertEquals(snapshot.getWaveId(), context.snapshot.getWaveId());
    assertEquals(snapshot.getWaveletId(), context.snapshot.getWaveletId());
    assertEquals(snapshot.getCreator(), context.snapshot.getCreator());
    assertEquals(snapshot.getVersion(), context.snapshot.getVersion());
    assertEquals(snapshot.getHashedVersion(), context.snapshot.getHashedVersion());

    assertEquals(accessibility, context.accessibility);
    return context.channel;
  }

  public void verifyOperationChannelRemoved(OperationChannel channel) {
    MethodCallContext context = methodCalls.remove();
    assertEquals(MethodCall.ON_OPERATION_CHANNEL_REMOVED, context.method);
    assertEquals(channel, context.channel);
  }

  public void verifyOpenFinished() {
    MethodCallContext context = methodCalls.remove();
    assertEquals(MethodCall.ON_OPEN_FINISHED, context.method);
  }

  public void verifyFailed() {
    MethodCallContext context = methodCalls.remove();
    assertEquals(MethodCall.ON_FAILED, context.method);
  }
}
