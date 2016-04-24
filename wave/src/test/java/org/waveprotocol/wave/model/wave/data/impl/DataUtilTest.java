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

package org.waveprotocol.wave.model.wave.data.impl;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.testing.ModelTestUtils;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.wave.data.core.impl.CoreWaveletDataImpl;

import java.util.HashMap;
import java.util.Map;

/**
 */

public class DataUtilTest extends TestCase {

  /**
   * Creates map of sequentially generated docIds (beginning with prefix + "0")
   * to the given documents contents.
   */
  private static Map<String, DocOp> createDocuments(String documentNamePrefix,
      DocOp... contents) {
    Map<String, DocOp> docs = new HashMap<String, DocOp>();
    int i = 0;
    for (DocOp c : contents) {
      docs.put(documentNamePrefix + i, c);
      i++;
    }
    return docs;
  }

  /**
   * Creates a CoreWaveletData with the given name, documents, and participants.
   */
  private static CoreWaveletData createCoreWaveletData(WaveletName name,
      Map<String, DocOp> documents, ParticipantId... participants)
      throws OperationException {
    CoreWaveletDataImpl data = new CoreWaveletDataImpl(name.waveId, name.waveletId);
    for (ParticipantId p : participants) {
      data.addParticipant(p);
    }
    for (Map.Entry<String, DocOp> d : documents.entrySet()) {
      data.modifyDocument(d.getKey(), d.getValue());
    }
    return data;
  }

  private static void assertOpEquals(DocOp expected, DocOp actual) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, actual));
  }

  private static final WaveletName WAVELET_NAME =
      WaveletName.of(WaveId.of("example.com", "wave"), WaveletId.of("example.com", "wavelet"));
  private static final DocInitialization[] CONTENTS = new DocInitialization[]{
    ModelTestUtils.createContent("the quick brown fox"),
    ModelTestUtils.createContent("nothing to fear but fear itself"),
    ModelTestUtils.createContent("these are not the droids you are looking for")
  };
  private static final ParticipantId BOB = new ParticipantId("bob@example.com");
  private static final ParticipantId JOE = new ParticipantId("joe@example.com");
  private static final ParticipantId LIZ = new ParticipantId("liz@example.com");
  HashedVersion VERSION = HashedVersion.of(42L, new byte[] {1, 2, 3, 4});

  public void testFromCoreWaveletData() throws Exception {
    Map<String, DocOp> docs = createDocuments("b+", CONTENTS);
    CoreWaveletData core = createCoreWaveletData(WAVELET_NAME, docs, BOB, JOE);
    // sanity checks
    assertEquals(WAVELET_NAME, core.getWaveletName());
    assertEquals(CONTENTS.length, core.getDocuments().size());
    assertOpEquals(CONTENTS[0], core.getDocuments().get("b+0"));
    assertEquals(CollectionUtils.newArrayList(BOB, JOE), core.getParticipants());

    ObservableWaveletData obs =
        DataUtil.fromCoreWaveletData(core, VERSION, SchemaCollection.empty());
    assertEquals(BOB, obs.getCreator());
    assertEquals(0L, obs.getCreationTime());
    assertEquals(0L, obs.getLastModifiedTime());
    assertEquals(VERSION, obs.getHashedVersion());
    assertEquals(VERSION.getVersion(), obs.getVersion());
    assertEquals(WAVELET_NAME.waveId, obs.getWaveId());
    assertEquals(WAVELET_NAME.waveletId, obs.getWaveletId());
    assertEquals(CollectionUtils.immutableSet(BOB, JOE), obs.getParticipants());

    assertEquals(docs.keySet(), obs.getDocumentIds());
    for (Map.Entry<String, DocOp> d : docs.entrySet()) {
      BlipData blip = obs.getDocument(d.getKey());
      WaveletData wavelet = blip.getWavelet();
      assertEquals(WAVELET_NAME.waveId, wavelet.getWaveId());
      assertEquals(WAVELET_NAME.waveletId, wavelet.getWaveletId());
      assertEquals(d.getKey(), blip.getId());
      assertNotNull(blip.getAuthor());
      assertEquals(0L, blip.getLastModifiedTime());
      assertEquals(-1, blip.getLastModifiedVersion());
      assertTrue(blip.getContributors().isEmpty());
      assertOpEquals(d.getValue(), blip.getContent().asOperation());

      try {
        blip.addContributor(JOE);
        fail("blips from DataUtil.fromCoreWaveletData() are immutable");
      } catch (UnsupportedOperationException expected) {
      }
    }

    try {
      obs.addParticipant(LIZ);
      fail("wavelets from DataUtil.fromCoreWaveletData() are immutable");
    } catch (UnsupportedOperationException expected) {
    }

    // test that changes to core are reflected in data
    core.addParticipant(LIZ);
    assertEquals(CollectionUtils.newArrayList(BOB, JOE, LIZ), core.getParticipants());
    assertEquals(CollectionUtils.immutableSet(BOB, JOE, LIZ), obs.getParticipants());
  }
}
