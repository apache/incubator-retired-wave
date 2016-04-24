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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMap;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationsUpdate;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.HashMap;

/**
 * Utilities for document automatons
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class Automatons {

  /**
   * @param doc
   * @return an automaton view of the given document
   */
  public static <N, E extends N, T extends N> AutomatonDocument fromReadable(
      final ReadableWDocument<N, E, T> doc) {

    return new AutomatonDocument() {

      @Override
      public AnnotationMap annotationsAt(final int pos) {
        Preconditions.checkElementIndex(pos, doc.size());
        class AnnoMap extends HashMap<String, String> implements AnnotationMap {
          @Override
          public AnnotationMap updateWith(AnnotationsUpdate mutation) {
            return new AnnotationMapImpl(this).updateWith(mutation);
          }

          @Override
          public AnnotationMap updateWithNoCompatibilityCheck(AnnotationsUpdate mutation) {
            return new AnnotationMapImpl(this).updateWithNoCompatibilityCheck(mutation);
          }
        }
        final AnnoMap annotations = new AnnoMap();
        doc.knownKeys().each(new Proc() {
          @Override
          public void apply(String key) {
            String value = doc.getAnnotation(pos, key);
            if (value != null) {
              annotations.put(key, value);
            }
          }
        });
        return annotations;
      }

      @Override
      public Attributes attributesAt(int pos) {
        E el = Point.elementAfter(doc, doc.locate(pos));
        return isUsableElement(el) ? new AttributesImpl(doc.getAttributes(el)) : null;
      }

      @Override
      public int charAt(int pos) {
        String str = DocHelper.getText(doc, pos, pos + 1);
        return str.length() > 0 ? str.charAt(0) : -1;
      }

      @Override
      public String elementEndingAt(int pos) {
        E el = Point.elementEndingAt(doc, doc.locate(pos));
        return isUsableElement(el) ? doc.getTagName(el) : null;
      }

      @Override
      public String elementStartingAt(int pos) {
        E el = Point.elementAfter(doc, doc.locate(pos));
        return isUsableElement(el) ? doc.getTagName(el) : null;
      }

      @Override
      public int length() {
        return doc.size();
      }

      @Override
      public String nthEnclosingElementTag(int insertionPoint, int depth) {
        E el = Point.enclosingElement(doc, doc.locate(insertionPoint));
        while (depth > 0 && isUsableElement(el)) {
          el = doc.getParentElement(el);
        }

        return isUsableElement(el) ? doc.getTagName(el) : null;
      }

      @SuppressWarnings("unchecked")
      @Override
      public int remainingCharactersInElement(int insertionPoint) {
        Point<N> point = doc.locate(insertionPoint);
        int num = 0;
        N node;
        if (point.isInTextNode()) {
          num += doc.getLength((T) point.getContainer()) - point.getTextOffset();
          node = doc.getNextSibling(point.getContainer());
        } else {
          node = point.getNodeAfter();
        }
        T textNode;
        while ((textNode = doc.asText(node)) != null) {
          num += doc.getLength(textNode);
          node = doc.getNextSibling(node);
        }
        return num;
      }

      private boolean isUsableElement(E element) {
        return element != null && element != doc.getDocumentElement();
      }

      @Override
      public int firstAnnotationChange(int start, int end, String key, String fromValue) {
        return doc.firstAnnotationChange(start, end, key, fromValue);
      }

      @Override
      public String getAnnotation(int pos, String key) {
        return doc.getAnnotation(pos, key);
      }

      @Override
      public String toString() {
        return "Automatons.fromReadable(): " + doc;
      }
    };
  }
}
