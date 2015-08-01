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
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.GenericAnnotationCursor;
import org.waveprotocol.wave.model.document.util.GenericAnnotationIntervalIterable;
import org.waveprotocol.wave.model.document.util.GenericRangedAnnotationIterable;
import org.waveprotocol.wave.model.operation.OpCursorException;
import org.waveprotocol.wave.model.util.CollectionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.LinkedList;

/**
 * An implementation of RawAnnotationSet based on BasicForceAnnotationTree.
 * Also provides a listener notification mechanism.
 *
 * @param <V> the value type
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class AnnotationTree<V> implements RawAnnotationSet<V> {

  private class Notification {
    int start;
    int end;
    String key;
    V value;

    Notification(int start, int end, String key, V value) {
      this.start = start;
      this.end = end;
      this.key = key;
      this.value = value;
    }

    void deliver() {
      if (listener != null) {
        listener.onAnnotationChange(start, end, key, value);
      }
    }

    @Override
    public String toString() {
      return "Notification(" + start + "-" + end + " " + key + "=" + value + ")";
    }
  }

  private final CollectionFactory factory = CollectionUtils.getCollectionFactory();

  private final BasicAnnotationTree<V> tree;
  private AnnotationSetListener<V> listener;

  private final StringMap<OpenAnnotation> openAnnotations = CollectionUtils.createStringMap();

  private boolean currentlyNotifying = false;

  private int itemsDeletedThisRun = 0;

  private final LinkedList<Notification> queuedNotifications = new LinkedList<Notification>();

  /**
   * Creates a new AnnotationTree with String keys and values of type V.
   *
   * The arguments oneValue and anotherValue must not be null, and they must
   * not be equal according to their equals() method.  AnnotationTree uses these
   * values internally as temporary placeholders during tree manipulations.  The
   * choice of values has no effect on AnnotationTree's externally visible
   * behavior.
   *
   * The argument listener may be null.  In that case, the AnnotationTree will
   * not produce notifications.
   */
  public AnnotationTree(V oneValue, V anotherValue, AnnotationSetListener<V> listener) {
    Preconditions.checkNotNull(oneValue, "The argument oneValue must not be null");
    Preconditions.checkNotNull(anotherValue, "The argument anotherValue must not be null");
    Preconditions.checkArgument(!oneValue.equals(anotherValue),
        "The arguments oneValue and anotherValue must not be equal");
    this.tree = new BasicAnnotationTree<V>(oneValue, anotherValue);
    this.listener = listener;
  }

  /**
   * For debugging.
   */
  public String toStringForDebugging() {
    return tree.toStringForDebugging();
  }

  /**
   * Checks the internal invariants of the tree data structure and throws an
   * exception if any are violated.  For debugging.
   */
  public void checkSomeInvariants() {
    tree.checkSomeInvariants();
  }

  /**
   * Set the listener that will receive annotation events.
   *
   * It is OK to set the listener to null.  In that case, the AnnotationTree
   * will not produce notifications.
   *
   * @param listener The listener that will receive annotation events.
   */
  public void setListener(AnnotationSetListener<V> listener) {
    this.listener = listener;
  }

  // -1 means not currently traversing.
  private int cursor = -1;
  private StringMap<V> inheritedAnnotationsForInsertion = null;

  private class OpenAnnotation {
    int start;
    String key;
    V value;

    OpenAnnotation(int start, String key, V value) {
      this.start = start;
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return "PendingAnnotation(" + start + ", " + key + ", " + value + ")";
    }
  }

  protected void queueNotification(int start, int end, String key, V value) {
    if (!queuedNotifications.isEmpty()) {
      Notification p = queuedNotifications.getLast();
      if (p.end == start && p.key.equals(key) && ValueUtils.equal(p.value, value)) {
        p.end = end;
        return;
      }
    }
    Notification n = new Notification(start, end, key, value);
    queuedNotifications.add(n);
  }

  @Override
  public void begin() {
    if (cursor != -1) {
      throw new IllegalStateException("begin() called twice with no finish() in between");
    }
    openAnnotations.clear();
    itemsDeletedThisRun = 0;
    if (!currentlyNotifying) {
      queuedNotifications.clear();
    }

    assert openAnnotations.isEmpty();
    if (!currentlyNotifying) {
      assert queuedNotifications.isEmpty();
    }
    assert itemsDeletedThisRun == 0;

    cursor = 0;
    inheritedAnnotationsForInsertion = factory.createStringMap();
  }

  @Override
  public void finish() {
    if (!openAnnotations.isEmpty()) {
      openAnnotations.each(new ReadableStringMap.ProcV<OpenAnnotation>() {
        @Override
        public void apply(String key, OpenAnnotation openAnnotation) {
          throw new IllegalStateException("finish() called while annotations are still open: "
              + openAnnotation);
        }
      });
      assert false;
    }
    if (cursor == -1) {
      throw new IllegalStateException("finish() called with no matching begin()");
    }
    cursor = -1;
    inheritedAnnotationsForInsertion = null;
    tree.cleanupKnownKeys();
    itemsDeletedThisRun = 0;

    if (!currentlyNotifying) {
      try {
        currentlyNotifying = true;
        while (!queuedNotifications.isEmpty()) {
          Notification n = queuedNotifications.remove();
          n.deliver();
        }
      } finally {
        queuedNotifications.clear();
        currentlyNotifying = false;
      }
    }
  }


  // An ill-formed operation is one with an incorrect structure (e.g., contains
  // endAnnotation with no matching startAnnotation or zero-length skips).
  // An invalid operation is an operation that does not apply to this document
  // in its current state but could potentially apply to other documents.
  //
  // We use assertions for well-formedness checks and throw OperationExceptions
  // for invalid documents.

  // TODO(ohler): export this.
  private void collectAllAnnotationsAt(int position, StringMap<V> accu) {
    Preconditions.checkElementIndex(position, tree.length());
    tree.collectAllAnnotationsAt(position, accu);
  }

  private void updateInheritedAnnotationsFromPosition(int position) {
    // TODO(ohler): We could save some work here by traveling the
    // direct path from the previous leaf to the new leaf.
    collectAllAnnotationsAt(position, inheritedAnnotationsForInsertion);
  }

  @Override
  public void skip(int distance) {
    assert cursor != -1;
    assert distance > 0;
    if (distance > size() - cursor) {
      throw new OpCursorException("Attempt to skip beyond end of document (cursor at "
          + cursor + ", size is " + size() + ", distance is " + distance + ")");
    }

    cursor += distance;
    assert cursor > 0;
    updateInheritedAnnotationsFromPosition(cursor - 1);
  }

  @Override
  public void delete(int deleteSize) {
    assert cursor != -1;
    assert deleteSize > 0;
    if (deleteSize > size() - cursor) {
      throw new OpCursorException("Attempt to delete beyond end of document (cursor at "
          + cursor + ", size is " + size() + ", deleteSize is " + deleteSize + ")");
    }

    updateInheritedAnnotationsFromPosition(cursor + deleteSize - 1);
    tree.delete(cursor, cursor + deleteSize);
  }

  @Override
  public void insert(int insertSize) {
    assert cursor != -1;
    assert insertSize > 0;
    tree.insert(cursor, insertSize);
    final int start = cursor;
    final int end = cursor + insertSize;
    inheritedAnnotationsForInsertion.each(new StringMap.ProcV<V>() {
      @Override
      public void apply(String key, V value) {
        if (!openAnnotations.containsKey(key)) {
          tree.setAnnotation(start, end, key, value);
        }
      }
    });
    // Unset all other annotations for the inserted region to avoid
    // inheriting in situations where an endAnnotation preceded this
    // insert.  TODO(ohler): Doing two passes (one for insertions and
    // deletions, one for setting annotations) would eliminate the
    // need for this and thus allow us to clean up known keys in
    // setAnnotation().
    tree.knownKeys().each(new StringSet.Proc() {
      @Override
      public void apply(String key) {
        if (!inheritedAnnotationsForInsertion.containsKey(key)
            && !openAnnotations.containsKey(key)) {
          tree.setAnnotation(start, end, key, null);
        }
      }
    });
    cursor += insertSize;
  }

  private static boolean isNonLocalAnnotationKey(String key) {
    return !(Annotations.isLocal(key));
  }

  @Override
  public String getInherited(String key) {
    if (isNonLocalAnnotationKey(key)) {
      return (String) inheritedAnnotationsForInsertion.get(key, null);
    } else {
      return null;
    }
  }

  @Override
  public void startAnnotation(String key, V value) {
    assert cursor != -1;
    assert key != null;
    if (isNonLocalAnnotationKey(key) && value != null && !(value instanceof String)) {
      throw new IllegalArgumentException(
          "Attempt to store a non-string object in a non-local annotation: "
          + key + "=" + value);
    }
    if (openAnnotations.containsKey(key)) {
      if (ValueUtils.equal(value, openAnnotations.getExisting(key).value)) {
        return;
      }
      endAnnotationUnchecked(key);
    }
    openAnnotations.put(key, new OpenAnnotation(cursor, key, value));
  }

  @Override
  public void endAnnotation(String key) {
    assert cursor != -1;
    assert key != null;
    assert openAnnotations.containsKey(key);
    endAnnotationUnchecked(key);
  }

  private void endAnnotationUnchecked(String key) {
    assert openAnnotations.containsKey(key);
    OpenAnnotation a = openAnnotations.getExisting(key);
    openAnnotations.remove(key);
    int end = cursor;
    tree.setAnnotation(a.start, end, a.key, a.value);
    queueNotification(a.start, end, a.key, a.value);
  }

  @Override
  public int size() {
    return tree.length();
  }

  @Override
  public V getAnnotation(int location, String key) {
    Preconditions.checkElementIndex(location, size());
    checkKeyNotNull(key);
    return tree.getAnnotation(location, key);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, V fromValue) {
    Preconditions.checkPositionIndexes(start, end, size());
    checkKeyNotNull(key);
    return tree.firstAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public int lastAnnotationChange(int start, int end, String key, V fromValue) {
    Preconditions.checkPositionIndexes(start, end, size());
    checkKeyNotNull(key);
    return tree.lastAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public void forEachAnnotationAt(int index, ReadableStringMap.ProcV<V> callback) {
    Preconditions.checkElementIndex(index, size());
    tree.forEachAnnotationAt(index, callback);
  }

  @Override
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    Preconditions.checkPositionIndexes(start, end, size());
    if (keys == null) {
      //return tree.allAnnotationsCursor(start, end);
      throw new RuntimeException("Not supported");
    } else {
      return new GenericAnnotationCursor<V>(this, start, end, keys);
    }
  }

  /**
   * For simplicity of implementation, this implementation doesn't
   * guarantee that the start of the first interval is minimal.  I.e., it
   * may be that the item to the left of start() of the first interval returned
   * actually has the same annotations.
   */
  @Override
  public Iterable<AnnotationInterval<V>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    Preconditions.checkPositionIndexes(start, end, size());
    if (keys == null || tree.knownKeys().isSubsetOf(keys)) {
      // TODO(ohler): Implement this more efficiently with support
      // from the underlying data structure.
      return new GenericAnnotationIntervalIterable<V>(this, start, end, tree.knownKeys());
    } else {
      return new GenericAnnotationIntervalIterable<V>(this, start, end, keys);
    }
  }

  @Override
  public Iterable<RangedAnnotation<V>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    Preconditions.checkPositionIndexes(start, end, size());
    if (keys == null) {
      keys = tree.knownKeys();
    }
    return new GenericRangedAnnotationIterable<V>(this, start, end, keys);
  }

  @Override
  public ReadableStringSet knownKeys() {
    return CollectionUtils.copyStringSet(tree.knownKeys());
  }

  @Override
  public ReadableStringSet knownKeysLive() {
    return tree.knownKeys();
  }

  protected void checkKeyNotNull(String key) {
    Preconditions.checkNotNull(key, "Key must not be null");
  }

// end hack

}
