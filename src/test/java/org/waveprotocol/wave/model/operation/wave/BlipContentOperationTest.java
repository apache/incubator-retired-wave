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

import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.testing.FakeDocument;
import org.waveprotocol.wave.model.wave.data.BlipData;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;

/**
 * Tests for BlipContentOperation.
 *
 * @author anorth@google.com (Alex North)
 */

public class BlipContentOperationTest extends OperationTestBase {

  private final static DocOp docOp = new DocOpBuilder().characters("Hello").build();

  public void testApply() throws OperationException {
    BlipContentOperation op = new BlipContentOperation(context, docOp);
    BlipData blip = waveletData.createDocument("root", jane, noParticipants,
        EmptyDocument.EMPTY_DOCUMENT, 0L, 0L);

    op.apply(blip);

    // the op eventually reached the document
    assertEquals(docOp, ((FakeDocument) blip.getContent()).getConsumed());
    // editing the document makes the op creator a blip contributor
    assertEquals(Collections.singleton(fred), blip.getContributors());
  }

  public void testReverseRestoresContent() throws OperationException {
    BlipContentOperation op = new BlipContentOperation(context, docOp);
    BlipData blip = waveletData.createDocument("root", fred, Collections.<ParticipantId>emptyList(),
        EmptyDocument.EMPTY_DOCUMENT, 0L, 0L);

    List<? extends BlipOperation> reverseOps = op.applyAndReturnReverse(blip);

    for (BlipOperation reverse : reverseOps) {
      reverse.apply(blip);
    }

    ExtraAsserts.checkContent("", blip);
    assertEquals(noParticipants, blip.getContributors());
  }

}
