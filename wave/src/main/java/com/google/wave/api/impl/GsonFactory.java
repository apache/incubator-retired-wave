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

package com.google.wave.api.impl;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.wave.api.Annotation;
import com.google.wave.api.Attachment;
import com.google.wave.api.BlipData;
import com.google.wave.api.Element;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.NonJsonSerializable;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.Range;
import com.google.wave.api.BlipThread;
import com.google.wave.api.SearchResult;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.model.id.WaveletId;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.io.UnsupportedEncodingException;

/**
 * A factory to instantiate a {@link Gson} instance, with pre-registered type
 * adapters for serializing and deserializing Wave API classes that are used
 * as data transfer objects.
 */
public class GsonFactory {

  public static final Type BLIP_MAP_TYPE = new TypeToken<Map<String, BlipData>>(){}.getType();
  public static final Type PARTICIPANT_LIST_TYPE = new TypeToken<List<String>>(){}.getType();
  public static final Type THREAD_MAP_TYPE = new TypeToken<Map<String, BlipThread>>(){}.getType();
  public static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();
  public static final Type WAVELET_ID_LIST_TYPE = new TypeToken<List<WaveletId>>(){}.getType();
  public static final Type RAW_DELTAS_TYPE = new TypeToken<List<byte[]>>(){}.getType();
  public static final Type RAW_ATTACHMENT_MAP_TYPE = new TypeToken<Map<String, RawAttachmentData>>(){}
      .getType();
  public static final Type OPERATION_REQUEST_LIST_TYPE = new TypeToken<List<OperationRequest>>(){}
      .getType();
  public static final Type JSON_RPC_RESPONSE_LIST_TYPE = new TypeToken<List<JsonRpcResponse>>(){}
      .getType();

  /** Additional type adapters. */
  private final Map<Type, Object> customTypeAdapters = new LinkedHashMap<Type, Object>();

  /**
   * Registers a custom type adapter.
   *
   * @param type the type that will be handled by the given adapter.
   * @param typeAdapter the adapter that performs the serialization and
   *     deserialization.
   */
  public void registerTypeAdapter(Type type, Object typeAdapter) {
    customTypeAdapters.put(type, typeAdapter);
  }

  /**
   * Creates a {@link Gson} instance, with additional type adapters for these
   * types:
   * <ul>
   *   <li>{@link EventMessageBundle}</li>
   *   <li>{@link OperationRequest}</li>
   *   <li>{@link Element}</li>
   * </ul>
   *
   * @return an instance of {@link Gson} with pre-registered type adapters.
   */
  public Gson create() {
    return create("");
  }

  /**
   * Creates a {@link Gson} instance, with additional type adapters for these
   * types:
   * <ul>
   *   <li>{@link EventMessageBundle}</li>
   *   <li>{@link OperationRequest}</li>
   *   <li>{@link Element}</li>
   *   <li>{@link JsonRpcResponse}</li>
   * </ul>
   *
   * @param opNamespace prefix that should be prepended to the operation during
   *     serialization.
   * @return an instance of {@link Gson} with pre-registered type adapters.
   */
  public Gson create(String opNamespace) {
    ElementGsonAdaptor elementGsonAdaptor = new ElementGsonAdaptor();
    GsonBuilder builder = new GsonBuilder()
        .setExclusionStrategies(new NonSerializableExclusionStrategy())
        .registerTypeAdapter(EventMessageBundle.class, new EventMessageBundleGsonAdaptor())
        .registerTypeAdapter(OperationRequest.class, new OperationRequestGsonAdaptor(opNamespace))
        .registerTypeAdapter(Element.class, elementGsonAdaptor)
        .registerTypeAdapter(Attachment.class, elementGsonAdaptor)
        .registerTypeAdapter(JsonRpcResponse.class, new JsonRpcResponseGsonAdaptor())
        .registerTypeAdapter(Annotation.class, new AnnotationInstanceCreator())
        .registerTypeAdapter(Range.class, new RangeInstanceCreator())
        .registerTypeAdapter(BlipThread.class, new ThreadInstanceCreator())
        .registerTypeAdapter(SearchResult.class, new SearchResultInstanceCreator())
        .registerTypeAdapter(SearchResult.Digest.class, new SearchResultDigestInstanceCreator())
        .registerTypeAdapter(WaveletId.class, new WaveletIdInstanceCreator())
        .registerTypeAdapter(RawAttachmentData.class, new AttachmentDataInstanceCreator())
        .registerTypeAdapter(byte[].class, new ByteArrayGsonAdaptor());

    // Register custom type adapters.
    for (Entry<Type, Object> entry : customTypeAdapters.entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }

    return builder.serializeNulls().create();
  }

  /**
   * A strategy definition that excludes all fields that are annotated with
   * {@link NonJsonSerializable}.
   */
  private static class NonSerializableExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return f.getAnnotation(NonJsonSerializable.class) != null;
    }
  }

  /**
   * An instance creator that creates an empty {@link Annotation}.
   */
  private static class AnnotationInstanceCreator implements InstanceCreator<Annotation> {
    @Override
    public Annotation createInstance(Type type) {
      return new Annotation("", "", -1, -1);
    }
  }

  /**
   * An instance creator that creates an empty {@link Annotation}.
   */
  private static class RangeInstanceCreator implements InstanceCreator<Range> {
    @Override
    public Range createInstance(Type type) {
      return new Range(-1, -1);
    }
  }

  /**
   * An instance creator that creates an empty {@link BlipThread}.
   */
  private static class ThreadInstanceCreator implements InstanceCreator<BlipThread> {
    @Override
    public BlipThread createInstance(Type type) {
      return new BlipThread(null, -1, null, null);
    }
  }

  /**
   * An instance creator that creates an empty {@link SearchResult}.
   */
  private static class SearchResultInstanceCreator implements InstanceCreator<SearchResult> {
    @Override
    public SearchResult createInstance(Type type) {
      return new SearchResult("");
    }
  }

  /**
   * An instance creator that creates an empty {@link SearchResult.Digest}.
   */
  private static class SearchResultDigestInstanceCreator implements
      InstanceCreator<SearchResult.Digest> {
    @Override
    public SearchResult.Digest createInstance(Type type) {
      List<String> participants = Lists.newLinkedList();
      return new SearchResult.Digest("", "", "", participants, -1, -1, -1, -1);
    }
  }

  /**
   * An instance creator that creates an empty {@link WaveletId}.
   */
  private static class WaveletIdInstanceCreator implements
      InstanceCreator<WaveletId> {
    @Override
    public WaveletId createInstance(Type type) {
      return WaveletId.of("dummy", "dummy");
    }
  }

  /**
   * An instance creator that creates an empty {@link AttachmentData}.
   */
  private static class AttachmentDataInstanceCreator implements InstanceCreator<RawAttachmentData> {
    @Override
    public RawAttachmentData createInstance(Type type) {
      return new RawAttachmentData("", "", new byte[0]);
    }
  }

  /**
   * An instance creator serializer and deserializer that creates
   * serializes and deserializes an empty {@link ByteString}.
   */
  private static class ByteArrayGsonAdaptor implements InstanceCreator<byte[]>,
      JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    @Override
    public byte[] createInstance(Type type) {
      return new byte[0];
    }

    @Override
    public JsonElement serialize(byte[] bytes, Type type, JsonSerializationContext jsc) {
      try {
        return new JsonPrimitive(new String(Base64.encodeBase64(bytes), "UTF-8"));
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public byte[] deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
      try {
        return Base64.decodeBase64(je.getAsString().getBytes("UTF-8"));
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
