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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveWrapper;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveViewListener;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.wave.opbased.WaveletListenerImpl;

/**
 * Optimistic implementation of a wave supplement.
 * <p>
 * Because read state is based on versions, and versions increment
 * asynchronously after local operations have been applied, performing actions
 * on the regular supplement model synchronously with reading actions does not
 * bring the supplement into the correct state, since the modification versions
 * of waves and blips will not have been updated. This class addresses that
 * synchronization issue.
 * <p>
 * This class decorates a regular supplement, and keeps track of two kinds of
 * blips: a blip that is currently being read, and blips that have been marked
 * as read. This class overrides the read state queries of the regular
 * supplement in order that a blip that is being read is always revealed to be
 * read by the supplement API. Blips that have been marked as read are
 * continuously marked as read in the background if they are modified, until a
 * remote change occurs on them. This is to ensure local operations, whose
 * version increments only occur asynchronously after server acknowledgements,
 * appear to be read. Note that server acknowledgements of local operations are
 * observed as version increments, while incoming remote operations are observed
 * as version increments accompanied by a
 * {@link WaveletListener#onRemoteBlipContentModified} event. The observation of
 * that event on a blip removes it from the set being automatically read.
 * <p>
 * Since it is not possible to detect, with the wavelet API, when all local
 * operations have been acknowledged, the automatically-read blip collection can
 * grow quite large. In order not to leak memory from continuous growth, a blip
 * in that is evicted after a generous amount of time, within which it is
 * assumed that the server acknowledgements for local operations on that blip
 * will have arrived.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class LocalSupplementedWaveImpl extends SupplementedWaveWrapper<
    ObservableSupplementedWave>
    implements WaveViewListener, LocalSupplementedWave, IncrementalTask {

  /** How often to mark auto-read blips as read. */
  @VisibleForTesting
  static final int REPEAT_MS = 10 * 1000;

  /**
   * Background auto-read is stopped for blips that have not changed for longer
   * than this.
   */
  @VisibleForTesting
  static final int EVICT_TIME_MS = 60 * 1000;

  private final TimerService timer;
  private final ObservableWaveView wave;

  /**
   * Blips in this collection are periodically (in intervals of
   * {@link #REPEAT_MS}) marked as read. Each blip maps to the last time it was
   * marked as read. Blips are evicted either when they are unchanged for a long
   * time, or a remote operation occurs on them.
   */
  private final IdentityMap<ConversationBlip, Double> autoRead =
      CollectionUtils.createIdentityMap();

  /**
   * The same blips as in {@link #autoRead}, but with their raw versions, for
   * cross-referencing with {@link WaveletListener#onRemoteBlipContentModified}
   * events.
   */
  private final IdentityMap<Blip, ConversationBlip> rawAutoRead =
      CollectionUtils.createIdentityMap();

  /** Listener that detects remote changes on blips. */
  private final WaveletListener remoteChangeDetector = new WaveletListenerImpl() {
    @Deprecated
    @Override
    public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
      onRemoteChange(blip);
    }
  };

  /** The blip that is actively being read, if there is one. */
  private ConversationBlip reading;

  LocalSupplementedWaveImpl(
      TimerService timer, ObservableWaveView wave, ObservableSupplementedWave delegate) {
    super(delegate);
    this.timer = timer;
    this.wave = wave;
  }

  public static LocalSupplementedWaveImpl create(
      ObservableWaveView wave, ObservableSupplementedWave delegate) {
    TimerService timer = SchedulerInstance.getLowPriorityTimer();
    LocalSupplementedWaveImpl supplement = new LocalSupplementedWaveImpl(timer, wave, delegate);
    supplement.init();
    return supplement;
  }

  @VisibleForTesting
  void init() {
    wave.addListener(this);
    for (ObservableWavelet wavelet : wave.getWavelets()) {
      wavelet.addListener(remoteChangeDetector);
    }
    timer.scheduleRepeating(this, REPEAT_MS, REPEAT_MS);
  }

  public void destroy() {
    timer.cancel(this);
    for (ObservableWavelet wavelet : wave.getWavelets()) {
      wavelet.removeListener(remoteChangeDetector);
    }
    wave.removeListener(this);
  }

  @Override
  public void startReading(ConversationBlip blip) {
    Preconditions.checkState(reading == null);
    reading = blip;
    startAutoReading(reading);
  }

  @Override
  public void stopReading(ConversationBlip blip) {
    Preconditions.checkState(reading != null);
    Preconditions.checkArgument(reading == blip);
    // Continue marking this blip as read while the acks come in. Delay eviction
    // by refreshing its timestamp.
    assert autoRead.has(reading);
    delegate.markAsRead(reading);
    autoRead.put(reading, timer.currentTimeMillis());
    reading = null;
  }

  /**
   * Puts a blip into the auto-read collection.
   */
  private void startAutoReading(ConversationBlip blip) {
    // Mark it as read first, before putting in the auto-read override, in order
    // to generate the correct read events.
    delegate.markAsRead(blip);
    autoRead.put(blip, timer.currentTimeMillis());
    rawAutoRead.put(blip.hackGetRaw(), blip);
  }

  /**
   * Removes a blip from the auto-read collection.
   */
  private void stopAutoReading(ConversationBlip blip) {
    assert blip != reading : "not allowed to remove the reading blip";
    autoRead.remove(blip);
    rawAutoRead.remove(blip.hackGetRaw());
  }

  @Override
  public boolean isUnread(ConversationBlip blip) {
    // All blips in autoRead are treated as read.
    return !autoRead.has(blip) && delegate.isUnread(blip);
  }

  @Override
  public void markAsRead(ConversationBlip blip) {
    startAutoReading(blip);
  }

  @Override
  public void markAsUnread() {
    autoRead.clear();
    rawAutoRead.clear();
    delegate.markAsUnread();
    if (reading != null) {
      startAutoReading(reading);
    }
  }

  @Override
  public boolean execute() {
    // Mark as read all blips in the auto-read list.
    // Evict any blips that have not been touched for more than some threshold,
    // but never evict the currently-reading blip.
    final IdentitySet<ConversationBlip> toEvict = CollectionUtils.createIdentitySet();
    if (reading != null) {
      delegate.markAsRead(reading);
    }
    autoRead.each(new ProcV<ConversationBlip, Double>() {
      final double now = timer.currentTimeMillis();

      @Override
      public void apply(ConversationBlip blip, Double touched) {
        delegate.markAsRead(blip);
        if (blip != reading && (now - touched >= EVICT_TIME_MS)) {
          toEvict.add(blip);
        }
      }
    });
    toEvict.each(new Proc<ConversationBlip>() {
      @Override
      public void apply(ConversationBlip blip) {
        stopAutoReading(blip);
      }
    });
    return true; // Run forever
  }

  //
  // Wave events.
  //

  @Override
  public void onWaveletAdded(ObservableWavelet wavelet) {
    wavelet.addListener(remoteChangeDetector);
  }

  @Override
  public void onWaveletRemoved(ObservableWavelet wavelet) {
    wavelet.removeListener(remoteChangeDetector);
  }

  private void onRemoteChange(Blip raw) {
    // Some op caused by someone else occurred. Stop auto-reading.
    ConversationBlip blip = rawAutoRead.get(raw);
    if (blip != null && blip != reading) {
      stopAutoReading(blip);
    }
  }

  //
  // Events.
  //

  @Override
  public void addListener(Listener listener) {
    delegate.addListener(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    delegate.removeListener(listener);
  }
}
