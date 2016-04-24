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

package com.google.wave.api.data.converter.v21;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.wave.api.impl.EventMessageBundle;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

/**
 * Test cases for {@link EventDataConverterV21}.
 *
 */

public class EventDataConverterV21Test extends TestCase {

  private static final WaveId WAVE_ID = WaveId.of("example.com", "123");
  private static final WaveletId WAVELET_ID = WaveletId.of("example.com", "conv+root");

  public void testToBlipDataHandlesBlipWithEmptyDocument() throws Exception {
    Blips.init();

    Conversation conversation = makeConversation();

    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getWaveId()).thenReturn(WAVE_ID);
    when(wavelet.getId()).thenReturn(WAVELET_ID);

    EventDataConverterV21 converter = new EventDataConverterV21();
    assertEquals("",
        converter.toBlipData(conversation.getRootThread().getFirstBlip(), wavelet,
            new EventMessageBundle(null, null)).getContent());
  }

  public void testFindBlipParent() {
    Conversation conversation = makeConversation();
    ConversationBlip first = conversation.getRootThread().getFirstBlip();
    ConversationBlip second = conversation.getRootThread().appendBlip();
    ConversationBlip reply = first.addReplyThread().appendBlip();
    ConversationBlip secondReply = reply.getThread().appendBlip();

    EventDataConverterV21 converter = new EventDataConverterV21();
    assertNull(converter.findBlipParent(first));
    assertSame(first, converter.findBlipParent(second));
    assertSame(first, converter.findBlipParent(reply));
    assertSame(reply, converter.findBlipParent(secondReply));
  }

  public void testFindBlipPreviousSibling() {
    Conversation conversation = makeConversation();
    ConversationBlip first = conversation.getRootThread().getFirstBlip();
    ConversationBlip second = conversation.getRootThread().appendBlip();
    ConversationBlip reply = first.addReplyThread().appendBlip();
    ConversationBlip secondReply = reply.getThread().appendBlip();

    assertNull(EventDataConverterV21.findPreviousSibling(first));
    assertSame(first, EventDataConverterV21.findPreviousSibling(second));
    assertNull(EventDataConverterV21.findPreviousSibling(reply));
    assertSame(reply, EventDataConverterV21.findPreviousSibling(secondReply));
  }

  public void testFindBlipNextSibling() {
    Conversation conversation = makeConversation();
    ConversationBlip first = conversation.getRootThread().getFirstBlip();
    ConversationBlip second = conversation.getRootThread().appendBlip();
    ConversationBlip reply = first.addReplyThread().appendBlip();
    ConversationBlip secondReply = reply.getThread().appendBlip();

    assertSame(second, EventDataConverterV21.findNextSibling(first));
    assertNull(EventDataConverterV21.findNextSibling(second));
    assertSame(secondReply, EventDataConverterV21.findNextSibling(reply));
    assertNull(EventDataConverterV21.findNextSibling(secondReply));
  }

  private static Conversation makeConversation() {
    IdGenerator idGenerator = FakeIdGenerator.create();
    ObservableWaveView waveView = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    ConversationView convView = WaveBasedConversationView.create(waveView, idGenerator);
    Conversation conversation = convView.createRoot();
    // Force empty document.
    conversation.getRootThread().appendBlip(new DocInitializationBuilder().build());
    return conversation;
  }
}
