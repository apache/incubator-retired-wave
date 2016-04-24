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
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Map;

/**
 * Implementation of a basic value, on an attribute of a document element.
 *
 * TODO(user): unit test observable aspect.
 *
 * @param <E> type of a document element
 * @param <T> type of the basic value
 */
public final class DocumentBasedBasicValue<E, T> implements ObservableBasicValue<T>,
    AttributeListener<E> {

  /** Backing document service. */
  private final MutableDocument<? super E, E, ?> document;

  /** Serializer for converting between attribute values and abstract values. */
  private final Serializer<T> serializer;

  /** Name to use for the value attribute. */
  private final String valueAttrName;

  /** Element holding the value. */
  private final E element;

  /** Listeners */
  private final CopyOnWriteSet<Listener<T>> listeners = CopyOnWriteSet.create();

  /**
   * Creates a basic value.
   *
   * @param router router for the document holding the value state
   * @param element element on which the value state is stored
   * @param serializer converter between strings and values
   * @param valueAttrName name to use for value attributes
   */
  public static <E, C extends Comparable<C>> DocumentBasedBasicValue<E, C> create(
      DocumentEventRouter<? super E, E, ?> router, E element, Serializer<C> serializer,
      String valueAttrName) {
    DocumentBasedBasicValue<E, C> value = new DocumentBasedBasicValue<E, C>(
        router.getDocument(), element, serializer, valueAttrName);
    router.addAttributeListener(element, value);
    return value;
  }

  /**
   * Creates an initializer for a basic value.
   *
   * @param serializer serializer for the value type
   * @param valueAttrName attribute name for the value
   * @param value the initial value
   */
  public static <C> Initializer createInitialiser(
      final Serializer<C> serializer, final String valueAttrName, final C value) {
    return new Initializer() {
      @Override
      public void initialize(Map<String, String> target) {
        if (value != null) {
          Initializer.Helper.initialiseAttribute(target, valueAttrName,
              serializer.toString(value));
        }
      }
    };
  }

  /**
   * Creates a basic value.
   *
   * @see #create(DocumentEventRouter, Object, Serializer, String)
   */
  private DocumentBasedBasicValue(MutableDocument<? super E, E, ?> document, E element,
      Serializer<T> serializer, String valueAttrName) {
    this.document = document;
    this.element = element;
    this.serializer = serializer;
    this.valueAttrName = valueAttrName;
  }

  @Override
  public T get() {
    String valueStr = document.getAttribute(element, valueAttrName);
    return serializer.fromString(valueStr);
  }

  @Override
  public void set(T value) {
    String valueStr = serializer.toString(value);
    document.setElementAttribute(element, valueAttrName, valueStr);
  }

  //
  // Listener stuff.
  //

  @Override
  public void onAttributesChanged(
      E element, Map<String, String> oldValues, Map<String, String> newValues) {
    assert element.equals(this.element) : "Received event for unrelated element";

    String oldValueStr = oldValues.get(valueAttrName);
    String newValueStr = newValues.get(valueAttrName);
    T oldValue = serializer.fromString(oldValueStr);
    T newValue = serializer.fromString(newValueStr);

    if (ValueUtils.notEqual(oldValue, newValue)) {
      triggerOnValueChanged(oldValue, newValue);
    }
  }

  private void triggerOnValueChanged(T oldValue, T newValue) {
    for (Listener<T> listener : listeners) {
      listener.onValueChanged(oldValue, newValue);
    }
  }

  @Override
  public void addListener(Listener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<T> listener) {
    listeners.remove(listener);
  }
}
