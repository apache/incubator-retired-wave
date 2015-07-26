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

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilities for comparing documents.
 *
 * Mainly useful for testing - implementations are not necessarily efficient.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class DocCompare {

  /**
   * Document shape - presence of elements, size of text runs (still ignores
   * text node boundaries)
   */
  public static final int SHAPE = 1 << 0;

  /**
   * Element types (tag names)
   */
  public static final int TYPES = 1 << 1;

  /**
   * Element attribute names
   */
  public static final int ATTR_NAMES = 1 << 2;

  /**
   * Subtract this to avoid caring about attribute values, only care about names
   */
  public static final int ATTR_VALUES = 1 << 3;

  /**
   * Text data
   */
  public static final int TEXT = 1 << 4;

  /**
   * Annotation keys, caring only whether the corresponding values are null or not
   */
  public static final int ANNOTATION_KEYS = 1 << 5;

  /**
   * Subtract this to avoid caring about annotation values, only care about keys
   */
  private static final int ANNOTATION_VALUES = 1 << 6;

  /**
   * Element attribute names and values.
   */
  public static final int ATTRS = ATTR_NAMES | ATTR_VALUES;

  /**
   * All structure (still ignoring text node boundaries)
   */
  public static final int STRUCTURE = SHAPE | TYPES | ATTRS | TEXT;

  /**
   * Annotation keys and values
   */
  public static final int ANNOTATIONS = ANNOTATION_KEYS | ANNOTATION_VALUES;

  /**
   * Both structure and annotations
   */
  public static final int ALL = STRUCTURE | ANNOTATIONS;

  /**
   * Normalises a document view based on what structural information we do and
   * do not care about
   */
  private static class NormalisingView<N, E extends N, T extends N> extends IdentityView<N, E, T> {

    private final int flags;

    NormalisingView(ReadableDocument<N, E, T> inner, int flags) {
      super(inner);
      this.flags = flags;
    }

    @Override public String getTagName(E element) {
      return flag(TYPES) ? super.getTagName(element) : "x";
    }

    @Override public String getAttribute(E element, String name) {
      if (flag(ATTR_VALUES)) {
        return super.getAttribute(element, name);
      } else if (flag(ATTR_NAMES)) {
        return super.getAttribute(element, name) != null ? "x" : null;
      } else {
        return null;
      }
    }

    @Override public Map<String,String> getAttributes(E element) {
      if (flag(ATTR_VALUES)) {
        // Ensure consistent order of keys
        return new TreeMap<String, String>(super.getAttributes(element));
      } else if (flag(ATTR_NAMES)) {
        Map<String, String> copy = new TreeMap<String, String>();
        for (String key : super.getAttributes(element).keySet()) {
          copy.put(key, "x");
        }
        return copy;
      } else {
        return Collections.emptyMap();
      }
    }

    @Override public String getData(T textNode) {
      String data = super.getData(textNode);
      return flag(TEXT) ? data : CollectionUtils.repeat('x', data.length());
    }

    private boolean flag(int mask) {
      return (flags & mask) != 0;
    }
  }

  /**
   * Checks whether two documents are equivalent.
   *
   * @param flags specifies the aspects to compare for equivalence
   * @return true if the two documents are equivalent
   */
  public static <N1, N2> boolean equivalent(final int flags, ReadableWDocument<N1, ?, ?> doc1,
      ReadableWDocument<N2, ?, ?> doc2) {
    return fungeEquivalent(flags, doc1, doc2);
  }

  private static <N1, E1 extends N1, T1 extends N1,
                 N2, E2 extends N2, T2 extends N2> boolean fungeEquivalent(
      final int flags,
      ReadableWDocument<N1, E1, T1> doc1, ReadableWDocument<N2, E2, T2> doc2) {

    if ((flags & ANNOTATIONS) != 0) {
      throw new AssertionError("Annotations comparison not yet implemented");
    }

    return structureEquivalent(flags & ~ANNOTATIONS, doc1, doc2)
        ;// && annotationsEquivalent(flags & ~STRUCTURE, doc1, doc2);
  }

  /**
   * Checks whether the structures of two documents are equivalent.
   *
   * Note: the documents' annotations are currently ignored, but this behaviour
   * should not be relied upon.
   *
   * @param flags specifies the aspects to compare for equivalence
   */
  public static <N1, E1 extends N1, T1 extends N1,
                 N2, E2 extends N2, T2 extends N2> boolean structureEquivalent(
      final int flags,
      ReadableDocument<N1, E1, T1> doc1, ReadableDocument<N2, E2, T2> doc2) {

    checkValidFlags(flags);

    ReadableDocument<N1, E1, T1> view1 = new NormalisingView<N1, E1, T1>(doc1, flags);
    ReadableDocument<N2, E2, T2> view2 = new NormalisingView<N2, E2, T2>(doc2, flags);

    return XmlStringBuilder.innerXml(view1).toString().equals(
        XmlStringBuilder.innerXml(view2).toString());
  }

  /**
   * Checks whether a string is a representation of an equivalent document. The
   * string need not be normalized.
   *
   * Note: the documents' annotations are currently ignored, but this behaviour
   * should not be relied upon.
   *
   * @param str string for comparison
   * @param doc document for comparison
   * @param flags specifies the aspects to compare for equivalence
   * @return true if the string is a representation of an equivalent document
   */
  public static <N, E extends N, T extends N> boolean equivalent(
      int flags,
      String str, ReadableDocument<N, E, T> doc) {

    return structureEquivalent(flags, DocProviders.POJO.parse(str), doc);
  }

  /**
   * Checks whether two strings are representations of equivalent documents. The
   * strings need not be normalized.
   *
   * @param flags specifies the aspects to compare for equivalence
   * @return true if the strings represent equivalent documents, based on the
   *         types of things that are important as given by the flags
   */
  public static <N, E extends N, T extends N> boolean equivalent(int flags, String a, String b) {
    return structureEquivalent(flags, DocProviders.POJO.parse(a), DocProviders.POJO.parse(b));
  }

  /* TODO(danilatos)
  public static <V> boolean annotationsEquivalent(
    final int flags,
    ReadableAnnotationSet<V> doc1, ReadableAnnotationSet<V> doc2) {

    checkValidFlags(flags);
    Preconditions.checkArgument((flags & STRUCTURE) == 0,
        "ReadableAnnotationSet provides no access to structure");

    throw new AssertionError("annotationsEquivalent not implemented");
  }
  */

  private static void checkValidFlags(int flags) {
    Preconditions.checkArgument(flags != 0, "flags must be non-empty");
    Preconditions.checkArgument(implies(flags, TYPES, SHAPE), "flag TYPES must imply flag SHAPE");
    Preconditions.checkArgument(implies(flags, ATTR_NAMES, SHAPE),
        "flag ATTR_NAMES must imply flag SHAPE");
    Preconditions.checkArgument(implies(flags, TEXT, SHAPE), "flag TEXT must imply flag SHAPE");
    Preconditions.checkArgument(implies(flags, ATTR_VALUES, ATTR_NAMES),
        "flag ATTR_VALUES must imply flag ATTR_NAMES");
    Preconditions.checkArgument(implies(flags, ANNOTATION_VALUES, ANNOTATION_KEYS),
        "flag ANNOTATION_VALUES must imply flag ANNOTATION_KEYS");
  }

  /**
   * The presence of the antecedent implies the presence of the consequent in the
   * flags parameter
   * @return true if the implication holds
   */
  private static boolean implies(int flags, int antecedent, int consequent) {
    return (flags & antecedent) == 0 || (flags & consequent) != 0;
  }

  private DocCompare() { }
}
