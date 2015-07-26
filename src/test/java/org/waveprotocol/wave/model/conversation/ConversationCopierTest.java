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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

/**
 * Unit tests for conversation copy utility methods.
 *
 */

public class ConversationCopierTest extends TestCase {
  private static final String SAMPLE_TEXT = "some sample text";

  private WaveletBasedConversation source;
  private ObservableWavelet destWavelet;
  private ObservableWavelet sourceWavelet;

  @Override
  protected void setUp() throws Exception {
    IdGenerator idGenerator = FakeIdGenerator.create();
    ObservableWaveView waveView = ConversationTestUtils.createWaveView(idGenerator);
    WaveBasedConversationView conversationView = WaveBasedConversationView
        .create(waveView, idGenerator);
    destWavelet = ConversationTestUtils.createWaveView(idGenerator).createRoot();
    source = conversationView.createConversation();
    sourceWavelet = getWavelet(source);
  }

  /**
   * Tests copy of an empty wavelet.
   */
  public void testEmptyWaveletCopy() {
    ConversationCopier.copyWaveletContents(sourceWavelet, destWavelet);
    compareWavelets(sourceWavelet, destWavelet);
  }

  /**
   * Tests copying of a wavelet with a single blip containing some content.
   */
  public void testSingleBlip() {
    WaveletBasedConversationBlip blip = source.getRootThread().appendBlip();
    Document doc = blip.getContent();
    LineContainers.appendToLastLine(doc, XmlStringBuilder.createText(SAMPLE_TEXT));
    ConversationCopier.copyWaveletContents(sourceWavelet, destWavelet);
    compareWavelets(sourceWavelet, destWavelet);
  }

  /**
   * Tests copying of a wavelet with a root thread and a reply thread.
   */
  public void testReplyThreadCopy() {
    WaveletBasedConversationBlip blip = source.getRootThread().appendBlip();
    WaveletBasedConversationThread conversationThread = blip.addReplyThread();
    Document doc = conversationThread.appendBlip().getContent();
    LineContainers.appendToLastLine(doc, XmlStringBuilder.createText(SAMPLE_TEXT));
    ConversationCopier.copyWaveletContents(sourceWavelet, destWavelet);
    compareWavelets(sourceWavelet, destWavelet);
  }

  @SuppressWarnings("deprecation")
  private static ObservableWavelet getWavelet(WaveletBasedConversation wavelet) {
    return wavelet.getWavelet();
  }

  private static void compareWavelets(ObservableWavelet wavelet1, ObservableWavelet wavelet2) {
    assertEquals(wavelet1.getDocumentIds().size(), wavelet2.getDocumentIds().size());
    for (String docId : wavelet1.getDocumentIds()) {
      assertTrue(DocCompare.equivalent(DocCompare.ALL & (~DocCompare.ANNOTATIONS),
          wavelet2.getDocument(docId), wavelet1.getDocument(docId)));
    }
  }
}
