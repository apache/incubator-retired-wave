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

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A normalizer for annotations.
 *
 * @param <T> the type of the value returned by this normalizer
 */
public final class AnnotationsNormalizer<T> implements EvaluatingDocOpCursor<T> {

  private static final class AnnotationChange {

    final String key;
    final String oldValue;
    final String newValue;

    AnnotationChange(String key, String oldValue, String newValue) {
      this.key = key;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

  }

  private static final class AnnotationChangeValues {

    final String oldValue;
    final String newValue;

    AnnotationChangeValues(String oldValue, String newValue) {
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

  }

  private final EvaluatingDocOpCursor<? extends T> target;

  // TODO(danilatos/alexmah): Use efficient StringMap/StringSet,
  // and sort on output.
  // Even better, optionally don't sort (indexed document doesn't need sorting for its use).

  private final Map<String, AnnotationChangeValues> annotationTracker =
      new TreeMap<String, AnnotationChangeValues>();
  private final Map<String, AnnotationChangeValues> annotationChanges =
      new TreeMap<String, AnnotationChangeValues>();

//  private final Set<String> ignores = new HashSet<String>();

  public AnnotationsNormalizer(EvaluatingDocOpCursor<? extends T> target) {
    this.target = target;
  }

  @Override
  public T finish() {
    flushAnnotations();
    return target.finish();
  }

  @Override
  public void retain(int itemCount) {
    if (itemCount > 0) {
      flushAnnotations();
      target.retain(itemCount);
    }
  }

  @Override
  public void characters(String chars) {
    if (!chars.isEmpty()) {
      flushAnnotations();
      target.characters(chars);
    }
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    flushAnnotations();
    target.elementStart(type, attrs);
  }

  @Override
  public void elementEnd() {
    flushAnnotations();
    target.elementEnd();
  }

  @Override
  public void deleteCharacters(String chars) {
    if (!chars.isEmpty()) {
      flushAnnotations();
      target.deleteCharacters(chars);
    }
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    flushAnnotations();
    target.deleteElementStart(type, attrs);
  }

  @Override
  public void deleteElementEnd() {
    flushAnnotations();
    target.deleteElementEnd();
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    flushAnnotations();
    target.replaceAttributes(oldAttrs, newAttrs);
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    flushAnnotations();
    target.updateAttributes(attrUpdate);
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
    int changeSize = map.changeSize();
    for (int i = 0; i < changeSize; ++i)  {
      startAnnotation(map.getChangeKey(i), map.getOldValue(i), map.getNewValue(i));
    }
    int endSize = map.endSize();
    for (int i = 0; i < endSize; ++i)  {
      endAnnotation(map.getEndKey(i));
    }
  }

  public void startAnnotation(String key, String oldValue, String newValue) {
    annotationChanges.put(key, new AnnotationChangeValues(oldValue, newValue));
  }

  public void endAnnotation(String key) {
    annotationChanges.put(key, null);
  }

  private void flushAnnotations() {
    final List<AnnotationChange> changes = new ArrayList<AnnotationChange>();
    final List<String> ends = new ArrayList<String>();
    for (Map.Entry<String, AnnotationChangeValues> change : annotationChanges.entrySet()) {
      String key = change.getKey();
      AnnotationChangeValues values = change.getValue();
      AnnotationChangeValues previousValues = annotationTracker.get(key);
      if (values == null) {
        if (previousValues != null) {
          annotationTracker.remove(key);
          ends.add(key);
        }
      } else {
        if (previousValues == null
            || !ValueUtils.equal(values.oldValue, previousValues.oldValue)
            || !ValueUtils.equal(values.newValue, previousValues.newValue)) {
          annotationTracker.put(key, values);
          changes.add(new AnnotationChange(key, values.oldValue, values.newValue));
        }
      }
    }
    if (!changes.isEmpty() || !ends.isEmpty()) {
      target.annotationBoundary(new AnnotationBoundaryMap() {

        @Override
        public int changeSize() {
          return changes.size();
        }

        @Override
        public String getChangeKey(int changeIndex) {
          return changes.get(changeIndex).key;
        }

        @Override
        public String getOldValue(int changeIndex) {
          return changes.get(changeIndex).oldValue;
        }

        @Override
        public String getNewValue(int changeIndex) {
          return changes.get(changeIndex).newValue;
        }

        @Override
        public int endSize() {
          return ends.size();
        }

        @Override
        public String getEndKey(int endIndex) {
          return ends.get(endIndex);
        }

      });
    }
    annotationChanges.clear();
  }

}
