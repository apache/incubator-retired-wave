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

import junit.framework.Assert;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.NindoSink;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for applying operations to documents.
 *
 */
public class NindoTestCases {

  /**
   * A parser that can serialise documents to strings and deserialise documents from strings.
   *
   * @param <D> The type of the document.
   */
  public interface DocumentParser<D extends NindoSink & ModifiableDocument> {

    /**
     * Deserialise the document from an XML string.
     *
     * @param documentString The XML string.
     * @return The document.
     */
    D parseDocument(String documentString);

    /**
     * @return a copy of the given document
     */
    D copyDocument(D other);

    /**
     * Serialise the document to an XML string.
     *
     * @param document The document.
     * @return The XML string.
     */
    String asString(D document);

  }

  private static final class TestComponent {

    final Nindo mutation;
    final List<String> expectedResultAlternatives = new ArrayList<String>();

    TestComponent(Nindo mutation, String... expectedResultAlternatives) {
      this.mutation = mutation;
      for (String expected : expectedResultAlternatives) {
        this.expectedResultAlternatives.add(expected);
      }
    }

    <D extends NindoSink & ModifiableDocument> void run(D document, DocumentParser<D> parser) {
      String initialState = parser.asString(document);
      D copy = parser.copyDocument(document);
      DocOp docOp = null;

      Assert.assertEquals("Copy did not work",
          initialState, parser.asString(copy));

      try {
        docOp = document.consumeAndReturnInvertible(mutation);
        copy.consume(docOp);
      } catch (OperationException e) {
        Assert.fail("An OperationException was thrown: " + e);
      }

      String computedDocument = parser.asString(document);
      String message = "Computed: " + computedDocument + " Expected: "
          + expectedResultAlternatives + " Nindo: " + mutation
          + " Op: " + DocOpUtil.toConciseString(docOp);
      Assert.assertTrue(message, expectedResultAlternatives.contains(computedDocument));

      Assert.assertEquals("Generated invertible op not equivalent to non-invertible one",
          parser.asString(document), parser.asString(copy));

      try {
        copy.consume(DocOpInverter.invert(docOp));
      } catch (OperationException e) {
        Assert.fail("An OperationException was thrown: " + e);
      }
      Assert.assertEquals("Inversion of generated invertible op is incorrect",
          initialState, parser.asString(copy));
    }

  }

  private static final class TestParameters {

    private final String initialDocument;
    private final List<TestComponent> testComponents;

    TestParameters(Nindo documentMutation, String initialDocument,
        String... finalDocuments) {
      this.initialDocument = initialDocument;
      testComponents =
          Collections.singletonList(new TestComponent(documentMutation, finalDocuments));
    }

    TestParameters(String initialDocument, TestComponent... testComponents) {
      this.initialDocument = initialDocument;
      this.testComponents = Arrays.asList(testComponents);
    }

    <D extends NindoSink & ModifiableDocument> void run(DocumentParser<D> parser) {
      D document = parser.parseDocument(initialDocument);
      for (TestComponent component : testComponents) {
        component.run(document, parser);
      }
    }
  }

