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

import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * An iterator over RangedAnnotations for a ReadableAnnotationSet
 * restricted to a given set of keys and a given range.
 *
 * The iterator will iterate over all ranged annotations that
 * intersect the specified range; the starts and ends of the annotations
 * returned may lie outside of the range the cursor is iterating over.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of values in the annotation set
 */
public class GenericRangedAnnotationIterable<V> implements Iterable<RangedAnnotation<V>> {
  private static class GenericRangedAnnotationIterator<V> implements Iterator<RangedAnnotation<V>> {
    private static class KeyEntry<V> implements Comparable<KeyEntry<V>> {
      final String key;
      int start;
      int end;
      V value;

      private KeyEntry(String key, int initialStart, int initialEnd, V initialValue) {
        this.key = key;
        this.start = initialStart;
        this.end = initialEnd;
        this.value = initialValue;
      }

      @Override
      public int compareTo(KeyEntry<V> other) {
        // PriorityQueue sorts the smallest item to the top.
        return start - other.start;
      }
    }

    private final ReadableAnnotationSet<V> a;
    private final int end;
    private final Queue<KeyEntry<V>> entries = new PriorityQueue<KeyEntry<V>>();

    private RangedAnnotationImpl<V> rangedAnnotation = null;

    public GenericRangedAnnotationIterator(final ReadableAnnotationSet<V> a,
        final int start, int end, ReadableStringSet keys) {
      Preconditions.checkPositionIndexes(start, end, a.size());
      Preconditions.checkNotNull(keys,
          "GenericRangedAnnotationIterator: Key set must not be null");

      this.a = a;
      this.end = end;

      if (start >= end) {
        // entries remains empty.
        return;
      }

      keys.each(new Proc() {
        @Override
        public void apply(String key) {
          V firstValue = a.getAnnotation(start, key);
          int firstStart;
          firstStart = a.lastAnnotationChange(0, start, key, firstValue);
          if (firstStart == -1) {
            firstStart = 0;
          }
          int firstEnd;
          firstEnd = a.firstAnnotationChange(start, a.size(), key, firstValue);
          if (firstEnd == -1) {
            firstEnd = a.size();
          }
          entries.add(new KeyEntry<V>(key, firstStart, firstEnd, firstValue));
        }
      });
    }

    @Override
    public boolean hasNext() {
      return !entries.isEmpty();
    }

    @Override
    public RangedAnnotation<V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more ranged annotations");
      }

      KeyEntry<V> entry = entries.remove();

      if (rangedAnnotation == null) {
        rangedAnnotation = new RangedAnnotationImpl<V>(entry.key, entry.value,
            entry.start, entry.end);
      } else {
        rangedAnnotation.set(entry.key, entry.value, entry.start, entry.end);
      }

      entry.start = entry.end;
      if (entry.start < end) {
        entry.value = a.getAnnotation(entry.start, entry.key);
        entry.end = a.firstAnnotationChange(entry.start, a.size(), entry.key, entry.value);
        if (entry.end == -1) {
          entry.end = a.size();
        }
        entries.add(entry);
      }
      return rangedAnnotation;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing a ranged annotation is not supported");
    }
  }

  private final ReadableAnnotationSet<V> a;
  private final int start;
  private final int end;
  private final ReadableStringSet keys;

  public GenericRangedAnnotationIterable(ReadableAnnotationSet<V> a, int start, int end,
      ReadableStringSet keys) {
    this.a = a;
    this.start = start;
    this.end = end;
    this.keys = keys;
  }

  @Override
  public Iterator<RangedAnnotation<V>> iterator() {
    return new GenericRangedAnnotationIterator<V>(a, start, end, keys);
  }
}
