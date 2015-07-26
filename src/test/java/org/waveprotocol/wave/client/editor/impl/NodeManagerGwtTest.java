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

package org.waveprotocol.wave.client.editor.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Text;
import com.google.gwt.junit.client.GWTTestCase;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRawDocument;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.testing.TestEditors;

import org.waveprotocol.wave.model.document.util.Point;

/**
 * Tests our mapping logic from HTML to wrapper nodes.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class NodeManagerGwtTest extends GWTTestCase {
  ContentRawDocument c = null;
  NodeManager m = null;

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.impl.Tests";
  }

  public void testFindsTextNodeWrapperNormally() throws HtmlInserted, HtmlMissing {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();
    ContentElement root = c.getDocumentElement();
    m = dom.getContext().rendering().getNodeManager();

    String s1 = "some text", s2 = "other writings", s3 = "more information";

    // t1
    ContentTextNode t1 = c.createTextNode(s1, root, null);
    Text n1 = t1.getImplNodelet();
    checkWrapper(t1, n1);

    // b1 t1
    ContentElement b1 = c.createElement("b", root, t1);
    checkWrapper(t1, n1);

    // b1 t1 b2
    ContentElement b2 = c.createElement("b", root, null);
    checkWrapper(t1, n1);
    Text n1b = n1.splitText(2);
    checkWrapper(t1, n1b);

    // b1 t1 t2 b2
    ContentTextNode t2 = c.createTextNode(s2, root, b2);
    Text n2 = t2.getImplNodelet(), n2b;
    checkWrapper(t1, n1);
    checkWrapper(t1, n1b);
    n2b = n2.splitText(2);
    checkWrapper(t2, n2);
    checkWrapper(t2, n2b);

    // t1 b2
    c.removeChild(root, b1);
    checkWrapper(t1, n1b);
    checkWrapper(t2, n2b);

    c.removeChild(root, t2);
    c.removeChild(root, b2);
    checkWrapper(t1, n1);

  }

  public void testRepairsWherePossible() throws HtmlInserted, HtmlMissing {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();
    ContentElement root = c.getDocumentElement();
    m = dom.getContext().rendering().getNodeManager();

    // TODO(danilatos): Expand this and other test cases

    String s1 = "some text", s2 = "other writings", s3 = "more information";
    ContentTextNode t1 = c.createTextNode(s1, root, null);

    Text n1 = t1.getImplNodelet(), n1b;

    n1b = n1.splitText(1);
    n1b.setData(s1);
    n1.removeFromParent();

    try {
      m.findTextWrapper(n1b, false);
      fail("Expected exception when not repairing");
    } catch (HtmlInserted e) {
    } catch (HtmlMissing e) {
    }

    checkWrapper(t1, n1b, true);
  }

  public void testThrowsExceptionsWhenNecessary() {
    //TODO(danilatos)
  }

  public void testWrapperElementPointToNodeletPoint() {
    ContentDocument dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();

    ContentElement root = c.getDocumentElement();
    m = dom.getContext().rendering().getNodeManager();

    Element rootNodelet = root.getImplNodelet();
    // meta element, null impl nodelet
    ContentElement n1 = c.createElement("m", root, null);
    n1.setImplNodelets(null, null);

    // regular node
    ContentNode n2 = c.createElement("a", root, null);

    // basic check
    assertSame(null, m.wrapperElementPointToNodeletPoint(
        Point.<ContentNode>end(root)).getNodeAfter());

    // check left-biasing
    rootNodelet.appendChild(Document.get().createBRElement());
    assertSame(rootNodelet.getLastChild(), m.wrapperElementPointToNodeletPoint(
        Point.<ContentNode>end(root)).getNodeAfter());

    // basic check
    assertSame(n2.getImplNodelet(), m.wrapperElementPointToNodeletPoint(
        Point.inElement(root, n2)).getNodeAfter());

    // search-rightwards for next impl nodelet check (n1 has null impl nodelet)
    assertSame(n2.getImplNodelet(), m.wrapperElementPointToNodeletPoint(
        Point.<ContentNode>inElement(root, n1)).getNodeAfter());

  }

  protected void checkWrapper(ContentTextNode wrapper, Text nodelet)
      throws HtmlInserted, HtmlMissing {
    checkWrapper(wrapper, nodelet, false);
  }

  protected void checkWrapper(ContentTextNode wrapper, Text nodelet, boolean attemptRepair)
      throws HtmlInserted, HtmlMissing {
    assertSame(wrapper, m.findTextWrapper(nodelet, attemptRepair));
    assertEquals(wrapper.getData(), wrapper.getImplData());
    assertSame(wrapper, m.findTextWrapper(nodelet, true));
    assertEquals(wrapper.getData(), wrapper.getImplData());
  }
}
