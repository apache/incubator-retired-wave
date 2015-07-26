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

package org.waveprotocol.wave.model.document.operation.algorithm;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.testing.DocOpCreator;

/**
 */

public class DocOpTransformerTest extends TestCase {

  private static class TestParameters {

    final DocOp clientMutation;
    final DocOp serverMutation;
    final DocOp transformedClientMutation;
    final DocOp transformedServerMutation;

    TestParameters(DocOp clientMutation, DocOp serverMutation,
        DocOp transformedClientMutation, DocOp transformedServerMutation) {
      this.clientMutation = clientMutation;
      this.serverMutation = serverMutation;
      this.transformedClientMutation = transformedClientMutation;
      this.transformedServerMutation = transformedServerMutation;
    }

    void run() {
      singleTest(clientMutation, serverMutation,
          transformedClientMutation, transformedServerMutation);
    }

  }

  private static final class ReversibleTestParameters extends TestParameters {

    ReversibleTestParameters(DocOp clientMutation, DocOp serverMutation,
        DocOp transformedClientMutation, DocOp transformedServerMutation) {
      super(clientMutation, serverMutation, transformedClientMutation, transformedServerMutation);
    }

    @Override
    void run() {
      singleTest(clientMutation, serverMutation,
          transformedClientMutation, transformedServerMutation);
      singleTest(serverMutation, clientMutation,
          transformedServerMutation, transformedClientMutation);
    }

  }

