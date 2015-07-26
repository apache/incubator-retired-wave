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

package org.waveprotocol.wave.client.wave;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;

/**
 * Tests for {@link SimpleDiffDoc}.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class SimpleDiffDocTest extends TestCase {

  private static final DocInitialization EMPTY = new DocInitializationBuilder().build();

  // Note that this test does not matter that much; it is not immediately clear
  // how isCompleteDiff and isCompleteState should degenerate for the empty
  // state. In practice, the empty state occurs during the implicit creation of
  // new documents, so there is an expectation that ops/diffs are about to occur
  // on it (making isCompleteDiff true), so it makes sense for the diff-ness of
  // the empty state to be continuous with that.
  public void testEmptyStateIsAllDiff() {
    SimpleDiffDoc doc = SimpleDiffDoc.create(EMPTY, null);

    assertTrue(doc.isCompleteDiff());
    assertFalse(doc.isCompleteState());
  }
}