  /**
   * Tests for insertion of text.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> INSERT_TEXT_TESTS = Arrays.asList(
      new TestParameters(
          Nindo.insertCharacters(1, "ab"),
          "<p></p>", "<p>ab</p>"
      ),
      new TestParameters(
          Nindo.insertCharacters(2, "12"),
          "<p>abcde</p>", "<p>a12bcde</p>"
      ),
      new TestParameters(
          Nindo.insertCharacters(6, "12"),
          "<p>abcde</p>", "<p>abcde12</p>"
      ),
      new TestParameters(
          Nindo.insertCharacters(5, "12"),
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>fg</i>" +
            "</b>" +
            " h" +
          "</p>",
          "<p>" +
            "<i>ab</i>" +
            "12cd " +
            "<b>" +
              "e" +
              "<i>fg</i>" +
            "</b>" +
            " h" +
          "</p>"
      ),
      new TestParameters(
          Nindo.insertCharacters(12, " 12 "),
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>fg</i>" +
            "</b>" +
            " h" +
          "</p>",
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>f 12 g</i>" +
            "</b>" +
            " h" +
          "</p>"
      )
  );

  /**
   * Tests for deletion of text.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> DELETE_TEXT_TESTS = Arrays.asList(
      new TestParameters(
          createDeletion(1, 3),
          "<p>ab</p>", "<p></p>", "<p/>"
      ),
      new TestParameters(
          createDeletion(2, 5),
          "<p>abcde</p>", "<p>ae</p>"
      ),
      new TestParameters(
          createDeletion(4, 6),
          "<p>ab<i>cd</i>ef</p>", "<p>ab<i></i>ef</p>", "<p>ab<i/>ef</p>"
      ),
      new TestParameters(
          createDeletion(2, 3, 4, 5),
          "<p>ab<i>cd</i>ef</p>", "<p>a<i>d</i>ef</p>"
      ),
      new TestParameters(
          createDeletion(5, 6, 7, 8),
          "<p>ab<i>cd</i>ef</p>", "<p>ab<i>c</i>f</p>"
      ),
      new TestParameters(
          createDeletion(3, 4, 6, 7),
          "<p><i>ab</i><i>cd</i></p>", "<p><i>a</i><i>d</i></p>"
      ),
      new TestParameters(
          createDeletion(5, 6, 7, 8, 10, 11),
          "<p><i>a<b>ef</b>b</i><i>cd</i></p>", "<p><i>a<b>e</b></i><i>d</i></p>"
      ),
      new TestParameters(
          createDeletion(3, 4, 6, 7, 8, 9),
          "<p><i>ab</i><i>c<b>ef</b>d</i></p>", "<p><i>a</i><i><b>f</b>d</i></p>"
      ),
      new TestParameters(
          createDeletion(5, 6, 7, 8, 10, 11, 12, 13),
          "<p><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i></p>", "<p><i>a<b>e</b></i><i><b>f</b>d</i></p>"
      ),
      new TestParameters(
          createDeletion(4, 6, 7, 8, 10, 11),
          "<p><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i></p>",
          "<p><i>a<b></b></i><i><b>ef</b>d</i></p>", "<p><i>a<b/></i><i><b>ef</b>d</i></p>"
      ),
      new TestParameters(
          createDeletion(7, 8, 10, 11, 12, 14),
          "<p><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i></p>",
          "<p><i>a<b>ef</b></i><i><b></b>d</i></p>", "<p><i>a<b>ef</b></i><i><b/>d</i></p>"
      )
  );

  /**
   * Tests for insertion of elements.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> INSERT_ELEMENT_TESTS = Arrays.asList(
      new TestParameters(
          structuralSample1(1),
          "<p>abc</p>", "<p>12<u>34</u>56abc</p>"
      ),
      new TestParameters(
          structuralSample1(4),
          "<p>abc</p>", "<p>abc12<u>34</u>56</p>"
      ),
      new TestParameters(
          structuralSample1(3),
          "<p>abc</p>", "<p>ab12<u>34</u>56c</p>"
      ),
      new TestParameters(
          structuralSample1(5),
          "<p>a<b>b</b><i>c</i></p>", "<p>a<b>b</b>12<u>34</u>56<i>c</i></p>"
      ),
      new TestParameters(
          structuralSample2(3),
          "<p>abc</p>", "<p>ab12<u>3<i>hello</i><b>world</b>4</u>56c</p>"
      ),
      new TestParameters(
          structuralSample3(3),
          "<p>abc</p>",
          "<p>ab12<u>3<i></i><b></b>4</u>56c</p>", "<p>ab12<u>3<i/><b/>4</u>56c</p>"
      ),
      new TestParameters(
          structuralSample4(3),
          "<p>abc</p>", "<p>ab<i>hello</i><b>world</b>c</p>"
      ),
      new TestParameters(
          structuralSample5(3),
          "<p>abc</p>", "<p>ab<a href=\"http://www.google.com/\">google</a>c</p>"
      ),
      new TestParameters(
          structuralSample1(12),
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>fg</i>" +
            "</b>" +
            " h" +
          "</p>",
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>f12<u>34</u>56g</i>" +
            "</b>" +
            " h" +
          "</p>"
      )
  );

  /**
   * Tests for deletion of elements.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> DELETE_ELEMENT_TESTS = Arrays.asList(
      new TestParameters(
          structuralDeletionSample1(1),
          "<p>12<u>34</u>56abc</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(4),
          "<p>abc12<u>34</u>56</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(3),
          "<p>ab12<u>34</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(5),
          "<p>a<b>b</b>12<u>34</u>56<i>c</i></p>", "<p>a<b>b</b><i>c</i></p>"
      ),
      new TestParameters(
          structuralDeletionSample2(3),
          "<p>ab12<u>3<i>hello</i><b>world</b>4</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample3(3),
          "<p>ab12<u>3<i></i><b></b>4</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample4(3),
          "<p>ab<i>hello</i><b>world</b>c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample5(3),
          "<p>ab<a href=\"http://www.google.com/\">google</a>c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(12),
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>f12<u>34</u>56g</i>" +
            "</b>" +
            " h" +
          "</p>",
          "<p>" +
            "<i>ab</i>" +
            "cd " +
            "<b>" +
              "e" +
              "<i>fg</i>" +
            "</b>" +
            " h" +
          "</p>"
      )
  );

  /**
   * Tests for the setting and removal of attributes.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> ATTRIBUTE_TESTS = Arrays.asList(
      new TestParameters(
          Nindo.replaceAttributes(1, new AttributesImpl("href", "http://www.google.com/")),
          "<p><a>ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          Nindo.replaceAttributes(1, new AttributesImpl("href", "http://www.google.com/")),
          "<p><a name=\"thing\">ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          Nindo.setAttribute(1, "href", "http://www.google.com/"),
          "<p><a>ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          Nindo.setAttribute(2, "href", "http://www.google.com/"),
          "<p>a<a>b</a></p>",
          "<p>a<a href=\"http://www.google.com/\">b</a></p>"
      ),
      new TestParameters(
          Nindo.setAttribute(2, "href", "http://www.google.com/"),
          "<p>a<a href=\"http://www.yahoo.com/\">b</a></p>",
          "<p>a<a href=\"http://www.google.com/\">b</a></p>"
      ),
      new TestParameters(
          Nindo.removeAttribute(2, "href"),
          "<p>a<a href=\"http://www.google.com/\">b</a></p>",
          "<p>a<a>b</a></p>"
      )
  );

  /**
   * Miscellaneous tests.
   */
  // OFFSET ADJUSTED
  private static final List<TestParameters> MISCELLANEOUS_TESTS = Arrays.asList(
      new TestParameters(
          "<p></p>",
          new TestComponent(Nindo.insertCharacters(1, "a"), "<p>a</p>"),
          new TestComponent(Nindo.insertCharacters(2, "b"), "<p>ab</p>"),
          new TestComponent(Nindo.insertCharacters(3, "c"), "<p>abc</p>")
      ),
      new TestParameters(
          "",
          new TestComponent(Nindo.insertElement(0, "p", Attributes.EMPTY_MAP),
              "<p></p>", "<p/>"),
          new TestComponent(Nindo.insertCharacters(1, "a"), "<p>a</p>"),
          new TestComponent(Nindo.insertCharacters(2, "b"), "<p>ab</p>"),
          new TestComponent(Nindo.insertCharacters(3, "c"), "<p>abc</p>")
      ),
      new TestParameters(
          "<p></p>",
          new TestComponent(structuralSample1(1), "<p>12<u>34</u>56</p>"),
          new TestComponent(structuralSample1(8), "<p>12<u>34</u>512<u>34</u>566</p>"),
          new TestComponent(structuralSample1(15), "<p>12<u>34</u>512<u>34</u>512<u>34</u>5666</p>")
      )
  );

