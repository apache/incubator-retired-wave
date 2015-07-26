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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.waveprotocol.wave.client.common.util.LinkedSequence;
import org.waveprotocol.wave.client.render.ReductionBasedRenderer;
import org.waveprotocol.wave.client.render.RenderingRules;
import org.waveprotocol.wave.client.render.WaveRenderer;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;
import org.waveprotocol.wave.model.util.IdentityMap.Reduce;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A wave renderer that renders waves into fake view objects.
 */
public final class FakeRenderer implements WaveRenderer<View>, ModelAsViewProvider {

  /** Factory and registry of fake views. */
  class ViewStore {
    final BiMap<ConversationBlip, FakeBlipView> blipUis = HashBiMap.create();
    final BiMap<Conversation, FakeConversationView> convUis = HashBiMap.create();
    final BiMap<ConversationThread, FakeRootThreadView> rootThreadUis = HashBiMap.create();
    final BiMap<ConversationThread, FakeInlineThreadView> inlineThreadUis = HashBiMap.create();
    final BiMap<ConversationThread, FakeAnchor> defaultAnchorUis = HashBiMap.create();
    final BiMap<ConversationThread, FakeAnchor> inlineAnchorUis = HashBiMap.create();

    /** Puts a value in a map and returns it. */
    private <K, V> V put(Map<? super K, ? super V> map, K key, V value) {
      map.put(key, value);
      return value;
    }

    FakeBlipView createBlipView(ConversationBlip blip, LinkedSequence<FakeAnchor> anchors,
        LinkedSequence<FakeInlineConversationView> convos) {
      return put(blipUis, blip, new FakeBlipView(FakeRenderer.this, anchors, convos));
    }

    FakeConversationView createTopConversationView(Conversation conv, FakeRootThreadView thread) {
      return put(convUis, conv, new FakeTopConversationView(thread));
    }

    FakeConversationView createInlineConversationView(
        Conversation conv, FakeRootThreadView thread) {
      return put(convUis, conv, new FakeInlineConversationView(thread));
    }

    FakeThreadView createRootThreadView(
        ConversationThread thread, LinkedSequence<FakeBlipView> blipUis) {
      return put(rootThreadUis, thread, new FakeRootThreadView(FakeRenderer.this, blipUis));
    }

    FakeThreadView createInlineThreadView(
        ConversationThread thread, LinkedSequence<FakeBlipView> blipUis) {
      return put(inlineThreadUis, thread, new FakeInlineThreadView(FakeRenderer.this, blipUis));
    }

    FakeAnchor createDefaultAnchorView(ConversationThread thread) {
      return put(defaultAnchorUis, thread, new FakeAnchor());
    }

    FakeAnchor createInlineAnchorView(ConversationThread thread) {
      return put(inlineAnchorUis, thread, new FakeAnchor());
    }
  }

  class Rules implements RenderingRules<View> {

    @Override
    public View render(ConversationBlip blip, IdentityMap<ConversationThread, View> replies) {
      return new FakeDocumentView(blip.getContent().toXmlString());
    }

    @Override
    public FakeBlipView render(ConversationBlip blip, View document,
        IdentityMap<ConversationThread, View> defaultAnchors,
        IdentityMap<Conversation, View> nestedReplies) {
      LinkedSequence<FakeAnchor> anchorsUi = LinkedSequence.create();
      for (ConversationThread reply : blip.getReplyThreads()) {
        anchorsUi.append((FakeAnchor) defaultAnchors.get(reply));
      }
      LinkedSequence<FakeInlineConversationView> nestedUis = LinkedSequence.create();
      // Order by conversation id. Ideally, the sort key would be creation
      // time, but that is not exposed in the conversation API.
      final List<Conversation> ordered = CollectionUtils.newArrayList();
      nestedReplies.each(new ProcV<Conversation, View>() {
        @Override
        public void apply(Conversation conv, View ui) {
          ordered.add(conv);
        }
      });
      Collections.sort(ordered, new Comparator<Conversation>() {
        @Override
        public int compare(Conversation o1, Conversation o2) {
          return o1.getId().compareTo(o2.getId());
        }
      });
      for (Conversation nested : ordered) {
        nestedUis.append((FakeInlineConversationView) nestedReplies.get(nested));
      }
      FakeBlipView blipUi = views.createBlipView(blip, anchorsUi, nestedUis);
      blipUi.getMeta().setContent((FakeDocumentView) document);
      return blipUi;
    }

    @Override
    public FakeThreadView render(
        ConversationThread thread, IdentityMap<ConversationBlip, View> blips) {
      LinkedSequence<FakeBlipView> blipUis = LinkedSequence.create();
      for (ConversationBlip blip : thread.getBlips()) {
        blipUis.append((FakeBlipView) blips.get(blip));
      }
      if (thread.getConversation().getRootThread().equals(thread)) {
        return views.createRootThreadView(thread, blipUis);
      } else {
        return views.createInlineThreadView(thread, blipUis);
      }
    }

