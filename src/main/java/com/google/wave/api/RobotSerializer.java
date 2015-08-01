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

import static com.google.wave.api.OperationType.ROBOT_NOTIFY;
import static com.google.wave.api.OperationType.ROBOT_NOTIFY_CAPABILITIES_HASH;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcConstant.RequestProperty;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Utility class to serialize and deserialize Events and Operations to and from
 * JSON string for V2.* of the protocol.
 * 
 * @author mprasetya@google.com (Marcel Prasetya)
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotSerializer {

  /** The counter for protocol versions. */
  public static final Map<ProtocolVersion, AtomicInteger> PROTOCOL_VERSION_COUNTERS;

  static {
    PROTOCOL_VERSION_COUNTERS = new HashMap<ProtocolVersion, AtomicInteger>();
    // Put in the V2 protocols that are used in this serializer
    for (ProtocolVersion protcolVersion : ProtocolVersion.values()) {
      if (protcolVersion.isGreaterThanOrEqual(ProtocolVersion.V2)) {
        PROTOCOL_VERSION_COUNTERS.put(protcolVersion, new AtomicInteger());
      }
    }
  }

  private static final Logger LOG = Logger.getLogger(RobotSerializer.class.getName());

  /** An map of {@link Gson}s for serializing and deserializing JSON. */
  private final NavigableMap<ProtocolVersion, Gson> gsons;

  /** The default protocol version. */
  private final ProtocolVersion defaultProtocolVersion;

  /**
   * An instance of {@link JsonParser} to parse JSON string into
   * {@link JsonElement}.
   */
  private final JsonParser jsonParser;

  /**
   * Constructor. Note that the defaultprotocol version must occur in the map
   * of {@link Gson}s.
   *
   * @param gsons an map of {@link Gson}s for serializing and deserializing
   *     JSON, keyed by protocol version.
   * @param defaultProtocolVersion the default protocol version.
   */
  public RobotSerializer(NavigableMap<ProtocolVersion, Gson> gsons,
      ProtocolVersion defaultProtocolVersion) {
    if (!gsons.containsKey(defaultProtocolVersion)) {
      throw new IllegalArgumentException(
          "The serializer map does not contain a serializer for the default protocol version");
    }
    this.gsons = gsons;
    this.defaultProtocolVersion = defaultProtocolVersion;
    this.jsonParser = new JsonParser();
  }

  /**
   * Deserializes the given JSON string into an instance of the given type.
   *
   * @param <T> the generic type of the given class.
   * @param jsonString the JSON string to deserialize.
   * @param type the type to deserialize the JSON string into.
   * @param protocolVersion the wire protocol version of the given JSON string.
   * @return an instance of {@code type}, that is constructed by deserializing
   *     the given {@code jsonString}
   */
  public <T> T deserialize(String jsonString, Type type, ProtocolVersion protocolVersion) {
    return getGson(protocolVersion).<T>fromJson(jsonString, type);
  }

  /**
   * Serializes the given object into a JSON string.
   *
   * @param <T> the generic type of the given object.
   * @param object the object to serialize.
   * @return a JSON string representation of {@code object}.
   */
  public <T> String serialize(T object) {
    return serialize(object, defaultProtocolVersion);
  }

  /**
   * Serializes the given object into a JSON string.
   *
   * @param <T> the generic type of the given object.
   * @param object the object to serialize.
   * @param type the specific genericized type of {@code object}.
   * @return a JSON string representation of {@code object}.
   */
  public <T> String serialize(T object, Type type) {
    return serialize(object, type, defaultProtocolVersion);
  }

  /**
   * Serializes the given object into a JSON string.
   *
   * @param <T> the generic type of the given object.
   * @param object the object to serialize.
   * @param protocolVersion the version of the serializer to use.
   * @return a JSON string representation of {@code object}.
   */
  public <T> String serialize(T object, ProtocolVersion protocolVersion) {
    return getGson(protocolVersion).toJson(object);
  }

  /**
   * Serializes the given object into a JSON string.
   *
   * @param <T> the generic type of the given object.
   * @param object the object to serialize.
   * @param type the specific genericized type of {@code object}.
   * @param protocolVersion the version of the serializer to use.
   * @return a JSON string representation of {@code object}.
   */
  public <T> String serialize(T object, Type type, ProtocolVersion protocolVersion) {
    return getGson(protocolVersion).toJson(object, type);
  }

  /**
   * Parses the given JSON string into a {@link JsonElement}.
   *
   * @param jsonString the string to parse.
   * @return a {@link JsonElement} representation of the input
   *     {@code jsonString}.
   */
  public JsonElement parse(String jsonString) {
    return jsonParser.parse(jsonString);
  }

  /**
   * Deserializes operations. This method supports only the new JSON-RPC style
   * operations.
   *
   * @param jsonString the operations JSON string to deserialize.
   * @return a list of {@link OperationRequest},that represents the operations.
   * @throws InvalidRequestException if there is a problem deserializing the
   *     operations.
   */
  public List<OperationRequest> deserializeOperations(String jsonString)
      throws InvalidRequestException {
    if (Util.isEmptyOrWhitespace(jsonString)) {
      return Collections.emptyList();
    }

    // Parse incoming operations.
    JsonArray requestsAsJsonArray = null;

    JsonElement json = null;
    try {
      json = jsonParser.parse(jsonString);
    } catch (JsonParseException e) {
      throw new InvalidRequestException("Couldn't deserialize incoming operations: " +
          jsonString, null, e);
    }

    if (json.isJsonArray()) {
      requestsAsJsonArray = json.getAsJsonArray();
    } else {
      requestsAsJsonArray = new JsonArray();
      requestsAsJsonArray.add(json);
    }

    // Convert incoming operations into a list of JsonRpcRequest.
    ProtocolVersion protocolVersion = determineProtocolVersion(requestsAsJsonArray);
    PROTOCOL_VERSION_COUNTERS.get(protocolVersion).incrementAndGet();
    List<OperationRequest> requests = new ArrayList<OperationRequest>(requestsAsJsonArray.size());
    for (JsonElement requestAsJsonElement : requestsAsJsonArray) {
      validate(requestAsJsonElement);
      requests.add(getGson(protocolVersion).fromJson(requestAsJsonElement,
          OperationRequest.class));
    }
    return requests;
  }

  /**
   * Determines the protocol version of a given operation bundle JSON by
   * inspecting the first operation in the bundle. If it is a
   * {@code robot.notify} operation, and contains {@code protocolVersion}
   * parameter, then this method will return the value of that parameter.
   * Otherwise, this method will return the default version.
   *
   * @param operationBundle the operation bundle to check.
   * @return the wire protocol version of the given operation bundle.
   */
  private ProtocolVersion determineProtocolVersion(JsonArray operationBundle) {
    if (operationBundle.size() == 0 || !operationBundle.get(0).isJsonObject()) {
      return defaultProtocolVersion;
    }

    JsonObject firstOperation = operationBundle.get(0).getAsJsonObject();
    if (!firstOperation.has(RequestProperty.METHOD.key())) {
      return defaultProtocolVersion;
    }

    String method = firstOperation.get(RequestProperty.METHOD.key()).getAsString();
    if (isRobotNotifyOperationMethod(method)) {
      JsonObject params = firstOperation.get(RequestProperty.PARAMS.key()).getAsJsonObject();
      if (params.has(ParamsProperty.PROTOCOL_VERSION.key())) {
        JsonElement protocolVersionElement = params.get(ParamsProperty.PROTOCOL_VERSION.key());
        if (!protocolVersionElement.isJsonNull()) {
          return ProtocolVersion.fromVersionString(protocolVersionElement.getAsString());
        }
      }
    }
    return defaultProtocolVersion;
  }

  /**
   * Determines the protocol version of a given operation bundle by inspecting
   * the first operation in the bundle. If it is a {@code robot.notify}
   * operation, and contains {@code protocolVersion} parameter, then this method
   * will return the value of that parameter. Otherwise, this method will return
   * the default version.
   *
   * @param operationBundle the operation bundle to check.
   * @return the wire protocol version of the given operation bundle.
   */
  private ProtocolVersion determineProtocolVersion(List<OperationRequest> operationBundle) {
    if (operationBundle.size() == 0) {
      return defaultProtocolVersion;
    }

    OperationRequest firstOperation = operationBundle.get(0);
    if (isRobotNotifyOperationMethod(firstOperation.getMethod())) {
      String versionString = (String) firstOperation.getParameter(ParamsProperty.PROTOCOL_VERSION);
      if (versionString != null) {
        return ProtocolVersion.fromVersionString(versionString);
      }
    }
    return defaultProtocolVersion;
  }

  /**
   * Serializes a list of {@link OperationRequest} objects into a JSON string.
   *
   * @param operations List of operations to serialize.
   * @return A JSON string representing the serialized operations.
   */
  public String serializeOperations(List<OperationRequest> operations)
      throws JsonParseException {
    ProtocolVersion protocolVersion = determineProtocolVersion(operations);
    return getGson(protocolVersion).toJson(operations);
  }

  /**
   * Returns an instance of Gson for the given protocol version.
   *
   * @param protocolVersion the protocol version.
   * @return an instance of {@link Gson}.
   */
  private Gson getGson(ProtocolVersion protocolVersion) {
    // Returns the last entry which protocol version is less than or equal to
    // the given protocol version.
    Entry<ProtocolVersion, Gson> entry = gsons.floorEntry(protocolVersion);
    if (entry == null) {
      LOG.severe("Could not find the proper Gson for protocol version " + protocolVersion);
      return null;
    }
    return entry.getValue();
  }

  /**
   * Validates that the incoming JSON is a JSON object that represents a
   * JSON-RPC request.
   *
   * @param jsonElement the incoming JSON.
   * @throws InvalidRequestException if the incoming JSON does not have the
   *     required properties.
   */
  private static void validate(JsonElement jsonElement) throws InvalidRequestException {
    if (!jsonElement.isJsonObject()) {
      throw new InvalidRequestException("The incoming JSON is not a JSON object: " + jsonElement);
    }

    JsonObject jsonObject = jsonElement.getAsJsonObject();

    StringBuilder missingProperties = new StringBuilder();
    for (RequestProperty requestProperty : RequestProperty.values()) {
      if (!jsonObject.has(requestProperty.key())) {
        missingProperties.append(requestProperty.key());
      }
    }

    if (missingProperties.length() > 0) {
      throw new InvalidRequestException("Missing required properties " + missingProperties +
          "operation: " + jsonObject);
    }
  }

  /**
   * Checks whether the given operation method is of a robot notify operation.
   *
   * @param method the method to check.
   * @return {@code true} if the given method is a robot notify operation's
   *     method.
   */
  @SuppressWarnings("deprecation")
  private static boolean isRobotNotifyOperationMethod(String method) {
    return ROBOT_NOTIFY_CAPABILITIES_HASH.method().equals(method) ||
        ROBOT_NOTIFY.method().equals(method);
  }
}
