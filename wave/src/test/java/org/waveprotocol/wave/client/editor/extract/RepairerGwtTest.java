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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRawDocument;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.testing.TestEditors;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.El;

/**
 * Test the repairer
 *
 * When testing the revert method, we directly access revertInner, because the normal
 * method has some more failsafe code to handle the case of invalid arguments. This could
 * skew our test cases.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class RepairerGwtTest extends GWTTestCase {
  ContentRawDocument c = null;
  Repairer r = null;
  ContentDocument dom = null;
  ContentElement root, p, b;
  ContentTextNode t1, t2, t3;
  MockRepairListener l = new MockRepairListener();

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.extract.Tests";
  }

  public void testRevertEmptyElement() {
    init();
    initEmptyP();
    c.insertBefore(root, p, null);

    // essentially a noop
    assertTrue(p.isConsistent());
    p.revertImplementation();
    assertTrue(p.isConsistent());

    // add some random text node
    p.getImplNodelet().appendChild(Document.get().createTextNode("blah"));
    p.revertImplementation();
    assertEquals(null, p.getImplNodelet().getFirstChild());

    p.getImplNodelet().appendChild(ca());
    p.revertImplementation();
    assertEquals(null, p.getImplNodelet().getFirstChild());

    p.getImplNodelet().appendChild(ca());
    p.revertImplementation();
    assertEquals(null, p.getImplNodelet().getFirstChild());

    l.check(0, 0); // calls through revertInner, so none reported
  }

  public void testRevertComplicatedStructure() {
    init();
    initHelloThere();

    // no-op
    p.revertImplementation();
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // blow it all away
    p.getImplNodelet().setInnerHTML("");
    assertEquals("", p.getImplNodelet().getInnerHTML());
    p.revertImplementation();
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // insert stuff at start and end
    p.getImplNodelet().appendChild(ca());
    t1.getImplNodelet().getParentNode().insertBefore(ca(), t1.getImplNodelet());
    assertEquals("<a></a>hello <b>th</b>ere<a></a>", p.getImplNodelet().getInnerHTML());
    p.revertImplementation();
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // insert stuff inside inner element
    b.getImplNodelet().appendChild(ca());
    p.revertImplementation();
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // reorganise stuff
    b.getImplNodelet().appendChild(t3.getImplNodelet());
    assertEquals("hello <b>there</b>", p.getImplNodelet().getInnerHTML());
    p.revertImplementation();
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    l.check(0, 0); // calls through revertInner, so none reported
  }

  public void testRevertRange() {
    init();
    initHelloThere();

    // blow it all away
    p.getImplNodelet().setInnerHTML("");
    assertEquals("", p.getImplNodelet().getInnerHTML());
    r.revertInner(Point.before(c, t1), Point.after(c, t3));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // insert stuff at start and end, revert whole p
    p.getImplNodelet().appendChild(ca());
    t1.getImplNodelet().getParentNode().insertBefore(ca(), t1.getImplNodelet());
    r.revertInner(Point.before(c, p), Point.after(c, p));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // insert stuff at start and end, revert parts of p
    p.getImplNodelet().appendChild(ca());
    t1.getImplNodelet().getParentNode().insertBefore(ca(), t1.getImplNodelet());
    r.revertInner(Point.before(c, t1), Point.before(c, t3));
    assertEquals("hello <b>th</b>ere<a></a>", p.getImplNodelet().getInnerHTML());
    r.revertInner(Point.before(c, t1), Point.end((ContentNode)p));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // insert stuff inside inner element
    b.getImplNodelet().appendChild(ca());
    r.revertInner(Point.after(c, t1), Point.before(c, t3));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // reorganise stuff
    b.getImplNodelet().appendChild(t3.getImplNodelet());
    assertEquals("hello <b>there</b>", p.getImplNodelet().getInnerHTML());
    r.revertInner(Point.before(c, t1), Point.after(c, t3));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    // wrap stuff randomly
    b.getImplNodelet().appendChild(t3.getImplNodelet());
    assertEquals("hello <b>there</b>", p.getImplNodelet().getInnerHTML());
    Element wrapper = ca();
    Node bit = t1.getImplNodelet().splitText(2);
    bit.getParentNode().insertBefore(wrapper, bit);
    wrapper.appendChild(bit);
    r.revertInner(Point.before(c, t1), Point.after(c, t3));
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());

    l.check(0, 0); // calls through revertInner, so none reported
  }

  public void testReporting() {
    init();
    initHelloThere();
    l.check(0, 0);

    // fix and revert a range
    p.getImplNodelet().appendChild(ca());
    t1.getImplNodelet().getParentNode().insertBefore(ca(), t1.getImplNodelet());
    r.revert(Point.before(c, t1), Point.before(c, t3));
    assertEquals("hello <b>th</b>ere<a></a>", p.getImplNodelet().getInnerHTML());
    l.check(0, 1);

    // revert everything
    r.revert(c, c.getDocumentElement());
    assertEquals("hello <b>th</b>ere", p.getImplNodelet().getInnerHTML());
    l.check(1, 1);
  }

  protected Element ca() {
    return Document.get().createAnchorElement();
  }

  protected void init() {
    dom = TestEditors.createTestDocument();
    Repairer.debugRepairIsFatal = false;
    r = new Repairer(dom.getContext().persistentView(),
        dom.getContext().rendering().getRenderedContentView(),
        dom.getStrippingHtmlView(), l);
    c = dom.debugGetRawDocument();
    root = c.getDocumentElement();
    l.clear();
  }

  protected void initEmptyP() {
    p = c.createElement("q", root, null);
  }

  protected void initHelloThere() {
    initEmptyP();
    b = c.createElement("b", p, null);
    t2 = c.createTextNode("th", b, null);
    t1 = c.createTextNode("hello ", p, b);
    t3 = c.createTextNode("ere", p, null);
  }

  public class MockRepairListener implements RepairListener {
    private int full = 0;
    private int range = 0;
    @Override
    public void onFullDocumentRevert(
        ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc) {
      full++;
    }
    @Override
    public void onRangeRevert(El<ContentNode> start, El<ContentNode> end) {
      range++;
    }
    public void check(int full, int range) {
      assertEquals(full, this.full);
      assertEquals(range, this.range);
    }
    public void clear() {
      full = 0;
      range = 0;
    }

    @SuppressWarnings("static")
    public void fail() {
      RepairerGwtTest.this.fail("Ended up with " + full + ", " + range);
    }
  }
}
