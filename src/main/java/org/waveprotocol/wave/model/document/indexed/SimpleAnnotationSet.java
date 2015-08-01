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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.indexed.OffsetPoint.Finder;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.AnnotationIntervalImpl;
import org.waveprotocol.wave.model.document.util.GenericAnnotationCursor;
import org.waveprotocol.wave.model.document.util.GenericAnnotationIntervalIterable;
import org.waveprotocol.wave.model.operation.OpCursorException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.EvaluableOffsetList;
import org.waveprotocol.wave.model.util.OffsetList;
import org.waveprotocol.wave.model.util.OffsetList.Container;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Simple implementation of an annotation set.
 *
 * The implementation is based on a single {@link OffsetList}, with each
 * container in the list representing a contiguous range of identical key-value
 * pairs.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SimpleAnnotationSet implements RawAnnotationSet<Object> {

  /**
   * "Immutable" map of values (can be abused, but please don't).
   */
  private static final class Values {
    private static final Values EMPTY = new Values();

    /** Cached hash value, used for fast equality comparison */
    private final int hash;
    /** The values */
    private final Map<String, Object> map;

    Values() {
      map = Collections.emptyMap();
      hash = map.hashCode();
    }

    Values(Values other, Changes changes) {
      map = new HashMap<String, Object>(other.map);
      for (Map.Entry<String, Pair<Integer, Object>> entry : changes.entrySet()) {
        if (entry.getValue() == null) {
          map.remove(entry.getKey());
        } else {
          map.put(entry.getKey(), entry.getValue().getSecond());
        }
      }
      hash = map.hashCode();
    }

    Object get(String key) {
      return map.get(key);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      } else if (obj instanceof Values) {
        Values v = (Values) obj;
        if (v.hash != hash) {
          return false;
        } else {
          return map.equals(v.map);
        }
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      return map.toString();
    }
  }

  /** Convenience type alias */
  @SuppressWarnings("serial")
  private static class Changes extends HashMap<String, Pair<Integer, Object>> {
  }

  /**
   * Represents a notification for our listeners, which we bunch up until the
   * end of a mutation.
   */
  private class Notification {
    int start;
    int end;
    String key;
    Object value;

    Notification(int start, int end, String key, Object value) {
      this.start = start;
      this.end = end;
      this.key = key;
      this.value = value;
    }

    void exe() {
      listener.onAnnotationChange(start, end, key, value);
    }
  }

  /**
   * Handy interface for traversing the list. Changes may be made to it in the
   * called methods, but only to the given container or previous containers. The
   * next and following containers may not be touched.
   */
  private interface ContainerTraverser {
    /** Called when the whole container is being traversed */
    void wholeContainer(OffsetList.Container<Values> container);

    /** Called when a portion of the container is being traversed */
    void partialContainer(OffsetList.Container<Values> container, int startOffset, int endOffset);

    /**
     * Called after the traversal is finished, with the container that would
     * have been traversed next
     */
    void finished(OffsetList.Container<Values> missedContainer);
  }

  /**
   * Handy finder instance.
   */
  private final Finder<Values> finder = OffsetPoint.finder();

  /**
   * Listener for changes.
   */
  private final AnnotationSetListener<Object> listener;

  /**
   * List of annotation ranges.
   */
  private final OffsetList<Values> ranges;

  /**
   * Current location during a mutation from the streaming interface.
   */
  private int currentLocation;

  /**
   * The current key-value pairs being "painted" during a mutation as the
   * currentLocation progresses.
   */
  private Changes currentChanges;

  /**
   * Values to the left of the current location, ignoring the effects of
   * annotation changes.
   */
  private Values oldValues;

  /**
   * Notifications being batched to be sent to the listener at the end of a
   * modification
   */
  private List<Notification> notifications;

  /**
   * @param listener listener for changes.
   */
  public SimpleAnnotationSet(AnnotationSetListener<Object> listener) {
    this.listener = listener;
    ranges = new EvaluableOffsetList<Values, Void>();
  }

  @Override
  public void begin() {
    reset();
    notifications = new ArrayList<Notification>();
  }

  @Override
  public void finish() {
    // For good measure
    reset();

    for (Notification notification : notifications) {
      notification.exe();
    }
  }

  private void reset() {
    currentChanges = new Changes();
    oldValues = Values.EMPTY;
    currentLocation = 0;
  }

  @Override
  public void skip(int skipSize) {
    if (skipSize > size() - currentLocation) {
      throw new OpCursorException("attempt to skip beyond end of document (cursor at "
          + currentLocation + ", size is " + size() + ", distance is " + skipSize + ")");
    }
    traverse(skipTraverser, skipSize);
    currentLocation += skipSize;
  }

  /**
   * Traverser used by skip, which "paints" the current key-value pairs into the
   * data structure, also handling merging and splitting of containers where
   * appropriate.
   */
  private final ContainerTraverser paintingTraverser = new ContainerTraverser() {
    public void partialContainer(OffsetList.Container<Values> container,
        int startOffset, int endOffset) {
      Values newValue = applyChanges(container.getValue());
      if (newValue.equals(container.getValue())) {
        // Don't split it up if the change has no effect to this container
        return;
      }

      if (startOffset > 0) {
        container = container.split(startOffset, container.getValue());
      }
      int size = endOffset - startOffset;
      if (size < container.size()) {
        container.split(size, container.getValue());
        container.setValue(newValue);
      } else {
        container.setValue(newValue);
      }
    }

    public void wholeContainer(OffsetList.Container<Values> container) {
      container.setValue(applyChanges(container.getValue()));
      maybeMergeWithPrevious(container);
    }

    public void finished(OffsetList.Container<Values> missedContainer) {
      maybeMergeWithPrevious(missedContainer);
    }
  };

  private final ContainerTraverser skipTraverser = new ContainerTraverser() {
    public void partialContainer(Container<Values> container, int startOffset, int endOffset) {
      oldValues = container.getValue();
      paintingTraverser.partialContainer(container, startOffset, endOffset);
    }

    public void wholeContainer(Container<Values> container) {
      oldValues = container.getValue();
      paintingTraverser.wholeContainer(container);
    }

    public void finished(Container<Values> missedContainer) {
      paintingTraverser.finished(missedContainer);
    }
  };

  @Override
  public void insert(int insertSize) {
    // Semantics: inserts always push an annotation boundary.
    OffsetPoint<Values> p = ranges.performActionAt(currentLocation, finder);
    OffsetList.Container<Values> current = p.getContainer();

    if (p.getOffset() > 0) {
      current = current.split(p.getOffset(), current.getValue());
    }

    current.insertBefore(oldValues, insertSize);
    traverse(paintingTraverser, insertSize);
    currentLocation += insertSize;


  }

  @Override
  public void delete(int deleteSize) {
    if (deleteSize > size() - currentLocation) {
      throw new OpCursorException("attempt to delete beyond end of document (cursor at "
          + currentLocation + ", size is " + size() + ", deleteSize is " + deleteSize + ")");
    }
    traverse(deleteTraverser, deleteSize);
  }

  /**
   * Traverser for deleting containers or parts of containers across a range.
   */
  private final ContainerTraverser deleteTraverser = new ContainerTraverser() {
    public void partialContainer(OffsetList.Container<Values> container,
        int startOffset, int endOffset) {
      oldValues = container.getValue();
      container.increaseSize(startOffset - endOffset);
    }

    public void wholeContainer(OffsetList.Container<Values> container) {
      oldValues = container.getValue();
      container.remove();
    }

    public void finished(OffsetList.Container<Values> missedContainer) {
      maybeMergeWithPrevious(missedContainer);
    }
  };

  @Override
  public void startAnnotation(String key, Object value) {
    maybeNoteChange(key);
    currentChanges.put(key, new Pair<Integer, Object>(currentLocation, value));
  }

  @Override
  public void endAnnotation(String key) {
    assert currentChanges.containsKey(key);
    maybeNoteChange(key);
    currentChanges.remove(key);
  }

  /**
   * Check if a notification is relevant at this point for the given key, and if
   * so, note it for later.
   *
   * @param key
   */
  private void maybeNoteChange(String key) {
    if (listener == null) {
      return;
    }
    if (currentChanges.containsKey(key)) {
      Pair<Integer, Object> info = currentChanges.get(key);

      notifications.add(new Notification(info.getFirst().intValue(), currentLocation, key, info
          .getSecond()));
    }
  }

  /**
   * Return a new value with the currentChanges applied to the given value
   */
  private Values applyChanges(Values values) {
    return currentChanges.isEmpty() ? values : new Values(values, currentChanges);
  }

  /**
   * Merge the given container with the previous container, if they have equal
   * values
   */
  private void maybeMergeWithPrevious(OffsetList.Container<Values> container) {
    OffsetList.Container<Values> previous = container.getPreviousContainer();
    if (previous != ranges.sentinel() && previous.getValue().equals(container.getValue())) {
      container.increaseSize(previous.size());
      previous.remove();
    }
  }

  /**
   * @see ContainerTraverser
   */
  private void traverse(ContainerTraverser traverser, int distance) {
    OffsetPoint<Values> p = ranges.performActionAt(currentLocation, finder);
    int offset = p.getOffset();
    OffsetList.Container<Values> container = p.getContainer();

    while (distance > 0) {
      int containerMoveSize = Math.min(distance, container.size() - offset);
      OffsetList.Container<Values> next = container.getNextContainer();
      if (offset > 0) {
        traverser.partialContainer(container, offset, offset + containerMoveSize);
      } else if (containerMoveSize == container.size()) {
        traverser.wholeContainer(container);
      } else {
        traverser.partialContainer(container, 0, containerMoveSize);
      }

      offset = 0;
      distance -= containerMoveSize;
      container = next;
    }

    traverser.finished(container);
  }

  // Reader methods

  @Override
  public int size() {
    return ranges.size();
  }

  @Override
  public Object getAnnotation(int location, String key) {
    Preconditions.checkElementIndex(location, size());
    checkKeyNotNull(key);

    Values values = ranges.performActionAt(location, finder).getValue();
    return values == null ? null : values.get(key);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, Object fromValue) {
    Preconditions.checkPositionIndexes(start, end, size());
    checkKeyNotNull(key);
    start = Math.max(0, start);
    end = Math.min(end, ranges.size());

    OffsetPoint<Values> point = ranges.performActionAt(start, finder);
    OffsetList.Container<Values> container = point.getContainer();
    int offset = point.getOffset();

    int location = start;
    while (location < end) {
      if (!eq(getValue(container, key), fromValue)) {
        return location;
      }

      if (container == ranges.sentinel()) {
        break;
      }

      location += container.size() - offset;
      container = container.getNextContainer();
      offset = 0;
    }

    return -1;
  }

  @Override
  public int lastAnnotationChange(int start, int end, String key, Object fromValue) {
    Preconditions.checkPositionIndexes(start, end, size());
    checkKeyNotNull(key);
    start = Math.max(0, start);
    end = Math.min(end, ranges.size());

    OffsetPoint<Values> point = ranges.performActionAt(end, finder);
    OffsetList.Container<Values> container = point.getContainer();
    int offset = point.getOffset();
    if (offset == 0) {
      container = container.getPreviousContainer();
      offset = container == ranges.sentinel() ? 0 : container.size();
    }

    int location = end;
    while (location > start) {
      if (!eq(getValue(container, key), fromValue)) {
        return location;
      }

      if (container == null) {
        break;
      }

      location -= offset;
      container = container.getPreviousContainer();
      offset = container == ranges.sentinel() ? 0 : container.size();
    }

    return -1;
  }

  @Override
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    if (keys == null) {
      throw new RuntimeException("not implemented");
    }
    return new GenericAnnotationCursor<Object>(this, start, end, keys);
  }

  private Object getValue(OffsetList.Container<Values> container, String key) {
    // If only we had the Maybe monad in Java...
    return (container == ranges.sentinel()) ? null :
        (container.getValue() == null) ? null : container.getValue().get(key);
  }

  private boolean eq(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }

  protected void checkKeyNotNull(String key) {
    Preconditions.checkNotNull(key, "key must not be null");
  }

  @Override
  public void forEachAnnotationAt(int location, ReadableStringMap.ProcV<Object> callback) {
    throw new RuntimeException("not implemented");
  }

  private class RangesIterator implements Iterator<AnnotationInterval<Object>> {

    Container<Values> next = ranges.firstContainer();

    @Override
    public boolean hasNext() {
      return next != ranges.sentinel();
    }

    @Override
    public AnnotationInterval<Object> next() {
      if (!hasNext()) {
        throw new NoSuchElementException("no more intervals");
      }
      StringMap<Object> annotations = CollectionUtils.createStringMap();
      annotations.putAll(next.getValue().map);
      StringMap<Object> diffFromLeft = CollectionUtils.createStringMap();

      for (Map.Entry<String, Object> e : next.getValue().map.entrySet()) {
        String key = e.getKey();
        Object value = e.getValue();
        if ((next.getPreviousContainer() == ranges.sentinel())) {
          if (value != null) {
            diffFromLeft.put(key, value);
          }
        } else {
          if (!eq(value, next.getPreviousContainer().getValue().get(key))) {
            diffFromLeft.put(key, value);
          }
        }
      }

      if (next.getPreviousContainer() != ranges.sentinel()) {
        // Find and add value=null entries that may be implicit.
        for (Map.Entry<String, Object> e : next.getPreviousContainer().getValue().map.entrySet()) {
          String key = e.getKey();
          Object value = e.getValue();
          if (value != null && !next.getValue().map.containsKey(key)) {
            diffFromLeft.put(key, null);
            annotations.put(key, null);
          }
        }
      }

      AnnotationInterval<Object> i = new AnnotationIntervalImpl<Object>(next.offset(),
          next.offset() + next.size(), annotations, diffFromLeft);

      next = next.getNextContainer();

      return i;

    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("removing an annotation interval is not supported");
    }

  }

  @Override
  public Iterable<AnnotationInterval<Object>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    if (keys == null) {
      if (start > 0 || end < size()) {
        throw new RuntimeException("not implemented");
      }
      return new Iterable<AnnotationInterval<Object>>() {
        @Override
        public Iterator<AnnotationInterval<Object>> iterator() {
          return new RangesIterator();
        }
      };
    }
    return new GenericAnnotationIntervalIterable<Object>(this, start, end, keys);
  }

  @Override
  public Iterable<RangedAnnotation<Object>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public ReadableStringSet knownKeysLive() {
    throw new UnsupportedOperationException("knownKeysLive");
  }

  @Override
  public ReadableStringSet knownKeys() {
    throw new UnsupportedOperationException("knownKeys");
  }

  @Override
  public String getInherited(String key) {
    throw new UnsupportedOperationException("getInherited");
  }
}
