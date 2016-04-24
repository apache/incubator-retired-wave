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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.LinkedPruningSequenceMap;
import org.waveprotocol.wave.client.common.util.SequenceElement;
import org.waveprotocol.wave.client.common.util.VolatileComparable;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.BlipInstaller;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.selection.content.SelectionUtil;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc;
import org.waveprotocol.wave.model.wave.Wavelet;

/**
 * Renders a reply element as an anchor view.
 *
 */
public final class InlineAnchorLiveRenderer extends RenderingMutationHandler {

  interface AnchorHandler {
    void onAnchorAddedBefore(ConversationBlip blip, ReplyDoodad ref, ReplyDoodad anchor);

    void onAnchorRemoved(ConversationBlip blip, ReplyDoodad anchor);
  }

  public final class ReplyDoodad implements VolatileComparable<ReplyDoodad> {
    private final ContentElement el;
    private final String threadId;
    private AnchorView anchor;

    public ReplyDoodad(ContentElement el, String id) {
      this.el = el;
      this.threadId = id;
    }

    public ConversationBlip getBlip() {
      return blip;
    }

    public String getId() {
      return threadId;
    }

    public AnchorView getAnchor() {
      if (anchor == null) {
        anchor = views.asAnchor(el.getImplNodelet());
      }
      return anchor;
    }

    public void setDomId(String id) {
      if (id != null) {
        el.getImplNodelet().setId(id);
        el.getImplNodelet().setAttribute(
            BuilderHelper.KIND_ATTRIBUTE, TypeCodes.kind(Type.ANCHOR));
      } else {
        el.getImplNodelet().setId(null);
        el.getImplNodelet().removeAttribute(BuilderHelper.KIND_ATTRIBUTE);
      }
    }

    @Override
    public int compareTo(ReplyDoodad o) {
      return el.compareTo(o.el);
    }

    @Override
    public boolean isComparable() {
      return el.isComparable();
    }

    @Override
    public boolean equals(Object obj) {
      return (obj == this || ((obj instanceof ReplyDoodad) && el.equals(((ReplyDoodad) obj).el)));
    }
  }

  public static BlipInstaller installer(final ViewIdMapper viewIdMapper,
      final AnchorHandler manager, final DomAsViewProvider views) {
    return new BlipInstaller() {
      @Override
      public void install(Wavelet w, Conversation c, ConversationBlip b, Registries r) {
        InlineAnchorLiveRenderer renderer =
            new InlineAnchorLiveRenderer(viewIdMapper, b, manager, views);

        r.getElementHandlerRegistry().registerRenderingMutationHandler(
            Blips.THREAD_INLINE_ANCHOR_TAGNAME, renderer);
        r.getElementHandlerRegistry().registerEventHandler(
            Blips.THREAD_INLINE_ANCHOR_TAGNAME, ANCHOR_HANDLER);
      }
    };
  }

  /**
   * Event handling logic for inline thread doodads
   */
  private final static NodeEventHandler ANCHOR_HANDLER = new ChunkyElementHandler() {
    @Override
    public boolean handleClick(ContentElement element, EditorEvent event) {
      SelectionUtil.placeCaretBeforeElement(element.getSelectionHelper(), element);
      return true;
    }

    @Override
    public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
      // Do nothing, and report gesture as handled.
      return true;
    }

