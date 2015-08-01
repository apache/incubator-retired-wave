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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.communication.json.RawStringData;

/**
 * Implementation of a JSON message.
 * This is done by extending JavaScriptObject to minimise the size of the resulting code.
 *
 */
public class JsonMessage extends JavaScriptObject {
  private static final JsonSerializer serializer;
  private static boolean registerToString = false;

  static {
    if (GWT.isClient() && BrowserAidedJsonSerailizer.hasBrowserSupport()) {
      serializer = new BrowserAidedJsonSerailizer();
    } else {
      serializer = new JavascriptJsonSerializer();
    }
  }
  /**
   * Protected constructor. Instantiate via static create method.
   */
  protected JsonMessage() {
  }

  /**
   * @return a new empty JsonMessage.
   */
  public static JsonMessage createJsonMessage() {
    JsonMessage instance = JavaScriptObject.createObject().cast();
    registerNativeJsonMessageToString(instance);
    return instance;
  }

  /**
   * Delete all existing properties on this object.
   */
  public final native void clear() /*-{
    for (var p in this) {
      if (this.hasOwnProperty(p) && typeof this[p] != "function") {
        delete p;
      }
    }
  }-*/;

  /**
   * Copy the contents of an existing JsonMessage into this one.
   * @param message
   */
  public final native void copyFrom(JsonMessage message) /*-{
    var clone = function (input) {
      if (input === undefined || input === null) {
        return null;
      }
      if (typeof input == 'object' && input.constructor === Array) {
        var output = [];
        for (var property = 0; property < input.length; ++property) {
          output[property] = clone(input[property]);
        }
        return output;
      }
      if (typeof input == 'object') {
        var output = {};
        for (var property in input) {
          if (input.hasOwnProperty(property)) {
            output[property] = clone(input[property]);
          }
        }
        return output;
      }
      return input;
    };

    // delete any existing properties
    for (var property in this) {
      if (this.hasOwnProperty(property) && typeof this[property] != "function") {
        delete property;
      }
    }

    // copy over new properties
    for (var property in message) {
      if (message.hasOwnProperty(property)) {
        this[property] = clone(message[property]);
      }
    }
  }-*/;

  /**
   * Convert this JsonMessage into a JSON String.
   * @return a JSON string representation.
   */
  public final String toJson() {
    return serializer.serialize(this);
  }


  public static <T extends JsonMessage> T parse(String json) throws JsonException {
    return createJsonMessage(json).<T>cast();
  }

  /**
   * Create a JsonMessage object from the given json
   * @param json The JSON String to load from.
   * @return true if evaluation is successful, false otherwise.
   */
  public static JsonMessage createJsonMessage(String json) throws JsonException {
    try {
      JsonMessage obj = (JsonMessage) serializer.parse(json);
      registerNativeJsonMessageToString(obj);
      return obj;
    } catch (JavaScriptException e) {
      throw new JsonException(e);
    }
  }

  /**
   * Create a JsonMessage object from the given json
   * @param json The JSON String to load from.
   * @return true if evaluation is successful, false otherwise.
   */
  public static JsonMessage createJsonMessage(RawStringData data) throws JsonException {
    try {
      JsonMessage obj = (JsonMessage) serializer.parse(data.getBaseString());
      JsonHelper.registerRawStringData(obj, data);
      registerNativeJsonMessageToString(obj);
      return obj;
    } catch (JavaScriptException e) {
      throw new JsonException(e);
    }
  }

  /**
   * JSON implementation of equals() since we can override it.
   *
   * TODO(user): Rename this to isEqualTo() once GWT compiler is fixed to
   * not crash when another interface defines this method and is implemented by a subclass
   * of JsonMessage.
   *
   * @return true if the other object is the same as this object.
   */
  public final native boolean nativeIsEqualTo(Object toCompare) /*-{
    var same = function (objectA, objectB) {
      if (typeof objectA == "object" && typeof objectB == "object") {
        // Check all A is in B
        for (var property in objectA) {
          if (objectA.hasOwnProperty(property) && typeof objectA[property] != "function") {
            if (!same(objectA[property], objectB[property])) {
              return false;
            }
          }
        }

        // Check all B is in A
        for (var property in objectB) {
          // Something in B we don't know in A.
          if (objectB.hasOwnProperty(property) && typeof objectB[property] != "function" &&
              !objectA.hasOwnProperty(property)) {
            return false;
          }
        }
        return true;
      } else  {
        return objectA == objectB;
      }
    };

    return same(this, toCompare);
  }-*/;

  /** Static version of toJson to allow us to call from JSNI */
  @SuppressWarnings("unused") // Called from JSNI
  private static final String jsonMessageToJson(JsonMessage instance) {
    return instance.toJson();
  }

  /** Enables toString on JSOs */
  public static void enableJsoToString() {
    registerToString = true;
  }

  /** Disables toString on JSOs */
  public static void disableJsoToString() {
    registerToString = false;
  }

  protected static final void registerNativeJsonMessageToString(JsonMessage instance) {
    if (registerToString) {
      nativeRegisterNativeJsonMessageToString(instance);
    }
  }

  /**
   * Recursively registers a native toString function on the object.
   * This toString method will be invoked from JavaScriptObject.toString(),
   * allowing us to print JSON when toString() is called on a JsonMessage.
   */
  private static final native void nativeRegisterNativeJsonMessageToString(JsonMessage instance)
  /*-{
    instance.toString = function () {
      return (
          @org.waveprotocol.wave.communication.gwt.JsonMessage::jsonMessageToJson(Lorg/waveprotocol/wave/communication/gwt/JsonMessage;)
              (this));
    }

    // set toString on all sub objects.
    for (var property in instance) {
      if (instance.hasOwnProperty(property) && typeof instance[property] == "object") {
        @org.waveprotocol.wave.communication.gwt.JsonMessage::registerNativeJsonMessageToString(Lorg/waveprotocol/wave/communication/gwt/JsonMessage;)
            (instance[property]);
      }
    }
  }-*/;
}
