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

package org.waveprotocol.wave.client.editor.annotation;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.PainterRegistryImpl;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for random tests of the annotation painter.
 *
 * Subclass to define the actual dom context to use (in particular, what substrate).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class AnnotationPainterTestBase extends TestCase {

  private final ReadableStringSet rangeKeys = CollectionUtils.newStringSet("a", "b", "c", "d");
  private final ReadableStringSet boundaryKeys = CollectionUtils.newStringSet("w", "x", "y", "z");

  PersistentContent<Node, Element, Text> persistentDoc;
  FakeTimerService timerService = new FakeTimerService();

  public TestDocumentContext<Node, Element, Text> createAnnotationContext() {

    final AnnotationPainter painter = new AnnotationPainter(timerService);
    final PainterRegistry painterRegistry =
        new PainterRegistryImpl("l:p", "l:b", painter);
    painterRegistry.registerPaintFunction(rangeKeys, new PaintFunction() {
      @Override
      public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
        Map<String, String> ret = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : from.entrySet()) {
          ret.put(entry.getKey(), (String) entry.getValue());
        }
        return ret;
      }
    });
    boundaryKeys.each(new Proc() {
      @Override
      public void apply(String key) {
        final String k = key;
        painterRegistry.registerBoundaryFunction(boundaryKeys, new BoundaryFunction() {
          @Override
          public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
              N nodeAfter, Map<String, Object> before, Map<String, Object> after,
              boolean isEditing) {
            Point.checkRelationship(localDoc, parent, nodeAfter, "Test boundary function");

            if (before.containsKey(k) || after.containsKey(k)) {
              Map<String, String> attrs = new HashMap<String, String>();
              attrs.put("z", before.toString() + "->" + after.toString());
              return localDoc.transparentCreate("b-" + k, attrs, parent, nodeAfter);
            } else {
              return null;
            }
          }
        });
      }
    });

    // To get around initialisation circularity.
    // It's safe because we won't get any annotation changes in the initialisation.
    class Box {
      TestDocumentContext<Node, Element, Text> cxt;
    }
    final Box cxtBox = new Box();
    final TestDocumentContext<Node, Element, Text> cxt = ContextProviders.createTestPojoContext(
        "",
        null, new AnnotationSetListener<Object>() {
          public void onAnnotationChange(int start, int end, String key, Object newValue) {
            if (key.length() == 1) {
              if (key.charAt(0) < 'w') {
                painter.scheduleRepaint(cxtBox.cxt, start, end);
              } else {
                painter.scheduleRepaint(cxtBox.cxt, start, start);
                painter.scheduleRepaint(cxtBox.cxt, end, end);
              }
            }
          }
        }, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    cxtBox.cxt = cxt;
    AnnotationPainter.createAndSetDocPainter(cxt, painterRegistry);
    //IndexedDocument<Node, Element, Text> indexedDoc = cxt.getIndexedDoc();

    return cxt;
  }
}
