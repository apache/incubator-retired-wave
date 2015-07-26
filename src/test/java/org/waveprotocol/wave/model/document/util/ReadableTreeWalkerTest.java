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

import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;

import java.util.Collections;

/**
 * Test cases for the ReadableTreeWalker
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class ReadableTreeWalkerTest extends TestCase {
  /** Check walking an entire document correctly. */
  public void testWalkEntireDocument() {
    Bundle data = new Bundle();
    assertEquals(data.A, data.walker.checkElement("a", Collections.<String, String>emptyMap()));
    assertEquals(data.T, data.walker.checkTextNode("child"));
    assertTrue(data.walker.checkComplete());
  }

  /** Check that walking a subtree correctly works. */
  public void testWalkSubtree() {
    RawDocumentImpl doc;
    Node A, B, C, S, T;
    ReadableTreeWalker<Node, Element, Text> walker;
    doc = DocProviders.ROJO.parse("<a><b><c x=\"y\"></c>sub</b>child</a>");
    A = doc.getDocumentElement();
    B = doc.getFirstChild(A);
    C = doc.getFirstChild(B);
    S = doc.getLastChild(B);
    T = doc.getLastChild(A);
    walker = new ReadableTreeWalker<Node, Element, Text>(doc, B);

    // walk!
    assertEquals(B, walker.checkElement("b", Collections.<String, String>emptyMap()));
    assertEquals(C, walker.checkElement("c", Collections.singletonMap("x", "y")));
    assertEquals(S, walker.checkTextNode("sub"));
    assertTrue(walker.checkComplete());
  }

  /** Make sure assertions fail when text/element received when the other is expected. */
  public void testIncorrectStructure() {
    // fails state check , expecting element rather than text
    Bundle data = new Bundle();
    try {
      assertEquals(data.A, data.walker.checkTextNode("need element"));
      fail();
    } catch (IllegalStateException e) {}

    // fails state check , expecting text rather than element
    data = new Bundle();
    assertEquals(data.A, data.walker.checkElement("a", Collections.<String, String>emptyMap()));
    try {
      assertEquals(data.A, data.walker.checkElement("b", Collections.<String, String>emptyMap()));
      fail();
    } catch (IllegalStateException e) {}
  }

  /** Make sure assertions checking the element/text contents fail when incorrect. */
  public void testIncorrectValues() {
    // fails state check  with wrong tag name
    Bundle data = new Bundle();
    try {
      assertEquals(data.A, data.walker.checkElement("b", Collections.<String, String>emptyMap()));
      fail();
    } catch (IllegalStateException e) {}

    // fails state check  with wrong attributes
    data = new Bundle();
    try {
      assertEquals(data.A, data.walker.checkElement("a", Collections.singletonMap("x", "y")));
      fail();
    } catch (IllegalStateException e) {}

    // fails state check with wrong text data
    data = new Bundle();
    try {
      assertEquals(data.A, data.walker.checkElement("a", Collections.<String, String>emptyMap()));
      assertEquals(data.T, data.walker.checkTextNode("wrong data!"));
      fail();
    } catch (IllegalStateException e) {}
  }

  /** Make sure assertion fails when the walk leaves the subtree. */
  public void testWalkTooFar() {
    Bundle data = new Bundle();

    // ...and walk
    assertEquals(data.A, data.walker.checkElement("a", Collections.<String, String>emptyMap()));
    assertEquals(data.T, data.walker.checkTextNode("child"));
    try {
      data.walker.checkTextNode("should fail");
      fail();
    } catch (IllegalStateException e) {}
  }

  /** Make sure assertion fails at end of walk when subtree isn't finished. */
  public void testWalkTooShort() {
    Bundle data = new Bundle();

    // Walk just the element then try to stop
    assertEquals(data.A, data.walker.checkElement("a", Collections.<String, String>emptyMap()));
    try {
      assertFalse(data.walker.checkComplete()); // should throw exception
      fail();
    } catch (IllegalStateException e) {}
  }

  /** Make sure that a walk over the same subtree passes. */
  public void testIdenticalTreeWalkPasses() {
    Bundle data = new Bundle();
    assertTrue(data.walker.checkWalk(data.doc, data.A));

    // do the same, but with an isomorphic tree instead of the actual same tree.
    data = new Bundle();
    RawDocumentImpl doc = DocProviders.ROJO.parse("<r><a>child</a></r>");
    assertTrue(data.walker.checkWalk(doc, doc.getDocumentElement().getFirstChild()));
  }

  /** Make sure that a walk over a non-identical tree fails. */
  public void testDifferentTreeWalkFails() {
    Bundle data = new Bundle();
    RawDocumentImpl doc = DocProviders.ROJO.parse("<a>child<b></b></a>");
    try {
      assertFalse(data.walker.checkWalk(doc, doc.getDocumentElement()));
    } catch (IllegalStateException e) {}
  }

  //
  // Utility to create simple tree
  //
  private static class Bundle {
    final RawDocumentImpl doc;
    final Node A, T;
    final ReadableTreeWalker<Node, Element, Text> walker;
    public Bundle() {
      // Set up...
      doc = DocProviders.ROJO.parse("<a>child</a>");
      A = doc.getDocumentElement();
      T = doc.getFirstChild(A);
      walker = new ReadableTreeWalker<Node, Element, Text>(doc, A);
    }
  }
}
