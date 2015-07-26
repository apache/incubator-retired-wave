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

import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;

/**
 * Unit test applying Operations thru {@link Editor#execute(DocOp)}
 *
 */

public class OperationGwtTest extends TestBase {
  /**
   * Tests that applying an operation results in correct
   * content and selection
   *
   * @param startContent
   * @param nindo
   * @param expectedContent
   * @throws OperationException
   */
  private void testOperation(String startContent,
      Nindo nindo, String expectedContent)
      throws OperationException {

    // Compute a message identifying the test
    String msg = startContent + " + " + nindo + " => " + expectedContent;
    logger.trace().logXml("Testing: " + msg);

    // Execute the operation on starting content
    setContent(editor, startContent);
    // Get a docOp out of the nindo
    DocOp docOp = getDocOpFromNindo(nindo);
    editor.getContent().consume(docOp);

    // Assert editor health (editor already does this in debug builds)
    if (!LogLevel.showDebug()) {
      EditorTestingUtil.checkHealth(editor);
    }

    // Assert result
    assertEditorContent(msg, expectedContent, editor);
  }

  private DocOp getDocOpFromNindo(Nindo nindo) {
    try {
      return DocProviders.POJO.build(editor.getDocumentInitialization(),
          DocumentSchema.NO_SCHEMA_CONSTRAINTS).consumeAndReturnInvertible(nindo);
    } catch (OperationException e) {
      throw new OperationRuntimeException("initialization failed", e);
    }
  }

  /**
   * Tests that applying an operation causes exception
   * TODO(user): consider testing for specific exception types
   *
   * @param startContent
   * @param operation
   */
  private void testOperationFailure(
      String startContent, Nindo nindo) {

    // Compute a message identifying the test
    String msg = startContent + " + " + nindo + " => FAIL";
    logger.trace().logXml("Testing: " + msg);

    try {
      // Execute the operation on starting content
      setContent(editor, startContent);

      DocOp docOp = getDocOpFromNindo(nindo);
      editor.getContent().consume(docOp);
    } catch (Throwable e) {
      // Test succeeds
      return;
    }

    // We didn't get the exception
    fail(msg);
  }

  /**
   * {@link #testOperation} specialised to InsertText operation
   *
   * @param startContent
   * @param insertionPoint
   * @param insertString
   * @param expectedContent
   * @throws OperationException
   */
  public void testInsertText(String startContent,
      int insertionPoint, String insertString, String expectedContent)
      throws OperationException {
    // Fix to wrap in line container body
    startContent = "<body>" + abbreviations.expand(startContent) + "</body>";
    expectedContent = "<body>" + abbreviations.expand(expectedContent) + "</body>";
    testOperation(
        startContent,
        Nindo.insertCharacters(insertionPoint, insertString),
        expectedContent);
  }

  /**
   * {@link #testOperationFailure} specialised to InsertText operation
   *
   * @param startContent
   * @param insertionPoint
   * @param insertString
   */
  private void testInsertTextFailure(
      String startContent, int insertionPoint, String insertString) {
    // Fix to wrap in line container body
    startContent = "<body>" + abbreviations.expand(startContent) + "</body>";
    testOperationFailure(
        startContent,
        Nindo.insertCharacters(insertionPoint, insertString));
  }

  /**
   * {@link #testOperation} specialised to InsertXML operation
   *
   * @param startContent
   * @param insertionPoint
   * @param insertXML
   * @param insertLength
   * @param expectedContent
   * @throws OperationException
   */
  public void testInsertXML(String startContent,
      int insertionPoint, String insertXML, int insertLength,
      String expectedContent)
      throws OperationException {
    // TODO(danilatos): Bring this back
    /*
    testOperation(
        startContent,
        new XmlInsertXml(insertionPoint, insertXML, insertLength),
        expectedContent);
        */
  }

  /**
   * {@link #testOperationFailure} specialised to InsertXML operation
   *
   * @param startContent
   * @param insertionPoint
   * @param insertXML
   * @param insertLength
   */
  @SuppressWarnings("unused")  // TODO(user): Add this back.
  private void testInsertXMLFailure(String startContent,
      int insertionPoint, String insertXML, int insertLength) {
    // TODO(danilatos): Bring this back
    /*
    testOperationFailure(
        startContent,
        new XmlInsertXml(insertionPoint, insertXML, insertLength));
        */
  }

