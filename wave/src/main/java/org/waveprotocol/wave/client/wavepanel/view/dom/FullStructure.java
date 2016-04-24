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

package org.waveprotocol.wave.client.wavepanel.view.dom;

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.KIND_ATTRIBUTE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.attachAfter;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.attachBefore;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.getAfter;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.getBefore;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes.kind;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes.type;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.scroll.DomScrollPanel;
import org.waveprotocol.wave.client.scroll.Extent;
import org.waveprotocol.wave.client.scroll.ScrollPanel;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.FocusFrameView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipLinkPopupWidget;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.DomRenderer;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.FocusFrame;
import org.waveprotocol.wave.client.wavepanel.view.impl.AbstractConversationViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.AbstractStructuredView;
import org.waveprotocol.wave.client.wavepanel.view.impl.AnchorViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.BlipMenuItemViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.BlipMetaViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.BlipViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.ContinuationIndicatorViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.InlineConversationViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.InlineThreadViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.ParticipantViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.ParticipantsViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.ReplyBoxViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.RootThreadViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.TopConversationViewImpl;
import org.waveprotocol.wave.client.widget.popup.AlignedPopupPositioner;
import org.waveprotocol.wave.client.widget.profile.ProfilePopupView;
import org.waveprotocol.wave.client.widget.profile.ProfilePopupWidget;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Implements structural accessors and mutators for wave-panel views.
 *
 */
public class FullStructure implements UpgradeableDomAsViewProvider {

  /** Mapping of kinds to types. */
  private static final ReadableStringSet KNOWN_KINDS;

  static {
    StringSet kinds = CollectionUtils.createStringSet();
    for (Type type : new Type[] {Type.BLIP,
        Type.META,
        Type.MENU_ITEM,
        Type.ROOT_THREAD,
        Type.REPLY_BOX,
        Type.INLINE_THREAD,
        Type.CONTINUATION_INDICATOR,
        Type.ANCHOR,
        Type.ROOT_CONVERSATION,
        Type.INLINE_CONVERSATION,
        Type.PARTICIPANT,
        Type.PARTICIPANTS}) {
      kinds.add(kind(type));
    }
    KNOWN_KINDS = kinds;
  }

  private final BlipMenuItemViewImpl.Helper<BlipMenuItemDomImpl> menuHelper =
      new BlipMenuItemViewImpl.Helper<BlipMenuItemDomImpl>() {

        @Override
        public BlipMetaView getParent(BlipMenuItemDomImpl impl) {
          return asBlipMeta(parentOf(impl));
        }

        @Override
        public void remove(BlipMenuItemDomImpl impl) {
          impl.remove();
        }
      };

  private final BlipMetaViewImpl.Helper<BlipMetaDomImpl> metaHelper =
      new BlipMetaViewImpl.Helper<BlipMetaDomImpl>() {

        private Element frameElement(FocusFrameView frame) {
          return ((FocusFrame) frame).getElement();
        }

        @Override
        public void insertChrome(BlipMetaDomImpl impl, FocusFrameView frame) {
          impl.getElement().appendChild(frameElement(frame));
        }

        @Override
        public void removeChrome(BlipMetaDomImpl impl, FocusFrameView frame) {
          frameElement(frame).removeFromParent();
        }

        @Override
        public AnchorView getInlineAnchorAfter(BlipMetaDomImpl impl, AnchorView ref) {
          return asAnchor(impl.getInlineAnchorAfter(elementOf(ref)));
        }

        @Override
        public AnchorView getInlineAnchorBefore(BlipMetaDomImpl impl, AnchorView ref) {
          return asAnchor(impl.getInlineAnchorBefore(elementOf(ref)));
        }

        @Override
        public void insertInlineAnchorBefore(BlipMetaDomImpl impl, AnchorView ref, AnchorView x) {
          @SuppressWarnings("unchecked")
          AnchorDomImpl anchor = ((AnchorViewImpl<AnchorDomImpl>) x).getIntrinsic();
          impl.insertInlineLocatorBefore(elementOf(ref), anchor.getElement());
          anchor.setParentId(impl.getId());
        }

        @Override
        public BlipView getBlip(BlipMetaDomImpl impl) {
          return asBlip(parentOf(impl));
        }

        @Override
        public void remove(BlipMetaDomImpl impl) {
          impl.remove();
        }
      };

