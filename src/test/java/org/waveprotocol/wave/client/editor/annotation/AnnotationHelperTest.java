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

import org.waveprotocol.wave.client.editor.content.misc.AnnotationHelper;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Test for checkGetRangePrecedingLocation
 *
 */

public class AnnotationHelperTest extends TestCase {

  public TestDocumentContext<Node, Element, Text> initializeTestContext(String initialContent) {
    return ContextProviders.createTestPojoContext(initialContent,
        null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);
  }

  public void testGetRangePrecedingLocation() {
    // TODO(user): test cases with intervening nodes, boundary nodes and
    // transparent nodes etc..
    checkGetRangePrecedingLocation("<x>hello</x>", new Range(1, 2), new Range(1, 2), 3);

    checkGetRangePrecedingLocation("<x>hello</x>", new Range(1, 3), new Range(1, 3), 3);
    checkGetRangePrecedingLocation("<x>hello</x>", new Range(1, 3), new Range(1, 3), 4);
    checkGetRangePrecedingLocation("<x>hello</x>", new Range(1, 3), null, 1);
  }

  public void checkGetRangePrecedingLocation(String initialContent, Range annotatedRange,
      Range expected, int end) {
    final TestDocumentContext<Node, Element, Text> cxt = initializeTestContext(initialContent);
    MutableDocument<Node, Element, Text> doc = cxt.document();
    doc.setAnnotation(annotatedRange.getStart(), annotatedRange.getEnd(), "test", "value");
    assertEquals(expected, AnnotationHelper.getRangePrecedingLocation(doc, end, "test"));
  }
}
