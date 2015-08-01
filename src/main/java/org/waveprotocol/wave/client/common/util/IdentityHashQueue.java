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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;

/**
 * A fast queue implementation for GWT that also supports O(1) remove.
 *
 * @param <T>
 *          Key type. Restrictions: It most satisfy the construct that's needed
 *          for use with IdentityMap.  The current restriction is it must NOT
 *          override Object's implementation of hashCode() or equals()
 *
 */
public class IdentityHashQueue<T> extends FastQueue<T> {
  /**
   * A map between the inserted element and their position in the queue.
   * Using identity map now, may want to investigate to see if we want to change it to a
   * IdentityToIntMap<T> which maps a object into integer to avoid box/unbox.
   */
  // TODO(user): Check to see if using Integer causes any slow down.
  private final IdentityMap<T, Integer> indexMap = CollectionUtils.createIdentityMap();

  @Override
  protected int addEntry(T e) {
    // should only add an item in the queue at most once
    assert !indexMap.has(e);
    if (indexMap.has(e)) {
      throw new IllegalStateException("Add Entry " + e + " map " + indexMap);
    }
    int index = super.addEntry(e);
    indexMap.put(e, index);
    assert indexMap.has(e);
    return index;
  }

  @Override
  protected T removeEntry(int index) {
    T ret = super.removeEntry(index);
    if (!indexMap.has(ret)) {
      throw new IllegalStateException("Remove Entry " + ret + " map " + indexMap);
    }
    assert indexMap.has(ret);
    indexMap.remove(ret);
    return ret;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean remove(Object e) {
    if (e != null && indexMap.has((T) e)) {
      removeEntry(indexMap.get((T) e));
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    super.clear();
    indexMap.clear();
  }
}
