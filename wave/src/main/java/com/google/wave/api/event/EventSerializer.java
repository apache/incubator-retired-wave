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

package com.google.wave.api.event;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.wave.api.Wavelet;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.impl.EventMessageBundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Object that is responsible for serializing and deserializing implementors
 * of {@link Event} to and from {@link JsonObject}.
 */
public class EventSerializer {

  /**
   * An interface for map key conversion.
   *
   * @param <T> the generic type of the resulting key.
   */
  private static interface KeyConverter<T> {

    /**
     * Converts the given string key into an instance of {@code T}.
     *
     * @param key the key to be converted.
     * @return an instance of {@code T} that represents the given string key.
     */
    T convert(String key);
  }

  /** Constants for accessing the properties of the given JSON object. */
  private static final String TYPE = "type";
  private static final String MODIFIED_BY = "modifiedBy";
  private static final String TIMESTAMP = "timestamp";
  private static final String BLIP_ID = "blipId";
  private static final String WAVELET = "wavelet";
  private static final String PROPERTIES = "properties";
  private static final String BUNDLE = "bundle";

  /**
   * Serializes the given {@link Event} into a {@link JsonObject}.
   *
   * @param event the {@link Event} to be serialized.
   * @param context the serialization context.
   * @return an instance of {@link JsonObject}, that is the JSON representation
   *     of the given {@link Event}.
   */
  public static JsonObject serialize(Event event, JsonSerializationContext context)
      throws EventSerializationException {
    JsonObject result = new JsonObject();

    // Serialize basic properties from Event.
    result.addProperty(TYPE, event.getType().name());
    result.addProperty(MODIFIED_BY, event.getModifiedBy());
    result.addProperty(TIMESTAMP, event.getTimestamp());
    result.addProperty(TIMESTAMP, event.getTimestamp());

    // Construct a properties object.
    JsonObject properties = new JsonObject();

    try {
      // Serialize the blip id.
      Field blipIdField = AbstractEvent.class.getDeclaredField(BLIP_ID);
      blipIdField.setAccessible(true);
      properties.addProperty(BLIP_ID, (String) blipIdField.get(event));

      // Serialize event specific properties.
      for (Field field : event.getClass().getDeclaredFields()) {
        field.setAccessible(true);
        properties.add(field.getName(), context.serialize(field.get(event)));
      }
    } catch (IllegalArgumentException e) {
      throw new EventSerializationException("Unable to serialize event: " + BLIP_ID +
          " in " +  event.getClass() + " is not accessible.");
    } catch (IllegalAccessException e) {
      throw new EventSerializationException("Unable to serialize event: " + BLIP_ID +
          " in " +  event.getClass() + " is not accessible.");
    } catch (NoSuchFieldException e) {
      throw new EventSerializationException("Unable to serialize event: " + BLIP_ID +
          " in " +  event.getClass() + " is not accessible.");
    }

    result.add(PROPERTIES, properties);
    return result;
  }

  /**
   * Deserializes the given {@link JsonObject} into an {@link Event}, and
   * assign the given {@link Wavelet} to the {@link Event}.
   *
   * @param wavelet the wavelet where the event occurred.
   * @param json the JSON representation of {@link Event}.
   * @param context the deserialization context.
   * @return an instance of {@link Event}.
   *
   * @throw {@link EventSerializationException} if there is a problem
   *     deserializing the event JSON.
   */
  public static Event deserialize(Wavelet wavelet, EventMessageBundle bundle, JsonObject json,
      JsonDeserializationContext context) throws EventSerializationException {
    // Construct the event object.
    String eventTypeString = json.get(TYPE).getAsString();
    EventType type = EventType.valueOfIgnoreCase(eventTypeString);
    if (type == EventType.UNKNOWN) {
      throw new EventSerializationException("Trying to deserialize event JSON with unknown " +
          "type: " + json, json);
    }

    // Parse the generic parameters.
    String modifiedBy = json.get(MODIFIED_BY).getAsString();
    Long timestamp = json.get(TIMESTAMP).getAsLong();

    // Construct the event object.
    Class<? extends Event> clazz = type.getClazz();
    Constructor<? extends Event> ctor;
    try {
      ctor = clazz.getDeclaredConstructor();
      ctor.setAccessible(true);
      Event event = ctor.newInstance();

      // Set the default fields from AbstractEvent.
      Class<?> rootClass = AbstractEvent.class;
      setField(event, rootClass.getDeclaredField(WAVELET), wavelet);
      setField(event, rootClass.getDeclaredField(MODIFIED_BY), modifiedBy);
      setField(event, rootClass.getDeclaredField(TIMESTAMP), timestamp);
      setField(event, rootClass.getDeclaredField(TYPE), type);
      setField(event, rootClass.getDeclaredField(BUNDLE), bundle);

      JsonObject properties = json.get(PROPERTIES).getAsJsonObject();

      // Set the blip id field, that can be null for certain events, such as
      // OPERATION_ERROR.
      JsonElement blipId = properties.get(BLIP_ID);
      if (blipId != null && !(blipId instanceof JsonNull)) {
        setField(event, rootClass.getDeclaredField(BLIP_ID), blipId.getAsString());
      }

      // Set the additional fields.
      for (Field field : clazz.getDeclaredFields()) {
        String fieldName = field.getName();
        if (properties.has(fieldName)) {
          setField(event, field, context.deserialize(properties.get(fieldName),
              field.getGenericType()));
        }
      }
      return event;
    } catch (NoSuchMethodException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    } catch (NoSuchFieldException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    } catch (InstantiationException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    } catch (IllegalAccessException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    } catch (InvocationTargetException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    } catch (JsonParseException e) {
      throw new EventSerializationException("Unable to deserialize event JSON: " + json, json);
    }
  }

