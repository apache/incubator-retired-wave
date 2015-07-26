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

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.DocumentTestCases;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.Automatons;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Collections;
import java.util.Iterator;

/**
 * Tests for IndexedDocumentImpl.
 *
 */

public class IndexedDocumentImplTest extends TestCase {

  /**
   * A parser for documents.
   */
  public static final NindoTestCases.DocumentParser<
      IndexedDocumentImpl<Node, Element, Text, ?>> nindoDocumentParser =
    new NindoTestCases.DocumentParser<IndexedDocumentImpl<Node, Element, Text, ?>>() {

    public IndexedDocumentImpl<Node, Element, Text, ?> parseDocument(String documentString) {
      return doParseDocument(documentString);
    }

    public String asString(IndexedDocumentImpl<Node, Element, Text, ?> document) {
      return document.toXmlString();
    }

    @Override
    public IndexedDocumentImpl<Node, Element, Text, ?> copyDocument(
        IndexedDocumentImpl<Node, Element, Text, ?> other) {
      return doCopyDocument(other);
    }

  };

  /**
   * A parser for documents.
   */
  public static final DocumentTestCases.DocumentParser<
      IndexedDocumentImpl<Node, Element, Text, ?>> documentParser =
    new DocumentTestCases.DocumentParser<IndexedDocumentImpl<Node, Element, Text, ?>>() {

    public IndexedDocumentImpl<Node, Element, Text, ?> parseDocument(String documentString) {
      return doParseDocument(documentString);
    }

    public String asString(IndexedDocumentImpl<Node, Element, Text, ?> document) {
      return document.toXmlString();
    }

    @Override
    public IndexedDocumentImpl<Node, Element, Text, ?> copyDocument(
        IndexedDocumentImpl<Node, Element, Text, ?> other) {
      return doCopyDocument(other);
    }

  };

