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

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.render.RenderingRules;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.uibuilder.HtmlClosure;
import org.waveprotocol.wave.client.uibuilder.HtmlClosureCollection;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.AnchorViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipMetaViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ContinuationIndicatorViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.InlineThreadViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ParticipantAvatarViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ParticipantsViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ReplyBoxViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.RootThreadViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ViewFactory;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;
import org.waveprotocol.wave.model.util.IdentityMap.Reduce;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Renders conversational objects with UiBuilders.
 *
 */
public final class FullDomRenderer implements RenderingRules<UiBuilder> {

  public interface DocRefRenderer {
    UiBuilder render(ConversationBlip blip,
        IdentityMap<ConversationThread, UiBuilder> replies);

    DocRefRenderer EMPTY = new DocRefRenderer() {
      @Override
      public UiBuilder render(ConversationBlip blip,
          IdentityMap<ConversationThread, UiBuilder> replies) {
        return UiBuilder.Constant.of(EscapeUtils.fromSafeConstant("<div></div>"));
      }
    };
  }

  public interface ParticipantsRenderer {
    UiBuilder render(Conversation c);

    ParticipantsRenderer EMPTY = new ParticipantsRenderer() {
      @Override
      public UiBuilder render(Conversation c) {
        return UiBuilder.Constant.of(EscapeUtils.fromSafeConstant("<div></div>"));
      }
    };
  }

  private final ShallowBlipRenderer blipPopulator;
  private final DocRefRenderer docRenderer;
  private final ViewIdMapper viewIdMapper;
  private final ViewFactory viewFactory;
  private final ProfileManager profileManager;
  private final ThreadReadStateMonitor readMonitor;

  public FullDomRenderer(ShallowBlipRenderer blipPopulator, DocRefRenderer docRenderer,
      ProfileManager profileManager, ViewIdMapper viewIdMapper, ViewFactory viewFactory,
      ThreadReadStateMonitor readMonitor) {
    this.blipPopulator = blipPopulator;
    this.docRenderer = docRenderer;
    this.profileManager = profileManager;
    this.viewIdMapper = viewIdMapper;
    this.viewFactory = viewFactory;
    this.readMonitor = readMonitor;
  }

  @Override
  public UiBuilder render(ConversationView wave,
      IdentityMap<Conversation, UiBuilder> conversations) {
    // return the first conversation in the view.
    // TODO(hearnden): select the 'best' conversation.
    return conversations.isEmpty() ? null : getFirstConversation(conversations);
  }

  public UiBuilder getFirstConversation(IdentityMap<Conversation, UiBuilder> conversations) {
    return conversations.reduce(null, new Reduce<Conversation, UiBuilder, UiBuilder>() {
      @Override
      public UiBuilder apply(UiBuilder soFar, Conversation key, UiBuilder item) {
        // Pick the first rendering (any will do).
        return soFar == null ? item : soFar;
      }
    });
  }

  @Override
  public UiBuilder render(Conversation conversation, UiBuilder participantsUi, UiBuilder threadUi) {
    String id = viewIdMapper.conversationOf(conversation);
    boolean isTop = !conversation.hasAnchor();
    return isTop ? viewFactory.createTopConversationView(id, threadUi, participantsUi)
        : viewFactory.createInlineConversationView(id, threadUi, participantsUi);
  }

  @Override
  public UiBuilder render(Conversation conversation, StringMap<UiBuilder> participantUis) {
    HtmlClosureCollection participantsUi = new HtmlClosureCollection();
    for (ParticipantId participant : conversation.getParticipantIds()) {
      participantsUi.add(participantUis.get(participant.getAddress()));
    }
    String id = viewIdMapper.participantsOf(conversation);
    return ParticipantsViewBuilder.create(id, participantsUi);
  }

