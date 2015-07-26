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

import org.waveprotocol.wave.model.adt.ObservableMonotonicValue;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a monotonically increasing value, implemented using a region of a
 * concurrent document.
 *
 */
public class DocumentBasedMonotonicValue<E, C extends Comparable<C>>
    implements ObservableMonotonicValue<C>, ElementListener<E> {

  /** Backing document service. */
  private final DocumentEventRouter<? super E, E, ?> router;

  /** Serializer for converting between attribute values and entry values. */
  private final Serializer<C> serializer;

  /** Element containing value entries. */
  private final E container;

  /** Name to use for entry elements. */
  private final String entryTagName;

  /** Name to use for the value attribute. */
  private final String valueAttrName;

  /** Element holding the current value. */
  private E value;

  private final Set<E> obsoleteEntries = new HashSet<E>();

  /** Listeners. */
  private final CopyOnWriteSet<Listener<? super C>> listeners = CopyOnWriteSet.create();

  /**
   * Creates a monotonic map.
   *
   * @param router          router for the document holding the map state
   * @param entryContainer  element in which entry elements should be created
   * @param serializer      converter between strings and values
   * @param entryTagName    name to use for entry elements
   * @param valueAttrName   name to use for value attributes
   */
  private DocumentBasedMonotonicValue(DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<C> serializer, String entryTagName, String valueAttrName) {
    this.router = router;
    this.container = entryContainer;
    this.serializer = serializer;
    this.entryTagName = entryTagName;
    this.valueAttrName = valueAttrName;
  }

  /**
   * Creates a monotonic map.
   *
   * @see #DocumentBasedMonotonicValue(DocumentEventRouter, Object, Serializer, String, String)
   */
  public static <E, C extends Comparable<C>> DocumentBasedMonotonicValue<E, C> create(
      DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<C> serializer,
      String entryTagName, String valueAttrName) {
    DocumentBasedMonotonicValue<E, C> value = new DocumentBasedMonotonicValue<E, C>(
        router, entryContainer, serializer, entryTagName, valueAttrName);
    router.addChildListener(entryContainer, value);
    value.load();
    return value;
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  /**
   * Loads from the document state, aggressively removing obsolete values.
   */
  private void load() {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    E value = DocHelper.getFirstChildElement(document, container);
    E nextValue;

    while (value != null) {
      nextValue = DocHelper.getNextSiblingElement(document, value);
      onElementAdded(value);
      value = nextValue;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public C get() {
    return (value != null) ? valueOf(value) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void set(C value) {
    Preconditions.checkNotNull(value, "value must not be null");
    C currentValue = get();
    if (currentValue == null || currentValue.compareTo(value) < 0) {
      createEntry(value);
      cleanup();
    }
  }

  /**
   * Deletes the cached entry element for a particular key, if one exists.
   */
  private void invalidateCacheEntry() {
    invalidateEntry(value);
    value = null;
  }

  /**
   * Deletes an entry from the document.
   *
   * @param entry
   */
  private void invalidateEntry(E entry) {
    if (entry != null) {
      obsoleteEntries.add(entry);
    }
  }

  /**
   * Creates an entry element for the specified map entry.
   *
   * @param value
   */
  private void createEntry(C value) {
    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put(valueAttrName, serializer.toString(value));
    E entry = getDocument().createChildElement(container, entryTagName, attrs);
  }

  private void cleanup() {
    if (!obsoleteEntries.isEmpty()) {
      ObservableMutableDocument<? super E, E, ?> document = getDocument();
      Collection<E> toDelete = new ArrayList<E>(obsoleteEntries);
      for (E e : toDelete) {
        document.deleteNode(e);
      }
      // Deletion callbacks should cleanup obsoleteEntries collection one by one.
      assert obsoleteEntries.isEmpty();
    }
  }

  /**
   * Gets the value of an entry.
   *
   * @param entry
   * @return the value of an entry.
   */
  private C valueOf(E entry) {
    return serializer.fromString(getDocument().getAttribute(entry, valueAttrName));
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

    // It is possible, in transient states, that there are multiple entries in the document for the
    // same key.  Therefore, we can not blindly remove the entry from the cache based on key alone.
    if (value == entry) {
      C oldValue = get();
      value = null;
      triggerOnEntryChanged(oldValue, null);
    } else {
      obsoleteEntries.remove(entry);
    }
  }

  /**
   * Updates the entry cache, if necessary, in response to an entry element
   * being added.
   *
   * This method also aggressively deletes any entries that are not greater
   * than the cached entry.
   */
  @Override
  public void onElementAdded(E entry) {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    assert container.equals(document.getParentElement(entry));
    if (!entryTagName.equals(document.getTagName(entry))) {
      return;
    }

    C newValue = valueOf(entry);
    C oldValue = get();

    // If the new value should end up in the cache, delete the old one (if applicable) and update
    // the entry cache.
    // Otherwise, the new value is aggressively deleted.
    if (oldValue == null || oldValue.compareTo(newValue) < 0) {
      invalidateCacheEntry();  // This should clean up the old entry in onEntryRemoved.
      value = entry;
      triggerOnEntryChanged(oldValue, newValue);
    } else {
      invalidateEntry(entry);
    }
  }

  /**
   * Broadcasts a state-change event to registered listeners.
   *
   * @param oldValue  old value
   * @param newValue  new value
   */
  private void triggerOnEntryChanged(C oldValue, C newValue) {
    for (Listener<? super C> l : listeners) {
      l.onSet(oldValue, newValue);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener<? super C> l) {
    listeners.add(l);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Listener<? super C> l) {
    listeners.remove(l);
  }
}
