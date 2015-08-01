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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;

import org.waveprotocol.box.common.comms.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.box.common.comms.WaveClientRpc.WaveViewSnapshot;
import org.waveprotocol.box.common.comms.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.box.common.comms.proto.DocumentSnapshotProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolAuthenticateProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolAuthenticationResultProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolOpenRequestProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolSubmitRequestProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolSubmitResponseProtoImpl;
import org.waveprotocol.box.common.comms.proto.ProtocolWaveletUpdateProtoImpl;
import org.waveprotocol.box.common.comms.proto.WaveViewSnapshotProtoImpl;
import org.waveprotocol.box.common.comms.proto.WaveletSnapshotProtoImpl;
import org.waveprotocol.box.profile.ProfilesProto.ProfileResponse;
import org.waveprotocol.box.profile.proto.ProfileResponseProtoImpl;
import org.waveprotocol.box.search.SearchProto.SearchResponse;
import org.waveprotocol.box.search.proto.SearchResponseProtoImpl;
import org.waveprotocol.box.server.rpc.Rpc.CancelRpc;
import org.waveprotocol.box.server.rpc.Rpc.RpcFinished;
import org.waveprotocol.box.server.rpc.proto.CancelRpcProtoImpl;
import org.waveprotocol.box.server.rpc.proto.RpcFinishedProtoImpl;
import org.waveprotocol.box.attachment.AttachmentProto.AttachmentsResponse;
import org.waveprotocol.box.attachment.proto.AttachmentsResponseProtoImpl;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.communication.gson.GsonSerializable;
import org.waveprotocol.wave.communication.json.RawStringData;
import org.waveprotocol.wave.communication.proto.ProtoWrapper;

import java.util.Map;

/**
 * Serializes protos to/from JSON objects.
 * <p>
 * This class uses the PST-generated message classes to perform serialization
 * and deserialization.
 */
public final class ProtoSerializer {

  public static final class SerializationException extends Exception {
    public SerializationException(Exception cause) {
      super(cause);
    }

    public SerializationException(String message) {
      super(message);
    }
  }

  /**
   * Serializes protos of a particular type to/from JSON objects.
   *
   * @param <P> proto type
   * @param <D> GSON-based DTO wrapper type for a {@code P}
   */
  static final class ProtoImplSerializer<
      P extends Message,
      D extends ProtoWrapper<P> & GsonSerializable> {
    private final Class<P> protoClass;
    private final Class<D> dtoClass;

    ProtoImplSerializer(Class<P> protoClass, Class<D> dtoClass) {
      this.protoClass = protoClass;
      this.dtoClass = dtoClass;
    }

    static <P extends Message, D extends ProtoWrapper<P> & GsonSerializable>
        ProtoImplSerializer<P, D> of(
        Class<P> protoClass, Class<D> dtoClass) {
      return new ProtoImplSerializer<P, D>(protoClass, dtoClass);
    }

    D newDto() throws SerializationException {
      try {
        return dtoClass.newInstance();
      } catch (InstantiationException e) {
        throw new SerializationException(e);
      } catch (IllegalAccessException e) {
        throw new SerializationException(e);
      }
    }

    JsonElement toGson(MessageLite proto, RawStringData data, Gson gson)
        throws SerializationException {
      Preconditions.checkState(protoClass.isInstance(proto));
      D dto = newDto();
      dto.setPB(protoClass.cast(proto));
      return dto.toGson(data, gson);
    }

    P fromJson(JsonElement json, RawStringData data, Gson gson) throws SerializationException {
      D dto = newDto();
      try {
        dto.fromGson(json, gson, data);
      } catch (GsonException e) {
        throw new SerializationException(e);
      }
      return dto.getPB();
    }
  }

  private final Gson gson = new Gson();
  private final Map<Class<?>, ProtoImplSerializer<?, ?>> byClass = Maps.newHashMap();
  private final Map<String, ProtoImplSerializer<?, ?>> byName = Maps.newHashMap();

  public ProtoSerializer() {
    init();
  }

