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

import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations.AnnotationResolver;
import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.AnnotationRegistryImpl;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.util.StringMap;

/** Check a number of different inheritence cases. */

public class AnnotationBehaviourLogicTest extends TestCase {
  private AnnotationRegistry REGISTRY = null;
  private static final String LINK_KEY = "link";
  private static final String STYLE_KEY = "style";

  /** Sets up a state with anything needed by all the tests. */
  private AnnotationBehaviourLogic<Node> initDefault() {
    LineContainers.setTopLevelContainerTagname("body");
    /**
     * Create a registry that has two types of behaviours:
     *   - style-like (no bias change, inherits inside)
     *   - link-like (bias away, with priority overriding containers).
     */
    REGISTRY = AnnotationRegistryImpl.ROOT;
    REGISTRY.registerBehaviour(STYLE_KEY, new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
    REGISTRY.registerBehaviour(LINK_KEY, new AnnotationBehaviour() {
      public BiasDirection getBias(StringMap<Object> left, StringMap<Object> right,
          CursorDirection cursor) {
        assert left.containsKey(LINK_KEY) && right.containsKey(LINK_KEY);
        return left.get(LINK_KEY) == null ? BiasDirection.LEFT : BiasDirection.RIGHT; // away
      }
      public double getPriority() {
        return 10.0; // higher than default
      }
      public InheritDirection replace(StringMap<Object> inside, StringMap<Object> outside,
          ContentType type) {
        return InheritDirection.INSIDE;
      }
      public AnnotationFamily getAnnotationFamily() {
        return AnnotationFamily.CONTENT;
      }
    });

    /**
     * Create document with content:     key: [] = link annotation, {} = style
     * <line/>l[ink] blank {style}
     * <line/>
     * <line/>[{both}]
     * <line/>normal
     */
    useDocument("<line/>link blank style<line/><line/>both<line/>normal",
        "link:L:4:7", "style:S:14:19", "link:L:23:27", "style:S:23:27");
    useCaretAnnotations();
    return new AnnotationBehaviourLogic<Node>(REGISTRY, doc, caret);
  }

  /** Makes sure ranged rebias is always right. */
  public void testRangedRebias() {
    AnnotationBehaviourLogic<Node> logic = initDefault();
    useSelection(4, 7);
    for (CursorDirection dir : CursorDirection.values()) {
      assertEquals(BiasDirection.RIGHT, logic.rebias(start, end, dir));
    }
  }

  /** Test collapsed start/end of line, no annotations, bias inwards. */
  public void testLineBoundaryRebias() {
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // start
    useSelection(29, 29);
    for (CursorDirection dir : CursorDirection.values()) { // always bias right
      assertEquals(BiasDirection.RIGHT, logic.rebias(start, end, dir));
    }

    // end
    useSelection(35, 35);
    for (CursorDirection dir : CursorDirection.values()) { // always bias left
      assertEquals(BiasDirection.LEFT, logic.rebias(start, end, dir));
    }

    // both
    useSelection(21, 21);
    for (CursorDirection dir : CursorDirection.values()) { // don't change bias
      assertEquals(CursorDirection.toBiasDirection(dir), logic.rebias(start, end, dir));
    }
  }

  /** Test collapsed start/end of link, middle of a line. */
  public void testLinkOutwardsBias() {
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // start of link
    useSelection(4, 4);
    for (CursorDirection dir : CursorDirection.values()) { // always bias left
      assertEquals(BiasDirection.LEFT, logic.rebias(start, end, dir));
    }

    // middle = nothing
    useSelection(5, 5);
    for (CursorDirection dir : CursorDirection.values()) { // don't change
      assertEquals(CursorDirection.toBiasDirection(dir), logic.rebias(start, end, dir));
    }

    // end
    useSelection(7, 7);
    for (CursorDirection dir : CursorDirection.values()) { // always bias right
      assertEquals(BiasDirection.RIGHT, logic.rebias(start, end, dir));
    }
  }

  /** Priority checking case: test collapsed, start of link, end of line. */
  public void testLineLinkPriority() {
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // start of line and link
    useSelection(23, 23);
    for (CursorDirection dir : CursorDirection.values()) { // always bias left - link takes priority
      assertEquals(BiasDirection.LEFT, logic.rebias(start, end, dir));
    }
  }

  /** Test annotation supplementing when wholly outside/inside a ranged annotation. */
  public void testSupplementNotAtBoundary() {
    // desired behaviour: no supplementing when wholly inside or outside an annotation
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // outside:
    useSelection(10, 10);
    for (BiasDirection dir : BiasDirection.values()) { // never inherit
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertFalse(caret.hasAnnotation(LINK_KEY));
    }

    // inside:
    useSelection(5, 5);
    for (BiasDirection dir : BiasDirection.values()) { // never inherit
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertFalse(caret.hasAnnotation(LINK_KEY));
    }
  }

  /** Test supplementing style-like annotations at start/end with both bias. */
  public void testSupplementStyle() {
    // desired behaviour: only changes caret annotations when right biased, either end
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // start:
    useSelection(14, 14);
    for (BiasDirection dir : BiasDirection.values()) { // only inherit when right biased.
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertEquals(dir == BiasDirection.RIGHT, caret.hasAnnotation(STYLE_KEY));
      caret.removeAnnotation(STYLE_KEY);
    }

    // end:
    useSelection(19, 19);
    for (BiasDirection dir : BiasDirection.values()) { // only inherit when right biased.
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertEquals(dir == BiasDirection.RIGHT, caret.hasAnnotation(STYLE_KEY));
      caret.removeAnnotation(STYLE_KEY);
    }
  }

  /** Test supplementing link-like annotations at start/end with both bias. */
  public void testSupplementLink() {
    // desired behaviour: same as styles
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // start:
    useSelection(4, 4);
    for (BiasDirection dir : BiasDirection.values()) {
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertEquals(dir == BiasDirection.RIGHT, caret.hasAnnotation(LINK_KEY));
      caret.removeAnnotation(LINK_KEY);
    }

    // end:
    useSelection(7, 7);
    for (BiasDirection dir : BiasDirection.values()) {
      logic.supplementAnnotations(start, dir, ContentType.PLAIN_TEXT);
      assertEquals(dir == BiasDirection.RIGHT, caret.hasAnnotation(LINK_KEY));
      caret.removeAnnotation(LINK_KEY);
    }
  }

  /** Test that supplementing doesn't clobber over caret annotations. */
  public void testSupplementCaret() {
    // desired behaviour: same as styles
    AnnotationBehaviourLogic<Node> logic = initDefault();

    // make sure it tries to add when none exists:
    useSelection(4, 4);
    logic.supplementAnnotations(start, BiasDirection.RIGHT, ContentType.PLAIN_TEXT);
    assertEquals("L", caret.getAnnotation(LINK_KEY)); // supplemented!

    // change and try again
    caret.setAnnotation(LINK_KEY, "different");
    logic.supplementAnnotations(start, BiasDirection.RIGHT, ContentType.PLAIN_TEXT);
    assertEquals("different", caret.getAnnotation(LINK_KEY)); // left alone
  }

  ///
  /// Convenience methods for a context, copied from EditorAnnotationUtilTest
  ///

  // Parameters to make generating the EditorContext wrappers easier
  private int start;
  private int end;
  private MutableDocument<Node, ?, ?> doc;
  private CaretAnnotations caret;

  // Resolve the annotations by left-biasing within the document, stores it in doc.
  AnnotationResolver annotationResolver = new AnnotationResolver() {
    public String getAnnotation(String key) {
      return start == 0 || doc.getAnnotation(start - 1, key) == null ?
          null : doc.getAnnotation(start - 1, key).toString();
    }
  };

  // Converts the document and annotations into a CMutableDocument and stores in doc
  private void useDocument(String docContent, String... annotations) {
    MutableDocument<Node, ?, ?> mutable =
      DocProviders.MOJO.parse("<body>" + docContent + "</body>");

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
}
