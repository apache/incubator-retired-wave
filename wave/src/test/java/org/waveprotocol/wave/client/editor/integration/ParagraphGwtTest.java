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

package org.waveprotocol.wave.client.editor.integration;

import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;

import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * Unit tests for {@link Paragraph} element
 *
 */

public class ParagraphGwtTest extends ElementTestBase {

  /**
   * Test Rendering
   * @throws OperationException
   */
  public void testRendering() throws OperationException {
    testRendering(LineContainers.LINE_TAGNAME);
  }

  /**
   * Test Keydown
   * @throws OperationException
   */
  public void testKeyDown() throws OperationException {
    testKeyDowns(LineContainers.LINE_TAGNAME);
  }


  /**
   * Goes thru all tests for a given paragraph tag name
   *
   * @param tagName
   * @throws OperationException
   */
  public void testTag(String tagName) throws OperationException {
    testKeyDowns(tagName);
    testRendering(tagName);
  }

  /**
   * Tests rendering of a paragraph element
   *
   * @param tagName
   * @throws OperationException
   */
  public void testRendering(String tagName) throws OperationException {
    // Setup abbreviations from <p> to <line></line>
    abbreviations.clear();
    abbreviations.add("<p>", "<" + tagName + "></" + tagName + ">");
    abbreviations.add("</p>", "");
    abbreviations.add("<p/>", "<" + tagName + "></" + tagName + ">");
    abbreviations.add("<lc>", "<body>");
    abbreviations.add("</lc>", "</body>");

    // TODO(user): These tests aren't measuring the right things. (Its
    // measuring the size of the editor, but the editor has a minimum size, so
    // the newline only expands the editor by a small amount. Also these tests
    // are not very useful as it is obvious when a trivial new line doesn't
    // create a new line.) Either fix or remove.
    // int minHeight = 15;
//    testMinHeight("<p></p>", minHeight);
//    testTaller("<p></p><p></p>", "<p></p>", minHeight);
//    testTaller("<p></p><p></p><p></p>", "<p></p><p></p>", minHeight);

    testEqualHeight(format("<p></p>"), format("<p>aXj</p>"));
//    testEqualHeight("<p></p><p></p>", "<p>aXj</p><p>aXjADFSG</p>");

    testContentWrap("<p>|</p>");
    testContentWrap("<p>|</p><p></p>");
    testContentWrap("<p></p><p>|</p>");
    testContentWrap("<p>|</p><p>XX</p>");
    testContentWrap("<p>XX</p><p>|</p>");
    testContentWrap("<p></p><p>|</p><p></p>");
    testContentWrap("<p>XX</p><p>|</p><p></p>");
    testContentWrap("<p></p><p>|</p><p>XX</p>");
    testContentWrap("<p>XX</p><p>|</p><p>XX</p>");
    testContentWrap("<p>XX|</p><p></p>");
    testContentWrap("<p>XX|</p><p>XX</p>");
  }

