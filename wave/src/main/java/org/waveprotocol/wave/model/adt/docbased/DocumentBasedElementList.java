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

import org.waveprotocol.wave.model.adt.ElementList;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An element list backed by elements in a document.
 *
 * Users must specify the type of the
 * abstract list element and the tag name to be used when creating list items.
 * An @link {@link Initializer} interprets abstract initial state into document
 * element attributes. A {@link Factory} adapts document elements to create
 * abstract list elements.
 *
 * <p>
 * Use delegation to implement a document based element
 * list. Create your list that implements the {@link ElementList} interface; at
 * the construction time, create an instance of this class, then use delegation
 * for all methods of the {@link ElementList} interface.
 * </p>
 *
 * <p>
 * If you are using an observable element list, the order of events, when adding
 * a new element, is as follows:
 * </p>
 * <ul>
 *   <li>
 *     A new element is created and inserted into the document (no calls, yet)
 *   </li>
 *   <li>
 *     The factory supplied at the construction time has the
 *     {@link Factory#adapt(DocumentEventRouter, Object)} method called.
 *   </li>
 *   <li>
 *     The element created by the factory is recorded in in-memory data
 *     structure.
 *   </li>
 *   <li>
 *     The {@link ObservableElementList.Listener#onValueAdded(Object)}
 *     method is called
 *   </li>
 * </ul>
 * <p>
 * In particular this means you can safely call {@link #get(int)} method in
 * the observable list listener.
 * </p>
 *
 * <p>
 * A simple example of use:
 * <pre>
 * class MyElement {
 *   class Initialiser {
 *     public final String value;
 *
 *     Initialiser(String value) {
 *       this.value = value;
 *     }
 *   }
 *
 *   private String value;
 *
 *   MyElement(String value) {
 *    this.value = value;
 *   }
 * }
 *
 * DBEL.Factory<E, MyElement> factory = new DBEL.Factory<E, MyElement>() {
 *   MyElement adapt(ObservableMutableDocument<? super E, E, ?> doc, E container) {
 *     return new MyElement(doc.getElementAttribute(container, "myattribute"));
 *   }
 * }
 *
 * DBEL.Initialiser initialiser = new DBEL.initialiser<MyElement.Initialiser>() {
 *   public Map<String, String> makeInitialState(MyElement.Initialiser initialState) {
 *     if (initialState != null) {
 *       return Collections.<String, String> singletonMap("myattribute", initialState.value);
 *     }
 *     return Collections.<String, String> emptyMap();
 *   }
 * }
 *
 * ElementList<MyElement> list = DocumentBasedElementList.create(doc, container, "mytag",
 *     factory, initialiser);
 *
 * MyElement newElement = list.add("newvalue");
 * </pre>
 * </p>
 *
 *
 * @param <E> type of the document element
 * @param <T> type of the abstract element hosted in this list
 * @param <I> type of the initialisation data for new elements
 */
public final class DocumentBasedElementList<E, T, I> implements ObservableElementList<T, I>,
                                                                ElementListener<E> {
  /** Maps list values to document elements that host them. */
  private final Map<T, E> valueToElement;

  /** Maps document elements to list values. */
  private final Map<E, T> elementToValue;

  /** Router for the document supporting this list */
  private final DocumentEventRouter<? super E, E, ?> router;

  /** The parent that holds the children. */
  private final E parent;

  /** In-memory mirror of the document. */
  protected final List<T> orderedValues;

  /** The tag associated with children. */
  private final String childTag;

  /** The factory for abstract children. */
  private final Factory<E, ? extends T, I> factory;

  /** Listeners. */
  private final CopyOnWriteSet<ObservableElementList.Listener<? super T>> listeners;

  /**
   * Creates a new element list supported by the given document and hosted in the given
   * element.
   *
   * @param router router for document supporting this list
   * @param parent the element in which this list is effectively stored
   * @param childTag the tag name for new elements
   * @param factory factory for adapting document elements to abstract elements
   */
  private DocumentBasedElementList(DocumentEventRouter<? super E, E, ?> router,
      E parent, String childTag, Factory<E, ? extends T, I> factory) {
    this.orderedValues = new ArrayList<T>();
    this.valueToElement = new HashMap<T, E>();
    this.elementToValue = new HashMap<E, T>();
    this.listeners = CopyOnWriteSet.create();
    this.parent = parent;
    this.childTag = childTag;
    this.factory = factory;
    this.router = router;
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  /**
   * Creates a new document-based element list. The list may store other
   * elements and even elements which itself are lists.
   *
   * @param <E> The type of the element node
   * @param <T> The type of the child node
   * @param <I> type of the initialisation data for new elements
   * @param router Router for the document supporting this list
   * @param parent The parent element holding list elements
   * @param childTag The tag with which list elements are created
   * @param factory The factory that for each element creates a child object
   * @return A new, document based element list
   */
  public static <E, T, I> DocumentBasedElementList<E, T, I> create(
      DocumentEventRouter<? super E, E, ?> router, E parent, String childTag,
      Factory<E, ? extends T, I> factory) {
    DocumentBasedElementList<E, T, I> list = new DocumentBasedElementList<E, T, I>(
        router, parent, childTag, factory);
    list.dispatchAndLoad();
    return list;
  }

  @Override
  public T add(I initialState) {
    Map<String, String> attributes = Initializer.Helper.buildAttributes(initialState, factory);
    E element = getDocument().createChildElement(parent, childTag, attributes);
    // As we are using an observable document, that is registered to post us events, we expect
    // that onElementAdded is called once the child is created. This method in turn creates a
    // child object for the given element and store it in the elementToValue map. We use this to
    // return the child for the given element.
    return elementToValue.get(element);
  }

  @Override
  public T get(int index) {
    return orderedValues.get(index);
  }

  @Override
  public int indexOf(T child) {
    return orderedValues.indexOf(child);
  }

  @Override
  public T add(int index, I initialState) {
    int last = orderedValues.size();
    Preconditions.checkPositionIndex(index, last);
    if (index == last) {
      return add(initialState);
    }
    T childAfter = orderedValues.get(index);
    E nodeAfter = valueToElement.get(childAfter);
    Map<String, String> attributes = Initializer.Helper.buildAttributes(initialState, factory);
    E fresh = createBefore(getDocument(), nodeAfter, attributes);
    return elementToValue.get(fresh);
  }

  private <N, F extends N> F createBefore(ObservableMutableDocument<N, F, ?> doc, F element,
      Map<String, String> attributes) {
    Point<N> where = Point.before(doc, element);
    return doc.createElement(where, childTag, attributes);
  }

  @Override
  public Iterable<T> getValues() {
    return orderedValues;
  }

  @Override
  public boolean remove(T child) {
    E element = valueToElement.remove(child);
    if (element == null) {
      return false;
    }
    getDocument().deleteNode(element);
    return true;
  }

  @Override
  public void clear() {
    List<T> copy = new ArrayList<T>(orderedValues);
    for (T child : copy) {
      remove(child);
    }
    assert orderedValues.isEmpty();
  }

  @Override
  public int size() {
    return orderedValues.size();
  }

  @Override
  public void onElementAdded(E element) {
    assert getDocument().getParentElement(element).equals(parent) :
        "Received event for unrelated element";
    if (childTag.equals(getDocument().getTagName(element))) {
      T child = factory.adapt(router, element);
      T sibling = getPreviousKnownValue(element);
      orderedValues.add(sibling == null ? 0 : orderedValues.indexOf(sibling) + 1, child);
      elementToValue.put(element, child);
      valueToElement.put(child, element);
      fireElementAdded(child);
    }
  }

  /**
   * Attempts to find the first known sibling value of the given element. This method looks back,
   * in hope that if we get notifications late, we get notifications about elements inserted
   * early first.
   *
   * @param added The freshly inserted element.
   * @return Either an existing list value, or null, if one cannot be found.
   */
  private T getPreviousKnownValue(E added) {
    E prev = DocHelper.getPreviousSiblingElement(getDocument(), added);
    while (prev != null) {
      T value = elementToValue.get(prev);
      if (value != null) {
        return value;
      }
      prev = DocHelper.getPreviousSiblingElement(getDocument(), prev);
    }
    return null;
  }

  @Override
  public void onElementRemoved(E element) {
    T child = elementToValue.remove(element);
    if (child != null) {
      orderedValues.remove(child);
      fireElementRemoved(child);
    }
  }

  /**
   * Hooks up events for the observable document and loads this list from the document.
   */
  private void dispatchAndLoad() {
    router.addChildListener(parent, this);
    load();
  }

  /**
   * Loads this list from the document.
   */
  private void load() {
    E entry = DocHelper.getFirstChildElement(getDocument(), parent);

    while (entry != null) {
      onElementAdded(entry);
      entry = DocHelper.getNextSiblingElement(getDocument(), entry);
    }
  }

  @Override
  public void addListener(ObservableElementList.Listener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ObservableElementList.Listener<T> listener) {
    listeners.remove(listener);
  }

  private void fireElementAdded(T child) {
    for (ObservableElementList.Listener<? super T> l : listeners) {
      l.onValueAdded(child);
    }
  }

  private void fireElementRemoved(T child) {
    for (ObservableElementList.Listener<? super T> l : listeners) {
      l.onValueRemoved(child);
    }
  }
}
