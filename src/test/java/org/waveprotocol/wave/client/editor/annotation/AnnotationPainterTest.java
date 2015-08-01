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

package org.waveprotocol.wave.client.editor.annotation;


import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;


public class AnnotationPainterTest extends AnnotationPainterTestBase {

  public void testNodeAtEnd() {
    TestDocumentContext<Node, Element, Text> cxt = createAnnotationContext();

    MutableDocument<Node, Element, Text> doc = cxt.document();
    Element e = doc.createChildElement(doc.getDocumentElement(), "p", Attributes.EMPTY_MAP);
    doc.insertText(Point.<Node>inElement(e, null), "Hi");

    doc.setAnnotation(3, 4, "x", "1");
    timerService.tick(100);

    ReadableDocument<Node, Element, Text> fullDoc = cxt.getFullRawDoc();
    Element boundary = fullDoc.asElement(fullDoc.getLastChild(e));
    assertNotNull(boundary);

    assertEquals("l:b", boundary.getTagName());

    doc.setAnnotation(3, 4, "x", null);
    timerService.tick(100);

    boundary = fullDoc.asElement(fullDoc.getLastChild(e));
    assertNull(boundary);
  }

  /** Tests bug 1839733 */
  public void testNestedPaintNodes() {
    TestDocumentContext<Node, Element, Text> cxt = createAnnotationContext();

    MutableDocument<Node, Element, Text> doc = cxt.document();
    Element e = doc.createChildElement(doc.getDocumentElement(), "p", Attributes.EMPTY_MAP);
    doc.insertText(doc.locate(1), "A");
    doc.insertText(doc.locate(2), "B");
    doc.setAnnotation(2, 3, "a", "1");
    timerService.tick(100);

    checkXml("<p>A<l:p a=\"1\">B</l:p></p>", XmlStringBuilder.innerXml(cxt.getFullRawDoc()));

    doc.insertText(doc.locate(3), "C");
    doc.setAnnotation(3, 4, "b", "2");
    timerService.tick(100);

    checkXml("<p>A<l:p a=\"1\">B</l:p><l:p a=\"1\" b=\"2\">C</l:p></p>",
        XmlStringBuilder.innerXml(cxt.getFullRawDoc()));

    doc.insertText(doc.locate(4), "D");
    doc.setAnnotation(4, 5, "a", null);
    timerService.tick(100);

    checkXml("<p>A<l:p a=\"1\">B</l:p><l:p a=\"1\" b=\"2\">C</l:p><l:p b=\"2\">D</l:p></p>",
        XmlStringBuilder.innerXml(cxt.getFullRawDoc()));

    doc.insertText(doc.locate(5), "E");
    doc.setAnnotation(5, 6, "b", null);
    timerService.tick(100);

    checkXml("<p>A<l:p a=\"1\">B</l:p><l:p a=\"1\" b=\"2\">C</l:p><l:p b=\"2\">D</l:p>E</p>",
        XmlStringBuilder.innerXml(cxt.getFullRawDoc()));
  }

  public void testCorrectBoundaryPoint() {
    // immitate the structore of line containers with a local paragraph
    TestDocumentContext<Node, Element, Text> cxt = createAnnotationContext();
    MutableDocument<Node, Element, Text> doc = cxt.document();
    Element lc = doc.createChildElement(doc.getDocumentElement(), "body", Attributes.EMPTY_MAP);
    Element l = doc.createChildElement(lc, "line", Attributes.EMPTY_MAP);
    Element label = doc.createChildElement(lc, "label", Attributes.EMPTY_MAP);
    doc.insertText(Point.end((Node)label), "test");

    // set up local content, moving the label into it
    Element local = cxt.annotatableContent().transparentCreate(
        "l:l", Attributes.EMPTY_MAP, lc, null);
    PersistentContent.makeHard(cxt.annotatableContent(), local);
    cxt.annotatableContent().transparentMove(local, l.getNextSibling(), local, null);

    try {
      doc.setAnnotation(3, 9, "a", "1");
      timerService.tick(100);
    } catch (Exception e) {
      fail("Annotation painting threw exception - " + e.getMessage());
    }
  }

  void checkXml(String a, XmlStringBuilder b) {
    String b2 = b.toString();
    if (!a.equals(b2)) {
      System.out.println(a);
      System.out.println(b2);
      assertEquals(a, b2);
    }
  }
}
