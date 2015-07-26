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

package org.waveprotocol.wave.client.editor.extract;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.Nindo.Builder;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.RangedAnnotationImpl;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles various parts of annotation logic in terms of behaviour during paste operations.
 * This comprimises the following abilities:
 *   - strip/unstrip annotation keys, given a current document state and position
 *       where a content will be inserted by a paste.
 *   - normalising annotations within a section of the document
 *
 */
public class PasteAnnotationLogic<N, E extends N, T extends N> {
  // Document where annotations are to be extracted.
  private final ReadableWDocument<N, E, T> doc;

  // All annotation logic
  private final AnnotationRegistry annotationLogic;

  /**
   * Constructs an AnnotationExtractor where normalized annotations can be
   * extracted.
   *
   * @param doc
   */
  public PasteAnnotationLogic(ReadableWDocument<N, E, T> doc, AnnotationRegistry annotationLogic) {
    this.doc = doc;
    this.annotationLogic = annotationLogic;
  }

  // Logic for stripping keys for content about to be pasted

  /**
   * Strip keys at the start of a paste event, based on annotation behaviour defined.
   * Also returns the set of changed keys so they can be closed at the end of the paste.
   *
   * @param annotationSet Known annotations
   * @param position Where in the document the content is being inserted
   * @param cursorBias Current cursor bias
   * @param type Type of content being inserted
   * @param builder Nindo builder for the annotation ops to be sent to.
   * @return key->value map for all annotations whose inherited value isn't what is currently used.
   */
  public StringMap<String> stripKeys(final ReadableAnnotationSet<String> annotationSet,
      final int position, final BiasDirection cursorBias,
      final ContentType type, final Builder builder) {
    // NOTE(patcoleman): currently all that is used is type = RICH_TEXT
    // when PLAIN_TEXT is hooked up, please make sure it's well tested.
    // Likewise, with cursorBias = RIGHT, as all that's used & tested so far is cursorBias = LEFT

    // calculate annotation maps before and after
    final StringMap<Object> before = CollectionUtils.createStringMap();
    final StringMap<Object> after = CollectionUtils.createStringMap();
    annotationSet.knownKeys().each(new Proc() {
      @Override
      public void apply(String key) {
        if (position > 0) {
          Object beforeV = annotationSet.getAnnotation(position - 1, key);
          if (beforeV != null) {
            before.put(key, beforeV);
          }
        }
        if (position< annotationSet.size()) {
          Object afterV = annotationSet.getAnnotation(position, key);
          if (afterV != null) {
            after.put(key, afterV);
          }
        }
      }
    });

    // assign them to inside/outside the cursor based on bias:
    final StringMap<Object> inside = (cursorBias == BiasDirection.RIGHT ? after : before);
    final StringMap<Object> outside = (cursorBias == BiasDirection.RIGHT ? before : after);

    final StringMap<String> changedAnnotations = CollectionUtils.createStringMap();
    annotationSet.knownKeys().each(new Proc() {
      @Override
      public void apply(String key) {
        interpretReplace(key, type, builder, inside, outside, before, changedAnnotations);
      }
    });
    return changedAnnotations;
  }

  /** Mirror of stripKeys, called at the end of mutation to close annotations. */
  public void unstripKeys(final Builder builder, final ReadableStringSet stripKeys,
      final ReadableStringSet ignoreSet) {
    // NOTE(patcoleman) - maybe worth adding a set minus here. stripKeys.subtract(ignoreSet).each(
    stripKeys.each(new Proc() {
      @Override
      public void apply(String element) {
        if (!ignoreSet.contains(element)) {
          builder.endAnnotation(element);
        }
      }});
  }

  /** Interpret a replacement based on the annotation behaviour, return true if it was set. */
  private boolean interpretReplace(String key, ContentType type, Builder builder,
      StringMap<Object> inside, StringMap<Object> outside, StringMap<Object> current,
      StringMap<String> changeCollector) {
    AnnotationBehaviour logic = annotationLogic.getClosestBehaviour(key);
    if (logic != null) {
      switch (logic.replace(inside, outside, type)) {
        case INSIDE :
          return safeSet(builder, key, inside.get(key), current.get(key), changeCollector);
        case OUTSIDE :
          return safeSet(builder, key, outside.get(key), current.get(key), changeCollector);
        case NEITHER :
          return safeSet(builder, key, null, current.get(key), changeCollector);
      }
    }
    return false;
  }

  /** Utility to set an annotation key if not already set, returns true if it is set. */
  private boolean safeSet(Builder builder, String key, Object newValue, Object oldValue,
      StringMap<String> changeCollector) {
    String newString = newValue == null ? null : newValue.toString();
    String oldString = oldValue == null ? null : oldValue.toString();
    if (ValueUtils.notEqual(newString, oldString)) {
      builder.startAnnotation(key, newString);
      changeCollector.put(key, newString);
      return true;
    } else {
      return false;
    }
  }

  // Logic for normalizations of annotations in content about to be pasted

  /**
   * Extract and normalizes annotations inside a given range for the associated
   * document.
   *
   * By normalize, the position starting at the given range, will be normalized
   * to position 0, and the annotations are bounded by the size of the range.
   *
   * @param normalizedStart
   * @param normalizedEnd
   */
  public List<RangedAnnotation<String>> extractNormalizedAnnotation(Point<N> normalizedStart,
      Point<N> normalizedEnd) {
    int start = doc.getLocation(normalizedStart);
    int end = doc.getLocation(normalizedEnd);
    ReadableStringSet interested = filterContentAnnotations(doc.knownKeys());

    return trimAnnotations(doc.rangedAnnotations(start, end, interested), start, end - start);
  }

  /**
   * Normalizes the annotations in the range (offset, offset + length) to the
   * range (0, length)
   *
   * @param rangedAnnotations
   * @param offset
   * @param length
   */
  private static List<RangedAnnotation<String>> trimAnnotations(
      Iterable<RangedAnnotation<String>> rangedAnnotations, int offset, int length) {
    List<RangedAnnotation<String>> ret = new ArrayList<RangedAnnotation<String>>();

    for (RangedAnnotation<String> ann : rangedAnnotations) {
      int nStart = Math.max(0, ann.start() - offset);
      int nEnd = Math.min(ann.end() - offset, length);
      ret.add(new RangedAnnotationImpl<String>(ann.key(), ann.value(), nStart, nEnd));
    }
    return ret;
  }

  /**
   * Given a set of keys, return a subset that starts with prefixes in the
   * whitelist.
   *
   * @param known
   */
  private ReadableStringSet filterContentAnnotations(ReadableStringSet known) {
    final StringSet interested = CollectionUtils.createStringSet();
    known.each(new Proc() {
      @Override
      public void apply(final String key) {
        AnnotationBehaviour behaviour = annotationLogic.getClosestBehaviour(key);
        if (behaviour != null && behaviour.getAnnotationFamily() == AnnotationFamily.CONTENT) {
          interested.add(key);
        }
      }
    });
    return interested;
  }
}
