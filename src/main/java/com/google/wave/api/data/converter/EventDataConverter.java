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

import com.google.wave.api.BlipData;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Collection;
import java.util.List;

/**
 * A utility that handles the conversion from server side model objects, for
 * example, {@link Wavelet}, {@link ConversationBlip}, and so on, to the
 * API model objects that are serializable to JSON.
 *
 */
public interface EventDataConverter {

  /**
   * Converts a {@link Wavelet} into a serializable {@link WaveletData}
   * object so that it can be serialized into JSON, and sent to the robot.
   *
   * @param wavelet the wavelet to convert.
   * @param conversation the conversation manifest.
   * @param eventMessageBundle the event bundle where this {@link WaveletData}
   *     will be added to.
   * @return the converted {@link WaveletData} object.
   */
  WaveletData toWaveletData(Wavelet wavelet,
      Conversation conversation, EventMessageBundle eventMessageBundle);

  /**
   * Converts a {@link ConversationBlip} into a serializable {@link BlipData}
   * object so that it can be serialized into JSON, and sent to the robot.
   *
   * @param blip the blip to convert.
   * @param wavelet the wavelet that contains the blip.
   * @param eventMessageBundle the event bundle where this {@link BlipData}
   *     will be added to.
   * @return the converted {@link BlipData} object.
   */
  BlipData toBlipData(ConversationBlip blip, Wavelet wavelet,
      EventMessageBundle eventMessageBundle);

  /**
   * Finds the parent of a blip.
   *
   * @param blip the blip.
   * @return the blip's parent, or {@code null} if blip is the first blip in a
   *     conversation.
   */
  ConversationBlip findBlipParent(ConversationBlip blip);

  /**
   * Finds the children of a blip.
   *
   * @param blip the blip.
   * @return a list of the blip's children.
   */
  List<ConversationBlip> findBlipChildren(ConversationBlip blip);

  /**
   * Converts a collection of {@link ParticipantId}s to a list of addresses.
   *
   * @param participantIds the participant ids to convert.
   * @return a list of addresses.
   */
  List<String> idsToParticipantIdList(Collection<ParticipantId> participantIds);

}
