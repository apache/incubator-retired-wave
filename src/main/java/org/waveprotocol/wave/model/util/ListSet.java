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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A set backed by an array list.  Modifying this set has linear complexity since
 * looking elements up in the backing list is linear, so this works best for small
 * sets.  On the other hand this type of set is compact and has efficient iteration.
 *
 *
 * @param <S> type of list elements
 */
public final class ListSet<S> extends AbstractSet<S> {

  private final ArrayList<S> store;

  public ListSet() {
    store = new ArrayList<S>();
  }

  public ListSet(ListSet<S> values) {
    store = new ArrayList<S>(values.store);
  }

  @Override
  public boolean remove(Object value) {
    return store.remove(value);
  }

  @Override
  public Iterator<S> iterator() {
    return store.iterator();
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public boolean contains(Object o) {
    return store.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return store.containsAll(c);
  }

  @Override
  public boolean add(S value) {
    if (store.contains(value)) {
      return false;
    } else {
      store.add(value);
      return true;
    }
  }

  @Override
  public boolean addAll(Collection<? extends S> c) {
    boolean addedElement = false;
    for (S elm : c) {
      if (add(elm)) {
        addedElement = true;
      }
    }
    return addedElement;
  }

  @Override
  public void clear() {
    store.clear();
  }

}
