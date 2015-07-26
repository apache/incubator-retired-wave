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

package org.waveprotocol.box.server.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;

/**
 * Some utility methods for manipulating wavelet data.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class TestDataUtil {
  /**
   * Create a wavelet data object for testing.
   *
   * @return a simple wavelet.
   */
  public static WaveletData createSimpleWaveletData() {
    WaveletName name = WaveletName.of(
        WaveId.of("example.com", "w+abc123"), WaveletId.of("example.com", "conv+root"));
    ParticipantId creator = ParticipantId.ofUnsafe("sam@example.com");
    long time = 1234567890;

    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(name, creator,
        HashedVersion.unsigned(0), time);

    DocInitialization content = new DocInitializationBuilder().characters("Hello there").build();
    wavelet.createDocument(
        "b+abc123", creator, Collections.<ParticipantId> emptySet(), content, time, 0);

    return wavelet;
  }

  /**
   * Check that the serialized fields of two documents match one another.
   */
  public static void checkSerializedDocument(BlipData expected, BlipData actual) {
    assertNotNull(expected);
    assertNotNull(actual);

    assertEquals(expected.getId(), actual.getId());

    assertTrue(
        DocCompare.equivalent(DocCompare.STRUCTURE, expected.getContent().getMutableDocument(),
            actual.getContent().getMutableDocument()));

    assertEquals(expected.getAuthor(), actual.getAuthor());
    assertEquals(expected.getContributors(), actual.getContributors());
    assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
    assertEquals(expected.getLastModifiedVersion(), actual.getLastModifiedVersion());
  }

  /**
   * Check that the serialized fields of two wavelets are equal.
   */
  public static void checkSerializedWavelet(WaveletData expected, WaveletData actual) {
    assertNotNull(expected);
    assertNotNull(actual);

    assertEquals(expected.getWaveId(), actual.getWaveId());
    assertEquals(expected.getParticipants(), actual.getParticipants());
    assertEquals(expected.getVersion(), actual.getVersion());
    assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
    assertEquals(expected.getCreator(), actual.getCreator());
    assertEquals(expected.getCreationTime(), actual.getCreationTime());

    // & check that the documents the wavelets contain are also the same.
    assertEquals(expected.getDocumentIds(), actual.getDocumentIds());
    for (String docId : expected.getDocumentIds()) {
      checkSerializedDocument(expected.getDocument(docId), actual.getDocument(docId));
    }
  }
}
