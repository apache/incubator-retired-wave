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

package com.google.wave.api.data.converter.v22;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.BlipData;
import com.google.wave.api.BlipThread;
import com.google.wave.api.impl.EventMessageBundle;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link EventDataConverterV22}.
 *
 */

public class EventDataConverterV22Test extends TestCase {

  private static final WaveId WAVE_ID = WaveId.of("example.com", "123");
  private static final WaveletId WAVELET_ID = WaveletId.of("example.com", "conv+root");

  private Conversation conversation;

  @Override
  protected void setUp() throws Exception {
    Blips.init();
    conversation = makeConversation();
  }

  public void testToBlipData() throws Exception {
    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getWaveId()).thenReturn(WAVE_ID);
    when(wavelet.getId()).thenReturn(WAVELET_ID);

    ConversationBlip blip = conversation.getRootThread().getFirstBlip();
    String replyThreadId = blip.addReplyThread(3).getId();

    EventDataConverterV22 converter = new EventDataConverterV22();
    EventMessageBundle eventMessageBundle = new EventMessageBundle(null, null);
    BlipData blipData = converter.toBlipData(blip, wavelet,
        eventMessageBundle);
    assertEquals(blip.getThread().getId(), blipData.getThreadId());
    assertEquals(Lists.newArrayList(replyThreadId), blipData.getReplyThreadIds());
    Map<String, BlipThread> threads = eventMessageBundle.getThreads();
    assertEquals(1, threads.size());
    assertEquals(1, threads.get(replyThreadId).getLocation());
  }

  public void testFindBlipParent() {
    ConversationBlip first = conversation.getRootThread().getFirstBlip();
    ConversationBlip second = conversation.getRootThread().appendBlip();
    ConversationBlip reply = first.addReplyThread().appendBlip();
    ConversationBlip secondReply = reply.getThread().appendBlip();
    ConversationBlip inlineReply = first.addReplyThread(3).appendBlip();

    EventDataConverterV22 converter = new EventDataConverterV22();
    assertNull(converter.findBlipParent(first));
    assertNull(converter.findBlipParent(second));
    assertSame(first, converter.findBlipParent(reply));
    assertSame(first, converter.findBlipParent(inlineReply));
    assertSame(first, converter.findBlipParent(secondReply));
  }

  public void testFindBlipChildren() {
    ConversationBlip first = conversation.getRootThread().getFirstBlip();
    ConversationBlip second = conversation.getRootThread().appendBlip();
    ConversationBlip reply = first.addReplyThread().appendBlip();
    ConversationBlip secondReply = reply.getThread().appendBlip();
    ConversationBlip inlineReply = first.addReplyThread(3).appendBlip();

    EventDataConverterV22 converter = new EventDataConverterV22();
    assertEquals(0, converter.findBlipChildren(second).size());

    List<ConversationBlip> children = converter.findBlipChildren(first);
    assertEquals(3, children.size());
    assertEquals(inlineReply.getId(), children.get(0).getId());
    assertEquals(reply.getId(), children.get(1).getId());
    assertEquals(secondReply.getId(), children.get(2).getId());
  }

  private static Conversation makeConversation() {
    IdGenerator idGenerator = FakeIdGenerator.create();
    ObservableWaveView waveView = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    ConversationView convView = WaveBasedConversationView.create(waveView, idGenerator);
    Conversation conversation = convView.createRoot();
    // Force empty document.
    ConversationBlip blip = conversation.getRootThread().appendBlip(
        new DocInitializationBuilder().build());
    Document document = blip.getContent();
    document.appendXml(Blips.INITIAL_BODY);
    return conversation;
  }
}
