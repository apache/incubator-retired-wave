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
import com.google.wave.api.BlipThread;
import com.google.wave.api.Context;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationBlip.LocatedReplyThread;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Class to resolve context for {@link EventMessageBundle}.
 *
 */
public class ContextResolver {

  /**
   * Resolve an {@link EventMessageBundle}'s context. Meaning that the
   * {@link WaveletData}, {@link BlipData} and {@link BlipThread} will be
   * resolved as dictated by the {@link EventMessageBundle}'s getRequiredBlips
   * method.
   *
   * @param eventMessageBundle bundle to put the context in
   * @param wavelet {@link Wavelet} to put in the {@link EventMessageBundle}
   * @param conversation {@link Conversation} to put in the
   *        {@link EventMessageBundle}
   * @param eventDataConverter {@link EventDataConverter} used to convert
   *        {@link Wavelet}s and {@link ConversationBlip}s.
   */
  public static void resolveContext(EventMessageBundle eventMessageBundle, Wavelet wavelet,
      Conversation conversation, EventDataConverter eventDataConverter) {
    // TODO(user): Refactor.
    WaveletData waveletData =
        eventDataConverter.toWaveletData(wavelet, conversation, eventMessageBundle);
    eventMessageBundle.setWaveletData(waveletData);
    for (Map.Entry<String, Set<Context>> entry : eventMessageBundle.getRequiredBlips().entrySet()) {
      Set<Context> contextSet = entry.getValue();
      ConversationBlip requiredBlip = conversation.getBlip(entry.getKey());
      if (contextSet.contains(Context.ALL)) {
        ContextResolver.addAllBlipsToEventMessages(
            eventMessageBundle, conversation, wavelet, eventDataConverter);
        // We now have all blips so we're done.
        break;
      }
      if (contextSet.contains(Context.ROOT)) {
        ConversationBlip rootBlip = conversation.getRootThread().getFirstBlip();
        if (rootBlip != requiredBlip) {
          ContextResolver.addBlipToEventMessages(
              eventMessageBundle, rootBlip, wavelet, eventDataConverter);
        }
      }

      // Required blip might be null, for example, in a blip deleted event.
      if (requiredBlip == null) {
        continue;
      }

      ContextResolver.addBlipToEventMessages(
          eventMessageBundle, requiredBlip, wavelet, eventDataConverter);
      if (contextSet.contains(Context.CHILDREN)) {
        for (ConversationBlip child : eventDataConverter.findBlipChildren(requiredBlip)) {
          ContextResolver.addBlipToEventMessages(
              eventMessageBundle, child, wavelet, eventDataConverter);
        }
      }
      ConversationThread containingThread = requiredBlip.getThread();
      if (contextSet.contains(Context.PARENT)) {
        ConversationBlip parent = eventDataConverter.findBlipParent(requiredBlip);
        if (parent != null) {
          ContextResolver.addBlipToEventMessages(
              eventMessageBundle, parent, wavelet, eventDataConverter);
        }
      }
      if (contextSet.contains(Context.SIBLINGS)) {
        for (ConversationBlip blip : containingThread.getBlips()) {
          if (blip != requiredBlip) {
            ContextResolver.addBlipToEventMessages(
                eventMessageBundle, blip, wavelet, eventDataConverter);
          }
        }
      }
    }
  }

  /**
   * Adds a single {@link ConversationBlip} to the {@link EventMessageBundle}.
   *
   * @param eventMessageBundle to add the blip to
   * @param blip {@link ConversationBlip} to add
   * @param wavelet {@link Wavelet} that the blip is based on
   * @param eventDataConverter {@link EventDataConverter} used for conversion
   */
  private static void addBlipToEventMessages(EventMessageBundle eventMessageBundle,
      ConversationBlip blip, Wavelet wavelet, EventDataConverter eventDataConverter) {
    if (blip != null && !eventMessageBundle.hasBlipId(blip.getId())) {
      eventMessageBundle.addBlip(
          blip.getId(), eventDataConverter.toBlipData(blip, wavelet, eventMessageBundle));
    }
  }

  /**
   * Adds all blips in the given conversation to the {@link EventMessageBundle}.
   *
   * @param eventMessageBundle to add the blips to
   * @param conversation {@link Conversation} to get all blips from
   * @param wavelet {@link Wavelet} that the {@link Conversation} is based on
   * @param eventDataConverter {@link EventDataConverter} used for conversion
   */
  public static void addAllBlipsToEventMessages(EventMessageBundle eventMessageBundle,
      Conversation conversation, Wavelet wavelet, EventDataConverter eventDataConverter) {
    Queue<ConversationThread> threads = new LinkedList<ConversationThread>();
    threads.add(conversation.getRootThread());
    while (!threads.isEmpty()) {
      ConversationThread thread = threads.remove();
      for (ConversationBlip blip : thread.getBlips()) {
        addBlipToEventMessages(eventMessageBundle, blip, wavelet, eventDataConverter);
        for (ConversationThread replyThread : blip.getReplyThreads()) {
          threads.add(replyThread);
        }
      }
    }
  }
}
