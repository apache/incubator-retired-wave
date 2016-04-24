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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.testing.ContentSerialisationUtil;
import org.waveprotocol.wave.client.editor.testing.TestEditors;
import org.waveprotocol.wave.client.editor.testing.TestInlineDoodad;
import org.waveprotocol.wave.client.editor.testtools.ContentWithSelection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

/**
 * Test cut and paste extracting logic.
 *
 * TODO(user): Create standard unit tests that test logic with POJO
 * nodes.
 *
 * TODO(mtsui): Test paste headings and test copy.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author davidbyttow@google.com (David Byttow)
 */

public class PasteExtractorGwtTest extends GWTTestCase {
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    TestInlineDoodad.register(Editor.ROOT_HANDLER_REGISTRY, "label");
    TestInlineDoodad.register(Editor.ROOT_HANDLER_REGISTRY, "input");
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.extract.Tests";
  }

  public enum Operation {
    CUT,
    PASTE
  }

  private static final String LINE_CONTAINER_TAGNAME = "body";
  private static final String lcStart = "<" + LINE_CONTAINER_TAGNAME + ">";
  private static final String lcEnd = "</" + LINE_CONTAINER_TAGNAME + ">";

  static {
    LineContainers.setTopLevelContainerTagname(LINE_CONTAINER_TAGNAME);
  }

  private static String wrapWithLineContainer(String content) {
    return lcStart + content + lcEnd;
  }

  public void testCutNothing() {
    executeSimpleCut("<line></line>[]");
    executeSimpleCut("<line></line>a[]bc");
    executeSimpleCut("<line></line>[]abc");
    executeSimpleCut("<line></line>abc[]");
  }

  public void testCutElements() {
    executeSimpleCut("<line></line>[<label></label>]");
    executeSimpleCut("<line></line>[<label>abc</label>]");
    executeSimpleCut("<line></line>[<label>hello</label>]<line></line>some stuff here");
    executeSimpleCut("<line></line>[<label>abc</label>]<label></label>");
    executeSimpleCut("<line></line>[<label>abc</label><label></label>]");
    executeSimpleCut("<line></line>[<line></line><label>abc</label><label></label>]");
    executeSimpleCut("<line></line>[<line></line><label>abc</label>"
        + "<label></label><line></line>]<line></line>");
    executeCut("<line></line>[this<line></line>is<line></line>a<line></line>]test",
        "<line></line>test");
  }

  public void testPasteNothing() {
    executeSimplePaste("<line></line>|", "");
    executeSimplePaste("<line></line>ab|c", "");
    executeSimplePaste("<line></line>|abc", "");
    executeSimplePaste("<line></line>abc|", "");
  }

  public void testPasteInlineText() {
    executeSimplePaste("<line></line>|", "x");
    executeSimplePaste("<line></line>ab|c", "x");
    executeSimplePaste("<line></line>|abc", "x");
    executeSimplePaste("<line></line>abc|", "x");
    executeSimplePaste("<line></line>|", "xyz");
    executeSimplePaste("<line></line>ab|c", "xyz");
    executeSimplePaste("<line></line><label>|abc</label>", "xyz");
  }

