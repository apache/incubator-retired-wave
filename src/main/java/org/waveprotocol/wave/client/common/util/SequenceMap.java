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

import org.waveprotocol.wave.model.util.SimpleMap;

/**
 * A partial ordered map interface
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 * @param <V>
 */
public interface SequenceMap<K, V> extends SimpleMap<K, V> {

  /**
   * @param key
   * @return a SeqElement for the given key, allowing left/right traversal
   *         thereafter
   */
  SequenceElement<V> getElement(K key);

  /**
   * @return First one, null if empty
   */
  SequenceElement<V> getFirst();

  /**
   * @return Last one, null if empty
   */
  SequenceElement<V> getLast();

  /**
   * SeqElements wrap, so this method is needed to tell if we are at the start
   * of a sequence
   *
   * @param elt
   * @return true if first
   */
  boolean isFirst(SequenceElement<V> elt);

  /**
   * SeqElements wrap, so this method is needed to tell if we are at the end of
   * a sequence
   *
   * @param elt
   * @return true if last
   */
  boolean isLast(SequenceElement<V> elt);

  /**
   * @param key
   * @return node at or just before the key, or null if none before it. Note: if
   *         none before it, the last node would be its predecessor, but we use
   *         null to distinguish "before first" as opposed to "after last"
   */
  SequenceElement<V> findBefore(K key);
}