  private final BlipViewImpl.Helper<BlipViewDomImpl> blipHelper =
      new BlipViewImpl.Helper<BlipViewDomImpl>() {

        @Override
        public ThreadView getBlipParent(BlipViewDomImpl thread) {
          return asThread(parentOf(thread));
        }

        @Override
        public BlipMetaViewImpl<BlipMetaDomImpl> getMeta(BlipViewDomImpl impl) {
          return asBlipMeta(impl.getMetaHolder());
        }

        @Override
        public AnchorView getDefaultAnchorAfter(BlipViewDomImpl impl, AnchorView ref) {
          return asAnchor(getAfter(impl.getDefaultAnchors(), elementOf(ref)));
        }

        @Override
        public AnchorView getDefaultAnchorBefore(BlipViewDomImpl impl, AnchorView ref) {
          return asAnchor(getBefore(impl.getDefaultAnchors(), elementOf(ref)));
        }

        @Override
        public AnchorView insertDefaultAnchorBefore(
            BlipViewDomImpl impl, AnchorView ref, ConversationThread t) {
          Element anchorDom = getRenderer().render(t);
          attachBefore(impl.getDefaultAnchors(), elementOf(ref), anchorDom);
          return asAnchorUnchecked(anchorDom);
        }

        @Override
        public AnchorView insertDefaultAnchorAfter(
            BlipViewDomImpl impl, AnchorView ref, ConversationThread t) {
          Element anchorDom = getRenderer().render(t);
          attachAfter(impl.getDefaultAnchors(), elementOf(ref), anchorDom);
          return asAnchorUnchecked(anchorDom);
        }

        @Override
        public InlineConversationView getConversationBefore(
            BlipViewDomImpl impl, InlineConversationView ref) {
          return asInlineConversation(getBefore(impl.getConversations(), elementOf(ref)));
        }

        @Override
        public InlineConversationView getConversationAfter(
            BlipViewDomImpl impl, InlineConversationView ref) {
          return asInlineConversation(getAfter(impl.getConversations(), elementOf(ref)));
        }

        @Override
        public InlineConversationView insertConversationBefore(
            BlipViewDomImpl impl, InlineConversationView ref, Conversation c) {
          Element convDom = getRenderer().render(c);
          attachBefore(impl.getConversations(), elementOf(ref), convDom);
          return asInlineConversationUnchecked(convDom);
        }

        @Override
        public void removeBlip(BlipViewDomImpl impl) {
          impl.remove();
        }

        @Override
        public BlipLinkPopupView createLinkPopup(BlipViewDomImpl impl) {
          return new BlipLinkPopupWidget(impl.getElement());
        }
      };

  private final RootThreadViewImpl.Helper<RootThreadDomImpl> rootThreadHelper =
      new RootThreadViewImpl.Helper<RootThreadDomImpl>() {

        @Override
        public BlipView getBlipBefore(RootThreadDomImpl thread, View ref) {
          return asBlip(getBefore(thread.getBlipContainer(), elementOf(ref)));
        }

        @Override
        public BlipView getBlipAfter(RootThreadDomImpl thread, View ref) {
          return asBlip(getAfter(thread.getBlipContainer(), elementOf(ref)));
        }

        @Override
        public ConversationView getThreadParent(RootThreadDomImpl thread) {
          return asConversation(parentOf(thread));
        }

        @Override
        public BlipView insertBlipBefore(
            RootThreadDomImpl thread, View ref, ConversationBlip blip) {
          Element t = getRenderer().render(blip);
          thread.getBlipContainer().insertBefore(t, elementOf(ref));
          return asBlip(t);
        }

        @Override
        public BlipView insertBlipAfter(
            RootThreadDomImpl thread, View ref, ConversationBlip blip) {
          Element t = getRenderer().render(blip);
          thread.getBlipContainer().insertAfter(t, elementOf(ref));
          return asBlip(t);
        }

        @Override
        public void removeThread(RootThreadDomImpl thread) {
          thread.remove();
        }

        @Override
        public ReplyBoxView getIndicator(RootThreadDomImpl impl) {
          return asReplyBox(impl.getIndicator());
        }
      };

