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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.ContextResolver;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.WaveletFetchedEvent;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Map;

/**
 * {@link OperationService} for the "fetchWave" operation.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class FetchWaveService implements OperationService {

  private FetchWaveService() {
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    if (OperationUtil.<Boolean>getOptionalParameter(operation, ParamsProperty.RETURN_WAVELET_IDS, false)) {
      ImmutableSet<WaveletId> waveletIds = context.getVisibleWaveletIds(operation, participant);
      Map<ParamsProperty, Object> data =
          ImmutableMap.<ParamsProperty, Object>of(ParamsProperty.WAVELET_IDS, waveletIds);
      context.constructResponse(operation, data);
    } else {
      OpBasedWavelet wavelet = context.openWavelet(operation, participant);
      ObservableConversation conversation =
          context.openConversation(operation, participant).getRoot();

      EventMessageBundle messages =
          mapWaveletToMessageBundle(context.getConverter(), participant, wavelet, conversation);

      String rootBlipId = ConversationUtil.getRootBlipId(conversation);
      String message = OperationUtil.getOptionalParameter(operation, ParamsProperty.MESSAGE);

      WaveletFetchedEvent event =
          new WaveletFetchedEvent(null, null, participant.getAddress(), System.currentTimeMillis(),
          message, rootBlipId, messages.getWaveletData(), messages.getBlipData(),
          messages.getThreads());

      context.processEvent(operation, event);
    }
  }

  /**
   * Maps a wavelet and its conversation to a new {@link EventMessageBundle}.
   *
   * @param converter to convert to API objects.
   * @param participant the participant who the bundle is for.
   * @param wavelet the wavelet to put in the bundle.
   * @param conversation the conversation to put in the bundle.
   */
  private EventMessageBundle mapWaveletToMessageBundle(EventDataConverter converter,
      ParticipantId participant, Wavelet wavelet, Conversation conversation) {
    EventMessageBundle messages = new EventMessageBundle(participant.getAddress(), "");
    WaveletData waveletData = converter.toWaveletData(wavelet, conversation, messages);
    messages.setWaveletData(waveletData);
    ContextResolver.addAllBlipsToEventMessages(messages, conversation, wavelet, converter);
    return messages;
  }

  public static FetchWaveService create() {
    return new FetchWaveService();
  }
}