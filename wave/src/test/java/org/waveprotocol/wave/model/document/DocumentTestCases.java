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

package org.waveprotocol.wave.model.document;

import junit.framework.Assert;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.DocOpCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for applying operations to documents.
 *
 */
public class DocumentTestCases {

  /**
   * A parser that can serialise documents to strings and deserialise documents from strings.
   *
   * @param <D> The type of the document.
   */
  public interface DocumentParser<D extends ModifiableDocument> {

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

    final DocOp mutation;
    final List<String> expectedResultAlternatives = new ArrayList<String>();

    TestComponent(DocOp mutation, String... expectedResultAlternatives) {
      this.mutation = mutation;
      for (String expected : expectedResultAlternatives) {
        this.expectedResultAlternatives.add(expected);
      }
    }

    <D extends ModifiableDocument> void run(D document, DocumentParser<D> parser) {
      String initialState = parser.asString(document);
      DocOp docOp = null;

      try {
        document.consume(mutation);
      } catch (OperationException e) {
        Assert.fail("An OperationException was thrown: " + e);
      }

      String computedDocument = parser.asString(document);
      String message = "Computed: " + computedDocument + " Expected: "
          + expectedResultAlternatives + " Op: " + DocOpUtil.toConciseString(mutation);
      Assert.assertTrue(message, expectedResultAlternatives.contains(computedDocument));

    }

  }

  private static final class TestParameters {

    private final String initialDocument;
    private final List<TestComponent> testComponents;

    TestParameters(DocOp documentMutation, String initialDocument,
        String... finalDocuments) {
      this.initialDocument = initialDocument;
      testComponents =
          Collections.singletonList(new TestComponent(documentMutation, finalDocuments));
    }

    TestParameters(String initialDocument, TestComponent... testComponents) {
      this.initialDocument = initialDocument;
      this.testComponents = Arrays.asList(testComponents);
    }