  private final ReplyBoxViewImpl.Helper<ReplyBoxDomImpl>
      rootThreadIndicatorHelper =
          new ReplyBoxViewImpl.Helper<ReplyBoxDomImpl>() {

    @Override
    public RootThreadView getParent(ReplyBoxDomImpl impl) {
      return asRootThread(parentOf(impl));
    }

    @Override
    public void remove(ReplyBoxDomImpl impl) {
      impl.remove();
    }

  };

  private final InlineThreadViewImpl.Helper<InlineThreadDomImpl> inlineThreadHelper =
      new InlineThreadViewImpl.Helper<InlineThreadDomImpl>() {

        @Override
        public BlipView getBlipBefore(InlineThreadDomImpl thread, View ref) {
          return asBlip(getBefore(thread.getBlipContainer(), elementOf(ref)));
        }

        @Override
        public BlipView getBlipAfter(InlineThreadDomImpl thread, View ref) {
          return asBlip(getAfter(thread.getBlipContainer(), elementOf(ref)));
        }

        @Override
        public AnchorView getThreadParent(InlineThreadDomImpl thread) {
          return asAnchor(parentOf(thread));
        }

        @Override
        public BlipView insertBlipBefore(
            InlineThreadDomImpl thread, View ref, ConversationBlip blip) {
          Element t = getRenderer().render(blip);
          thread.getBlipContainer().insertBefore(t, elementOf(ref));
          return asBlip(t);
        }

        @Override
        public BlipView insertBlipAfter(
            InlineThreadDomImpl thread, View ref, ConversationBlip blip) {
          Element t = getRenderer().render(blip);
          thread.getBlipContainer().insertAfter(t, elementOf(ref));
          return asBlip(t);
        }

        @Override
        public void removeThread(InlineThreadDomImpl thread) {
          thread.remove();
        }

        @Override
        public ContinuationIndicatorView getIndicator(InlineThreadDomImpl thread) {
          return asContinuationIndicator(thread.getContinuationIndicator());
        }
      };

  private final ContinuationIndicatorViewImpl.Helper<ContinuationIndicatorDomImpl>
      inlineThreadIndicatorHelper =
          new ContinuationIndicatorViewImpl.Helper<ContinuationIndicatorDomImpl>() {

    @Override
    public InlineThreadView getParent(ContinuationIndicatorDomImpl impl) {
      return asInlineThread(parentOf(impl));
    }

    @Override
    public void remove(ContinuationIndicatorDomImpl impl) {
      impl.remove();
    }

  };

  private final AnchorViewImpl.Helper<AnchorDomImpl> anchorHelper =
      new AnchorViewImpl.Helper<AnchorDomImpl>() {
        @Override
        public void attach(AnchorDomImpl anchor, InlineThreadView thread) {
          anchor.setChild(elementOf(thread));
        }

        @Override
        public void detach(AnchorDomImpl anchor, InlineThreadView thread) {
          anchor.removeChild(elementOf(thread));
        }

        @Override
        public InlineThreadView getThread(AnchorDomImpl anchor) {
          Element child = anchor.getChild();
          return asInlineThread(child);
        }

        @Override
        public void remove(AnchorDomImpl impl) {
          View parent = getParent(impl);
          if (parent.getType() == Type.BLIP) {
            BlipViewDomImpl parentImpl = ((BlipViewDomImpl) narrow(parent).getIntrinsic());
            DomViewHelper.detach(parentImpl.getDefaultAnchors(), impl.getElement());
          } else if (parent.getType() == Type.META) {
            BlipMetaDomImpl parentImpl = ((BlipMetaDomImpl) narrow(parent).getIntrinsic());
            parentImpl.removeInlineLocator(impl.getElement());
            // Do not detach here - editor rendering controls that.
            impl.setParentId(null);
          }
        }

        @Override
        public View getParent(AnchorDomImpl impl) {
          String pid = impl.getParentId();
          return pid != null ? viewOf(Document.get().getElementById(pid)) : parentOf(impl);
        }
      };

