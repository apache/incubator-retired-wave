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

package org.waveprotocol.wave.model.conversation;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mockito.InOrder;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.impl.UncheckedDocOpBuffer;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;

public class WaveBasedConversationViewTest extends ConversationViewTestBase {

  private FakeWaveView waveView;
  private IdGenerator idGenerator;
  private WaveBasedConversationView waveBasedConversationView;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    idGenerator = FakeIdGenerator.create();
    waveView = ConversationTestUtils.createWaveView(idGenerator);
    this.waveBasedConversationView =
        WaveBasedConversationView.create(waveView, idGenerator);
  }

  @Override
  protected WaveBasedConversationView getConversationView() {
    return waveBasedConversationView;
  }

  @Override
  protected void removeConversation(ObservableConversation conv) {
    assert conv instanceof WaveletBasedConversation;
    waveView.removeWavelet(((WaveletBasedConversation) conv).getWavelet());
  }

  public void testCreateRootUsesRootWavelet() {
    WaveletBasedConversation conv = waveBasedConversationView.createRoot();
    assertTrue(IdUtil.isConversationRootWaveletId(conv.getWavelet().getId()));
  }

  public void testCreateUsesConversationalId() {
    WaveBasedConversationView target = WaveBasedConversationView.create(waveView,
        idGenerator);
    WaveletBasedConversation conv = target.createConversation();
    assertFalse(IdUtil.isConversationRootWaveletId(conv.getWavelet().getId()));
  }

  public void testNonExistentConversationIsNull() {
    assertNull(waveBasedConversationView.getConversation(WaveletId.of("foo.com", "conv+bar")));
  }

  public void testManifestErasureFiresConversationRemoved() {
    ObservableConversation conv = waveBasedConversationView.createRoot();
    ObservableConversationView.Listener listener = mock(ObservableConversationView.Listener.class);

    waveBasedConversationView.addListener(listener);
    erase(WaveletBasedConversation.getManifestDocument(waveView.getRoot()));

    verify(listener).onConversationRemoved(conv);
  }

  public void testManifestAtomicReplacementFiresConversationRemovedThenAdded() {
    ObservableConversation conv = waveBasedConversationView.createRoot();
    ObservableConversationView.Listener listener = mock(ObservableConversationView.Listener.class);

    waveBasedConversationView.addListener(listener);
    restore(WaveletBasedConversation.getManifestDocument(waveView.getRoot()));

    InOrder order = inOrder(listener);
    order.verify(listener).onConversationRemoved(conv);
    order.verify(listener).onConversationAdded(waveBasedConversationView.getRoot());
  }

  private void erase(Document doc) {
    doc.emptyElement(doc.getDocumentElement());
  }

  private void restore(Document doc) {
    // No comment.
    UncheckedDocOpBuffer builder = new UncheckedDocOpBuffer();
    doc.toInitialization().apply(builder);
    DocOp state = builder.finish();
    DocOp erasure = DocOpInverter.invert(state);
    DocOp restoration;
    try {
      restoration = Composer.compose(erasure, state);
    } catch (OperationException e) {
      // If the code above fails, then there is a bug in the operation code, not
      // these tests.  Fail with an exception, not with a JUnit fail().
      throw new RuntimeException(e);
    }

    doc.hackConsume(Nindo.fromDocOp(restoration, false));
  }
}
