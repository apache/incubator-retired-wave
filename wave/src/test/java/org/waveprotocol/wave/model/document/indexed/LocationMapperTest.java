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

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * Tests for the point-locating capabilities of IndexedDocumentImpl.
 *
 */

public class LocationMapperTest extends TestCase {

  /**
   * Test that sample locations are being mapped correctly.
   */
  public void testSampleLocations1() {
    IndexedDocument<Node, Element, Text> document = createEmptyDocument();
    try {
      document.consume(element("p"));
      document.consume(characters("a", 1, 1));
      Node testNode = document.getDocumentElement().getFirstChild();
      assertNotNull(testNode);
      testNode = testNode.getFirstChild();
      assertNotNull(testNode);
      assertEquals(1, document.getLocation(testNode));
      assertEquals(1, testNode.getIndexingContainer().size());
      document.consume(characters("b", 2, 1));
      assertEquals(1, document.getLocation(testNode));
      assertEquals(2, testNode.getIndexingContainer().size());
      document.consume(characters("c", 3, 1));
      assertEquals(1, document.getLocation(testNode));
      assertEquals(3, testNode.getIndexingContainer().size());
    } catch (OperationException e) {
      fail(e.toString());
    }
  }

  private static DocOp element(String tag) {
    return new DocOpBuilder().elementStart(tag, Attributes.EMPTY_MAP).elementEnd().build();
  }

  private static DocOp characters(String text, int location, int trail) {
    return new DocOpBuilder().retain(location).characters(text).retain(trail).build();
  }

  private static IndexedDocument<Node, Element, Text> createEmptyDocument() {
    return DocProviders.POJO.parse("");
  }

}
