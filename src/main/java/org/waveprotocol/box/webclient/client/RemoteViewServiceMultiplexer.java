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

package org.waveprotocol.box.webclient.client;

import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.common.comms.jso.ProtocolOpenRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitRequestJsoImpl;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;

/**
 * Distributes the incoming update stream (from wave-in-a-box's client/server
 * protocol) into per-wave streams.
 */
public final class RemoteViewServiceMultiplexer implements WaveWebSocketCallback {

  /** Per-wave streams. */
  private final Map<WaveId, WaveWebSocketCallback> streams = CollectionUtils.newHashMap();

  //
  // Workaround for issue 128.
  // http://code.google.com/p/wave-protocol/issues/detail?id=128
  //
  // Filtering logic is as follows. Since not every update has a channel id, but
  // all updates have a wavelet name, wave ids remain the primary key. This
  // map's domain is a subset of streams' domain, and is monotonically set with
  // the first channel id observed for an open wave. Only updates that have no
  // channel id, or an equal channel id, are passed through to the stream.
  // Closing the stream removes any known channel id from this map (this follows
  // from the contraint that this maps's domain is a subset of streams' domain).
  //
  private final Map<WaveId, String> knownChannels = CollectionUtils.newHashMap();

  /** Underlying socket. */
  private final WaveWebSocketClient socket;

  /** Identity, for authoring messages. */
  private final String userId;

  /**
   * Creates a multiplexer.
   *
   * @param socket communication object
   * @param userId identity of viewer
   */
  public RemoteViewServiceMultiplexer(WaveWebSocketClient socket, String userId) {
    this.socket = socket;
    this.userId = userId;

    // Note: Currently, the client's communication stack (websocket) is opened
    // too early, before an identity is established. Once that is fixed, this
    // object will be registered as a callback when the websocket is opened,
    // rather than afterwards here.
    socket.attachHandler(this);
  }

  /** Dispatches an update to the appropriate wave stream. */
  @Override
  public void onWaveletUpdate(ProtocolWaveletUpdate message) {
    WaveletName wavelet = deserialize(message.getWaveletName());

    // Route to the appropriate stream handler.
    WaveWebSocketCallback stream = streams.get(wavelet.waveId);
    if (stream != null) {
      boolean drop;

      String knownChannelId = knownChannels.get(wavelet.waveId);
      if (knownChannelId != null) {
      // Drop updates with known mismatched channel ids.
        drop = message.hasChannelId() && !message.getChannelId().equals(knownChannelId);
      } else {
        if (message.hasChannelId()) {
          knownChannels.put(wavelet.waveId, message.getChannelId());
        }
        drop = false;
      }

      if (!drop) {
        stream.onWaveletUpdate(message);
      }
    } else {
      // This is either a server error, or a message after a stream has been
      // locally closed (there is no way to tell the server to stop sending
      // updates).
    }
  }

  /**
   * Opens a wave stream.
   *
   * @param id wave to open
   * @param stream handler to updates directed at that wave
   */
  public void open(WaveId id, IdFilter filter, WaveWebSocketCallback stream) {
    // Prepare to receive updates for the new stream.
    streams.put(id, stream);

    // Request those updates.
    ProtocolOpenRequestJsoImpl request = ProtocolOpenRequestJsoImpl.create();
    request.setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(id));
    request.setParticipantId(userId);
    for (String prefix : filter.getPrefixes()) {
      request.addWaveletIdPrefix(prefix);
    }
    // Issue 161: http://code.google.com/p/wave-protocol/issues/detail?id=161
    // The box protocol does not support explicit wavelet ids in the filter.
    // As a workaround, include them in the prefix list.
    for (WaveletId wid : filter.getIds()) {
      request.addWaveletIdPrefix(wid.getId());
    }
    socket.open(request);
  }

  /**
   * Closes a wave stream.
   *
   * @param id wave to close
   * @param stream stream previously registered against that wave
   */
  public void close(WaveId id, WaveWebSocketCallback stream) {
    if (streams.get(id) == stream) {
      streams.remove(id);
      knownChannels.remove(id);
    }

    // Issue 117: the client server protocol does not support closing a wave stream.
  }

  /**
   * Submits a delta.
   *
   * @param request delta to submit
   * @param callback callback for submit response
   */
  public void submit(ProtocolSubmitRequestJsoImpl request, SubmitResponseCallback callback) {
    request.getDelta().setAuthor(userId);
    socket.submit(request, callback);
  }

  public static WaveletName deserialize(String name) {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveletName(name);
    } catch (InvalidIdException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String serialize(WaveletName name) {
    return ModernIdSerialiser.INSTANCE.serialiseWaveletName(name);
  }
}
