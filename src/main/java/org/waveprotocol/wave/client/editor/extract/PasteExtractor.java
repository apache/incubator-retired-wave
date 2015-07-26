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

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.clipboard.AnnotationSerializer;
import org.waveprotocol.wave.client.clipboard.Clipboard;
import org.waveprotocol.wave.client.clipboard.PasteBufferImpl;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.editor.EditorInstrumentor;
import org.waveprotocol.wave.client.editor.EditorInstrumentor.Action;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.impl.HtmlViewImpl;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.client.scheduler.CommandQueue;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.indexed.Validator;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.Nindo.Builder;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.richtext.RichTextMutationBuilder;
import org.waveprotocol.wave.model.richtext.RichTextTokenizer;
import org.waveprotocol.wave.model.richtext.RichTextTokenizerImpl;
import org.waveprotocol.wave.model.richtext.RichTextTokenizerImplFirefox;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.List;

/**
 * Extractor to handle paste events
 *
 * Works by placing the caret in a hidden div, then allowing the paste to happen
 * there. We then interpret the html, clean up/adapt as necessary, grab it as
 * xml and mutate our document accordingly (and put the selection back)
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author davidbyttow@google.com (David Byttow)
 *
 * TODO(user): Scrollbar should be updated when pasting a lot of text.
 */
public class PasteExtractor {
  public static final LoggerBundle LOG = new DomLogger("paste");

  private static final Clipboard clipboard = Clipboard.get();
  // NOTE(user): Remove this once PasteBuffer functionality is abstracted into clipboard
  private static final PasteBufferImpl pasteBuffer = clipboard.getPasteBuffer();

  private boolean busy = false;

  private final CommandQueue deferredCommands;

  private final SelectionHelper aggressiveSelectionHelper;

  private final CMutableDocument mutableDocument;

  private final OperationSequencer<Nindo> operationSequencer;

  private final ReadableDocumentView<ContentNode, ContentElement, ContentTextNode>
      renderedContent;
  private final ReadableDocumentView<ContentNode, ContentElement, ContentTextNode>
      persistentContent;

  private static final PasteFormatRenderer PASTE_FORMAT_RENDERER = PasteFormatRenderer.get();

  private final SubTreeXmlRenderer<ContentNode, ContentElement, ContentTextNode> subtreeRenderer;
  private final PasteAnnotationLogic<ContentNode, ContentElement, ContentTextNode> annotationLogic;
  private final Validator validator;

  private final EditorInstrumentor instrumentor;

  private final boolean useSemanticCopyPaste;

  /**
   * Constructor.
   *
   * @param deferredCommands
   * @param aggressiveSelectionHelper
   * @param mutableDocument
   * @param operationSequencer
   */
  public PasteExtractor(CommandQueue deferredCommands,
      SelectionHelper aggressiveSelectionHelper,
      CMutableDocument mutableDocument,
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> renderedContent,
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> persistentContent,
      AnnotationRegistry annotationRegistry,
      OperationSequencer<Nindo> operationSequencer,
      Validator validator,
      EditorInstrumentor instrumentor,
      boolean useSemanticCopyPaste) {
    this.deferredCommands = deferredCommands;
    this.aggressiveSelectionHelper = aggressiveSelectionHelper;
    this.mutableDocument = mutableDocument;
    this.operationSequencer = operationSequencer;
    this.renderedContent = renderedContent;
    this.persistentContent = persistentContent;
    this.validator = validator;
    this.subtreeRenderer =
        new SubTreeXmlRenderer<ContentNode, ContentElement, ContentTextNode>(mutableDocument);
    this.annotationLogic =
        new PasteAnnotationLogic<ContentNode, ContentElement, ContentTextNode>(
            mutableDocument, annotationRegistry);
    this.instrumentor = instrumentor;
    this.useSemanticCopyPaste = useSemanticCopyPaste;
  }


  /**
   * Handler for the browser's paste event
   *
   * @param cursorBias current bias direction of the cursor
   * @return true to cancel browser's default, false otherwise. Generally, we'd
   *         only cancel if we cannot paste, i.e. selection not known
   */
  public boolean handlePasteEvent(final BiasDirection cursorBias) {
    // TODO(danilatos): Handle non-collapsed ranges
    final ContentRange previousSelection = aggressiveSelectionHelper.getOrderedSelectionPoints();

    // Selection shouldn't be null here, but its unsafe to make that assumption.
    // Cancel paste if we don't have selection.
    if (previousSelection == null) {
      return true;
    }

    busy = true;
    pasteBuffer.prepareForPaste();

    deferredCommands.addCommand(new Command() {
      public void execute() {
        extract(pasteBuffer.getPasteContainer(), previousSelection, cursorBias);
        busy = false;
      }
    });
    return false;
  }

  /**
   * @return true if we are in the middle of doing something
   */
  public boolean isBusy() {
    return busy;
  }

