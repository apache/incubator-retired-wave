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

package org.waveprotocol.wave.model.document.indexed;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMapBuilder;
import org.waveprotocol.wave.model.document.operation.Automatons;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * Performance tests for indexed document.
 *
 * @author ohler@google.com (Christian Ohler)
 */


public class IndexedDocumentImplPerformanceTest extends TestCase {

  public void testCheckRetainPerformace() throws OperationException {
    final int length = 10000000;

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      b.append("z");
    }
    AnnotationTree<Object> annotations = new AnnotationTree<Object>(
        "a", "b", null);
    IndexedDocumentImpl<Node, Element, Text, ?> doc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(
            RawDocumentImpl.PROVIDER.parse("<doc><p></p></doc>"), annotations,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    doc.consume(new DocOpBuilder().annotationBoundary(
        new AnnotationBoundaryMapBuilder().change("a", null, "0").build())
        .characters(b.toString()).retain(2)
        .annotationBoundary(new AnnotationBoundaryMapBuilder().end("a").build()).build());

    long startTime = System.currentTimeMillis();
    final int reps = 10000;
    for (int i = 0; i < reps; i++) {
      assertTrue(
          // The test is that this doesn't time out.
          DocOpValidator.validate(null, DocumentSchema.NO_SCHEMA_CONSTRAINTS,
              Automatons.fromReadable(doc),
              new DocOpBuilder().annotationBoundary(
                  new AnnotationBoundaryMapBuilder().change("a", "0", "1").build())
                  .retain(length + 2)
                  .annotationBoundary(new AnnotationBoundaryMapBuilder().end("a").build()).build())
                  .isValid());
    }
    long endTime = System.currentTimeMillis();
    long elapsed = endTime - startTime;
    System.err.println("millis per rep: " + (((float) elapsed) / reps));
  }
}
