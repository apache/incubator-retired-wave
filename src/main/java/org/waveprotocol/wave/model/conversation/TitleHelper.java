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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocIterate;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Utilities for dealing with titles
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class TitleHelper {
  /**
   * Annotation key used to define the title
   *
   * If it non existent (i.e. null value for the entire document) the title is
   * an empty string NOTE(danilatos): Until we get rid of paragraphs, we will
   * also use the old title extractor
   *
   * If it has an empty string value, then the title is the text encompassed by
   * the range of the annotation.
   *
   * If it has a non-empty value, the title is the value.
   *
   * If there is more than one non-null value for the annotation in the
   * document, only the first one counts, the rest are treated as if they were
   * null.
   */
  public static final String TITLE_KEY = Annotations.join(
      Blips.ANNOTATION_PREFIX , "title");

  public static final String AUTO_VALUE = "";

  private static final DocInitialization EMPTY_LINE_DOCUMENT_WITH_TITLE;
  static {
    DocInitializationBuilder b = new DocInitializationBuilder();
    Blips.buildBlipHead(b);
    b.elementStart(Blips.BODY_TAGNAME, Attributes.EMPTY_MAP);
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder().initializationValues(
        TITLE_KEY, AUTO_VALUE).build());
    b.elementStart(LineContainers.LINE_TAGNAME, Attributes.EMPTY_MAP);
    b.elementEnd();
    b.elementEnd();
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder().initializationEnd(TITLE_KEY).build());
    EMPTY_LINE_DOCUMENT_WITH_TITLE = b.build();
  }

  /**
   * @return An empty document with a title
   */
  public static DocInitialization emptyDocumentWithTitle() {
    return EMPTY_LINE_DOCUMENT_WITH_TITLE;
  }

  /**
   * @return The title from the given document
   */
  @SuppressWarnings("deprecation")
  public static <N, E extends N, T extends N> String extractTitle(ReadableWDocument<N, E, T> doc) {
    int start = doc.firstAnnotationChange(0, doc.size(), TITLE_KEY, null);

    if (start == -1) {
      return "";
    }

    String explicitValue = doc.getAnnotation(start, TITLE_KEY);
    if (!explicitValue.isEmpty()) {
      return explicitValue;
    }

    return DocHelper.getText(doc, start,
        Annotations.firstAnnotationBoundary(doc, start, doc.size(), TITLE_KEY, AUTO_VALUE));
  }

  /**
   * Set an explicit title on the document, independent of the document's text
   * content
   *
   * @param doc
   * @param title
   */
  public static void setExplicitTitle(MutableDocument<?, ?, ?> doc, String title) {
    // TODO(danilatos): Automatically insert content when it's an empty document?
    // Perhaps a special title tag?
    Preconditions.checkArgument(doc.size() > 0, "Cannot set title on empty document");
    doc.resetAnnotation(0, 1, TITLE_KEY, title);
  }

  /**
   * Set the title for the document to be defined as the text encompassed by the
   * given range.
   *
   * @param doc
   * @param start
   * @param end
   */
  public static void setImplicitTitle(MutableDocument<?, ?, ?> doc, int start, int end) {
    Preconditions.checkArgument(start < end, "Implicit title range is invalid");
    Annotations.guardedResetAnnotation(doc, start, end, TITLE_KEY, AUTO_VALUE);
  }

  /**
   * Finds the appropriate range for an implicit title in the document. If
   * no valid range was found, will return null.
   *
   * @param doc
   * @return a Range or null if no valid range was found.
   */
  public static <N, E extends N, T extends N> Range findImplicitTitle(
      final MutableDocument<N, E, T> doc) {
    boolean afterPunctuation = false;

    int start = -1, end = doc.size() -1;
    E firstLineReached = null;

    outer: for (N node : DocIterate.deep(doc, Blips.getBody(doc), null)) {
      E el = doc.asElement(node);
      if (el == null) {
        if (firstLineReached != null) {
          String text = doc.getData(doc.asText(node));
          // NOTE(danilatos): This doesn't take into account surrogate pairs,
          // etc. However, this is fine for now given the super naive
          // implementation of isPunctuation and isWhitespace, which are
          // barely acceptable even for English use.
          for (int textIndex = 0; textIndex < text.length(); textIndex++) {
            char c = text.charAt(textIndex);
            if (isPunctuation(c)) {
              afterPunctuation = true;
            } else if (afterPunctuation && isWhitespace(c)) {
              // TODO(danilatos): Find out if there are any differences between the JVM and
              // browsers' regex implementations w.r.t. what '\s' matches
              // Given that \n and \t are disallowed anyway, consider using just char 32?
              end = doc.getLocation(node) + textIndex;
              break outer;
            } else {
              afterPunctuation = false;
            }
          }
        }
      } else {
        if (doc.getTagName(el).equals(LineContainers.LINE_TAGNAME)) {
          if (firstLineReached != null) {
            end = doc.getLocation(node);
            break;
          } else {
            start = doc.getLocation(node);
            firstLineReached = el;
          }
        }
      }
    }

    if (start > 0 && end > start) {
      return new Range(start, end);
    }
    return null;
  }

  /**
   * @return true if the character is punctuation
   */
  private static boolean isPunctuation(char c) {
    // TODO(danilatos): This is woefully incomplete, especially w.r.t. other languages
    return c == '.' || c == '!' || c =='?';
  }

  /**
   * A very simplified version of Character.isWhitespace()
   *
   * Firstly, GWT does not implement that method, and secondly, we don't expect or
   * want some of those characters.
   */
  private static boolean isWhitespace(char c) {
    return c == ' ';
  }

  /**
   * Automatically finds and sets an appropriate implicit title for the
   * document. Will include the first line token as part of the covered range,
   * so it's easy to detect if a line is part of a title, by checking for the
   * annotation.
   *
   * If the title has been explicitly set, does nothing.
   *
   * @param doc
   */
  public static <N, E extends N, T extends N> void maybeFindAndSetImplicitTitle(
      final MutableDocument<N, E, T> doc) {
    if (hasExplicitTitle(doc)) {
      // Explicit title is set - do not touch
      return;
    }

    Range range = findImplicitTitle(doc);
    if (range != null) {
      setImplicitTitle(doc, range.getStart(), range.getEnd());
    }
  }

  /**
   * @param doc
   * @return true if the document's title is explicitly set
   */
  public static boolean hasExplicitTitle(MutableDocument<?, ?, ?> doc) {
    int start = doc.firstAnnotationChange(0, doc.size(), TITLE_KEY, null);
    if (start != -1 && !doc.getAnnotation(start, TITLE_KEY).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * An error-absorbing title retrieving convenience function.
   * Please use this rather than doing:
   * wave.getConversations().getRoot().getRootThread().getFirstBlip().getContent()
   * as that is the cause of _many_ shiny's in the client.
   * In the event something went wrong, or no title exists will return the empty string.
   * This is deliberate, so that it doesn't propagate an error for a rather non-critical
   * code path.
   */
  public static String getTitle(WaveContext waveCtx) {
    ObservableConversationView conversations = waveCtx.getConversations();
    if(conversations == null) {
      return "";
    }

    ObservableConversation rootConversation = conversations.getRoot();
    if(rootConversation == null) {
      return "";
    }

    ObservableConversationThread rootThread = rootConversation.getRootThread();
    if(rootThread == null) {
      return "";
    }

    ObservableConversationBlip firstBlip = rootThread.getFirstBlip();
    if(firstBlip == null) {
      return "";
    }

    Document doc = firstBlip.getContent();
    if(doc == null) {
      return "";
    }

    String title = extractTitle(doc);
    if(title == null) {
      return "";
    }

    return title;
  }

  private TitleHelper() {
  }
}