  private final TopConversationViewImpl.Helper<TopConversationDomImpl> convHelper =
      new TopConversationViewImpl.Helper<TopConversationDomImpl>() {
        private ScrollPanel<Element> createDomScroller(TopConversationDomImpl impl) {
          return DomScrollPanel.create(impl.getThreadContainer());
        }

        @Override
        public ScrollPanel<? super View> getScroller(TopConversationDomImpl impl) {
          final ScrollPanel<Element> domScroller = createDomScroller(impl);
          return new ScrollPanel<View>() {
            @Override
            public Extent extentOf(View ui) {
              return domScroller.extentOf(elementOf(ui));
            }

            @Override
            public Extent getContent() {
              return domScroller.getContent();
            }

            @Override
            public Extent getViewport() {
              return domScroller.getViewport();
            }

            @Override
            public void moveTo(double location) {
              domScroller.moveTo(location);
            }
          };
        }

        @Override
        public void setToolbar(TopConversationDomImpl impl, Element e) {
          impl.setToolbar(e);
        }

        @Override
        public ParticipantsView getParticipants(TopConversationDomImpl impl) {
          return asParticipants(impl.getParticipants());
        }

        @Override
        public RootThreadView getRootThread(TopConversationDomImpl impl) {
          return asRootThread(impl.getThread());
        }

        @Override
        public void remove(TopConversationDomImpl impl) {
          impl.remove();
        }
      };

  private final InlineConversationViewImpl.Helper<InlineConversationDomImpl> inlineConvHelper =
      new InlineConversationViewImpl.Helper<InlineConversationDomImpl>() {

        @Override
        public ParticipantsView getParticipants(InlineConversationDomImpl impl) {
          return asParticipants(impl.getParticipants());
        }

        @Override
        public RootThreadView getRootThread(InlineConversationDomImpl impl) {
          return asRootThread(impl.getRootThread());
        }

        @Override
        public void remove(InlineConversationDomImpl impl) {
          impl.remove();
        }

        @Override
        public BlipView getParent(InlineConversationDomImpl impl) {
          return asBlip(parentOf(impl));
        }
      };

  private final ParticipantViewImpl.Helper<ParticipantNameDomImpl> participantHelper =
      new ParticipantViewImpl.Helper<ParticipantNameDomImpl>() {

        @Override
        public void remove(ParticipantNameDomImpl impl) {
          Element container = impl.getElement().getParentElement();
          impl.remove();

          // Kick Webkit, because of its incremental layout bugs.
          if (UserAgent.isWebkit()) {
            // Erase layout. Querying getOffsetParent() forces layout.
            container.getStyle().setDisplay(Display.NONE);
            container.getOffsetParent();
            // Restore layout.
            container.getStyle().clearDisplay();
          }
        }

        @Override
        public ProfilePopupView showParticipation(ParticipantNameDomImpl impl) {
          return new ProfilePopupWidget(impl.getElement(), AlignedPopupPositioner.BELOW_RIGHT);
        }
      };

  private final ParticipantsViewImpl.Helper<ParticipantsDomImpl> participantsHelper =
      new ParticipantsViewImpl.Helper<ParticipantsDomImpl>() {

        @Override
        public void remove(ParticipantsDomImpl impl) {
          impl.remove();
        }

        @Override
        public ParticipantView append(
            ParticipantsDomImpl impl, Conversation conv, ParticipantId participant) {
          Element t = getRenderer().render(conv, participant);
          DomViewHelper.attachBefore(impl.getParticipantContainer(), impl.getSimpleMenu(), t);
          // Kick Webkit, because of its incremental layout bugs.
          if (UserAgent.isWebkit()) {

            String oldDisplay = impl.getElement().getStyle().getDisplay();

            // Erase layout. Querying getOffsetParent() forces layout.
            impl.getElement().getStyle().setDisplay(Display.NONE);
            impl.getElement().getOffsetParent();

            // Restore layout.
            impl.getElement().getStyle().setProperty("display", oldDisplay);
          }
          return asParticipant(t);
        }
      };

