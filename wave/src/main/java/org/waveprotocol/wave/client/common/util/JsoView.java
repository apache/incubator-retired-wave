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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

/**
 * Presents a raw, direct view on a javascript object. Useful for exposing
 * properties that are not possible to modify with existing APIs. Also used as a
 * backend for browser-optimised data structures.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class JsoView extends JavaScriptObject {

  // As required by GWT
  protected JsoView() {}

  /** Construct an empty Jso */
  public static native JsoView create() /*-{
    return {};
  }-*/;

  /**
   * Unsafely cast any jso to a JsoView, exposing its internals.
   *
   * @param jso
   * @return a JsoView of the input javascript object
   */
  public static JsoView as(JavaScriptObject jso) {
    return jso.cast();
  }

  /**
   * @param key
   * @return true if a value indexed by the given key is in the map
   */
  public native boolean containsKey(String key) /*-{
    return this[key] !== undefined;
  }-*/;

  /**
   * @param key
   * @return true if the given key exists in the map and is null
   */
  public native boolean isNull(String key) /*-{
    return this[key] === null;
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a boolean. The method may fail at runtime if the type
   *         is not correct.
   */
  public native boolean getBoolean(String key) /*-{
     return this[key];
   }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a string. The method may fail at runtime if the type
   *         is not correct.
   */
  public native String getString(String key) /*-{
    return this[key];
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a string. The method may fail at runtime if the type
   *         is not correct.
   */
  public native String getString(int key) /*-{
    return this[key];
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a primitive number. The method may fail at runtime if
   *         the type is not correct.
   */
  public native double getNumber(String key) /*-{
    return this[key];
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a JSO. The method may fail at runtime if the type
   *         is not correct.
   */
  public native JavaScriptObject getJso(String key) /*-{
    return this[key];
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a JSO. The method may fail at runtime if the type
   *         is not correct.
   */
  public native JavaScriptObject getJso(int key) /*-{
    return this[key];
  }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a JSO. The method may fail at runtime if the type
   *         is not correct.
   */
  public JsoView getJsoView(String key) {
    JavaScriptObject jso = getJso(key);
    return jso == null ? null : JsoView.as(jso);
  }

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a JSO. The method may fail at runtime if the type
   *         is not correct.
   */
  public JsoView getJsoView(int key) {
    JavaScriptObject jso = getJso(key);
    return jso == null ? null : JsoView.as(jso);
  }

  /**
   * @param key
   * @return The value with the given key, or null if not present. The value is
   *         assumed to be a "java" object. The method may fail at runtime if
   *         the type is not correct.
   */
  public native Object getObject(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Same as {@link #getObject(String)} but does not require casting of the
   * return value
   */
  public native <V> V getObjectUnsafe(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Removes the given key from the map
   * @param key
   */
  public native void remove(String key) /*-{
    delete this[key];
  }-*/;

  /**
   * Sets the value for the given key to null (does not remove it from the map)
   * @param key
   */
  public native void setNull(String key) /*-{
    this[key] = null;
  }-*/;

  /**
   * Sets the value for the key
   * @param key
   * @param value
   */
  public native void setBoolean(String key, boolean value) /*-{
    this[key] = value;
  }-*/;

  /**
   * Sets the value for the key
   * @param key
   * @param value
   */
  public native void setString(String key, String value) /*-{
    this[key] = value;
  }-*/;

  /**
   * Sets the value for the key
   * @param key
   * @param value
   */
  public native void setNumber(String key, double value) /*-{
    this[key] = value;
  }-*/;

  /**
   * Sets the value for the key
   * @param key
   * @param value
   */
  public native void setJso(String key, JavaScriptObject value) /*-{
    this[key] = value;
  }-*/;

  /**
   * Sets the value for the key
   * @param key
   * @param value
   */
  public native void setObject(String key, Object value) /*-{
    this[key] = value;
  }-*/;

  /**
   * Removes all entries from this jso.
   */
  public final native void clear() /*-{
    for (var key in this) {
      delete this[key];
    }
  }-*/;

  /**
   * Tests whether this jso is empty.
   *
   * @return true if this map has no entries.
   */
  public final native boolean isEmpty() /*-{
    for (var k in this) {
      return false;
    }
    return true;
  }-*/;

  /**
   * Counts the number of entries in this jso. This is a linear time operation.
   */
  public final native int countEntries() /*-{
    var n = 0;
    for (var k in this) {
      n++;
    }
    return n;
  }-*/;

  /**
   * @return the first key in the iteration order, or null if the object is empty
   */
  public final native String firstKey() /*-{
    for (var k in this) {
      return k;
    }
    return null;
  }-*/;

  /**
   * Ruby/prototype.js style iterating idiom, using a callback. Equivalent to a
   * for-each loop.
   *
   * @param <T> The value type. It is up to the caller to make sure only values
   *        of type T have been put into the Jso in the first place
   * @param proc
   */
  public final native <T> void each(ProcV<T> proc) /*-{
     for (var k in this) {
       proc.
           @org.waveprotocol.wave.model.util.ReadableStringMap.ProcV::apply(Ljava/lang/String;Ljava/lang/Object;)
               (k, this[k]);
     }
   }-*/;

  /**
   * Add all key value pairs from the source to the current map
   *
   * @param source
   */
  public final native void putAll(JsoView source) /*-{
    for (var k in source) {
      this[k] = source[k];
    }
  }-*/;
}
