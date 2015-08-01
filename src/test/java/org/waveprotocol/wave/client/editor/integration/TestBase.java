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

import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.debug.logger.BufferedLogger;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.editor.testing.TestEditors;
import org.waveprotocol.wave.client.editor.testing.TestInlineDoodad;
import org.waveprotocol.wave.client.editor.testtools.Abbreviations;
import org.waveprotocol.wave.client.editor.testtools.ContentWithSelection;
import org.waveprotocol.wave.client.editor.testtools.EditorAssert;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import java.util.List;

/**
 * Base class for Editor unit tests
 *
 */
public abstract class TestBase extends GWTTestCase {

  /**
   * Grab EditorTest's editor
   */
  protected Editor editor;

  /**
   * A logger in case we're running the test in the harness
   */
  protected LoggerBundle logger;

  /**
   * A map of abbreviations, e.g.,
   * <smiles/> ->
   * <image attachment='pics/Smiles.jpg'>
   *    <caption>Smiles</caption>
   * </image>
   */
  protected Abbreviations abbreviations = new Abbreviations();

  private static final DocumentSchema TEST_SCHEMA = new DocumentSchema() {
    @Override
    public List<String> getRequiredInitialChildren(String typeOrNull) {
      return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS.getRequiredInitialChildren(typeOrNull);
    }

    @Override
    public boolean permitsAttribute(String type, String attributeName) {
      return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS.permitsAttribute(type, attributeName);
    }

    @Override
    public boolean permitsAttribute(String type, String attributeName, String attributeValue) {
      return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS.permitsAttribute(type, attributeName, attributeValue);
    }

    @Override
    public boolean permitsChild(String parentTypeOrNull, String childType) {
      return ("body".equals(parentTypeOrNull) && TestInlineDoodad.FULL_TAGNAME.equals(childType)) ||
        ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS.permitsChild(parentTypeOrNull, childType);
    }

    @Override
    public PermittedCharacters permittedCharacters(String typeOrNull) {
      if (TestInlineDoodad.FULL_TAGNAME.equals(typeOrNull)) {
        return PermittedCharacters.BLIP_TEXT;
      } else {
        return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS.permittedCharacters(typeOrNull);
      }
    }
  };

  /**
   * {@inheritDoc}
   */
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.integration.Tests";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    prepareTest();
  }

  /**
   * Prepares test. Call this at beginning of each testXXX method.
   *
   * This is needed because you cannot call GWT.create() inside setUp()
   */
  protected void prepareTest() {
    if (logger == null) {
      logger = new BufferedLogger("test");

      if (editor == null) {
        editor = createEditor();
      }
    }
    LineContainers.setTopLevelContainerTagname("body");
    abbreviations.clear();
  }

  protected Editor createEditor() {
    return TestEditors.getMinimalEditor();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();
  }

  /**
   * Asserts that the selection in an editor (roughly) matches that given
   * by a {@link ContentWithSelection} as defined by
   * {@link EditorAssert#assertLocationsEquals(String, int, int, String)}
   *
   * @param msg
   * @param expected
   * @param actualEditor
   */
  protected void assertSelectionEquals(
      String msg, ContentWithSelection expected, Editor actualEditor) {

    EditorAssert.assertSelectionEquals(msg + ". Selections differ. ",
        expected.selection,  // TODO(danilatos): Check directed ranges
        actualEditor.getSelectionHelper().getSelectionRange() != null
            ? actualEditor.getSelectionHelper().getSelectionRange().asRange() : null,
        expected.content);
  }

  /**
   * Asserts Editor content and selection is as expected.
   *
   * @param msg
   * @param expectedContentWithSelection
   * @param actualEditor
   * @throws OperationException
   */
  protected void assertEditorContent(String msg,
      String expectedContentWithSelection, Editor actualEditor)
      throws OperationException {

    // Parse the expected content
    ContentWithSelection expected =
        parseContent(expectedContentWithSelection);

    // Assert result
    assertEditorContent(
        msg, expected, actualEditor);
  }

  /**
   * Asserts Editor content and selection is as expected
   *
   * @param msg
   * @param expected
   * @param actualEditor
   */
  protected void assertEditorContent(String msg,
      ContentWithSelection expected, Editor actualEditor) {
    // Assert content
    EditorAssert.assertXmlEquals(msg,
        expected.content,
        ContentSerialisationUtil.getContentString(editor));
    // Assert selection
    assertSelectionEquals(msg,
        expected,
        editor);
  }

  /**
   * Unabbreviates and parses selection from strings like <p>ab|cd</p>
   *
   * @param content
   * @return parsed content + selection
   * @throws OperationException
   */
  protected ContentWithSelection parseContent(String content)
      throws OperationException {
    return new ContentWithSelection(abbreviations.expand(content));
  }

  /**
   * Sets editor content and selection with string like <p>a[bc]d</p>
   * First expands the string according to current abbreviations.
   * Also asserts that resulting content + selection got set properly
   *
   * @param editor
   * @param content
   * @throws OperationException
   */
  protected void setContent(Editor editor, String content)
      throws OperationException {

    // Parse content
    ContentWithSelection parsed = parseContent(content);

    // Set content + selection in editor
    editor.setContent(DocProviders.POJO.parse(parsed.content).asOperation(),
        TEST_SCHEMA);
    editor.getSelectionHelper().setSelectionRange(parsed.selection == null ? null
        : new FocusedRange(parsed.selection.getStart(), parsed.selection.getEnd()));

    // Assert editor health (editor already does this in debug builds)
    if (!LogLevel.showDebug()) {
      EditorTestingUtil.checkHealth(editor);
    }
    // Assert content and selection
    assertEditorContent(
        "Editor.setContent(" + abbreviations.expand(content) + ")", parsed, editor);
  }
}
