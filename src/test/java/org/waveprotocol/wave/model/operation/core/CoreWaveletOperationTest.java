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
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.wave.data.core.impl.CoreWaveletDataImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for the {@code CoreWaveletOperation} containers.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */

public class CoreWaveletOperationTest extends TestCase {

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = WaveId.of(DOMAIN, "hello");
  private static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "world");
  private static final ParticipantId PARTICIPANT_ID = new ParticipantId("test@" + DOMAIN);
  private static final String DOC_ID = "doc";
  private static final String TEXT = "hi";

  private final CoreWaveletOperation addOp;
  private final CoreWaveletOperation removeOp;
  private final CoreWaveletOperation noOp;
  private final CoreWaveletOperation docOpCharacters;
  private final CoreWaveletOperation docOpDeleteCharacters;
  private final CoreWaveletOperation docOpRetainAndCharacters;
  private final CoreWaveletOperation docOpRetainAndDeleteCharacters;
  private final CoreWaveletDocumentOperation docOpComplex;

  private CoreWaveletData wavelet;
  private CoreWaveletData originalWavelet;

  public CoreWaveletOperationTest() {
    DocOpBuffer docOpBuilder;

    addOp = new CoreAddParticipant(PARTICIPANT_ID);
    removeOp = new CoreRemoveParticipant(PARTICIPANT_ID);
    noOp = CoreNoOp.INSTANCE;

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.characters(TEXT);
    docOpCharacters = new CoreWaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.deleteCharacters(TEXT);
    docOpDeleteCharacters = new CoreWaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.retain(TEXT.length());
    docOpBuilder.characters(TEXT);
    docOpRetainAndCharacters = new CoreWaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.retain(TEXT.length());
    docOpBuilder.deleteCharacters(TEXT);
    docOpRetainAndDeleteCharacters = new CoreWaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.elementStart("name1",
        new AttributesImpl(CollectionUtils.immutableMap("key1", "val1", "key2", "val2")));
    docOpBuilder.characters(TEXT);
    docOpBuilder.elementStart("name2",
        new AttributesImpl(CollectionUtils.immutableMap("key3", "val3", "key4", "val4")));
    docOpBuilder.characters(TEXT + TEXT);
    docOpBuilder.elementEnd();
    docOpBuilder.characters(TEXT + TEXT + TEXT);
    docOpBuilder.elementEnd();
    docOpComplex = new CoreWaveletDocumentOperation(DOC_ID, docOpBuilder.finish());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    wavelet = new CoreWaveletDataImpl(WAVE_ID, WAVELET_ID);
    originalWavelet = new CoreWaveletDataImpl(WAVE_ID, WAVELET_ID);
  }

  // Tests

  public void testTypeSanity() {
    assertTrue(noOp.getInverse() instanceof CoreNoOp);
    assertTrue(addOp.getInverse() instanceof CoreRemoveParticipant);
    assertTrue(removeOp.getInverse() instanceof CoreAddParticipant);
    assertTrue(docOpCharacters.getInverse() instanceof CoreWaveletDocumentOperation);
    assertTrue(docOpDeleteCharacters.getInverse() instanceof CoreWaveletDocumentOperation);
    assertTrue(docOpRetainAndCharacters.getInverse() instanceof CoreWaveletDocumentOperation);
    assertTrue(docOpRetainAndDeleteCharacters.getInverse() instanceof CoreWaveletDocumentOperation);
  }

  public void testConcreteInverse() {
    assertEquals(addOp.getInverse(), removeOp);
    assertEquals(removeOp.getInverse(), addOp);
    assertEquals(docOpCharacters.getInverse(), docOpDeleteCharacters);
    assertEquals(docOpDeleteCharacters.getInverse(), docOpCharacters);
    assertEquals(docOpRetainAndCharacters.getInverse(), docOpRetainAndDeleteCharacters);
    assertEquals(docOpRetainAndDeleteCharacters.getInverse(), docOpRetainAndCharacters);
  }

  public void testAddParticipant() throws OperationException {
    assertOpInvertible(addOp);
    addOp.apply(wavelet);
    assertListEquals(wavelet.getParticipants(), CollectionUtils.newArrayList(PARTICIPANT_ID));
  }

  public void testRemoveParticipant() throws OperationException {
    wavelet.addParticipant(PARTICIPANT_ID);
    originalWavelet.addParticipant(PARTICIPANT_ID);
    assertOpInvertible(removeOp);
  }

  public void testNoOp() throws OperationException {
    assertOpInvertible(noOp);
  }

  public void testCharacters() throws OperationException {
    assertOpInvertible(docOpCharacters);
  }

  public void testDeleteCharacters() throws OperationException {
    assertOpsInvertible(CollectionUtils.newArrayList(docOpCharacters, docOpDeleteCharacters));
    docOpCharacters.apply(wavelet);
    docOpCharacters.apply(originalWavelet);
    assertOpInvertible(docOpDeleteCharacters);
  }

  public void testRetainAndCharacters() throws OperationException {
    assertOpsInvertible(CollectionUtils.newArrayList(docOpCharacters, docOpRetainAndCharacters));
  }

  public void testRetainAndDeleteCharacters() throws OperationException {
    assertOpsInvertible(CollectionUtils.newArrayList(
        docOpCharacters, docOpRetainAndCharacters, docOpRetainAndDeleteCharacters));
    docOpCharacters.apply(wavelet);
    docOpCharacters.apply(originalWavelet);
    assertOpsInvertible(CollectionUtils.newArrayList(
        docOpRetainAndCharacters, docOpRetainAndDeleteCharacters));
  }

  public void testComplex() throws OperationException {
    assertOpInvertible(docOpComplex);
  }

  // Help

  private void assertOpInvertible(CoreWaveletOperation op) throws OperationException {
    assertOpsInvertible(CollectionUtils.newArrayList(op));
  }

  private void assertOpsInvertible(List<CoreWaveletOperation> ops) throws OperationException {
    assertOpListEquals(ops, getInverse(getInverse(ops)));
    for (CoreWaveletOperation op : ops) {
      op.apply(wavelet);
    }
    for (CoreWaveletOperation op : getInverse(ops)) {
      op.apply(wavelet);
    }
    assertWaveletEquals(wavelet, originalWavelet);
  }

  private List<CoreWaveletOperation> getInverse(List<CoreWaveletOperation> ops) {
    List<CoreWaveletOperation> inverse = CollectionUtils.newArrayList();
    for (CoreWaveletOperation op : ops) {
      inverse.add(op.getInverse());
    }
    Collections.reverse(inverse);
    return inverse;
  }

  private void assertWaveletEquals(CoreWaveletData w1, CoreWaveletData w2) {
    assertEquals(w1.getWaveletName(), w2.getWaveletName());
    assertListEquals(w1.getParticipants(), w2.getParticipants());

    Map<String, DocOp> docs1 = w1.getDocuments();
    Map<String, DocOp> docs2 = w2.getDocuments();

    List<String> docKeys1 = CollectionUtils.newArrayList(docs1.keySet());
    Collections.sort(docKeys1);
    List<String> docKeys2 = CollectionUtils.newArrayList(docs2.keySet());
    Collections.sort(docKeys2);
    assertListEquals(docKeys1, docKeys2);

    for (String key : docKeys1) {
      assertOpEquals(docs1.get(key), docs2.get(key));
    }
  }

  private void assertOpListEquals(List<CoreWaveletOperation> l1, List<CoreWaveletOperation> l2) {
    assertEquals(l1.size(), l2.size());
    for (int i = 0; i < l1.size(); i++) {
      assertEquals(l1.get(i), l2.get(i));
    }
  }

  private void assertOpEquals(DocOp op1, DocOp op2) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(op1, op2));
  }

  private void assertListEquals(List<?> l1, List<?> l2) {
    assertEquals(l1.size(), l2.size());
    for (int i = 0; i < l1.size(); i++) {
      assertEquals(l1.get(i), l2.get(i));
    }
  }
}
