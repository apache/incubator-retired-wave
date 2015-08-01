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

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of a boolean on a document.
 *
 * The document is interpreted as "true" if and only if there exists a child
 * element (of a particular tagname), where that child does not have a
 * particular attribute set to false. The latter part of that interpretation
 * (that the element does not have a false-valued attribute) is in order to work
 * with legacy state.
 *
 * Changing the document state to true involves inserting an attribute-less
 * element. Changing it to false involves deleting all matching child elements.
 *
 * @param <E> type of a document element
 */
public final class DocumentBasedBoolean<E> implements ObservableBasicValue<Boolean>,
    ElementListener<E> {

  private final static String FALSE = Serializer.BOOLEAN.toString(false);

  /** Backing document service. */
  private final DocumentEventRouter<? super E, E, ?> router;

  /** Tagname to use for matching elements. */
  private final String tag;

  /** Name to use for the value attribute. */
  private final String valueAttr;

  /** Element holding the value. */
  private final E container;

  /** Element chosen as the source of truth.  May be null.. */
  private E valueElement;

  /** Cached value. */
  private Boolean value;

  /** Redundant elements seen in the document. */
  private final Collection<E> redundantElements = new ArrayList<E>();

  /** Listeners */
  private final CopyOnWriteSet<Listener<Boolean>> listeners = CopyOnWriteSet.create();

  /**
   * Creates a basic value.
   *
   * @param router router for the document holding the value state
   * @param container container on which the value state is stored
   * @param tag tag name to use for child elements of the container
   * @param valueAttrName name to use for value attributes
   */
  public static <E> DocumentBasedBoolean<E> create(
      DocumentEventRouter<? super E, E, ?> router, E container, String tag,
      String valueAttrName) {
    DocumentBasedBoolean<E> value =
        new DocumentBasedBoolean<E>(router, container, tag, valueAttrName);
    router.addChildListener(container, value);
    value.load();
    return value;
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  /**
   * Creates a basic value.
   *
   * @see #create(DocumentEventRouter, Object, String, String)
   */
  private DocumentBasedBoolean(DocumentEventRouter<? super E, E, ?> router, E container,
      String tag, String valueAttr) {
    this.router = router;
    this.container = container;
    this.tag = tag;
    this.valueAttr = valueAttr;
  }

  /**
   * Loads state from the substrate document.
   */
  private void load() {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    E child = DocHelper.getFirstChildElement(document, container);
    while (child != null) {
      onElementAdded(child);
      child = DocHelper.getNextSiblingElement(document, child);
    }
  }

  private void changeValue(E newElement) {
    Boolean oldValue = get();

    if (newElement != null) {
      valueElement = newElement;
      // It's a positive element if it has the right tagname and does not have
      // value = false. Note, absence of attributes is interpreted as a positive
      // element.
      value = !FALSE.equals(getDocument().getAttribute(valueElement, valueAttr));
    } else {
      valueElement = null;
      value = null;
    }

    Boolean newValue = get();
    maybeTriggerOnValueChanged(oldValue, newValue);
  }

  /**
   * Deletes redundant elements.
   */
  private void cleanup() {
    Boolean beforeValue = get();
    // Delete all elements identified as redundant. We don't delete _every_
    // element in the document in order that this type can be embedded
    // collaboratively in the same document as other types.
    Collection<E> toDelete = new ArrayList<E>();
    toDelete.addAll(redundantElements);
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    for (E e : toDelete) {
      doc.deleteNode(e);
    }
    // Callbacks should have emptied the redundant collection.
    assert redundantElements.isEmpty();
    // Check that cleanup did not change the interpretation.
    assert equals(beforeValue, get());
  }

  @Override
  public Boolean get() {
    return value;
  }

  private static boolean equals(Boolean a, Boolean b) {
    return a == null ? b == null : a.equals(b);
  }

  @Override
  public void set(Boolean newValue) {
    Boolean oldValue = get();
    if (equals(oldValue, newValue)) {
      return;
    }

    if (newValue != null) {
      // Add an element to reflect new value.
      getDocument().createChildElement(container, tag,
          new AttributesImpl(valueAttr, Serializer.BOOLEAN.toString(newValue)));
      cleanup();
    } else {
      // Erase all elements. Cleanup first, so that removal of real element
      // does not promote a redundant element temporarily.
      cleanup();
      getDocument().deleteNode(valueElement);
    }

    assert equals(newValue, get());
  }

  //
  // Listener stuff.
  //

  @Override
  public void onElementAdded(E newElement) {
    ObservableMutableDocument<? super E, E, ?> document = getDocument();
    assert container.equals(document.getParentElement(newElement));
    if (!tag.equals(document.getTagName(newElement))) {
      return;
    }

    // Possibly changing an existing value?
    if (valueElement != null) {
      if (document.getLocation(newElement) < document.getLocation(valueElement)) {
        // New element loses.
        redundantElements.add(newElement);
      } else {
        // New element wins.
        redundantElements.add(valueElement);
        changeValue(newElement);
      }
    } else {
      // New element is the new value.
      changeValue(newElement);
    }
  }

  @Override
  public void onElementRemoved(E oldElement) {
    if (!tag.equals(getDocument().getTagName(oldElement))) {
      return;
    }

    redundantElements.remove(oldElement);
    if (oldElement == valueElement) {
      // Reference value is removed.  Find new best value from redundant collection.
      changeValue(extractLastRedundant());
    }
  }

  private E extractLastRedundant() {
    E maxElement = null;
    int maxLocation = -1;
    for (E element : redundantElements) {
      // NOTE(user): there is a possible bug here, which will be fixed by
      // not individualising the element events in the ElementListener
      // interface.
      int location = getDocument().getLocation(element);
      if (location > maxLocation) {
        maxLocation = location;
        maxElement = element;
      }
    }
    redundantElements.remove(maxElement);
    return maxElement;
  }

  /**
   * Notifies listeners if oldValue and newValue are different.
   */
  private void maybeTriggerOnValueChanged(Boolean oldValue, Boolean newValue) {
    if (!equals(oldValue, newValue)) {
      for (Listener<Boolean> listener : listeners) {
        listener.onValueChanged(oldValue, newValue);
      }
    }
  }

  @Override
  public void addListener(Listener<Boolean> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<Boolean> listener) {
    listeners.remove(listener);
  }
}
