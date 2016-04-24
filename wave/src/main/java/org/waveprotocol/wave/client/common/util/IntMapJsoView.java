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

import org.waveprotocol.wave.model.util.ReadableIntMap.ProcV;

import java.util.HashMap;
import java.util.Map;

/**
 * A super fast and memory efficient map (directly uses a js object as the map,
 * and requires only 1 object). Only allows int keys, thus taking advantage of
 * the "perfect hashing" of ints with respect to javascript.
 *
 * No one other than JsoIntMap should use this class.
 * TODO(danilatos): Move the clever logic into JsoIntMap and delete this file.
 *
 * NOTE(danilatos): Does not use hasOwnProperty semantics. So it's possible for
 * spurious entries to appear in the map if we're not careful. Easily fixable,
 * but would incur a slight performance hit (I'm now just handwaving).
 *
 * TODO(dan): Use a different version from the GWT team once it is available
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <T> Type of values in the map. Keys are always ints.
 */
final class IntMapJsoView<T> extends JsoMapBase {
  /**
   * A function that accepts an accumulated value, a key and the corresponding
   * item from the map and returns the new accumulated value.
   *
   * @see IntMapJsoView#reduce(Object, IntMapJsoView.Reduce)
   * @param <E>
   *            int map's type parameter
   * @param <R>
   *            The type of the value being accumulated
   */
  public interface Reduce<E, R> {
    /** The function */
    public R apply(R soFar, int key, E item);
  }

  /** Construct an empty IntMap */
  public static native <T> IntMapJsoView<T> create() /*-{
     return {};
   }-*/;


  /**
   * Represent a java script native function that change the integer key into index key.
   * This is done because in chrome, it is 20 times faster to store string index than to store
   * numeric index if the number is > 2^15.
   *
   * The following time are recorded under chrome linux  4.0.266.0
   * The recorderd time are in ms.
   *
   * iterations  Numeric Index  String index Hybrid time
   * 1024        1               2           0
   * 2048        0               5           1
   * 4096        1               14          1
   * 8192        4               30          4
   * 16384       8               63          9
   * 32768       20              132         24
   * 65536       266             264         148
   * 131072      5551            513         404
   * 262144      16719           1036        976
   *
   * for numeric index < 2^15, it is 6 times faster to use numeric index
   * for string index > 2^15, it is 20 times faster to use string index
   * hybrid approach represents what we are doing here, i.e. use numeric index for value < 2^15 and
   *   string index for value > 2^15
   *
   * DESPITE THIS RESULT, it is very bad to use numeric index at all.  Using the numeric index
   * slows down the javascript engine.  We always uses String index in chrome.  So DO NOT change
   * the code to do the hybrid approach.
   *
   * This function must be represented by JavaScriptObject and not normal java interfaces because
   * it's return data of different type depends on the input value.
   */
  @SuppressWarnings("unused") // used in native method
  private static JavaScriptObject prefixer;

  @SuppressWarnings("unused") // used in native method
  private static JavaScriptObject evaler;

  static {
    setupPrefix(UserAgent.isChrome());
  }

  private static native void setupPrefix(boolean usePrefix) /*-{
    if (usePrefix) {
      @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer = function(a) {
        return "a" + a;
      };
      @org.waveprotocol.wave.client.common.util.IntMapJsoView::evaler = function(a) {
        return a[0] == "a" ? parseInt(a.substr(1, a.length)) : parseInt(a);
      }
    } else {
      @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer = function(a) {
        return a;
      };
      @org.waveprotocol.wave.client.common.util.IntMapJsoView::evaler = function(a) {
        return parseInt(a);
      }
    }
  }-*/;

  /** Construct a IntMap from a java Map */
  public static <T> IntMapJsoView<T> fromMap(Map<Integer, T> map) {
    IntMapJsoView<T> intMap = create();
    for (int key : map.keySet()) {
      intMap.put(key, map.get(key));
    }
    return intMap;
  }

  protected IntMapJsoView() {
  }

  /**
   * @param key
   * @return true if a value indexed by the given key is in the map
   */
  public native boolean has(int key) /*-{
     var prefixer = @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer;
     return this[prefixer(key)] !== undefined;
   }-*/;

  /**
   * @param key
   * @return The value with the given key, or null if not present
   */
  public native T get(int key) /*-{
     var prefixer = @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer;
     return this[prefixer(key)];
   }-*/;

  /**
   * Put the value in the map at the given key. Note: Does not return the old
   * value.
   *
   * @param key
   * @param value
   */
  public native void put(int key, T value) /*-{
     var prefixer = @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer;
     this[prefixer(key)] = value;
   }-*/;

  /**
   * Remove the value with the given key from the map. Note: does not return the
   * old value.
   *
   * @param key
   */
  public native void remove(int key) /*-{
     var prefixer = @org.waveprotocol.wave.client.common.util.IntMapJsoView::prefixer;
     delete this[prefixer(key)];
   }-*/;

  /**
   * Same as {@link #remove(int)}, but returns what was previously there, if
   * anything.
   *
   * @param key
   * @return what was previously there or null
   */
  public final T removeAndReturn(int key) {
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
     var evaler = @org.waveprotocol.wave.client.common.util.IntMapJsoView::evaler;
     for (var k in this) {
       proc.
           @org.waveprotocol.wave.model.util.ReadableIntMap.ProcV::apply(ILjava/lang/Object;)
               (evaler(k), this[k]);
     }
   }-*/;

  public final native T someValue() /*-{
    for (var k in this) {
      return this[k]
    }
    return null;
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
     var evaler = @org.waveprotocol.wave.client.common.util.IntMapJsoView::evaler;
     for (var k in this) {
       reduction = proc.
           @org.waveprotocol.wave.client.common.util.IntMapJsoView.Reduce::apply(Ljava/lang/Object;ILjava/lang/Object;)
               (reduction, evaler(k), this[k]);
     }
     return reduction;
   }-*/;

  /**
   * Convert to a java Map
   */
  public final Map<Integer, T> toMap() {
    return addToMap(new HashMap<Integer, T>());
  }

  /**
   * Add all values to a java map.
   *
   * @param map
   *            The map to add values to
   * @return The same map, for convenience.
   */
  public final Map<Integer, T> addToMap(final Map<Integer, T> map) {
    each(new ProcV<T>() {
      public void apply(int key, T item) {
        map.put(key, item);
      }
    });
    return map;
  }

  /**
   * Add all values to a IntMap.
   *
   * @param map
   *            The map to add values to
   * @return The same map, for convenience.
   */
  public final IntMapJsoView<T> addToMap(final IntMapJsoView<T> map) {
    each(new ProcV<T>() {
      public void apply(int key, T item) {
        map.put(key, item);
      }
    });
    return map;
  }
}
