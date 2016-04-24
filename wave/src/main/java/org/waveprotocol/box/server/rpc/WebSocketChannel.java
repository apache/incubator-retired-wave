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

package org.waveprotocol.box.server.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.Message;

import org.waveprotocol.box.server.rpc.ProtoSerializer.SerializationException;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.communication.gson.GsonSerializable;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import org.waveprotocol.box.stat.SessionContext;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;

/**
 * A channel abstraction for websocket, for sending and receiving strings.
 */
public abstract class WebSocketChannel extends MessageExpectingChannel {
  private static final Log LOG = Log.get(WebSocketChannel.class);

  /**
   * Envelope for delivering arbitrary messages. Each envelope has a sequence
   * number and a message.
   * <p>
   * Note that this message can not be described by a protobuf, because it
   * contains an arbitrary protobuf, which breaks the protobuf typing rules.
   */
  private static class MessageWrapper {
    private final static JsonParser parser = new JsonParser();

    final int sequenceNumber;
    final String messageType;
    final JsonElement message;

    public MessageWrapper(int sequenceNumber, String messageType, JsonElement message) {
      this.sequenceNumber = sequenceNumber;
      this.messageType = messageType;
      this.message = message;
    }

    public static MessageWrapper deserialize(Gson gson, String data) {
      JsonElement e = parser.parse(data);
      JsonObject obj = e.getAsJsonObject();
      String type = obj.get("messageType").getAsString();
      int seqno = obj.get("sequenceNumber").getAsInt();
      JsonElement message = obj.get("message");
      return new MessageWrapper(seqno, type, message);
    }

    public static String serialize(String type,int seqno,  JsonElement message) {
      JsonObject o = new JsonObject();
      o.add("messageType", new JsonPrimitive(type));
      o.add("sequenceNumber", new JsonPrimitive(seqno));
      o.add("message", message);
      return o.toString();
    }
  }

  private final ProtoCallback callback;
  private final SessionContext sessionContext;
  private final Gson gson = new Gson();
  private final ProtoSerializer serializer;

  /**
   * Constructs a new WebSocketChannel, using the callback to handle any
   * incoming messages.
   *
   * @param callback a protocallback to be called when data arrives on this
   *                 channel
   */
  public WebSocketChannel(ProtoCallback callback) {
    this.callback = callback;
    this.sessionContext =
        Timing.isEnabled() ? Timing.getScopeValue(SessionContext.class) : null;
    // The ProtoSerializer could really be singleton.
    // TODO: Figure out a way to inject a singleton instance using Guice
    this.serializer = new ProtoSerializer();
  }

  public void handleMessageString(String data) {
    LOG.fine("received JSON message " + data);
    if (Timing.isEnabled()) {
      Timing.enterScope();
      Timing.setScopeValue(SessionContext.class, sessionContext);
    }
    try {
      Message message;

      MessageWrapper wrapper = MessageWrapper.deserialize(gson, data);

      try {
        message = serializer.fromJson(wrapper.message, wrapper.messageType);
      } catch (SerializationException e) {
        LOG.warning("message handling error", e);
        e.printStackTrace();
        return;
      }
      callback.message(wrapper.sequenceNumber, message);
    } finally {
      Timing.exitScope();
    }
  }

  static <T extends GsonSerializable> T load(JsonElement payload, T x, Gson gson) {
    try {
      x.fromGson(payload, gson, null);
      return x;
    } catch (GsonException e) {
      LOG.warning("JSON load error", e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message on the socket.
   *
   * @param data message to send
   * @throws IOException if the communication fails
   */
  protected abstract void sendMessageString(String data) throws IOException;

  @Override
  public void sendMessage(int sequenceNo, Message message) {
    JsonElement json;
    String str;

    Timer timer = Timing.start("serializeMessage");
    try {
      json = serializer.toJson(message);
      String type = message.getDescriptorForType().getName();
      str = MessageWrapper.serialize(type, sequenceNo, json);
    } catch (SerializationException e) {
      LOG.warning("Failed to JSONify proto message", e);
      return;
    } finally {
      Timing.stop(timer);
    }
    try {
      sendMessageString(str);
      LOG.fine("sent JSON message over websocket, sequence number " + sequenceNo
          + ", message " + message);
    } catch (IOException e) {
      // TODO(anorth): This failure should be communicated to the caller
      // so it can attempt retransmission.
      LOG.warning("Failed to transmit message on socket, sequence number " + sequenceNo
          + ", message " + message, e);
      return;
    }
  }
}
