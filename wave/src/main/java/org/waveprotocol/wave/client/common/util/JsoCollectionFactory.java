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

import org.waveprotocol.wave.model.util.CollectionFactory;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentitySet;

import java.util.Queue;

/**
 * An implementation of CollectionFactory based on JavaScript objects.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public final class JsoCollectionFactory implements CollectionFactory {

  @Override
  public <V> JsoStringMap<V> createStringMap() {
    return JsoStringMap.create();
  }

  @Override
  public <V> JsoNumberMap<V> createNumberMap() {
    return JsoNumberMap.create();
  }

  @Override
  public <V> JsoIntMap<V> createIntMap() {
    return JsoIntMap.create();
  }

  @Override
  public JsoStringSet createStringSet() {
    return JsoStringSet.create();
  }

  @Override
  public <T> IdentitySet<T> createIdentitySet() {
    return new JsIdentitySet<T>();
  }

  @Override
  public <E> Queue<E> createQueue() {
    return new FastQueue<E>();
  }

  @Override
  public org.waveprotocol.wave.model.util.NumberPriorityQueue createPriorityQueue() {
    return new NumberPriorityQueue();
  }

  @Override
  public <K, V> IdentityMap<K, V> createIdentityMap() {
    return new JsIdentityMap<K, V>();
  }
}
