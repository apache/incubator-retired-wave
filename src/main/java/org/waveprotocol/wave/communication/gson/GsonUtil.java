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

package org.waveprotocol.wave.communication.gson;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.waveprotocol.wave.communication.json.JsonLongHelper;
import org.waveprotocol.wave.communication.json.RawStringData;


/**
 * Utilities for working with GsonSerializable objects
 *
 */
public class GsonUtil {

  /**
   * A wrapper routine used to catch exceptions thrown while parsing and convert
   * them to GsonExceptions
   *
   * @param <T> A GsonSerializable object class
   * @param obj The object
   * @param json The root of a tree of JsonElements
   * @param gson The gson object used to parse multi-stage json
   * @param data The raw data object that contains the substring
   * @return The provided object
   * @throws GsonException
   */
  public static <T extends GsonSerializable> T parseGson(T obj, JsonElement json, Gson gson,
      RawStringData data) throws GsonException {
    try {
      obj.fromGson(json, gson, data);
      return obj;
    } catch (IllegalArgumentException e) {
      throw new GsonException("Unable to parse JSON", e);
    } catch (IllegalStateException e) {
      throw new GsonException("Unable to parse JSON", e);
    } catch (UnsupportedOperationException e) {
      throw new GsonException("Unable to parse JSON", e);
    }
  }

  /**
   * A wrapper routine used to catch exceptions thrown while parsing and convert
   * them to GsonExceptions
   *
   * @param <T> The type to deserialize
   * @param gson A Gson context
   * @param json The json string to interpret
   * @throws GsonException
   */
  public static <T extends GsonSerializable> void parseJson(T object,
      Gson gson, String json, RawStringData data) throws GsonException {
    try {
      JsonElement root = new JsonParser().parse(json);
      object.fromGson(root, gson, data);
    } catch (JsonParseException e) {
      throw new GsonException("Unable to parse Json", e);
    }
  }

  /**
   * A wrapper routine used to catch exceptions thrown while parsing and convert
   * them to GsonExceptions
   *
   * @param <T> The type to deserialize
   * @param gson A Gson context
   * @param json The json string to interpret
   * @param clazz The class object representing T
   * @return An instance representing the parsed json
   * @throws GsonException
   */
  public static <T> T parseJson(Gson gson, String json, Class<T> clazz) throws GsonException {
    try {
      return gson.fromJson(json, clazz);
    } catch (JsonParseException e) {
      throw new GsonException("Unable to parse Json", e);
    }
  }

  /**
   * Unpack a JsonElement into the object type
   *
   * @param <T> The type to deserialize
   * @param object The object used to accept the pare result
   * @param valueObj The root of a tree of JsonElements or an indirection index
   * @param gson A Gson context
   * @param raw
   * @throws GsonException
   */
  public static <T extends GsonSerializable> void
      extractJsonObject(T object, JsonElement valueObj, Gson gson, RawStringData raw)
      throws GsonException {
    if (valueObj.isJsonObject()) {
      object.fromGson(valueObj.getAsJsonObject(), gson, raw);
    } else if (valueObj.isJsonPrimitive()) {
      JsonPrimitive primitive = valueObj.getAsJsonPrimitive();
      String s = null;
      if (raw == null || !primitive.isString()) {
        throw new GsonException("Decoding " + valueObj + " as object " + object.getClass() +
            " with no RawStringData given");
      }
      s = raw.getString(valueObj.getAsString());
      GsonUtil.parseJson(object, gson, s, raw);
    } else {
      throw new GsonException("Cannot decode valueObject " + valueObj.getClass() +
          " as object " + object.getClass());
    }
  }

  /**
   * Faithfully serializes a 64-bit long value as a two-number array.
   */
  public static JsonArray toJson(long value) {
    JsonArray arr = new JsonArray();
    arr.add(new JsonPrimitive(JsonLongHelper.getLowWord(value)));
    arr.add(new JsonPrimitive(JsonLongHelper.getHighWord(value)));
    return arr;
  }

  /**
   * Deserializes a two-number array from {@link #toJson(long)} into a long.
   */
  public static long fromJson(JsonElement e) {
    JsonArray arr = e.getAsJsonArray();
    return JsonLongHelper.toLong(arr.get(1).getAsInt(), arr.get(0).getAsInt());
  }

  private GsonUtil() {
  }
}
