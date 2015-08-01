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

import com.google.common.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A container implementation that uses copy-on-write semantics to ensure that it is
 * safe to mutate the set while iterating.  The iterator semantics are
 * equivalent to iterating though a snapshot taking at the time of calling
 * {@link #iterator()}; however, a copy-on-write strategy is better suited for
 * infrequently-modified but frequently-iterated container.
 *
 * A minor optimization to copy-on-write is that the underlying container is only
 * copied if an iterator has previously been created for it.  This means that,
 * during a single iteration over a set of size N, a sequence of M operations
 * only costs O(N + M) rather than O(NM) (i.e., the first mutation does the
 * O(N) copy, and subsequent mutations do direct O(1) operations until another
 * iterator is created).
 *
 * NOTE(user): This class is not synchronized.
 *
 */
public final class CopyOnWriteSet<T> implements Iterable<T> {

  /** Factory for blah. */
  interface CollectionFactory {
    /** @return a copy of a collection. */
    <T> Collection<T> copy(Collection<T> source);
  }

  private final static CollectionFactory HASH_SET = new CollectionFactory() {
    @Override
    public <T> Collection<T> copy(Collection<T> source) {
      // HACK(zdwang): We should use a light weight container that does
      // not assume order when we feel confident enough that no one
      // have implicit reliance on order of iteration.
      return new LinkedHashSet<T>(source);
    }
  };

  private final static CollectionFactory LIST_SET = new CollectionFactory() {
    @Override
    public <T> Collection<T> copy(Collection<T> source) {
      return CollectionUtils.newArrayList(source);
    }
  };

  /** Factory for the underlying collection object. */
  private final CollectionFactory factory;

  /** The base collection. Initially refers to a shared empty collection. */
  private Collection<T> contents = Collections.emptySet();

  /** True iff a copy is to be made on the next mutation. */
  private boolean stale = true;

  @VisibleForTesting
  CopyOnWriteSet(CollectionFactory factory) {
    this.factory = factory;
  }

  /** @return a new copy-on-write set, with the default implementation. */
  public static <T> CopyOnWriteSet<T> create() {
    return createHashSet();
  }

  /** @return a new copy-on-write set, backed by a hash set. */
  public static <T> CopyOnWriteSet<T> createHashSet() {
    return new CopyOnWriteSet<T>(HASH_SET);
  }

  /** @return a new copy-on-write set, backed by an array list. */
  public static <T> CopyOnWriteSet<T> createListSet() {
    return new CopyOnWriteSet<T>(LIST_SET);
  }

  /**
   * Replaces the current set with a copy of it.
   */
  private void copy() {
    assert stale;
    contents = factory.copy(contents);
    stale = false;
  }

  /**
   * Adds an item to this set.
   *
   * @param o  object to add
   * @return whether the container changed due to the addition
   */
  public boolean add(T o) {
    if (!stale) {
      return contents.add(o);
    } else {
      if (!contains(o)) {
        copy();
        return contents.add(o);
      } else {
        return false;
      }
    }
  }

  /**
   * Removes an item from this set.
   *
   * @param o  object to remove
   * @return whether the container changed due to the removal
   */
  public boolean remove(T o) {
    if (!stale) {
      return contents.remove(o);
    } else {
      if (contains(o)) {
        copy();
        return contents.remove(o);
      } else {
        return false;
      }
    }
  }

  /**
   * Checks whether an object exists in this collection.
   *
   * @param o  object to check for existence
   */
  public boolean contains(T o) {
    return contents.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    stale = true;
    return contents.iterator();
  }

  /**
   * Clears this collection.
   */
  public void clear() {
    contents = Collections.emptySet();
    stale = true;
  }

  /**
   * @return true if this collection is empty.
   */
  public boolean isEmpty() {
    return contents.isEmpty();
  }

  /**
   * @return the size of this collection.
   */
  public int size() {
    return contents.size();
  }

  @Override
  public String toString() {
    return contents.toString();
  }
}
