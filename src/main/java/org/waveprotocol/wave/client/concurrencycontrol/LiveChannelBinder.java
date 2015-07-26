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

package org.waveprotocol.wave.client.concurrencycontrol;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.concurrencycontrol.channel.Accessibility;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer.KnownWavelet;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveViewListener;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;

import java.util.Collection;

/**
 * Binds operation channels from a {@link OperationChannelMultiplexer mux} with
 * the output sinks of wavelets, and keeps binding matching channel/wavelet
 * pairs while live.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class LiveChannelBinder
    implements WaveViewListener, OperationChannelMultiplexer.Listener {

  private final StaticChannelBinder binder;
  private final WaveletOperationalizer operationalizer;
  private final WaveViewImpl<OpBasedWavelet> wave;
  private final OperationChannelMultiplexer mux;
  private final Command whenOpened;

  /**
   * Operation channels waiting to be bound. This map is populated from {@link
   * #onOperationChannelCreated(OperationChannel, ObservableWaveletData,
   * Accessibility)}, and depleted by {@link #connect(String)}.
   */
  private final StringMap<OperationChannel> channels = CollectionUtils.createStringMap();

  //
  // The binding flow is not completely trivial, because it has to work with
  // two directions of control flow:
  //
  // Client-created wavelets:
  // 1. wavelet shows up in the model
  // 2. this binder tells mux to create op channel,
  // 3. its operation channel shows up, then
  // 4. wavelet and op channel are bound together.
  //
  // Server-created wavelets:
  // 1. op channel shows up in the mux,
  // 2. this binder builds wavelet and puts it in the model,
  // 3. wavelet shows up in the model, then
  // 4. wavelet and op channel are bound together.
  //
  // Also, the initial set of operation channels when opening a wave with known
  // wavelet states is just like the server-created wavelet flow, except without
  // step 2.
  //

  private LiveChannelBinder(StaticChannelBinder binder, WaveletOperationalizer operationalizer,
      WaveViewImpl<OpBasedWavelet> wave, OperationChannelMultiplexer mux, Command whenOpened) {
    this.binder = binder;
    this.operationalizer = operationalizer;
    this.wave = wave;
    this.mux = mux;
    this.whenOpened = whenOpened;
  }

  /**
   * Opens a mux, binding its operation channels with operation-supporting
   * wavelets.
   */
  public static void openAndBind(WaveletOperationalizer operationalizer,
      WaveViewImpl<OpBasedWavelet> wave,
      WaveDocuments<? extends CcDocument> docRegistry,
      OperationChannelMultiplexer mux,
      IdFilter filter,
      Command whenOpened) {
    StaticChannelBinder staticBinder = new StaticChannelBinder(operationalizer, docRegistry);
    LiveChannelBinder liveBinder =
        new LiveChannelBinder(staticBinder, operationalizer, wave, mux, whenOpened);

    final Collection<KnownWavelet> remoteWavelets = CollectionUtils.createQueue();
    final Collection<ObservableWaveletData> localWavelets = CollectionUtils.createQueue();
    for (ObservableWaveletData wavelet : operationalizer.getWavelets()) {
      // Version 0 wavelets must be wavelets that the client has created in this
      // session. They are not to be included in the known-wavelet collection,
      // because the server does not know about them.
      if (wavelet.getVersion() > 0) {
        remoteWavelets.add(
            new KnownWavelet(wavelet, wavelet.getHashedVersion(), Accessibility.READ_WRITE));
      } else {
        localWavelets.add(wavelet);
      }
    }

    // Start listening to wave events and channel events.
    wave.addListener(liveBinder);
    // This binder only starts getting events once open() has been called, since
    // that is what sets this binder as a mux listener. Since wavelet-to-channel
    // binding occurs through event callbacks, this listener setting must occur
    // before trying to bind localWavelets.
    mux.open(liveBinder, filter, remoteWavelets);
    for (ObservableWaveletData local : localWavelets) {
      mux.createOperationChannel(local.getWaveletId(), local.getCreator());
    }
  }

  @Override
  public void onFailed(CorruptionDetail detail) {
    throw new RuntimeException(detail);
  }

  @Override
  public void onOpenFinished() {
    if (whenOpened != null) {
      whenOpened.execute();
    }
  }

  //
  // Wavelet and Channel lifecycle events:
  //

  @Override
  public void onWaveletAdded(ObservableWavelet wavelet) {
    String id = ModernIdSerialiser.INSTANCE.serialiseWaveletId(wavelet.getId());
    if (channels.containsKey(id)) {
      connect(id);
    } else {
      // This will trigger the onOperationChannelCreated callback below.
      mux.createOperationChannel(wavelet.getId(), wavelet.getCreatorId());
    }
  }

  @Override
  public void onOperationChannelCreated(
      OperationChannel channel, ObservableWaveletData snapshot, Accessibility accessibility) {
    WaveletId wid = snapshot.getWaveletId();
    String id = ModernIdSerialiser.INSTANCE.serialiseWaveletId(wid);

    Preconditions.checkState(!channels.containsKey(id));
    channels.put(id, channel);

    if (wave.getWavelet(wid) != null) {
      connect(id);
    } else {
      // This will trigger the onWaveletAdded callback above.
      wave.addWavelet(operationalizer.operationalize(snapshot));
    }
  }

  @Override
  public void onWaveletRemoved(ObservableWavelet wavelet) {
    // TODO
  }

  @Override
  public void onOperationChannelRemoved(OperationChannel channel, WaveletId waveletId) {
    // TODO
  }

  private void connect(String id) {
    binder.bind(id, removeAndReturn(channels, id));
  }

  // Something that should have been on StringMap from the beginning.
  private static <V> V removeAndReturn(StringMap<V> map, String key) {
    V value = map.get(key);
    map.remove(key);
    return value;
  }
}
