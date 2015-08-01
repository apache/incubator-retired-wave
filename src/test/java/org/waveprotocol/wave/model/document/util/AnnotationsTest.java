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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 * @author patcoleman@google.com (Pat Coleman)
 */

public class AnnotationsTest extends TestCase {

  /***/
  public void testAnnotationBoundaryHelpers() {
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "abcdefgh", null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();

    int s = doc.size();

    assertEquals(s, Annotations.firstAnnotationBoundary(doc, 0, s, "a", null));
    assertEquals(0, Annotations.lastAnnotationBoundary(doc, 0, s, "a", null));
    assertEquals(s - 1, Annotations.firstAnnotationBoundary(doc, 1, s - 1, "a", null));
    assertEquals(1, Annotations.lastAnnotationBoundary(doc, 1, s - 1, "a", null));
    assertEquals(0, Annotations.firstAnnotationBoundary(doc, 0, s, "a", "x"));
    assertEquals(s, Annotations.lastAnnotationBoundary(doc, 0, s, "a", "x"));
    assertEquals(1, Annotations.firstAnnotationBoundary(doc, 1, s - 1, "a", ""));
    assertEquals(s - 1, Annotations.lastAnnotationBoundary(doc, 1, s - 1, "a", ""));

    doc.setAnnotation(2, 3, "a", "");
    doc.setAnnotation(4, 5, "a", "");
    assertEquals(2, Annotations.firstAnnotationBoundary(doc, 0, s, "a", null));
    assertEquals(5, Annotations.lastAnnotationBoundary(doc, 0, s, "a", null));
    assertEquals(2, Annotations.firstAnnotationBoundary(doc, 1, s - 1, "a", null));
    assertEquals(5, Annotations.lastAnnotationBoundary(doc, 1, s - 1, "a", null));

    assertEquals(0, Annotations.firstAnnotationBoundary(doc, 0, s, "a", "x"));
    assertEquals(s, Annotations.lastAnnotationBoundary(doc, 0, s, "a", "x"));
    assertEquals(1, Annotations.firstAnnotationBoundary(doc, 1, s - 1, "a", "y"));
    assertEquals(s - 1, Annotations.lastAnnotationBoundary(doc, 1, s - 1, "a", "y"));
    assertEquals(0, Annotations.firstAnnotationBoundary(doc, 0, s, "a", ""));
    assertEquals(s, Annotations.lastAnnotationBoundary(doc, 0, s, "a", ""));
    assertEquals(1, Annotations.firstAnnotationBoundary(doc, 1, s - 1, "a", ""));
    assertEquals(s - 1, Annotations.lastAnnotationBoundary(doc, 1, s - 1, "a", ""));
  }

  /** Test whether the guard is correctly applied. */
  public void testGuardedResetAnnotation() {
    CheckListener check = new CheckListener();
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "abcdefgh", null, check, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();

    // check boundary conditions
    check.reset();
    assertFalse(Annotations.guardedResetAnnotation(doc, 0, 3, "a", null));
    assertFalse(Annotations.guardedResetAnnotation(doc, 3, doc.size(), "a", null));
    assertFalse(Annotations.guardedResetAnnotation(doc, 0, doc.size(), "a", null));
    assertFalse(check.wasChanged());

    // and again with non-null value
    doc.setAnnotation(0, doc.size(), "a", "b");
    check.reset();
    assertFalse(Annotations.guardedResetAnnotation(doc, 0, doc.size(), "a", "b"));
    assertFalse(check.wasChanged());

    // check on null annotation:
    doc.setAnnotation(0, doc.size(), "a", null);
    check.reset();
    assertTrue(Annotations.guardedResetAnnotation(doc, 3, 6, "a", "b"));
    assertTrue(check.wasChanged());

    // check not applied on repeat annotation:
    check.reset();
    assertFalse(Annotations.guardedResetAnnotation(doc, 3, 6, "a", "b"));
    assertFalse(check.wasChanged());

    // check applied when restricting cover:
    check.reset();
    assertTrue(Annotations.guardedResetAnnotation(doc, 4, 6, "a", "b"));
    assertTrue(check.wasChanged());

    // check applied when extending cover:
    check.reset();
    assertTrue(Annotations.guardedResetAnnotation(doc, 4, 7, "a", "b"));
    assertTrue(check.wasChanged());

    // check applied when doing both:
    check.reset();
    assertTrue(Annotations.guardedResetAnnotation(doc, 3, 6, "a", "b"));
    assertTrue(check.wasChanged());
  }

  /** Test whether left/right alignment of annotation retrieval is working. */
  public void testGetAlignedAnnotation() {
    // setup: ab[cdef]gh
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "abcdefgh", null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();
    doc.setAnnotation(2, 6, "X", "Y");

    // check left-align at start, middle, and end and after of non-null annotation
    assertEquals(Annotations.getAlignedAnnotation(doc, 2, "X", true), null);
    assertEquals(Annotations.getAlignedAnnotation(doc, 4, "X", true), "Y");
    assertEquals(Annotations.getAlignedAnnotation(doc, 6, "X", true), "Y");
    assertEquals(Annotations.getAlignedAnnotation(doc, 8, "X", true), null);

    // check right-align at start, middle, and end of non-null annotation
    assertEquals(Annotations.getAlignedAnnotation(doc, 0, "X", false), null);
    assertEquals(Annotations.getAlignedAnnotation(doc, 2, "X", false), "Y");
    assertEquals(Annotations.getAlignedAnnotation(doc, 4, "X", false), "Y");
    assertEquals(Annotations.getAlignedAnnotation(doc, 6, "X", false), null);

    // Boundary cases - left aligning at start, right aligning at end
    assertNull(Annotations.getAlignedAnnotation(doc, 0, "X", true));
    assertNull(Annotations.getAlignedAnnotation(doc, doc.size(), "X", false));
  }

  /** Utility to track range annotation calls. */
  public static class CheckListener implements AnnotationSetListener<Object> {
    private int lastChangeStart = -1;
    private int lastChangeEnd = -1;

    @Override
    public void onAnnotationChange(int start, int end, String key, Object newValue) {
      lastChangeStart = start;
      lastChangeEnd = end;
    }

    public void reset() {
      lastChangeStart = -1;
      lastChangeEnd = -1;
    }

    public boolean wasChanged() {
      return lastChangeStart != -1;
    }
  }
}