  /** Adds the known proto types. */
  private void init() {
    // Note: this list is too inclusive, but has historically always been so.
    // The real list only needs about 5 protos, since only top-level rpc types
    // need to be here, not every single recursively reachable proto.
    add(ProtocolAuthenticate.class, ProtocolAuthenticateProtoImpl.class);
    add(ProtocolAuthenticationResult.class, ProtocolAuthenticationResultProtoImpl.class);
    add(ProtocolOpenRequest.class, ProtocolOpenRequestProtoImpl.class);
    add(ProtocolSubmitRequest.class, ProtocolSubmitRequestProtoImpl.class);
    add(ProtocolSubmitResponse.class, ProtocolSubmitResponseProtoImpl.class);
    add(ProtocolWaveletUpdate.class, ProtocolWaveletUpdateProtoImpl.class);
    add(WaveletSnapshot.class, WaveletSnapshotProtoImpl.class);
    add(DocumentSnapshot.class, DocumentSnapshotProtoImpl.class);
    add(WaveViewSnapshot.class, WaveViewSnapshotProtoImpl.class);

    add(CancelRpc.class, CancelRpcProtoImpl.class);
    add(RpcFinished.class, RpcFinishedProtoImpl.class);

    add(SearchResponse.class, SearchResponseProtoImpl.class);
    add(ProfileResponse.class, ProfileResponseProtoImpl.class);

    add(AttachmentsResponse.class, AttachmentsResponseProtoImpl.class);
  }

  /** Adds a binding between a proto class and a DTO message class. */
  private <P extends Message, D extends ProtoWrapper<P> & GsonSerializable> void add(
      Class<P> protoClass, Class<D> dtoClass) {
    ProtoImplSerializer<P, D> serializer = ProtoImplSerializer.of(protoClass, dtoClass);
    byClass.put(protoClass, serializer);
    byName.put(protoClass.getSimpleName(), serializer);
  }

  /**
   * Gets the serializer for a proto class. Never returns null.
   *
   * @throws SerializationException if there is no serializer for
   *         {@code protoClass}.
   */
  private <P> ProtoImplSerializer<? extends P, ?> getSerializer(Class<P> protoClass)
      throws SerializationException {
    @SuppressWarnings("unchecked")
    // use of serializers map is safe.
    ProtoImplSerializer<? extends P, ?> serializer =
        (ProtoImplSerializer<? extends P, ?>) byClass.get(protoClass);
    if (serializer == null) {
      throw new SerializationException("Unknown proto class: " + protoClass.getName());
    }
    return serializer;
  }

  /**
   * Gets the serializer for a proto class name. Never returns null.
   *
   * @throws SerializationException if there is no serializer for
   *         {@code protoName}.
   */
  private <P extends Message> ProtoImplSerializer<P, ?> getSerializer(String protoName)
      throws SerializationException {
    @SuppressWarnings("unchecked")
    // use of serializers map is safe.
    ProtoImplSerializer<P, ?> serializer = (ProtoImplSerializer<P, ?>) byName.get(protoName);
    if (serializer == null) {
      throw new SerializationException("Unknown proto class: " + protoName);
    }
    return serializer;
  }

  /**
   * Serializes a proto to JSON. Only protos whose classes have been registered
   * will be serialized.
   *
   * @throws SerializationException if the class of {@code message} has not been
   *         registered.
   */
  public JsonElement toJson(MessageLite message) throws SerializationException {
    return getSerializer(message.getClass()).toGson(message, null, gson);
  }

  /**
   * Deserializes a proto from JSON. Only protos whose classes have been
   * registered can be deserialized.
   *
   * @throws SerializationException if no class called {@code type} has been
   *         registered.
   */
  public Message fromJson(JsonElement json, String type) throws SerializationException {
    return getSerializer(type).fromJson(json, null, gson);
  }

  // Utility method for a test.
  @VisibleForTesting
  public <P extends Message> P fromJson(JsonElement json, Class<P> clazz)
      throws SerializationException {
    return getSerializer(clazz).fromJson(json, null, gson);
  }
}