    <D extends ModifiableDocument> void run(DocumentParser<D> parser) {
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
          DocOpCreator.insertCharacters(2, 1, "ab"),
          "<p></p>", "<p>ab</p>"
      ),
      new TestParameters(
          DocOpCreator.insertCharacters(7, 2, "12"),
          "<p>abcde</p>", "<p>a12bcde</p>"
      ),
      new TestParameters(
          DocOpCreator.insertCharacters(7, 6, "12"),
          "<p>abcde</p>", "<p>abcde12</p>"
      ),
      new TestParameters(
          DocOpCreator.insertCharacters(18, 5, "12"),
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
          DocOpCreator.insertCharacters(18, 12, " 12 "),
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
          DocOpCreator.deleteCharacters(4, 1, "ab"),
          "<p>ab</p>", "<p></p>", "<p/>"
      ),
      new TestParameters(
          DocOpCreator.deleteCharacters(7, 2, "bcd"),
          "<p>abcde</p>", "<p>ae</p>"
      ),
      new TestParameters(
          DocOpCreator.deleteCharacters(10, 4, "cd"),
          "<p>ab<i>cd</i>ef</p>", "<p>ab<i></i>ef</p>", "<p>ab<i/>ef</p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(10, 4, "c"),
              DocOpCreator.deleteCharacters(9, 2, "b")
          ),
          "<p>ab<i>cd</i>ef</p>", "<p>a<i>d</i>ef</p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(10, 7, "e"),
              DocOpCreator.deleteCharacters(9, 5, "d")
          ),
          "<p>ab<i>cd</i>ef</p>", "<p>ab<i>c</i>f</p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(10, 6, "c"),
              DocOpCreator.deleteCharacters(9, 3, "b")
          ),
          "<p><i>ab</i><i>cd</i></p>", "<p><i>a</i><i>d</i></p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(14, 10, "c"),
              DocOpCreator.deleteCharacters(13, 7, "b"),
              DocOpCreator.deleteCharacters(12, 5, "f")
          ),
          "<p><i>a<b>ef</b>b</i><i>cd</i></p>", "<p><i>a<b>e</b></i><i>d</i></p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(14, 8, "e"),
              DocOpCreator.deleteCharacters(13, 6, "c"),
              DocOpCreator.deleteCharacters(12, 3, "b")
          ),
          "<p><i>ab</i><i>c<b>ef</b>d</i></p>", "<p><i>a</i><i><b>f</b>d</i></p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(18, 12, "e"),
              DocOpCreator.deleteCharacters(17, 10, "c"),
              DocOpCreator.deleteCharacters(16, 7, "b"),
              DocOpCreator.deleteCharacters(15, 5, "f")
          ),
          "<p><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i></p>", "<p><i>a<b>e</b></i><i><b>f</b>d</i></p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(18, 10, "c"),
              DocOpCreator.deleteCharacters(17, 7, "b"),
              DocOpCreator.deleteCharacters(16, 4, "ef")
          ),
          "<p><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i></p>",
          "<p><i>a<b></b></i><i><b>ef</b>d</i></p>", "<p><i>a<b/></i><i><b>ef</b>d</i></p>"
      ),
      new TestParameters(
          compose(
              DocOpCreator.deleteCharacters(18, 12, "ef"),
              DocOpCreator.deleteCharacters(16, 10, "c"),
              DocOpCreator.deleteCharacters(15, 7, "b")
          ),
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
          structuralSample1(5, 1),
          "<p>abc</p>", "<p>12<u>34</u>56abc</p>"
      ),
      new TestParameters(
          structuralSample1(5, 4),
          "<p>abc</p>", "<p>abc12<u>34</u>56</p>"
      ),
      new TestParameters(
          structuralSample1(5, 3),
          "<p>abc</p>", "<p>ab12<u>34</u>56c</p>"
      ),
      new TestParameters(
          structuralSample1(9, 5),
          "<p>a<b>b</b><i>c</i></p>", "<p>a<b>b</b>12<u>34</u>56<i>c</i></p>"
      ),
      new TestParameters(
          structuralSample2(5, 3),
          "<p>abc</p>", "<p>ab12<u>3<i>hello</i><b>world</b>4</u>56c</p>"
      ),
      new TestParameters(
          structuralSample3(5, 3),
          "<p>abc</p>",
          "<p>ab12<u>3<i></i><b></b>4</u>56c</p>", "<p>ab12<u>3<i/><b/>4</u>56c</p>"
      ),
      new TestParameters(
          structuralSample4(5, 3),
          "<p>abc</p>", "<p>ab<i>hello</i><b>world</b>c</p>"
      ),
      new TestParameters(
          structuralSample5(5, 3),
          "<p>abc</p>", "<p>ab<a href=\"http://www.google.com/\">google</a>c</p>"
      ),
      new TestParameters(
          structuralSample1(18 ,12),
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
          structuralDeletionSample1(13, 1),
          "<p>12<u>34</u>56abc</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(13, 4),
          "<p>abc12<u>34</u>56</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(13, 3),
          "<p>ab12<u>34</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(17, 5),
          "<p>a<b>b</b>12<u>34</u>56<i>c</i></p>", "<p>a<b>b</b><i>c</i></p>"
      ),
      new TestParameters(
          structuralDeletionSample2(27, 3),
          "<p>ab12<u>3<i>hello</i><b>world</b>4</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample3(17, 3),
          "<p>ab12<u>3<i></i><b></b>4</u>56c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample4(19, 3),
          "<p>ab<i>hello</i><b>world</b>c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample5(13, 3),
          "<p>ab<a href=\"http://www.google.com/\">google</a>c</p>", "<p>abc</p>"
      ),
      new TestParameters(
          structuralDeletionSample1(26, 12),
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
          DocOpCreator.replaceAttributes(6, 1, Attributes.EMPTY_MAP,
              new AttributesImpl("href", "http://www.google.com/")),
          "<p><a>ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          DocOpCreator.replaceAttributes(6, 1, new AttributesImpl("name", "thing"),
              new AttributesImpl("href", "http://www.google.com/")),
          "<p><a name=\"thing\">ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          DocOpCreator.setAttribute(6, 1, "href", null, "http://www.google.com/"),
          "<p><a>ab</a></p>",
          "<p><a href=\"http://www.google.com/\">ab</a></p>"
      ),
      new TestParameters(
          DocOpCreator.setAttribute(6, 2, "href", null, "http://www.google.com/"),
          "<p>a<a>b</a></p>",
          "<p>a<a href=\"http://www.google.com/\">b</a></p>"
      ),
      new TestParameters(
          DocOpCreator.setAttribute(6, 2, "href", "http://www.yahoo.com/",
              "http://www.google.com/"),
          "<p>a<a href=\"http://www.yahoo.com/\">b</a></p>",
          "<p>a<a href=\"http://www.google.com/\">b</a></p>"
      ),
      new TestParameters(
          DocOpCreator.setAttribute(6, 2, "href", "http://www.google.com/", null),
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
          new TestComponent(DocOpCreator.insertCharacters(2, 1, "a"), "<p>a</p>"),
          new TestComponent(DocOpCreator.insertCharacters(3, 2, "b"), "<p>ab</p>"),
          new TestComponent(DocOpCreator.insertCharacters(4, 3, "c"), "<p>abc</p>")
      ),
      new TestParameters(
          "",
          new TestComponent(DocOpCreator.insertElement(0, 0, "p", Attributes.EMPTY_MAP),
              "<p></p>", "<p/>"),
          new TestComponent(DocOpCreator.insertCharacters(2, 1, "a"), "<p>a</p>"),
          new TestComponent(DocOpCreator.insertCharacters(3, 2, "b"), "<p>ab</p>"),
          new TestComponent(DocOpCreator.insertCharacters(4, 3, "c"), "<p>abc</p>")
      ),
      new TestParameters(
          "<p></p>",
          new TestComponent(structuralSample1(2, 1), "<p>12<u>34</u>56</p>"),
          new TestComponent(structuralSample1(10, 8), "<p>12<u>34</u>512<u>34</u>566</p>"),
          new TestComponent(structuralSample1(18, 15),
              "<p>12<u>34</u>512<u>34</u>512<u>34</u>5666</p>")
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

  /**
   * Creates an operation to insert the XML fragment "12<u>34</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static DocOp structuralSample1(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .characters("12")
        .elementStart("u", Attributes.EMPTY_MAP)
        .characters("34")
        .elementEnd()
        .characters("56")
        .retain(size - location)
        .build();
  }

  /**
   * Creates an operation to insert the XML fragment
   * "12<u>3<i>hello</i><b>world</b>4</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static DocOp structuralSample2(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .characters("12")
        .elementStart("u", Attributes.EMPTY_MAP)
        .characters("3")
        .elementStart("i", Attributes.EMPTY_MAP)
        .characters("hello")
        .elementEnd()
        .elementStart("b", Attributes.EMPTY_MAP)
        .characters("world")
        .elementEnd()
        .characters("4")
        .elementEnd()
        .characters("56")
        .retain(size - location)
        .build();
  }

  /**
   * Creates an operation to insert the XML fragment "12<u>3<i/><b/>4</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static DocOp structuralSample3(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .characters("12")
        .elementStart("u", Attributes.EMPTY_MAP)
        .characters("3")
        .elementStart("i", Attributes.EMPTY_MAP)
        .elementEnd()
        .elementStart("b", Attributes.EMPTY_MAP)
        .elementEnd()
        .characters("4")
        .elementEnd()
        .characters("56")
        .retain(size - location)
        .build();
  }

  /**
   * Creates an operation to insert the XML fragment "<i>hello</i><b>world</b>".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static DocOp structuralSample4(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .elementStart("i", Attributes.EMPTY_MAP)
        .characters("hello")
        .elementEnd()
        .elementStart("b", Attributes.EMPTY_MAP)
        .characters("world")
        .elementEnd()
        .retain(size - location)
        .build();
  }

  /**
   * Creates an operation to insert the XML fragment
   * "<a href=\"http://www.google.com/\">google</a>".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The location at which the insert the fragment.
   * @return The operation.
   */
  private static DocOp structuralSample5(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .elementStart("a", new AttributesImpl("href", "http://www.google.com/"))
        .characters("google")
        .elementEnd()
        .retain(size - location)
        .build();
  }

  /**
   * Creates an operation to delete the XML fragment "12<u>34</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static DocOp structuralDeletionSample1(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .deleteCharacters("12")
        .deleteElementStart("u", Attributes.EMPTY_MAP)
        .deleteCharacters("34")
        .deleteElementEnd()
        .deleteCharacters("56")
        .retain(size - location - 8)
        .build();
  }

  /**
   * Creates an operation to delete the XML fragment
   * "12<u>3<i>hello</i><b>world</b>4</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static DocOp structuralDeletionSample2(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .deleteCharacters("12")
        .deleteElementStart("u", Attributes.EMPTY_MAP)
        .deleteCharacters("3")
        .deleteElementStart("i", Attributes.EMPTY_MAP)
        .deleteCharacters("hello")
        .deleteElementEnd()
        .deleteElementStart("b", Attributes.EMPTY_MAP)
        .deleteCharacters("world")
        .deleteElementEnd()
        .deleteCharacters("4")
        .deleteElementEnd()
        .deleteCharacters("56")
        .retain(size - location - 22)
        .build();
  }

  /**
   * Creates an operation to delete the XML fragment "12<u>3<i/><b/>4</u>56".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static DocOp structuralDeletionSample3(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .deleteCharacters("12")
        .deleteElementStart("u", Attributes.EMPTY_MAP)
        .deleteCharacters("3")
        .deleteElementStart("i", Attributes.EMPTY_MAP)
        .deleteElementEnd()
        .deleteElementStart("b", Attributes.EMPTY_MAP)
        .deleteElementEnd()
        .deleteCharacters("4")
        .deleteElementEnd()
        .deleteCharacters("56")
        .retain(size - location - 12)
        .build();
  }

  /**
   * Creates an operation to delete the XML fragment "<i>hello</i><b>world</b>".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static DocOp structuralDeletionSample4(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .deleteElementStart("i", Attributes.EMPTY_MAP)
        .deleteCharacters("hello")
        .deleteElementEnd()
        .deleteElementStart("b", Attributes.EMPTY_MAP)
        .deleteCharacters("world")
        .deleteElementEnd()
        .retain(size - location - 14)
        .build();
  }

  /**
   * Creates an operation to delete the XML fragment
   * "<a href=\"http://www.google.com/\">google</a>".
   *
   * @param size The size of the document on which this operation should apply.
   * @param location The start of the range where the fragment to be deleted
   *        resides.
   * @return The operation.
   */
  private static DocOp structuralDeletionSample5(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .deleteElementStart("a", new AttributesImpl("href", "http://www.google.com/"))
        .deleteCharacters("google")
        .deleteElementEnd()
        .retain(size - location - 8)
        .build();
  }

  private static DocOp compose(DocOp... operations) {
    return Composer.compose(Arrays.asList(operations));
  }

}
