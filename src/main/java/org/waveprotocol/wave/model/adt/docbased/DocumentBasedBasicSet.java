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

import org.waveprotocol.wave.model.adt.AbstractObservableBasicSet;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of a basic set, using elements in a region of a concurrent
 * document.
 *
 * Does not support null values.
 *
 */
public final class DocumentBasedBasicSet<E, T> extends AbstractObservableBasicSet<T> {

  /** Backing document service. */
  private final DocumentEventRouter<? super E, E, ?> router;

  /** Serializer for converting between attribute values and entry values. */
  private final Serializer<T> serializer;

  /** Element containing set entries. */
  private final E container;

  /** Name to use for entry elements. */
  private final String entryTagName;

  /** Name to use for the value attribute. */
  private final String valueAttrName;

  /** Maps from value held, to the element that holds it. */
  private final Map<T, E> valueElements = CollectionUtils.newHashMap();

  /** Child elements of the container we wish would go away. */
  private final Set<E> obsoleteElements = CollectionUtils.newHashSet();

  /**
   * Creates a basic set. The returned object must be init()ialized before it is
   * valid.
   *
   * @param router router for the document holding the set state
   * @param entryContainer element in which entry elements should be created
   * @param serializer converter between strings and values
   * @param entryTagName name to use for entry elements
   * @param valueAttrName name to use for value attributes
   */
  private DocumentBasedBasicSet(DocumentEventRouter<? super E, E, ?> router,
      E entryContainer, Serializer<T> serializer,
      String entryTagName, String valueAttrName) {
    this.router = router;
    this.container = entryContainer;
    this.serializer = serializer;
    this.entryTagName = entryTagName;
    this.valueAttrName = valueAttrName;
  }

