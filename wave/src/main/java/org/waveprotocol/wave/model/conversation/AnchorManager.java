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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.ObservableMap;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Managers anchoring of transient parasites to transient items via fixed
 * locations.
 *
 * A "location" is understood to mean a reference to an "item", where items are
 * transient: over time, they come into existence and go out of existence. At
 * each location, any number of "parasites" can be attached. Parasites can also
 * be attached to no location. Parasites too are transient: they come and go
 * over time.
 *
 * This class reveals all the parasites, as being either {@link #getAttached()
 * attached} to an item (via a hidden location), or {@link #getUnattached()
 * unattached} to any item (either because the parasites is not bound to a
 * location, or it is bound to a location that has no item). This view is kept
 * live.
 *
 *
 * <ul>
 * <li>the item to location mapping is a surjection (every item has exactly one
 * location, and every location has at moast one item);</li>
 * <li>the parasite to location mapping is a partial function (every parasite
 * maps to at most one location, every location maps to any number of
 * parasites); and</li>
 * <li>the two mappings share the same co-domain (the meaning of 'locations' is
 * the same)</li>
 * </ul>
 *
 * @param <L> location type
 * @param <I> item type
 * @param <P> parasite type
 */
public final class AnchorManager<L, I, P> implements
    SourcesEvents<AnchorManager.Listener<? super I, ? super P>> {

  interface Listener<I, P> {
    /**
     * Notifies this listener that some parasites have been attached.
     *
     * This event occurs:
     * <ul>
     * <li>when new parasites are added to a location that has an item;</li>
     * <li>when new parasites are added to a location that has no item; and</li>
     * <li>when an item is added in a location to which parasites had previously
     * been added.</li>
     * </ul>
     *
     * @param item item to which the parasites are attached, or {@code null} for
     *        unanchored parasites
     * @param parasites newly attached parasites (non-empty)
     */
    void onAttached(I item, Collection<? extends P> parasites);

    /**
     * Notifies this listener that some parasites have been detached.
     *
     * This event occurs:
     * <ul>
     * <li>when new parasites are removed from a location that has an item;</li>
     * <li>when new parasites are fromved from a location that has no item; and</li>
     * <li>when an item is removed from a location to which parasites are
     * attached.</li>
     * </ul>
     *
     * @param item item where the parasites were attached, or {@code null} for
     *        unanchored parasites
     * @param parasites newly detached parasite (non-empty)
     */
    void onDetached(I item, Collection<? extends P> parasites);
  }

  //
  // External helpers.
  //

  /** Maps location references to (transient) items. */
  private final ObservableMap<L, ? extends I> locationResolver;

  //
  // Internal state.
  //

  /** Parasites attached to items (via locations). */
  private final Map<I, Collection<P>> attached = CollectionUtils.newHashMap();

  /** Parasites unattached (non-existent item or location). */
  private final Map<L, Collection<P>> unattached = CollectionUtils.newHashMap();

  /** Internal observer of location map. */
  private final ObservableMap.Listener<L, I> itemObserver = new ObservableMap.Listener<L, I>() {
    @Override
    public void onEntryAdded(L location, I item) {
      AnchorManager.this.onEntryAdded(location, item);
    }

    @Override
    public void onEntryRemoved(L location, I item) {
      AnchorManager.this.onEntryRemoved(location, item);
    }
  };

  /** Listeners. */
  private final CopyOnWriteSet<Listener<? super I, ? super P>> listeners = CopyOnWriteSet.create();

  /**
   * Creates an anchor manager.
   */
  private AnchorManager(ObservableMap<L, ? extends I> locationResolver) {
    this.locationResolver = locationResolver;
  }

  /**
   * Creates an anchor manager.
   *
   * @param items map from location references to items in those locations.
   * @return a new anchor manager.
   */
  public static <L, I, P> AnchorManager<L, I, P> create(ObservableMap<L, ? extends I> items) {
    AnchorManager<L, I, P> m = new AnchorManager<L, I, P>(items);
    m.init();
    return m;
  }

  /**
   * Observes the location map.
   */
  private void init() {
    locationResolver.addListener(itemObserver);
  }

  /**
   * Destroys this manager, releasing all resources it is using.
   */
  public void destroy() {
    locationResolver.removeListener(itemObserver);
  }

  /**
   * Attaches a parasite at a location, notifying listeners of the
   * {@link Listener#onAttached(Object, Collection) attachment} event.
   *
   * @param location
   * @param parasite
   */
  public void attachParasite(L location, P parasite) {
    // Does key point to something that exists yet?
    I item = locationResolver.get(location);

    if (item != null) {
      put(attached, item, parasite);
    } else {
      put(unattached, location, parasite);
    }

    triggerOnAttached(item, parasite);
  }

  /**
   * Detaches a parasite from a location, notifying listeners of the
   * {@link Listener#onDetached(Object, Collection) detachment} event.
   *
   * @param location
   * @param parasite
   */
  public void detachParasite(L location, P parasite) {
    I item = locationResolver.get(location);

    if (item != null) {
      remove(attached, item, parasite);
    } else {
      remove(unattached, location, parasite);
    }

    triggerOnDetached(item, parasite);
  }

  //
  // Helper methods for maps from keys to lazy collections.
  //

  /**
   * Puts a value in a lazy-collection map.
   */
  private static <K, P> void put(Map<K, Collection<P>> map, K key, P value) {
    Collection<P> keyValues = map.get(key);
    if (keyValues == null) {
      keyValues = CollectionUtils.newHashSet();
      map.put(key, keyValues);
    }
    keyValues.add(value);
  }

  /**
   * Removes a value from a lazy-collection map.
   */
  private static <K, P> void remove(Map<K, Collection<P>> map, K key, P value) {
    Collection<P> keyValues = map.get(key);
    keyValues.remove(value);
    if (keyValues.isEmpty()) {
      map.remove(key);
    }
  }

  //
  // Location map events.
  //

  private void onEntryAdded(L location, I item) {
    Collection<P> parasites = unattached.remove(location);
    if (parasites != null) {
      attached.put(item, parasites);

      triggerOnDetached(null, parasites);
      triggerOnAttached(item, parasites);
    }
  }

  private void onEntryRemoved(L location, I item) {
    Collection<P> parasites = attached.remove(item);
    if (parasites != null) {
      unattached.put(location, parasites);

      triggerOnDetached(item, parasites);
      triggerOnAttached(null, parasites);
    }
  }

  //
  // Anchoring state.
  //

  public Map<I, Collection<P>> getAttached() {
    return Collections.unmodifiableMap(attached);
  }

  public Collection<P> getUnattached() {
    Collection<P> allUnattached = CollectionUtils.newArrayList();
    for (Collection<P> unattachedValues : unattached.values()) {
      allUnattached.addAll(unattachedValues);
    }
    return allUnattached;
  }

  //
  // Anchoring events.
  //

  @Override
  public void addListener(Listener<? super I, ? super P> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<? super I, ? super P> listener) {
    listeners.remove(listener);
  }

  private void triggerOnAttached(I item, P parasite) {
    triggerOnAttached(item, Collections.singleton(parasite));
  }

  private void triggerOnDetached(I item, P parasite) {
    triggerOnDetached(item, Collections.singleton(parasite));
  }

  private void triggerOnAttached(I item, Collection<P> parasites) {
    for (Listener<? super I, ? super P> listener : listeners) {
      listener.onAttached(item, parasites);
    }
  }

  private void triggerOnDetached(I item, Collection<P> parasites) {
    for (Listener<? super I, ? super P> listener : listeners) {
      listener.onDetached(item, parasites);
    }
  }
}
