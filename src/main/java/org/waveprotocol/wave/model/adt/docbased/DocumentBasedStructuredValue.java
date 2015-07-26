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

import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Map;

/**
 * Implementation of a structured value, on a set of attributes of a document
 * element.
 *
 * The key type must override toString() to produce desired document attribute
 * names.
 *
 * @param <E> type of a document element
 * @param <K> enumerated type of the field names
 * @param <V> field value type
 * @author anorth@google.com (Alex North)
 */
public final class DocumentBasedStructuredValue<E, K extends Enum<K>, V> implements
    ObservableStructuredValue<K, V>, AttributeListener<E>, DeletionListener {

  /** Backing document service. */
  private final MutableDocument<? super E, E, ?> document;

  /** Element holding the value. */
  private final E element;

  /** Serializer for converting between attribute values and abstract values. */
  private final Serializer<V> serializer;

  /** Class object for the key enum type. */
  private final Class<K> keyClass;

  /** Listeners */
  private final CopyOnWriteSet<Listener<K, ? super V>> listeners = CopyOnWriteSet.create();

  /**
   * Creates a structured value.
   *
   * @param router router for the document holding the value state
   * @param element element on which the value state is stored
   * @param serializer converter between strings and values
   * @param keyClass class object for the key type
   */
  public static
  <E, K extends Enum<K>, C extends Comparable<C>> DocumentBasedStructuredValue<E, K, C> create(
      DocumentEventRouter<? super E, E, ?> router, E element, Serializer<C> serializer,
      Class<K> keyClass) {
    DocumentBasedStructuredValue<E, K, C> value =
        new DocumentBasedStructuredValue<E, K, C>(router.getDocument(), element,
            serializer, keyClass);
    router.addAttributeListener(element, value);
    router.addDeletionListener(element, value);
    return value;
  }

  /**
   * Creates an initialiser for a structured value.
   *
   * @param serializer serializer for the value type
   * @param initialValues initial values
   */
  public static <K extends Enum<K>, C extends Comparable<C>> Initializer createInitialiser(
      final Serializer<C> serializer, final Map<K, C> initialValues) {
    return new Initializer() {
      @Override
      public void initialize(Map<String, String>  target) {
        for (Map.Entry<K, C> entry : initialValues.entrySet()) {
          if (entry.getValue() != null) {
            Initializer.Helper.initialiseAttribute(target, entry.getKey().toString(),
                serializer.toString(entry.getValue()));
          }
        }
      }
    };
  }

  /**
   * Creates a structured value.
   *
   * @see #create(DocumentEventRouter, Object, Serializer, Class)
   */
  private DocumentBasedStructuredValue(MutableDocument<? super E, E, ?> document, E element,
      Serializer<V> serializer, Class<K> keyClass) {
    this.document = document;
    this.element = element;
    this.serializer = serializer;
    this.keyClass = keyClass;
  }

  @Override
  public V get(K name) {
    String valueStr = document.getAttribute(element, name.toString());
    return serializer.fromString(valueStr);
  }

  @Override
  public void set(K name, V value) {
    String valueStr = serializer.toString(value);
    document.setElementAttribute(element, name.toString(), valueStr);
  }

  @Override
  public void set(Map<K, V> values) {
    Map<String, String> valueStrings = CollectionUtils.newHashMap();
    for (Map.Entry<K, V> entry : values.entrySet()) {
      String valueStr = serializer.toString(entry.getValue());
      valueStrings.put(entry.getKey().toString(), valueStr);
    }
    document.updateElementAttributes(element, valueStrings);
  }

  //
  // Listener stuff.
  //

  @Override
  public void onAttributesChanged(E element, final Map<String, String> oldValues,
      final Map<String, String> newValues) {
    assert element.equals(this.element) : "Received event for unrelated element";

    final Map<K, V> oldFields = CollectionUtils.newHashMap();
    final Map<K, V> newFields = CollectionUtils.newHashMap();

    K[] names = keyClass.getEnumConstants();
    for (K name : names) {
      if (oldValues.containsKey(name.toString()) || newValues.containsKey(name.toString())) {
        String oldValueStr = oldValues.get(name.toString());
        String newValueStr = newValues.get(name.toString());
        V oldValue = serializer.fromString(oldValueStr);
        V newValue = serializer.fromString(newValueStr);
        if (ValueUtils.notEqual(oldValue, newValue)) {
          oldFields.put(name, oldValue);
          newFields.put(name, newValue);
        }
      }
    }

    assert oldFields.keySet().equals(newFields.keySet());
    if (!oldFields.isEmpty()) {
      triggerOnValuesChanged(oldFields, newFields);
    }
  }

  public void onDeleted() {
    // TODO(anorth): detach listeners when EventPlumber makes that possible.
    triggerOnDeleted();
  }

  private void triggerOnValuesChanged(Map<K, V> oldValues, Map<K, V> newValues) {
    for (Listener<K, ? super V> listener : listeners) {
      listener.onValuesChanged(oldValues, newValues);
    }
  }

  private void triggerOnDeleted() {
    for (Listener<K, ? super V> listener : listeners) {
      listener.onDeleted();
    }
  }

  @Override
  public void addListener(Listener<K, ? super V> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<K, ? super V> listener) {
    listeners.remove(listener);
  }
}
