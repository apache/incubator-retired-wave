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

package org.waveprotocol.wave.model.document.indexed;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.AttributesModified;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentDeleted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentInserted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.TextDeleted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.TextInserted;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler.EventBundle;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.Nindo.Builder;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic tests for observable indexed doc
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class ObservableIndexedDocumentTest extends TestCase {
  EventBundle<Node, Element, Text> events, events2;

  IndexedDocument<Node, Element, Text> indexed;
  IndexedDocument<Node, Element, Text> indexedCopy;
  MutableDocument<Node, Element, Text> doc;
  Element root;

  /** Empty attributes map. */
  private static final Map<String, String> NA = new HashMap<String, String>();

  DocumentHandler<Node, Element, Text> handler = new DocumentHandler<Node, Element, Text>() {
    @Override
    public void onDocumentEvents(EventBundle<Node, Element, Text> eventBundle) {
      events = eventBundle;
    }
  };

  DocumentHandler<Node, Element, Text> handler2 = new DocumentHandler<Node, Element, Text>() {
    @Override
    public void onDocumentEvents(EventBundle<Node, Element, Text> eventBundle) {
      events2 = eventBundle;
    }
  };

  public void testDeletes() throws OperationException {
    for (int i = 0; i < 2; i++) {
      testNindoConsume = i == 0;
      doTestDeletes();
    }
  }

  @SuppressWarnings("unchecked")
  public void doTestDeletes() {
    String a = "<p>abc<b>def</b></p>";
    String b = "<p><b>deH</b>abG</p>";
    String txt = "alksjdflk";

    create(a);

    doc.deleteRange(l(0), l(doc.size()));
    checkEvents(cd(0, 10, dl(st("p", NA), tt("abc"), st("b", NA), tt("def"), et("b"), et("p"))));

    create(a + b);
    List<Element> deletions = new ArrayList<Element>();
    deletions.addAll(
        elementsOuter(doc, DocHelper.getFirstChildElement(doc, doc.getDocumentElement())));
    deletions.addAll(
        elementsOuter(doc, DocHelper.getLastChildElement(doc, doc.getDocumentElement())));

    doc.deleteRange(l(0), l(doc.size()));
    checkEvents(
        cd(0, 10, dl(st("p", NA), tt("abc"), st("b", NA), tt("def"), et("b"), et("p"))),
        cd(0, 10, dl(st("p", NA), st("b", NA), tt("deH"), et("b"), tt("abG"), et("p")))
    );
    checkDeletions(deletions);
    deletions.clear();

    create(txt);
    doc.deleteRange(l(0), l(doc.size()));
    checkEvents(td(0, txt));

    create(a + txt + b);
    deletions.addAll(
        elementsOuter(doc, DocHelper.getFirstChildElement(doc, doc.getDocumentElement())));
    deletions.addAll(
        elementsOuter(doc, DocHelper.getLastChildElement(doc, doc.getDocumentElement())));
    doc.deleteRange(l(0), l(doc.size()));
    checkEvents(
        cd(0, 10, dl(st("p", NA), tt("abc"), st("b", NA), tt("def"), et("b"), et("p"))),
        td(0, txt),
        cd(0, 10, dl(st("p", NA), st("b", NA), tt("deH"), et("b"), tt("abG"), et("p")))
    );
    checkDeletions(deletions);
    deletions.clear();
  }

  private <E> List<E> elements(ReadableDocument<? super E, E, ?> doc) {
    return elementsInner(doc, doc.getDocumentElement());
  }

  private <E> List<E> elementsInner(ReadableDocument<? super E, E, ?> doc, E e) {
    List<E> els = new ArrayList<E>();
    buildElements(doc, e, els);
    return els;
  }

  private <E> List<E> elementsOuter(ReadableDocument<? super E, E, ?> doc, E e) {
    List<E> els = new ArrayList<E>();
    els.add(e);
    buildElements(doc, e, els);
    return els;
  }

  private <E> void buildElements(ReadableDocument<? super E, E, ?> doc, E e, List<E> els) {
    E child = DocHelper.getFirstChildElement(doc, e);
    while (child != null) {
      els.add(child);
      buildElements(doc, child, els);
      child = DocHelper.getNextSiblingElement(doc, child);
    }
  }

  public void testAttributes() throws OperationException {
    for (int i = 0; i < 2; i++) {
      testNindoConsume = i == 0;
      doTestAttributes();
    }
  }

  @SuppressWarnings("unchecked")
  public void doTestAttributes() {
    String a = "<p>abc<b>def</b></p>";

    create(a);
    Element elem = (Element) doc.getDocumentElement().getFirstChild();

    Attributes attrs = attrs("x", "1", "y", "2", "z", "3");
    doc.setElementAttributes(elem, attrs);
    checkEvents(am(elem, pairs("x", null, "y", null, "z", null), attrs));

    Attributes updates = attrs("w", "4", "x", "1", "y", "5");
    doc.updateElementAttributes(elem, updates);
    // x omitted because no change.
    checkEvents(am(elem, pairs("w", null, "y", "2"), attrs("w", "4", "y", "5")));

    Map<String, String> updates2 = pairs("w", null);
    doc.updateElementAttributes(elem, updates2);
    // x omitted because no change.
    checkEvents(am(elem, pairs("w", "4"), pairs("w", null)));

    Attributes sets = attrs("x", "1", "y", "6", "v", "7");
    doc.setElementAttributes(elem, sets);
    // x omitted because no change.
    checkEvents(am(elem,
        pairs("y", "5", "z", "3", "v", null),
        pairs("y", "6", "z", null, "v", "7")));

    // tests lack of concurrent modification exception:
    Attributes sets2 = attrs("x", "1", "y", "6", "v", "7");
    doc.setElementAttributes(elem, sets2);
    checkEvents(am(elem, pairs(), pairs()));
  }

  public void testInserts() throws OperationException {
    for (int i = 0; i < 2; i++) {
      testNindoConsume = i == 0;
      doTestInserts();
    }
  }

  @SuppressWarnings("unchecked")
  public void doTestInserts() throws OperationException {
    String a = "<p>abc<b>def</b></p>";

    create(a);

    Element firstEl = (Element) doc.getFirstChild(doc.getDocumentElement());

    Builder b = at(0);
    Attributes attrs = attrs("x", "1", "y", "2");
    b.replaceAttributes(attrs);
    b.elementStart("x", attrs("a", "1"));
    b.characters("hello");
    b.elementStart("y", attrs("b", "2", "c", "3"));
    b.characters("yeah");
    b.elementEnd();
    b.elementEnd();
    String moreText = "more text";
    b.characters(moreText);
    consumeNindo(b.build());

    checkEvents(
        am(firstEl, pairs("x", null, "y", null), attrs),
        ci(doc.asElement(firstEl.getFirstChild())),
        ti(14, moreText));
  }

  public void testAdjacentContentInsertsAndDeletes1() throws OperationException {
    testNindoConsume = true;
    doTestAdjacentContentInsertsAndDeletes();
  }

  public void testAdjacentContentInsertsAndDeletes2() throws OperationException {
    testNindoConsume = false;
    doTestAdjacentContentInsertsAndDeletes();
  }

  @SuppressWarnings("unchecked")
  public void doTestAdjacentContentInsertsAndDeletes() throws OperationException {
    String a1 = "<zz><p x=\"y\">abc<b a=\"b\">def</b></p></zz>";
    String a = a1 + "<p>blah</p>";

    create(a);

    // GWT doesn't define subList.
    Element top = (Element) doc.getDocumentElement().getFirstChild();
    Node n1 = doc.getFirstChild(top);
    Node n2 = doc.getLastChild(top);
    List<Element> all = elementsInner(doc, top);
    List<Element> els = Arrays.asList(all.get(0), all.get(1));

    Builder b = at(0);
    Attributes attrs = attrs("x", "1", "y", "2");
    b.replaceAttributes(attrs);
    b.elementStart("x", attrs("a", "1"));
    b.characters("hello");
    b.elementStart("y", attrs("b", "2", "c", "3"));
    b.characters("yeah");
    b.elementEnd();
    b.elementEnd();
    String moreText = "more text";
    b.characters(moreText);
    b.deleteElementStart();
    b.deleteCharacters(3);
    b.deleteElementStart();
    b.deleteCharacters(3);
    b.deleteElementEnd();
    b.deleteElementEnd();
    String moreText2 = "more";
    b.characters(moreText2);
    consumeNindo(b.build());

    checkEvents(
        am(top, pairs("x", null, "y", null), attrs),
        ci(doc.asElement(top.getFirstChild())),
        ti(14, moreText),
        cd(23, 10, dl(st("p", attrs("x", "y")), tt("abc"), st("b", attrs("a", "b")),
            tt("def"), et("b"), et("p"))),
        ti(14 + moreText.length(), moreText2));
    checkDeletions(els);
  }

  private Attributes attrs(String ... strs) {
    return new AttributesImpl(pairs(strs));
  }

  private Map<String, String> pairs(String ... strs) {
    assert strs.length % 2 == 0;
    Map<String, String> map = new HashMap<String, String>();
    for (int i = 0; i < strs.length; i += 2) {
      map.put(strs[i], strs[i + 1]);
    }
    return map;
  }

  private DocumentEvent<Node, Element, Text> ti(int loc, String text) {
    return new TextInserted<Node, Element, Text>(loc, text);
  }

  private DocumentEvent<Node, Element, Text> ci(Element e) {
    return new ContentInserted<Node, Element, Text>(e);
  }

  private DocumentEvent<Node, Element, Text> am(Element e,
      Map<String, String> oldVals, Map<String, String> newVals) {
    return new AttributesModified<Node, Element, Text>(e, oldVals, newVals);
  }

  private DocumentEvent<Node, Element, Text> td(int loc, String text) {
    return new TextDeleted<Node, Element, Text>(loc, text);
  }

  private static List<ContentDeleted.Token> dl(ContentDeleted.Token... tokens) {
    List<ContentDeleted.Token> tokenList = new ArrayList<ContentDeleted.Token>();
    for (ContentDeleted.Token token : tokens) {
      tokenList.add(token);
    }
    return tokenList;
  }

  private static ContentDeleted.Token tt(String text) {
    return ContentDeleted.Token.textToken(text);
  }

  private static ContentDeleted.Token st(String tagName, Map<String, String> attributes) {
    return ContentDeleted.Token.elementStartToken(tagName, attributes);
  }

  private static ContentDeleted.Token et(String tagName) {
    return ContentDeleted.Token.elementEndToken(tagName);
  }

  private DocumentEvent<Node, Element, Text> cd(int loc, int size,
      List<ContentDeleted.Token> tokens) {
    return new ContentDeleted<Node, Element, Text>(loc, size, tokens, null);
  }

  private void checkDeletions(Collection<Element> expected) {
    for (Element expectedDeletion : expected) {
      assertTrue(getEvents().wasDeleted(expectedDeletion));
    }
    for (Element realDeletion : getEvents().getDeletedElements()) {
      assertTrue(expected.contains(realDeletion));
    }
  }

  private void checkEvents(Object ... expectedEvents) {
    assertEquals(flatten(Arrays.asList(expectedEvents)), getEvents().getEventComponents());
  }

  private List<Object> flatten(List<?> objects) {
    List<Object> list = new ArrayList<Object>();
    for (Object o : objects) {
      if (o instanceof List) {
        list.addAll(flatten((List<?>) o));
      } else {
        list.add(o);
      }
    }
    return list;
  }

  private Point<Node> l(int location) {
    if (location < 0) {
      location = doc.size() + location;
    }
    return doc.locate(location);
  }

  private void consumeNindo(Nindo op) throws OperationException {
    indexedCopy.consume(indexed.consumeAndReturnInvertible(op));
  }

  boolean testNindoConsume;

  private void create(String xml) {

    RawDocumentImpl raw = DocProviders.ROJO.parse("<d>" + xml + "</d>");
    RawDocumentImpl raw2 = DocProviders.ROJO.parse("<d>" + xml + "</d>");

    indexed = new ObservableIndexedDocument<Node, Element, Text, Void>(handler, raw, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    indexedCopy = new ObservableIndexedDocument<Node, Element, Text, Void>(
        handler2, raw2, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    doc = new MutableDocumentImpl<Node, Element, Text>(
        new OperationSequencer<Nindo>() {
          @Override
          public void begin() {
          }

          @Override
          public void consume(Nindo op) {
            try {
              consumeNindo(op);
            } catch (OperationException e) {
              throw new OperationRuntimeException("Bug!", e);
            }
          }

          @Override
          public void end() {
          }
        }, testNindoConsume ? indexed : indexedCopy);

    root = doc.getDocumentElement();
  }

  EventBundle<Node, Element, Text> getEvents() {
    return testNindoConsume ? events : events2;
  }

  private Builder at(int location) {
    Builder b = new Builder();
    if (location > 0) {
      b.skip(location);
    }
    return b;
  }
}
