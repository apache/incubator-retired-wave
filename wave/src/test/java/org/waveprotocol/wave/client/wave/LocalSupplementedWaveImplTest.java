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

package org.waveprotocol.wave.client.wave;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Collections;

/**
 * Tests for {@link LocalSupplementedWaveImpl}.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class LocalSupplementedWaveImplTest extends TestCase {

  @Mock
  private ObservableSupplementedWave supplement;
  @Mock
  private ObservableWaveView wave;
  @Mock
  private ObservableWavelet root;
  private ConversationBlip blip;

  private LocalSupplementedWaveImpl target;
  private FakeTimerService timer;


  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);
    blip = mockBlip();
    timer = new FakeTimerService();

    doReturn(Collections.singleton(root)).when(wave).getWavelets();
    target = new LocalSupplementedWaveImpl(timer, wave, supplement);
    target.init();
  }

  private ConversationBlip mockBlip() {
    ConversationBlip blip = mock(ConversationBlip.class);
    Blip raw = mock(Blip.class);
    when(blip.hackGetRaw()).thenReturn(raw);
    return blip;
  }

  public void testReadingActionsAreScoped() {
    target.startReading(blip);
    try {
      target.startReading(blip);
      fail();
    } catch (IllegalStateException expected) {

    }
    target.stopReading(blip);
    try {
      target.stopReading(blip);
      fail();
    } catch (IllegalStateException expected) {

    }
  }

  public void testReadingBlipIsAlwaysReadOtherwiseIsWhatSupplementSays() {
    when(supplement.isUnread(blip)).thenReturn(true);

    target.startReading(blip);
    assertFalse(target.isUnread(blip));

    // Stop reading, fast-forwarding to when auto-reading has ceased.
    target.stopReading(blip);
    timer.tick(LocalSupplementedWaveImpl.EVICT_TIME_MS);
    assertTrue(target.isUnread(blip));
  }

  public void testNonreadingBlipIsWhatSupplementSays() {
    when(supplement.isUnread(blip)).thenReturn(false);
    assertFalse(target.isUnread(blip));
    when(supplement.isUnread(blip)).thenReturn(true);
    assertTrue(target.isUnread(blip));
  }

  public void testStopReadingCausesAutoReadingUntilEvicted() {
    target.startReading(blip);
    target.stopReading(blip);

    reset(supplement); // Ignore anything that happened before now.
    int invocations = LocalSupplementedWaveImpl.EVICT_TIME_MS / LocalSupplementedWaveImpl.REPEAT_MS;
    for (int i = 0; i < invocations; i++) {
      timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    }
    verify(supplement, times(invocations)).markAsRead(blip);

    reset(supplement);
    timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    verify(supplement, never()).markAsRead(blip);
  }

  public void testMarkAsReadCausesAutoReadingUntilEvicted() {
    target.markAsRead(blip);

    reset(supplement); // Ignore anything that happened before now.
    int invocations = LocalSupplementedWaveImpl.EVICT_TIME_MS / LocalSupplementedWaveImpl.REPEAT_MS;
    for (int i = 0; i < invocations; i++) {
      timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    }
    verify(supplement, times(invocations)).markAsRead(blip);

    reset(supplement);
    timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    verify(supplement, never()).markAsRead(blip);
  }

  @SuppressWarnings("deprecation")
  public void testRemoteOpStopsAutoReading() {
    ArgumentCaptor<WaveletListener> arg = ArgumentCaptor.forClass(WaveletListener.class);
    verify(root).addListener(arg.capture());
    WaveletListener listener = arg.getValue();

    target.markAsRead(blip);

    timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    reset(supplement); // Ignore anything that happened before now.
    listener.onRemoteBlipContentModified(root, blip.hackGetRaw());
    // Expect that target removed the blip from the auto-read collection.
    timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    verify(supplement, never()).markAsRead(blip);
  }

  public void testAutoReadSupportsManyBlips() {
    ConversationBlip blip2 = mockBlip();

    target.markAsRead(blip);
    target.markAsRead(blip2);

    timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    verify(supplement, times(2)).markAsRead(blip);
    verify(supplement, times(2)).markAsRead(blip2);
  }

  public void testMarkAsReadRefreshesEvictionTime() {
    target.markAsRead(blip);
    // Fast-forward to near eviction time.
    timer.tick(LocalSupplementedWaveImpl.EVICT_TIME_MS - 1);

    // Refresh, and expect a full set of future markings.
    target.markAsRead(blip);
    reset(supplement);
    int invocations = LocalSupplementedWaveImpl.EVICT_TIME_MS / LocalSupplementedWaveImpl.REPEAT_MS;
    for (int i = 0; i < invocations; i++) {
      timer.tick(LocalSupplementedWaveImpl.REPEAT_MS);
    }
    verify(supplement, times(invocations)).markAsRead(blip);
  }

  public void testMarkAsUnreadClearsAutoReadingButContinuesReadingActiveBlip() {
    ConversationBlip other = mockBlip();
    target.startReading(blip);
    target.markAsRead(other);

    reset(supplement);
    target.markAsUnread();
    verify(supplement, never()).markAsRead(other);
    verify(supplement).markAsRead(blip);
  }
}
