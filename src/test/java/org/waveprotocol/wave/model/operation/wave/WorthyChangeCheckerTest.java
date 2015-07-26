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

package org.waveprotocol.wave.model.operation.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMapBuilder;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * @author ohler@google.com (Christian Ohler)
 */

public class WorthyChangeCheckerTest extends TestCase {

  public void testAnchorRemovalIsUnworthy1() throws OperationException {
    IndexedDocument<Node, Element, Text> d = DocProviders.POJO.build(
        new DocInitializationBuilder()
            .annotationBoundary(new AnnotationBoundaryMapBuilder().change("a", null, "b").build())
            .characters("a")
            .annotationBoundary(new AnnotationBoundaryMapBuilder().end("a").build())
            .characters("a")
            .elementStart(WorthyChangeChecker.THREAD_INLINE_ANCHOR_TAGNAME, Attributes.EMPTY_MAP)
            .elementEnd()
            .build(),
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    DocOp op = d.consumeAndReturnInvertible(new Nindo.Builder() {
      {
        skip(2);
        deleteElementStart();
        deleteElementEnd();
      }
    }.build());
    assertFalse(WorthyChangeChecker.isWorthy(op));
  }

  // This one is still failing; fixing it is not as easy.
  public void XtestAnchorRemovalIsUnworthy2() throws OperationException {
    IndexedDocument<Node, Element, Text> d = DocProviders.POJO.build(
        new DocInitializationBuilder()
            .annotationBoundary(new AnnotationBoundaryMapBuilder().change("a", null, "b").build())
            .elementStart(WorthyChangeChecker.THREAD_INLINE_ANCHOR_TAGNAME, Attributes.EMPTY_MAP)
            .elementEnd()
            .annotationBoundary(new AnnotationBoundaryMapBuilder().end("a").build())
            .build(),
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    DocOp op = d.consumeAndReturnInvertible(new Nindo.Builder() {
      {
        deleteElementStart();
        deleteElementEnd();
      }
    }.build());
    assertFalse(WorthyChangeChecker.isWorthy(op));
  }

}
