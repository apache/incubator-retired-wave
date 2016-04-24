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

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a map of keys to values, implemented using a region of a concurrent
 * document. Concrete subclasses determine whether a new value can override an
 * existing value for a key by overriding the
 * {@link #canReplace(Object, Object, Object, Object)} method.
 *
 * Does not support null values.
 *
 * @param <E> document's element type
 * @param <K> map key type
 * @param <V> map value type
 */
public abstract class AbstractDocumentBasedMap<E, K, V>
    implements ObservableBasicMap<K, V>, ElementListener<E> {

  /** Serializer for converting between attribute values and key values. */
  private final Serializer<K> keySerializer;

  /** Serializer for converting between attribute values and entry values. */
  private final Serializer<V> valueSerializer;

  /** Element containing map entries. */
  private final E container;

  /** Name to use for entry elements. */
  private final String entryTagName;

  /** Name to use for the key attribute. */
  private final String keyAttrName;

  /** Name to use for the value attribute. */
  private final String valueAttrName;

  /** Map of keys to entry elements holding the mapping for that key. */
  private final Map<K, E> entries = new HashMap<K, E>();

  /** Collection of entries to remove on next write. */
  private final Set<E> obsoleteEntries = new HashSet<E>();

  /** Backing document event router. */
  private final DocumentEventRouter<? super E, E, ?> router;

  /** Listeners. */
  private final CopyOnWriteSet<Listener<? super K, ? super V>> listeners = CopyOnWriteSet.create();

  /** True if this object should clean up any redundant entries it observes. */
  private final boolean activeCleanUp;

  /**
   * True during an update, to ensure only a single change event is sent to listeners.
   * TODO(user) replace this with an atomic entry update, when available.
   */
  private boolean suppressBroadcasts;

  /**
   * Constructor for subclasses to pass through required parameters.
   *
   * @param router           event router for the document holding the map state
   * @param entryContainer   element in which entry elements should be created
   * @param keySerializer    converter between strings and keys
   * @param valueSerializer  converter between strings and values
   * @param entryTagName     name to use for entry elements
   * @param keyAttrName      name to use for key attributes
   * @param valueAttrName    name to use for value attributes
   * @param activeCleanUp    if true, will delete observed redundant entries
   */
  protected AbstractDocumentBasedMap(DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<K> keySerializer, Serializer<V> valueSerializer,
      String entryTagName, String keyAttrName, String valueAttrName,
      boolean activeCleanUp) {
    this.container = entryContainer;
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
    this.entryTagName = entryTagName;
    this.keyAttrName = keyAttrName;
    this.valueAttrName = valueAttrName;
    this.activeCleanUp = activeCleanUp;
    this.router = router;
    suppressBroadcasts = false;
  }

  protected ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  /**
   * Hook for concrete subclasses to perform EventPlumber dispatch and load from the document state.
   */
  protected void dispatchAndLoad() {
    router.addChildListener(container, this);
    this.load();
  }

  /**
   * Determines whether a new value is allowed to override an existing value for a key.
   * Concrete subclasses must implement this method.
   *
   * @param oldEntry a pre-existing entry for the key.
   * @param newEntry the entry for a newly added value; will not be null.
   * @param oldValue the previous value for this key.
   * @param newValue the newly-added value.
   */
  protected abstract boolean canReplace(E oldEntry, E newEntry, V oldValue, V newValue);

  /**
   * Determines whether a put of the new value would be redundant for the
   * semantics of this map, given the old value.
   * Concrete subclasses must implement this method.
   */
  protected abstract boolean isRedundantPut(V oldValue, V newValue);

  /**
   * Loads the cache from the document state, aggressively removing obsolete
   * map entries.
   */
  private void load() {
    E entry = DocHelper.getFirstChildElement(getDocument(), container);
    E nextEntry;

    while (entry != null) {
      nextEntry = DocHelper.getNextSiblingElement(getDocument(), entry);
      onElementAdded(entry);
      entry = nextEntry;
    }
  }

  @Override
  public V get(K key) {
    E entry = entries.get(key);
    return entry != null ? valueOf(entry) : null;
  }

  @Override
  public boolean put(K key, V value) {
    Preconditions.checkNotNull(key, "key must not be null");
    Preconditions.checkNotNull(value, "value must not be null");
    V currentValue = get(key);
    if (isRedundantPut(currentValue, value)) {
      return false;
    }
    // NOTE: this is a workaround for the current inability to perform a single transaction on a
    // MutableDocument that performs a replacement of an existing element.
    suppressBroadcasts = true;

    // Remove any existing values for the key.
    remove(key);

    // Create a new entry (at the start of the container).
    E entry = createEntry(key, value);

    // Notify listeners of the change as a single event.
    suppressBroadcasts = false;
    triggerOnEntryChanged(key, currentValue, value);

    return true;
  }

  private void cleanup() {
    if (!activeCleanUp) {
      assert obsoleteEntries.isEmpty();
      return;
    }

    // Remove obsolete entries.
    // Copy obsoleteEntries by hand -- workaround for b/2087687.
//    Collection<E> toRemove = new ArrayList<E>(obsoleteEntries);
    Collection<E> toRemove = new ArrayList<E>();
    for (E obsoleteEntry : obsoleteEntries) {
      toRemove.add(obsoleteEntry);
    }

    for (E e : toRemove) {
      getDocument().deleteNode(e);
    }

    // The callback firing should have emptied the obsoleteEntries collection.
    assert obsoleteEntries.isEmpty();
  }

  @Override
  public void remove(K key) {
    invalidateCurrentCacheEntry(key);
    cleanup();
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(entries.keySet());
  }

  /**
   * Helper method to satisfy type requirements when adding new elements.
   */
  static <N, E extends N> E prependChild(
      MutableDocument<N, E, ?> doc, E container,
      String tag, Map<String, String> attrs) {
    return doc.createElement(Point.start(doc, container), tag, attrs);
  }

  /**
   * Creates an entry element for the specified map entry.
   *
   * @param key
   * @param value
   */
  private E createEntry(K key, V value) {
    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put(keyAttrName, keySerializer.toString(key));
    attrs.put(valueAttrName, valueSerializer.toString(value));
    // Always insert the new element at the start of the parent container.
    return prependChild(getDocument(), container, entryTagName, new AttributesImpl(attrs));

    // The document should fire an onEntryAdded event, which will cause the value to show up in
    // the entries map.
  }

  /**
   * Deletes the cached entry element for a particular key, if one exists.
   *
   * @param key
   */
  private void invalidateCurrentCacheEntry(K key) {
    invalidateEntry(entries.get(key));
  }

  /**
   * Deletes an entry from the document.
   *
   * @param entry
   */
  private void invalidateEntry(E entry) {
    if (entry != null && activeCleanUp) {
      assert getDocument().getParentElement(entry).equals(container);
      obsoleteEntries.add(entry);
    }
  }

  /**
   * Gets the key of an entry.
   *
   * @param entry
   * @return the key of an entry.
   */
  private K keyOf(E entry) {
    String keyString = getDocument().getAttribute(entry, keyAttrName);
    return keySerializer.fromString(keyString);
  }

  /**
   * Gets the value of an entry.
   *
   * @param entry
   * @return the value of an entry.
   */
  private V valueOf(E entry) {
    String valueString = getDocument().getAttribute(entry, valueAttrName);
    return valueString != null ? valueSerializer.fromString(valueString) : null;
  }

  //
  // Document mutation callbacks.
  //

  /**
   * Clears the cache reference to an entry, if the cache refers to it, in
   * response to an entry element being removed.
   */
  @Override
  public void onElementRemoved(E entry) {
    if (!entryTagName.equals(getDocument().getTagName(entry))) {
      return;
    }

    K key = keyOf(entry);

    // It is possible, in transient states, that there are multiple entries in the document for the
    // same key.  Therefore, we can not blindly remove the entry from the cache based on key alone.
    if (entries.get(key) == entry) {
      entries.remove(key);
      triggerOnEntryChanged(key, valueOf(entry), null);
    }
    if (activeCleanUp) {
      obsoleteEntries.remove(entry);
    }
  }

  /**
   * Updates the entry cache, if necessary, in response to an entry element
   * being added.
   */
  @Override
  public void onElementAdded(E entry) {
    assert getDocument().getParentElement(entry).equals(container) :
        "Received event for unrelated element";
    if (!entryTagName.equals(getDocument().getTagName(entry))) {
      return;
    }

    K key = keyOf(entry);
    V newValue = valueOf(entry);
    E oldEntry = entries.get(key);
    V oldValue = oldEntry != null ? valueOf(oldEntry) : null;

    // If the new value should end up in the cache, delete the old one (if applicable) and update
    // the entry cache.
    // Otherwise, the new value is aggressively deleted.
    if (canReplace(oldEntry, entry, oldValue, newValue)) {
      invalidateCurrentCacheEntry(key);
      entries.put(key, entry);
      triggerOnEntryChanged(key, oldValue, newValue);
    } else {
      invalidateEntry(entry);
    }
  }

  /**
   * Broadcasts a state-change event to registered listeners.
   *
   * @param key key
   * @param oldValue old value
   * @param newValue new value
   */
  private void triggerOnEntryChanged(K key, V oldValue, V newValue) {
    // TODO(user) delay broadcasting events until all document events for an
    // operation have been processed, and broadcast the composed event.
    if (suppressBroadcasts) {
      return;
    }
    for (Listener<? super K, ? super V> l : listeners) {
      l.onEntrySet(key, oldValue, newValue);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener<? super K, ? super V> l) {
    listeners.add(l);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Listener<? super K, ? super V> l) {
    listeners.remove(l);
  }
}