  /**
   * Runs the tests for the insertion of text.
   */
  public static void runTextInsertionTests(DocumentParser<?> documentParser) {
    processTests(INSERT_TEXT_TESTS, documentParser);
  }

  /**
   * Runs the tests for the deletion of text.
   */
  public static void runTextDeletionTests(DocumentParser<?> documentParser) {
    processTests(DELETE_TEXT_TESTS, documentParser);
  }

  /**
   * Runs the tests for the insertion of elements.
   */
  public static void runElementInsertionTests(DocumentParser<?> documentParser) {
    processTests(INSERT_ELEMENT_TESTS, documentParser);
  }

  /**
   * Runs the tests for the deletion of elements.
   */
  public static void runElementDeletionTests(DocumentParser<?> documentParser) {
    processTests(DELETE_ELEMENT_TESTS, documentParser);
  }

  /**
   * Runs the tests for the setting and removal of attributes.
   */
  public static void runAttributeTests(DocumentParser<?> documentParser) {
    processTests(ATTRIBUTE_TESTS, documentParser);
  }

  /**
   * Runs a miscellany of tests.
   */
  public static void runMiscellaneousTests(DocumentParser<?> documentParser) {
    processTests(MISCELLANEOUS_TESTS, documentParser);
  }

  private static void processTests(List<TestParameters> tests, DocumentParser<?> documentParser) {
    for (TestParameters parameters : tests) {
      parameters.run(documentParser);
    }
  }

  private static Nindo createDeletion(int... deletionBoundaries) {
    Nindo.Builder builder = new Nindo.Builder();
    int location = 0;
    for (int i = 0; i < deletionBoundaries.length; i += 2) {
      builder.skip(deletionBoundaries[i] - location);
      int deletionSize = deletionBoundaries[i+1] - deletionBoundaries[i];
      builder.deleteCharacters(deletionSize);
      location = deletionBoundaries[i+1];
    }
    return builder.build();
  }

