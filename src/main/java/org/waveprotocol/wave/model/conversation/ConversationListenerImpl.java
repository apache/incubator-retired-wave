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

import org.waveprotocol.wave.model.wave.ParticipantId;



/**
 * Null implementation of a conversation listener.
 *
 * @author anorth@google.com (Alex North)
 */
public class ConversationListenerImpl implements ObservableConversation.Listener {
  @Override
  public void onParticipantAdded(ParticipantId participant) {
  }

  @Override
  public void onParticipantRemoved(ParticipantId participant) {
  }

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
  }

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
  }

  @Override
  public void onInlineThreadAdded(ObservableConversationThread thread, int location) {
  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
  }

  @Override
  public void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId contributor) {
  }

  @Override
  public void onBlipContributorRemoved(ObservableConversationBlip blip, ParticipantId contributor) {
  }

  @Override
  public void onBlipSumbitted(ObservableConversationBlip blip) {
  }

  @Override
  public void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
      long newTimestamp) {
  }
}
