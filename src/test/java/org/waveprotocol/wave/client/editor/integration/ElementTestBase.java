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

import com.google.gwt.event.dom.client.KeyCodes;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.testing.FakeEditorEvent;

import org.waveprotocol.wave.model.operation.OperationException;

/**
 * Base test case for Element tests
 *
 */
public abstract class ElementTestBase extends TestBase {

  /**
   * Test that content makes editor at least minHeight pixels tall
   *
   * @param content
   * @param minHeight
   * @throws OperationException
   */
  protected void testMinHeight(String content, int minHeight)
    throws OperationException {

    // Compute a message identifying the test
    String msg = content + " should be at least " + minHeight + " pixels tall";
    logger.trace().logXml("Testing: " + msg);

    // Measure height
    setContent(editor, content);
    int height = editor.getWidget().getOffsetHeight();

    // Assert
    assertTrue(msg, height >= minHeight);
  }

  /**
   * Tests that firstContent makes editor the same height as secondContent
   *
   * @param firstContent
   * @param secondContent
   * @throws OperationException
   */
  protected void testEqualHeight(String firstContent, String secondContent)
      throws OperationException {

    // Compute a message identifying the test
    String msg = firstContent + " should be same height as " + secondContent;
    logger.trace().logXml("Testing: " + msg);

    // Measure heights
    setContent(editor, firstContent);
    int firstHeight = editor.getWidget().getOffsetHeight();
    setContent(editor, secondContent);
    int secondHeight = editor.getWidget().getOffsetHeight();

    // Assert
    assertEquals(msg, firstHeight, secondHeight);
  }

  /**
   * Tests that firstContent makes editor roughly the same as secondContent
   *
   * @param firstContent
   * @param secondContent
   * @param maxDelta
   * @throws OperationException
   */
  protected void testRoughlyEqualHeight(String firstContent, String secondContent, int maxDelta)
      throws OperationException {

    // Compute a message identifying the test
    String msg = firstContent + " should be within " + maxDelta
        + " pixel height of " + secondContent;
    logger.trace().logXml("Testing: " + msg);

    // Measure heights
    setContent(editor, firstContent);
    int firstHeight = editor.getWidget().getOffsetHeight();
    setContent(editor, secondContent);
    int secondHeight = editor.getWidget().getOffsetHeight();

    // Assert
    assertTrue(msg, Math.abs(firstHeight - secondHeight) <= maxDelta);
  }

  /**
   * Tests that tallerContent makes editor taller than shorterContent
   * by at least minDelta
   *
   * @param tallerContent
   * @param shorterContent
   * @param minDelta
   * @throws OperationException
   */
  protected void testTaller(
      String tallerContent, String shorterContent, int minDelta)
      throws OperationException {

    // Compute a message identifying the test
    String msg = tallerContent + " should be at least " + minDelta +
        " pixels taller than " + shorterContent;
    logger.trace().logXml("Testing: " + msg);

    // Measure heights
    setContent(editor, tallerContent);
    int tallerHeight = editor.getWidget().getOffsetHeight();
    setContent(editor, shorterContent);
    int shorterHeight = editor.getWidget().getOffsetHeight();

    // Assert
    assertTrue(msg, tallerHeight >= shorterHeight + minDelta);
  }

  /**
   * Tests that editor can set content + selection correctly
   *
   * @param content
   * @throws OperationException
   */
  protected void testContent(String content)
      throws OperationException {
    logger.trace().logXml("Testing: " + content);
    setContent(editor, content);
  }

  /**
   * Tests that a single call to handleKeyDown results in correct content
   *
   * @param startContent
   * @param expectedContent
   * @param keyName
   * @param editorEvent
   * @throws OperationException
   */
  protected void testKeyDown(String startContent, String expectedContent,
      String keyName, EditorEvent editorEvent) throws OperationException {

    // Compute a message identifying the test
    String msg = abbreviations.expand(startContent) + " + " + keyName + " -> " +
        abbreviations.expand(expectedContent);
    logger.trace().logXml("Testing: " + msg);

    // Place content in editor, and simulate an enter hit
    setContent(editor, startContent);
    ((EditorImpl) editor).debugGetEventHandler().handleEvent(editorEvent);

    // Assert editor health (editor already does this in debug builds)
    if (!LogLevel.showDebug()) {
      EditorTestingUtil.checkHealth(editor);
    }

    // Assert result
    assertEditorContent(msg, expectedContent, editor);
  }

  /**
   * Tests that a single <enter> keydown results in the correct content
   *
   * @param startContent
   * @param expectedContent
   * @throws OperationException
   */
  protected void testEnter(String startContent, String expectedContent)
      throws OperationException {
    testKeyDown(startContent, expectedContent, "<enter>", FakeEditorEvent.create(
        KeySignalType.INPUT, KeyCodes.KEY_ENTER));
  }

  /**
   * Tests that a single <backspace> keydown results in the correct content
   *
   * @param startContent
   * @param expectedContent
   * @throws OperationException
   */
  protected void testBackspace(String startContent, String expectedContent)
      throws OperationException {
    testKeyDown(startContent, expectedContent, "<backspace>", FakeEditorEvent.create(
        KeySignalType.DELETE, KeyCodes.KEY_BACKSPACE));
  }

  /**
   * Tests that a single <delete> keydown results in the correct content
   *
   * @param startContent
   * @param expectedContent
   * @throws OperationException
   */
  protected void testDelete(String startContent, String expectedContent)
      throws OperationException {
    testKeyDown(startContent, expectedContent, "<delete>", FakeEditorEvent.create(
        KeySignalType.DELETE, KeyCodes.KEY_DELETE));
  }

  /**
   * Tests <enter>, <backspace> and <delete> in a single go
   *
   * @param closed e.g., <p>|</p>
   * @param backspaceOpen e.g., <p></p><p>|</p>
   * @param deleteOpen e.g., <p>|</p><p></p>
   * @throws OperationException
   */
  protected void testEnterBackspaceDelete(
      String closed, String backspaceOpen, String deleteOpen)
      throws OperationException {
    testEnter(closed, backspaceOpen);
    testBackspace(backspaceOpen, closed);
    testDelete(deleteOpen, closed);
  }
}