//  TODO(danilatos/davidbyttow): Bring something similar to this back, once annotations extraction
//  is done for paste.
//
//  public void testPasteInlineElements() {
//    executePaste("<p>|</p>", "<b>x</b>", "<p><style fontWeight=\"bold\">x</style></p>");
//  }

  public void testPasteTwoLines() {
    // NOTE(user): Should this be the correct behaviour?
    // executePaste("<p>|</p>", "<p></p><p></p><p></p>abc", "<p></p><p></p><p></p><p>abc</p>");

    executePaste("<line></line>|", "x<p></p>y", "<line></line>x<line></line>y");
    executePaste("<line></line>ab|cd", "x<p></p>y",
        "<line></line>abx<line></line>ycd");
    executePaste("<line></line>123<line></line>|", "x<p></p>y",
        "<line></line>123<line></line>x<line></line>y");
    executePaste("<line></line>123<line></line>ab|cd", "x<p></p>y",
        "<line></line>123<line></line>abx<line></line>ycd");
    executePaste("<line></line>|<line></line>123", "x<p></p>y",
        "<line></line>x<line></line>y<line></line>123");
    executePaste("<line></line>ab|cd<line></line>123", "x<p>y</p>z",
        "<line></line>abx<line></line>y<line></line>zcd<line></line>123");
  }

  public void testPasteManyLines() {
    executePaste("<line></line>|", "x<p>y</p><p>z</p>",
        "<line></line>x<line></line>y<line></line>z");
    executePaste("<line></line>ab|cd", "x<p>y</p>z",
        "<line></line>abx<line></line>y<line></line>zcd");
    executePaste("<line></line>123<line></line>|", "x<p>y</p>z",
        "<line></line>123<line></line>x<line></line>y<line></line>z");
    executePaste("<line></line>123<line></line>ab|cd", "x<p>y</p>z",
        "<line></line>123<line></line>abx<line></line>y<line></line>zcd");
    executePaste("<line></line>|<line></line>123", "x<p>y</p>z",
        "<line></line>x<line></line>y<line></line>z<line></line>123");
    executePaste("<line></line>ab|cd<line></line>123", "x<p>y</p>z",
        "<line></line>abx<line></line>y<line></line>zcd<line></line>123");
  }

  public void testPasteSanitization() {
    executePaste("<line></line>|", "<c>f</c>", "<line></line>f");
    executePaste("<line></line>|", "<a href=\"foo.com\">f</a>", "<line></line>f");
  }

  public void testPasteExamples() {
    executePaste("<line></line>|", "<span>Inline</span>", "<line></line>Inline");
    executePaste("<line></line>|", "<p>Single Line</p>", "<line></line>Single Line");
    executePaste("<line></line>|", "<p>Multiple</p><p>Lines</p>",
        "<line></line>Multiple<line></line>Lines");
    executePaste("<line></line>|", "Plain<br/>Text<br/>Lines<br/>",
        "<line></line>Plain<line></line>Text<line></line>Lines");
    executePaste("<line></line>|", "Left<p>Mid</p>Right",
        "<line></line>Left<line></line>Mid<line></line>Right");
    executePaste("<line></line>x|y", "Left<p>Mid</p>Right",
        "<line></line>xLeft<line></line>Mid<line></line>Righty");
    executePaste("<line></line>|", "<p>Inner</p>Inline<p>Inner</p>",
        "<line></line>Inner<line></line>Inline<line></line>Inner");
    executePaste("<line></line>|", "<p>Div</p><div></div><p>Ignored</p>",
        "<line></line>Div<line></line>Ignored");

  }

  public void testPasteLists() {
    executePaste("<line></line>|", "<ul><li>1<li>2</ul>",
        "<line></line><line t=\"li\"></line>1<line t=\"li\"></line>2");
    executePaste("<line></line>|", "<ul><li>a</li><li>b</li></ul>",
        "<line></line><line t=\"li\"></line>a<line t=\"li\"></line>b");
    executePaste("<line></line>|", "<ul><li>a</li><li>b</li></ul>hello",
        "<line></line><line t=\"li\"></line>a<line t=\"li\"></line>b<line></line>hello");
    // <body><line></line><line t="li"></line>a<line t="li"></line>b<line></line>hello</body>
    // <body><line></line><line t="li"></line>a<line t="li"></line>bhello</body>
    executePaste("<line></line>hi|world", "<ul><li>a</li><li>b</li></ul>hello",
        "<line></line>hi<line t=\"li\"></line>a<line t=\"li\"></line>b<line></line>helloworld");

  }

  public void testPasteQuirks() {
    executePaste("<line></line>|",
        "<p>Extra Linebreak<br/></p>", "<line></line>Extra Linebreak");
    executePaste("<line></line>|", "&nbsp;X&nbsp;", "<line></line> X ");

    // NOTE(user): This is different to the previous behaviour, but is still
    // not unreasonable
    executePaste("<line></line>|", "<div><div><p>Nested Divs</p></div></div>",
        "<line></line>Nested Divs");
  }

  // TODO(danilatos): Test the following:
  // - Sanitisation, once the valid schema is a bit more stable
  // - Possible non-standard results of paste
  // TODO(user): Test that the cut content is actually in the clipboard
  // data.

  /**
   * Test the scenario where the selected content is literally deleted from the
   * original.
   * @param content The content containing the selection to be cut.
   */
  protected void executeSimpleCut(String content) {
    int begin = content.indexOf('[');
    int end = content.indexOf(']');
    String expected;
    if (begin >= 0 && end >= 0) {
      expected = content.substring(0, begin);
      expected += content.substring(end + 1);
    } else {
      expected = content;
    }
    executeCut(content, expected);
  }

  /**
   * Test a simple scenario where the xml result is simply the insertion of
   * the pasted html at the given collapsed caret
   */
  protected void executeSimplePaste(String initialContent, String pastedHtml) {
    executePaste(initialContent, pastedHtml, initialContent.replace("|", pastedHtml));
  }

  /**
   * Performs a cut operation and checks the initial content with the expected.
   * @param initialContent The initial XML fragment inside the document.
   * @param expectedContent The expected XML fragment inside the document
   *    as a result of the cut.
   */
  protected void executeCut(String initialContent, String expectedContent) {
    Element scratchContainer = Document.get().createDivElement();
    executeScenario(initialContent, expectedContent, scratchContainer, Operation.CUT);
  }


  /**
   * Performs a paste operation and checks the initial content with the expected.
   * @param initialContent The initial XML fragment inside the document.
   * @param expectedContent The expected XML fragment inside the document
   *    as a result of the paste.
   * @param pastedHtml The SGML fragment that would exist at the point of
   *    the vertical bar in <p>x|x</p>, if something were pasted there.
   */
  protected void executePaste(String initialContent, String pastedHtml, String expectedContent) {
    Element scratchContainer = Document.get().createDivElement();
    scratchContainer.setInnerHTML(pastedHtml);
    executeScenario(initialContent, expectedContent, scratchContainer, Operation.PASTE);
  }

  private static class TestBundle {
    final EditorImpl local;
    final EditorImpl remote;
    final PasteExtractor extractor;

    private TestBundle(String initialContent) {
      local = createEditor();
      RootPanel.get().add(local);

      ContentWithSelection content = new ContentWithSelection(initialContent);
      ContentSerialisationUtil.setContentString(local, content.content);
      local.getAggressiveSelectionHelper().setSelectionRange(
          new FocusedRange(content.selection, true));

      remote = createEditor();
      RootPanel.get().add(remote);

      remote.setContent(local.getDocumentInitialization(),
          ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);

      local.setOutputSink(new SilentOperationSink<DocOp>() {
        public void consume(DocOp op) {
          remote.getContent().consume(op);
        }
      });

      extractor = local.debugGetPasteExtractor();
    }

    private EditorImpl createEditor() {
      EditorImpl editor = (EditorImpl) TestEditors.getMinimalEditor();
      return editor;
    }

    public void destroy() {
      RootPanel.get().remove(local);
      RootPanel.get().remove(remote);
    }
  }

  /**
   * Performs either a "cut" or "paste" operation and checks the initial content
   * with the expected content.
   * @param initialContent The initial XML fragment inside the document.
   * @param expectedContent The expected XML fragment inside the document
   *    as a result of the operation.
   */
  private void executeScenario(String initialContent, String expectedContent,
      Element scratchContainer, Operation op) {
    initialContent = wrapWithLineContainer(initialContent);
    expectedContent = wrapWithLineContainer(expectedContent.replaceAll("[\\[\\]|]", ""));

    TestBundle ctx = new TestBundle(initialContent);

    if (op == Operation.CUT) {
      // Perform cut.
      ctx.extractor.performCopyOrCut(ctx.local, scratchContainer,
          ctx.local.getSelectionHelper().getOrderedSelectionPoints(), true);
    } else if (op == Operation.PASTE) {
      // Perform paste.
      ctx.extractor.extract(scratchContainer,
          ctx.local.getSelectionHelper().getOrderedSelectionPoints(), BiasDirection.LEFT);
    }

    String localResult = XmlStringBuilder.innerXml(ctx.local.getDocument()).toString();

    String remoteResult = XmlStringBuilder.innerXml(ctx.remote.getDocument()).toString();

    // Check the operation happened correctly
    assertEquals(expectedContent, localResult);

    // Check the generated operation resulted in the same thing
    assertEquals(localResult, remoteResult);
    ctx.destroy();
  }
}
