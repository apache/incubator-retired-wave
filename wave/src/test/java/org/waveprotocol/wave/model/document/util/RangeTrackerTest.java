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
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;

/**
 * Tests RangeTracker
 *
 *
 */

public class RangeTrackerTest extends TestCase {
  /**
   * Basic sanity test.
   */
  public void testTrackRange() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker rangeTracker = new RangeTracker(test.localAnnotations());

    assertNull(rangeTracker.getRange());

    rangeTracker.trackRange(new Range(1, 2));
    assertEquals(new Range(1, 2), rangeTracker.getRange());

    doc.insertText(doc.locate(0), "012");
    assertEquals(new Range(4, 5), rangeTracker.getRange());

    doc.insertText(doc.locate(4), "a");
    assertEquals(new Range(5, 6), rangeTracker.getRange());

    doc.insertText(doc.locate(6), "a");
    assertEquals(new Range(5, 7), rangeTracker.getRange());

    rangeTracker.clearRange();
    assertNull(rangeTracker.getRange());
  }

  public void testRangesWithSameDebugIdDontCollide() {
    String initialContent = "hello world";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker trackerA = new RangeTracker(test.localAnnotations(), "debugId");
    RangeTracker trackerB = new RangeTracker(test.localAnnotations(), "debugId");

    trackerA.trackRange(new Range(1, 2));
    trackerB.trackRange(new Range(0, 4));

    assertEquals(new Range(1, 2), trackerA.getRange());
    assertEquals(new Range(0, 4), trackerB.getRange());

    doc.insertText(doc.locate(3), "a");
    assertEquals(new Range(1, 2), trackerA.getRange());
    assertEquals(new Range(0, 5), trackerB.getRange());

    trackerA.clearRange();
    assertEquals(null, trackerA.getRange());
    assertEquals(new Range(0, 5), trackerB.getRange());
  }

  /** Check that a collapsed range in the middle behaves correctly. */
  public void testCollapsedRange() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker tracker = new RangeTracker(test.localAnnotations());

    // collapsed at the start, through mutation:
    tracker.trackRange(new Range(2, 2));
    assertEquals(new Range(2, 2), tracker.getRange());

    doc.insertText(doc.locate(0), "a");
    assertEquals(new Range(3, 3), tracker.getRange());

    doc.insertText(doc.locate(4), "b");
    assertEquals(new Range(3, 3), tracker.getRange());

    doc.insertText(doc.locate(3), "c");
    assertEquals(new Range(4, 4), tracker.getRange());
  }

  /** Check that a collapsed range at 0 behaves correctly. */
  public void testCollapsedStartRange() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker tracker = new RangeTracker(test.localAnnotations());

    // collapsed at the start, through mutation:
    tracker.trackRange(new Range(0, 0));
    assertEquals(new Range(0, 0), tracker.getRange());

    doc.insertText(doc.locate(3), "a");
    assertEquals(new Range(0, 0), tracker.getRange());

    doc.insertText(doc.locate(0), "x");
    assertEquals(new Range(1, 1), tracker.getRange());
  }

  /** Check that a collapsed range at doc.size() behaves correctly. */
  public void testCollapsedEndRange() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker tracker = new RangeTracker(test.localAnnotations());

    int end = doc.size();

    // collapsed at the start, through mutation:
    tracker.trackRange(new Range(end, end));
    assertEquals(new Range(end, end), tracker.getRange());

    doc.insertText(doc.locate(3), "a");
    assertEquals(new Range(end + 1, end + 1), tracker.getRange());

    doc.insertText(doc.locate(doc.size()), "x");
    assertEquals(new Range(end + 2, end + 2), tracker.getRange());
  }

  /** Check that a collapsed range over the document behaves correctly. */
  public void testEntireCoverRanges() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    MutableDocument<Node, Element, Text> doc = test.document();
    RangeTracker tracker = new RangeTracker(test.localAnnotations());

    // entire range through mutation at middle, end, start
    tracker.trackRange(new Range(0, doc.size()));
    assertEquals(new Range(0, doc.size()), tracker.getRange());

    doc.insertText(doc.locate(1), "a");
    assertEquals(new Range(0, doc.size()), tracker.getRange());

    doc.insertText(doc.locate(doc.size()), "b");
    assertEquals(new Range(0, doc.size()), tracker.getRange());

    doc.insertText(doc.locate(0), "c");
    assertEquals(new Range(1, doc.size()), tracker.getRange());
  }

  public void testOrdering() {
    String initialContent = "abc";
    TestDocumentContext<Node, Element, Text> test =
        ContextProviders.createTestPojoContext(
            DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
                DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    RangeTracker tracker = new RangeTracker(test.localAnnotations());
    tracker.trackRange(new FocusedRange(2, 1));
    assertEquals(new Range(1, 2), tracker.getRange());
    assertEquals(new FocusedRange(2, 1), tracker.getFocusedRange());
  }
}
