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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ModelTestUtils;
import org.waveprotocol.wave.model.testing.WaveletDataFactory;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.List;

/**
 * Test for WaveBlipOperation.
 *
 * @author zdwang@google.com (David Wang)
 */

public class VersionUpdateOpTest extends TestCase {

  private static final String ROOT_BLIP = "root";
  private static final byte[] SIGNATURE = new byte[] { 4, 4, 4, 4 };
  private static final byte[] SIGNATURE2 = new byte[] { 8, 8, 8, 8 };

  private static final ParticipantId CREATOR = new ParticipantId("lars@gwave.com");
  private static final ParticipantId FRED = new ParticipantId("fred@gwave.com");

  /**
   * An operation context typical of a locally-generated op; doesn't update
   * metadata.
   */
  private static final WaveletOperationContext LOCAL_CONTEXT =
      new WaveletOperationContext(CREATOR, Constants.NO_TIMESTAMP, 0);

  private WaveletDataImpl waveletData;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    waveletData = WaveletDataFactory.of(BasicFactories.waveletDataImplFactory()).create();
  }

  /**
   * Tests version update from a wavelet op ack.
   */
  public void testVersionUpdateOpFromWaveletOpAck() throws OperationException {
    assertWaveletMetadataIsInitialState();
    // Apply an op that has no version information.
    WaveletOperationContext context = LOCAL_CONTEXT;
    WaveletOperation op = addParticipant(context);
    assertWaveletMetadataIsInitialState();

    // Create version update op from ack.
    WaveletOperation updateOp = op.createVersionUpdateOp(5, HashedVersion.of(5, SIGNATURE));
    updateOp.apply(waveletData);
    assertWaveletVersion(5);
    assertWaveletSignature(HashedVersion.of(5, SIGNATURE));
  }

  /**
   * Tests version update from a blip no-op ack.
   */
  public void testVersionUpdateOpFromBlipNoOpAck() throws Exception {
    assertWaveletMetadataIsInitialState();
    // Apply a local op that has no version information.
    WaveletOperation waveletOp = touchBlip(LOCAL_CONTEXT, ROOT_BLIP);
    assertWaveletMetadataIsInitialState();

    // Create version update op from ack.
    WaveletOperation updateOp = waveletOp.createVersionUpdateOp(1,
        HashedVersion.of(1, SIGNATURE));
    updateOp.apply(waveletData);
    assertWaveletVersion(1);
    assertWaveletSignature(HashedVersion.of(1, SIGNATURE));
    assertBlipVersion(ROOT_BLIP, 0);
  }

  /**
   * Tests version update from a blip content operation ack.
   */
  public void testVersionUpdateOpFromBlipContentOperationAck() throws Exception {
    assertWaveletMetadataIsInitialState();
    // Apply a document mutation.
    WaveletOperation waveletOp = mutateBlip(LOCAL_CONTEXT, ROOT_BLIP);
    assertWaveletMetadataIsInitialState();

    // Create version update op from ack.
    WaveletOperation updateOp = waveletOp.createVersionUpdateOp(1,
        HashedVersion.of(1, SIGNATURE));
    updateOp.apply(waveletData);
    assertWaveletVersion(1);
    assertWaveletSignature(HashedVersion.of(1, SIGNATURE));
    assertBlipVersion(ROOT_BLIP, 1);
  }

  /**
   * Test a version is set properly if there are version info in Submit
   */
  public void testVersionUpdateOpFromSubmitAck() throws Exception {
    assertWaveletMetadataIsInitialState();
    // Apply a submit.
    BlipOperation blipOp = new SubmitBlip(LOCAL_CONTEXT);
    WaveletOperation waveletOp = new WaveletBlipOperation(ROOT_BLIP, blipOp);
    waveletOp.apply(waveletData);
    assertWaveletMetadataIsInitialState();

    // Create version update op from ack.
    WaveletOperation updateOp = waveletOp.createVersionUpdateOp(1,
        HashedVersion.of(1, SIGNATURE));
    updateOp.apply(waveletData);
    assertWaveletVersion(1);
    assertWaveletSignature(HashedVersion.of(1, SIGNATURE));
    assertBlipVersion(ROOT_BLIP, 0);
  }

  public void testMultipleVersionUpdates() throws OperationException {
    assertWaveletMetadataIsInitialState();

    // First add participant.
    WaveletOperation addParticipantOp = addParticipant(LOCAL_CONTEXT);
    WaveletOperation addParticipantUpdateOp = addParticipantOp.createVersionUpdateOp(1,
        HashedVersion.of(1, SIGNATURE));
    addParticipantUpdateOp.apply(waveletData);
    assertWaveletVersion(1);
    assertWaveletSignature(HashedVersion.of(1, SIGNATURE));

    // Then mutate blip.
    WaveletOperation mutateBlipOp = mutateBlip(LOCAL_CONTEXT, ROOT_BLIP);
    WaveletOperation mutateBlipUpdateOp = mutateBlipOp.createVersionUpdateOp(1,
        HashedVersion.of(2, SIGNATURE2));
    mutateBlipUpdateOp.apply(waveletData);
    assertWaveletVersion(2);
    assertWaveletSignature(HashedVersion.of(2, SIGNATURE2));
    assertBlipVersion(ROOT_BLIP, 2);
  }

  /**
   * Test a version is set properly if we reverse the version op.
   */
  public void testReverse() throws Exception {
    assertWaveletMetadataIsInitialState();
    // Apply a document mutation.
    WaveletOperation waveletOp = mutateBlip(LOCAL_CONTEXT, ROOT_BLIP);
    assertWaveletMetadataIsInitialState();

    // Create version update op from ack.
    WaveletOperation updateOp = waveletOp.createVersionUpdateOp(1,
        HashedVersion.of(1, SIGNATURE));
    List<? extends WaveletOperation> reverse = updateOp.applyAndReturnReverse(waveletData);

    assertWaveletVersion(1);
    assertWaveletSignature(HashedVersion.of(1, SIGNATURE));
    assertBlipVersion(ROOT_BLIP, 1);

    // apply the reverse and see the version is back to it was before
    assertEquals(1, reverse.size());
    reverse.get(0).apply(waveletData);
    assertWaveletMetadataIsInitialState();
  }

  private void assertWaveletVersion(long waveletVersion) {
    assertEquals(waveletVersion, waveletData.getVersion());
  }

  private void assertWaveletSignature(HashedVersion distinctVersion) {
    assertEquals(distinctVersion, waveletData.getHashedVersion());
  }

  private void assertBlipVersion(String blipId, long lastModifiedVersion) {
    assertEquals(lastModifiedVersion, waveletData.getDocument(blipId).getLastModifiedVersion());
  }

  private void assertWaveletMetadataIsInitialState() {
    assertWaveletVersion(0);
    assertWaveletSignature(HashedVersion.unsigned(0));
    BlipData blip = waveletData.getDocument(ROOT_BLIP);
    if (blip != null) {
      assertBlipVersion(ROOT_BLIP, 0);
    }
  }

  /**
   * Applies an op to add FRED to the wavelet with some context.
   *
   * @return the operation applied
   */
  private WaveletOperation addParticipant(WaveletOperationContext context)
      throws OperationException {
    WaveletOperation waveletOp = new AddParticipant(context, FRED);
    waveletOp.apply(waveletData);
    return waveletOp;
  }

  /**
   * Applies an empty document op to a blip with some context.
   *
   * @return the operation applied
   */
  private WaveletOperation touchBlip(WaveletOperationContext context, String blipId)
      throws OperationException {
    WaveletOperation waveletOp = new WaveletBlipOperation(blipId,
        new BlipContentOperation(context, EmptyDocument.EMPTY_DOCUMENT));
    waveletOp.apply(waveletData);
    return waveletOp;
  }

  private WaveletOperation mutateBlip(WaveletOperationContext context, String blipId)
      throws OperationException {
    DocOp docOp = ModelTestUtils.createContent("Hello");
    BlipOperation blipOp = new BlipContentOperation(LOCAL_CONTEXT, docOp);
    WaveletOperation waveletBlipOp = new WaveletBlipOperation(blipId, blipOp);
    waveletBlipOp.apply(waveletData);
    return waveletBlipOp;
  }
}
