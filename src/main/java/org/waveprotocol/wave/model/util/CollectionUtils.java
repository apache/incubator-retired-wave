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

package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Utilities related to StringMap, StringSet, and CollectionFactory.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class CollectionUtils {

  private CollectionUtils() {
  }

  public static final DataDomain<ReadableStringSet, StringSet> STRING_SET_DOMAIN =
      new DataDomain<ReadableStringSet, StringSet>() {
    @Override
    public void compose(StringSet target, ReadableStringSet changes, ReadableStringSet base) {
      target.clear();
      target.addAll(base);
      target.addAll(changes);
    }

    @Override
    public StringSet empty() {
      return createStringSet();
    }

    @Override
    public ReadableStringSet readOnlyView(StringSet modifiable) {
      return modifiable;
    }
  };

  public static final DataDomain<ReadableStringMap<Object>, StringMap<Object>> STRING_MAP_DOMAIN =
      new DataDomain<ReadableStringMap<Object>, StringMap<Object>>() {
    @Override
    public void compose(StringMap<Object> target, ReadableStringMap<Object>  changes,
        ReadableStringMap<Object> base) {
      target.clear();
      target.putAll(base);
      target.putAll(changes);
    }

    @Override
    public StringMap<Object> empty() {
      return createStringMap();
    }

    @Override
    public ReadableStringMap<Object> readOnlyView(StringMap<Object> modifiable) {
      return modifiable;
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> DataDomain<StringMap<T>, StringMap<T>> stringMapDomain() {
    return (DataDomain) STRING_MAP_DOMAIN;
  }

  @SuppressWarnings("unchecked")
  public static <T> DataDomain<Set<T>, Set<T>> hashSetDomain() {
    return (DataDomain) HASH_SET_DOMAIN;
  }

  public static final DataDomain<Set<Object>, Set<Object>> HASH_SET_DOMAIN =
      new DataDomain<Set<Object>, Set<Object>>() {
    @Override
    public void compose(Set<Object> target, Set<Object> changes, Set<Object> base) {
      target.clear();
      target.addAll(changes);
      target.addAll(base);
    }

    @Override
    public Set<Object> empty() {
      return new HashSet<Object>();
    }

    @Override
    public Set<Object> readOnlyView(Set<Object> modifiable) {
      return Collections.unmodifiableSet(modifiable);
    }
  };

  /**
   * An adapter that turns a java.util.Map<String, V> into a StringMap<V>.
   *
   * @author ohler@google.com (Christian Ohler)
   *
   * @param <V> type of values in the map
   */
  private static final class StringMapAdapter<V> implements StringMap<V> {
    private final Map<String, V> backend;

    private StringMapAdapter(Map<String, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    @Override
    public void putAll(ReadableStringMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((StringMapAdapter<V>) pairsToAdd).backend);
    }

    @Override
    public void putAll(Map<String, V> sourceMap) {
      Preconditions.checkArgument(!sourceMap.containsKey(null),
          "Source map must not contain a null key");
      backend.putAll(sourceMap);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public void put(String key, V value) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      backend.put(key, value);
    }

    @Override
    public void remove(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      backend.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      return backend.containsKey(key);
    }

    @Override
    public V getExisting(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      if (!backend.containsKey(key)) {
        // Not using Preconditions.checkState to avoid unecessary string concatenation
        throw new IllegalStateException("getExisting: Key '" + key + "' is not in map");
      }
      return backend.get(key);
    }

    @Override
    public V get(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      return backend.get(key);
    }

    @Override
    public V get(String key, V defaultValue) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public void each(ProcV<? super V> callback) {
      for (Map.Entry<String, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public void filter(EntryFilter<? super V> filter) {
      for (Iterator<Map.Entry<String, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<String, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    @Override
    public int countEntries() {
      return backend.size();
    }

    @Override
    public String someKey() {
      return isEmpty() ? null : backend.keySet().iterator().next();
    }

    @Override
    public ReadableStringSet keySet() {
      return new StringSetAdapter(backend.keySet());
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An adapter that turns a java.util.Map<Double, V> into a NumberMap<V>.
   *
   * @param <V> type of values in the map
   */
  private static final class NumberMapAdapter<V> implements NumberMap<V> {
    private final Map<Double, V> backend;

    private NumberMapAdapter(Map<Double, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    @Override
    public void putAll(ReadableNumberMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((NumberMapAdapter<V>) pairsToAdd).backend);
    }

    @Override
    public void putAll(Map<Double, V> sourceMap) {
      backend.putAll(sourceMap);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public void put(double key, V value) {
      backend.put(key, value);
    }

    @Override
    public void remove(double key) {
      backend.remove(key);
    }

    @Override
    public boolean containsKey(double key) {
      return backend.containsKey(key);
    }

    @Override
    public V getExisting(double key) {
      assert backend.containsKey(key);
      return backend.get(key);
    }

    @Override
    public V get(double key) {
      return backend.get(key);
    }

    @Override
    public V get(double key, V defaultValue) {
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public void each(ProcV<V> callback) {
      for (Map.Entry<Double, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public void filter(EntryFilter<V> filter) {
      for (Iterator<Map.Entry<Double, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<Double, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    @Override
    public int countEntries() {
      return backend.size();
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }


  /**
   * An adapter that turns a java.util.Map<Integer, V> into an IntMap<V>.
   *
   * @param <V> type of values in the map
   */
  private static final class IntMapAdapter<V> implements IntMap<V> {
    private final Map<Integer, V> backend;

    private IntMapAdapter(Map<Integer, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    @Override
    public void putAll(ReadableIntMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((IntMapAdapter<V>) pairsToAdd).backend);
    }

    @Override
    public void putAll(Map<Integer, V> sourceMap) {
      backend.putAll(sourceMap);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public void put(int key, V value) {
      backend.put(key, value);
    }

    @Override
    public void remove(int key) {
      backend.remove(key);
    }

    @Override
    public boolean containsKey(int key) {
      return backend.containsKey(key);
    }

    @Override
    public V getExisting(int key) {
      assert backend.containsKey(key);
      return backend.get(key);
    }

    @Override
    public V get(int key) {
      return backend.get(key);
    }

    @Override
    public V get(int key, V defaultValue) {
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public void each(ProcV<V> callback) {
      for (Map.Entry<Integer, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public void filter(EntryFilter<V> filter) {
      for (Iterator<Map.Entry<Integer, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<Integer, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    @Override
    public int countEntries() {
      return backend.size();
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An adapter that turns a java.util.Set<String> into a StringSet.
   *
   * @author ohler@google.com (Christian Ohler)
   */
  private static class StringSetAdapter implements StringSet {
    private final Set<String> backend;

    private StringSetAdapter(Set<String> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null set");
      this.backend = backend;
    }

    @Override
    public void add(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      backend.add(s);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public boolean contains(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      return backend.contains(s);
    }

    @Override
    public void remove(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      backend.remove(s);
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public void each(ReadableStringSet.Proc callback) {
      for (String s : backend) {
        callback.apply(s);
      }
    }

    @Override
    public boolean isSubsetOf(Set<String> set) {
      return set.containsAll(backend);
    }

    @Override
    public boolean isSubsetOf(final ReadableStringSet other) {
      for (String s : backend) {
        if (!other.contains(s)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void addAll(ReadableStringSet set) {
      backend.addAll(((StringSetAdapter) set).backend);
    }

    @Override
    public void removeAll(ReadableStringSet set) {
      backend.removeAll(((StringSetAdapter) set).backend);
    }

    @Override
    public void filter(StringPredicate filter) {
      for (Iterator<String> iterator = backend.iterator(); iterator.hasNext();) {
        String x = iterator.next();
        if (filter.apply(x)) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    @Override
    public String someElement() {
      return isEmpty() ? null : backend.iterator().next();
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    @Override
    public int countEntries() {
      return backend.size();
    }
  }

  /**
   * An adapter that wraps a {@link IdentityHashMap}, presenting it as an
   * {@link IdentitySet}.
   */
  private static class IdentitySetAdapter<T> implements IdentitySet<T> {
    private final Map<T, T> backend = new IdentityHashMap<T, T>();

    private IdentitySetAdapter() {
    }

    @Override
    public void add(T x) {
      Preconditions.checkNotNull(x, "IdentitySet cannot contain null values");
      // Note: Boxed primitives, and String, are disallowed. There are special
      // purpose maps for those key types, and the equality semantics between
      // the boxed primitives of Javascript and Java are dubious at best.
      if (x instanceof String || x instanceof Integer || x instanceof Double || x instanceof Long
          || x instanceof Boolean) {
        throw new UnsupportedOperationException(
            "Should NOT use boxed primitives with IdentitySet");
      }

      backend.put(x, x);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public boolean contains(T s) {
      Preconditions.checkNotNull(s, "IdentitySet cannot contain null values");
      return backend.containsKey(s);
    }

    @Override
    public void remove(T s) {
      Preconditions.checkNotNull(s, "IdentitySet cannot contain null values");
      backend.remove(s);
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public T someElement() {
      for (T e : backend.keySet()) {
        return e;
      }
      return null;
    }

    @Override
    public void each(Proc<? super T> procedure) {
      for (T s : backend.keySet()) {
        procedure.apply(s);
      }
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    @Override
    public int countEntries() {
      return backend.size();
    }
  }

  private static class NumberPriorityQueueAdapter implements NumberPriorityQueue {
    private final Queue<Double> queue;

    private NumberPriorityQueueAdapter(Queue<Double> queue) {
      this.queue = queue;
    }

    @Override
    public boolean offer(double e) {
      return queue.offer(e);
    }

    @Override
    public double peek() {
      return queue.peek();
    }

    @Override
    public double poll() {
      return queue.poll();
    }

    @Override
    public int size() {
      return queue.size();
    }
  }

  /**
   * An adapter that wraps a java.util.IdentityHashMap<K, V> into an
   * IdentityMap<K, V>. Note that this is a simple map, so 'identity' is defined
   * by the hashCode/equals of K instances.
   *
   * @param <K> type of keys in the map.
   * @param <V> type of values in the map
   */
  private static class IdentityHashMapAdapter<K, V> implements IdentityMap<K, V> {
    private final Map<K, V> backend = new IdentityHashMap<K, V>();

    private IdentityHashMapAdapter() {
    }

    @Override
    public V get(K key) {
      return backend.get(key);
    }

    @Override
    public boolean has(K key) {
      return backend.containsKey(key);
    }

    @Override
    public void put(K key, V value) {
      // Note: Boxed primitives, and String, are disallowed. See explanation in
      // IdentitySetAdapter.
      if (key instanceof String || key instanceof Integer || key instanceof Double
          || key instanceof Long || key instanceof Boolean) {
        throw new UnsupportedOperationException(
            "Should NOT use boxed primitives as key with identity map");
      }
      backend.put(key, value);
    }

    @Override
    public void remove(K key) {
      removeAndReturn(key);
    }

    @Override
    public V removeAndReturn(K key) {
      return backend.remove(key);
    }

    @Override
    public void clear() {
      backend.clear();
    }

    @Override
    public boolean isEmpty() {
      return backend.isEmpty();
    }

    @Override
    public void each(ProcV<? super K, ? super V> proc) {
      for (Map.Entry<K, V> entry : backend.entrySet()) {
        proc.apply(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public <R> R reduce(R initial, Reduce<? super K, ? super V, R> proc) {
      R reduction = initial;
      for (Map.Entry<K, V> entry : backend.entrySet()) {
        reduction = proc.apply(reduction, entry.getKey(), entry.getValue());
      }
      return reduction;
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    @Override
    public int countEntries() {
      return backend.size();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An implementation of CollectionFactory based on java.util.HashSet and
   * java.util.HashMap.
   *
   * @author ohler@google.com (Christian Ohler)
   */
  private static class HashCollectionFactory implements CollectionFactory {
    @Override
    public <V> StringMap<V> createStringMap() {
      return CollectionUtils.adaptStringMap(new HashMap<String, V>());
    }

    @Override
    public <V> NumberMap<V> createNumberMap() {
      return CollectionUtils.adaptNumberMap(new HashMap<Double, V>());
    }

    @Override
    public <V> IntMap<V> createIntMap() {
      return CollectionUtils.adaptIntMap(new HashMap<Integer, V>());
    }

    @Override
    public StringSet createStringSet() {
      return CollectionUtils.adaptStringSet(new HashSet<String>());
    }

    @Override
    public <T> IdentitySet<T> createIdentitySet() {
      return new IdentitySetAdapter<T>();
    }

    @Override
    public <E> Queue<E> createQueue() {
      return new LinkedList<E>();
    }

    @Override
    public NumberPriorityQueue createPriorityQueue() {
      return CollectionUtils.adaptNumberPriorityQueue(new PriorityQueue<Double>());
    }

    @Override
    public <K, V> IdentityMap<K, V> createIdentityMap() {
      return new IdentityHashMapAdapter<K, V>();
    }
  }

  private static final HashCollectionFactory HASH_COLLECTION_FACTORY =
      new HashCollectionFactory();

  private static CollectionFactory defaultCollectionFactory = HASH_COLLECTION_FACTORY;

  /**
   * Implements a persistently empty string map that throws exceptions on
   * attempt to add keys.
   */
  private static final class EmptyStringMap<V> implements StringMap<V> {
    @Override
    public void clear() {
      // Success as the map is already empty.
    }

    @Override
    public void filter(StringMap.EntryFilter<? super V> filter) {
    }

    @Override
    public void put(String key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(ReadableStringMap<V> pairsToAdd) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<String, V> sourceMap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void remove(String key) {
    }

    @Override
    public boolean containsKey(String key) {
      return false;
    }

    @Override
    public int countEntries() {
      return 0;
    }

    @Override
    public void each(org.waveprotocol.wave.model.util.ReadableStringMap.ProcV<? super V> callback) {
    }

    @Override
    public V get(String key, V defaultValue) {
      return null;
    }

    @Override
    public V get(String key) {
      return null;
    }

    @Override
    public V getExisting(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public String someKey() {
      return null;
    }

    @Override
    public ReadableStringSet keySet() {
      // TODO(danilatos/ohler): Implement an immutable EMPTY_SET
      return CollectionUtils.createStringSet();
    }
  }

  private static final EmptyStringMap<Object> EMPTY_MAP = new EmptyStringMap<Object>();

  private static final IdentityMap<Object, Object> EMPTY = new IdentityMap<Object, Object>() {

    @Override
    public void clear() {
    }

    @Override
    public int countEntries() {
      return 0;
    }

    @Override
    public void each(ProcV<Object, Object> proc) {
    }

    @Override
    public Object get(Object key) {
      return null;
    }

    @Override
    public boolean has(Object key) {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public void put(Object key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <R> R reduce(R initial, Reduce<Object, Object, R> proc) {
      return initial;
    }

    @Override
    public void remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object removeAndReturn(Object key) {
      throw new UnsupportedOperationException();
    }

  };

  //
  // Plain old collections.
  //

  /**
   * Creates an empty {@code HashSet}.
   */
  public static <E> HashSet<E> newHashSet() {
    return new HashSet<E>();
  }

  /**
   * Creates a {@code HashSet} instance containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created {@code HashSet} containing those elements.
   */
  @SuppressWarnings("unchecked")
  public static <E> HashSet<E> newHashSet(E... elements) {
    int capacity = Math.max((int) (elements.length / .75f) + 1, 16);
    HashSet<E> set = new HashSet<E>(capacity);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates a {@code HashSet} instance containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created {@code HashSet} containing those elements.
   */
  public static <E> HashSet<E> newHashSet(Collection<? extends E> elements) {
    return new HashSet<E>(elements);
  }

  /**
   * Creates an empty immutable set.
   *
   * @return a newly created set containing those elements.
   */
  public static <E> Set<E> immutableSet() {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(CollectionUtils.<E>newHashSet());
  }
  /**
   * Creates an immutable set containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created set containing those elements.
   */
  public static <E> Set<E> immutableSet(Collection<? extends E> elements) {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(newHashSet(elements));
  }

  /**
   * Creates an immutable set containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created set containing those elements.
   */
  @SuppressWarnings("unchecked")
  public static <E> Set<E> immutableSet(E... elements) {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(newHashSet(elements));
  }

  /** Creates an empty {@link HashMap}. */
  public static <K, V> HashMap<K, V> newHashMap() {
    return new HashMap<K, V>();
  }

  /**
   * Creates a {@link HashMap} containing the elements in the given map.
   */
  public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }

  /** Creates a new immutable map with one entry. */
  public static <K, V> Map<K, V> immutableMap(K k1, V v1) {
    // TODO(anorth): optimise to a truly immutable map.
    return Collections.singletonMap(k1, v1);
  }

  /** Creates a new immutable map with the given entries. */
  public static <K, V> Map<K, V> immutableMap(K k1, V v1, K k2, V v2) {
    Map<K, V> map = newHashMap();
    map.put(k1, v1);
    map.put(k2, v2);
    return Collections.unmodifiableMap(map);
  }

  /** Creates a new, empty linked list. */
  public static <T> LinkedList<T> newLinkedList() {
    return new LinkedList<T>();
  }

  /** Creates a new linked list containing elements provided by an iterable. */
  public static <T> LinkedList<T> newLinkedList(Iterable<? extends T> elements) {
    LinkedList<T> list = newLinkedList();
    for (T e : elements) {
      list.add(e);
    }
    return list;
  }

  /** Creates a new linked list containing the provided elements. */
  @SuppressWarnings("unchecked")
  public static <T> LinkedList<T> newLinkedList(T... elements) {
    return newLinkedList(Arrays.asList(elements));
  }

  /** Creates a new, empty array list. */
  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  /** Creates a new array list containing elements provided by an iterable. */
  public static <T> ArrayList<T> newArrayList(Iterable<? extends T> elements) {
    ArrayList<T> list = newArrayList();
    for (T e : elements) {
      list.add(e);
    }
    return list;
  }

  /** Creates a new array list containing the provided elements. */
  @SuppressWarnings("unchecked")
  public static <T> ArrayList<T> newArrayList(T... elements) {
    return newArrayList(Arrays.asList(elements));
  }

  //
  // String-based collections.
  //

  /**
   * Sets the default collection factory.
   *
   * This is used in the GWT client initialization code to plug in the JSO-based
   * collection factory. There shouldn't be any need to call this from other
   * places.
   */
  public static void setDefaultCollectionFactory(CollectionFactory f) {
    defaultCollectionFactory = f;
  }

  /**
   * Returns a CollectionFactory based on HashSet and HashMap from java.util.
   *
   * Note: getCollectionFactory() is probably a better choice.
   */
  public static CollectionFactory getHashCollectionFactory() {
    return HASH_COLLECTION_FACTORY;
  }

  /**
   * Returns the default CollectionFactory.
   */
  public static CollectionFactory getCollectionFactory() {
    return defaultCollectionFactory;
  }

  /**
   * Creates a new StringMap using the default collection factory.
   */
  public static <V> StringMap<V> createStringMap() {
    return CollectionUtils.getCollectionFactory().createStringMap();
  }

  /**
   * @returns an immutable empty map object. Always reuses the same object, does
   *          not create new ones.
   */
  @SuppressWarnings("unchecked")
  public static <V> StringMap<V> emptyMap() {
    return (StringMap<V>) EMPTY_MAP;
  }

  /**
   * @returns an immutable empty map object. Always reuses the same object, does
   *          not create new ones.
   */
  @SuppressWarnings("unchecked")
  public static <K, V> IdentityMap<K, V> emptyIdentityMap() {
    return (IdentityMap<K, V>) EMPTY;
  }

  /**
   * Creates a new NumberMap using the default collection factory.
   */
  public static <V> NumberMap<V> createNumberMap() {
    return CollectionUtils.getCollectionFactory().createNumberMap();
  }

  /**
   * Creates a new NumberMap using the default collection factory.
   */
  public static <V> IntMap<V> createIntMap() {
    return CollectionUtils.getCollectionFactory().createIntMap();
  }

  /**
   * Creates a new queue using the default collection factory.
   */
  public static <V> Queue<V> createQueue() {
    return CollectionUtils.getCollectionFactory().createQueue();
  }

  /**
   * Creates a new priority queue using the default collection factory.
   */
  public static NumberPriorityQueue createPriorityQueue() {
    return CollectionUtils.getCollectionFactory().createPriorityQueue();
  }

  /**
   * Creates a new IdentityMap using the default collection factory.
   */
  public static <K, V> IdentityMap<K, V> createIdentityMap() {
    return CollectionUtils.getCollectionFactory().createIdentityMap();
  }

  /**
   * Creates a new IdentitySet using the default collection factory.
   */
  public static <V> IdentitySet<V> createIdentitySet() {
    return CollectionUtils.getCollectionFactory().createIdentitySet();
  }

  /**
   * Creates a new, immutable, singleton IdentitySet.
   */
  public static <V> ReadableIdentitySet<V> singleton(final V value) {
    Preconditions.checkNotNull(value, "Can not create singleton of null");
    return new ReadableIdentitySet<V>() {
      @Override
      public boolean contains(V s) {
        // Note that == is used, not .equals(), because this is an identity set.
        return value == s;
      }

      @Override
      public int countEntries() {
        return 1;
      }

      @Override
      public void each(Proc<? super V> procedure) {
        procedure.apply(value);
      }

      @Override
      public V someElement() {
        return value;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }
    };
  }

  /**
   * Creates a new StringSet using the default collection factory.
   */
  public static StringSet createStringSet() {
    return getCollectionFactory().createStringSet();
  }

  public static <V> StringMap<V> copyStringMap(ReadableStringMap<V> m) {
    StringMap<V> copy = createStringMap();
    copy.putAll(m);
    return copy;
  }

  public static StringSet copyStringSet(ReadableStringSet s) {
    StringSet copy = createStringSet();
    copy.addAll(s);
    return copy;
  }

  /**
   * Adds all entries from the source map to the target map.
   *
   * @return the target map, for convenience
   */
  public static <V, M extends Map<String, V>> M copyToJavaMap(ReadableStringMap<V> source,
      final M target) {
    source.each(new StringMap.ProcV<V>() {
      @Override
      public void apply(String key, V value) {
        target.put(key, value);
      }
    });
    return target;
  }

  /**
   * Adds all entries from the source map to the target map. NOTE(patcoleman):
   * please only call from assertions/testing code. Ideally everything should be
   * ignorant of the java.util.Map implementations as the collection API here
   * becomes more useful.
   *
   * @return java.util.Map version of our IdentityMap
   */
  public static <K, V> Map<K, V> copyToJavaIdentityMapForTesting(IdentityMap<K, V> source) {
    final Map<K, V> result = new IdentityHashMap<K, V>();
    source.each(new IdentityMap.ProcV<K, V>() {
      @Override
      public void apply(K key, V value) {
        result.put(key, value);
      }
    });
    return result;
  }

  /**
   * Creates a new java set with the same contents as the source StringSet.
   */
  public static <V> Map<String, V> newJavaMap(ReadableStringMap<V> source) {
    return copyToJavaMap(source, new HashMap<String, V>());
  }

  /**
   * Adds all elements from the source set to the target collection.
   *
   * @return the target collection, for convenience
   */
  public static <C extends Collection<String>> C copyToJavaCollection(
      ReadableStringSet source, final C target) {
    source.each(new StringSet.Proc() {
      @Override
      public void apply(String element) {
        target.add(element);
      }
    });
    return target;
  }

  /**
   * Adds all values from the source map to the target collection.
   *
   * @return the target collection, for convenience
   */
  public static <T, C extends Collection<T>> C copyValuesToJavaCollection(
      ReadableStringMap<T> source, final C target) {
    source.each(new StringMap.ProcV<T>() {
      @Override
      public void apply(String key, T value) {
        target.add(value);
      }
    });
    return target;
  }

  /**
   * Creates a new java set with the same contents as the source StringSet.
   */
  public static Set<String> newJavaSet(ReadableStringSet source) {
    return copyToJavaCollection(source, new HashSet<String>());
  }

  /**
   * Creates a new java list with the same contents as the source StringSet.
   */
  public static List<String> newJavaList(ReadableStringSet source) {
    return copyToJavaCollection(source, new ArrayList<String>());
  }

  /**
   * Creates a new java list with the same contents as the values of the source
   * StringMap.
   */
  public static <T> List<T> newJavaList(ReadableStringMap<T> source) {
    return copyValuesToJavaCollection(source, new ArrayList<T>());
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> StringMap<V> adaptStringMap(Map<String, V> a) {
    return new StringMapAdapter<V>(a);
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> NumberMap<V> adaptNumberMap(Map<Double, V> a) {
    return new NumberMapAdapter<V>(a);
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> IntMap<V> adaptIntMap(Map<Integer, V> a) {
    return new IntMapAdapter<V>(a);
  }

  /**
   * Returns a StringSet view of the specified set.
   */
  public static StringSet adaptStringSet(Set<String> a) {
    return new StringSetAdapter(a);
  }

  /**
   * Returns a NumberPriorityQueue adaptor of a regular java.util.PriorityQueue
   */
  public static NumberPriorityQueue adaptNumberPriorityQueue(PriorityQueue<Double> priorityQueue) {
    return new NumberPriorityQueueAdapter(priorityQueue);
  }

  /**
   * Returns a StringSet copy of the specified set.
   */
  public static StringSet newStringSet(Set<String> a) {
    StringSet s = createStringSet();
    for (String value : a) {
      s.add(value);
    }
    return s;
  }

  /**
   * Returns a StringSet consisting of the specified values, removing duplicates
   */
  public static StringSet newStringSet(String... values) {
    StringSet s = createStringSet();
    for (String value : values) {
      s.add(value);
    }
    return s;
  }

  /**
   * Returns a StringMap consisting of the specified key-value pairs
   */
  public static StringMap<String> newStringMap(String... pairs) {
    Preconditions.checkArgument(pairs.length % 2 == 0, "newStringMap: pairs must have even length");
    StringMap<String> m = createStringMap();
    for (int i = 0; i < pairs.length; i += 2) {
      m.put(pairs[i], pairs[i + 1]);
    }
    return m;
  }

  /**
   * Returns a list containing all the values of the given string map.  The result
   * will be a copy not backed by the map so it is safe to modify the map while
   * concurrently iterating the list.
   */
  public static <T> List<T> valueList(ReadableStringMap<T> map) {
    final List<T> result = newArrayList();
    map.each(new ProcV<T>() {
      public void apply(String key, T value) {
        result.add(value);
      }
    });
    return result;
  }

  /**
   * Joins an array of strings with the given separator
   */
  public static String join(char separator, String first, String... rest) {
    StringBuilder ret = new StringBuilder(first);
    for (int i = 0; i < rest.length; i++) {
      ret.append(separator);
      ret.append(rest[i]);
    }
    return ret.toString();
  }


  /**
   * Joins an array of strings with the given separator
   */
  public static String join(char separator, String... parts) {
    StringBuilder ret = new StringBuilder();
    if (parts.length > 0) {
      ret.append(parts[0]);
    }
    for (int i = 1; i < parts.length; i++) {
      ret.append(separator);
      ret.append(parts[i]);
    }
    return ret.toString();
  }

  /**
   * Joins an array of strings.
   */
  public static String join(String... parts) {
    StringBuilder ret = new StringBuilder();
    if (parts.length > 0) {
      ret.append(parts[0]);
    }
    for (int i = 1; i < parts.length; i++) {
      ret.append(parts[i]);
    }
    return ret.toString();
  }

  public static String repeat(char component, int repeat) {
    Preconditions.checkArgument(repeat >= 0, "Cannot have negative repeat");
    char[] chars = new char[repeat];
    Arrays.fill(chars, component);
    return String.valueOf(chars);
  }
}
