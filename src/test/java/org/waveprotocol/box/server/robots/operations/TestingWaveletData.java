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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.collect.ImmutableList;

import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.List;

/**
 * Builds a wavelet and provides direct access to the various layers of
 * abstraction.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class TestingWaveletData {
  private final ObservableWaveletData waveletData;
  private final ObservableWaveletData userWaveletData;
  private final Conversation conversation;
  private final WaveViewData waveViewData;

  public TestingWaveletData(
      WaveId waveId, WaveletId waveletId, ParticipantId author, boolean isConversational) {
    waveletData =
        new WaveletDataImpl(waveletId, author, 1234567890, 0, HashedVersion.unsigned(0), 0,
            waveId, BasicFactories.observablePluggableMutableDocumentFactory());
    userWaveletData =
        new WaveletDataImpl(WaveletId.of("example.com", "user+foo@example.com"), author,
            1234567890, 0, HashedVersion.unsigned(0), 0,
          waveId, BasicFactories.observablePluggableMutableDocumentFactory());
    
    OpBasedWavelet wavelet =
      new OpBasedWavelet(waveId, waveletData, new BasicWaveletOperationContextFactory(author),
          ParticipationHelper.DEFAULT,
          SilentOperationSink.Executor.<WaveletOperation, WaveletData>build(waveletData),
          SilentOperationSink.VOID);
    ReadOnlyWaveView waveView = new ReadOnlyWaveView(waveId);
    waveView.addWavelet(wavelet);
    
    if (isConversational) {
      ConversationView conversationView = WaveBasedConversationView.create(waveView, FakeIdGenerator.create());
      WaveletBasedConversation.makeWaveletConversational(wavelet);
      conversation = conversationView.getRoot();

      conversation.addParticipant(author);
    } else {
      conversation = null;
    }

    waveViewData = WaveViewDataImpl.create(waveId, ImmutableList.of(waveletData, userWaveletData));
  }

  public void appendBlipWithText(String text) {
    ConversationBlip blip = conversation.getRootThread().appendBlip();
    LineContainers.appendToLastLine(blip.getContent(), XmlStringBuilder.createText(text));
    TitleHelper.maybeFindAndSetImplicitTitle(blip.getContent());
  }

  public List<ObservableWaveletData> copyWaveletData() {
    // This data object already has an op-based owner on top. Must copy it.
    return ImmutableList.of(WaveletDataUtil.copyWavelet(waveletData),
        WaveletDataUtil.copyWavelet(userWaveletData));
  }

  public WaveViewData copyViewData() {
    return WaveViewDataImpl.create(waveViewData.getWaveId(),copyWaveletData());
  }
}
