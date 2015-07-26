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

package org.waveprotocol.box.server.waveserver;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.server.util.testing.TestingConstants.PARTICIPANT;
import static org.waveprotocol.box.server.util.testing.TestingConstants.WAVE_ID;

import com.google.wave.api.SearchResult.Digest;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.robots.operations.TestingWaveletData;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.SupplementedWave;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * Unit tests for {@link WaveDigester}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class WaveDigesterTest extends TestCase {

  private static final WaveletId CONVERSATION_WAVELET_ID = WaveletId.of("example.com", "conv+root");

  @Mock private IdGenerator idGenerator;

  private ConversationUtil conversationUtil;

  private WaveDigester digester;

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);

    conversationUtil = new ConversationUtil(idGenerator);
    digester = new WaveDigester(conversationUtil);
  }

  public void testWaveletWithNoBlipsResultsInEmptyTitleAndNoBlips() {
    TestingWaveletData data =
        new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, true);
    ObservableWaveletData observableWaveletData = data.copyWaveletData().get(0);
    ObservableWavelet wavelet = OpBasedWavelet.createReadOnly(observableWaveletData);
    ObservableConversationView conversation = conversationUtil.buildConversation(wavelet);

    SupplementedWave supplement = mock(SupplementedWave.class);
    when(supplement.isUnread(any(ConversationBlip.class))).thenReturn(true);

    Digest digest = digester.generateDigest(conversation, supplement, observableWaveletData);

    assertEquals("", digest.getTitle());
    assertEquals(digest.getBlipCount(), 0);
  }


  public void testWaveletWithBlipsResultsInNonEmptyTitle() {
    TestingWaveletData data =
        new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, true);
    String title = "title";
    data.appendBlipWithText(title);
    ObservableWaveletData observableWaveletData = data.copyWaveletData().get(0);
    ObservableWavelet wavelet = OpBasedWavelet.createReadOnly(observableWaveletData);
    ObservableConversationView conversation = conversationUtil.buildConversation(wavelet);

    SupplementedWave supplement = mock(SupplementedWave.class);
    when(supplement.isUnread(any(ConversationBlip.class))).thenReturn(true);

    Digest digest = digester.generateDigest(conversation, supplement, observableWaveletData);

    assertEquals(title, digest.getTitle());
    assertEquals(1, digest.getBlipCount());
  }

  public void testUnreadCount() {
    TestingWaveletData data =
        new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, true);
    data.appendBlipWithText("blip number 1");
    data.appendBlipWithText("blip number 2");
    data.appendBlipWithText("blip number 3");
    ObservableWaveletData observableWaveletData = data.copyWaveletData().get(0);
    ObservableWavelet wavelet = OpBasedWavelet.createReadOnly(observableWaveletData);
    ObservableConversationView conversation = conversationUtil.buildConversation(wavelet);

    SupplementedWave supplement = mock(SupplementedWave.class);
    when(supplement.isUnread(any(ConversationBlip.class))).thenReturn(true, true, false);
    Digest digest = digester.generateDigest(conversation, supplement, observableWaveletData);

    assertEquals(3, digest.getBlipCount());
    assertEquals(2, digest.getUnreadCount());
  }
}