  /** Injected Css resources providing access to style names to apply. */
  private final CssProvider cssProvider;
      
  /**
   * Renderer for creating new parts of the DOM. Initially unset, then set once
   * in {@link #setRenderer(DomRenderer)}.
   */
  private DomRenderer renderer;

  /**
   * Creates a view provider/manager/handler/oracle.
   */
  public FullStructure(CssProvider cssProvider) {
    this.cssProvider = cssProvider;
  }

  @Override
  public void setRenderer(DomRenderer renderer) {
    Preconditions.checkArgument(renderer != null);
    Preconditions.checkState(this.renderer == null);
    this.renderer = renderer;
  }

  /** @return the renderer for creating new sections of the DOM. */
  private DomRenderer getRenderer() {
    Preconditions.checkState(renderer != null, "Renderer not ready");
    return renderer;
  }

  /**
   * Narrows a view to the most specific common supertype of all views in this
   * package.
   *
   * @return {@code} v as a {@link DomView}.
   */
  @SuppressWarnings("unchecked")
  private AbstractStructuredView<?, ? extends DomView> narrow(View v) {
    // Note:
    // Since the view APIs only exposes closed-universe synthesis of view
    // objects, this cast is safe. This could alternatively be implemented by
    // saving all synthesized views in a map of View -> DomView, and
    // implementing this via a lookup. The existence of a non-cast
    // implementation shows that narrowing is not the same as down-casting. The
    // use of down-casting here is just an optimization, and is not logically
    // unsound like regular down-casting is.
    return (AbstractStructuredView<?, ? extends DomView>) v;
  }

  /** @return the DOM element of a view. */
  private Element elementOf(View v) {
    return v == null ? null : narrow(v).getIntrinsic().getElement();
  }

  //
  // Adapters.
  //

  /**
   * Adapts a DOM element to the view implementation from this package. The view
   * implementation is chosen based on the element's kind attribute.
   */
  private View viewOf(Element e) {
    if (e == null) {
      return null;
    }
    switch (typeOf(e)) {
      case ANCHOR:
        return asAnchorUnchecked(e);
      case BLIP:
        return asBlipUnchecked(e);
      case META:
        return asBlipMetaUnchecked(e);
      case MENU_ITEM:
        return asBlipMenuItemUnchecked(e);
      case INLINE_THREAD:
        return asInlineThreadUnchecked(e);
      case ROOT_THREAD:
        return asRootThreadUnchecked(e);
      case REPLY_BOX:
        return asRootThreadIndicatorUnchecked(e);
      case CONTINUATION_INDICATOR:
        return asContinuationIndicatorUnchecked(e);
      case ROOT_CONVERSATION:
        return asTopConversationUnchecked(e);
      case INLINE_CONVERSATION:
        return asInlineConversationUnchecked(e);
      case PARTICIPANT:
        return asParticipant(e);
      case PARTICIPANTS:
        return asParticipants(e);
      default:
        throw new AssertionError();
    }
  }

  /**
   * @return true if {@code e} has a kind supported by a view implementation
   *         from this package.
   */
  private boolean hasKnownType(Element e) {
    return e.hasAttribute(KIND_ATTRIBUTE) && KNOWN_KINDS.contains(e.getAttribute(KIND_ATTRIBUTE));
  }

  /** @return the view type of an element. */
  private Type typeOf(Element e) {
    Type type = e.hasAttribute(KIND_ATTRIBUTE) ? type(e.getAttribute(KIND_ATTRIBUTE)) : null;
    if (type == null) {
      throw new RuntimeException("element has no known kind: " + e.getAttribute(KIND_ATTRIBUTE));
    } else {
      return type;
    }
  }

