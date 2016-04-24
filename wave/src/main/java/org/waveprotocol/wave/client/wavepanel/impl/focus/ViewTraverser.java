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

package org.waveprotocol.wave.client.wavepanel.impl.focus;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Traverses the wave panel's view structure, defining the blip ordering for
 * focus movement.
 *
 */
public final class ViewTraverser {

  private boolean skipCollapsedContent;

  public void skipCollapsedContent() {
    this.skipCollapsedContent = true;
  }

  public void includeCollapsedContent() {
    this.skipCollapsedContent = false;
  }

  /** @return the input view, but after maybe filtering collapsed content. */
  private boolean skip(InlineConversationView view) {
    return skipCollapsedContent && view.isCollapsed();
  }

  /** @return the input view, but after maybe filtering collapsed content. */
  private boolean skip(InlineThreadView view) {
    return skipCollapsedContent && view.isCollapsed();
  }

  public BlipView getFirst(ConversationView v) {
    return getNextPre(v, v.getRootThread());
  }

  public BlipView getNext(BlipView blip) {
    BlipMetaView meta = blip.getMeta();
    return meta != null ? getNextPre(blip, meta) : getNextPost(blip, meta);
  }

  public BlipView getLast(ConversationView v) {
    return getPrevPre(v, v.getRootThread());
  }

  public BlipView getPrevious(BlipView b) {
    return getPrevPost(b.getParent(), b);
  }

  //
  // Next Pre.
  //

  private BlipView getNextPre(ConversationView parent, RootThreadView thread) {
    BlipView first = thread.getBlipAfter(null);
    return first != null ? getNextPre(thread, first) : getNextPost(parent, thread);
  }

  private BlipView getNextPre(ThreadView parent, BlipView blip) {
    return blip;
  }

  private BlipView getNextPre(BlipView parent, BlipMetaView meta) {
    AnchorView first = meta.getInlineAnchorAfter(null);
    return first != null ? getNextPre(meta, first) : getNextPost(parent, meta);
  }

  private BlipView getNextPre(BlipMetaView parent, AnchorView anchor) {
    InlineThreadView contents = anchor.getThread();
    return contents != null ? getNextPre(anchor, contents) : getNextPost(parent, anchor);
  }

  private BlipView getNextPre(AnchorView parent, InlineThreadView thread) {
    BlipView first = skip(thread) ? null : thread.getBlipAfter(null);
    return first != null ? getNextPre(thread, first) : getNextPost(parent, thread);
  }

  private BlipView getNextPre(BlipView parent, AnchorView anchor) {
    InlineThreadView contents = anchor.getThread();
    return contents != null ? getNextPre(anchor, contents) : getNextPost(parent, anchor);
  }

  private BlipView getNextPre(BlipView parent, InlineConversationView conversation) {
    RootThreadView root = skip(conversation) ? null : conversation.getRootThread();
    return root != null ? getNextPre(conversation, root) : getNextPost(parent, conversation);
  }

  //
  // Next Post.
  //

  private BlipView getNextPost(View parent, View child) {
    if (parent == null) {
      return null;
    }
    switch (parent.getType()) {
      case ANCHOR:
        return getNextPost((AnchorView) parent, (InlineThreadView) child);
      case META:
        return getNextPost((BlipMetaView) parent, (AnchorView) child);
      case BLIP:
        switch (child.getType()) {
          case META:
            return getNextPost((BlipView) parent, (BlipMetaView) child);
          case ANCHOR:
            return getNextPost((BlipView) parent, (AnchorView) child);
          case INLINE_CONVERSATION:
            return getNextPost((BlipView) parent, (InlineConversationView) child);
          default:
            throw new RuntimeException("unknown blip child type: " + child.getType());
        }
      case INLINE_THREAD:
        return getNextPost((InlineThreadView) parent, (BlipView) child);
      case ROOT_THREAD:
        return getNextPost((RootThreadView) parent, (BlipView) child);
      case ROOT_CONVERSATION:
        return getNextPost((TopConversationView) parent, (RootThreadView) child);
      case INLINE_CONVERSATION:
        return getNextPost((InlineConversationView) parent, (RootThreadView) child);
      default:
        throw new RuntimeException("unknown container type: " + parent.getType());
    }
  }

  private BlipView getNextPost(InlineConversationView parent, RootThreadView child) {
    return getNextPost(parent.getParent(), parent);
  }

  private BlipView getNextPost(TopConversationView parent, RootThreadView child) {
    return null;
  }

  private BlipView getNextPost(ThreadView parent, BlipView child) {
    BlipView next = parent.getBlipAfter(child);
    return next != null ? getNextPre(parent, next) : getNextPost(parent.getParent(), parent);
  }

  private BlipView getNextPost(BlipView parent, BlipMetaView meta) {
    return getNextPost(parent, (AnchorView) null);
  }

  private BlipView getNextPost(BlipView parent, AnchorView anchor) {
    AnchorView next = parent.getDefaultAnchorAfter(anchor);
    return (next != null) ? getNextPre(parent, next) : getNextPost(parent,
        (InlineConversationView) null);
  }

  private BlipView getNextPost(BlipView parent, InlineConversationView conversation) {
    InlineConversationView next = parent.getConversationAfter(conversation);
    return (next != null) ? getNextPre(parent, next) : getNextPost(parent.getParent(), parent);
  }

