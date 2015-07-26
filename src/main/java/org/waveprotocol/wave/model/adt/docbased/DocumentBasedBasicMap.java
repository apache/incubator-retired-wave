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

import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;

/**
 * Provides a map of keys to values, as a region of a concurrent document.
 *
 * The implementation behavior of {@link AbstractDocumentBasedMap}
 * inserts new elements at the start of the containing element. Updates are
 * equivalent to deleting any existing entries followed by an insert.
 * Consistency is achieved in this context by this class by always interpreting
 * the document-last value for a key as the canonical one.
 *
 * @param <E> document's element type
 * @param <K> map key type
 * @param <V> map value type
 */
public final class DocumentBasedBasicMap<E, K, V>
    extends AbstractDocumentBasedMap<E, K, V> {
  /**
   * Creates a Basic Map.
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
  DocumentBasedBasicMap(DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<K> keySerializer, Serializer<V> valueSerializer,
      String entryTagName, String keyAttrName, String valueAttrName,
      boolean activeCleanUp) {
    super(router, entryContainer, keySerializer, valueSerializer,
          entryTagName, keyAttrName, valueAttrName, activeCleanUp);
  }

  /**
   * Creates a Basic Map.
   */
  public static <E, K, V> DocumentBasedBasicMap<E, K, V> create(
      DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<K> keySerializer, Serializer<V> valueSerializer,
      String entryTagName, String keyAttrName, String valueAttrName) {
    DocumentBasedBasicMap<E, K, V> map = new DocumentBasedBasicMap<E, K, V>(
        router, entryContainer, keySerializer, valueSerializer, entryTagName, keyAttrName,
        valueAttrName, true);
    map.dispatchAndLoad();
    return map;
  }

  /**
   * Replacement is permitted only when the newEntry is later in the document.
   */
  @Override
  protected boolean canReplace(E oldEntry, E newEntry, V oldValue, V newValue) {
    if (oldEntry == null) {
      return true;
    }
    if (getDocument().getLocation(newEntry) > getDocument().getLocation(oldEntry)) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean isRedundantPut(V oldValue, V newValue) {
    if (oldValue == null) {
      return newValue == null;
    }
    if (newValue != null) {
      return oldValue.equals(newValue);
    }
    return false;
  }
}
