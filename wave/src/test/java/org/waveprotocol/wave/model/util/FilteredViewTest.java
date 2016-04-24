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

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.FilteredView;

/**
 * Test cases for FilteredView behaviour
 *
 * TODO(danilatos): Define some behaviour for calling non-visibility methods on
 * invisible nodes (such as, throw exception, or return null), and implement and
 * write unit tests for it.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class FilteredViewTest extends TestCase {

  /**
   * Simplistic filtering view
   *
   * Elements with "s" as the tag name are shallow skipped, with "d" are deep
   * skipped.
   */
  private class BasicFilteringView extends FilteredView<Node, Element, Text> {

    public BasicFilteringView(ReadableDocument<Node, Element, Text> innerView) {
      super(innerView);
    }

    @Override
    protected FilteredView.Skip getSkipLevel(Node node) {
      // Check if it's in the document, for validity
      Node find = node;
      while (find != getDocumentElement()) {
        if (find == null) {
          return Skip.INVALID;
        }
        find = find.getParentElement();
      }

      Element e = asElement(node);
      if (e == null) {
        return Skip.NONE;
      } else if (e.getTagName().equals("s")) {
        return Skip.SHALLOW;
      } else if (e.getTagName().equals("d")) {
        return Skip.DEEP;
      } else {
        return Skip.NONE;
      }
    }

    protected Node getNode(int ... xmlPoint) {
      Node current = getDocumentElement();
      for (int i = 0; i < xmlPoint.length; i++) {
        current = current.getFirstChild();
        for (int remaining = xmlPoint[i]; remaining > 0; remaining--) {
          current = current.getNextSibling();
        }
      }
      return current;
    }
  }

  public void testUnfilteredTraversalStillWorks() {
    BasicFilteringView doc = parse("hi<a/>there");
    assertSame(doc.getNode(1), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(1), doc.getPreviousSibling(doc.getNode(2)));
    assertSame(null, doc.getPreviousSibling(doc.getNode(0)));
    assertSame(null, doc.getNextSibling(doc.getNode(2)));
    assertSame(null, doc.getFirstChild(doc.getNode(1)));
    assertSame(null, doc.getLastChild(doc.getNode(1)));
    assertSame(doc.getDocumentElement(), doc.getParentElement(doc.getNode(1)));
    assertSame(doc.getNode(0), doc.getFirstChild(doc.getDocumentElement()));
    assertSame(doc.getNode(2), doc.getLastChild(doc.getDocumentElement()));
  }

  public void testSubtreeSkippedForDeepNodes() {
    BasicFilteringView doc = parse("hi<d>xyz</d>there");
    assertSame(doc.getNode(2), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(0), doc.getPreviousSibling(doc.getNode(2)));
    doc = parse("<d>xyz</d>hi there<d>abc</d>");
    assertSame(null, doc.getNextSibling(doc.getNode(1)));
    assertSame(null, doc.getPreviousSibling(doc.getNode(1)));
    assertSame(doc.getNode(1), doc.getFirstChild(doc.getDocumentElement()));
    assertSame(doc.getNode(1), doc.getLastChild(doc.getDocumentElement()));
  }

  public void testSubtreeRepeatedlySkippedForDeepNodes() {
    BasicFilteringView doc = parse("hi<d>abc</d><d>xyz</d>there");
    assertSame(doc.getNode(3), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(0), doc.getPreviousSibling(doc.getNode(3)));
    doc = parse("<d>zyx</d><d>xyz</d>hi there<d>abc</d><d>def</d>");
    assertSame(null, doc.getNextSibling(doc.getNode(2)));
    assertSame(null, doc.getPreviousSibling(doc.getNode(2)));
    assertSame(doc.getNode(2), doc.getFirstChild(doc.getDocumentElement()));
    assertSame(doc.getNode(2), doc.getLastChild(doc.getDocumentElement()));
  }

  public void testElementSkippedForShallowNodes() {
    BasicFilteringView doc = parse("hi<s/>there");
    assertSame(doc.getNode(2), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(0), doc.getPreviousSibling(doc.getNode(2)));
    doc = parse("hi<s>xyz</s>there");
    assertSame(doc.getNode(1, 0), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(1, 0), doc.getPreviousSibling(doc.getNode(2)));
    assertSame(doc.getDocumentElement(), doc.getParentElement(doc.getNode(1, 0)));
    doc = parse("<s>xyz</s>hi there<s>abc</s>");
    assertSame(doc.getNode(2, 0), doc.getNextSibling(doc.getNode(1)));
    assertSame(doc.getNode(0, 0), doc.getPreviousSibling(doc.getNode(1)));
    assertSame(doc.getNode(0, 0), doc.getFirstChild(doc.getDocumentElement()));
    assertSame(doc.getNode(2, 0), doc.getLastChild(doc.getDocumentElement()));
  }

  public void testElementRepeatedlySkippedForShallowNodes() {
    BasicFilteringView doc = parse("hi<s><s>xyz</s><s>zyx</s></s>there");
    assertSame(doc.getNode(1, 0, 0), doc.getNextSibling(doc.getNode(0)));
    assertSame(doc.getNode(1, 1, 0), doc.getPreviousSibling(doc.getNode(2)));
    assertSame(doc.getDocumentElement(), doc.getParentElement(doc.getNode(1, 0, 0)));
    doc = parse("<s><s>xyz</s><s>zyx</s></s>hi there<s><s>abc</s><s>cba</s></s>");
    assertSame(doc.getNode(2, 0, 0), doc.getNextSibling(doc.getNode(1)));
    assertSame(doc.getNode(0, 1, 0), doc.getPreviousSibling(doc.getNode(1)));
    assertSame(doc.getNode(0, 0, 0), doc.getFirstChild(doc.getDocumentElement()));
    assertSame(doc.getNode(2, 1, 0), doc.getLastChild(doc.getDocumentElement()));
  }

  public void testVisibilityMethodsAlwaysReturnGivenNodeIfVisible() {
    BasicFilteringView doc = parse("hi<a>th</a>ere");
    assertSame(doc.getNode(1), doc.getVisibleNode(doc.getNode(1)));
    assertSame(doc.getNode(1), doc.getVisibleNodeNext(doc.getNode(1)));
    assertSame(doc.getNode(1), doc.getVisibleNodePrevious(doc.getNode(1)));
    assertSame(doc.getNode(1), doc.getVisibleNodeFirst(doc.getNode(1)));
    assertSame(doc.getNode(1), doc.getVisibleNodeLast(doc.getNode(1)));
  }

  public void testVisibilityMethodsAreDepthFirstAndExitVisibleContainers() {
    BasicFilteringView doc = parse("hi<s><s>xyz</s><s>zyx</s></s>there<s/>");
    assertSame(doc.getNode(1, 0, 0), doc.getVisibleNodeFirst(doc.getNode(1)));
    assertSame(doc.getNode(2), doc.getVisibleNodeNext(doc.getNode(1)));

    assertSame(doc.getNode(1, 0, 0), doc.getVisibleNodeFirst(doc.getNode(1, 0)));
    assertSame(doc.getNode(1, 1, 0), doc.getVisibleNodeNext(doc.getNode(1, 0)));
    assertSame(doc.getNode(1, 1, 0), doc.getVisibleNodeLast(doc.getNode(1, 1)));
    assertSame(doc.getNode(1, 0, 0), doc.getVisibleNodePrevious(doc.getNode(1, 1)));
    assertSame(doc.getDocumentElement(), doc.getVisibleNode(doc.getNode(1, 0)));

    doc = parse("hi<d><d/>abc</d><d>xyz</d>there");
    assertSame(doc.getNode(3), doc.getVisibleNodeNext(doc.getNode(1)));
    assertSame(doc.getNode(0), doc.getVisibleNodePrevious(doc.getNode(2)));
    assertSame(doc.getDocumentElement(), doc.getVisibleNode(doc.getNode(1, 0)));

    doc = parse("hi<s><d/><d/></s>there");
    assertSame(doc.getNode(2), doc.getVisibleNodeFirst(doc.getNode(1, 0)));
    assertSame(doc.getNode(2), doc.getVisibleNodeNext(doc.getNode(1, 0)));
    assertSame(doc.getNode(0), doc.getVisibleNodeLast(doc.getNode(1, 1)));
    assertSame(doc.getNode(0), doc.getVisibleNodePrevious(doc.getNode(1, 1)));
  }

  public void testVisibilityMethodsReturnNullForNullInput() {
    BasicFilteringView doc = parse("");
    assertSame(null, doc.getVisibleNodeFirst(null));
    assertSame(null, doc.getVisibleNodeNext(null));
    assertSame(null, doc.getVisibleNodePrevious(null));
    assertSame(null, doc.getVisibleNodePrevious(null));
    assertSame(null, doc.getVisibleNode(null));
  }

  public void testNullReturnedForInvalidNodes() {
    BasicFilteringView doc = parse("");
    Node invalidNode = parse("<x>a<y>b</y>c</x>").getNode(0);
    assertSame(null, doc.getFirstChild(invalidNode));
    assertSame(null, doc.getLastChild(invalidNode));
    assertSame(null, doc.getNextSibling(invalidNode));
    assertSame(null, doc.getPreviousSibling(invalidNode));
    assertSame(null, doc.getParentElement(invalidNode));
  }

  protected BasicFilteringView parse(String xml) {
    return new BasicFilteringView(DocProviders.POJO.parse(xml));
  }
}
