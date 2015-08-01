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

import static org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil.clearAnnotationsOverRange;
import static org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil.getAnnotationOverRangeIfFull;
import static org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil.getFirstCoveringAnnotationOverRange;
import static org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil.setAnnotationOverRange;
import static org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil.supplementAnnotations;

import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations.AnnotationResolver;
import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * Tests the methods in EditorAnnotationUtilTest, paired up with the context and direct versions
 * tested at the same time.
 *
 * TODO(patcoleman): These tests should be properly split into large and small tests after this CL,
 * or alternatively those with buildContext could be made small, which requires nodelet-free
 * CMutableDocuments to be created. As the context methods call the pojo methods, it should be
 * safe to just test the pojo ones for now.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class EditorAnnotationUtilTest extends TestCase {
  // Parameters to make generating the EditorContext wrappers easier
  private int start;
  private int end;
  private MutableDocument<?, ?, ?> doc;
  private CaretAnnotations caret;

  // Resolve the annotations by left-biasing within the document, stores it in doc.
  AnnotationResolver annotationResolver = new AnnotationResolver() {
    public String getAnnotation(String key) {
      return start == 0 || doc.getAnnotation(start - 1, key) == null ?
          null : doc.getAnnotation(start - 1, key).toString();
    }
  };

  // Builds a fake editor context out of the state of the test.
  /*private EditorContext buildContext() {
    return new FakeEditorContext(doc, caret, "", new DocumentFreeSelectionHelper(start, end));
  }*/

  // Converts the document and annotations into a CMutableDocument and stores in doc
  private void useDocument(String docContent, String... annotations) {
    MutableDocument<?, ?, ?> mutable =
      DocProviders.MOJO.parse("<body><line></line>" + docContent + "</body>");

    // format: key:value:start:end
    for (String value : annotations) {
      String[] vals = value.split(":");
      mutable.setAnnotation(Integer.parseInt(vals[2]), Integer.parseInt(vals[3]), vals[0], vals[1]);
    }
    doc = mutable;
  }

  // Converts the annotations passed into a CaretAnnotations bundle and stores in caret
  private void useCaretAnnotations(String... annotations) {
    CaretAnnotations localCaret = new CaretAnnotations();
    for (String value : annotations) {
      String[] dup = value.split(":");
      localCaret.setAnnotation(dup[0], dup[1]);
    }
    caret = localCaret;
    caret.setAnnotationResolver(annotationResolver);
  }

  // Stores the start and end of the selection range to simulate.
  private void useSelection(int begin, int finish) {
    start = begin;
    end = finish;
  }

  ///
  /// tests begin (anything with buildContext() currently disabled)
  ///

  /** check getting first cover for (null, non-null) x (range, collapsed) */
  public void testGetFirstCover() {
    // cover the 'owd', offsets 4 to 7
    useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    useCaretAnnotations("W:w");
    useSelection(4, 7);

    // null return for range selection
    //assertNull(getFirstAnnotationOverSelection(buildContext(), "A", "B"));
    assertNull(getFirstCoveringAnnotationOverRange(doc, caret, new String[]{"A", "B"}, start, end));

    // non-null return for range selection
    //assertEquals(getFirstAnnotationOverSelection(buildContext(), "A", "X", "Y"), "x");
    //assertEquals("x", getFirstCoveringAnnotationOverRange(doc, caret, new String[]{"A", "X", "Y"},
    //    start, end));

    // collapse:
    useSelection(5, 5);

    // null for collapsed selection
    //assertNull(getFirstAnnotationOverSelection(buildContext(), "Q", "A"));
    assertNull(getFirstCoveringAnnotationOverRange(doc, caret, new String[]{"Q", "A"}, start, end));

    // non-null for collapsed selection, using direct caret annotation
    //assertEquals(getFirstAnnotationOverSelection(buildContext(), "W", "X", "Y"), "w");
    assertEquals("w", getFirstCoveringAnnotationOverRange(doc, caret, new String[]{"W", "X", "Y"},
        start, end));

    // non-null for collapsed selection, using annotation resolver
    //assertEquals(getFirstAnnotationOverSelection(buildContext(), "X", "Y", "W"), "x");
    assertEquals("x", getFirstCoveringAnnotationOverRange(doc, caret, new String[]{"X", "Y", "W"},
        start, end));
  }

  /** check getting an annotation cover for (null, non-null) x (range, collapsed) */
  public void testGetAnnotationIfFull() {
    // cover the 'owd', offsets 4 to 7
    useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    useCaretAnnotations("W:w");
    useSelection(4, 7);

    // null for range:
    //assertNull(getAnnotationOverSelectionIfFull(buildContext(), "A"));
    assertNull(getAnnotationOverRangeIfFull(doc, caret, "A", start, end));

    // non-null for range:
    //assertEquals(getAnnotationOverSelectionIfFull(buildContext(), "Y"), "y");
    assertEquals(getAnnotationOverRangeIfFull(doc, caret, "Y", start, end), "y");

    // collapse selection
    useSelection(5, 5);

    // null for collapsed selection
    //assertNull(getAnnotationOverSelectionIfFull(buildContext(), "$"));
    assertNull(getAnnotationOverRangeIfFull(doc, caret, "$", start, end));

    // non-null for collapsed selection, using direct annotation
    //assertEquals(getAnnotationOverSelectionIfFull(buildContext(), "W"), "w");
    assertEquals(getAnnotationOverRangeIfFull(doc, caret, "W", start, end), "w");

    // non-null for collapsed selection, using annotation resolver
    //assertEquals(getAnnotationOverSelectionIfFull(buildContext(), "Y"), "y");
    assertEquals(getAnnotationOverRangeIfFull(doc, caret, "Y", start, end), "y");
  }

  /** check setting an annotation for (null, non-null) x (range, collapsed) */
  public void testSetAnnotation() {
    // cover the 'owd', offsets 4 to 7
    useCaretAnnotations("W:w");
    useSelection(4, 7);

    // null over ranged selection
    //useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    //setAnnotationOverSelection(buildContext(), "Y", null);
    //assertNull(doc.getAnnotation(start, "Y"));
    //assertEquals(doc.firstAnnotationChange(start, end + 1, "Y", null), end);
    useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    setAnnotationOverRange(doc, caret, "Y", null, start, end);
    assertNull(doc.getAnnotation(start, "Y"));
    assertEquals(doc.firstAnnotationChange(start, end + 1, "Y", null), end);

    // non-null over ranged selection
    //useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    //setAnnotationOverSelection(buildContext(), "A", "a");
    //assertEquals(doc.getAnnotation(start, "A"), "a");
    //assertEquals(doc.firstAnnotationChange(start, end, "A", "a"), -1);
    useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    setAnnotationOverRange(doc, caret, "A", "a", start, end);
    assertEquals(doc.getAnnotation(start, "A"), "a");
    assertEquals(doc.firstAnnotationChange(start, end, "A", "a"), -1);

    // null over collapsed selection
    useSelection(5, 5);
    //useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    //setAnnotationOverSelection(buildContext(), "A", null);
    //assertTrue(caret.hasAnnotation("A"));
    //assertNull(caret.getAnnotation("A"));
    useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    setAnnotationOverRange(doc, caret, "A", null, start, end);
    assertTrue(caret.hasAnnotation("A"));
    assertNull(caret.getAnnotation("A"));

    // non-null over collapsed selection
    //useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    //setAnnotationOverSelection(buildContext(), "A", "!");
    //assertEquals(caret.getAnnotation("A"), "!");
    useDocument("howdy", "A:a:5:6", "X:x:4:7", "Y:y:3:8");
    setAnnotationOverRange(doc, caret, "A", "!", start, end);
    assertEquals(caret.getAnnotation("A"), "!");
  }

  /** check clearing annotations for (range, collapsed) */
  public void testClearAnnotations() {
    // cover the 'owd', offsets 4 to 7
    useCaretAnnotations("W:w");
    useSelection(4, 7);

    // clear ranged annotations
    //useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    //assertFalse(clearAnnotationsOverSelection(buildContext(), "?"));
    //assertTrue(clearAnnotationsOverSelection(buildContext(), "Q", "X", "Y"));
    //assertNull(doc.getAnnotation(start, "X"));
    //assertEquals(doc.firstAnnotationChange(start, end + 1, "Y", null), end);
    useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    assertFalse(clearAnnotationsOverRange(doc, caret, new String[]{"?"}, start, end));
    assertTrue(clearAnnotationsOverRange(doc, caret, new String[]{"Q", "X", "Y"}, start, end));
    assertNull(doc.getAnnotation(start, "X"));
    assertEquals(doc.firstAnnotationChange(start, end + 1, "Y", null), end);

    // clear over caret annotation
    useSelection(5, 5);
    //assertFalse(clearAnnotationsOverSelection(buildContext(), "P"));

    // clearing a direct annotation:
    useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    assertTrue(clearAnnotationsOverRange(doc, caret, new String[]{"Y"}, start, end));
    assertTrue(caret.hasAnnotation("Y"));
    assertNull(caret.getAnnotation("Y"));

    // clearing one that was already a caret annotation:
    useDocument("howdy", "A:a:5:7", "X:x:4:7", "Y:y:3:8");
    assertTrue(clearAnnotationsOverRange(doc, caret, new String[]{"W"}, start, end));
    assertTrue(caret.hasAnnotation("W"));
    assertNull(caret.getAnnotation("W"));
  }

  /** Check whether caret annotations are applied correctly. */
  public void testSupplementAnnotationsRightAligned() {
    // NOTE(patcoleman): currently, nothing happens on supplementing annotations from the left, as
    //   the behaviour of annotations is to do this anyway. If this behaviour changes, a test
    //   should be added to test left-alignment too.
    useDocument("howdy", "A:a:5:7", "X:x:2:8");
    useCaretAnnotations("X:?", "Y:y");

    // caret annotation already exists, don't supplement:
    StringSet keys = CollectionUtils.createStringSet();
    keys.add("X");
    supplementAnnotations(doc, caret, keys, 5, false);
    assertTrue(caret.hasAnnotation("X"));
    assertEquals("?", caret.getAnnotation("X")); // keeps the caret annotation
    assertTrue(caret.hasAnnotation("Y"));

    // subset of annotations are supplemented, make sure new value is right:
    caret.removeAnnotation("X");
    keys.add("A");
    supplementAnnotations(doc, caret, keys, 5, false);
    assertFalse(caret.hasAnnotation("X"));
    assertTrue(caret.hasAnnotation("A"));
    assertEquals("a", caret.getAnnotation("A"));
  }
}
