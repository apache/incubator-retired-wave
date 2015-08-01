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

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * An iterable over AnnotationIntervals for a ReadableAnnotationSet
 * restricted to a given set of keys and a given range.
 *
 * The iterator will iterate over all intervals that
 * intersect the specified range; the start of the first interval and the end
 * of the last interval returned may lie outside of the range being iterated
 * over.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of values in the annotation set
 */
public class GenericAnnotationIntervalIterable<V> implements Iterable<AnnotationInterval<V>> {

  private static class GenericAnnotationIntervalIterator<V>
      implements Iterator<AnnotationInterval<V>> {
    private static class KeyEntry implements Comparable<KeyEntry> {
      final String key;
      int nextChange;

      private KeyEntry(String key, int nextChange) {
        this.key = key;
        this.nextChange = nextChange;
      }

      @Override
      public int compareTo(KeyEntry other) {
        // PriorityQueue sorts the smallest item to the top.
        return nextChange - other.nextChange;
      }
    }

    private final ReadableAnnotationSet<V> set;
    private final int end;
    private final Queue<KeyEntry> entries = new PriorityQueue<KeyEntry>();

    private AnnotationIntervalImpl<V> interval = null;
    private int intervalStart;
    private final StringMap<V> annotationsHere;
    private final StringMap<V> diffFromLeft;

    private boolean valuesEqual(V a, V b) {
      if (a == null) {
        return b == null;
      } else {
        return a.equals(b);
      }
    }

    boolean first = true;

    // TODO(ohler): simplify.
    public GenericAnnotationIntervalIterator(final ReadableAnnotationSet<V> set,
        final int start, final int end, ReadableStringSet keys) {

      Preconditions.checkPositionIndexes(start, end, set.size());
      Preconditions.checkNotNull(keys,
          "GenericAnnotationIntervalIterator: Key set must not be null");

      this.set = set;
      this.end = end;

      this.annotationsHere = CollectionUtils.createStringMap();
      this.diffFromLeft = CollectionUtils.createStringMap();
      this.intervalStart = start;

      if (start >= end) {
        first = false;
        // entries remains empty.
        return;
      }

      // Make annotations and diffFromLeft reflect the first interval
      // make every keyEntry point to the next change for that key
      keys.each(new Proc() {
        @Override
        public void apply(String key) {
          V startValue = set.getAnnotation(start, key);
          V valueLeft;
          if (start == 0) {
            valueLeft = null;
          } else {
            valueLeft = set.getAnnotation(start - 1, key);
          }
          if (valuesEqual(valueLeft, startValue)) {
            // no entry in diffFromLeft
          } else {
            diffFromLeft.put(key, startValue);
          }
          annotationsHere.put(key, startValue);
          int nextChange = set.firstAnnotationChange(start, end, key, startValue);
          if (nextChange == -1) {
            // don't add
          } else {
            entries.add(new KeyEntry(key, nextChange));
          }
        }
      });
    }

    @Override
    public boolean hasNext() {
      // Maybe check nextIntervalStart?
      return first || !entries.isEmpty();
    }

    @Override
    public AnnotationInterval<V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more intervals");
      }

      int thisIntervalStart = intervalStart;
      int nextIntervalStart;
      assert thisIntervalStart < end;
      if (first == true) {
        if (entries.isEmpty()) {
          nextIntervalStart = end;
        } else {
          nextIntervalStart = entries.peek().nextChange;
        }
        first = false;
      } else {
        diffFromLeft.clear();
        do {
          KeyEntry entry = entries.remove();
          assert thisIntervalStart == entry.nextChange;
          String key = entry.key;
          V value = set.getAnnotation(thisIntervalStart, key);
          diffFromLeft.put(key, value);
          annotationsHere.put(key, value);
          entry.nextChange = set.firstAnnotationChange(thisIntervalStart + 1, end, key, value);
          if (entry.nextChange != -1) {
            entries.add(entry);
          }
        } while (!entries.isEmpty() && entries.peek().nextChange == thisIntervalStart);
        if (!entries.isEmpty()) {
          nextIntervalStart = entries.peek().nextChange;
        } else {
          nextIntervalStart = end;
        }
      }

      if (interval == null) {
        interval = new AnnotationIntervalImpl<V>(thisIntervalStart, nextIntervalStart,
            annotationsHere, diffFromLeft);
      } else {
        interval.set(thisIntervalStart, nextIntervalStart, annotationsHere, diffFromLeft);
      }
      intervalStart = nextIntervalStart;
      return interval;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing an annotation interval is not supported");
    }
  }

  private final ReadableAnnotationSet<V> a;
  private final int start;
  private final int end;
  private final ReadableStringSet keys;

  public GenericAnnotationIntervalIterable(ReadableAnnotationSet<V> a, int start, int end,
      ReadableStringSet keys) {
    this.a = a;
    this.start = start;
    this.end = end;
    this.keys = keys;
  }

  @Override
  public Iterator<AnnotationInterval<V>> iterator() {
    return new GenericAnnotationIntervalIterator<V>(a, start, end, keys);
  }
}
