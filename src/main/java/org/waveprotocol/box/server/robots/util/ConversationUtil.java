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

package org.waveprotocol.box.server.robots.util;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;

/**
 * Utility class for {@link Conversation}s used by the Robot API.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class ConversationUtil {

  private final IdGenerator idGenerator;

  @Inject
  public ConversationUtil(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  /**
   * Builds an {@link ObservableConversationView} for the given wavelet. Note
   * that this can be expensive since the conversation is not garbage collected
   * until the wavelet is.
   *
   * @param wavelet The wavelet to return the conversation for, must be a valid
   *        conversation wavelet.
   * @throws IllegalArgumentException if the wavelet is not a valid conversation
   *         wavelet.
   */
  public ObservableConversationView buildConversation(ObservableWavelet wavelet) {
    Preconditions.checkArgument(IdUtil.isConversationalId(wavelet.getId()),
        "Expected conversational wavelet, got " + wavelet.getId());
    Preconditions.checkArgument(WaveletBasedConversation.waveletHasConversation(wavelet),
        "Conversation can't be build on a wavelet " + wavelet.getId()
            + " without conversation structure");

    ReadOnlyWaveView wv = new ReadOnlyWaveView(wavelet.getWaveId());
    wv.addWavelet(wavelet);

    return WaveBasedConversationView.create(wv, idGenerator);
  }

  /**
   * Generates a {@link WaveletName} for a conversational wavelet.
   */
  public WaveletName generateWaveletName() {
    return WaveletName.of(idGenerator.newWaveId(), idGenerator.newConversationRootWaveletId());
  }

  /**
   * Returns the blip id of the first blip in the root thread.
   *
   * @param conversation the conversation to get the blip id from.
   */
  public static String getRootBlipId(Conversation conversation) {
    ConversationBlip rootBlip = conversation.getRootThread().getFirstBlip();
    return (rootBlip != null) ? rootBlip.getId() : "";
  }
}
