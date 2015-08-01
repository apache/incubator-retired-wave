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

package org.waveprotocol.wave.model.supplement;

import static org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow.ALWAYS;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.testing.FakeConversationView;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.schema.account.AccountSchemas;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.schema.supplement.UserDataSchemas;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

/**
 *
 */

public final class WaveletBasedSupplementTest extends TestCase {

  private static final SchemaCollection schemas = new SchemaCollection();
  static {
    schemas.add(new AccountSchemas());
    schemas.add(new ConversationSchemas());
    schemas.add(new UserDataSchemas());
  }

  public void testLoadOnDomainlessWaveletIdsDoesNotFail() {
    DocInitialization readOp =
        BasicFactories.observableDocumentProvider().parse(
            "<wavelet i=\"conversation/root\"></wavelet>").toInitialization();
    DocInitialization archiveOp =
        BasicFactories.observableDocumentProvider().parse(
            "<archive i=\"conversation/root\"></archive>").toInitialization();
    DocInitialization seenOp =
        BasicFactories.observableDocumentProvider().parse(
            "<seen i=\"conversation/root\"></seen>").toInitialization();

    FakeWaveView view = BasicFactories.fakeWaveViewBuilder().build();
    Wavelet userData = view.createUserData();

    userData.getDocument(WaveletBasedSupplement.READSTATE_DOCUMENT).hackConsume(
        Nindo.fromDocOp(readOp, false));
    userData.getDocument(WaveletBasedSupplement.ARCHIVING_DOCUMENT).hackConsume(
        Nindo.fromDocOp(archiveOp, false));
    userData.getDocument(WaveletBasedSupplement.SEEN_DOCUMENT).hackConsume(
        Nindo.fromDocOp(seenOp, false));

    WaveletBasedSupplement.create(userData);
  }

  /**
   * Tests that the copy logic in
   * {@link PrimitiveSupplementImpl#PrimitiveSupplementImpl(PrimitiveSupplement)}
   * does not trigger any writes.
   *
   * This is a regression test for bug 2459305.
   */
  public void testCopySnapshotDoesNotCauseWrites() {
    FakeConversationView view = FakeConversationView.builder().with(schemas).build();
    Wavelet udw = view.getWaveView().createUserData();
    PrimitiveSupplement substrate = WaveletBasedSupplement.create(udw);
    ParticipantId viewer = new ParticipantId("nobody@google.com");
    SupplementedWave swave = SupplementedWaveImpl.create(substrate, view, viewer, ALWAYS);

    // Do something that is readable
    view.createRoot().addParticipant(viewer);
    swave.markAsRead();

    // Save version to detect writes
    long udwVersion = udw.getVersion();

    // Copy into pojo
    new PrimitiveSupplementImpl(substrate);

    // Verify no writes.
    assertEquals(udwVersion, udw.getVersion());
  }
}
