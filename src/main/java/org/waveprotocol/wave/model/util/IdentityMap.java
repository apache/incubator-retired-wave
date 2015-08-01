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

package org.waveprotocol.wave.model.util;

/**
 * Fast object identity map, where key set collisions are based on object
 * identity.
 *
 * There are two implementations, one for web mode and one for hosted mode.
 * While not strictly necessary for the web mode implementation, the hosted mode
 * implementation requires that the user does NOT override Object's
 * implementation of hashCode() or equals() for their key type. (It is also good
 * to enforce this, just to avoid confusion). This requirement is not statically
 * enforced, but runtime exceptions will be thrown if a non-identity .equals()
 * collision occurs with keys in the hosted mode implementation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 *          Key type. Restrictions: Must NOT override Object's implementation of
 *          hashCode() or equals()
 * @param <V>
 *          Value type.
 */
public interface IdentityMap<K, V> {
  /**
   * A procedure that accepts a key and the corresponding item from the map and
   * does something with them.
   *
   * @see IdentityMap#each(IdentityMap.ProcV)
   * @param <K> IdentityMap's key type
   * @param <V> IdentityMap's value type
   */
  public interface ProcV<K, V> {
    /** The procedure */
    public void apply(K key, V item);
  }

  /**
   * A function that accepts an accumulated value, a key and the corresponding
   * item from the map and returns the new accumulated value.
   *
   * @see IdentityMap#reduce(Object, IdentityMap.Reduce)
   * @param <K> IdentityMap's key type
   * @param <V> IdentityMap's value type
   * @param <R> The type of the value being accumulated
   */
  public interface Reduce<K, V, R> {
    /** The function */
    public R apply(R soFar, K key, V item);
  }

  /**
   * @param key
   * @return true if a value indexed by the given key is in the map
   */
  boolean has(K key);

  /**
   * @param key
   * @return The value with the given key, or null if not present
   */
  V get(K key);

  /**
   * Put the value in the map at the given key. Note: Does not return the old
   * value.
   *
   * @param key
   * @param value
   */
  void put(K key, V value);

  /**
   * Remove the value with the given key from the map. Note: does not return the
   * old value.
   *
   * @param key
   */
  void remove(K key);

  /**
   * Same as {@link #remove(Object)}, but returns what was previously there, if
   * anything.
   *
   * @param key
   * @return what was previously there or null
   */
  V removeAndReturn(K key);

  /**
   * Removes all entries from this map.
   */
  void clear();

  /**
   * Tests whether this map is empty.
   *
   * @return true if this map has no entries.
   */
  boolean isEmpty();

  /**
   * Ruby/prototype.js style iterating idiom, using a callback. Equivalent to a
   * for-each loop. TODO(danilatos): Implement break and through a la
   * prototype.js if needed.
   *
   * @param proc
   */
  void each(ProcV<? super K, ? super V> proc);

  /**
   * Same as ruby/prototype reduce. Same as functional fold. Apply a function
   * to an accumulator and key/value in the map. The function returns the new
   * accumulated value. TODO(danilatos): Implement break and through a la
   * prototype.js if needed.
   *
   * @param initial
   * @param proc
   * @return The accumulated value
   * @param <R> The accumulating type
   */
  <R> R reduce(R initial, Reduce<? super K, ? super V, R> proc);

  /**
   * Returns the number of entries in the map.
   *
   * Note: This may be a time-consuming operation.
   */
  int countEntries();
}
