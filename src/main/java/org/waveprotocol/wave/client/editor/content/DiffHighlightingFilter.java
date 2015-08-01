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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.impl.DiffManager;
import org.waveprotocol.wave.client.editor.impl.DiffManager.DiffType;

import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableIntMap;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper for a content document, for the purpose of displaying diffs.
 *
 * Operations applied will be rendered as diffs.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author dyukon@gmail.com (Denis Konovalchik)
 */
public class DiffHighlightingFilter implements ModifiableDocument {

  /**
   * Wrapper for a bunch of deleted stuff, for diff highlighting
   */
  public static final class DeleteInfo {
    private final List<Element> htmlElements = new ArrayList<Element>();

    /**
     * The html of the deleted content
     */
    public List<Element> getDeletedHtmlElements() {
      return htmlElements;
    }
  }

  /**
   * Dependencies for implementing the diff filter
   */
  public interface DiffHighlightTarget extends MutableAnnotationSet<Object>, ModifiableDocument {

    /**
     * To be called during application of an operation, to interleave local annotations
     * in with the operation. Will only be called with local keys.
     */
    void startLocalAnnotation(String key, Object value);

    /**
     * To be called during application of an operation, to interleave local annotations
     * in with the operation. Will only be called with local keys.
     */
    void endLocalAnnotation(String key);

    /**
     * IndexedDocumentImpl's "currentNode"
     *
     * This method breaks encapsulation, think of a better way to do this later.
     */
    ContentNode getCurrentNode();

    /**
     * @return true only if the operation is currently being applied to the
     *         document itself - false otherwise (so we don't do the diff logic
     *         for, e.g. pretty printing or validation cursors)
     */
    boolean isApplyingToDocument();
  }

  /**
   * Prefix for diff local annotations
   */
  public static final String DIFF_KEY = Annotations.makeUniqueLocal("diff");

  /**
   * Diff annotation marking inserted content
   */
  public static final String DIFF_INSERT_KEY = DIFF_KEY + "/ins";

  /**
   * Diff annotation whose left boundary represents deleted content, the content
   * being stored in the annotation value as a DeleteInfo.
   */
  public static final String DIFF_DELETE_KEY = DIFF_KEY + "/del";

  private static final Object INSERT_MARKER = new Object();

  private final DiffHighlightTarget inner;

  // Munging to wrap the op

  private DocOpCursor target;

  private DocOp operation;

  // Diff state

  private int diffDepth = 0;

  private DeleteInfo currentDeleteInfo = null;

  private int currentDeleteLocation = 0;

  IntMap<Object> deleteInfos;

  int currentLocation = 0;

  public DiffHighlightingFilter(DiffHighlightTarget contentDocument) {
    this.inner = contentDocument;
  }

  @Override
  public void consume(DocOp op) throws OperationException {
    Preconditions.checkState(target == null, "Diff inner target not initialised");

    operation = op;
    inner.consume(opWrapper);

    final int size = inner.size();

    deleteInfos.each(new ReadableIntMap.ProcV<Object>() {
      public void apply(int location, Object _item) {
        assert location <= size;

        if (location == size) {
          // TODO(danilatos): Figure out a way to render this.
          // For now, do nothing, which is better than crashing.
          return;
        }

        if (_item instanceof DeleteInfo) {
          DeleteInfo item = (DeleteInfo) _item;
          DeleteInfo existing = (DeleteInfo) inner.getAnnotation(location, DIFF_DELETE_KEY);

          if (existing != null) {
            item.htmlElements.addAll(existing.htmlElements);
          }

          inner.setAnnotation(location, location + 1, DIFF_DELETE_KEY, item);
        }
      }
    });
  }

  private final DocOp opWrapper =
      new DiffOpWrapperBase("The document isn't expected to call this method") {
        @Override
        public void apply(DocOpCursor innerCursor) {
          if (!inner.isApplyingToDocument()) {
            operation.apply(innerCursor);
            return;
          }

          target = innerCursor;
          deleteInfos = CollectionUtils.createIntMap();
          currentDeleteInfo = null;
          currentDeleteLocation = -1;
          currentLocation = 0;

          operation.apply(filter);

          maybeSavePreviousDeleteInfo();

          target = null;
        }

        @Override
        public String toString() {
          return "DiffOpWrapper(" + operation + ")";
        }
      };

