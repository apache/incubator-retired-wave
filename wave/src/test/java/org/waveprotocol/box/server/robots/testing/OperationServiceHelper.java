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

package org.waveprotocol.box.server.robots.testing;

import static org.mockito.Mockito.mock;

import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.v22.EventDataConverterV22;

import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

/**
 * Helper for testing {@link OperationService}. Puts a single empty
 * conversational wavelet with one participant in the operation context.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationServiceHelper {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final DocumentFactory<? extends DocumentOperationSink> DOCUMENT_FACTORY =
      BasicFactories.observablePluggableMutableDocumentFactory();

  private final WaveletProvider waveletProvider;
  private final OperationContextImpl context;

  /**
   * Constructs a new {@link OperationServiceHelper} with a wavelet with the
   * name and participant that are passed in.
   *
   * @param waveletName the name of the empty wavelet to open in the context.
   * @param participant the participant that should be on that empty wavelet.
   */
  public OperationServiceHelper(WaveletName waveletName, ParticipantId participant) {
    waveletProvider = mock(WaveletProvider.class);
    EventDataConverter converter = new EventDataConverterV22();

    ObservableWaveletData waveletData = WaveletDataImpl.Factory.create(DOCUMENT_FACTORY).create(
        new EmptyWaveletSnapshot(waveletName.waveId, waveletName.waveletId, participant,
            HASH_FACTORY.createVersionZero(waveletName), 0L));
    waveletData.addParticipant(participant);

    BasicWaveletOperationContextFactory CONTEXT_FACTORY =
        new BasicWaveletOperationContextFactory(participant);

    SilentOperationSink<WaveletOperation> executor =
        SilentOperationSink.Executor.<WaveletOperation, WaveletData>build(waveletData);
    OpBasedWavelet wavelet =
        new OpBasedWavelet(waveletData.getWaveId(), waveletData, CONTEXT_FACTORY,
            ParticipationHelper.DEFAULT, executor, SilentOperationSink.VOID);

    // Make a conversation with an empty root blip
    WaveletBasedConversation.makeWaveletConversational(wavelet);
    ConversationUtil conversationUtil = new ConversationUtil(FakeIdGenerator.create());
    ObservableConversation conversation = conversationUtil.buildConversation(wavelet).getRoot();
    conversation.getRootThread().appendBlip();

    context = new OperationContextImpl(waveletProvider, converter, conversationUtil);
    context.putWavelet(waveletName.waveId, waveletName.waveletId,
        new RobotWaveletData(waveletData, HASH_FACTORY.createVersionZero(waveletName)));
  }

  /**
   * @return the {@link WaveletProvider} mock
   */
  public WaveletProvider getWaveletProvider() {
    return waveletProvider;
  }

  /**
   * @return the {@link OperationContextImpl} with the empty wavelet opened.
   */
  public OperationContextImpl getContext() {
    return context;
  }
}