  /**
   * {@link #testOperation} specialised to Delete operation
   *
   * @param startContent
   * @param start
   * @param end
   * @param expectedContent
   * @throws OperationException
   */
  private void testDelete(String startContent,
      int start, int end,
      String expectedContent)
      throws OperationException {
    // TODO(danilatos): Bring this back
    /*
    testOperation(
        startContent,
        new XmlDelete(start, end),
        expectedContent);
        */
  }

  /**
   * {@link #testOperationFailure} specialised to Delete operation
   *
   * @param startContent
   * @param start
   * @param end
   */
  private void testDeleteFailure(String startContent, int start, int end) {
    // TODO(danilatos): Bring this back
    /*
    testOperationFailure(
        startContent,
        new XmlDelete(start, end));
        */
  }

  /**
   * Tests insert text operation
   *
   * @throws OperationException
   */
  public void testInsertText()
      throws OperationException {
    prepareTest();

    // Setup abbreviations from <l/> to <tagName/>
    abbreviations.add("<l/>", "<" + LineContainers.LINE_TAGNAME + "/>");
    abbreviations.add("<u>", "<span>");
    abbreviations.add("</u>", "</span>");
    abbreviations.add("<i>", "<span>");
    abbreviations.add("</i>", "</span>");

    // Test inserting X into empty paragraph
    testInsertText(
        "<l/>",
        3, "X",
        "<l/>X"
    );

    // Test inserting X into simple text node
    testInsertText(
        "<l/>abcd",
        3, "X",
        "<l/>Xabcd"
    );
    testInsertText(
        "<l/>abcd",
        5, "X",
        "<l/>abXcd"
    );
    testInsertText(
        "<l/>abcd",
        7, "X",
        "<l/>abcdX"
    );

    // Test inserting X into slightly more complex DOM,
    // including creating new text node
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        3, "X",
        "<l/>Xa<i>bc</i><u>de</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        4, "X",
        "<l/>aX<i>bc</i><u>de</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        5, "X",
        "<l/>a<i>Xbc</i><u>de</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        7, "X",
        "<l/>a<i>bcX</i><u>de</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        8, "X",
        "<l/>a<i>bc</i>X<u>de</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        9, "X",
        "<l/>a<i>bc</i><u>Xde</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        10, "X",
        "<l/>a<i>bc</i><u>dXe</u>f"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        12, "X",
        "<l/>a<i>bc</i><u>de</u>Xf"
    );
    testInsertText(
        "<l/>a<i>bc</i><u>de</u>f",
        13, "X",
        "<l/>a<i>bc</i><u>de</u>fX"
    );

    // Test we can insert special xml chars in text nodes w/o escaping
    testInsertText(
        "<l/>abcd",
        5, "<br/>",
        "<l/>ab&lt;br/&gt;cd"
    );

    // Borrowed from zdwang...
    /*
    testInsertText(
        "<l/>" +
          "<i>ab</i>" +
          "cd " +
          "<b>" +
            "e" +
            "<i>fg</i>" +
          "</b>" +
          " h" +
        "",
        6, "12",
        "<l/>" +
          "<i>ab</i>" +
          "12cd " +
          "<b>" +
            "e" +
            "<i>fg</i>" +
          "</b>" +
          " h" +
        ""
    );
    testInsertText(
        "<l/>" +
          "<i>ab</i>" +
          "cd " +
          "<b>" +
            "e" +
            "<i>fg</i>" +
          "</b>" +
          " h" +
        "",
        13, " 12 ",
        "<l/>" +
          "<i>ab</i>" +
          "cd " +
          "<b>" +
            "e" +
            "<i>f 12 g</i>" +
          "</b>" +
          " h" +
        ""
    );
    */

    // Test caret preservation when inserting X into empty paragraph
    testInsertText(
        "<l/>|",
        3, "X",
        "<l/>|X"
    );

    // Test caret preservation when inserting X into simple text node
    // We want the caret to stay left of the insertion when the insertion
    // point coincides with the caret location.
    testInsertText(
        "<l/>ab|cd",
        3, "X",
        "<l/>Xab|cd"
    );
    testInsertText(
        "<l/>ab|cd",
        4, "X",
        "<l/>aXb|cd"
    );
    testInsertText(
        "<l/>ab|cd",
        5, "X",
        "<l/>ab|Xcd"
    );
    testInsertText(
        "<l/>ab|cd",
        6, "X",
        "<l/>ab|cXd"
    );
    testInsertText(
        "<l/>ab|cd",
        7, "X",
        "<l/>ab|cdX"
    );
    testInsertText(
        "<l/>|abcd",
        3, "X",
        "<l/>|Xabcd"
    );
    testInsertText(
        "<l/>|abcd",
        4, "X",
        "<l/>|aXbcd"
    );
    testInsertText(
        "<l/>abcd|",
        7, "X",
        "<l/>abcd|X"
    );
    testInsertText(
        "<l/>abcd|",
        6, "X",
        "<l/>abcXd|"
    );

