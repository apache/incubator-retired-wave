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

package org.waveprotocol.wave.client.wavepanel.impl.edit;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

import java.util.Collections;

/**
 * Tests for {@link DocumentUtil}.
 */
public class DocumentUtilTest extends TestCase {
  {
    LineContainers.setTopLevelContainerTagname("body");
  }
  TestDocumentContext<Node, Element, Text> cxt;
  MutableDocument<Node, Element, Text> doc;

  private class TestState {
    {
      getDocWithoutSchema("<body><line id=\"alpha\"/>alpha<line id=\"beta\"/>beta"
          + "<line id=\"empty\"/><line id=\"gamma\"/>gamma</body>");
    }

    // Get <line/> elements
    Element alphaLine = DocHelper.findElementById(doc, "alpha");
    Element betaLine = DocHelper.findElementById(doc, "beta");
    Element emptyLine = DocHelper.findElementById(doc, "empty");
    Element gammaLine = DocHelper.findElementById(doc, "gamma");

    // Get text nodes
    Node alpha = alphaLine.getNextSibling();
    Node beta = betaLine.getNextSibling();
    Node gamma = gammaLine.getNextSibling();

    // Wrap each line in <l:p> elements
    Element alphaLp = wrapInLp(alpha, betaLine);
    Element betaLp = wrapInLp(beta, emptyLine);
    Element emptyLp = insertLp(gammaLine);
    Element gammaLp = wrapInLp(gamma, null);

    // Get various points for tests
    Point<Node> startAlpha = Point.inText(alpha, 0);
    Point<Node> inAlpha = Point.inText(alpha, 2);
    Point<Node> endAlpha = Point.textOrElementEnd(cxt.annotatableContent(), alpha);
    Point<Node> beforeBeta = Point.start(cxt.annotatableContent(), betaLp);
    Point<Node> startBeta = Point.inText(beta, 0);
    Point<Node> inBeta = Point.inText(beta, 2);
    Point<Node> endBeta = Point.textOrElementEnd(cxt.annotatableContent(), beta);
    Point<Node> inEmpty = Point.start(cxt.annotatableContent(), emptyLp);
    Point<Node> beforeGamma = Point.start(cxt.annotatableContent(), gammaLp);
    Point<Node> startGamma = Point.inText(gamma, 0);
    Point<Node> inGamma = Point.inText(gamma, 2);
    Point<Node> endGamma = Point.textOrElementEnd(cxt.annotatableContent(), gamma);
    {
      PersistentContent.makeHard(cxt.elementManager(), alphaLp);
    }
  }

  /**
   * A single test case for getBoundaryPointNearSelection with proper selection
   * range
   *
   * @param startSelection beginning of selection
   * @param endSelection end of selection
   * @param correct the correct answer
   */
  private void testGetBoundaryPointNearSelectionRange(
      Point<Node> startSelection, Point<Node> endSelection, Point<Node> correct) {
    int location = DocumentUtil.<Node, Element, Text> getLocationNearSelection(
        doc, cxt.hardView(), new PointRange<Node>(startSelection, endSelection));
    assertEquals(location, DocHelper.getFilteredLocation(doc, cxt.persistentView(), correct));
  }

  /**
   * A single test case for getBoundaryPointNearSelection with collapsed
   * selection
   *
   * @param caret collapsed section
   * @param correct the correct answer
   */
  private void testGetBoundaryPointNearSelectionPoint(Point<Node> caret, Point<Node> correct) {
    int location =
        DocumentUtil.getLocationNearSelection(doc, cxt.hardView(), new PointRange<Node>(caret));
    assertEquals(location, DocHelper.getFilteredLocation(doc, cxt.persistentView(), correct));
  }

  public void testGetBoundaryPointNearSelectionRange() {
    TestState s = new TestState();
    testGetBoundaryPointNearSelectionRange(s.startAlpha, s.endAlpha, s.endAlpha);
    testGetBoundaryPointNearSelectionRange(s.startAlpha, s.inAlpha, s.endAlpha);
    testGetBoundaryPointNearSelectionRange(s.inAlpha, s.endAlpha, s.endAlpha);
    // see http://b/issue?id=2059680
    testGetBoundaryPointNearSelectionRange(s.startAlpha, s.beforeBeta, s.endAlpha);
    testGetBoundaryPointNearSelectionRange(s.inAlpha, s.beforeBeta, s.endAlpha);
    testGetBoundaryPointNearSelectionRange(s.endAlpha, s.beforeBeta, s.endAlpha);

    testGetBoundaryPointNearSelectionRange(s.startBeta, s.endBeta, s.endBeta);
    testGetBoundaryPointNearSelectionRange(s.startBeta, s.inBeta, s.endBeta);
    testGetBoundaryPointNearSelectionRange(s.startBeta, s.inEmpty, s.endBeta);
    testGetBoundaryPointNearSelectionRange(s.endBeta, s.inEmpty, s.endBeta);
    testGetBoundaryPointNearSelectionRange(s.inBeta, s.inEmpty, s.endBeta);

    testGetBoundaryPointNearSelectionRange(s.inEmpty, s.beforeGamma, s.inEmpty);

    testGetBoundaryPointNearSelectionRange(s.startGamma, s.endGamma, s.endGamma);
    testGetBoundaryPointNearSelectionRange(s.startGamma, s.inGamma, s.endGamma);
    testGetBoundaryPointNearSelectionRange(s.inGamma, s.endGamma, s.endGamma);
  }

  public void testGetBoundaryPointNearSelectionPoint() {
    TestState s = new TestState();
    testGetBoundaryPointNearSelectionPoint(s.startAlpha, s.endAlpha);
    testGetBoundaryPointNearSelectionPoint(s.inAlpha, s.endAlpha);
    testGetBoundaryPointNearSelectionPoint(s.endAlpha, s.endAlpha);

    testGetBoundaryPointNearSelectionPoint(s.startBeta, s.endBeta);
    testGetBoundaryPointNearSelectionPoint(s.inBeta, s.endBeta);
    testGetBoundaryPointNearSelectionPoint(s.endBeta, s.endBeta);

    testGetBoundaryPointNearSelectionPoint(s.inEmpty, s.inEmpty);

    testGetBoundaryPointNearSelectionPoint(s.startGamma, s.endGamma);
    testGetBoundaryPointNearSelectionPoint(s.inGamma, s.endGamma);
    testGetBoundaryPointNearSelectionPoint(s.endGamma, s.endGamma);
  }

  private Element insertLp(Node nodeAfter) {
    return cxt.annotatableContent().transparentCreate("l:p",
        Collections.<String, String> emptyMap(),
        cxt.annotatableContent().getParentElement(nodeAfter), nodeAfter);
  }

  private Element wrapInLp(Node fromIncl, Node toExcl) {
    Element lp = insertLp(fromIncl);
    cxt.annotatableContent().transparentMove(lp, fromIncl, toExcl, null);
    return lp;
  }

  private MutableDocument<Node, Element, Text> getDocWithoutSchema(String innerXml) {
    cxt = ContextProviders.createTestPojoContext(
        innerXml, null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    return doc = cxt.document();
  }
}
