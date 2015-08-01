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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.ValueUtils;

public class EditorAnnotationUtil {
  /** Un-constructable utility class. */
  private EditorAnnotationUtil() {}

  /**
   * Finds the first of the given keys that covers the entire selected range, and returns its value.
   *
   * @param editor Contains the non-null selection and document
   * @param keys Keys to look through
   * @return The first value annotation that covers the range at one of the given keys, else null.
   */
  public static String getFirstAnnotationOverSelection(EditorContext editor, String... keys) {
    Range range = Preconditions.checkNotNull(
        editor.getSelectionHelper().getSelectionRange(), "Editor must have selection").asRange();

    return getFirstCoveringAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(),
        keys, range.getStart(), range.getEnd());
  }

  /**
   * Looks through a list of annotation keys, finding the first to cover the given range,
   * and return its annotation value. If the range is collapsed, it's assumed that the
   * desired annotation set is stored in the caret parameter.
   *
   * @param doc Document to check for annotations.
   * @param caret Annotations at the current selection.
   * @param keys Keys to look through
   * @param start Start of range to check
   * @param end End of range to check
   * @return The first value annotation that covers the range at one of the given keys, else null.
   */
  public static String getFirstCoveringAnnotationOverRange(MutableAnnotationSet<String> doc,
      CaretAnnotations caret, String[] keys, int start, int end) {
    // iterate through each key:
    for (String key : keys) {
      String value = getAnnotationOverRangeIfFull(doc, caret, key, start, end);
      if (value != null) {
        return value;
      }
    }
    return null; // none found.
  }

  /**
   * Returns an annotation over the selected range only if the entire range has a single annotation.
   * If the annotation changes or if the range is not annotated (annotated with null), returns null.
   *
   * @param editor Editor whose annotations are to be checked, with non-null selection and doc.
   * @param key Key of annotation to retrieve
   */
  public static String getAnnotationOverSelectionIfFull(EditorContext editor, String key) {
    Range range = Preconditions.checkNotNull(
        editor.getSelectionHelper().getSelectionRange(), "Editor must have selection").asRange();

    return getAnnotationOverRangeIfFull(editor.getDocument(), editor.getCaretAnnotations(),
        key, range.getStart(), range.getEnd());
  }

  /**
   * Returns an annotation over a range only if the entire range has a single annotation.
   * If the annotation changes or if the range is not annotated (annotated with null), returns null.
   * If the range is collapsed it's assumed that the desired annotation set is in the caret param.
   *
   * @param doc Document to check for annotations.
   * @param caret Annotations at the current selection.
   * @param key Key of annotation to retrieve
   * @param start Start offset of range.
   * @param end End offset of range.
   */
  public static String getAnnotationOverRangeIfFull(MutableAnnotationSet<String> doc,
      CaretAnnotations caret, String key, int start, int end) {
    if (start == end) {
      // Try to use the information about the cursor, even if it doesn't match
      // where the selection is.
      return caret.getAnnotation(key);
    }

    String currentValue = doc.getAnnotation(start, key);
    if (doc.firstAnnotationChange(start, end, key, currentValue) == -1) {
      // no change, fully annotated
      return currentValue;
    }

    // change is found, so return:
    return null;
  }

  /**
   * Sets the annotation key to a particular value over the entire selected range in an editor.
   *
   * @param editor Editor to set the annotation, with non-null selection and doc.
   * @param key Annotation key to set.
   * @param value Annotation value to set key to.
   */
  public static void setAnnotationOverSelection(EditorContext editor, String key, String value) {
    Range range = Preconditions.checkNotNull(
        editor.getSelectionHelper().getSelectionRange(), "Editor must have selection").asRange();

    setAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(),
        key, value, range.getStart(), range.getEnd());
  }

  /**
   * Sets the annotation key to a particular value over an entire range.
   * If the range is collapsed it's assumed that the desired annotation set is in the caret param.
   *
   * @param doc Document to set the annotation in.
   * @param caret Collapsed-range annotations.
   * @param key key of annotation to set.
   * @param value value to set annotation to.
   * @param start start of range to set over.
   * @param end end of range to set over.
   */
  public static void setAnnotationOverRange(MutableAnnotationSet<String> doc,
      CaretAnnotations caret, String key, String value, int start, int end) {
    // simple switch depending on whether the range is collapsed:
    if (start == end) {
      caret.setAnnotation(key, value);
    } else {
      doc.setAnnotation(start, end, key, value);
    }
  }

  /**
   * Clears all annotations for a set of keys over the current selected range.
   *
   * @param editor Editor whose annotations are to be cleared, with non-null selection and doc.
   * @param keys List of annotation keys to clear
   * @return true if annotations were actually changed
   */
  public static boolean clearAnnotationsOverSelection(EditorContext editor, String... keys) {
    Range range = Preconditions.checkNotNull(
        editor.getSelectionHelper().getSelectionRange(), "Editor must have selection").asRange();

    return clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(),
        keys, range.getStart(), range.getEnd());
  }

  /**
   * Clears all annotations over a particular range in the editor's document.
   * If the range is collapsed it's assumed that the desired annotation set is in the caret param.
   *
   * @param doc Document to check for annotations.
   * @param caret Annotations at the current collapsed range.
   * @param keys List of annotation keys to clear
   * @param start Start offset of range.
   * @param end End offset of range.
   * @return true if annotations were actually changed
   */
  public static boolean clearAnnotationsOverRange(MutableAnnotationSet<String> doc,
      CaretAnnotations caret, String[] keys, int start, int end) {
    boolean wasRemoved = false;

    if (start == end) {
      // clear from caret annotation if collapsed range
      for (String key : keys) {
        if (caret.getAnnotation(key) != null) {
          caret.setAnnotation(key, null); // remove if present
          wasRemoved = true;
        }
      }
    } else {
      // clear from the entire range
      for (String key : keys) {
        if (doc.firstAnnotationChange(start, end, key, null) != -1) {
          doc.setAnnotation(start, end, key, null); // remove if present
          wasRemoved = true;
        }
      }
    }
    return wasRemoved;
  }

  /**
   * Finds the range of an adjacent or containing non-null range of contiguous
   * value for a given annotation key.
   *
   * If there are two ranges (the given location being at their boundary), then
   * prefer the one to the right.
   *
   * If there are no ranges (the key is null on either side of the location),
   * null is returned.
   *
   * @param doc
   * @param key
   * @param location
   * @return the range, or null if none found
   */
  public static <V> Range getEncompassingAnnotationRange(
      final ReadableAnnotationSet<V> doc, String key, int location) {

    V value = doc.getAnnotation(location, key);
    if (value == null && location > 0) {
      value = doc.getAnnotation(location - 1, key);
    }

    if (value == null) {
      return null;
    }

    int start = doc.lastAnnotationChange(0, location, key, value);
    int end = doc.firstAnnotationChange(location, doc.size(), key, value);

    assert start < end : "Range should not be collapsed";

    return new Range(start, end);
  }

  /**
   * Given the editor state, this examines the current caret annotations and adds any that
   * can be inferred from the position, given the alignment type.
   *
   * @param doc Document to check for annotations.
   * @param caret Current annotation styles at the caret.
   * @param keys Keys to supplement over the caret styles.
   * @param location Location of the caret in the document.
   * @param leftAlign Whether the annotations come from the left or right.
   */
  public static void supplementAnnotations(final MutableAnnotationSet<String> doc,
      final CaretAnnotations caret, final ReadableStringSet keys, final int location,
      final boolean leftAlign) {
    // by default, everything inherits from the left, so for now, no need!
    if (leftAlign) {
      return;
    }

    // supplement anything that's missing and different:
    keys.each(new Proc() {
      @Override
      public void apply(String key) {
        if (!caret.hasAnnotation(key)) {
          String newValue = Annotations.getAlignedAnnotation(doc, location, key, leftAlign);
          String oldValue = doc.getAnnotation(location - 1, key);
          if (!ValueUtils.equal(newValue, oldValue)) {
            caret.setAnnotation(key, newValue);
          }
        }
      }
    });
  }
}