  private static IndexedDocumentImpl<Node, Element, Text, ?>
      doParseDocument(String documentString) {
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
      new IndexedDocumentImpl<Node, Element, Text, Void>(
          RawDocumentImpl.PROVIDER.parse("<blah>" + documentString + "</blah>"), null,
          DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    return doc;
  }

  private static IndexedDocumentImpl<Node, Element, Text, ?> doCopyDocument(
      IndexedDocumentImpl<Node, Element, Text, ?> other) {
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
      new IndexedDocumentImpl<Node, Element, Text, Void>(
          RawDocumentImpl.PROVIDER.create("doc", Attributes.EMPTY_MAP), null,
          DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    try {
      doc.consume(other.asOperation());
    } catch (OperationException e) {
      throw new OperationRuntimeException("Copy should not fail", e);
    }
    return doc;
  }

  /**
   * Runs the tests for the insertion of text.
   */
  public void testNindoTextInsertion() {
    NindoTestCases.runTextInsertionTests(nindoDocumentParser);
  }

  /**
   * Runs the tests for the deletion of text.
   */
  public void testNindoTextDeletion() {
    NindoTestCases.runTextDeletionTests(nindoDocumentParser);
  }

  /**
   * Runs the tests for the insertion of elements.
   */
  public void testNindoElementInsertion() {
    NindoTestCases.runElementInsertionTests(nindoDocumentParser);
  }

  /**
   * Runs the tests for the deletion of elements.
   */
  public void testNindoElementDeletion() {
    NindoTestCases.runElementDeletionTests(nindoDocumentParser);
  }

  /**
   * Runs the tests for the setting and removal of attributes.
   */
  public void testNindoAttributes() {
    NindoTestCases.runAttributeTests(nindoDocumentParser);
  }

  /**
   * Runs a miscellany of tests.
   */
  public void testNindoMiscellaneous() {
    NindoTestCases.runMiscellaneousTests(nindoDocumentParser);
  }

  /**
   * Runs the tests for the insertion of text.
   */
  public void testTextInsertion() {
    DocumentTestCases.runTextInsertionTests(documentParser);
  }

  /**
   * Runs the tests for the deletion of text.
   */
  public void testTextDeletion() {
    DocumentTestCases.runTextDeletionTests(documentParser);
  }

  /**
   * Runs the tests for the insertion of elements.
   */
  public void testElementInsertion() {
    DocumentTestCases.runElementInsertionTests(documentParser);
  }

  /**
   * Runs the tests for the deletion of elements.
   */
  public void testElementDeletion() {
    DocumentTestCases.runElementDeletionTests(documentParser);
  }

  /**
   * Runs the tests for the setting and removal of attributes.
   */
  public void testAttributes() {
    DocumentTestCases.runAttributeTests(documentParser);
  }

  /**
   * Runs a miscellany of tests.
   */
  public void testMiscellaneous() {
    DocumentTestCases.runMiscellaneousTests(documentParser);
  }

  /**
   * Tests the asOperation method.
   */
  public void testAsOperation() {
    IndexedDocumentImpl<Node, Element, Text, ?> document =
        documentParser.parseDocument(
          "<blip><p><i>ab</i>cd<b>ef</b>gh</p></blip>");
    DocInitialization expected = new DocInitializationBuilder()
        .elementStart("blip", Attributes.EMPTY_MAP)
        .elementStart("p", Attributes.EMPTY_MAP)
        .elementStart("i", Attributes.EMPTY_MAP)
        .characters("ab")
        .elementEnd()
        .characters("cd")
        .elementStart("b", Attributes.EMPTY_MAP)
        .characters("ef")
        .elementEnd()
        .characters("gh")
        .elementEnd()
        .elementEnd()
        .build();
    document.asOperation();
    assertEquals(
        DocOpUtil.toConciseString(expected),
        DocOpUtil.toConciseString(document.asOperation()));
  }

  private void checkApply(IndexedDocument<Node, Element, Text> doc, Nindo op)
      throws OperationException {

    System.out.println("");
    System.out.println("============================================");

    DocInitialization docAsOp = doc.asOperation();
    String initial = DocOpUtil.toXmlString(docAsOp);
    IndexedDocument<Node, Element, Text> copy = DocProviders.POJO.build(docAsOp,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    System.out.println(doc);

    DocOp docOp = doc.consumeAndReturnInvertible(op);

    System.out.println(op + "==========> " + docOp);
    ViolationCollector v = new ViolationCollector();
    if (!DocOpValidator.validate(v, DocumentSchema.NO_SCHEMA_CONSTRAINTS,
        Automatons.fromReadable(copy), docOp).isValid()) {
      v.printDescriptions(System.err);
      fail("Invalid operation");
    }

    copy.consume(docOp);

    System.out.println("=======" + doc + " --------- " + copy);
    assertEquals(
        DocOpUtil.toXmlString(doc.asOperation()),
        DocOpUtil.toXmlString(copy.asOperation()));
    DocOp inverted = DocOpInverter.invert(docOp);
    v = new ViolationCollector();
    if (!DocOpValidator.validate(v, DocumentSchema.NO_SCHEMA_CONSTRAINTS,
        Automatons.fromReadable(copy), inverted).isValid()) {
      v.printDescriptions(System.err);
      fail("Invalid operation");
    }
    copy.consume(inverted);
    assertEquals(initial, DocOpUtil.toXmlString(copy.asOperation()));
  }

  public void testReverseAnnotations() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("<a></a>");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.characters("x");
    b.endAnnotation("a");
    checkApply(doc, b.build());

    // mutating into:
    // <a>
    // x{a=2}
    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());

    // mutating into:
    // <a>
    // w{a=2}
    // x{a=2, b=1}
    // y{a=3, b=1}
    // z{a=3, b=2}
    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.characters("w");
    b.endAnnotation("a");
    b.startAnnotation("b", "1");
    b.skip(1);
    b.startAnnotation("a", "3");
    b.characters("y");
    b.startAnnotation("b", "2");
    b.characters("z");
    b.endAnnotation("a");
    b.endAnnotation("b");
    checkApply(doc, b.build());

    // mutating into:
    // <a>
    // y{a=4, b=1}
    b = new Nindo.Builder();
    b.skip(1);
    b.deleteCharacters(2);
    b.startAnnotation("a", "4");
    b.skip(1);
    b.deleteCharacters(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());

  }

  public void testAnnotationThroughInsertionEndingInDeletion() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());


    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.characters("x");
    b.deleteCharacters(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testAnnotationThroughInsertionFollowedByDeletion() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());


    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.characters("x");
    b.endAnnotation("a");
    b.deleteCharacters(1);
    checkApply(doc, b.build());
  }

  public void testInsertionThenDeletionWithAnnotations() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.skip(2);
    b.endAnnotation("a");
    checkApply(doc, b.build());


    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", null);
    b.characters("x");
    b.deleteCharacters(1);
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testReAnnotate() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "2");
    b.skip(1);
    b.startAnnotation("a", "3");
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "3");
    b.skip(2);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testEndBeforeAndStartAfterDeletion() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", null);
    b.skip(1);
    b.endAnnotation("a");
    b.deleteCharacters(1);
    b.startAnnotation("a", "1");
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testEndBeforeAndStartAfterDeletionThenInsertion() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", null);
    b.skip(1);
    b.endAnnotation("a");
    b.deleteCharacters(1);
    b.startAnnotation("a", "1");
    b.characters("x");
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testChangeBetweenInsertionAndDeletion() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.characters("x");
    b.startAnnotation("a", "2");
    b.deleteCharacters(1);
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testOpenClose() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.startAnnotation("b", "2");
    b.startAnnotation("c", "3");
    b.endAnnotation("a");
    b.endAnnotation("c");
    b.endAnnotation("b");
    checkApply(doc, b.build());
  }

  public void testOpenInsertOpenClose() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.characters("xyz");
    b.startAnnotation("a", "1");
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testOpenDuringInsertionThenUpdate() throws OperationException {
    IndexedDocument<Node, Element, Text> doc =
	DocProviders.POJO.parse("<q><r/></q>abcdefghijkl");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.startAnnotation("a", "1");
    b.skip(7);
    b.endAnnotation("a");
    checkApply(doc, b.build());

    b = new Nindo.Builder();
    b.elementStart("p", Attributes.EMPTY_MAP);
    b.startAnnotation("a", null);
    b.elementEnd();
    b.updateAttributes(Collections.singletonMap("u", "v"));
    b.replaceAttributes(new AttributesImpl("v", "u"));
    b.skip(1);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testOpenDuringInsertionThenUpdate2() throws OperationException {
    IndexedDocument<Node, Element, Text> doc =
	DocProviders.POJO.parse("abcdef<q><r/></q>ghijkl");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(8);
    b.startAnnotation("a", "1");
    b.skip(5);
    b.endAnnotation("a");
    checkApply(doc, b.build());

    b = new Nindo.Builder();
    b.startAnnotation("a", "1");
    b.skip(7);
    b.updateAttributes(Collections.singletonMap("u", "v"));
    //b.replaceAttributes(new AttributesImpl("v", "u"));
    b.skip(3);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testDeletionResets() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefghijkl");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.startAnnotation("a", "1");
    b.skip(3);
    b.deleteCharacters(3);
    b.skip(3);
    b.endAnnotation("a");
    checkApply(doc, b.build());
  }

  public void testRedundantAnnotationsPreserved() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefg");
    IndexedDocument<Node, Element, Text> doc2 = DocProviders.POJO.parse("abcdefg");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.startAnnotation("a", "1");
    b.skip(7);
    b.endAnnotation("a");
    checkApply(doc2, b.build());

    b = new Nindo.Builder();
    b.startAnnotation("a", null);
    b.skip(2);
    b.startAnnotation("a", "2");
    b.skip(2);
    b.startAnnotation("a", null);
    b.skip(3);
    b.endAnnotation("a");
    DocOp docOp = doc.consumeAndReturnInvertible(b.build());

    doc2.consumeAndReturnInvertible(Nindo.fromDocOp(docOp, true));
    assertEquals(
        DocOpUtil.toXmlString(doc.asOperation()),
        DocOpUtil.toXmlString(doc2.asOperation()));
  }

  public void testNoRedundantSkips() throws OperationException {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abcdefghijkl");

    Nindo.Builder b;

    b = new Nindo.Builder();
    b.skip(1);
    b.startAnnotation("a", "1");
    b.skip(1);
    b.startAnnotation("b", "1");
    b.skip(1);
    b.endAnnotation("a");
    b.skip(1);
    b.startAnnotation("c", "1");
    b.skip(1);
    b.endAnnotation("c");
    b.skip(1);
    b.endAnnotation("b");
    b.skip(1);
    b.startAnnotation("c", "1");
    b.skip(1);
    b.endAnnotation("c");
    checkApply(doc, b.build());

    b = new Nindo.Builder();
    b.startAnnotation("z", "1");
    b.skip(doc.size());
    b.endAnnotation("z");
    DocOp docOp = doc.consumeAndReturnInvertible(b.build());
    assertEquals(3, docOp.size());
  }

  public void testBug1() throws OperationException {
    IndexedDocumentImpl<Node, Element, Text, ?> d = nindoDocumentParser.parseDocument(
      "<a>a</a>");
    Nindo.Builder b = new Nindo.Builder();
    b.skip(1);
    b.deleteCharacters(1);
    checkApply(d, b.build());
  }