    // Test selection preservation when inserting XX into simple text node
    // We want the insertion to stay outside the selection when the insertion
    // point coincides with an end point in the selection
    testInsertText(
        "<l/>a[bc]d",
        3, "XX",
        "<l/>XXa[bc]d"
    );
    // TODO(user): debate outcome with zdwang and alexmah
    //  "<l/>aXX[bc]d"
    testInsertText(
        "<l/>a[bc]d",
        4, "XX",
        "<l/>a[XXbc]d"
    );
    testInsertText(
        "<l/>a[bc]d",
        5, "XX",
        "<l/>a[bXXc]d"
    );
    testInsertText(
        "<l/>a[bc]d",
        6, "XX",
        "<l/>a[bc]XXd"
    );
    testInsertText(
        "<l/>a[bc]d",
        7, "XX",
        "<l/>a[bc]dXX"
    );

    // TODO(user): debate outcome with zdwang and alexmah
    //  "<l/>XX[ab]cd"
    testInsertText(
        "<l/>[ab]cd",
        3, "XX",
        "<l/>[XXab]cd"
    );
    testInsertText(
        "<l/>[ab]cd",
        4, "XX",
        "<l/>[aXXb]cd"
    );
    testInsertText(
        "<l/>[ab]cd",
        5, "XX",
        "<l/>[ab]XXcd"
    );
    testInsertText(
        "<l/>[ab]cd",
        6, "XX",
        "<l/>[ab]cXXd"
    );
    testInsertText(
        "<l/>[ab]cd",
        7, "XX",
        "<l/>[ab]cdXX"
    );
    testInsertText(
        "<l/>ab[cd]",
        3, "XX",
        "<l/>XXab[cd]"
    );
    testInsertText(
        "<l/>ab[cd]",
        4, "XX",
        "<l/>aXXb[cd]"
    );

    // TODO(user): debate outcome with zdwang and alexmah
    //  "<l/>abXX[cd]"
    testInsertText(
        "<l/>ab[cd]",
        5, "XX",
        "<l/>ab[XXcd]"
    );
    testInsertText(
        "<l/>ab[cd]",
        6, "XX",
        "<l/>ab[cXXd]"
    );
    testInsertText(
        "<l/>ab[cd]",
        7, "XX",
        "<l/>ab[cd]XX"
    );
  }

  public void testInsertInvalidText() {
    // Test invalid insertion point
    testInsertTextFailure(
        "<l/>abcd",
        9, "X"
    );
    testInsertTextFailure(
        "<l/>abcd",
        -1, "X"
    );
    testInsertTextFailure(
        "<l/><i>abcd</i>",
        11, "X"
    );
  }

  /**
   * Tests insert xml operation
   *
   * @throws OperationException
   */
  public void testInsertXML()
      throws OperationException {

    prepareTest();

    // Setup abbreviations from <l/> to <tagName/>
    abbreviations.add("<l/>", "<" + LineContainers.LINE_TAGNAME + "/>");
    abbreviations.add("<u>", "<label>");
    abbreviations.add("</u>", "</label>");
    abbreviations.add("<i>", "<label>");
    abbreviations.add("</i>", "</label>");

    // Test inserting simple <br/> element into text node
    // TODO(user, lars): XmlInsertXml doesn't parse <br/> correctly
    testInsertXML(
        "<l/>abcd",
        3, "<br/>", 2,
        "<l/><br/>abcd"
    );
    testInsertXML(
        "<l/>abcd",
        5, "<br/>", 2,
        "<l/>ab<br/>cd"
    );
    testInsertXML(
        "<l/>abcd",
        7, "<br/>", 2,
        "<l/>abcd<br/>"
    );

    // Test inserting <br/> into slightly more complex DOM
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        3, "<br/>", 2,
        "<l/><br/>a<i>bc</i><u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        4, "<br/>", 2,
        "<l/>a<br/><i>bc</i><u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        5, "<br/>", 2,
        "<l/>a<i><br/>bc</i><u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        7, "<br/>", 2,
        "<l/>a<i>bc<br/></i><u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        8, "<br/>", 2,
        "<l/>a<i>bc</i><br/><u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        9, "<br/>", 2,
        "<l/>a<i>bc</i><u><br/>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        10, "<br/>", 2,
        "<l/>a<i>bc</i><u>d<br/>e</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        12, "<br/>", 2,
        "<l/>a<i>bc</i><u>de</u><br/>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>f",
        13, "<br/>", 2,
        "<l/>a<i>bc</i><u>de</u>f<br/>"
    );

    // Test inserting more complex stuff
    testInsertXML(
        "<l/>abcd",
        8, "<l/>efgh", 6,
        "<l/>abcd<l/>efgh"
    );
    testInsertXML(
        "<l/>abcd",
        3, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>V<i>X</i><u>Y</u>ZZabcd"
    );
    testInsertXML(
        "<l/>abcd",
        5, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>abV<i>X</i><u>Y</u>ZZcd"
    );
    testInsertXML(
        "<l/>abcd",
        7, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>abcdV<i>X</i><u>Y</u>ZZ"
    );

    // inserting A with attribute
    // TODO(user, lars): getContent fails to report href attribute
