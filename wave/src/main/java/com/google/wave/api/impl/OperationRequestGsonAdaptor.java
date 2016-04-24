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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcConstant.RequestProperty;
import com.google.wave.api.OperationRequest.Parameter;

import java.lang.reflect.Type;
import java.util.Map.Entry;

/**
 * Custom serializer and deserializer that serializes and deserializes an
 * {@link OperationRequest}.
 *
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class OperationRequestGsonAdaptor implements JsonSerializer<OperationRequest>,
    JsonDeserializer<OperationRequest> {

  /**
   * A namespace that should be prepended to the operation method during
   * serialization.
   */
  private final String operationNamespace;

  /**
   * Constructor.
   */
  public OperationRequestGsonAdaptor() {
    this("");
  }

  /**
   * Constructor.
   *
   * @param operationNamespace namespace that should be prepended to the
   *     operation method during serialization.
   */
  public OperationRequestGsonAdaptor(String operationNamespace) {
    if (operationNamespace != null && !operationNamespace.isEmpty() &&
        !operationNamespace.endsWith(".")) {
      operationNamespace += ".";
    }
    this.operationNamespace = operationNamespace;
  }

  @Override
  public JsonElement serialize(OperationRequest req, Type type, JsonSerializationContext ctx) {
    JsonObject object = new JsonObject();
    object.addProperty(RequestProperty.METHOD.key(), operationNamespace + req.getMethod());
    object.addProperty(RequestProperty.ID.key(), req.getId());

    JsonObject parameters = new JsonObject();
    for (Entry<ParamsProperty, Object> entry : req.getParams().entrySet()) {
      if (entry.getValue() != null) {
        parameters.add(entry.getKey().key(), ctx.serialize(entry.getValue()));
      }
    }
    object.add(RequestProperty.PARAMS.key(), parameters);
    return object;
  }

  @Override
  public OperationRequest deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    JsonObject parameters = jsonObject.getAsJsonObject(RequestProperty.PARAMS.key());

    OperationRequest request = new OperationRequest(
        jsonObject.get(RequestProperty.METHOD.key()).getAsString(),
        jsonObject.get(RequestProperty.ID.key()).getAsString(),
        getPropertyAsStringThenRemove(parameters, ParamsProperty.WAVE_ID),
        getPropertyAsStringThenRemove(parameters, ParamsProperty.WAVELET_ID),
        getPropertyAsStringThenRemove(parameters, ParamsProperty.BLIP_ID));

    for (Entry<String, JsonElement> parameter : parameters.entrySet()) {
      ParamsProperty parameterType = ParamsProperty.fromKey(parameter.getKey());
      if (parameterType != null) {
        Object object;
        if (parameterType == ParamsProperty.RAW_DELTAS) {
          object = ctx.deserialize(parameter.getValue(), GsonFactory.RAW_DELTAS_TYPE);
        } else {
          object = ctx.deserialize(parameter.getValue(), parameterType.clazz());
        }
        request.addParameter(Parameter.of(parameterType, object));
      }
    }

    return request;
  }

  /**
   * Returns a property of {@code JsonObject} as a {@link String}, then remove
   * that property.
   *
   * @param jsonObject the {@code JsonObject} to get the property from.
   * @param key the key of the property.
   * @return the property as {@link String}, or {@code null} if not found.
   */
  private static String getPropertyAsStringThenRemove(JsonObject jsonObject, ParamsProperty key) {
    JsonElement property = jsonObject.get(key.key());
    if (property != null) {
      jsonObject.remove(key.key());
      if (property.isJsonNull()) {
        return null;
      }
      return property.getAsString();
    }
    return null;
  }
}
