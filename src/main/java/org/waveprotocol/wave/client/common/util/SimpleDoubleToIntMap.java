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


/**
 * Cut down map where the keys are unboxed doubles and the values are unboxed ints
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class SimpleDoubleToIntMap extends JsoMapBase {

  protected SimpleDoubleToIntMap() {}

  /** Construct an empty SimpleDoubleMap */
  public static native SimpleDoubleToIntMap create() /*-{
     return {};
   }-*/;

  /**
   * @param key
   * @return true if a value indexed by the given key is in the map
   */
  public native boolean has(double key) /*-{
     return this[key] !== undefined;
   }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present
   */
  public native int get(double key) /*-{
     return this[key];
   }-*/;

  /**
   * Put the value in the map at the given key. Note: Does not return the old
   * value.
   *
   * @param key
   * @param value
   */
  public native void put(double key, int value) /*-{
     this[key] = value;
   }-*/;

  /**
   * Remove the value with the given key from the map. Note: does not return the
   * old value.
   *
   * @param key
   */
  public native void remove(double key) /*-{
     delete this[key];
   }-*/;
}