  private BlipView getNextPost(BlipMetaView parent, AnchorView anchor) {
    AnchorView next = parent.getInlineAnchorAfter(anchor);
    return next != null ? getNextPre(parent, next) : getNextPost(parent.getParent(), parent);
  }

  private BlipView getNextPost(AnchorView parent, InlineThreadView thread) {
    return getNextPost(parent.getParent(), parent);
  }

  //
  // Prev Pre.
  //

  private BlipView getPrevPre(ConversationView parent, RootThreadView thread) {
    BlipView last = thread.getBlipBefore(null);
    return last != null ? getPrevPre(thread, last) : getPrevPost(parent, thread);
  }

  private BlipView getPrevPre(ThreadView parent, BlipView blip) {
    InlineConversationView last = blip.getConversationBefore(null);
    return last != null ? getPrevPre(blip, last) : getPrevPost(blip, (InlineConversationView) null);
  }

  private BlipView getPrevPre(AnchorView parent, InlineThreadView thread) {
    BlipView last = skip(thread) ? null : thread.getBlipBefore(null);
    return last != null ? getPrevPre(thread, last) : getPrevPost(parent, thread);
  }

  private BlipView getPrevPre(BlipView parent, BlipMetaView meta) {
    AnchorView last = meta.getInlineAnchorBefore(null);
    return last != null ? getPrevPre(meta, last) : getPrevPost(parent, meta);
  }

  private BlipView getPrevPre(BlipView parent, AnchorView anchor) {
    InlineThreadView contents = anchor.getThread();
    return contents != null ? getPrevPre(anchor, contents)
        : getPrevPost(anchor.getParent(), anchor);
  }

  private BlipView getPrevPre(BlipView parent, InlineConversationView conversation) {
    RootThreadView root = skip(conversation) ? null : conversation.getRootThread();
    return root != null ? getPrevPre(conversation, root) : getPrevPost(conversation, root);
  }

  private BlipView getPrevPre(BlipMetaView parent, AnchorView anchor) {
    InlineThreadView contents = anchor.getThread();
    return contents != null ? getPrevPre(anchor, contents)
        : getPrevPost(anchor.getParent(), anchor);
  }

  //
  // Prev Post.
  //

  private BlipView getPrevPost(View parent, View child) {
    if (parent == null) {
      return null;
    }
    switch (parent.getType()) {
      case ANCHOR:
        return getPrevPost((AnchorView) parent, (InlineThreadView) child);
      case META:
        return getPrevPost((BlipMetaView) parent, (AnchorView) child);
      case BLIP:
        switch (child.getType()) {
          case META:
            return getPrevPost((BlipView) parent, (BlipMetaView) child);
          case ANCHOR:
            return getPrevPost((BlipView) parent, (AnchorView) child);
          case INLINE_CONVERSATION:
            return getPrevPost((BlipView) parent, (InlineConversationView) child);
          default:
            throw new RuntimeException("unknown blip child type: " + child.getType());
        }
      case INLINE_THREAD:
        return getPrevPost((ThreadView) parent, (BlipView) child);
      case ROOT_THREAD:
        return getPrevPost((ThreadView) parent, (BlipView) child);
      case INLINE_CONVERSATION:
        return getPrevPost((InlineConversationView) parent, (RootThreadView) child);
      case ROOT_CONVERSATION:
        return getPrevPost((TopConversationView) parent, (RootThreadView) child);
      default:
        throw new RuntimeException("unknown parent type: " + parent.getType());
    }
  }

  private BlipView getPrevPost(InlineConversationView parent, RootThreadView child) {
    return getPrevPost(parent.getParent(), parent);
  }

  private BlipView getPrevPost(TopConversationView parent, RootThreadView child) {
    return null;
  }

 private BlipView getPrevPost(BlipView parent, AnchorView child) {
    AnchorView prev = parent.getDefaultAnchorBefore(child);
    BlipMetaView meta;
    return prev != null ? getPrevPre(parent, prev) // \u2620
        : ((meta = parent.getMeta()) != null) // \u2620
            ? getPrevPre(parent, meta) // \u2620
            : getPrevPost(parent, meta);
  }

  private BlipView getPrevPost(BlipView parent, BlipMetaView child) {
    return parent;
  }

  private BlipView getPrevPost(BlipView parent, InlineConversationView child) {
    InlineConversationView prev = parent.getConversationBefore(child);
    return prev != null ? getPrevPre(parent, prev) : getPrevPost(parent, (AnchorView) null);
  }

  private BlipView getPrevPost(BlipMetaView parent, AnchorView child) {
    AnchorView prev = parent.getInlineAnchorBefore(child);
    return prev != null ? getPrevPre(parent, prev) : getPrevPost(parent.getParent(), parent);
  }

  private BlipView getPrevPost(ThreadView parent, BlipView child) {
    BlipView prev = parent.getBlipBefore(child);
    return prev != null ? getPrevPre(parent, prev) : getPrevPost(parent.getParent(), parent);
  }

  private BlipView getPrevPost(AnchorView parent, InlineThreadView child) {
    return getPrevPost(parent.getParent(), parent);
  }
}