  /**
   * Creates a document-based basic set.
   *
   * @see #DocumentBasedBasicSet(DocumentEventRouter, Object, Serializer,
   *      String, String)
   */
  public static <E, C extends Comparable<C>> DocumentBasedBasicSet<E, C> create(
      DocumentEventRouter<? super E, E, ?> router, E entryContainer,
      Serializer<C> serializer, String entryTagName, String valueAttrName) {
    return new DocumentBasedBasicSet<E, C>(
        router, entryContainer, serializer, entryTagName, valueAttrName).init();
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  /**
   * Initializes a new object. Call after construction is complete.
   */
  private DocumentBasedBasicSet<E, T> init() {
    // Plumb events through
    router.addChildListener(container, new ElementListener<E>() {
      @Override
      public void onElementAdded(E element) {
        handleElementAdded(element);
      }

      @Override
      public void onElementRemoved(E element) {
        handleElementRemoved(element);
      }
    });

    // Call handleElementAdded() to notify this class of existing data
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    E curr = DocHelper.getFirstChildElement(document, container);
    E next;

    while (curr != null) {
      next = DocHelper.getNextSiblingElement(document, curr);
      handleElementAdded(curr);
      curr = next;
    }
    return this;
  }

  //
  // Methods from BasicSet interface. State is read only from the valueElements
  // and obsoleteElements members, and state is changed only by manipulating the
  // underlying document state.
  //

  @Override
  public Iterable<T> getValues() {
    return CollectionUtils.newArrayList(valueElements.keySet());
  }

  @Override
  public boolean contains(T value) {
    return valueElements.containsKey(value);
  }

  @Override
  public void add(T value) {
    Preconditions.checkNotNull(value, "value must not be null");
    // Add an element to represent the value
    if (!valueElements.containsKey(value)) {
      Attributes attrs = new AttributesImpl(valueAttrName, serializer.toString(value));
      getDocument().createChildElement(container, entryTagName, attrs);
      deleteObsoleteElements();
    }
  }

  @Override
  public void remove(T value) {
    if (valueElements.containsKey(value)) {
      deleteObsoleteElements();
      getDocument().deleteNode(valueElements.get(value));
    }
  }

  @Override
  public void clear() {
    if (!valueElements.isEmpty()) {
      deleteObsoleteElements();
      // Remove every container element that corresponds to a value
      ObservableMutableDocument<? super E, E, ?> document = getDocument();
      while (!valueElements.isEmpty()) {
        document.deleteNode(valueElements.values().iterator().next());
      }
    }
  }

  //
  // Methods that respond to changes in the underlying document. These methods
  // update valueElements and obsoleteElements, and trigger any listeners to
  // this BasicSet.
  //

  /**
   * Handles a new element being added to the container. If it represents a new
   * value, that value is recorded. If it represents a value already in this
   * container, either the old element or the new element is marked obsolete.
   * The element marked obsolete is the one that appears later in the document.
   */
  private void handleElementAdded(E newElement) {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    assert container.equals(document.getParentElement(newElement));
    if (!entryTagName.equals(document.getTagName(newElement))) {
      return;
    }

    T value = valueOf(newElement);

    E oldEntry = valueElements.get(value);
    if (oldEntry == null) {
      // Entry is for a new value - add it to the element map and fire an event
      // to collection listeners
      valueElements.put(value, newElement);
      fireOnValueAdded(value);
    } else if (document.getLocation(oldEntry) < document.getLocation(newElement)) {
      // newEntry is not needed, so mark it obsolete
      obsoleteElements.add(newElement);
    } else {
      // oldEntry is no needed, so mark it obsoleted and use the new one instead
      obsoleteElements.add(oldEntry);
      valueElements.put(value, newElement);
    }
  }

  /**
   * Handles an element being removed from the container.
   */
  private void handleElementRemoved(E deletedElement) {
    // To start with though, we do a quick check to see if deletedElement has
    // the same tag name as value elements.
    if (!entryTagName.equals(getDocument().getTagName(deletedElement))) {
      // Exit, because deletedElement definitely isn't part of this container.
      return;
    }

    if (obsoleteElements.remove(deletedElement)) {
      // Element was obsolete, so ignore the removal event
      return;
    }

    T value = valueOf(deletedElement);
    E existingElement = valueElements.get(value);
    if (existingElement != deletedElement) {
      // deleted element was not part of the backing store for this map, so
      // ignore it
    } else {
      valueElements.remove(value);
      fireOnValueRemoved(value);
    }
  }

  //
  // Helper methods.
  //

  /**
   * Deletes those elements that represent doubled-up values. This condition is
   * detected in the handleElementAdded() method, but the actual deletion is
   * only performed after some kind of write was made to the document via a call
   * to {@link #add(Object)}, {@link #remove(Object)} or {@link #clear()}.
   *
   * <p>
   * Delaying document mutation ensures that agents and clients that are only
   * listening to a document do not send mutation operations, preventing issues
   * of writing to read-only wavelets and minimizing the number of concurrent
   * writers sending ops to a wave server.
   *
   * <p>
   * When removing values from the container, obsolete elements ought to be
   * deleted <i>before</i> removing valid elements. This ensures that other
   * agents do not see an intermediate state where an obsolete element is
   * present, but a valid one is not.
   *
   * <p>
   * When adding values to the container, obsolete elements ought to be deleted
   * <i>after</i> adding valid elements. This is to maintain symmetry with
   * removal, thereby pleasing all right-thinking engineers.
   */
  private void deleteObsoleteElements() {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    while (!obsoleteElements.isEmpty()) {
      // Delete the obsolete element from the document. The element removed
      // event callback will remove the item from the obsoleteElements set.
      document.deleteNode(obsoleteElements.iterator().next());
    }
  }

  /**
   * Gets the value of an entry.
   *
   * @param element entry to evaluate
   * @return the value embedded in the element.
   */
  private T valueOf(E element) {
    return serializer.fromString(getDocument().getAttribute(element, valueAttrName));
  }
}
