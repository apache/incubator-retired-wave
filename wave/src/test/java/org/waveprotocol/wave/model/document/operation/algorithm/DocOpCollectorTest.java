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

package org.waveprotocol.wave.model.document.operation.algorithm;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;

/**
 * Some basic tests for the doc-op collector.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class DocOpCollectorTest extends TestCase {

  public void testSimpleMonotonicComposition() {
    DocOpCollector collector = new DocOpCollector();

    DocOp a = new DocOpBuilder().characters("a").build();
    DocOp b = new DocOpBuilder().retain(1).characters("b").build();
    DocOp c = new DocOpBuilder().retain(2).characters("c").build();
    DocOp d = new DocOpBuilder().retain(3).characters("d").build();

    collector.add(a);
    collector.add(b);
    collector.add(c);
    collector.add(d);

    DocOp expected = new DocOpBuilder().characters("abcd").build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, collector.composeAll()));
  }

  // There is no DocOp object that can represent a universal no-op, so null
  // needs to be used instead. To work correctly, the collector needs to compose
  // an empty collection to a universal no-op.
  public void testEmptyCollectionComposesToNull() {
    assertNull(new DocOpCollector().composeAll());
  }
}