  private final DocOpCursor filter = new DocOpCursor() {

    @Override
    public void elementStart(String tagName, Attributes attributes) {
      if (diffDepth == 0) {
        inner.startLocalAnnotation(DIFF_INSERT_KEY, INSERT_MARKER);
      }

      diffDepth++;

      target.elementStart(tagName, attributes);
      currentLocation++;
    }

    @Override
    public void elementEnd() {
      target.elementEnd();
      currentLocation++;

      diffDepth--;

      if (diffDepth == 0) {
        inner.endLocalAnnotation(DIFF_INSERT_KEY);
      }
    }

    @Override
    public void characters(String characters) {
      if (diffDepth == 0) {
        inner.startLocalAnnotation(DIFF_INSERT_KEY, INSERT_MARKER);
      }

      target.characters(characters);
      currentLocation += characters.length();

      if (diffDepth == 0) {
        inner.endLocalAnnotation(DIFF_INSERT_KEY);
      }
    }

    private void updateDeleteInfo() {
      if (currentLocation != currentDeleteLocation || currentDeleteInfo == null) {
        maybeSavePreviousDeleteInfo();

        currentDeleteInfo = (DeleteInfo) inner.getAnnotation(currentLocation, DIFF_DELETE_KEY);
        if (currentDeleteInfo == null) {
          currentDeleteInfo = new DeleteInfo();
        }
      }
      currentDeleteLocation = currentLocation;
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      if (diffDepth == 0 && isOutsideInsertionAnnotation()) {
        ContentElement currentElement = (ContentElement) inner.getCurrentNode();
        Element e = currentElement.getImplNodelet();

        // HACK(danilatos): Line rendering is somewhat special, so special case it
        // for now. Once there are more use cases, we can figure out an appropriate
        // generalisation for this.
        if (LineRendering.isLineElement(currentElement)) {
          // This loses paragraph-level formatting, but is better than nothing.
          // Indentation and direction inherit from the pervious line, which is
          // quite acceptable.
          e = Document.get().createBRElement();
        }

        if (e != null) {
          e = e.cloneNode(true).cast();
          deletify(e);

          updateDeleteInfo();

          currentDeleteInfo.htmlElements.add(e);
        }
      }

      diffDepth++;

      target.deleteElementStart(type, attrs);
    }

    @Override
    public void deleteElementEnd() {
      target.deleteElementEnd();

      diffDepth--;
    }

    private boolean isOutsideInsertionAnnotation() {
      int location = currentLocation;
      return inner.firstAnnotationChange(location, location + 1, DIFF_INSERT_KEY, null) == -1;
    }

    private void deletify(Element element) {
      if (element == null) {
        // NOTE(danilatos): Not handling the case where the content element
        // is transparent w.r.t. the rendered view, but has visible children.
        return;
      }

      DiffManager.styleElement(element, DiffType.DELETE);
      DomHelper.makeUnselectable(element);

      for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
        if (!DomHelper.isTextNode(n)) {
          deletify(n.<Element> cast());
        }
      }
    }

    @Override
    public void deleteCharacters(String text) {
      if (diffDepth == 0 && isOutsideInsertionAnnotation()) {
        int endLocation = currentLocation + text.length();

        updateDeleteInfo();

        int scanLocation = currentLocation;
        int nextScanLocation;

        do {
          DeleteInfo surroundedInfo = (DeleteInfo) inner.getAnnotation(scanLocation,
              DIFF_DELETE_KEY);
          nextScanLocation = inner.firstAnnotationChange(scanLocation, endLocation,
              DIFF_DELETE_KEY, surroundedInfo);
          if (nextScanLocation == -1) {
            nextScanLocation = endLocation;
          }

          saveDeletedText(text, currentLocation, scanLocation, nextScanLocation);

          if (surroundedInfo != null) {
            currentDeleteInfo.htmlElements.addAll(surroundedInfo.htmlElements);
          }

          scanLocation = nextScanLocation;

        } while (nextScanLocation < endLocation);
      }

      target.deleteCharacters(text);
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      target.annotationBoundary(map);
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      currentLocation++;
      target.replaceAttributes(oldAttrs, newAttrs);
    }

