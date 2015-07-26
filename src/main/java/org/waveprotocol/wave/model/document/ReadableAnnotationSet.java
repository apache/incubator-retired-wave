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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * An annotation set which supports querying only.
 *
 * An annotation set is conceptually a map of key-value pairs for every item in
 * an XML document. An "item" is a start tag, end tag, or text character. All
 * keys have a default value of null in every position.
 *
 * The set of items referred to by a range is the set of items with location
 * greater than or equal to start, and strictly less than end.  Start must be
 * greater than or equal to zero, end must be greater than or equal to start,
 * and end must be less than or equal to {@code size()}.  Implementations are
 * not required to detect violations of these conditions, but if they do, they
 * should throw an IndexOutOfBoundsException.
 *
 * TODO(danilatos): More detailed / link to some design doc or similar?
 *
 * @param <V> base type of values that may be placed in this set. These values
 *        must have immutable semantics - the annotation set implementation must
 *        be free to reuse a given value in multiple places, and the values must
 *        implement {@code equals()} and {@code hashCode()} correctly.
 *
 * @author ohler@google.com (Christian Ohler)
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ReadableAnnotationSet<V> {

  /**
   * @return the size of the document
   */
  int size();

  /**
   * Finds the first item within a range with the specified key having a value
   * other than fromValue.
   *
   * @param start start of the range to search
   * @param end end of the range to search
   * @param key key to search for
   * @param fromValue value to check for change from
   * @return the location of the first change, or -1 if there is no change
   */
  int firstAnnotationChange(int start, int end, String key, V fromValue);

  /**
   * Finds the last item within a range with the specified key having a value
   * other than fromValue, and returns the position to the right of it
   * (its index plus one).
   *
   * If all items in the given range have the value fromValue, returns -1.
   *
   * @param start start of the range to search
   * @param end end of the range to search
   * @param key key to search for
   * @param fromValue value to check for change from
   * @return the position to the right of the last change, or -1 if there is
   * no change
   */
  int lastAnnotationChange(int start, int end, String key, V fromValue);

  /**
   * Gets the value of an annotation for a key on the item at the given
   * location.
   *
   * @param location a document location
   * @param key key to read
   * @return annotation value
   */
  V getAnnotation(int location, String key);

  /**
   * Call the callback for all annotations at the specified location, in
   * undefined order.
   */
  void forEachAnnotationAt(int location, ReadableStringMap.ProcV<V> callback);

  /**
   * Creates an AnnotationCursor over the specified range and key set.
   *
   * NOTE(ohler): We should deprecate/remove this.  Use annotationIntervals()
   * or rangedAnnotations() instead.
   *
   * @param start start of the range
   * @param end end of the range
   * @param keys key set for which to search for changes
   * @return the new annotation cursor
   */
  AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys);

  /**
   * Permits iteration over the annotation intervals that overlap a given range
   * for a given key set.
   *
   * The annotation intervals returned may extend beyond the boundaries
   * specified by start and end.
   *
   * NOTE: The iterator retains ownership of the AnnotationInterval objects
   * that it returns and may modify them destructively to reuse them.
   *
   * @param start start of the range
   * @param end end of the range
   * @param keys key set to which to project the annotation set; null means all
   * keys
   */
  Iterable<AnnotationInterval<V>> annotationIntervals(int start, int end, ReadableStringSet keys);

  /**
   * Permits iteration over the ranged annotations that overlap a given range
   * for a given key set.
   *
   * The ranged annotations returned may extend beyond the boundaries specified
   * by start and end.
   *
   * The ranged annotations returned provide a complete coverage of the given
   * range, including null values. It is easier to skip null values when they
   * are not needed than the inverse.
   *
   * NOTE: The iterator retains ownership of the RangedAnnotation objects that
   * it returns and may modify them destructively to reuse them.
   *
   * The order of traversal is left-to-right by start index. Ranged annotations
   * with the same start index may be returned in arbitrary order.
   *
   * @param start start of the range
   * @param end end of the range
   * @param keys key set to which to project the annotation set; null means all
   *        keys
   */
  Iterable<RangedAnnotation<V>> rangedAnnotations(int start, int end, ReadableStringSet keys);

  /**
   * @return a set of all known keys in the annotation set
   */
  ReadableStringSet knownKeys();
}
