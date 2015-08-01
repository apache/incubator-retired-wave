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

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.operation.OpComparators;

import java.util.Collections;

/**
 * Test cases for the DomOperationUtil helper methods.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class DomOperationUtilTest extends TestCase {
  /** Check that generating an op from a text node calls characters with the text's data. */
  public void testOperationFromText() {
    Bundle data = new Bundle();

    DocOpBuffer cursorA = new DocOpBuffer(), cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromTextNode(data.D, data.T.asText(), cursorA);
    cursorB.characters("child");

    checkCursors(cursorA, cursorB);
  }

  /** Check that generating an op from just an element calls element start & end. */
  public void testOperationFromElementNotRecursive() {
    Bundle data = new Bundle();
    DocOpBuffer cursorA = new DocOpBuffer(), cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromElement(data.D, data.A.asElement(), cursorA, false);
    cursorB.elementStart("a", Attributes.EMPTY_MAP);
    cursorB.elementEnd();

    checkCursors(cursorA, cursorB);

    // do likewise with B
    cursorA = new DocOpBuffer();
    cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromElement(data.D, data.B.asElement(), cursorA, false);
    cursorB.elementStart("b", new AttributesImpl(Collections.singletonMap("x", "y")));
    cursorB.elementEnd();

    checkCursors(cursorA, cursorB);
  }

  /** Check that generating an op from just an element calls element start & end. */
  public void testOperationFromElementRecursive() {
    Bundle data = new Bundle();
    DocOpBuffer cursorA = new DocOpBuffer(), cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromElement(data.D, data.A.asElement(), cursorA, true);
    cursorB.elementStart("a", Attributes.EMPTY_MAP);
    cursorB.characters("child");
    cursorB.elementStart("b", new AttributesImpl(Collections.singletonMap("x", "y")));
    cursorB.elementEnd();
    cursorB.elementEnd();

    checkCursors(cursorA, cursorB);
  }

  /** Check that passing to buildSubtree delegates correctly for elements and text. */
  public void testOperationFromSubtree() {
    Bundle data = new Bundle();

    // for element:
    DocOpBuffer cursorA = new DocOpBuffer(), cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromSubtree(data.D, data.A, cursorA);
    DomOperationUtil.buildDomInitializationFromElement(data.D, data.A.asElement(), cursorB, true);

    checkCursors(cursorA, cursorB);

    // for text:
    cursorA = new DocOpBuffer();
    cursorB = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromSubtree(data.D, data.T, cursorA);
    DomOperationUtil.buildDomInitializationFromTextNode(data.D, data.T.asText(), cursorB);

    checkCursors(cursorA, cursorB);
  }

  /** Asserts that two DocOpBuffers are equivalent. */
  private void checkCursors(DocOpBuffer actual, DocOpBuffer expected) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(actual.finish(), expected.finish()));
  }

  //
  // Utility to create simple tree
  //
  private static class Bundle {
    final RawDocumentImpl D;
    final Node A, B, T;
    public Bundle() {
      // Set up...
      D = DocProviders.ROJO.parse("<a>child<b x=\"y\"></b></a>");
      A = D.getDocumentElement();
      T = D.getFirstChild(A);
      B = D.getLastChild(A);
    }
  }
}