    @Override
    public void retain(int itemCount) {
      currentLocation += itemCount;
      target.retain(itemCount);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      currentLocation++;
      target.updateAttributes(attrUpdate);
    }

    /**
     * Creates text spans reflecting every combination of text formatting annotation values.
     *
     * @param text text to be saved
     * @param textLocation location of the text beginning in the document
     * @param startLocation start location of the deleted block
     * @param finishLocation finish location of the deleted block
     */
    private void saveDeletedText(String text, int textLocation, int startLocation, int finishLocation) {
      // TODO(dyukon): This solution supports only text styles (weight, decoration, font etc.)
      // which can be applied to text SPANs.
      // It's necessary to add support for paragraph styles (headers ordered/numbered lists,
      // indents) which cannot be kept in text SPANs.
      Iterator<AnnotationInterval<Object>> aiIterator = inner.annotationIntervals(
          startLocation, finishLocation, AnnotationConstants.DELETED_STYLE_KEYS).iterator();
      if (aiIterator.hasNext()) { // Some annotations are changed throughout deleted text
        while (aiIterator.hasNext()) {
          AnnotationInterval<Object> ai = aiIterator.next();
          createDeleteElement(text.substring(ai.start() - textLocation, ai.end() - textLocation),
              ai.annotations());
        }
      } else { // No annotations are changed throughout deleted text
        createDeleteElement(text.substring(startLocation - textLocation, finishLocation - textLocation),
            findDeletedStyleAnnotations(startLocation));
      }
    }

    private ReadableStringMap<Object> findDeletedStyleAnnotations(final int location) {
      final StringMap<Object> annotations = CollectionUtils.createStringMap();
      AnnotationConstants.DELETED_STYLE_KEYS.each(new ReadableStringSet.Proc() {
        @Override
        public void apply(String key) {
          annotations.put(key, inner.getAnnotation(location, key));
        }
      });
      return annotations;
    }

    private void createDeleteElement(String innerText, ReadableStringMap<Object> annotations) {
      Element element = Document.get().createSpanElement();
      applyAnnotationsToElement(element, annotations);
      DiffManager.styleElement(element, DiffType.DELETE);
      element.setInnerText(innerText);
      currentDeleteInfo.htmlElements.add(element);
    }

    private void applyAnnotationsToElement(Element element, ReadableStringMap<Object> annotations) {
      final Style style = element.getStyle();
      annotations.each(new ReadableStringMap.ProcV<Object>() {
        @Override
        public void apply(String key, Object value) {
          if (value != null && value instanceof String) {
            String styleValue = (String) value;
            if (!styleValue.isEmpty()) {
              style.setProperty(StyleAnnotationHandler.suffix(key), styleValue);
            }
          }
        }
      });
    }
  };

  /**
   * Save previous delete info - assumes currentDeleteLocation and
   * currentDeleteInfo still reflect the previous info.
   */
  private void maybeSavePreviousDeleteInfo() {
    if (currentDeleteInfo != null) {
      deleteInfos.put(currentDeleteLocation, currentDeleteInfo);
    }
  }

  /**
   * Remove all diff markup
   */
  public void clearDiffs() {
    clearDiffs(inner);
  }

  public static void clearDiffs(MutableAnnotationSet.Local doc) {
    clearDiffs((DiffHighlightTarget) doc);
  }

  public static void clearDiffs(DiffHighlightingFilter.DiffHighlightTarget target) {
    // Guards to prevent setting the annotation when there is nothing
    // to do, thus saving a repaint
    Annotations.guardedResetAnnotation(target, 0, target.size(), DIFF_INSERT_KEY, null);
    Annotations.guardedResetAnnotation(target, 0, target.size(), DIFF_DELETE_KEY, null);
  }
}