    @Override
    public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
      // Do nothing, and report gesture as handled.
      return true;
    }
  };

  /** Reveals DOM elements as semantic views. */
  private final DomAsViewProvider views;

  /** Blip whose anchors this renderer renders. */
  private final ConversationBlip blip;

  /** View id mapper used to look up view id from model object. */
  private final ViewIdMapper viewIdMapper;

  //
  // Local state needed for providing AnchorHandler events through incremental
  // processing.
  //
  // Note that it might be possible to encode this state as extra attributes /
  // properties on the view itself, which may have benefits.
  //

  /** Anchors that have no predecessor with the same thread id. Lazily created. */
  private StringMap<ReplyDoodad> canonicals;

  /** Anchors that have a predecessor with the same thread id. Lazily created. */
  private IdentitySet<ReplyDoodad> duplicates;

  /** Ordering of canonical anchors in this document. Lazily created. */
  private LinkedPruningSequenceMap<ReplyDoodad, ReplyDoodad> canonicalOrder;

  /** Option listener to handle canonical anchor addition and removal. */
  private final AnchorHandler handler;

  static class CanonicalAnchorFinder implements Proc<ReplyDoodad> {
    /** Id of the anchor to find. */
    private final String id;
    /** Current minimum anchor with id {@code id}. */
    private ReplyDoodad min;

    CanonicalAnchorFinder(String id) {
      this.id = id;
    }

    static ReplyDoodad findMinimum(String id, IdentitySet<ReplyDoodad> anchors) {
      CanonicalAnchorFinder finder = new CanonicalAnchorFinder(id);
      anchors.each(finder);
      return finder.min;
    }

    @Override
    public void apply(ReplyDoodad duplicate) {
      if (duplicate.getId().equals(id)) {
        min = (min == null || duplicate.compareTo(min) < 0) ? duplicate : min;
      }
    }
  }

  private InlineAnchorLiveRenderer(ViewIdMapper viewIdMapper, ConversationBlip blip,
      AnchorHandler h, DomAsViewProvider views) {
    this.viewIdMapper = viewIdMapper;
    this.blip = blip;
    this.handler = h;
    this.views = views;
  }

  private IdentitySet<ReplyDoodad> getDuplicates() {
    if (duplicates == null) {
      duplicates = CollectionUtils.createIdentitySet();
    }
    return duplicates;
  }

  public StringMap<ReplyDoodad> getCanonicals() {
    if (canonicals == null) {
      canonicals = CollectionUtils.createStringMap();
    }
    return canonicals;
  }

  private LinkedPruningSequenceMap<ReplyDoodad, ReplyDoodad> getCanonicalOrder() {
    if (canonicalOrder == null) {
      canonicalOrder = LinkedPruningSequenceMap.create();
    }
    return canonicalOrder;
  }

  private void add(ReplyDoodad anchor) {
    ReplyDoodad existing = getCanonicals().get(anchor.getId());
    if (existing == null) {
      addCanonical(anchor);
    } else if (anchor.compareTo(existing) < 0) {
      // New anchor occurs earlier. Replace.
      removeCanonical(existing);
      getDuplicates().add(existing);
      addCanonical(anchor);
    } else {
      // New anchor occurs later. Ignore.
      getDuplicates().add(anchor);
    }
  }

  private void remove(ReplyDoodad anchor) {
    ReplyDoodad existing = getCanonicals().get(anchor.getId());
    if (!anchor.equals(existing)) {
      // Non-canonical removal.
      getDuplicates().remove(anchor);
    } else {
      removeCanonical(anchor);

      // Is there a duplicate to be promoted to a canonical?
      ReplyDoodad toPromote = CanonicalAnchorFinder.findMinimum(anchor.getId(), getDuplicates());
      if (toPromote != null) {
        getDuplicates().remove(toPromote);
        addCanonical(toPromote);
      }
    }
  }

  private void addCanonical(ReplyDoodad anchor) {
    getCanonicals().put(anchor.getId(), anchor);
    getCanonicalOrder().put(anchor, anchor);

    // Find next canonical, remembering to eliminate inconvenient circularity.
    SequenceElement<ReplyDoodad> nextNode = getCanonicalOrder().getElement(anchor).getNext();
    ReplyDoodad next = (nextNode != getCanonicalOrder().getFirst()) ? nextNode.value() : null;
    anchor.setDomId(viewIdMapper.inlineAnchorOf(blip, anchor.getId()));
    handler.onAnchorAddedBefore(blip, next, anchor);
  }

  private void removeCanonical(ReplyDoodad anchor) {
    getCanonicals().remove(anchor.getId());
    getCanonicalOrder().remove(anchor);
    handler.onAnchorRemoved(blip, anchor);
    // Clear DOM id only after handler processing.
    anchor.setDomId(null);
  }

  @Override
  public Element createDomImpl(Renderable element) {
    // HTML does not allow block elements inside inline elements, so it has to
    // be divs all the way, not spans.
    Element e = Document.get().createDivElement();
    e.getStyle().setDisplay(Display.INLINE);
    // Do the things that the doodad API should be doing by default.
    DomHelper.setContentEditable(e, false, false);
    DomHelper.makeUnselectable(e);
    // ContentElement attempts this, and fails, so we have to do this ourselves.
    e.getStyle().setProperty("whiteSpace", "normal");
    e.getStyle().setProperty("lineHeight", "normal");
    return e;
  }

  @Override
  public void onActivatedSubtree(ContentElement element) {
    String id = element.getAttribute(Blips.THREAD_INLINE_ANCHOR_ID_ATTR);
    if (id != null) {
      add(new ReplyDoodad(element, id));
    }
  }

  @Override
  public void onDeactivated(ContentElement element) {
    String id = element.getAttribute(Blips.THREAD_INLINE_ANCHOR_ID_ATTR);
    if (id != null) {
      remove(new ReplyDoodad(element, id));
    }
  }

  //
  // This renderer is only intended for a static rendering,
  // and is not intended for keeping that rendering live in response to events.
  // Since there is no API for static renderers (all renderers must be
  // implemented as mutation handlers), the only way to prevent misuse is to
  // detect it dynamically.
  //

  @Override
  public void onAttributeModified(ContentElement element, String name, String oldValue,
      String newValue) {
    if (Blips.THREAD_INLINE_ANCHOR_ID_ATTR.equals(name)) {
      if (oldValue != null) {
        remove(new ReplyDoodad(element, oldValue));
      }
      if (newValue != null) {
        add(new ReplyDoodad(element, newValue));
      }
    }
  }
}
