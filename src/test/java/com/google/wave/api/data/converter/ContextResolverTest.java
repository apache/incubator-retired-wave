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

package com.google.wave.api.data.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.Context;
import com.google.wave.api.data.converter.v21.EventDataConverterV21;
import com.google.wave.api.impl.EventMessageBundle;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Collections;
import java.util.Set;

/**
 * Testcases for the {@link ContextResolver}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */

public class ContextResolverTest extends TestCase {

  private Conversation conversation;
  private Wavelet wavelet;
  private EventMessageBundle eventMessages;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Blips.init();

    conversation = makeConversation();
    wavelet = createMockWavelet(conversation);
    eventMessages = new EventMessageBundle("foo@appspot.com", "http://opensocial.example.com");
  }

  public void testResolveContext() throws Exception {
    eventMessages.requireBlip(
        conversation.getRootThread().getFirstBlip().getId(), Lists.newArrayList(Context.CHILDREN));

    ContextResolver.resolveContext(
        eventMessages, wavelet, conversation, new EventDataConverterV21());

    Set<String> blips = eventMessages.getBlipData().keySet();
    assertEquals(2, blips.size());
    for (ConversationBlip blip : conversation.getRootThread().getBlips()) {
      assertTrue(blips.contains(blip.getId()));
    }
  }

  public void testResolveRootContext() throws Exception {
    ConversationBlip newBlip = conversation.getRootThread().appendBlip();
    eventMessages.requireBlip(newBlip.getId(), Lists.newArrayList(Context.ROOT));

    ContextResolver.resolveContext(
        eventMessages, wavelet, conversation, new EventDataConverterV21());

    Set<String> blips = eventMessages.getBlipData().keySet();
    assertEquals(2, blips.size());
    assertTrue(blips.contains(conversation.getRootThread().getFirstBlip().getId()));
    assertTrue(blips.contains(newBlip.getId()));
  }

  private Wavelet createMockWavelet(Conversation forConversation) {
    Wavelet mockWavelet = mock(Wavelet.class);
    when(mockWavelet.getCreationTime()).thenReturn(123L);
    ParticipantId creator = new ParticipantId("foo@test.com");
    when(mockWavelet.getCreatorId()).thenReturn(creator);
    when(mockWavelet.getWaveId()).thenReturn(WaveId.of("example.com", "123"));
    when(mockWavelet.getId()).thenReturn(WaveletId.of("example.com", "conv+root"));
    when(mockWavelet.getLastModifiedTime()).thenReturn(123L);
    when(mockWavelet.getVersion()).thenReturn(1L);
    when(mockWavelet.getDocumentIds()).thenReturn(Collections.<String>emptySet());
    return mockWavelet;
  }

  private Conversation makeConversation() {
    IdGenerator idGenerator = FakeIdGenerator.create();
    ObservableWaveView waveView = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    ConversationView convView = WaveBasedConversationView.create(waveView, idGenerator);
    Conversation conversation = convView.createRoot();
    conversation.getRootThread().appendBlip();
    conversation.getRootThread().appendBlip();
    return conversation;
  }
}