//    ContentElement.registerSchema("a", ContentElement.Schema.create(new String[] {"href"}));
//    testInsertXML(
//        "<l/>abc",
//        4, "<a href='http://www.google.com/'>google</a>", 1,
//        "<l/>ab<a href=\"http://www.google.com/\">google</a>c"
//    );

    // Test invalid operations (length wrong)
    // TODO(user, lars): XmlInsertXml doesn't fail when length wrong
//    testInsertXMLFailure(
//        "<l/>abcd",
//        4, "V<i>X</i><u>Y</u>ZZ", 8
//    );
//    testInsertXMLFailure(
//        "<l/>abcd",
//        4, "V<i>X</i><u>Y</u>ZZ", 10
//    );

    // Test caret preservation when inserting into text node
    testInsertXML(
        "<l/>ab|cd",
        4, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>aV<i>X</i><u>Y</u>ZZb|cd"
    );
    testInsertXML(
        "<l/>ab|cd",
        5, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>ab|V<i>X</i><u>Y</u>ZZcd"
    );
    testInsertXML(
        "<l/>ab|cd",
        6, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>ab|cV<i>X</i><u>Y</u>ZZd"
    );

    // Test caret preservation when inserting outside text node
    testInsertXML(
        "<l/>a<i>bc|</i><u>de</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>bc|</i>V<i>X</i><u>Y</u>ZZ<u>de</u>f"
    );
    testInsertXML(
        "<l/>a|<i>bc</i><u>de</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a|<i>bc</i>V<i>X</i><u>Y</u>ZZ<u>de</u>f"
    );
    /* TODO(user): bring this test back somehow. Problem is that
    // browsers don't like placing the caret between the <i> and <u>
    // nodes. Different browsers move the caret to different locations
    // inside text nodes + the result of the test then differs :-(
    testInsertXML(
        "<l/>a<i>bc</i>|<u>de</u>f",
        new int[] {0, 2}, "V<i>X</i><u>Y</u>ZZ", 5,
        "<l/>a<i>bc</i>|V<i>X</i><u>Y</u>ZZ<u>de</u>f"
    );*/
    testInsertXML(
        "<l/>a<i>bc</i><u>d|e</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>bc</i>V<i>X</i><u>Y</u>ZZ<u>d|e</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>de</u>|f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>bc</i>V<i>X</i><u>Y</u>ZZ<u>de</u>|f"
    );

    // Test selection preservation when inserting into text node
    testInsertXML(
        "<l/>[ab]cd",
        5, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>[ab]V<i>X</i><u>Y</u>ZZcd"
    );
    testInsertXML(
        "<l/>a[bc]d",
        5, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a[bV<i>X</i><u>Y</u>ZZc]d"
    );
    // TODO(user): debate outcome with zdwang and alexmah
    //  "<l/>abV<i>X</i><u>Y</u>ZZ[cd]"
    testInsertXML(
        "<l/>ab[cd]",
        5, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>ab[V<i>X</i><u>Y</u>ZZcd]"
    );

    // Test selection preservation when inserting outside text node
    testInsertXML(
        "<l/>[a<i>bc</i>]<u>de</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>[a<i>bc</i>]V<i>X</i><u>Y</u>ZZ<u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>b[c</i><u>d]e</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>b[c</i>V<i>X</i><u>Y</u>ZZ<u>d]e</u>f"
    );
    /* TODO(user): bring this test back somehow. Problem is that
    // browsers don't like placing the caret between the <i> and <u>
    // nodes. Different browsers move the caret to different locations
    // inside text nodes + the result of the test then differs :-(
    // TODO(user): debate outcome with zdwang and alexmah
    //  "<l/>a<i>bc</i>V<i>X</i><u>Y</u>ZZ[<u>de</u>f]"
    testInsertXML(
        "<l/>a<i>bc</i>[<u>de</u>f]",
        new int[] {0, 2}, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>bc</i>[V<i>X</i><u>Y</u>ZZ<u>de</u>f]"
    );*/
    testInsertXML(
        "<l/>[a<i>b]c</i><u>de</u>f",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>[a<i>b]c</i>V<i>X</i><u>Y</u>ZZ<u>de</u>f"
    );
    testInsertXML(
        "<l/>a<i>bc</i><u>d[e</u>f]",
        8, "V<i>X</i><u>Y</u>ZZ", 9,
        "<l/>a<i>bc</i>V<i>X</i><u>Y</u>ZZ<u>d[e</u>f]"
    );
  }

  /**
   * Tests delete operation
   *
   * @throws OperationException
   */
  public void testDelete()
      throws OperationException {

    prepareTest();

    // Setup abbreviations from <l/> to <tagName/>
    abbreviations.add("<l/>", "<" + LineContainers.LINE_TAGNAME + "/>");
    abbreviations.add("<u>", "<label>");
    abbreviations.add("</u>", "</label>");
    abbreviations.add("<i>", "<label>");
    abbreviations.add("</i>", "</label>");

    // simple delete all
    testDelete(
        "<l/>abcd",
        3, 7,
        "<p/>"
    );
    // simple delete some
    testDelete(
        "<l/>abcde",
        4, 7,
        "<l/>ae"
    );
    // simply delete some child node
    testDelete(
        "<l/>ab<i>c</i>de",
        4, 5,
        "<l/>a<i>c</i>de");
    // delete <i> node
    testDelete(
        "<l/>ab<i>cd</i>ef",
        5, 9,
        "<l/>abef");


    // fail to delete across a tree from shallow to deep
    // TODO(user): await answer from alex and maybe bring the delete test below
    // back...
//    testDelete(
//        "<l/>ab<i>cd</i>ef",
//        3, 6,
//        "<l/>a<i>d</i>ef"
//    );
    // fail to delete across a tree from deep to shallow
    testDeleteFailure(
        "<l/>ab<i>cd</i>ef",
        7, 10
    );
    // fail to delete across a tree at equal depth
    testDeleteFailure(
        "<l/><i>ab</i><i>cd</i>",
        5, 9
    );
    // fail to delete across a tree where the start point is deeper
    testDeleteFailure(
        "<l/><i>a<b>ef</b>b</i><i>cd</i>",
        7, 13
    );
    // fail to delete across a tree where the end point is deeper
    testDeleteFailure(
        "<l/><i>ab</i><i>c<b>ef</b>d</i>",
        5, 11
    );
    // fail to delete across a tree where both points are deep
    testDeleteFailure(
        "<l/><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i>",
        7, 15
    );
    // fail to delete across a tree where both points are the leftmost points in
    // their containers
    testDeleteFailure(
        "<l/><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i>",
        6, 14
    );
    // fail to delete across a tree where both points are the rightmost points in
    // their containers
    // TODO(danilatos): this delete silently does nothing, when it should probably
    // throw an exception
//    testDeleteFailure(
//        "<l/><i>a<b>ef</b>b</i><i>c<b>ef</b>d</i>",
//        7, 15
//    );
    // fail to delete across a tree where the each child path passes through an
    // element node that is a leftmost sibling after the two paths diverge
    testDeleteFailure(
        "<l/><i><b>ef</b>ab</i><i><b>ef</b>cd</i>",
        6, 14
    );
    // fail to delete across a tree where the each child path passes through an
    // element node that is a rightmost sibling after the two paths diverge
    testDeleteFailure(
        "<l/><i>ab<b>ef</b></i><i>cd<b>ef</b></i>",
        8, 16
    );
    // fail to delete from parent to child
    testDeleteFailure(
        "<l/>ab<i>cd</i>ef",
        5, 7
    );
    // fail to delete from child to parent
    testDeleteFailure(
        "<l/>ab<i>cd</i>ef",
        7, 9
    );
    // fail to delete inverted range
//    testDeleteFailure(
//        "<l/>abcde",
//        4, 3
//    );

    // simple fail, index out of bounds
    testDeleteFailure(
        "<l/>abcde",
        5, 12
    );
    // simple fail, start not found
    testDeleteFailure(
        "<l/>abcde",
        12, 14
    );
  }

}
