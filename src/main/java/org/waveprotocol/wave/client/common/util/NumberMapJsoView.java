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

import org.waveprotocol.wave.model.util.ReadableNumberMap.ProcV;

import java.util.HashMap;
import java.util.Map;

/**
 * A super fast and memory efficient map (directly uses a js object as the map,
 * and requires only 1 object). Only allows double keys, thus taking advantage
 * of the "perfect hashing" of doubles with respect to javascript.
 *
 * NOTE(danilatos): Does not use hasOwnProperty semantics. So it's possible for
 * spurious entries to appear in the map if we're not careful. Easily fixable,
 * but would incur a slight performance hit (I'm now just handwaving).
 *
 * TODO(dan): Use a different version from the GWT team once it is available
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <T> Type of values in the map. Keys are always doubles.
 *
 * @deprecated use {@link org.waveprotocol.wave.model.util.NumberMap} or
 *             {@link JsoView} instead, depending on use case
 */
@Deprecated
public final class NumberMapJsoView<T> extends JsoMapBase {

  /**
   * A function that accepts an accumulated value, a key and the corresponding
   * item from the map and returns the new accumulated value.
   *
   * @see NumberMapJsoView#reduce(Object, NumberMapJsoView.Reduce)
   * @param <E>
   *            double map's type parameter
   * @param <R>
   *            The type of the value being accumulated
   */
  public interface Reduce<E, R> {
    /** The function */
    public R apply(R soFar, double key, E item);
  }

  /** Construct an empty NumberMap */
  public static native <T> NumberMapJsoView<T> create() /*-{
     return {};
   }-*/;

  /** Construct a NumberMap from a java Map */
  public static <T> NumberMapJsoView<T> fromMap(Map<Double, T> map) {
    NumberMapJsoView<T> doubleMap = create();
    for (double key : map.keySet()) {
      doubleMap.put(key, map.get(key));
    }
    return doubleMap;
  }

  protected NumberMapJsoView() {
  }

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
  public native T get(double key) /*-{
     return this[key];
   }-*/;

  /**
   * Put the value in the map at the given key. Note: Does not return the old
   * value.
   *
   * @param key
   * @param value
   */
  public native void put(double key, T value) /*-{
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

  /**
   * Same as {@link #remove(double)}, but returns what was previously there, if
   * anything.
   *
   * @param key
   * @return what was previously there or null
   */
  public final T removeAndReturn(double key) {
    T val = get(key);
    remove(key);
    return val;
  }

  /**
   * Ruby/prototype.js style iterating idiom, using a callbak. Equivalent to a
   * for-each loop. TODO(danilatos): Implement break and through a la
   * prototype.js if needed.
   *
   * @param proc
   */
  public final native void each(ProcV<? super T> proc) /*-{
     for (var k in this) {
       proc.
           @org.waveprotocol.wave.model.util.ReadableNumberMap.ProcV::apply(DLjava/lang/Object;)
               (parseFloat(k), this[k]);
     }
   }-*/;

  /**
   * Same as ruby/prototype reduce. Same as functional foldl. Apply a function
   * to an accumulator and key/value in the map. The function returns the new
   * accumulated value. TODO(danilatos): Implement break and through a la
   * prototype.js if needed.
   *
   * @param initial
   * @param proc
   * @return The accumulated value
   * @param <R>
   *            The accumulating type
   */
  public final native <R> R reduce(R initial, Reduce<T, R> proc) /*-{
     var reduction = initial;
     for (var k in this) {
       reduction = proc.
                       @org.waveprotocol.wave.client.common.util.NumberMapJsoView.Reduce::apply(Ljava/lang/Object;DLjava/lang/Object;)
                           (reduction, parseFloat(k), this[k]);
     }
     return reduction;
   }-*/;

  /**
   * Convert to a java Map
   */
  public final Map<Double, T> toMap() {
    return addToMap(new HashMap<Double, T>());
  }

  /**
   * Add all values to a java map.
   *
   * @param map
   *            The map to add values to
   * @return The same map, for convenience.
   */
  public final Map<Double, T> addToMap(final Map<Double, T> map) {
    each(new ProcV<T>() {
      public void apply(double key, T item) {
        map.put(key, item);
      }
    });
    return map;
  }

  /**
   * Add all values to a NumberMap.
   *
   * @param map
   *            The map to add values to
   * @return The same map, for convenience.
   */
  public final NumberMapJsoView<T> addToMap(final NumberMapJsoView<T> map) {
    each(new ProcV<T>() {
      public void apply(double key, T item) {
        map.put(key, item);
      }
    });
    return map;
  }
}
