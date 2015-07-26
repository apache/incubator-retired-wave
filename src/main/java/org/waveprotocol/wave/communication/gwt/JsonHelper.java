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

package org.waveprotocol.wave.communication.gwt;

import org.waveprotocol.wave.communication.json.RawStringData;

import com.google.gwt.core.client.JavaScriptObject;


/**
 * Helper class for manipulating JSOs.
 *
 */
public class JsonHelper {

  @SuppressWarnings("unused") // used in JSNI functions
  /**
   * The key to use inside the JSO object to store the raw string look up function.
   */
  private final static String RAW_DATA_LOOKUP_FUNCTION_KEY = "__g";

  /**
   * Test that a property exists on a given JavaScriptObject.
   * @param object The object containing the property to check.
   * @param key The property name to check.
   * @return true if the property exists, otherwise false.
   */
  public static final native boolean hasProperty(JavaScriptObject object, String key) /*-{
    return object.hasOwnProperty(key);
  }-*/;

  /**
   * Delete a property of a given JavaScriptObject.
   * @param object The object containing the property to delete.
   * @param key The name of the property to delete.
   */
  public static final native void deleteProperty(JavaScriptObject object, String key) /*-{
    delete object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a JavaScriptObject object.
   * @param object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native JavaScriptObject
      getPropertyAsObject(JavaScriptObject object, String key) /*-{
    var func = object[
      @org.waveprotocol.wave.communication.gwt.JsonHelper::RAW_DATA_LOOKUP_FUNCTION_KEY
    ];
    return func ? func(object,key,func) : object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a JavaScriptObject object.
   *
   * @param object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the data at the given index.
   */
  public static final native JavaScriptObject
      getArrayIndexAsObject(JavaScriptObject object, String key, int index) /*-{
    var func = object[
      @org.waveprotocol.wave.communication.gwt.JsonHelper::RAW_DATA_LOOKUP_FUNCTION_KEY
    ];
    var arr = object[key];
    return func ? func(arr,index,func) : arr[index];
  }-*/;

  /**
   * Register the raw string data that contains the continuation needed for the given object
   *
   * @param object
   * @param data The RawStringData that contains serialized JSON used to further decode the object.
   */
  public static final native void registerRawStringData(
      JavaScriptObject object, RawStringData data) /*-{
    object[@org.waveprotocol.wave.communication.gwt.JsonHelper::RAW_DATA_LOOKUP_FUNCTION_KEY
      ] = function(object, index, func) {
          if (typeof(object[index]) == "string") {
            var str = data.
                @org.waveprotocol.wave.communication.json.RawStringData::getString(Ljava/lang/String;)
                (object[index])
            object[index] =
                @org.waveprotocol.wave.communication.gwt.JsonMessage::createJsonMessage(Ljava/lang/String;)
                (str);
          }
          if (!object[index][
              @org.waveprotocol.wave.communication.gwt.JsonHelper::RAW_DATA_LOOKUP_FUNCTION_KEY
            ]) {
            object[index][
                @org.waveprotocol.wave.communication.gwt.JsonHelper::RAW_DATA_LOOKUP_FUNCTION_KEY
              ] = func;
          }
          return object[index];
    };
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a String.
   * @param object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native String getPropertyAsString(JavaScriptObject object, String key) /*-{
    return object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a double.
   * @param object object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native double getPropertyAsDouble(JavaScriptObject object, String key) /*-{
    return object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a float.
   * @param object object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native float getPropertyAsFloat(JavaScriptObject object, String key) /*-{
    return object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a long.
   * @param object object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final long getPropertyAsLong(JavaScriptObject object, String key) {
    JsLong value = getPropertyAsObject(object, key).cast();
    return value.toLong();
  }

  /**
   * Get the value of a property of a JavaScriptObject as an int.
   * @param object object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native int getPropertyAsInteger(JavaScriptObject object, String key) /*-{
    return object[key];
  }-*/;

  /**
   * Get the value of a property of a JavaScriptObject as a boolean.
   * @param object object The object containing the property to access.
   * @param key The name of the property to return.
   * @return The value of the property.
   */
  public static final native boolean getPropertyAsBoolean(JavaScriptObject object, String key) /*-{
    return object[key];
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsObject(JavaScriptObject object, String key,
                                                      JavaScriptObject value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsString(JavaScriptObject object, String key,
                                                      String value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsDouble(JavaScriptObject object,
                                                      String key, double value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsFloat(JavaScriptObject object,
                                                     String key, float value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final void setPropertyAsLong(JavaScriptObject object, String key, long value) {
    setPropertyAsObject(object, key, JsLong.create(value));
  }

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsInteger(JavaScriptObject object,
                                                           String key, int value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Set the value of a property of a JavaScriptObject.
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   * @param value The value of the property to set.
   */
  public static final native void setPropertyAsBoolean(JavaScriptObject object,
                                                           String key, boolean value) /*-{
    object[key] = value;
  }-*/;

  /**
   * Initialize an empty array if the key name does not already exist.
   *
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   */
  public static final void initArray(JavaScriptObject object, String key) {
    if (!hasProperty(object, key)) {
      clearArray(object, key);
    }
  }

  /**
   * Clear the contents of an array.
   *
   * @param object The object containing the property to set.
   * @param key The name of the property to set.
   */
  public static final native void clearArray(JavaScriptObject object, String key) /*-{
    object[key] = [];
  }-*/;
}