  /**
   * Extracts event specific properties into a map. This method will not include
   * the basic properties from {@link AbstractEvent}, except for blip id, in the
   * resulting map.
   *
   * @param event the event whose properties will be extracted.
   * @return a map of {@link ParamsProperty} to {@link Object} of properties.
   *
   * @throws EventSerializationException if there is a problem accessing the
   *     event's fields.
   */
  public static Map<ParamsProperty, Object> extractPropertiesToParamsPropertyMap(Event event)
      throws EventSerializationException {
    return extractProperties(event, new KeyConverter<ParamsProperty>() {
      @Override
      public ParamsProperty convert(String key) {
        return ParamsProperty.fromKey(key);
      }
    });
  }

  /**
   * Extracts event specific properties into a map. This method will not include
   * the basic properties from {@link AbstractEvent}, except for blip id, in the
   * resulting map.
   *
   * @param event the event whose properties will be extracted.
   * @return a map of {@link String} to {@link Object} of properties.
   *
   * @throws EventSerializationException if there is a problem accessing the
   *     event's fields.
   */
  public static Map<String, Object> extractPropertiesToStringMap(Event event)
      throws EventSerializationException {
    return extractProperties(event, new KeyConverter<String>() {
      @Override
      public String convert(String key) {
        return key;
      }
    });
  }

  /**
   * Extracts event specific properties into a map. This method will not include
   * the basic properties from {@link AbstractEvent}, except for blip id, in the
   * resulting map.
   *
   * @param event the event whose properties will be extracted.
   * @param keyConverter the converter to convert the event property name into
   *     a proper key object for the resulting map.
   * @return a map of {@code T} to {@link Object} of properties.
   *
   * @throws EventSerializationException if there is a problem accessing the
   *     event's fields.
   */
  private static <T> Map<T, Object> extractProperties(Event event, KeyConverter<T> keyConverter)
      throws EventSerializationException {
    Field[] fields = event.getClass().getDeclaredFields();
    Map<T, Object> data = new HashMap<T, Object>(fields.length + 1);
    try {
      for (Field field : fields) {
        field.setAccessible(true);
        data.put(keyConverter.convert(field.getName()), field.get(event));
      }

      Field field = AbstractEvent.class.getDeclaredField(BLIP_ID);
      field.setAccessible(true);
      data.put(keyConverter.convert(BLIP_ID), field.get(event));
    } catch (IllegalArgumentException e) {
      throw new EventSerializationException(e.getMessage());
    } catch (IllegalAccessException e) {
      throw new EventSerializationException(e.getMessage());
    } catch (NoSuchFieldException e) {
      throw new EventSerializationException(e.getMessage());
    }
    return data;
  }

  /**
   * Sets the field of the given {@link Event} object.
   *
   * @param event the {@link Event} object whose field will be set.
   * @param field the {@link Field} object that represents the field.
   * @param value the value to be set to the field.
   *
   * @throws IllegalAccessException if the field is not accessible.
   */
  private static void setField(Event event, Field field, Object value)
      throws IllegalAccessException {
    field.setAccessible(true);
    field.set(event, value);
  }
}
