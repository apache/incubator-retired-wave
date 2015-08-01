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

import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock WaveViewService. Captures arguments for easy access in tests.
 *
 */
public class MockWaveViewService implements WaveViewService {
  public static class OpenArguments {
    public final IdFilter waveletFilter;
    public final Map<WaveletId, List<HashedVersion>> knownWavelets;
    public final OpenCallback callback;

    private OpenArguments(IdFilter waveletFilter,
        Map<WaveletId, List<HashedVersion>> knownWavelets, OpenCallback callback) {
      this.waveletFilter = waveletFilter;
      this.knownWavelets = knownWavelets;
      this.callback = callback;
    }
  }

  public static class SubmitArguments {
    public final WaveletName wavelet;
    public final WaveletDelta delta;
    public final String channelId;
    public final SubmitCallback callback;

    private SubmitArguments(WaveletName wavelet, WaveletDelta delta, String channelId,
        SubmitCallback callback) {
      this.wavelet = wavelet;
      this.delta = delta;
      this.channelId = channelId;
      this.callback = callback;
    }
  }

  public static class CloseArguments {
    public final WaveId waveId;
    public final String channelId;
    public final CloseCallback callback;

    private CloseArguments(WaveId waveId, String channelId, CloseCallback callback) {
      this.waveId = waveId;
      this.channelId = channelId;
      this.callback = callback;
    }
  }

  public final List<OpenArguments> opens = new ArrayList<OpenArguments>();
  public final List<SubmitArguments> submits = new ArrayList<SubmitArguments>();
  public final List<CloseArguments> closes = new ArrayList<CloseArguments>();

  public OpenArguments lastOpen() {
    Assert.assertFalse(opens.isEmpty());
    return opens.get(opens.size() - 1);
  }

  public SubmitArguments lastSubmit() {
    Assert.assertFalse(submits.isEmpty());
    return submits.get(submits.size() - 1);
  }

  public CloseArguments lastClose() {
    Assert.assertFalse(closes.isEmpty());
    return closes.get(closes.size() - 1);
  }

  @Override
  public void viewOpen(IdFilter waveletFilter, Map<WaveletId, List<HashedVersion>> knownWavelets,
      OpenCallback callback) {
    opens.add(new OpenArguments(waveletFilter, knownWavelets, callback));
  }

  @Override
  public String viewSubmit(WaveletName wavelet, WaveletDelta delta, String channelId,
      SubmitCallback callback) {
    submits.add(new SubmitArguments(wavelet, delta, channelId, callback));
    return null;
  }

  @Override
  public void viewClose(WaveId waveId, String channelId, CloseCallback callback) {
    closes.add(new CloseArguments(waveId, channelId, callback));
  }

  @Override
  public String debugGetProfilingInfo(String requestId) {
    return "";
  }
}
