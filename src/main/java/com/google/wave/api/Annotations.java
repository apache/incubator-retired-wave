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

package com.google.wave.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A class that models a map of annotations, keyed by the annotation name.
 *
 * Each key maps into a list of {@link Annotation} instances, since one
 * annotation name can exist in different parts (denoted by different ranges) of
 * a blip, each with its own value.
 *
 * This class is iterable, but the iterator does not support element removal
 * yet.
 */
public class Annotations implements Iterable<Annotation>, Serializable {

  /** A map of annotation name to a list of annotations instances. */
  private final Map<String, List<Annotation>> store = new HashMap<String, List<Annotation>>();

  /** The total number of annotations. */
  private int size;

  /**
   * Adds a new annotation.
   *
   * @param name the name of the annotation.
   * @param value the value of the annotation.
   * @param start the starting index of the annotation.
   * @param end the end index of the annotation.
   */
  void add(String name, String value, int start, int end) {
    if (!store.containsKey(name)) {
      store.put(name, Arrays.asList(new Annotation(name, value, start, end)));
      size++;
      return;
    }

    int existingSize = store.get(name).size();
    List<Annotation> newList = new ArrayList<Annotation>();
    for (Annotation existing : store.get(name)) {
      if (start > existing.getRange().getEnd() || end < existing.getRange().getStart()) {
        // Add non-overlapping annotation to the new list as is.
        newList.add(existing);
      } else if (existing.getValue().equals(value)) {
        // Merge the annotation.
        start = Math.min(existing.getRange().getStart(), start);
        end = Math.max(existing.getRange().getEnd(), end);
      } else {
        // Chop the bits off the existing annotation.
        if (existing.getRange().getStart() < start) {
          newList.add(new Annotation(existing.getName(), existing.getValue(),
              existing.getRange().getStart(), start));
        }
        if (existing.getRange().getEnd() > end) {
          newList.add(new Annotation(existing.getName(), existing.getValue(),
              end, existing.getRange().getEnd()));
        }
      }
    }
    newList.add(new Annotation(name, value, start, end));
    store.put(name, newList);
    size += newList.size() - existingSize;
  }

  /**
   * Deletes an annotation. This is a no-op if the blip doesn't have this
   * annotation.
   *
   * @param name the name of the annotation to be deleted.
   * @param start the starting index of the annotation to be deleted.
   * @param end the end index of the annotation to be deleted.
   */
  void delete(String name, int start, int end) {
    if (!store.containsKey(name)) {
      return;
    }

    int existingSize = store.get(name).size();
    List<Annotation> newList = new ArrayList<Annotation>();
    for (Annotation existing : store.get(name)) {
      if (start > existing.getRange().getEnd() || end < existing.getRange().getStart()) {
        newList.add(existing);
      } else if (start < existing.getRange().getStart() && end > existing.getRange().getEnd()) {
        continue;
      } else {
        // Chop the bits off the existing annotation.
        if (existing.getRange().getStart() < start) {
          newList.add(new Annotation(existing.getName(), existing.getValue(),
              existing.getRange().getStart(), start));
        }
        if (existing.getRange().getEnd() > end) {
          newList.add(new Annotation(existing.getName(), existing.getValue(),
              end, existing.getRange().getEnd()));
        }
      }
    }

    if (!newList.isEmpty()) {
      store.put(name, newList);
    } else {
      store.remove(name);
    }
    size -= existingSize - newList.size();
  }

  /**
   * Shifts all annotations that have a range that is after or covers the given
   * position.
   *
   * @param position the anchor position.
   * @param shiftAmount the amount to shift the annotation range.
   */
  void shift(int position, int shiftAmount) {
    for (List<Annotation> annotations : store.values()) {
      for (Annotation annotation : annotations) {
        annotation.shift(position, shiftAmount);
      }
    }

    // Merge fragmented annotations that should be contiguous, for example:
    // Annotation("foo", "bar", 1, 2) and Annotation("foo", "bar", 2, 3).
    for (Entry<String, List<Annotation>> entry : store.entrySet()) {
      List<Annotation> existingList = entry.getValue();
      List<Annotation> newList = new ArrayList<Annotation>(existingList.size());

      for (int i = 0; i < existingList.size(); ++i) {
        Annotation annotation = existingList.get(i);
        String name = annotation.getName();
        String value = annotation.getValue();
        int start = annotation.getRange().getStart();
        int end = annotation.getRange().getEnd();

        // Find the last end index.
        for (int j = i + 1; j < existingList.size(); ++j) {
          if (end < existingList.get(j).getRange().getStart()) {
            break;
          }

          if (end == existingList.get(j).getRange().getStart() &&
              value.equals(existingList.get(j).getValue())) {
            end = existingList.get(j).getRange().getEnd();
            existingList.remove(j--);
          }
        }
        newList.add(new Annotation(name, value, start, end));
      }
      entry.setValue(newList);
    }
  }

  /**
   * Returns a list of annotation instances that has the given name.
   *
   * @param name the annotation name.
   * @return a list of {@link Annotation} instances in the owning blip that has
   *     the given name.
   */
  public List<Annotation> get(String name) {
    return store.get(name);
  }

  /**
   * Returns the number of distinct annotation names that the owning blip has.
   *
   * @return the number of distinct annotation names.
   */
  public int size() {
    return store.size();
  }

  /**
   * Returns a set of annotation names that the owning blip has.
   *
   * @return a set of annotation names.
   */
  public Set<String> namesSet() {
    return new HashSet<String>(store.keySet());
  }

  /**
   * Returns this {@link Annotations} object as a {@link List} of annotations.
   *
   * @return an unmodifiable list of annotations.
   */
  public List<Annotation> asList() {
    List<Annotation> annotations = new ArrayList<Annotation>(size);
    for (Annotation annotation : this) {
      annotations.add(annotation);
    }
    return Collections.unmodifiableList(annotations);
  }

  @Override
  public Iterator<Annotation> iterator() {
    return new AnnotationIterator(store);
  }

  /**
   * An iterator over all annotations in this annotation set. Currently, it
   * doesn't support {@code remove()} operation.
   */
  private static class AnnotationIterator implements Iterator<Annotation> {
    private Iterator<Annotation> listIterator;
    private final Iterator<List<Annotation>> mapIterator;

    /**
     * Constructor.
     *
     * @param store a map of annotation name to a list of annotations instances.
     */
    private AnnotationIterator(Map<String, List<Annotation>> store) {
      mapIterator = store.values().iterator();
      if (!store.isEmpty()) {
        listIterator = mapIterator.next().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      return mapIterator.hasNext() || (listIterator != null && listIterator.hasNext());
    }

    @Override
    public Annotation next() {
      if (!listIterator.hasNext() && mapIterator.hasNext()) {
        listIterator = mapIterator.next().iterator();
      }
      return listIterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