  /**
   * Creates an operation to insert the XML fragment "12<u>34</u>56".
   *
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static Nindo structuralSample1(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.characters("12");
    builder.elementStart("u", Attributes.EMPTY_MAP);
    builder.characters("34");
    builder.elementEnd();
    builder.characters("56");
    return builder.build();
  }

  /**
   * Creates an operation to insert the XML fragment
   * "12<u>3<i>hello</i><b>world</b>4</u>56".
   *
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static Nindo structuralSample2(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.characters("12");
    builder.elementStart("u", Attributes.EMPTY_MAP);
    builder.characters("3");
    builder.elementStart("i", Attributes.EMPTY_MAP);
    builder.characters("hello");
    builder.elementEnd();
    builder.elementStart("b", Attributes.EMPTY_MAP);
    builder.characters("world");
    builder.elementEnd();
    builder.characters("4");
    builder.elementEnd();
    builder.characters("56");
    return builder.build();
  }

  /**
   * Creates an operation to insert the XML fragment "12<u>3<i/><b/>4</u>56".
   *
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static Nindo structuralSample3(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.characters("12");
    builder.elementStart("u", Attributes.EMPTY_MAP);
    builder.characters("3");
    builder.elementStart("i", Attributes.EMPTY_MAP);
    builder.elementEnd();
    builder.elementStart("b", Attributes.EMPTY_MAP);
    builder.elementEnd();
    builder.characters("4");
    builder.elementEnd();
    builder.characters("56");
    return builder.build();
  }

  /**
   * Creates an operation to insert the XML fragment "<i>hello</i><b>world</b>".
   *
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static Nindo structuralSample4(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.elementStart("i", Attributes.EMPTY_MAP);
    builder.characters("hello");
    builder.elementEnd();
    builder.elementStart("b", Attributes.EMPTY_MAP);
    builder.characters("world");
    builder.elementEnd();
    return builder.build();
  }

  /**
   * Creates an operation to insert the XML fragment
   * "<a href=\"http://www.google.com/\">google</a>".
   *
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static Nindo structuralSample5(int location) {
    Attributes attributes = new AttributesImpl(
        Collections.singletonMap("href", "http://www.google.com/"));
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.elementStart("a", new AttributesImpl(attributes));
    builder.characters("google");
    builder.elementEnd();
    return builder.build();
  }

  /**
   * Creates an operation to delete the XML fragment "12<u>34</u>56".
   *
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static Nindo structuralDeletionSample1(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.deleteCharacters(2);
    builder.deleteElementStart();
    builder.deleteCharacters(2);
    builder.deleteElementEnd();
    builder.deleteCharacters(2);
    return builder.build();
  }

  /**
   * Creates an operation to delete the XML fragment
   * "12<u>3<i>hello</i><b>world</b>4</u>56".
   *
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static Nindo structuralDeletionSample2(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.deleteCharacters(2);
    builder.deleteElementStart();
    builder.deleteCharacters(1);
    builder.deleteElementStart();
    builder.deleteCharacters(5);
    builder.deleteElementEnd();
    builder.deleteElementStart();
    builder.deleteCharacters(5);
    builder.deleteElementEnd();
    builder.deleteCharacters(1);
    builder.deleteElementEnd();
    builder.deleteCharacters(2);
    return builder.build();
  }

  /**
   * Creates an operation to delete the XML fragment "12<u>3<i/><b/>4</u>56".
   *
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static Nindo structuralDeletionSample3(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.deleteCharacters(2);
    builder.deleteElementStart();
    builder.deleteCharacters(1);
    builder.deleteElementStart();
    builder.deleteElementEnd();
    builder.deleteElementStart();
    builder.deleteElementEnd();
    builder.deleteCharacters(1);
    builder.deleteElementEnd();
    builder.deleteCharacters(2);
    return builder.build();
  }

  /**
   * Creates an operation to delete the XML fragment "<i>hello</i><b>world</b>".
   *
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static Nindo structuralDeletionSample4(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.deleteElementStart();
    builder.deleteCharacters(5);
    builder.deleteElementEnd();
    builder.deleteElementStart();
    builder.deleteCharacters(5);
    builder.deleteElementEnd();
    return builder.build();
  }

  /**
   * Creates an operation to delete the XML fragment
   * "<a href=\"http://www.google.com/\">google</a>".
   *
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static Nindo structuralDeletionSample5(int location) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    builder.deleteElementStart();
    builder.deleteCharacters(6);
    builder.deleteElementEnd();
    return builder.build();
  }


}
