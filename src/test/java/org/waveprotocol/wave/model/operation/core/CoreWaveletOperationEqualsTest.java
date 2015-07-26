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

package org.waveprotocol.wave.model.operation.core;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OpComparators.OpEquator;
import org.waveprotocol.wave.model.wave.ParticipantId;


public class CoreWaveletOperationEqualsTest extends TestCase {

  public void testTypes() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    CoreWaveletOperation a = CoreNoOp.INSTANCE;
    CoreWaveletOperation b = new CoreAddParticipant(new ParticipantId(""));
    CoreWaveletOperation c = new CoreRemoveParticipant(new ParticipantId(""));
    CoreWaveletOperation d = new CoreWaveletDocumentOperation("", new DocOpBuffer().finish());

    assertTrue (a.equals(a));
    assertFalse(a.equals(b));
    assertFalse(a.equals(c));
    assertFalse(a.equals(d));

    assertFalse(b.equals(a));
    assertTrue (b.equals(b));
    assertFalse(b.equals(c));
    assertFalse(b.equals(d));

    assertFalse(c.equals(a));
    assertFalse(c.equals(b));
    assertTrue (c.equals(c));
    assertFalse(c.equals(d));

    assertFalse(d.equals(a));
    assertFalse(d.equals(b));
    assertFalse(d.equals(c));
    assertTrue (d.equals(d));
  }

  public void testAddParticipant() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    CoreAddParticipant a1 = new CoreAddParticipant(new ParticipantId("a"));
    CoreAddParticipant a2 = new CoreAddParticipant(new ParticipantId("a"));
    CoreAddParticipant b1 = new CoreAddParticipant(new ParticipantId("b"));
    CoreAddParticipant b2 = new CoreAddParticipant(new ParticipantId("b"));

    assertTrue(a1.equals(a1));
    assertTrue(a1.equals(a2));
    assertTrue(a2.equals(a1));
    assertTrue(a2.equals(a2));

    assertTrue(b1.equals(b1));
    assertTrue(b1.equals(b2));
    assertTrue(b2.equals(b1));
    assertTrue(b2.equals(b2));

    assertFalse(a1.equals(b1));
    assertFalse(a1.equals(b2));
    assertFalse(a2.equals(b1));
    assertFalse(a2.equals(b2));
  }

  public void testRemoveParticipant() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    CoreRemoveParticipant a1 = new CoreRemoveParticipant(new ParticipantId("a"));
    CoreRemoveParticipant a2 = new CoreRemoveParticipant(new ParticipantId("a"));
    CoreRemoveParticipant b1 = new CoreRemoveParticipant(new ParticipantId("b"));
    CoreRemoveParticipant b2 = new CoreRemoveParticipant(new ParticipantId("b"));

    assertTrue(a1.equals(a1));
    assertTrue(a1.equals(a2));
    assertTrue(a2.equals(a1));
    assertTrue(a2.equals(a2));

    assertTrue(b1.equals(b1));
    assertTrue(b1.equals(b2));
    assertTrue(b2.equals(b1));
    assertTrue(b2.equals(b2));

    assertFalse(a1.equals(b1));
    assertFalse(a1.equals(b2));
    assertFalse(a2.equals(b1));
    assertFalse(a2.equals(b2));
  }

  public void testWaveletDocumentOperationDocumentId() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer b = new DocOpBuffer();
    b.characters("a");
    DocOp d = b.finish();

    CoreWaveletDocumentOperation a1 = new CoreWaveletDocumentOperation("a", d);
    CoreWaveletDocumentOperation a2 = new CoreWaveletDocumentOperation("a", d);
    CoreWaveletDocumentOperation b1 = new CoreWaveletDocumentOperation("b", d);
    CoreWaveletDocumentOperation b2 = new CoreWaveletDocumentOperation("b", d);

    assertTrue(a1.equals(a1));
    assertTrue(a1.equals(a2));
    assertTrue(a2.equals(a1));
    assertTrue(a2.equals(a2));

    assertTrue(b1.equals(b1));
    assertTrue(b1.equals(b2));
    assertTrue(b2.equals(b1));
    assertTrue(b2.equals(b2));

    assertFalse(a1.equals(b1));
    assertFalse(a1.equals(b2));
    assertFalse(a2.equals(b1));
    assertFalse(a2.equals(b2));
  }

  public void testWaveletDocumentOperationDocOp() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer ba = new DocOpBuffer();
    ba.characters("a");
    DocOp da = ba.finish();
    DocOpBuffer bb = new DocOpBuffer();
    bb.deleteCharacters("a");
    DocOp db = bb.finish();

    CoreWaveletDocumentOperation a1 = new CoreWaveletDocumentOperation("a", da);
    CoreWaveletDocumentOperation a2 = new CoreWaveletDocumentOperation("a", da);
    CoreWaveletDocumentOperation b1 = new CoreWaveletDocumentOperation("a", db);
    CoreWaveletDocumentOperation b2 = new CoreWaveletDocumentOperation("a", db);

    assertTrue(a1.equals(a1));
    assertTrue(a1.equals(a2));
    assertTrue(a2.equals(a1));
    assertTrue(a2.equals(a2));

    assertTrue(b1.equals(b1));
    assertTrue(b1.equals(b2));
    assertTrue(b2.equals(b1));
    assertTrue(b2.equals(b2));

    assertFalse(a1.equals(b1));
    assertFalse(a1.equals(b2));
    assertFalse(a2.equals(b1));
    assertFalse(a2.equals(b2));
  }

}
