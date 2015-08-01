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

import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * Collection of per-gadget state maps, implemented by embedding them in
 * elements of a document.
 *
 */
class GadgetStateCollection<E> implements ElementListener<E> {
  private final DocumentEventRouter<? super E, E, ?> router;
  private final E container;

  /** Gadget state, expressed as a per-wavelet structure. */
  private final StringMap<GadgetState> gadgetSupplements = CollectionUtils.createStringMap();

  /** Listener to inject into each read-state. */
  private final Listener listener;

  private GadgetStateCollection(DocumentEventRouter<? super E, E, ?> router, E container,
      Listener listener) {
    this.router = router;
    this.container = container;
    this.listener = listener;
  }

  /**
   * Creates a gadget state collection in a given document.
   *
   * @param <E> Element and container type.
   * @param doc Document that will hold the gadget state collection.
   * @param container Collection container.
   * @param listener Event listener for the collection.
   * @return Gadget state collection.
   */
  public static <E> GadgetStateCollection<E> create(
      DocumentEventRouter<? super E, E, ?> router, E container, Listener listener) {
    GadgetStateCollection<E> col = new GadgetStateCollection<E>(router, container, listener);
    router.addChildListener(container, col);
    col.load();
    return col;
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  private void load() {
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    E child = DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
    while (child != null) {
      onElementAdded(child);
      child = DocHelper.getNextSiblingElement(doc, child);
    }
  }

  private String valueOf(E element) {
    return getDocument().getAttribute(element, WaveletBasedSupplement.ID_ATTR);
  }

  @Override
  public void onElementAdded(E element) {
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    assert container.equals(doc.getParentElement(element));
    if (!WaveletBasedSupplement.GADGET_TAG.equals(doc.getTagName(element))) {
      return;
    }

    String gadgetId = valueOf(element);
    if (gadgetId != null) {
      GadgetState existing = gadgetSupplements.get(gadgetId);
      if (existing == null) {
        GadgetState state = DocumentBasedGadgetState.create(router, element, gadgetId, listener);
        gadgetSupplements.put(gadgetId, state);
        // TODO(user): Follow the changes in WaveletReadStateCollection and update this class.
        //
        // NOTE(user): it is important that these events get fired after the new read-state
        //   object is added to the map above, in order that the interface presented by this
        //   collection object is consistent with the events being broadcast to the listener.
        //
        listener.onGadgetStateChanged(gadgetId, null, null, null);
      } else {
        // TODO(user): Follow the changes in WaveletReadStateCollection and update this class.
      }
    } else {
      // XML error: someone added a WAVELET element without an id. Ignore.
      // TODO(user): log this at error level, once loggers are injected into
      // these classes.
      // TODO(user): Follow the changes in WaveletReadStateCollection and update this class.
    }
  }

  @Override
  public void onElementRemoved(E element) {
    if (WaveletBasedSupplement.GADGET_TAG.equals(getDocument().getTagName(element))) {
      String gadgetId = valueOf(element);
      if (gadgetId != null) {
        gadgetSupplements.remove(gadgetId);
      }
    }
  }

  private void createEntry(String gadgetId) {
    getDocument().createChildElement(getDocument().getDocumentElement(),
        WaveletBasedSupplement.GADGET_TAG,
        new AttributesImpl(WaveletBasedSupplement.ID_ATTR, gadgetId));
  }

  GadgetState getSupplement(String gadgetId) {
    Preconditions.checkNotNull(gadgetId, "Gadget ID must not be null");
    GadgetState state = gadgetSupplements.get(gadgetId);
    if (state == null) {
      // Create a new container element for tracking state for the gadget.
      createEntry(gadgetId);
      state = gadgetSupplements.get(gadgetId);
      assert state != null;
    }
    return state;
  }

  /**
   * Saves the gadget state in the underlying implementation.
   *
   * @param gadgetId ID of the gadget that owns the state.
   * @param key The key.
   * @param value The value for the key. If null, the key will be removed.
   */
  void setGadgetState(String gadgetId, String key, String value) {
    getSupplement(gadgetId).setState(key, value);
  }

  /**
   * Removes entire saved object.
   */
  void clear() {
    final StringSet keys = CollectionUtils.createStringSet();
    gadgetSupplements.each(new ProcV<GadgetState>() {
      @Override
      public void apply(String key, GadgetState value) {
        keys.add(key);
      }
    });
    keys.each(new Proc() {
      @Override
      public void apply(String key) {
        gadgetSupplements.get(key).remove();
      }
    });
  }

  ReadableStringMap<String> getGadgetState(String gadgetId) {
    GadgetState state = gadgetSupplements.get(gadgetId);
    return state != null ? state.getStateMap() : CollectionUtils.<String> emptyMap();
  }
}