//
//  public void testReverseBug1() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.startAnnotation("b", "3");
//    d.characters("abc");
//    d.endAnnotation("b");
//    d.elementEnd();
//    d.finish();
//
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//    String beforeXml = OperationXmlifier.xmlify(d);
//
//    d.begin();
//    d.skip(2);
//    d.startAnnotation("a", "2");
//    d.characters("abcd");
//    d.deleteCharacters(1);
//    d.endAnnotation("a");
//    d.finish();
//
//    String afterXml = OperationXmlifier.xmlify(d);
//    DocumentMutation reverse = reverseSink.operation;
//    reverse.apply(d);
//    String reversedXml = OperationXmlifier.xmlify(d);
//
//    assertEquals(beforeXml, reversedXml);
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(2);
//    r.deleteCharacters(4);
//    r.startAnnotation("a", null);
//    r.startAnnotation("b", "3");
//    r.characters("b");
//    r.endAnnotation("a");
//    r.endAnnotation("b");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverse.apply(checker);
//  }
//
//  public void testReverseBug2() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.characters("ababa");
//    d.startAnnotation("e", "2");
//    d.characters("d");
//    d.startAnnotation("a", "1");
//    d.characters("abcd");
//    d.endAnnotation("a");
//    d.characters("babc");
//    d.endAnnotation("e");
//    d.characters("de");
//    d.elementEnd();
//    d.finish();
//
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//
//    d.begin();
//    d.skip(1);
//    d.skip(14);
//    d.deleteCharacters(1);
//    d.startAnnotation("d", "2");
//    d.startAnnotation("b", null);
//    d.endAnnotation("d");
//    d.endAnnotation("b");
//    d.finish();
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(15);
//    r.startAnnotation("a", null);
//    r.startAnnotation("e", null);
//    r.characters("d");
//    r.endAnnotation("a");
//    r.endAnnotation("e");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverseSink.operation.apply(checker);
//  }
//
//  public void testReverseBug3() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.characters("babcdefabcdabfabcdefabcdefghabcdefgh");
//    d.startAnnotation("d", "3");
//    d.characters("gab");
//    d.startAnnotation("e", "1");
//    d.characters("gababcabcefghidefghefaababcdefghiabcdefgh");
//    d.endAnnotation("d");
//    d.characters("defghi");
//    d.startAnnotation("a", "1");
//    d.characters("abcd");
//    d.endAnnotation("e");
//    d.characters("efg");
//    d.endAnnotation("a");
//    d.characters("cdefe");
//    d.startAnnotation("b", "3");
//    d.characters("f");
//    d.endAnnotation("b");
//    d.elementEnd();
//    d.finish();
//
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//
//    String beforeXml = OperationXmlifier.xmlify(d);
//
//    d.begin();
//    d.skip(1);
//    d.skip(15);
//    d.startAnnotation("e", "1");
//    d.skip(3);
//    d.characters("abcd");
//    d.skip(2);
//    d.characters("abcdefgh");
//    d.deleteCharacters(1);
//    d.startAnnotation("a", "2");
//    d.skip(24);
//    d.startAnnotation("e", "3");
//    d.skip(4);
//    d.characters("abcd");
//    d.startAnnotation("a", "1");
//    d.deleteCharacters(1);
//    d.skip(3);
//    d.characters("abcdefghi");
//    d.deleteCharacters(1);
//    d.skip(13);
//    d.startAnnotation("b", "1");
//    d.endAnnotation("e");
//    d.endAnnotation("b");
//    d.endAnnotation("a");
//    d.finish();
//
//    String afterXml = OperationXmlifier.xmlify(d);
//    DocumentMutation reverse = reverseSink.operation;
//    reverse.apply(d);
//    String reversedXml = OperationXmlifier.xmlify(d);
//
//    assertEquals(beforeXml, reversedXml);
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(16);
//    r.startAnnotation("e", null);
//    r.skip(3);
//    r.deleteCharacters(4);
//    r.skip(2);
//    r.deleteCharacters(8);
//    r.startAnnotation("a", null);
//    r.startAnnotation("b", null);
//    r.startAnnotation("d", null);
//    r.characters("a");
//    r.endAnnotation("b");
//    r.endAnnotation("d");
//    r.skip(18);
//    r.endAnnotation("e");
//    r.skip(6);
//    r.startAnnotation("e", "1");
//    r.skip(4);
//    r.deleteCharacters(4);
//    r.startAnnotation("b", null);
//    r.startAnnotation("d", "3");
//    r.characters("f");
//    r.endAnnotation("b");
//    r.endAnnotation("d");
//    r.skip(3);
//    r.deleteCharacters(9);
//    r.startAnnotation("b", null);
//    r.startAnnotation("d", "3");
//    r.characters("d");
//    r.endAnnotation("b");
//    r.endAnnotation("d");
//    r.skip(13);
//    r.endAnnotation("a");
//    r.endAnnotation("e");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverse.apply(checker);
//
//    assertEquals(beforeXml, reversedXml);
//  }
//
//  public void testReverseBug4() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.characters("a");
//    d.startAnnotation("d", "2");
//    d.characters("bc");
//    d.endAnnotation("d");
//    d.elementEnd();
//    d.finish();
//
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//
//    String beforeXml = OperationXmlifier.xmlify(d);
//
//    d.begin();
//    d.skip(1);
//    d.startAnnotation("e", "3");
//    d.skip(1);
//    d.endAnnotation("e");
//    d.characters("x");
//    d.deleteCharacters(1);
//    d.finish();
//
//    String afterXml = OperationXmlifier.xmlify(d);
//    DocumentMutation reverse = reverseSink.operation;
//    reverse.apply(d);
//    String reversedXml = OperationXmlifier.xmlify(d);
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(1);
//    r.startAnnotation("e", null);
//    r.skip(1);
//    r.deleteCharacters(1);
//    r.startAnnotation("d", "2");
//    r.characters("b");
//    r.endAnnotation("d");
//    r.endAnnotation("e");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverse.apply(checker);
//
//    assertEquals(beforeXml, reversedXml);
//  }
//
//  public void testReverseBug5() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.startAnnotation("e", "1");
//    d.characters("aab");
//    d.endAnnotation("e");
//    d.characters("c");
//    d.elementEnd();
//    d.finish();
//
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//
//    String beforeXml = OperationXmlifier.xmlify(d);
//
//    d.begin();
//    d.skip(1);
//    d.skip(1);
//    d.startAnnotation("e", "2");
//    d.characters("a");
//    d.deleteCharacters(1);
//    d.skip(1);
//    d.deleteCharacters(1);
//    d.endAnnotation("e");
//    d.finish();
//
//    String afterXml = OperationXmlifier.xmlify(d);
//    DocumentMutation reverse = reverseSink.operation;
//    reverse.apply(d);
//    String reversedXml = OperationXmlifier.xmlify(d);
//
//    assertEquals(beforeXml, reversedXml);
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(2);
//    r.deleteCharacters(1);
//    r.startAnnotation("e", "1");
//    r.characters("a");
//    r.skip(1);
//    r.startAnnotation("e", null);
//    r.characters("c");
//    r.endAnnotation("e");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverse.apply(checker);
//  }
//
//  public void testReverseBug6() throws OperationException {
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.BUILDER,
//          new AnnotationTree<Object>("a", "b", null));
//    d.begin();
//    d.elementStart("a", Attributes.EMPTY_MAP);
//    d.startAnnotation("d", "2");
//    d.characters("a");
//    d.startAnnotation("e", "1");
//    d.characters("b");
//    d.endAnnotation("d");
//    d.endAnnotation("e");
//    d.characters("b");
//    d.elementEnd();
//    d.finish();
//    OperationContainer reverseSink = new OperationContainer();
//    d.registerReverseSink(reverseSink);
//
//    String beforeXml = OperationXmlifier.xmlify(d);
//
//    d.begin();
//    d.skip(1);
//    d.startAnnotation("e", null);
//    d.skip(2);
//    d.endAnnotation("e");
//    d.deleteCharacters(1);
//    d.finish();
//
//    String afterXml = OperationXmlifier.xmlify(d);
//    DocumentMutation reverse = reverseSink.operation;
//    reverse.apply(d);
//    String reversedXml = OperationXmlifier.xmlify(d);
//
//    assertEquals(beforeXml, reversedXml);
//
//    DocumentOperationChecker.Recorder r = new DocumentOperationChecker.Recorder();
//    r.begin();
//    r.skip(2);
//    r.startAnnotation("e", "1");
//    r.skip(1);
//    r.startAnnotation("d", null);
//    r.startAnnotation("e", null);
//    r.characters("b");
//    r.endAnnotation("d");
//    r.endAnnotation("e");
//    r.finish();
//    DocumentOperationChecker checker = r.finishRecording();
//    reverse.apply(checker);
//  }
//
//  public void testConcurrentModificationException() throws OperationException {
//    // The test is that this doesn't throw a ConcurrentModificationException.
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.PROVIDER,
//          new AnnotationTree<Object>("a", "b", null));
//    // initial
//    d.begin();
//    d.elementStart("blip", Attributes.EMPTY_MAP);
//    {
//      Map<String, String> a = new HashMap<String, String>();
//      a.put("_t", "title");
//      a.put("t", "h1");
//      d.elementStart("p", new Attributes(a));
//    }
//    d.elementEnd();
//    d.elementEnd();
//    d.finish();
//    // mutation
//    d.begin();
//    d.skip(1);
//    d.setAttributes(new Attributes("_t", "title"));
//    d.finish();
//  }
//
//  public void testNPE1() throws OperationException {
//    // The test is that this doesn't throw a NullPointerException.
//    IndexedDocumentImpl<Node, Element, Text, ?> d =
//      new IndexedDocumentImpl<Node, Element, Text, Void>(
//          RawDocumentImpl.PROVIDER,
//          new AnnotationTree<Object>("a", "b", null));
//
//    // initialization steps
//    d.begin();
//    d.elementStart("blip", Attributes.EMPTY_MAP);
//    d.elementStart("p", new Attributes("_t", "title"));
//
//    d.elementEnd();
//    d.elementStart("p", Attributes.EMPTY_MAP);
//    d.characters("a");
//    d.elementEnd();
//    d.elementStart("p", Attributes.EMPTY_MAP);
//
//    d.elementEnd();
//    d.elementStart("p", new Attributes("_t", "title"));
//    d.elementEnd();
//    d.elementStart("p", Attributes.EMPTY_MAP);
//    d.elementEnd();
//    d.elementEnd();
//    d.finish();
//
//    d.begin();
//    d.skip(2);
//    d.deleteAntiElementStart();
//    d.deleteElementStart();
//    d.deleteCharacters(1);
//    d.deleteElementEnd();
//    d.deleteAntiElementEnd(new Attributes("t", ""));
//    d.finish();
//
//    d.begin();
//    d.skip(1);
//    d.deleteElementStart();
//    d.deleteElementEnd();
//    d.finish();
//
//    d.begin();
//    d.skip(5);
//    d.elementStart("p", Attributes.EMPTY_MAP);
//    d.elementEnd();
//    d.finish();
//
//    // mutation that crashes
//    // current state: <blip><p _t=title></p><p></p><p></p></blip>
//    d.begin();
//    d.skip(4);
//    d.deleteAntiElementStart();
//    d.deleteAntiElementEnd(Attributes.EMPTY_MAP);
//    d.antiElementStart();
//    d.antiElementEnd(Attributes.EMPTY_MAP);
//    d.finish();
//  }

  public void testAnnotationIntervalIterator() throws OperationException {
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(
            RawDocumentImpl.PROVIDER.parse("<doc><x><p>abcdefgh</p></x></doc>"),
            new AnnotationTree<Object>("a", "b", null), DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    // 1-3: a=1, c=1
    // 3-5: a=1, b=1, c=1
    // 5-6: a=2, b=1, c=1
    // 6-8: b=1, c=1
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(1, 5, "a", "1"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(5, 6, "a", "2"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(3, 8, "b", "1"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(1, 8, "c", "1"));

    {
      Iterator<AnnotationInterval<String>> iterator =
        doc.annotationIntervals(2, 10, CollectionUtils.newStringSet("a")).iterator();
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(2, i.start());
        assertEquals(5, i.end());
        assertEquals(1, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("1", i.annotations().get("a", "x"));
        assertEquals(0, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("x", i.diffFromLeft().get("a", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(5, i.start());
        assertEquals(6, i.end());
        assertEquals(1, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("2", i.annotations().get("a", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("2", i.diffFromLeft().get("a", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(6, i.start());
        assertEquals(10, i.end());
        assertEquals(1, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals(null, i.annotations().get("a", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals(null, i.diffFromLeft().get("a", "x"));
      }
      assertFalse(iterator.hasNext());
    }

    // 1-3: a=1, c=1
    // 3-5: a=1, b=1, c=1
    // 5-6: a=2, b=1, c=1
    // 6-8: b=1, c=1
    {
      Iterator<AnnotationInterval<String>> iterator =
        doc.annotationIntervals(2, 10, null).iterator();
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(2, i.start());
        assertEquals(3, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("1", i.annotations().get("a", "x"));
        assertEquals(null, i.annotations().get("b", "x"));
        assertEquals("1", i.annotations().get("c", "x"));
        assertEquals(0, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("x", i.diffFromLeft().get("a", "x"));
        assertEquals("x", i.diffFromLeft().get("b", "x"));
        assertEquals("x", i.diffFromLeft().get("c", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(3, i.start());
        assertEquals(5, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("1", i.annotations().get("a", "x"));
        assertEquals("1", i.annotations().get("b", "x"));
        assertEquals("1", i.annotations().get("c", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("x", i.diffFromLeft().get("a", "x"));
        assertEquals("1", i.diffFromLeft().get("b", "x"));
        assertEquals("x", i.diffFromLeft().get("c", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(5, i.start());
        assertEquals(6, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("2", i.annotations().get("a", "x"));
        assertEquals("1", i.annotations().get("b", "x"));
        assertEquals("1", i.annotations().get("c", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("2", i.diffFromLeft().get("a", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(6, i.start());
        assertEquals(8, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals(null, i.annotations().get("a", "x"));
        assertEquals("1", i.annotations().get("b", "x"));
        assertEquals("1", i.annotations().get("c", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals(null, i.diffFromLeft().get("a", "x"));
        assertEquals("x", i.diffFromLeft().get("b", "x"));
        assertEquals("x", i.diffFromLeft().get("c", "x"));
      }
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(8, i.start());
        assertEquals(10, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals(null, i.annotations().get("a", "x"));
        assertEquals(null, i.annotations().get("b", "x"));
        assertEquals(null, i.annotations().get("c", "x"));
        assertEquals(2, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("x", i.diffFromLeft().get("a", "x"));
        assertEquals(null, i.diffFromLeft().get("b", "x"));
        assertEquals(null, i.diffFromLeft().get("c", "x"));
      }
      assertFalse(iterator.hasNext());
    }

    // 1-3: a=1, c=1
    // 3-5: a=1, b=1, c=1
    // 5-6: a=2, b=1, c=1
    // 6-8: b=1, c=1
    {
      Iterator<AnnotationInterval<String>> iterator =
        doc.annotationIntervals(3, 4, null).iterator();
      {
        AnnotationInterval<String> i = iterator.next();
        assertEquals(3, i.start());
        assertEquals(4, i.end());
        assertEquals(3, CollectionUtils.newJavaMap(i.annotations()).size());
        assertEquals("1", i.annotations().get("a", "x"));
        assertEquals("1", i.annotations().get("b", "x"));
        assertEquals("1", i.annotations().get("c", "x"));
        assertEquals(1, CollectionUtils.newJavaMap(i.diffFromLeft()).size());
        assertEquals("x", i.diffFromLeft().get("a", "x"));
        assertEquals("1", i.diffFromLeft().get("b", "x"));
        assertEquals("x", i.diffFromLeft().get("c", "x"));
      }
      assertFalse(iterator.hasNext());
    }

    {
      Iterator<AnnotationInterval<String>> iterator =
        doc.annotationIntervals(3, 3, null).iterator();
      assertFalse(iterator.hasNext());
    }
  }

  public void testRangedAnnotationIterator() throws OperationException {
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(
            RawDocumentImpl.PROVIDER.parse("<doc><x><p>abcdefgh</p></x></doc>"),
            new AnnotationTree<Object>("a", "b", null), DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    // 1-3: a=1, c=1
    // 3-5: a=1, b=1, c=1
    // 5-6: a=2, b=1, c=1
    // 6-8: b=1, c=1
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(1, 5, "a", "1"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(5, 6, "a", "2"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(3, 8, "b", "1"));
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(1, 8, "c", "1"));

    {
      Iterator<RangedAnnotation<String>> iterator =
        doc.rangedAnnotations(2, 10, CollectionUtils.newStringSet("a")).iterator();
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals("1", r.value());
        assertEquals(1, r.start());
        assertEquals(5, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals("2", r.value());
        assertEquals(5, r.start());
        assertEquals(6, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals(null, r.value());
        assertEquals(6, r.start());
        assertEquals(12, r.end());
      }
      assertFalse(iterator.hasNext());
    }

    {
      Iterator<RangedAnnotation<String>> iterator =
        doc.rangedAnnotations(2, 10, null).iterator();
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("b", r.key());
        assertEquals(null, r.value());
        assertEquals(0, r.start());
        assertEquals(3, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        RangedAnnotation<String> r2 = iterator.next();
        // Order of these two ranges is unspecified; normalize it.
        if ("c".equals(r.key())) {
          RangedAnnotation<String> tmp = r2;
          r2 = r;
          r = tmp;
        }

        assertEquals("a", r.key());
        assertEquals("1", r.value());
        assertEquals(1, r.start());
        assertEquals(5, r.end());

        assertEquals("c", r2.key());
        assertEquals("1", r2.value());
        assertEquals(1, r2.start());
        assertEquals(8, r2.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("b", r.key());
        assertEquals("1", r.value());
        assertEquals(3, r.start());
        assertEquals(8, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals("2", r.value());
        assertEquals(5, r.start());
        assertEquals(6, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals(null, r.value());
        assertEquals(6, r.start());
        assertEquals(12, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        RangedAnnotation<String> r2 = iterator.next();
        // Order of these two ranges is unspecified; normalize it.
        if ("c".equals(r.key())) {
          RangedAnnotation<String> tmp = r2;
          r2 = r;
          r = tmp;
        }

        assertEquals("b", r.key());
        assertEquals(null, r.value());
        assertEquals(8, r.start());
        assertEquals(12, r.end());

        assertEquals("c", r2.key());
        assertEquals(null, r2.value());
        assertEquals(8, r2.start());
        assertEquals(12, r2.end());
      }
      assertFalse(iterator.hasNext());
    }

    {
      Iterator<AnnotationInterval<String>> iterator =
        doc.annotationIntervals(3, 3, null).iterator();
      assertFalse(iterator.hasNext());
    }
  }

  public void testRangedAnnotationIteratorWithNonStrings() throws OperationException {
    AnnotationTree<Object> annotations = new AnnotationTree<Object>(
        "a", "b", null);
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(
            RawDocumentImpl.PROVIDER.parse("<doc><x><p>abcdefgh</p></x></doc>"), annotations,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    // 1-3: a=1, @c=Object
    // 3-5: a=1
    doc.consumeAndReturnInvertible(Nindo.setAnnotation(1, 5, "a", "1"));
    annotations.begin();
    annotations.skip(1);
    String c = Annotations.makeLocal("c");
    annotations.startAnnotation(c,  new Object());
    annotations.skip(2);
    annotations.endAnnotation(c);
    annotations.finish();

    {
      Iterator<RangedAnnotation<String>> iterator =
          doc.rangedAnnotations(2, 10, CollectionUtils.newStringSet("a")).iterator();
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals("1", r.value());
        assertEquals(1, r.start());
        assertEquals(5, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals(null, r.value());
        assertEquals(5, r.start());
        assertEquals(12, r.end());
      }
      assertFalse(iterator.hasNext());
    }
    {
      Iterator<RangedAnnotation<String>> iterator =
          doc.rangedAnnotations(2, 10, null).iterator();
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals("1", r.value());
        assertEquals(1, r.start());
        assertEquals(5, r.end());
      }
      {
        RangedAnnotation<String> r = iterator.next();
        assertEquals("a", r.key());
        assertEquals(null, r.value());
        assertEquals(5, r.start());
        assertEquals(12, r.end());
      }
      assertFalse(iterator.hasNext());
    }
    {
      Iterator<AnnotationInterval<String>> iterator =
          doc.annotationIntervals(3, 3, null).iterator();
      assertFalse(iterator.hasNext());
    }
  }

  public void testSplitTextNeverReturnsSibling() {
    TestDocumentContext<Node, Element, Text> cxt = ContextProviders.createTestPojoContext(
        DocProviders.POJO.parse("ab").asOperation(),
        null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    TextNodeOrganiser<Text> organiser = cxt.textNodeOrganiser();
    MutableDocument<Node, Element, Text> doc = cxt.document();
    Text first = (Text) doc.getFirstChild(doc.getDocumentElement());
    Text text = organiser.splitText(first, 1);
    LocalDocument<Node, Element, Text> local = cxt.annotatableContent();

    Element tr = local.transparentCreate("l", Attributes.EMPTY_MAP, doc.getDocumentElement(), text);
    local.transparentMove(tr, text, null, null);

    assertNull(cxt.getIndexedDoc().splitText(first, 1));
    assertNull(organiser.splitText(first, 1));

    assertSame(first, organiser.splitText(first, 0));
    assertSame(first, organiser.splitText(first, 0));

    assertEquals("a<l>b</l>", XmlStringBuilder.innerXml(local).toString());
    assertEquals("ab", XmlStringBuilder.innerXml(doc).toString());
  }

  public void testCantGetLocationOfInvalidNode() throws OperationException {
    AnnotationTree<Object> annotations = new AnnotationTree<Object>(
        "a", "b", null);
    RawDocumentImpl rawDoc = RawDocumentImpl.PROVIDER.parse("<doc><p></p></doc>");
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(rawDoc, annotations,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    Node element = doc.getDocumentElement().getFirstChild();

    // element is valid
    assertEquals(0, doc.getLocation(element));

    doc.consumeAndReturnInvertible(Nindo.deleteElement(0));

    // element was deleted
    try {
      doc.getLocation(element);
      fail("Expected: IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // OK
    }

    // element that was never valid to begin with
    try {
      doc.getLocation(rawDoc.createElement("abc", Collections.<String, String>emptyMap(),
          doc.getDocumentElement(), null));
      fail("Expected: IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // OK
    }
  }
}