    @Override
    public FakeConversationView render(Conversation conversation, View participants, View thread) {
      if (!conversation.hasAnchor()) {
        return views.createTopConversationView(conversation, (FakeRootThreadView) thread);
      } else {
        return views.createInlineConversationView(conversation, (FakeRootThreadView) thread);
      }
    }

    @Override
    public View render(Conversation conversation, ParticipantId participant) {
      // Ignore participants; not yet exercised by tests.
      return null;
    }

    @Override
    public View render(Conversation conversation, StringMap<View> participants) {
      // Ignore participants; not yet exercised by tests.
      return null;
    }

    @Override
    public TopConversationView render(
        ConversationView wave, IdentityMap<Conversation, View> conversations) {
      // Pick the first one.
      return conversations.isEmpty() ? null :
        conversations.reduce(null, new Reduce<Conversation, View, TopConversationView>() {
          @Override
          public TopConversationView apply(TopConversationView soFar, Conversation key, View item) {
            return soFar != null ? soFar : (TopConversationView) item;
          }
        });
    }

    @Override
    public FakeAnchor render(ConversationThread thread, View threadUi) {
      FakeAnchor anchor = views.createDefaultAnchorView(thread);
      anchor.attach((InlineThreadView) threadUi);
      return anchor;
    }

  }

  private final ViewStore views = new ViewStore();
  private final WaveRenderer<View> renderer;

  private FakeRenderer(ConversationView wave) {
    this.renderer = ReductionBasedRenderer.of(new Rules(), wave);
  }

  /**
   * Creates a renderer of fake views.
   */
  public static FakeRenderer create(ConversationView wave) {
    return new FakeRenderer(wave);
  }

  // TODO: Expose view store, so that fake views can remove themselves after
  // destruction, so that view lookups below do not report spurious results.
  // This code path is unique to this fake renderer, because in a DOM renderer,
  // cleanup occurs implicitly by virtue of lookups being based on
  // Document.getElementById().

  public FakeAnchor createInlineAnchor(ConversationThread thread) {
    return views.createInlineAnchorView(thread);
  }

  // Delegate wave-rendering to the internal driver.

  @Override
  public View render(Conversation conversation, ParticipantId participant) {
    return renderer.render(conversation, participant);
  }

  @Override
  public View render(Conversation conversation) {
    return renderer.render(conversation);
  }

  @Override
  public View render(ConversationBlip blip) {
    return renderer.render(blip);
  }

  @Override
  public View render(ConversationThread thread) {
    return renderer.render(thread);
  }

  @Override
  public View render(ConversationView wave) {
    return renderer.render(wave);
  }

  // Delegate view lookup to view store.

  @Override
  public BlipView getBlipView(ConversationBlip blip) {
    return views.blipUis.get(blip);
  }

  @Override
  public InlineThreadView getInlineThreadView(ConversationThread thread) {
    return views.inlineThreadUis.get(thread);
  }

  @Override
  public RootThreadView getRootThreadView(ConversationThread thread) {
    return views.rootThreadUis.get(thread);
  }

  @Override
  public org.waveprotocol.wave.client.wavepanel.view.ConversationView getConversationView(
      Conversation conv) {
    return views.convUis.get(conv);
  }

  @Override
  public BlipMetaView getBlipMetaView(ConversationBlip blip) {
    BlipView blipUi = getBlipView(blip);
    return blipUi != null ? blipUi.getMeta() : null;
  }

  @Override
  public AnchorView getDefaultAnchor(ConversationThread thread) {
    return views.defaultAnchorUis.get(thread);
  }

  @Override
  public AnchorView getInlineAnchor(ConversationThread thread) {
    return views.inlineAnchorUis.get(thread);
  }

  @Override
  public ParticipantsView getParticipantsView(Conversation conv) {
    // Participant views not supported.
    return null;
  }

  @Override
  public ParticipantView getParticipantView(Conversation conv, ParticipantId source) {
    return null;
  }

  // Inverse lookup.

  @Override
  public ConversationBlip getBlip(BlipView blipUi) {
    return views.blipUis.inverse().get(blipUi);
  }

  @Override
  public ConversationThread getThread(ThreadView threadUi) {
    ConversationThread inline = views.inlineThreadUis.inverse().get(threadUi);
    return inline != null ? inline : views.rootThreadUis.inverse().get(threadUi);
  }

  @Override
  public Pair<Conversation, ParticipantId> getParticipant(ParticipantView participantUi) {
    return null;
  }

  @Override
  public Conversation getParticipants(ParticipantsView participantsUi) {
    return null;
  }
}