  @Override
  public UiBuilder render(Conversation conversation, ParticipantId participant) {
    Profile profile = profileManager.getProfile(participant);
    String id = viewIdMapper.participantOf(conversation, participant);
    ParticipantAvatarViewBuilder participantUi = ParticipantAvatarViewBuilder.create(id);
    participantUi.setAvatar(profile.getImageUrl());
    participantUi.setName(profile.getFullName());
    return participantUi;
  }

  @Override
  public UiBuilder render(final ConversationThread thread,
      final IdentityMap<ConversationBlip, UiBuilder> blipUis) {
    HtmlClosure blipsUi = new HtmlClosure() {
      @Override
      public void outputHtml(SafeHtmlBuilder out) {
        for (ConversationBlip blip : thread.getBlips()) {
          UiBuilder blipUi = blipUis.get(blip);
          // Not all blips are rendered.
          if (blipUi != null) {
            blipUi.outputHtml(out);
          }
        }
      }
    };
    String threadId = viewIdMapper.threadOf(thread);
    String replyIndicatorId = viewIdMapper.replyIndicatorOf(thread);
    UiBuilder builder = null;
    if (thread.getConversation().getRootThread() == thread) {
      ReplyBoxViewBuilder replyBoxBuilder =
          ReplyBoxViewBuilder.create(replyIndicatorId);
      builder = RootThreadViewBuilder.create(threadId, blipsUi, replyBoxBuilder);
    } else {
      ContinuationIndicatorViewBuilder indicatorBuilder = ContinuationIndicatorViewBuilder.create(
          replyIndicatorId);
      InlineThreadViewBuilder inlineBuilder =
          InlineThreadViewBuilder.create(threadId, blipsUi, indicatorBuilder);
      int read = readMonitor.getReadCount(thread);
      int unread = readMonitor.getUnreadCount(thread);
      inlineBuilder.setTotalBlipCount(read + unread);
      inlineBuilder.setUnreadBlipCount(unread);
      builder = inlineBuilder;
    }
    return builder;
  }

  @Override
  public UiBuilder render(final ConversationBlip blip, UiBuilder document,
      final IdentityMap<ConversationThread, UiBuilder> anchorUis,
      final IdentityMap<Conversation, UiBuilder> nestedConversations) {
    UiBuilder threadsUi = new UiBuilder() {
      @Override
      public void outputHtml(SafeHtmlBuilder out) {
        for (ConversationThread thread : blip.getReplyThreads()) {
          anchorUis.get(thread).outputHtml(out);
        }
      }
    };

    UiBuilder convsUi = new UiBuilder() {
      @Override
      public void outputHtml(SafeHtmlBuilder out) {
        // Order by conversation id. Ideally, the sort key would be creation
        // time, but that is not exposed in the conversation API.
        final List<Conversation> ordered = CollectionUtils.newArrayList();
        nestedConversations.each(new ProcV<Conversation, UiBuilder>() {
          @Override
          public void apply(Conversation conv, UiBuilder ui) {
            ordered.add(conv);
          }
        });
        Collections.sort(ordered, new Comparator<Conversation>() {
          @Override
          public int compare(Conversation o1, Conversation o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });
        List<UiBuilder> orderedUis = CollectionUtils.newArrayList();
        for (Conversation conv : ordered) {
          nestedConversations.get(conv).outputHtml(out);
        }
      }
    };

    BlipMetaViewBuilder metaUi = BlipMetaViewBuilder.create(viewIdMapper.metaOf(blip), document);
    blipPopulator.render(blip, metaUi);

    return BlipViewBuilder.create(viewIdMapper.blipOf(blip), metaUi, threadsUi, convsUi);
  }

  /**
   */
  @Override
  public UiBuilder render(
      ConversationBlip blip, IdentityMap<ConversationThread, UiBuilder> replies) {
    return docRenderer.render(blip, replies);
  }

  @Override
  public UiBuilder render(ConversationThread thread, UiBuilder threadR) {
    String id = EscapeUtils.htmlEscape(viewIdMapper.defaultAnchorOf(thread));
    return AnchorViewBuilder.create(id, threadR);
  }
}