  /**
   * Performs tests for transforming text deletions against text deletions.
   */
  public void testDeleteVsDelete() {
    // A's deletion spatially before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.deleteCharacters(20, 1, "abcde"),
        DocOpCreator.deleteCharacters(20, 7, "fg"),
        DocOpCreator.deleteCharacters(18, 1, "abcde"),
        DocOpCreator.deleteCharacters(15, 2, "fg")
    ).run();
    // A's deletion spatially adjacent to and before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.deleteCharacters(20, 1, "abcde"),
        DocOpCreator.deleteCharacters(20, 6, "fg"),
        DocOpCreator.deleteCharacters(18, 1, "abcde"),
        DocOpCreator.deleteCharacters(15, 1, "fg")
    ).run();
    // A's deletion overlaps B's deletion
    new ReversibleTestParameters(
        DocOpCreator.deleteCharacters(20, 1, "abcde"),
        DocOpCreator.deleteCharacters(20, 3, "cdefghi"),
        DocOpCreator.deleteCharacters(13, 1, "ab"),
        DocOpCreator.deleteCharacters(15, 1, "fghi")
    ).run();
    // A's deletion a subset of B's deletion
    new ReversibleTestParameters(
        DocOpCreator.deleteCharacters(20, 1, "abcdefg"),
        DocOpCreator.deleteCharacters(20, 3, "cd"),
        DocOpCreator.deleteCharacters(18, 1, "abefg"),
        DocOpCreator.identity(13)
    ).run();
    // A's deletion identical to B's deletion
    new ReversibleTestParameters(
        DocOpCreator.deleteCharacters(20, 1, "abcdefg"),
        DocOpCreator.deleteCharacters(20, 1, "abcdefg"),
        DocOpCreator.identity(13),
        DocOpCreator.identity(13)
    ).run();
  }

  /**
   * Performs tests for transforming text insertions against text deletions.
   */
  public void testInsertVsDelete() {
    // A's insertion spatially before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 1, "abc"),
        DocOpCreator.deleteCharacters(20, 2, "de"),
        DocOpCreator.insertCharacters(18, 1, "abc"),
        DocOpCreator.deleteCharacters(23, 5, "de")
    ).run();
    // A's insertion spatially inside B's deletion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 2, "abc"),
        DocOpCreator.deleteCharacters(20, 1, "ce"),
        DocOpCreator.insertCharacters(18, 1, "abc"),
        new DocOpBuilder()
            .retain(1)
            .deleteCharacters("c")
            .retain(3)
            .deleteCharacters("e")
            .retain(17)
            .build()
    ).run();
    // A's insertion spatially at the start of B's deletion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 1, "abc"),
        DocOpCreator.deleteCharacters(20, 1, "de"),
        DocOpCreator.insertCharacters(18, 1, "abc"),
        DocOpCreator.deleteCharacters(23, 4, "de")
    ).run();
    // A's insertion spatially at the end of B's deletion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 3, "abc"),
        DocOpCreator.deleteCharacters(20, 1, "de"),
        DocOpCreator.insertCharacters(18, 1, "abc"),
        DocOpCreator.deleteCharacters(23, 1, "de")
    ).run();
    // A's insertion spatially after B's deletion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 4, "abc"),
        DocOpCreator.deleteCharacters(20, 1, "de"),
        DocOpCreator.insertCharacters(18, 2, "abc"),
        DocOpCreator.deleteCharacters(23, 1, "de")
    ).run();
  }

  /**
   * Performs tests for transforming text insertions against text insertions.
   */
  public void testInsertVsInsert() {
    // A's insertion spatially before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 1, "a"),
        DocOpCreator.insertCharacters(20, 2, "1"),
        DocOpCreator.insertCharacters(21, 1, "a"),
        DocOpCreator.insertCharacters(21, 3, "1")
    ).run();
    // client's insertion spatially at the same location as server's insertion
    new TestParameters(
        DocOpCreator.insertCharacters(20, 2, "abc"),
        DocOpCreator.insertCharacters(20, 2, "123"),
        DocOpCreator.insertCharacters(23, 2, "abc"),
        DocOpCreator.insertCharacters(23, 5, "123")
    ).run();
  }

  /**
   * Performs tests for transforming element insertions against text insertions.
   */
  public void testStructuralVsInsert() {
    // A's insertion spatially before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 1, "a"),
        sampleStructural(20, 2),
        DocOpCreator.insertCharacters(33, 1, "a"),
        sampleStructural(21, 3)
    ).run();
    // A's insertion spatially after B's insertion
    new ReversibleTestParameters(
        DocOpCreator.insertCharacters(20, 2, "a"),
        sampleStructural(20, 1),
        DocOpCreator.insertCharacters(33, 15, "a"),
        sampleStructural(21, 1)
    ).run();
    // client's insertion spatially at the same location as server's insertion
    new TestParameters(
        DocOpCreator.insertCharacters(20, 2, "a"),
        sampleStructural(20, 2),
        DocOpCreator.insertCharacters(33, 2, "a"),
        sampleStructural(21, 3)
    ).run();
    // client's insertion spatially at the same location as server's insertion
    new TestParameters(
        sampleStructural(20, 2),
        DocOpCreator.insertCharacters(20, 2, "a"),
        sampleStructural(21, 2),
        DocOpCreator.insertCharacters(33, 15, "a")
    ).run();
  }

  /**
   * Performs tests for transforming element insertions against element
   * insertions.
   */
  public void testStructuralVsStructural() {
    // A's insertion spatially before B's insertion
    new ReversibleTestParameters(
        sampleStructural(20, 1),
        sampleStructural(20, 2),
        sampleStructural(33, 1),
        sampleStructural(33, 15)
    ).run();
    // client's insertion spatially at the same location as server's insertion
    new TestParameters(
        sampleStructural(20, 2),
        sampleStructural(20, 2),
        sampleStructural(33, 2),
        sampleStructural(33, 15)
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against text deletions.
   */
  public void testAttributesVsDelete() {
    // A's attributes replacement spatially before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.replaceAttributes(17, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attributes replacement spatially adjacent to and before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 4, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.replaceAttributes(17, 4, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attributes replacement spatially after B's deletion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.replaceAttributes(17, 6, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attributes replacement adjacent to and after B's deletion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 8, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.replaceAttributes(17, 5, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against text insertions.
   */
  public void testAttributesVsInsert() {
    // A's attributes replacement spatially before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 6, "hello"),
        DocOpCreator.replaceAttributes(25, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 6, "hello")
    ).run();
    // A's attributes replacement spatially after B's insertion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 2, "hello"),
        DocOpCreator.replaceAttributes(25, 8, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 2, "hello")
    ).run();
    // A's attributes replacement spatially adjacent to and after B's insertion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 3, "hello"),
        DocOpCreator.replaceAttributes(25, 8, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 3, "hello")
    ).run();
    // A's attributes replacement spatially adjacent to and before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 4, "hello"),
        DocOpCreator.replaceAttributes(25, 3, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.insertCharacters(20, 4, "hello")
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against attribute
   * settings.
   */
  public void testAttributesVsAttributes() {
    // A's attributes replacement spatially before B's attributes replacement
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 5, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/")),
        DocOpCreator.replaceAttributes(20, 5, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/"))
    ).run();
    // client's attributes replacement coincides with server's attributes replacement
    new TestParameters(
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("name", "Google!")),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "Google!"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.identity(20)
    ).run();
    // client's attributes replacement coincides with server's attributes replacement
    new TestParameters(
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/")),
        DocOpCreator.replaceAttributes(20, 9, attributes("href", "http://www.yahoo.com/"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.identity(20)
    ).run();
  }

  /**
   * Performs tests for transforming an attribute setting against an element
   * deletion.
   */
  public void testAttributesVsElementDeletion() {
    // A's attributes replacement coincides with B's element deletion.
    new ReversibleTestParameters(
        DocOpCreator.replaceAttributes(20, 6, attributes("name", "value"),
            attributes("href", "http://www.google.com/")),
        DocOpCreator.deleteElement(20, 6, "type", attributes("name", "value")),
        DocOpCreator.identity(18),
        DocOpCreator.deleteElement(20, 6, "type", attributes("href", "http://www.google.com/"))
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against text deletions.
   */
  public void testAttributeVsDelete() {
    // A's attribute update spatially before B's deletion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.setAttribute(17, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attribute update spatially adjacent to and before B's deletion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 4, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.setAttribute(17, 4, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attribute update spatially after B's deletion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.setAttribute(17, 6, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
    // A's attribute update spatially adjacent to and after B's deletion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 8, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc"),
        DocOpCreator.setAttribute(17, 5, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteCharacters(20, 5, "abc")
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against text insertions.
   */
  public void testAttributeVsInsert() {
    // A's attribute update spatially before B's insertion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 6, "hello"),
        DocOpCreator.setAttribute(25, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 6, "hello")
    ).run();
    // A's attribute update spatially adjacent to and before B's insertion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 4, "hello"),
        DocOpCreator.setAttribute(25, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 4, "hello")
    ).run();
    // A's attribute update spatially after B's insertion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 2, "hello"),
        DocOpCreator.setAttribute(25, 8, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 2, "hello")
    ).run();
    // A's attribute update spatially adjacent to and after B's insertion.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 3, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 3, "hello"),
        DocOpCreator.setAttribute(25, 8, "href", "initial", "http://www.google.com/"),
        DocOpCreator.insertCharacters(20, 3, "hello")
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against attribute
   * settings.
   */
  public void testAttributeVsAttributes() {
    // A's attribute update spatially before B's attributes replacement.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 5, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/")),
        DocOpCreator.setAttribute(20, 5, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 9, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/"))
    ).run();
    // A's attribute update spatially after B's attributes replacement.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 5, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/")),
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 5, attributes("name", "value"),
            attributes("href", "http://www.yahoo.com/"))
    ).run();
    // A's attribute update coincides with B's attributes replacement.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 9, attributes("href", "initial"),
            attributes("name", "Google!")),
        DocOpCreator.identity(20),
        DocOpCreator.replaceAttributes(20, 9, attributes("href", "http://www.google.com/"),
            attributes("name", "Google!"))
    ).run();
    // A's attribute update coincides with B's attributes replacement.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.replaceAttributes(20, 9, attributes("href", "initial"),
            attributes("href", "http://www.yahoo.com/")),
        DocOpCreator.identity(20),
        DocOpCreator.replaceAttributes(20, 9, attributes("href", "http://www.google.com/"),
            attributes("href", "http://www.yahoo.com/"))
    ).run();
  }

  /**
   * Performs tests for transforming attribute settings against attribute
   * settings.
   */
  public void testAttributeVsAttribute() {
    // A's attribute update spatially before B's attribute update.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 5, "href", "initial", "http://www.google.com/"),
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.yahoo.com/"),
        DocOpCreator.setAttribute(20, 5, "href", "initial", "http://www.google.com/"),
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.yahoo.com/")
    ).run();
    // A's attribute update has different key than B's attribute update.
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.setAttribute(20, 9, "name", "initial", "Google!"),
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.setAttribute(20, 9, "name", "initial", "Google!")
    ).run();
    // client's attribute update has same key as server's attribute update.
    new TestParameters(
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.google.com/"),
        DocOpCreator.setAttribute(20, 9, "href", "initial", "http://www.yahoo.com/"),
        DocOpCreator.setAttribute(20, 9, "href", "http://www.yahoo.com/", "http://www.google.com/"),
        new DocOpBuilder()
            .retain(9)
            .updateAttributes(AttributesUpdateImpl.EMPTY_MAP)
            .retain(10)
            .build()
    ).run();
  }

  /**
   * Performs tests for transforming an attribute setting against an element
   * deletion.
   */
  public void testAttributeVsElementDeletion() {
    // A's attribute update coincides with B's element deletion
    new ReversibleTestParameters(
        DocOpCreator.setAttribute(20, 6, "href", "initial", "http://www.google.com/"),
        DocOpCreator.deleteElement(20, 6, "type", attributes("href", "initial")),
        DocOpCreator.identity(18),
        DocOpCreator.deleteElement(20, 6, "type", attributes("href", "http://www.google.com/"))
    ).run();
  }

  /**
   * Performs tests for transforming a structural deletion against a text
   * insertion.
   */
  public void testStructuralDeletionVsInsert() {
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.insertCharacters(20, 6, "hello"),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hellocdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.insertCharacters(20, 7, "hello"),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("chellodefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.insertCharacters(20, 16, "hello"),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteCharacters("hello")
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion spatially adjacent to and before B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.insertCharacters(20, 17, "hello"),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(8)
            .build(),
        DocOpCreator.insertCharacters(5, 2, "hello")
    ).run();
    // A's deletion spatially before B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.insertCharacters(20, 18, "hello"),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(8)
            .build(),
        DocOpCreator.insertCharacters(5, 3, "hello")
    ).run();
  }

  /**
   * Performs tests for transforming a structural deletion against an element
   * insertion.
   */
  public void testStructuralDeletionVsStructural() {
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        sampleStructural(20, 6),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteElementStart("sampleElement", Attributes.EMPTY_MAP)
            .deleteCharacters("sample text")
            .deleteElementEnd()
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        sampleStructural(20, 7),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("c")
            .deleteElementStart("sampleElement", Attributes.EMPTY_MAP)
            .deleteCharacters("sample text")
            .deleteElementEnd()
            .deleteCharacters("defg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion engulfs B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        sampleStructural(20, 16),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementStart("sampleElement", Attributes.EMPTY_MAP)
            .deleteCharacters("sample text")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
    // A's deletion spatially adjacent to and before B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        sampleStructural(20, 17),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(16)
            .build(),
        sampleStructural(5, 2)
    ).run();
    // A's deletion spatially before B's insertion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        sampleStructural(20, 18),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(16)
            .build(),
        sampleStructural(5, 3)
    ).run();
  }

  /**
   * Performs tests for transforming structural deletions.
   */
  public void testStructuralDeletionTransformations() {
    // A's deletion engulfs B's deletion
    new ReversibleTestParameters(
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        new DocOpBuilder()
            .retain(5)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("cdefg")
            .deleteElementEnd()
            .retain(8)
            .build(),
        new DocOpBuilder()
            .retain(2)
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("ab")
            .deleteElementStart("type", Attributes.EMPTY_MAP)
            .deleteCharacters("hi")
            .deleteElementEnd()
            .deleteElementEnd()
            .retain(3)
            .build(),
        DocOpCreator.identity(5)
    ).run();
  }

  /**
   * Performs tests for transforming annotation modifications against deletions.
   */
  public void testAnnotationVsDelete() {
    // A's annotation spatially before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 7, "ab"),
        DocOpCreator.setAnnotation(18, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 7, "ab")
    ).run();
    // A's annotation spatially after B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 1, "ab"),
        DocOpCreator.setAnnotation(18, 2, 4, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 1, "ab")
    ).run();
    // A's annotation spatially adjacent to and before B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 6, "abc"),
        DocOpCreator.setAnnotation(17, 4, 6, "hello", null, "world"),
        new DocOpBuilder()
            .retain(6)
            .annotationBoundary(beginAnnotation("hello", null, "world"))
            .deleteCharacters("abc")
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build()
    ).run();
    // A's annotation spatially adjacent to and after B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 1, "abc"),
        DocOpCreator.setAnnotation(17, 1, 3, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 1, "abc")
    ).run();
    // A's annotation overlaps B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 1, "abcd"),
        DocOpCreator.setAnnotation(16, 1, 2, "hello", null, "world"),
        new DocOpBuilder()
            .retain(1)
            .deleteCharacters("abc")
            .annotationBoundary(beginAnnotation("hello", "world", null))
            .deleteCharacters("d")
            .annotationBoundary(finishAnnotation("hello"))
            .retain(15)
            .build()
    ).run();
    // A's annotation overlaps B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 5, "abcd"),
        DocOpCreator.setAnnotation(16, 4, 5, "hello", null, "world"),
        new DocOpBuilder()
            .retain(5)
            .deleteCharacters("a")
            .annotationBoundary(beginAnnotation("hello", null, "world"))
            .deleteCharacters("bcd")
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build()
    ).run();
    // A's annotation encloses B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 2, 8, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 5, "ab"),
        DocOpCreator.setAnnotation(18, 2, 6, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 5, "ab")
    ).run();
    // A's annotation inside B's deletion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 5, 7, "hello", null, "world"),
        DocOpCreator.deleteCharacters(20, 2, "abcdef"),
        DocOpCreator.identity(14),
        new DocOpBuilder()
            .retain(2)
            .deleteCharacters("abc")
            .annotationBoundary(beginAnnotation("hello", "world", null))
            .deleteCharacters("de")
            .annotationBoundary(finishAnnotation("hello"))
            .deleteCharacters("f")
            .retain(12)
            .build()
    ).run();
    // A's operation clears an annotation
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 4, 6, "hello", "world", null),
        DocOpCreator.deleteCharacters(20, 6, "abc"),
        DocOpCreator.setAnnotation(17, 4, 6, "hello", "world", null),
        new DocOpBuilder()
            .retain(6)
            .annotationBoundary(beginAnnotation("hello", "world", null))
            .deleteCharacters("abc")
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build()
    ).run();
  }

  /**
   * Performs tests for transforming annotation modifications against
   * insertions.
   */
  public void testAnnotationVsInsert() {
    // A's annotation spatially after B's insertion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 2, "abcd"),
        DocOpCreator.setAnnotation(24, 7, 9, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 2, "abcd")
    ).run();
    // A's annotation spatially before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 6, "abcd"),
        DocOpCreator.setAnnotation(24, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 6, "abcd")
    ).run();
    // A's annotation encloses B's insertion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 4, "abcd"),
        DocOpCreator.setAnnotation(24, 3, 9, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 4, "abcd")
    ).run();
    // A's annotation spatially adjacent to and after B's insertion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 3, "abcd"),
        DocOpCreator.setAnnotation(24, 7, 9, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 3, "abcd")
    ).run();
    // A's annotation spatially adjacent to and before B's insertion
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 5, "abcd"),
        DocOpCreator.setAnnotation(24, 3, 9, "hello", null, "world"),
        DocOpCreator.insertCharacters(20, 5, "abcd")
    ).run();
    // A's operation clears an annotation
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 3, 5, "hello", "world", null),
        DocOpCreator.insertCharacters(20, 4, "abcd"),
        DocOpCreator.setAnnotation(24, 3, 9, "hello", "world", null),
        DocOpCreator.insertCharacters(20, 4, "abcd")
    ).run();
  }

  /**
   * Performs tests for transforming annotation modifications against annotation
   * modifications.
   */
  public void testAnnotationVsAnnotation() {
    // A's annotation overlaps B's annotation and has different key
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 2, 6, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 9, "hi", "initial", "there"),
        DocOpCreator.setAnnotation(20, 2, 6, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 9, "hi", "initial", "there")
    ).run();
    // A's annotation overlaps B's annotation and has different key
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 2, 9, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 7, "hi", "initial", "there"),
        DocOpCreator.setAnnotation(20, 2, 9, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 7, "hi", "initial", "there")
    ).run();
    // A's annotation spatially before B's annotation
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 2, 5, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 6, 9, "hello", "initial", "there"),
        DocOpCreator.setAnnotation(20, 2, 5, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 6, 9, "hello", "initial", "there")
    ).run();
    // A's annotation spatially adjacent to and before B's annotation
    new ReversibleTestParameters(
        DocOpCreator.setAnnotation(20, 2, 5, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 9, "hello", "initial", "there"),
        DocOpCreator.setAnnotation(20, 2, 5, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 9, "hello", "initial", "there")
    ).run();
    // client's annotation overlaps server's annotation
    new TestParameters(
        DocOpCreator.setAnnotation(20, 2, 6, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 9, "hello", "initial", "there"),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(3)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(14)
            .build(),
        DocOpCreator.setAnnotation(20, 6, 9, "hello", "initial",  "there")
    ).run();
    // client's annotation overlaps server's annotation
    new TestParameters(
        DocOpCreator.setAnnotation(20, 5, 9, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 2, 6, "hello", "initial", "there"),
        new DocOpBuilder()
            .retain(5)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(3)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build(),
        DocOpCreator.setAnnotation(20, 2, 5, "hello", "initial", "there")
    ).run();
    // client's annotation encloses server's annotation
    new TestParameters(
        DocOpCreator.setAnnotation(20, 2, 9, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 5, 7, "hello", "initial", "there"),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(3)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(2)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build(),
        DocOpCreator.identity(20)
    ).run();
    // client's annotation inside server's annotation
    new TestParameters(
        DocOpCreator.setAnnotation(20, 5, 7, "hello", "initial", "world"),
        DocOpCreator.setAnnotation(20, 2, 9, "hello", "initial", "there"),
        DocOpCreator.setAnnotation(20, 5, 7, "hello", "there", "world"),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(3)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(2)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build()
    ).run();
    // client's annotation overlaps server's incontiguous annotation
    new TestParameters(
        DocOpCreator.setAnnotation(20, 4, 8, "hello", "initial", "world"),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(3)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(2)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build(),
        new DocOpBuilder()
            .retain(4)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(12)
            .build(),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(2)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(4)
            .annotationBoundary(beginAnnotation("hello", "initial", "there"))
            .retain(1)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build()
    ).run();
    // client's incontiguous annotation overlaps server's annotation
    new TestParameters(
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(3)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(2)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build(),
        DocOpCreator.setAnnotation(20, 4, 8, "hello", "initial", "there"),
        new DocOpBuilder()
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(2)
            .annotationBoundary(beginAnnotation("hello", "there", "world"))
            .retain(1)
            .annotationBoundary(beginAnnotation("hello", "initial", "world"))
            .retain(1)
            .annotationBoundary(finishAnnotation("hello"))
            .retain(11)
            .build(),
        DocOpCreator.setAnnotation(20, 5, 7, "hello", "initial", "there")
    ).run();
  }

  /**
   * Performs test for cases where exceptions should be thrown.
   */
  public void testExceptions() {
    // TODO(ohler,user): exceptionTest(...)
  }

  private static void singleTest(DocOp clientMutation, DocOp serverMutation,
      DocOp transformedClientMutation, DocOp transformedServerMutation) {
    try {
      OperationPair<DocOp> mutationPair =
          Transformer.transform(clientMutation, serverMutation);
      testEquality(transformedClientMutation, mutationPair.clientOp());
      testEquality(transformedServerMutation, mutationPair.serverOp());
    } catch (TransformException e) {
      fail("An unexpected exception was encountered.");
    }
  }

  private static void exceptionTest(DocOp clientMutation, DocOp serverMutation) {
    try {
      Transformer.transform(clientMutation, serverMutation);
      fail("An expected exception was not thrown.");
    } catch (TransformException e) {}
  }

  private static void testEquality(DocOp expected, DocOp actual) {
    assertEquals(DocOpUtil.toConciseString(expected), DocOpUtil.toConciseString(actual));
  }

  private static DocOp sampleStructural(int size, int location) {
    return new DocOpBuilder()
        .retain(location)
        .elementStart("sampleElement", Attributes.EMPTY_MAP)
        .characters("sample text")
        .elementEnd()
        .retain(size - location)
        .build();
  }

  private static AnnotationBoundaryMap beginAnnotation(String key, String oldValue,
      String newValue) {
    return AnnotationBoundaryMapImpl.builder().updateValues(key, oldValue, newValue).build();
  }

  private static AnnotationBoundaryMap finishAnnotation(String key) {
    return AnnotationBoundaryMapImpl.builder().initializationEnd(key).build();
  }

  private static Attributes attributes(String... attributes) {
    return new AttributesImpl(attributes);
  }

}
