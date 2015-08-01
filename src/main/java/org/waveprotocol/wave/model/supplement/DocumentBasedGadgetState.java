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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement.Listener;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Implements the gadget state, using the {@link DocumentBasedBasicMap}
 * embedding.
 *
 * @param <E> Element type of the document implementation.
 */
public class DocumentBasedGadgetState<E> implements GadgetState {
  private final ObservableBasicMap<String, String> state;
  private final DocumentEventRouter<? super E, E, ?> router;
  private final E container;

  DocumentBasedGadgetState(DocumentEventRouter<? super E, E, ?> router, E container) {
    this.router = router;
    this.container = container;
    state =
        DocumentBasedBasicMap.create(router, container,
            Serializer.STRING, Serializer.STRING, WaveletBasedSupplement.STATE_TAG,
            WaveletBasedSupplement.NAME_ATTR, WaveletBasedSupplement.VALUE_ATTR);
  }

  /**
   * Creates document-based gadget state object.
   *
   * @param router Router for the document that holds the state.
   * @param container Element in which the state is kept.
   * @param id ID of the gadget that uses the state.
   * @param listener Listener for gadget state changes.
   * @return A new gadget state tracker.
   */
  public static <E> DocumentBasedGadgetState<E> create(
      DocumentEventRouter<? super E, E, ?> router, E container, String id,
      Listener listener) {
    DocumentBasedGadgetState<E> state = new DocumentBasedGadgetState<E>(router, container);
    state.installListeners(id, listener);
    return state;
  }

  /**
   * Injects listeners into the underlying ADTs that translate their events
   * into primitive-supplement events.
   *
   * @param gadgetId ID of the gadget.
   * @param listener Listener to notify.
   */
  private void installListeners(final String gadgetId, final Listener listener) {
    state.addListener(new ObservableBasicMap.Listener<String, String>() {
      @Override
      public void onEntrySet(String key, String oldValue, String newValue) {
        listener.onGadgetStateChanged(gadgetId, key, oldValue, newValue);
      }
    });
  }

  @Override
  public ReadableStringMap<String> getStateMap() {
    if (state.keySet().isEmpty()) {
      return CollectionUtils.<String> emptyMap();
    }
    StringMap<String> stateMap = CollectionUtils.createStringMap();
    for (String key : state.keySet()) {
      stateMap.put(key, state.get(key));
    }
    return stateMap;
  }

  @Override
  public void remove() {
    router.getDocument().deleteNode(container);
  }

  @Override
  public void setState(String key, String value) {
    Preconditions.checkNotNull(key, "Private gadget state key is null.");
    if (value != null) {
      state.put(key, value);
    } else {
      state.remove(key);
    }
  }
}
