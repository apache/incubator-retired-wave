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

package org.waveprotocol.wave.client.wavepanel.impl.diff;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.wave.DocumentRegistry;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.testing.FakeConversationView;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

/**
 * Tests for {@link DiffController}
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class DiffControllerTest extends TestCase {

  interface MockDoc extends InteractiveDocument, DocumentOperationSink {
  }

  @Mock
  ObservableSupplementedWave supplement;
  @Mock
  ModelAsViewProvider models;
  @Mock
  DocumentRegistry<MockDoc> documents;

  private DiffController target;
  private FakeConversationView wave;

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);

    wave = FakeConversationView.builder().build();
    target = new DiffController(wave, supplement, documents, models);
  }

  public void testFreshBlipsAreSuppressedOnStartup() {
    Conversation conv = wave.createRoot();
    ConversationBlip root = conv.getRootThread().appendBlip();

    MockDoc fresh = mock(MockDoc.class);
    when(fresh.isCompleteDiff()).thenReturn(true, false);
    when(documents.get(root)).thenReturn(fresh);
    target.install();

    verify(fresh).startDiffSuppression();
    verify(fresh, never()).stopDiffSuppression();
  }

  public void testReadBlipsNotSuppressedOnStartup() {
    Conversation conv = wave.createRoot();
    ConversationBlip root = conv.getRootThread().appendBlip();

    MockDoc seen = mock(MockDoc.class);
    when(seen.isCompleteDiff()).thenReturn(false);
    when(documents.get(root)).thenReturn(seen);
    target.install();

    verify(seen, never()).startDiffSuppression();
    verify(seen, never()).stopDiffSuppression();
  }

  public void testDynamicFreshBlipsAreSuppressed() {
    Conversation conv = wave.createRoot();

    target.install();

    // Instrument first.
    MockDoc doc = mock(MockDoc.class);
    when(doc.isCompleteDiff()).thenReturn(true, false);
    when(documents.get(any(ConversationBlip.class))).thenReturn(doc);

    conv.getRootThread().appendBlip();

    verify(doc).startDiffSuppression();
    verify(doc, never()).stopDiffSuppression();
  }

  public void testFreshBecomingReadStopsSuppression() {
    ObservableConversation conv = wave.createRoot();
    ObservableConversationBlip root = conv.getRootThread().appendBlip();

    MockDoc doc = mock(MockDoc.class);
    when(doc.isCompleteDiff()).thenReturn(true, false);
    when(documents.get(root)).thenReturn(doc);
    target.install();

    ArgumentCaptor<ObservableSupplementedWave.Listener> d = ArgumentCaptor.forClass(ObservableSupplementedWave.Listener.class);
    verify(supplement).addListener(d.capture());
    d.getValue().onMaybeBlipReadChanged(root);

    verify(doc).startDiffSuppression();
    verify(doc).stopDiffSuppression();
  }

  public void testReadBecomingReadClearsDiffsButNotSuppression() {
    ObservableConversation conv = wave.createRoot();
    ObservableConversationBlip root = conv.getRootThread().appendBlip();

    MockDoc doc = mock(MockDoc.class);
    when(doc.isCompleteDiff()).thenReturn(false);
    when(documents.get(root)).thenReturn(doc);
    target.install();

    ArgumentCaptor<ObservableSupplementedWave.Listener> d = ArgumentCaptor.forClass(ObservableSupplementedWave.Listener.class);
    verify(supplement).addListener(d.capture());
    d.getValue().onMaybeBlipReadChanged(root);

    verify(doc).clearDiffs();
    verify(doc, never()).startDiffSuppression();
    verify(doc, never()).stopDiffSuppression();
  }

  public void testEditModeStartsAndStopsDiffSuppression() {
    target.install();

    Editor e = mock(Editor.class);
    MockDoc doc = mock(MockDoc.class);
    ConversationBlip blip = mock(ConversationBlip.class);
    BlipView blipUi = mock(BlipView.class);
    when(models.getBlip(blipUi)).thenReturn(blip);
    when(documents.get(blip)).thenReturn(doc);

    target.onSessionStart(e, blipUi);
    target.onSessionEnd(e, blipUi);

    verify(doc).startDiffSuppression();
    verify(doc).stopDiffSuppression();
  }
}
