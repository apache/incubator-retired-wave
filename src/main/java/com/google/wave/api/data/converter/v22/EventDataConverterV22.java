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

import com.google.common.collect.Lists;
import com.google.wave.api.BlipData;
import com.google.wave.api.BlipThread;
import com.google.wave.api.data.ApiView;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.v21.EventDataConverterV21;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationBlip.LocatedReplyThread;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.List;

/**
 * An implementation of {@link EventDataConverter} for protocol version v0.22.
 *
 * The previous converter implementation, {@link EventDataConverterV21}, does
 * not expose the proper blip hierarchy, for example, parent blip can be the
 * blip that contains the container thread, or the previous sibling blip. This
 * implementation, however, is purely based on the {@link ConversationThread}.
 *
 */
public class EventDataConverterV22 extends EventDataConverterV21 {

  @Override
  public WaveletData toWaveletData(Wavelet wavelet, Conversation conversation,
      EventMessageBundle eventMessageBundle) {
    WaveletData waveletData = super.toWaveletData(wavelet, conversation,
        eventMessageBundle);
    List<String> blipIds = Lists.newLinkedList();
    for (ConversationBlip conversationBlip : conversation.getRootThread().getBlips()) {
      blipIds.add(conversationBlip.getId());
    }
    waveletData.setRootThread(new BlipThread("", -1 , blipIds, null));
    return waveletData;
  }

  @Override
  public BlipData toBlipData(ConversationBlip blip, Wavelet wavelet,
      EventMessageBundle eventMessageBundle) {
    BlipData blipData = super.toBlipData(blip, wavelet, eventMessageBundle);
    String threadId = blip.getThread().getId();
    blipData.setThreadId(threadId);

    // If it's the root thread, that doesn't have thread id, then skip.
    if (!threadId.isEmpty()) {
      ConversationThread thread = blip.getThread();
      addThread(eventMessageBundle, thread, -1, wavelet);
    }

    // Add the inline reply threads.
    List<String> threadIds = Lists.newLinkedList();
    for (LocatedReplyThread<? extends ConversationThread> thread : blip.locateReplyThreads()) {
      String replyThreadId = thread.getThread().getId();
      threadIds.add(replyThreadId);
      addThread(eventMessageBundle, thread.getThread(), thread.getLocation(), wavelet);
    }

    blipData.setReplyThreadIds(threadIds);
    return blipData;
  }

  /**
   * Finds the children of a blip, defined as all blips in all reply threads.
   *
   * @param blip the blip.
   * @return the children of the given blip.
   */
  @Override
  public List<ConversationBlip> findBlipChildren(ConversationBlip blip) {
    List<ConversationBlip> children = Lists.newArrayList();
    // Add all children from the inline reply threads.
    for (LocatedReplyThread<? extends ConversationThread> thread : blip.locateReplyThreads()) {
      for (ConversationBlip child : thread.getThread().getBlips()) {
        children.add(child);
      }
    }
    return children;
  }

  /**
   * Finds the parent of a blip.
   *
   * @param blip the blip.
   * @return the blip's parent, or {@code null} if the blip is the first blip
   *     in a conversation.
   */
  @Override
  public ConversationBlip findBlipParent(ConversationBlip blip) {
    return blip.getThread().getParentBlip();
  }

  /**
   * Converts a {@link ConversationThread} into API {@link BlipThread}, then add it
   * to the given {@link EventMessageBundle}.
   *
   * @param eventMessageBundle the event message bundle to add the thread to.
   * @param thread the {@link ConversationThread} to convert.
   * @param location the anchor location of the thread, or -1 if it's not an
   *     inline reply thread.
   * @param wavelet the wavelet to which the given thread belongs.
   */
  private static void addThread(EventMessageBundle eventMessageBundle, ConversationThread thread,
      int location, Wavelet wavelet) {
    String threadId = thread.getId();
    if (eventMessageBundle.hasThreadId(threadId)) {
      // The bundle already has the thread, so we don't need to do the
      // conversion.
      return;
    }

    // Convert the XML offset into the text offset.
    ConversationBlip parent = thread.getParentBlip();

    // Locate the thread, if necessary.
    if (location == -1) {
      for (LocatedReplyThread<? extends ConversationThread> inlineReplyThread :
        parent.locateReplyThreads()) {
        if (thread.getId().equals(inlineReplyThread.getThread().getId())) {
          location = inlineReplyThread.getLocation();
          break;
        }
      }     
    }

    // Use ApiView to convert the offset.
    if (location != -1) {
      ApiView apiView = new ApiView(parent.getContent(), wavelet);
      location = apiView.transformToTextOffset(location);
    }

    // Get the ids of the contained blips.
    List<String> blipIds = Lists.newLinkedList();
    for (ConversationBlip blip : thread.getBlips()) {
      blipIds.add(blip.getId());
    }
    eventMessageBundle.addThread(threadId, new BlipThread(thread.getId(), location, blipIds, null));
  }
}