  private RootThreadViewImpl<RootThreadDomImpl> asRootThreadUnchecked(Element e) {
    return e == null ? null : new RootThreadViewImpl<RootThreadDomImpl>(
        rootThreadHelper, RootThreadDomImpl.of(e));
  }

  private ReplyBoxViewImpl<ReplyBoxDomImpl> asRootThreadIndicatorUnchecked(Element e) {
    return e == null ? null : new ReplyBoxViewImpl<ReplyBoxDomImpl>(
        rootThreadIndicatorHelper, ReplyBoxDomImpl.of(e));
  }

  private InlineThreadViewImpl<InlineThreadDomImpl> asInlineThreadUnchecked(Element e) {
    return e == null ? null : new InlineThreadViewImpl<InlineThreadDomImpl>(
        inlineThreadHelper, InlineThreadDomImpl.of(e, cssProvider.getCollapsibleCss()));
  }

  private ContinuationIndicatorViewImpl<ContinuationIndicatorDomImpl>
      asContinuationIndicatorUnchecked(Element e) {
    return e == null ? null : new ContinuationIndicatorViewImpl<ContinuationIndicatorDomImpl>(
        inlineThreadIndicatorHelper, ContinuationIndicatorDomImpl.of(e));
  }

  private AnchorViewImpl<AnchorDomImpl> asAnchorUnchecked(Element e) {
    return e == null ? null : new AnchorViewImpl<AnchorDomImpl>(
        anchorHelper, AnchorDomImpl.of(e));
  }

  private BlipViewImpl<BlipViewDomImpl> asBlipUnchecked(Element e) {
    return e == null ? null : new BlipViewImpl<BlipViewDomImpl>(
        blipHelper, BlipViewDomImpl.of(e));
  }

  private ParticipantViewImpl<ParticipantNameDomImpl> asParticipantUnchecked(Element e) {
    return e == null ? null : new ParticipantViewImpl<ParticipantNameDomImpl>(
        participantHelper, ParticipantNameDomImpl.of(e));
  }

  private ParticipantsViewImpl<ParticipantsDomImpl> asParticipantsUnchecked(Element e) {
    return e == null ? null : new ParticipantsViewImpl<ParticipantsDomImpl>(
        participantsHelper, ParticipantsDomImpl.of(e));
  }

  private BlipMetaViewImpl<BlipMetaDomImpl> asBlipMetaUnchecked(Element e) {
    return e == null ? null : new BlipMetaViewImpl<BlipMetaDomImpl>(
        metaHelper, BlipMetaDomImpl.of(e, cssProvider.getBlipCss()));
  }

  private BlipMenuItemViewImpl<BlipMenuItemDomImpl> asBlipMenuItemUnchecked(Element e) {
    return e == null ? null : BlipMenuItemViewImpl.create(
        menuHelper, BlipMenuItemDomImpl.of(e, cssProvider.getBlipCss()));
  }

  private TopConversationViewImpl<TopConversationDomImpl> asTopConversationUnchecked(Element e) {
    return e == null ? null : new TopConversationViewImpl<TopConversationDomImpl>(
        convHelper, TopConversationDomImpl.of(e));
  }

  private InlineConversationViewImpl<InlineConversationDomImpl> asInlineConversationUnchecked(
      Element e) {
    return e == null ? null : new InlineConversationViewImpl<InlineConversationDomImpl>(
        inlineConvHelper, InlineConversationDomImpl.of(e, cssProvider.getCollapsibleCss()));
  }

