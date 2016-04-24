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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Generic {@link AnnotationCursor}.
 *
 * Implemented in terms of an annotation set.
 *
 * @param <V> Value parameter of the annotation set
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// NOTE(danilatos/ohler): It may at some distant point in the future be worth
// having an implementation inside the annotation set impl, for efficiency.
public final class GenericAnnotationCursor<V> implements AnnotationCursor {

  /**
   * Represents the current location for a key. This class is mutable in its
   * location.
   */
  private static final class KeyLocation implements Comparable<KeyLocation> {
    private final String key;
    private int location;

    private KeyLocation(String key, int initialLocation) {
      this.key = key;
      this.location = initialLocation;
    }

    @Override
    // The PriorityQueue documentation states that the head will have the least
    // value, so we want a natural ordering on location.
    public int compareTo(KeyLocation other) {
      return location - other.location;
    }
  }

  // Invariants.

  private final ReadableAnnotationSet<V> annotations;
  private final int end;

  // State.

  private final Queue<KeyLocation> locations = new PriorityQueue<KeyLocation>();
  int currentLocation;

  /**
   * The range values have the same meaning as the annotation set interfaces.
   *
   * @param annotations annotation set
   * @param start start of the range
   * @param end end of the range
   * @param keys key set for which to search for changes
   */
  public GenericAnnotationCursor(ReadableAnnotationSet<V> annotations, final int start, int end,
      ReadableStringSet keys) {
    Preconditions.checkPositionIndexes(start, end, annotations.size());
    Preconditions.checkNotNull(keys, "GenericAnnotationCursor: Key set must not be null");

    this.annotations = annotations;
    this.end = end;
    keys.each(new Proc() {
      @Override
      public void apply(String key) {
        advance(new KeyLocation(key, start));
      }
    });
    this.currentLocation = hasNext() ? start : -1;
  }

  @Override
  public ReadableStringSet nextLocation() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    StringSet currentKeys = CollectionUtils.createStringSet();

    currentLocation = locations.peek().location;

    do {
      KeyLocation keyLocation = locations.remove();
      currentKeys.add(keyLocation.key);
      advance(keyLocation);
    } while (!locations.isEmpty() && locations.peek().location == currentLocation);

    return currentKeys;
  }

  @Override
  public int currentLocation() {
    return currentLocation;
  }

  @Override
  public boolean hasNext() {
    return !locations.isEmpty();
  }

  /**
   * Advance the given key location to its next value change and set its
   * location. If it has one, place it back in the queue. If not (location ==
   * -1), do not place it back in the queue.
   *
   * @param keyLocation
   */
  private void advance(KeyLocation keyLocation) {
    V val = annotations.getAnnotation(keyLocation.location, keyLocation.key);
    keyLocation.location = annotations.firstAnnotationChange(keyLocation.location, end,
        keyLocation.key, val);
    if (keyLocation.location != -1) {
      locations.add(keyLocation);
    }
  }
}
