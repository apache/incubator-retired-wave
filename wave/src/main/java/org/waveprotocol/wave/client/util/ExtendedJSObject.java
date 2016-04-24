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


package org.waveprotocol.wave.client.util;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashSet;

/**
 * This class extends a JavaScriptObject and provides unchecked access to the
 * properties as well as hasX methods to test whether the JavaScriptObject
 * contains a property of typeX
 *
 *
 */
public final class ExtendedJSObject extends JavaScriptObject {

  protected ExtendedJSObject() {
    super();
  }

  private final native void addKeys(HashSet<String> s) /*-{
    for (x in this) {
      s.@java.util.HashSet::add(Ljava/lang/Object;)(x);
    }
  }-*/;

  /**
   * Returns a boolean corresponding to the key value. This is an unsafe method
   * which assumes key exists and is of correct type. If not, will throw
   * HostedModeException in hosted mode and cause undefined behaviour in
   * compiled code.
   */
  public final native boolean getBooleanUnchecked(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns a int corresponding to the key value. This is an unsafe method
   * which assumes key exists and is of correct type. If not, will throw
   * HostedModeException in hosted mode and cause undefined behaviour in
   * compiled code.
   */
  public final native int getIntegerUnchecked(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns a double corresponding to the key value. This is an unsafe method
   * which assumes key exists and is of correct type. If not, will throw
   * HostedModeException in hosted mode and cause undefined behaviour in
   * compiled code.
   */
  public final native double getDoubleUnchecked(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns a String corresponding to the key value. This is an unsafe method
   * which assumes key exists and is of correct type. If not, will throw
   * HostedModeException in hosted mode and cause undefined behaviour in
   * compiled code.
   */
  public final native String getStringUnchecked(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns an Object corresponding to the key value. This is an unsafe method
   * which assumes key exists and is of correct type. If not, will throw
   * HostedModeException in hosted mode and cause undefined behaviour in
   * compiled code.
   */
  public final native ExtendedJSObject getObjectUnchecked(String key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns a set of keys in the JavaScriptObject.
   */
  public final HashSet<String> keySet() {
    HashSet<String> s = new HashSet<String>();
    addKeys(s);
    return s;
  }

  /**
   * Returns true if key exists, else returns false.
   */
  public final native boolean hasKey(String key) /*-{
    return this[key] != undefined;
  }-*/;

  /**
   * Returns true if key exists and is a boolean, else returns false.
   */
  public final native boolean hasBoolean(String key) /*-{
    return typeof(this[key]) == 'boolean';
  }-*/;

  /**
   * Returns true if key exists and is a number, else returns false.
   */
  public final native boolean hasNumber(String key) /*-{
    return typeof(this[key]) == 'number';
  }-*/;

  /**
   * Returns true if key exists and is a string, else returns false.
   */
  public final native boolean hasString(String key) /*-{
    return typeof(this[key]) == 'string';
  }-*/;

  /**
   * Returns true if key exists and is an object, else returns false.
   */
  public final native boolean hasObject(String key) /*-{
    return typeof(this[key]) == 'object';
  }-*/;

}