  @VisibleForTesting  // For testing with p/line container.
  void extract(Element srcContainer, ContentRange previousSelection, BiasDirection cursorBias) {
    final CMutableDocument destDoc = mutableDocument;
    final OperationSequencer<Nindo> destOperationSequencer =
        operationSequencer;
    final LocationMapper<ContentNode> mapper = mutableDocument;

    Point<ContentNode> start = normalize(previousSelection.getFirst());
    Point<ContentNode> end = normalize(previousSelection.getSecond());

    // Delete content if a range was selected
    if (!previousSelection.isCollapsed()) {
      PointRange<ContentNode> range = destDoc.deleteRange(start, end);

      start = range.getFirst();
      end = range.getSecond();
    }

    Point<ContentNode> insertAt = end;
    int pos = mapper.getLocation(insertAt);

    String waveXml = null;
    String annotations = null;
    if (useSemanticCopyPaste) {
      waveXml = clipboard.maybeGetWaveXml(srcContainer);
      annotations = clipboard.maybeGetAnnotations(srcContainer);
    }

    // TODO(user): Pass in whether the pasted content is rich or play
    // TODO(patcoleman): once we have non rich-text paste, fix cursor bias correctly
    cursorBias = BiasDirection.LEFT;

    if (useSemanticCopyPaste && waveXml != null) {
      if (!waveXml.isEmpty()) {
        instrumentor.record(Action.CLIPBOARD_PASTE_FROM_WAVE);

        // initialise the XML:
        Builder builder = at(pos);
        XmlStringBuilder createdFromXmlString =
            XmlStringBuilder.createFromXmlStringWithContraints(waveXml,
                PermittedCharacters.BLIP_TEXT);

        // Strip annotations based on behaviour:
        StringMap<String> modified = annotationLogic.stripKeys(
            destDoc, pos, cursorBias, ContentType.RICH_TEXT, builder);

        double startTime = Duration.currentTimeMillis();
        // apply xml change
        MutableDocumentImpl.appendXmlToBuilder(createdFromXmlString, builder);
        double timeTaken = Duration.currentTimeMillis() - startTime;
        LOG.trace().log("time taken: " + timeTaken);

        // handle the end of annotations
        annotationLogic.unstripKeys(builder, modified.keySet(), CollectionUtils.createStringSet());
        builder.finish();
        Nindo nindo = builder.build();

        try {
          validator.maybeThrowOperationExceptionFor(nindo);

          int locationAfter = destDoc.getLocation(insertAt) + createdFromXmlString.getLength();
          destOperationSequencer.begin();
          destOperationSequencer.consume(nindo);
          destOperationSequencer.end();
          aggressiveSelectionHelper.setCaret(locationAfter);

          LOG.trace().log("annotations: " + String.valueOf(annotations));
          if (annotations != null && !annotations.isEmpty()) {
            List<RangedAnnotation<String>> deserialize =
                AnnotationSerializer.deserialize(annotations);
            for (RangedAnnotation<String> ann : deserialize) {
              destDoc.setAnnotation(pos + ann.start(), pos + ann.end(), ann.key(), ann.value());
              LOG.trace().log(
                  "pos: " + pos + "start: " + (pos + ann.start()) + " end: " + (pos + ann.end())
                      + " key: " + ann.key() + " value: " + ann.value());
            }
          }
        } catch (OperationException e) {
          LOG.error().log("Semantic paste failed");
          // Restore caret
          aggressiveSelectionHelper.setCaret(insertAt);
        }
      }
    } else {
      instrumentor.record(Action.CLIPBOARD_PASTE_FROM_OUTSIDE);

      // initialize tokenizer and builder
      RichTextTokenizer tokenizer = createTokenizer(srcContainer);
      Builder builder = at(pos);

      // handle annotation starts
      StringMap<String> modified = annotationLogic.stripKeys(
          destDoc, pos, cursorBias, ContentType.RICH_TEXT, builder);

      // parse the tokens and apply ops
      RichTextMutationBuilder mutationBuilder = new RichTextMutationBuilder(modified);
      ReadableStringSet affectedKeys =
          mutationBuilder.applyMutations(tokenizer, builder, destDoc, insertAt.getContainer());

      // close annotations and finish
      annotationLogic.unstripKeys(builder, modified.keySet(), affectedKeys);
      builder.finish();
      Nindo nindo = builder.build();

      try {
        validator.maybeThrowOperationExceptionFor(nindo);

        destOperationSequencer.begin();
        destOperationSequencer.consume(nindo);
        destOperationSequencer.end();

        int cursorLocation = pos + mutationBuilder.getLastGoodCursorOffset();
        Point<ContentNode> caret = mapper.locate(cursorLocation);
        aggressiveSelectionHelper.setCaret(caret);
      } catch (OperationException e) {
        LOG.error().log("Paste failed");
        aggressiveSelectionHelper.setCaret(insertAt);
      }
    }

    srcContainer.setInnerHTML("");

    // Restore focus back to the editor
    // TODO(user): Write a webdriver to test selection is correct after paste.
    DomHelper.focus(destDoc.getDocumentElement().getContainerNodelet());
  }