  @Override
  public RootThreadViewImpl<RootThreadDomImpl> asRootThread(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.ROOT_THREAD);
    return asRootThreadUnchecked(e);
  }

  @Override
  public ReplyBoxViewImpl<ReplyBoxDomImpl> asReplyBox(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.REPLY_BOX);
    return asRootThreadIndicatorUnchecked(e);
  }

  @Override
  public InlineThreadViewImpl<InlineThreadDomImpl> asInlineThread(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.INLINE_THREAD);
    return asInlineThreadUnchecked(e);
  }

  @Override
  public ContinuationIndicatorView asContinuationIndicator(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.CONTINUATION_INDICATOR);
    return asContinuationIndicatorUnchecked(e);
  }

  @Override
  public BlipViewImpl<BlipViewDomImpl> asBlip(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.BLIP);
    return asBlipUnchecked(e);
  }

  @Override
  public ParticipantView asParticipant(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.PARTICIPANT);
    return asParticipantUnchecked(e);
  }

  @Override
  public ParticipantsView asParticipants(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.PARTICIPANTS);
    return asParticipantsUnchecked(e);
  }

  @Override
  public BlipMetaViewImpl<BlipMetaDomImpl> asBlipMeta(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.META);
    return asBlipMetaUnchecked(e);
  }

  @Override
  public BlipMenuItemViewImpl<BlipMenuItemDomImpl> asBlipMenuItem(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.MENU_ITEM);
    return asBlipMenuItemUnchecked(e);
  }

  @Override
  public AnchorViewImpl<AnchorDomImpl> asAnchor(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.ANCHOR);
    return asAnchorUnchecked(e);
  }

  @Override
  public TopConversationViewImpl<TopConversationDomImpl> asTopConversation(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.ROOT_CONVERSATION);
    return asTopConversationUnchecked(e);
  }

  @Override
  public InlineConversationViewImpl<InlineConversationDomImpl> asInlineConversation(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.INLINE_CONVERSATION);
    return asInlineConversationUnchecked(e);
  }

  @Override
  public AbstractConversationViewImpl<?, ?> asConversation(Element e) {
    if (e == null) {
      return null;
    } else {
      View.Type type = typeOf(e);
      switch (type) {
        case ROOT_CONVERSATION:
          return asTopConversationUnchecked(e);
        case INLINE_CONVERSATION:
          return asInlineConversationUnchecked(e);
        default:
          throw new IllegalArgumentException("Element has a non-conversation type: " + type);
      }
    }
  }

  @Override
  public InlineThreadView fromToggle(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.TOGGLE);
    return e == null ? null : new InlineThreadViewImpl<InlineThreadDomImpl>(
        inlineThreadHelper, InlineThreadDomImpl.ofToggle(e, cssProvider.getCollapsibleCss()));
  }

  @Override
  public ParticipantsView fromAddButton(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.ADD_PARTICIPANT);
    while (e != null && !hasKnownType(e)) {
      e = e.getParentElement();
    }
    // Assume that the nearest kinded ancestor of the add button is the
    // participants view (an exception is thrown if not).
    return asParticipants(e);
  }

  @Override
  public ParticipantsView fromNewWaveWithParticipantsButton(Element e) {
    Preconditions.checkArgument(e == null || typeOf(e) == Type.NEW_WAVE_WITH_PARTICIPANTS);
    while (e != null && !hasKnownType(e)) {
      e = e.getParentElement();
    }
    // Assume that the nearest kinded ancestor of the add button is the
    // participants view (an exception is thrown if not).
    return asParticipants(e);
  }

  private AnchorView asAnchor(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.ANCHOR);
    return (AnchorView) v;
  }

  private BlipView asBlip(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.BLIP);
    return (BlipView) v;
  }

  private BlipMetaView asBlipMeta(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.META);
    return (BlipMetaView) v;
  }

  private ThreadView asThread(View v) {
    Preconditions.checkArgument(
        v == null || v.getType() == Type.INLINE_THREAD || v.getType() == Type.ROOT_THREAD);
    return (ThreadView) v;
  }

  private RootThreadView asRootThread(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.ROOT_THREAD);
    return (RootThreadView) v;
  }

  private InlineThreadView asInlineThread(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.INLINE_THREAD);
    return (InlineThreadView) v;
  }

  private ConversationView asConversation(View v) {
    Preconditions.checkArgument(v == null || v.getType() == Type.ROOT_CONVERSATION
        || v.getType() == Type.INLINE_CONVERSATION);
    return (ConversationView) v;
  }

  //
  // Shared structure.
  //

  private View parentOf(DomView v) {
    Element c = v.getElement().getParentElement();
    while (c != null && !hasKnownType(c)) {
      c = c.getParentElement();
    }
    return viewOf(c);
  }
}
