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

package com.google.wave.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link OperationQueue}.
 */
public class OperationQueueRobotTest extends TestCase {

  private Wavelet wavelet;

  @Override
  protected void setUp() throws Exception {
    wavelet = mock(Wavelet.class);
    when(wavelet.getWaveId()).thenReturn(WaveId.of("example.com", "wave1"));
    when(wavelet.getWaveletId()).thenReturn(WaveletId.of("example.com", "wavelet1"));
    when(wavelet.getThread(anyString())).thenReturn(new BlipThread("rootThread", -1,
        Lists.<String>newArrayList(), new HashMap<String, Blip>()));
  }

  public void testProxyFor() throws Exception {
    OperationQueue queue = new OperationQueue();
    queue.createWavelet("google.com", Collections.<String>emptySet());

    OperationQueue proxyingForQueue = queue.proxyFor("foo");
    assertEquals(1, proxyingForQueue.getPendingOperations().size());
    proxyingForQueue.setTitleOfWavelet(wavelet, "My title");

    List<OperationRequest> ops = proxyingForQueue.getPendingOperations();
    assertEquals(2, ops.size());
    assertEquals(OperationType.ROBOT_CREATE_WAVELET.method(), ops.get(0).getMethod());
    assertNull(ops.get(0).getParameter(ParamsProperty.PROXYING_FOR));

    assertEquals(OperationType.WAVELET_SET_TITLE.method(), ops.get(1).getMethod());
    assertEquals("foo", ops.get(1).getParameter(ParamsProperty.PROXYING_FOR));

    OperationQueue nonProxyingForQueue = proxyingForQueue.proxyFor(null);
    nonProxyingForQueue.fetchWavelet(WaveId.of("example.com", "wave2"),
        WaveletId.of("example.com", "wavelet2"));

    ops = nonProxyingForQueue.getPendingOperations();
    assertEquals(3, ops.size());
    assertNull(ops.get(2).getParameter(ParamsProperty.PROXYING_FOR));
  }

  public void testSubmitWith() throws Exception {
    OperationQueue queue = new OperationQueue();
    queue.createWavelet("example.com", Collections.<String>emptySet());

    OperationQueue queue2 = new OperationQueue();
    queue2.setTitleOfWavelet(wavelet, "My title");

    queue2.submitWith(queue);

    List<OperationRequest> ops = queue.getPendingOperations();
    assertEquals(2, ops.size());
    assertEquals(OperationType.ROBOT_CREATE_WAVELET.method(), ops.get(0).getMethod());
    assertEquals(OperationType.WAVELET_SET_TITLE.method(), ops.get(1).getMethod());
    assertEquals(queue.getPendingOperations(), queue2.getPendingOperations());
  }

  public void testCreateChildOfBlip() throws Exception {
    Blip blip = mock(Blip.class);
    when(blip.getBlipId()).thenReturn("blip1");
    when(blip.getWavelet()).thenReturn(wavelet);

    Map<String, Blip> blips = new HashMap<String, Blip>();
    blips.put("blip1", blip);
    when(wavelet.getBlip("blip1")).thenReturn(blip);
    when(wavelet.getBlips()).thenReturn(blips);

    OperationQueue queue = new OperationQueue();
    Blip newBlip = queue.createChildOfBlip(blip);
    assertEquals(blip.getBlipId(), newBlip.getParentBlipId());
    assertEquals(blip, newBlip.getParentBlip());
    assertEquals(2, blips.size());
  }
}
