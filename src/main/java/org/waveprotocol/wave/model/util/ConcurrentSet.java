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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set implementation that permits modification while iterating, if the
 * iteration is scoped by calls to {@link #lock()} and {@link #unlock()}.
 * Any changes made to the set after a {@link #lock} have no effect
 * on the iterated elements; such changes are pushed to the iterated elements
 * on {@link #unlock()}.
 *
 * This implementation has low space and runtime cost, and is simple to
 * understand, at the expense of more boilerplate around iterations.
 *
 * Suggested usage:
 * <pre>
 *   private final ConcurrentSet&lt;Listener&gt; listeners;
 *   ...
 *   void triggerX() {
 *     listeners.lock();
 *     try {
 *       for (Listener l : listeners) {
 *         l.onX();
 *       }
 *     } finally {
 *       listeners.unlock();
 *     }
 *   }
 * </pre>
 *
 */
public final class ConcurrentSet<T> implements Iterable<T> {
  /** The base set. */
  private final Set<T> set = new HashSet<T>();

  /** Collection of elements added while locked. */
  private final Set<T> added = new HashSet<T>();

  /** Collection of elements removed while locked. */
  private final Set<T> removed = new HashSet<T>();

  /** Lock state. */
  private boolean locked = false;

  /**
   * Creates a new ConcurrentSet.
   *
   * @param <T>
   * @return a new ConcurrentSet
   */
  public static <T> ConcurrentSet<T> create() {
    return new ConcurrentSet<T>();
  }

  /**
   * Locks this set.  All changes made through {@link #add(Object)}
   * and {@link #remove(Object)} will not affect the
   * {@link #iterator() iterated} elements until {@link #unlock()} is called.
   * This method is idempotent.
   */
  public void lock() {
    if (!locked) {
      locked = true;
    }
  }

  /**
   * Unlocks this set, pushing cached changes since {@link #lock()} to
   * the iterated set.  This method is idempotent.
   */
  public void unlock() {
    if (locked) {
      locked = false;
      if (!removed.isEmpty()) {
        set.removeAll(removed);
        removed.clear();
      }
      if (!added.isEmpty()) {
        set.addAll(added);
        added.clear();
      }
    }
  }

  /**
   * Adds an item to this set.
   *
   * @param o  object to add
   */
  public void add(T o) {
    if (!locked) {
      set.add(o);
    } else {
      // Maintains invariant:      S intersect A is empty, and R is subset of S
      // Maintains postcondition:  S U {o}  ==  (S - R) U A
      if (!set.contains(o)) {
        added.add(o);
      } else {
        removed.remove(o);
      }
    }
  }

  /**
   * Removes an item from this set.
   *
   * @param o  object to remove
   */
  public void remove(T o) {
    if (!locked) {
      set.remove(o);
    } else {
      // Maintains invariant:      S intersect A is empty, and R is subset of S
      // Maintains postcondition:  S - {o}  ==  (S - R) U A
      if (set.contains(o)) {
        removed.add(o);
      } else {
        added.remove(o);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public Iterator<T> iterator() {
    return set.iterator();
  }
}
