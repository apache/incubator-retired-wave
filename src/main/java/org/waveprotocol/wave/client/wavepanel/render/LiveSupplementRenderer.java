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

package org.waveprotocol.wave.client.wavepanel.render;

import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc;

/**
 * Listens to supplement updates and update the read state of blips.
 *
 */
public final class LiveSupplementRenderer extends ObservableSupplementedWave.ListenerImpl implements
    ThreadReadStateMonitor.Listener {
  private final ModelAsViewProvider views;
  private final ObservableSupplementedWave supplement;
  private final ThreadReadStateMonitor readMonitor;

  LiveSupplementRenderer(ObservableSupplementedWave supplement, ModelAsViewProvider views,
      ThreadReadStateMonitor readMonitor) {
    this.supplement = supplement;
    this.views = views;
    this.readMonitor = readMonitor;
    readMonitor.addListener(this);
  }

  public static LiveSupplementRenderer create(ObservableSupplementedWave supplement,
      ModelAsViewProvider views, ThreadReadStateMonitor readMonitor) {
    return new LiveSupplementRenderer(supplement, views, readMonitor);
  }

  void init() {
    supplement.addListener(this);
  }

  void destroy() {
    supplement.removeListener(this);
  }

  @Override
  public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
    BlipView blipUi = views.getBlipView(blip);
    BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;

    if (metaUi != null) {
      metaUi.setRead(!supplement.isUnread(blip));
    }
  }

  @Override
  public void onReadStateChanged(IdentitySet<ConversationThread> threads) {
    threads.each(new Proc<ConversationThread>() {
      @Override
      public void apply(ConversationThread thread) {
        ThreadView threadUi = null;
        
        // NOTE: The isInline function had meaning when we had non-inline reply threads.  We
        // don't have them at this point.  Right now if a thread has a parent blip, then it
        // is an inline reply.  If not it must be a root thread.  Since the 'inline' attribute
        // doesn't have official meaning we can't rely on it at this point.  It should probably
        // be removed from the API, since it still exists, this note was left to explain why
        // it is not being used here.
        
        // if (thread.isInline()) {
        if (thread.getParentBlip() != null) {
          threadUi = views.getInlineThreadView(thread);
        } else {
          threadUi = views.getRootThreadView(thread);
        }
        threadUi.setUnreadBlipCount(readMonitor.getUnreadCount(thread));
      }
    });
  }
}
