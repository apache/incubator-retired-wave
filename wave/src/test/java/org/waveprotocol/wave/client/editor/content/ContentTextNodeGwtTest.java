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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.testing.TestEditors;

/**
 * Tests for ContentTextNode behaviour
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class ContentTextNodeGwtTest extends ContentTestBase {

  public void testContentDataInitialisedFromNodelet() {
    ContentDocument dom = TestEditors.createTestDocument();

    ContentTextNode txt = new ContentTextNode(
        Document.get().createTextNode("abc"),
        dom.debugGetContext());
    assertEquals("abc", txt.getData());
  }

  public void testImplDataSumsTextNodes() throws HtmlMissing {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();
    ContentElement root = c.getDocumentElement();

    ContentTextNode t1 = c.createTextNode("hello", root, null);
    Text txt = t1.getImplNodelet();

    assertEquals("hello", t1.getImplData());
    Text txt3 = Document.get().createTextNode(" there");
    txt.getParentNode().insertAfter(txt3, txt);
    assertEquals("hello there", t1.getImplData());
    Text txt2 = txt.splitText(2);
    assertEquals("hello there", t1.getImplData());
    assertSame(txt3, txt.getNextSibling().getNextSibling());
    assertTrue(t1.owns(txt) && t1.owns(txt2) && t1.owns(txt3));

    ContentTextNode t2 = c.createTextNode("before", root, t1);

    assertEquals("before", t2.getImplData());
    Text t2_2 = t2.getImplNodelet().splitText(3);
    assertEquals("before", t2.getImplData());
    assertTrue(t2.owns(t2_2));
  }

  public void testExceptionsThrownWhenHtmlIsBroken() {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();
    ContentElement root = c.getDocumentElement();

    ContentTextNode t1 = c.createTextNode("hello", root, null);
    ContentTextNode t2 = c.createTextNode("blah", root, null);
    Text txt1 = t1.getImplNodelet();
    Text txt2 = t2.getImplNodelet();

    txt1.removeFromParent();

    try {
      t1.getImplData();
      fail("Did not throw HtmlMissing exception");
    } catch (HtmlMissing e) {
      assertEquals(t1, e.getBrokenNode());
      assertEquals(root.getImplNodelet(), e.getNode());
    }

    txt2.getParentNode().insertBefore(txt1, txt2);
    txt2.removeFromParent();

    try {
      t1.getImplData();
      fail("Did not throw HtmlMissing exception");
    } catch (HtmlMissing e) {
      assertEquals(t2, e.getBrokenNode());
      assertEquals(root.getImplNodelet(), e.getNode());
    }

    Text txt1b = txt1.splitText(2);
    try {
      t1.getImplData();
      fail("Did not throw HtmlMissing exception");
    } catch (HtmlMissing e) {
      assertEquals(t2, e.getBrokenNode());
      assertEquals(root.getImplNodelet(), e.getNode());
    }

    txt1b.getParentNode().insertAfter(t2.getImplNodelet(), txt1b);
    try {
      assertEquals("hello", t1.getImplData());
    } catch (HtmlMissing e) {
      fail(e.getMessage());
      e.printStackTrace();
    }
  }


  public void testSplitTextNodesStayGrouped() throws HtmlMissing {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();

    ContentElement root = c.getDocumentElement();
    ContentElement p2 = c.createElement("q", root, null);
    ContentElement p1 = c.createElement("q", root, p2);

    String s1 = "something interesting", s2 = "amazingly fascinating";
    ContentTextNode t1 = c.createTextNode(s1, p1, null);
    ContentTextNode t2 = c.createTextNode(s2, p2, null);

    assertEquals("a", s1, t1.getImplData());
    assertEquals("b", s2, t2.getImplData());

    t1.getImplNodelet().splitText(3).splitText(3).splitText(3);
    assertEquals("c", s1, t1.getImplData());

    t2.getImplNodelet().splitText(3).splitText(3).splitText(3);
    assertEquals("d", s2, t2.getImplData());

    c.insertBefore(p1, t2, t1);
    assertEquals("e", s1, t1.getImplData());
    assertEquals("f", s2, t2.getImplData());

    c.insertBefore(p1, t1, t2);
    assertEquals("g", s1, t1.getImplData());
    assertEquals("h", s2, t2.getImplData());

    c.removeChild(p1, t2);
    assertEquals("i", s1, t1.getImplData());

    c.removeChild(p1, t1);
    assertEquals(0, p1.getImplNodelet().getChildCount());
  }
}
