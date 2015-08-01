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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer.PagingHandler;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.Conversation.Anchor;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Renderer the conversation update.
 *
 */
public class LiveConversationViewRenderer
    implements ObservableConversationView.Listener, PagingHandler {

  private class LiveConversationRenderer implements ObservableConversation.Listener,
      ObservableConversation.AnchorListener, PagingHandler {
    private final ObservableConversation conversation;
    private final LiveProfileRenderer profileRenderer;

    LiveConversationRenderer(
        ObservableConversation conversation, LiveProfileRenderer profileRenderer) {
      this.conversation = conversation;
      this.profileRenderer = profileRenderer;
    }

    private LiveConversationRenderer init() {
      profileRenderer.init();
      // Note: blip contributions are only monitored once a blip is paged in.
      for (ParticipantId participant : conversation.getParticipantIds()) {
        profileRenderer.monitorParticipation(conversation, participant);
      }
      conversation.addListener((ObservableConversation.AnchorListener) this);
      conversation.addListener((ObservableConversation.Listener) this);
      return this;
    }

    public void destroy() {
      conversation.removeListener((ObservableConversation.AnchorListener) this);
      conversation.removeListener((ObservableConversation.Listener) this);
      profileRenderer.destroy();
    }

    @Override
    public void onParticipantAdded(ParticipantId participant) {
      ParticipantsView participantUi = views.getParticipantsView(conversation);
      // Note: this does not insert the participant in the correct order.
      participantUi.appendParticipant(conversation, participant);
      profileRenderer.monitorParticipation(conversation, participant);
    }

    @Override
    public void onParticipantRemoved(ParticipantId participant) {
      ParticipantView participantUi = views.getParticipantView(conversation, participant);
      if (participantUi != null) {
        participantUi.remove();
      }
      profileRenderer.unmonitorParticipation(conversation, participant);
    }

    @Override
    public void onThreadAdded(ObservableConversationThread thread) {
      ObservableConversationBlip parentBlip = thread.getParentBlip();
      BlipView blipView = views.getBlipView(parentBlip);

      if (blipView != null) {
        ConversationThread next = findBefore(thread, parentBlip.getReplyThreads());
        replyHandler.presentAfter(blipView, next, thread);
      } else {
        throw new IllegalStateException("blipView not present");
      }
    }

    @Override
    public void onInlineThreadAdded(ObservableConversationThread thread, int location) {
      // inline threads are ignored for now.
    }

    @Override
    public void onThreadDeleted(ObservableConversationThread thread) {
      InlineThreadView threadView = views.getInlineThreadView(thread);
      if (threadView != null) {
        threadView.remove();
      }
    }

    @Override
    public void onBlipAdded(ObservableConversationBlip blip) {
      ConversationThread parentThread = blip.getThread();
      ThreadView threadView = viewOf(parentThread);
      if (threadView != null) {
        ConversationBlip ref = findBefore(blip, parentThread.getBlips());
        BlipView refView = viewOf(ref);

        // Render the new blip.
        threadView.insertBlipAfter(refView, blip);
        bubbleBlipCountUpdate(blip);
      } else {
        throw new IllegalStateException("threadView not present");
      }
    }

    @Override
    public void onBlipDeleted(ObservableConversationBlip blip) {
      BlipView blipView = views.getBlipView(blip);
      if (blipView != null) {
        // TODO(user): Hide parent thread if it becomes empty.
        blipView.remove();
      }
      for (ParticipantId contributor : blip.getContributorIds()) {
        profileRenderer.unmonitorContribution(blip, contributor);
      }
      bubbleBlipCountUpdate(blip);
    }

    private void bubbleBlipCountUpdate(ConversationBlip blip) {
      ConversationThread thread = blip.getThread();
      ThreadView threadUi = viewOf(thread);
      threadUi.setTotalBlipCount(readMonitor.getTotalCount(thread));
      ConversationBlip parentBlip = thread.getParentBlip();
      if (parentBlip != null) {
        bubbleBlipCountUpdate(parentBlip);
      }
    }

    @Override
    public void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId contributor) {
      profileRenderer.monitorContribution(blip, contributor);
    }

    @Override
    public void onBlipContributorRemoved(
        ObservableConversationBlip blip, ParticipantId contributor) {
      profileRenderer.unmonitorContribution(blip, contributor);
    }

    @Override
    public void onBlipSumbitted(ObservableConversationBlip blip) {
    }

    @Override
    public void onBlipTimestampChanged(
        ObservableConversationBlip blip, long oldTimestamp, long newTimestamp) {
      BlipView blipUi = views.getBlipView(blip);
      BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;
      if (metaUi != null) {
        blipRenderer.renderTime(blip, metaUi);
      }
    }

    @Override
    public void pageIn(ConversationBlip blip) {
      // listen to the contributors on the blip
      for (ParticipantId contributor : blip.getContributorIds()) {
        profileRenderer.monitorContribution(blip, contributor);
      }
    }

    @Override
    public void pageOut(ConversationBlip blip) {
      for (ParticipantId contributor : blip.getContributorIds()) {
        profileRenderer.unmonitorContribution(blip, contributor);
      }
    }

    @Override
    public void onAnchorChanged(Anchor oldAnchor, Anchor newAnchor) {
      // Since anchors are application-level immutable, this is a rare case, so
      // the gain in simplicity of implementing it as removal then addition
      // outweighs the efficiency gain from implementing a
      // conversation-view-move mechanism.
      if (oldAnchor != null) {
        // Remove old view.
        ConversationView oldUi = viewOf(conversation);
        if (oldUi != null) {
          oldUi.remove();
        }
      }
      if (newAnchor != null) {
        // Insert new view.
        BlipView containerUi = viewOf(newAnchor.getBlip());
        if (containerUi != null) {
          ConversationView convUi = containerUi.insertConversationBefore(null, conversation);
        }
      }
    }

    /**
     * Finds the predecessor of an item in an iterable. This method runs in
     * linear time.
     */
    private <T> T findBefore(T o, Iterable<? extends T> xs) {
      T last = null;
      for (T x : xs) {
        if (x.equals(o)) {
          return last;
        }
        last = x;
      }
      throw new IllegalArgumentException("Item " + o + " not found in " + xs);
    }
  }

  private final TimerService timer;
  private final ObservableConversationView wave;
  private final ModelAsViewProvider views;
  private final ShallowBlipRenderer blipRenderer;
  private final ReplyManager replyHandler;
  private final ThreadReadStateMonitor readMonitor;
  private final ProfileManager profiles;
  private final LiveSupplementRenderer supplementRenderer;
  private final IdentityMap<Conversation, LiveConversationRenderer> conversationRenderers =
      CollectionUtils.createIdentityMap();

  LiveConversationViewRenderer(TimerService timer, ObservableConversationView wave,
      ModelAsViewProvider views, ShallowBlipRenderer blipRenderer, ReplyManager replyHandler,
      ThreadReadStateMonitor readMonitor, ProfileManager profiles,
      LiveSupplementRenderer supplementRenderer) {
    this.timer = timer;
    this.wave = wave;
    this.views = views;
    this.blipRenderer = blipRenderer;
    this.replyHandler = replyHandler;
    this.readMonitor = readMonitor;
    this.profiles = profiles;
    this.supplementRenderer = supplementRenderer;
  }

  /**
   * Creates a live renderer for a wave. The renderer will start incremental
   * updates of an existing rendering once it is {@link #init initialized}.
   */
  public static LiveConversationViewRenderer create(TimerService timer,
      ObservableConversationView wave, ModelAsViewProvider views, ShallowBlipRenderer blipRenderer,
      ReplyManager replyHandler, ThreadReadStateMonitor readMonitor, ProfileManager profiles,
      ObservableSupplementedWave supplement) {
    LiveSupplementRenderer supplementRenderer =
        LiveSupplementRenderer.create(supplement, views, readMonitor);
    return new LiveConversationViewRenderer(
        timer, wave, views, blipRenderer, replyHandler, readMonitor, profiles, supplementRenderer);
  }

  /**
   * Observes the conversations to which this renderer is bound, updating their
   * renderings as the conversation changes.
   */
  public void init() {
    supplementRenderer.init();
    for (ObservableConversation conv : wave.getConversations()) {
      observe(conv);
    }

    wave.addListener(this);
  }

  /**
   * Destroys this renderer, releasing its resources. It is no longer usable
   * after a call to this method.
   */
  public void destroy() {
    wave.removeListener(this);
    conversationRenderers.each(new ProcV<Conversation, LiveConversationRenderer>() {
      @Override
      public void apply(Conversation _, LiveConversationRenderer value) {
        value.destroy();
      }
    });
    supplementRenderer.destroy();
  }

  /**
   * Observes a conversation, updating its view as it changes.
   *
   * @param conversation conversation to observe
   */
  private void observe(ObservableConversation conversation) {
    LiveProfileRenderer profileRenderer =
        LiveProfileRenderer.create(timer, profiles, views, blipRenderer);
    LiveConversationRenderer renderer = new LiveConversationRenderer(conversation, profileRenderer);
    renderer.init();
    conversationRenderers.put(conversation, renderer);
  }

  /**
   * Stops observing a conversation, releasing any resources that were used to
   * observe it.
   *
   * @param conversation conversation to stop observing
   */
  private void unobserve(ObservableConversation conversation) {
    LiveConversationRenderer renderer = conversationRenderers.get(conversation);
    if (renderer != null) {
      conversationRenderers.remove(conversation);
      renderer.destroy();
    }
  }

  private ThreadView viewOf(ConversationThread thread) {
    return thread == null ? null // \u2620
        : (thread.getConversation().getRootThread() == thread) // \u2620
            ? views.getRootThreadView(thread) // \u2620
            : views.getInlineThreadView(thread);
  }

  private BlipView viewOf(ConversationBlip ref) {
    return ref == null ? null : views.getBlipView(ref);
  }

  private ConversationView viewOf(Conversation ref) {
    return ref == null ? null : views.getConversationView(ref);
  }

  @Override
  public void pageIn(ConversationBlip blip) {
    LiveConversationRenderer renderer = conversationRenderers.get(blip.getConversation());
    Preconditions.checkState(renderer != null);
    renderer.pageIn(blip);
  }

  @Override
  public void pageOut(ConversationBlip blip) {
    LiveConversationRenderer renderer = conversationRenderers.get(blip.getConversation());
    Preconditions.checkState(renderer != null);
    renderer.pageOut(blip);
  }

  //
  // Note: the live maintenance of nested conversations is not completely
  // correct, because the conversation model does not broadcast correct and
  // consistent events. The rendering is only as correct as the model events,
  // and it is not considered to be worthwhile for the rendering to generate the
  // correct events manually rather than wait for the model events to be fixed.
  //
  // Additionally, the conversation model does not expose the conversations
  // anchored at a particular blip, which makes a stable sibling ordering of
  // conversations infeasible.
  //

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    BlipView container = viewOf(conversation.getAnchor().getBlip());
    if (container != null) {
      ConversationView conversationUi = container.insertConversationBefore(null, conversation);
    }

    observe(conversation);
  }

  @Override
  public void onConversationRemoved(ObservableConversation conversation) {
    unobserve(conversation);

    ConversationView convUi = viewOf(conversation);
    if (convUi != null) {
      convUi.remove();
    }
  }
}