  /**
   * Tests various keydowns that we can simulate
   *
   * @param tagName
   * @throws OperationException
   */
  private void testKeyDowns(String tagName) throws OperationException {
    LineContainers.setTopLevelContainerTagname("body");
    // Setup abbreviations from <pp> to <tagName>
    abbreviations.clear();
    abbreviations.add("<pp>", "<" + tagName + "></" + tagName + ">");
    abbreviations.add("</pp>", "");
    abbreviations.add("<pp/>", "<" + tagName + "></" + tagName + ">");
    abbreviations.add("<lc>", "<body>");
    abbreviations.add("</lc>", "</body>");

    // TODO(user): also test heights are ok when two <p>s are constructed with <enter>
    testEnterBackspaceDeleteWrap(
        "<pp>|</pp>",
        "<pp></pp><pp>|</pp>",
        "<pp>|</pp><pp></pp>"
    );
    testEnterBackspaceDeleteWrap(
        "<pp></pp><pp>|</pp><pp></pp>",
        "<pp></pp><pp></pp><pp>|</pp><pp></pp>",
        "<pp></pp><pp>|</pp><pp></pp><pp></pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>|abcd</pp>",
        "<pp></pp><pp>|abcd</pp>",
        "<pp>|</pp><pp>abcd</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>ab|cd</pp>",
        "<pp>ab</pp><pp>|cd</pp>",
        "<pp>ab|</pp><pp>cd</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>abcd|</pp>",
        "<pp>abcd</pp><pp>|</pp>",
        "<pp>abcd|</pp><pp></pp>"
        );

    testBackspaceWrap("<pp>|</pp><pp></pp>", "<pp>|</pp><pp></pp>");
    testBackspaceWrap("<pp>|xx</pp><pp></pp>", "<pp>|xx</pp><pp></pp>");
    testDeleteWrap("<pp></pp><pp>|</pp>", "<pp></pp><pp>|</pp>");
    testDeleteWrap("<pp></pp><pp>xx|</pp>", "<pp></pp><pp>xx|</pp>");

    /*
     * NOTE(patcoleman): the below use <i> and <u> tags for styles.
     * That behaviour for annotations is tested elsewhere, but any new actual element tags
     * could be tested here instead. For now, disabling.
    testEnterBackspaceDeleteWrap(
        "<pp>a|<i>bc</i>d</pp>",
        "<pp>a</pp><pp>|<i>bc</i>d</pp>",
        "<pp>a|</pp><pp><i>bc</i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>|bc</i>d</pp>",
        "<pp>a</pp><pp><i>|bc</i>d</pp>",
        "<pp>a|</pp><pp><i>bc</i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b|c</i>d</pp>",
        "<pp>a<i>b</i></pp><pp><i>|c</i>d</pp>",
        "<pp>a<i>b|</i></pp><pp><i>c</i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>bc|</i>d</pp>",
        "<pp>a<i>bc</i></pp><pp>|d</pp>",
        "<pp>a<i>bc|</i></pp><pp>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>bc</i>|d</pp>",
        "<pp>a<i>bc</i></pp><pp>|d</pp>",
        "<pp>a<i>bc|</i></pp><pp>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp><i>|abc</i>d</pp>",
        "<pp></pp><pp><i>|abc</i>d</pp>",
        "<pp>|</pp><pp><i>abc</i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>b|c</u></i>d</pp>",
        "<pp>a<i><u>b</u></i></pp><pp><i><u>|c</u></i>d</pp>",
        "<pp>a<i><u>b|</u></i></pp><pp><i><u>c</u></i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>|bc</u></i>d</pp>",
        "<pp>a</pp><pp><i><u>|bc</u></i>d</pp>",
        "<pp>a|</pp><pp><i><u>bc</u></i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>|<u>bc</u></i>d</pp>",
        "<pp>a</pp><pp><i><u>|bc</u></i>d</pp>",
        "<pp>a|</pp><pp><i><u>bc</u></i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a|<i><u>bc</u></i>d</pp>",
        "<pp>a</pp><pp><i><u>|bc</u></i>d</pp>",
        "<pp>a|</pp><pp><i><u>bc</u></i>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>bc|</u></i>d</pp>",
        "<pp>a<i><u>bc</u></i></pp><pp>|d</pp>",
        "<pp>a<i><u>bc|</u></i></pp><pp>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>bc</u>|</i>d</pp>",
        "<pp>a<i><u>bc</u></i></pp><pp>|d</pp>",
        "<pp>a<i><u>bc|</u></i></pp><pp>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>bc</u></i>|d</pp>",
        "<pp>a<i><u>bc</u></i></pp><pp>|d</pp>",
        "<pp>a<i><u>bc|</u></i></pp><pp>d</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i><u>bc</u></i>d|</pp>",
        "<pp>a<i><u>bc</u></i>d</pp><pp>|</pp>",
        "<pp>a<i><u>bc</u></i>d|</pp><pp></pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>|a<i>b<u>c</u>d</i>e</pp>",
        "<pp></pp><pp>|a<i>b<u>c</u>d</i>e</pp>",
        "<pp>|</pp><pp>a<i>b<u>c</u>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a|<i>b<u>c</u>d</i>e</pp>",
        "<pp>a</pp><pp>|<i>b<u>c</u>d</i>e</pp>",
        "<pp>a|</pp><pp><i>b<u>c</u>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>|b<u>c</u>d</i>e</pp>",
        "<pp>a</pp><pp>|<i>b<u>c</u>d</i>e</pp>",
        "<pp>a|</pp><pp><i>b<u>c</u>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b|<u>c</u>d</i>e</pp>",
        "<pp>a<i>b</i></pp><pp>|<i><u>c</u>d</i>e</pp>",
        "<pp>a<i>b|</i></pp><pp><i><u>c</u>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>|c</u>d</i>e</pp>",
        "<pp>a<i>b</i></pp><pp>|<i><u>c</u>d</i>e</pp>",
        "<pp>a<i>b|</i></pp><pp><i><u>c</u>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>c|</u>d</i>e</pp>",
        "<pp>a<i>b<u>c</u></i></pp><pp>|<i>d</i>e</pp>",
        "<pp>a<i>b<u>c|</u></i></pp><pp><i>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>c</u>|d</i>e</pp>",
        "<pp>a<i>b<u>c</u></i></pp><pp>|<i>d</i>e</pp>",
        "<pp>a<i>b<u>c|</u></i></pp><pp><i>d</i>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>c</u>d|</i>e</pp>",
        "<pp>a<i>b<u>c</u>d</i></pp><pp>|e</pp>",
        "<pp>a<i>b<u>c</u>d|</i></pp><pp>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>c</u>d</i>|e</pp>",
        "<pp>a<i>b<u>c</u>d</i></pp><pp>|e</pp>",
        "<pp>a<i>b<u>c</u>d|</i></pp><pp>e</pp>"
        );
    testEnterBackspaceDeleteWrap(
        "<pp>a<i>b<u>c</u>d</i>e|</pp>",
        "<pp>a<i>b<u>c</u>d</i>e</pp><pp>|</pp>",
        "<pp>a<i>b<u>c</u>d</i>e|</pp><pp></pp>"
        );
    */
  }

  /** Utility that tests backspace, wrapping everything in a line container */
  private void testBackspaceWrap(String first, String second) throws OperationException {
    testBackspace(format(first), format(second));
  }

  /** Utility that tests delete, wrapping everything in a line container */
  private void testDeleteWrap(String first, String second) throws OperationException {
    testDelete(format(first), format(second));
  }

  /** Utility that tests enter-backspace-delete, wrapping everything in a line container */
  private void testEnterBackspaceDeleteWrap(String first, String second, String third)
      throws OperationException {
    testEnterBackspaceDelete(format(first), format(second), format(third));
  }

  /** Utility that tests content, wrapping everything in a line container */
  private void testContentWrap(String content) throws OperationException {
    testContent(format(content));
  }

  /** Utility that wraps input content within a line container. */
  private String format(String input) {
    return "<lc>" + input + "</lc>";
  }

}
