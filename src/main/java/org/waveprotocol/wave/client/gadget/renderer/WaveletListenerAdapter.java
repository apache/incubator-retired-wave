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

package org.waveprotocol.wave.client.gadget.renderer;

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationListenerImpl;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Gadget wavelet listener adapter class to relay participant/contributor event
 * notifications from the wavelet listener interface to the gadget listener.
 *
 */
public class WaveletListenerAdapter extends ConversationListenerImpl {

  private final ConversationBlip myBlip;
  private final GadgetWaveletListener listener;

  /**
   * Constructs the listener adapter.
   *
   * @param blip listener's blip
   * @param listener the listener interface
   */
  public WaveletListenerAdapter(ConversationBlip blip, GadgetWaveletListener listener) {
    this.myBlip = blip;
    this.listener = listener;
  }

  @Override
  public void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId contributor) {
    if (blip == myBlip) {
      listener.onBlipContributorAdded(contributor);
    }
  }

  @Override
  public void onBlipContributorRemoved(ObservableConversationBlip blip, ParticipantId contributor) {
    if (blip == myBlip) {
      listener.onBlipContributorRemoved(contributor);
    }
  }

  @Override
  public void onParticipantAdded(ParticipantId participant) {
    listener.onParticipantAdded(participant);
  }

  @Override
  public void onParticipantRemoved(ParticipantId participant) {
    listener.onParticipantRemoved(participant);
  }
}