  private Nindo.Builder at(int pos) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.begin();
    builder.skip(pos);
    return builder;
  }

  /**
   * Handler for the browser's copy or cut event. The current idea is to copy
   * the selected content to an offscreen div, move the browser selection to the
   * content within that copy and then allow the action to happen. Afterwards,
   * if it was a cut, fire off the proper delete event for the original
   * selection.
   *
   * TODO(user): Move this into its own class.
   * TODO(user): Add unit tests.
   *
   * @return true to cancel if the event should be cancelled.
   */
  public boolean handleCopyOrCutEvent(HtmlSelectionHelper selectionHelper, final boolean isCut) {
    // First gather the selection.
    final ContentRange contentSelection =
        aggressiveSelectionHelper.getOrderedSelectionPoints();

    if (contentSelection == null) {
      return true;
    }

    final FocusedRange selection = aggressiveSelectionHelper.getSelectionRange();
    performCopyOrCut(selectionHelper, pasteBuffer.getContainer(), contentSelection, isCut);

    busy = true;
    deferredCommands.addCommand(new Command() {
      public void execute() {
        if (isCut) {
          aggressiveSelectionHelper.setCaret(selection.asRange().getStart());
        } else {
          aggressiveSelectionHelper.setSelectionRange(selection);
        }
        busy = false;
      }
    });
    return false;
  }

  protected void performCopyOrCut(
      HtmlSelectionHelper selectionHelper, Element srcContainer, ContentRange contentSelection,
      boolean isCut) {
    if (contentSelection.isCollapsed()) {
      // Nothing to do.
      return;
    }

    Point<ContentNode> first = contentSelection.getFirst();
    Point<ContentNode> second = contentSelection.getSecond();
    SelectionMatcher selectionMatcher =
        new SelectionMatcher(first, second);

    ContentNode commonAncestor =
        DocHelper.nearestCommonAncestor(renderedContent, first
            .getContainer(), second.getContainer());

    Node fragment = PASTE_FORMAT_RENDERER.renderTree(renderedContent, commonAncestor,
        selectionMatcher);

    Preconditions.checkNotNull(selectionMatcher.getHtmlStart(), "html start is null, first: " + first);
    Preconditions.checkNotNull(selectionMatcher.getHtmlEnd(), "html end is null second: " + second);

    assert fragment.isOrHasChild(selectionMatcher.getHtmlStart().getContainer()) :
      "SelectionMatcher start not attached";
    assert fragment.isOrHasChild(selectionMatcher.getHtmlEnd().getContainer()) :
      "SelectionMatcher end not attached";


    PointRange<Node> newRange = new PointRange<Node>(
        selectionMatcher.getHtmlStart(),
        selectionMatcher.getHtmlEnd());

    Point<ContentNode> normalizedStart = normalize(contentSelection.getFirst());
    Point<ContentNode> normalizedEnd = normalize(contentSelection.getSecond());

    final XmlStringBuilder xmlInRange;
    final List<RangedAnnotation<String>> normalizedAnnotations;
    if (useSemanticCopyPaste) {
      String debugString =
          "Start: " + contentSelection.getFirst() + " End: " + contentSelection.getSecond()
              + " docDebug: " + mutableDocument.toDebugString();
      try {
        xmlInRange =
            subtreeRenderer.renderRange(normalizedStart, normalizedEnd);
        normalizedAnnotations =
            annotationLogic.extractNormalizedAnnotation(normalizedStart, normalizedEnd);
      } catch (Exception e) {
        LOG.error().logPlainText(debugString);
        throw new RuntimeException(e);
      }
    } else {
      xmlInRange = null;
      normalizedAnnotations = null;
    }

    if (isCut) {
      // Delete the originally selected content.
      mutableDocument.deleteRange(normalizedStart, normalizedEnd).getFirst();
    }

    // Set the browser's selection to the hidden div for the copy/cut.
    clipboard
        .fillBufferAndSetSelection(fragment, newRange, xmlInRange, normalizedAnnotations, false);
  }

  /**
   * Normalize point with respect to the mutable doc.
   * @param p
   */
  Point<ContentNode> normalize(Point<ContentNode> p) {
    return DocHelper.getFilteredPoint(persistentContent, p);
  }

  /**
   * Handle copy event.
   * @see #handleCopyOrCutEvent(HtmlSelectionHelper, boolean)
   *
   * @param selectionHelper
   */
  public boolean handleCopyEvent(HtmlSelectionHelper selectionHelper) {
    instrumentor.record(Action.CLIPBOARD_COPY);
    return handleCopyOrCutEvent(selectionHelper, false);
  }

  /**
   * Handle cut event.
   * @see #handleCopyOrCutEvent(HtmlSelectionHelper, boolean)
   *
   * @param selectionHelper
   */
  public boolean handleCutEvent(HtmlSelectionHelper selectionHelper) {
    instrumentor.record(Action.CLIPBOARD_CUT);
    return handleCopyOrCutEvent(selectionHelper, true);
  }

  /** Utility for creating tokenizer based on UA. */
  private RichTextTokenizer createTokenizer(Element container) {
    if (UserAgent.isFirefox()) {
      return new RichTextTokenizerImplFirefox<Node, Element, Text>(new HtmlViewImpl(container));
    } else {
      return new RichTextTokenizerImpl<Node, Element, Text>(new HtmlViewImpl(container));
    }
  }
}
