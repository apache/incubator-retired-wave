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

package org.waveprotocol.wave.model.adt.docbased;

import org.waveprotocol.wave.model.adt.ObservableMonotonicMap;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;

/**
 * Provides a map of keys to monotonically increasing values, as a region of a concurrent document.
 *
 * @param <E> document's element type
 * @param <K> map key type
 * @param <C> map value type (comparable)
 */
public class DocumentBasedMonotonicMap<E, K, C extends Comparable<C>>
    extends AbstractDocumentBasedMap<E, K, C> implements ObservableMonotonicMap<K, C> {

  /**
   * Monotonicity enforcement. The entry parameter is ignored.
   *
   * @return true iff the new value is greater than the current value or null.
   */
  @Override
  protected boolean canReplace(E oldEntry, E newEntry, C currentValue, C value) {
    if (currentValue == null || currentValue.compareTo(value) < 0) {
      return true;
    }
    return false;
  }

  /** Do not alter the map for entries that would be rejected for non-monotonicity. */
  @Override
  protected boolean isRedundantPut(C currentValue, C newValue) {
    if (currentValue == null) {
      return newValue == null;
    } else if (newValue != null) {
      // Reject currentValue >= newValue.
      return currentValue.compareTo(newValue) >= 0;
    } else {
      // This condition (currentValue not null, newValue is null) should only
      // occur when the map is being deleted.
      return false;
    }
  }

  /**
   * Creates a monotonic map.
   *
   * @param router           router for the document holding the map state
   * @param entryContainer   element in which entry elements should be created
   * @param keySerializer    converter between strings and keys
   * @param valueSerializer  converter between strings and values
   * @param entryTagName     name to use for entry elements
   * @param keyAttrName      name to use for key attributes
   * @param valueAttrName    name to use for value attributes
   * @param activeCleanUp    if true, will delete observed redundant entries
   */
  DocumentBasedMonotonicMap(DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<K> keySerializer, Serializer<C> valueSerializer,
      String entryTagName, String keyAttrName, String valueAttrName,
      boolean activeCleanUp) {
    super(router, entryContainer, keySerializer, valueSerializer, entryTagName, keyAttrName,
        valueAttrName, activeCleanUp);
  }

  /**
   * Creates a monotonic map.
   */
  public static <E, K, C extends Comparable<C>> DocumentBasedMonotonicMap<E, K, C> create(
      DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<K> keySerializer, Serializer<C> valueSerializer,
      String entryTagName, String keyAttrName, String valueAttrName) {
    DocumentBasedMonotonicMap<E, K, C> map = new DocumentBasedMonotonicMap<E, K, C>(
        router, entryContainer, keySerializer, valueSerializer,
        entryTagName, keyAttrName, valueAttrName, true);
    map.dispatchAndLoad();
    return map;
  }
}
