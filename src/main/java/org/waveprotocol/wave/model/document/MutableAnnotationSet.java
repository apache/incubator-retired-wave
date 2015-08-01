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

import java.util.Comparator;
import java.util.List;

/**
 * An annotation set which supports setting and clearing annotated regions.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface MutableAnnotationSet<V> extends ReadableAnnotationSet<V> {

  // TODO(danilatos): Move/rename these alias interfaces as a top level
  // interface once use and semantics have crystallised.

  /**
   * An annotation set that may be serialised (e.g. via operations).
   */
  public interface Persistent extends MutableAnnotationSet<String> {
  }

  /**
   * An annotation set for local book-keeping only.
   */
  public interface Local extends MutableAnnotationSet<Object> {
  }

  /**
   * Sets the value for a key over a range. The value may be null, which clears
   * the annotation over the range.
   *
   * Only affects the given range, in contrast with
   * {@link #resetAnnotation(int, int, String, Object)}
   *
   * @param start location of first item in the range
   * @param end location of first item beyond the range
   * @param key annotation key
   * @param value value to set
   */
  void setAnnotation(int start, int end, String key, V value);

  /**
   * Sets the value for a key over a range and clears the value for all
   * locations outside the range as an atomic action.
   *
   * @see #setAnnotation(int, int, String, Object)
   */
  void resetAnnotation(int start, int end, String key, V value);

  /**
   * Immutable class to hold a range with a value
   */
  public final class RangedValue<V> {
    public final int start;
    public final int end;
    public final V value;

    public RangedValue(int start, int end, V value) {
      if (start > end) {
        throw new IllegalArgumentException("start must be <= end");
      }
      this.start = start;
      this.end = end;
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RangedValue) || obj == null) {
        return false;
      }
      RangedValue<V> rv = (RangedValue<V>) obj;
      return start == rv.start && end == rv.end && value.equals(rv.value);
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + start;
      result = 31 * result + end;
      result = 31 * result + value.hashCode();
      return result;
    }
  }

  /**
   * A special purpose comparator that compares RangedValues only using their
   * start and end values. It doesn't use the value, as the value is not
   * necessarily Comparable.
   *
   * Note that the ordering produced by this comparator is not consistent
   * with RangedValue<V>.equals(), eg
   *
   *   RangedValue<String>(1, 1, "a") and
   *   RangedValue<String>(1, 1, "b")
   *
   * are considered equal.
   */
  public class CompareRangedValueByStartThenEnd<V> implements Comparator<RangedValue<V>> {

    @Override
    public int compare(RangedValue<V> left, RangedValue<V> right) {
      int startDelta = left.start - right.start;
      if (startDelta != 0) {
        return startDelta;
      }

      // We don't compare on the value - we are only interested in the
      // start/end positions
      int endDelta = left.end - right.end;
      return endDelta;
    }
  }

  /**
   * Sets a set of values for a key within a given range and clears the value for all
   * other locations inside the range as an atomic action.
   *
   * The Map of ranges to set must be in order of increasing location and be
   * non-overlapping.
   *
   * NOTE(user) : This function is marked deprecated since it attempts to
   * emit a minimal set of mutations to set the requested list of annotations. This
   * may harm the semantics of the requested list in the presence of transforms.
   * The minimisation of mutations is an optimisation for demo purposes, and you
   * should only use this function if you know what you are doing. It will go away
   * once we have op combining so that the semantically correct mutations can be
   * efficiently sent.
   *
   * @param rangeStart the beginning of the range
   * @param rangeEnd the end of the range
   * @param key the key to be setting
   * @param values a mapping of a range onto a value
   */
  @Deprecated
  void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<RangedValue<V>> values);
}
