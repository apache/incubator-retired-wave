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

import org.waveprotocol.wave.model.util.IdentitySet;

/**
 * Efficient (for js) implementation that will NOT work in hosted mode for
 * non-JSO key types. (It will work for all key types in web mode).
 *
 * Unit tests for this must be run in web mode, or use JSO elements, because
 * the implementation sets an expando property on each key object. (This
 * also means things like StringMap are not suitable for elements, because of
 * the extra property set). The property is 'x$h'.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @see JsIdentityMap
 */
public final class JsIdentitySet<T> implements IdentitySet<T> {
  private final IntMapJsoView<T> elements = IntMapJsoView.create();

  @Override
  public boolean contains(T key) {
    return elements.has(getId(key));
  }

  @Override
  public void add(T key) {
    elements.put(getId(key), key);
  }

  @Override
  public void remove(T key) {
    elements.remove(getId(key));
  }

  @Override
  public void clear() {
    elements.clear();
  }

  @Override
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public void each(Proc<? super T> proc) {
    eachInner(elements, proc);
  }

  @Override
  public int countEntries() {
    return elements.countEntries();
  }

  @Override
  public T someElement() {
    return elements.someValue();
  }

  /**
   * Get the unique id for the object
   *
   * TODO(danilatos): See if just calling .hashCode() will "Just Work (TM)"
   * both for jso and "real java" objects at the same time. Then we won't need
   * to do it manually.
   */
  private native int getId(T key) /*-{
    return key.x$h || (key.x$h = @com.google.gwt.core.client.impl.Impl::getNextHashId()());
  }-*/;

  private final native void eachInner(IntMapJsoView<T> elements, Proc<? super T> proc) /*-{
    for (var k in elements) {
      proc.
          @org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc::apply(Ljava/lang/Object;)
              (elements[k]);
    }
  }-*/;

  @Override
  public String toString() {
    return elements.toSource();
  }
}
