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

import org.waveprotocol.wave.model.util.IdentityMap;

/**
 * Efficient (for js) implementation that will NOT work in hosted mode for
 * non-JSO key types. (It will work for all key types in web mode).
 *
 * Unit tests for this must be run in web mode, or use JSO keys, because
 * the implementation sets an expando property on each key object. (This
 * also means things like StringMap are not suitable for keys, because of
 * the extra property set). The property is 'x$h'
 *
 * TODO(danilatos): If necessary, make an even more efficient version that
 * supports a different each & reduce interface that does not provide the
 * key values. Currently, in order to support that, two jso maps are used
 * internally, one for keys, one for values. In practice, probably not
 * a big deal...
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class JsIdentityMap<K, V> implements IdentityMap<K, V> {
  private final IntMapJsoView<K> keys = IntMapJsoView.create();
  private final IntMapJsoView<V> values = IntMapJsoView.create();

  /** {@inheritDoc} */
  @Override
  public V get(K key) {
    return values.get(getId(key));
  }

  /** {@inheritDoc} */
  @Override
  public boolean has(K key) {
    return keys.has(getId(key));
  }

  /** {@inheritDoc} */
  @Override
  public void put(K key, V value) {
    int id = getId(key);
    keys.put(id, key);
    values.put(id, value);
  }

  /** {@inheritDoc} */
  @Override
  public void remove(K key) {
    int id = getId(key);
    keys.remove(id);
    values.remove(id);
  }

  /** {@inheritDoc} */
  @Override
  public V removeAndReturn(K key) {
    int id = getId(key);
    keys.remove(id);
    return values.removeAndReturn(id);
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    keys.clear();
    values.clear();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty() {
    return values.isEmpty();
  }

  /**
   * Get the unique id for the object
   *
   * TODO(danilatos): See if just calling .hashCode() will "Just Work (TM)"
   * both for jso and "java' objects at the same time. Then we won't need to
   * do it manually.
   */
  private native int getId(K key) /*-{
    return key.x$h || (key.x$h = @com.google.gwt.core.client.impl.Impl::getNextHashId()());
  }-*/;

  /** {@inheritDoc} */
  @Override
  public void each(ProcV<? super K, ? super V> proc) {
    eachInner(keys, values, proc);
  }

  /** {@inheritDoc} */
  @Override
  public <R> R reduce(R initial, Reduce<? super K, ? super V, R> proc) {
    return reduceInner(keys, values, initial, proc);
  }

  private final native void eachInner(IntMapJsoView<K> keys, IntMapJsoView<V> values,
      ProcV<? super K, ? super V> proc) /*-{
    for (var k in values) {
      proc.
          @org.waveprotocol.wave.model.util.IdentityMap.ProcV::apply(Ljava/lang/Object;Ljava/lang/Object;)
              (keys[k], values[k]);
    }
  }-*/;

  private final native <R> R reduceInner(IntMapJsoView<K> keys, IntMapJsoView<V> values,
      R initial, Reduce<? super K, ? super V, R> proc) /*-{
    var reduction = initial;
    for (var k in values) {
      reduction = proc.
                      @org.waveprotocol.wave.model.util.IdentityMap.Reduce::apply(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)
                          (reduction, keys[k], values[k]);
    }
    return reduction;
  }-*/;

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return values.toSource();
  }

  @Override
  public int countEntries() {
    return values.countEntries();
  }
}
