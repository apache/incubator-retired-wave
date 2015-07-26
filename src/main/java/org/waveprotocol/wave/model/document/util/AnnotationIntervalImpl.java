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
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;

/**
 * An implementation of AnnotationInterval that simply holds values passed to it.
 *
 * This class has a mutator so that instances can be reused.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of annotation values
 */
public final class AnnotationIntervalImpl<V> implements AnnotationInterval<V> {
  private int start;
  private int end;
  private ReadableStringMap<V> annotations;
  private ReadableStringMap<V> diffFromLeft;

  public AnnotationIntervalImpl(int start, int end, ReadableStringMap<V> annotations,
      ReadableStringMap<V> diffFromLeft) {
    set(start, end, annotations, diffFromLeft);
  }

  public AnnotationIntervalImpl(AnnotationInterval<V> other) {
    this(other.start(), other.end(),
        CollectionUtils.copyStringMap(other.annotations()),
        CollectionUtils.copyStringMap(other.diffFromLeft()));
  }

  public void set(int start, int end, ReadableStringMap<V> annotations,
      ReadableStringMap<V> diffFromPrevious) {
    // We don't have access to the size of the annotation set, but we can
    // still check 0 <= start <= end.
    Preconditions.checkPositionIndexes(start, end, Integer.MAX_VALUE);
    Preconditions.checkNotNull(annotations, "annotations must not be null");
    Preconditions.checkNotNull(diffFromPrevious, "diffFromPrevious must not be null");
    if (start >= end) {
      throw new IllegalArgumentException("Attempt to set AnnotationInterval to zero length");
    }
    this.start = start;
    this.end = end;
    this.annotations = annotations;
    this.diffFromLeft = diffFromPrevious;
  }

  @Override
  public int start() {
    return start;
  }

  @Override
  public int end() {
    return end;
  }

  @Override
  public int length() {
    return end - start;
  }

  @Override
  public ReadableStringMap<V> annotations() {
    return annotations;
  }

  @Override
  public ReadableStringMap<V> diffFromLeft() {
    return diffFromLeft;
  }

  private String mapToString(ReadableStringMap<V> map) {
    final StringBuilder buf = new StringBuilder("{");
    final boolean first[] = new boolean[] { true };
    map.each(new ReadableStringMap.ProcV<V>() {
      @Override
      public void apply(String key, V value) {
        if (first[0]) {
          first[0] = false;
        } else {
          buf.append(", ");
        }
        buf.append(key + "=" + value);
      }
    });
    buf.append("}");
    return buf.toString();
  }

  @Override
  public String toString() {
    return "AnnotationIntervalImpl(" + start + ", " + end + ", " + mapToString(annotations)
        + ", " + mapToString(diffFromLeft) + ")";
  }
}
