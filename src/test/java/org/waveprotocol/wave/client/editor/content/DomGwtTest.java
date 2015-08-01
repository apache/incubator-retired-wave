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

import org.waveprotocol.wave.client.editor.testing.TestEditors;

import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Low level sanity tests for the editor's DOM implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DomGwtTest extends ContentTestBase {

  public void testDomElementManipulations() {
    ContentDocument dom = TestEditors.createTestDocument();

    c = dom.debugGetRawDocument();

    ContentElement docElement = c.getDocumentElement();
    c.createElement("x", docElement, null);

    // empty root
    ContentElement root = (ContentElement) c.getDocumentElement().getFirstChild();

    assertStructure(root, docElement, null, null, null, null);

    // single child
    ContentElement p = c.createElement("q", root, null);

    assertStructure(root, docElement, null, null, p, p);
    assertStructure(p, root, null, null, null, null);

    // child at beginning, before exiting child
    ContentElement b = c.createElement("b", root, p);

    assertStructure(root, docElement, null, null, b, p);
    assertStructure(b, root, null, p, null, null);
    assertStructure(p, root, b, null, null, null);

    // swap children from start to end
    c.insertBefore(root, p, b);

    assertStructure(root, docElement, null, null, p, b);
    assertStructure(p, root, null, b, null, null);
    assertStructure(b, root, p, null, null, null);

    // child at end, after exiting child
    ContentElement a = c.createElement("a", root, null);

    assertStructure(root, docElement, null, null, p, a);
    assertStructure(p, root, null, b, null, null);
    assertStructure(b, root, p, a, null, null);
    assertStructure(a, root, b, null, null, null);

    // remove child from middle
    c.removeChild(root, b);

    assertStructure(root, docElement, null, null, p, a);
    assertStructure(p, root, null, a, null, null);
    assertStructure(a, root, p, null, null, null);
    assertStructure(b, null, null, null, null, null);

    // create again, can't reuse removed nodes
    b = c.createElement("b", root, p);
    // insert child between exiting children
    c.insertBefore(root, b, a);

    assertStructure(root, docElement, null, null, p, a);
    assertStructure(p, root, null, b, null, null);
    assertStructure(b, root, p, a, null, null);
    assertStructure(a, root, b, null, null, null);

    // remove child from end
    c.removeChild(root, a);

    assertStructure(root, docElement, null, null, p, b);
    assertStructure(p, root, null, b, null, null);
    assertStructure(b, root, p, null, null, null);
    assertStructure(a, null, null, null, null, null);

    // remove child from beginning
    c.removeChild(root, p);

    assertStructure(root, docElement, null, null, b, b);
    assertStructure(b, root, null, null, null, null);
    assertStructure(p, null, null, null, null, null);

    // remove only child
    c.removeChild(root, b);

    assertStructure(root, docElement, null, null, null, null);
    assertStructure(b, null, null, null, null, null);
  }

  public void testAttributesReflectedOnlyInContent() {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();

    ContentElement e = c.createElement("a", c.getDocumentElement(), null);
    assertEquals(0, c.getAttributes(e).size());
    c.setAttribute(e, "href", "blah");
    c.setAttribute(e, "x", "y");
    c.setAttribute(e, "aa", "bb");
    assertEquals("bb", c.getAttribute(e, "aa"));
    assertSame(null, c.getAttribute(e, "notthere"));
    assertEquals(3, c.getAttributes(e).size());
    for (String key : c.getAttributes(e).keySet()) {
      assertTrue(!e.getImplNodelet().hasAttribute(key));
    }
    c.setAttribute(e, "x", "abc");
    c.removeAttribute(e, "aa");
    assertEquals("abc", c.getAttribute(e, "x"));
    assertSame(null, c.getAttribute(e, "aa"));
    assertEquals(2, c.getAttributes(e).size());
  }

  public void testDomElementManipulationsWithMetaElements() {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();

    // empty root
    ContentElement root = c.getDocumentElement();

    ContentElement p1 = c.createElement("q", root, null);
    ContentElement p2 = c.createElement("q", root, null);

    ContentTextNode t1 = c.createTextNode("abc", p1, null);
    ContentElement m = c.createElement("m", p1, null);
    ContentTextNode t2 = c.createTextNode("def", p1, null);
    c.createElement("m", m, null);

    c.insertBefore(p2, t1, null, null);

    ContentTextNode t1b = c.splitText(t1, 1);
    c.splitText(t2, 1);

    c.removeChild(p2, t1b);
    c.removeChild(p2, m);
    c.removeChild(p2, t2);

    assertEquals("<q>aef</q>", XmlStringBuilder.createNode(
        dom.getContext().rendering().getFullHtmlView(),
        p2.getImplNodelet()).toString().toLowerCase());
    assertEquals("<q>aef</q>", XmlStringBuilder.createNode(
        c, p2).toString().toLowerCase());
  }

}
