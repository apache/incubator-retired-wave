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

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.StringSequence;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.DocumentRegistry;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.dom.BlipMetaDomImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.BlipViewDomImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer.PagingHandler;
import org.waveprotocol.wave.client.wavepanel.view.impl.BlipMetaViewImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.BlipViewImpl;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * Transfers blips between their paged in and paged out states.
 *
 */
public final class BlipPager implements PagingHandler {
  /** Fills in an existing blip view. */
  private final ShallowBlipRenderer blipPopulator;

  /** Provides access to the document implementation of a blip. */
  private final DocProvider docProvider;

  /** Interprets DOM elements as views. */
  private final DomAsViewProvider views;

  /** Get the view for a particular model */
  private final ModelAsViewProvider viewProvider;

  /** Reveals the registries to use to render a document. */
  private final DocumentRegistries registries;

  /** Logical panel for widgets in paged-in blips to attach themselves to */
  private final LogicalPanel logicalPanel;

  /**
   * Maps models to implementations.
   */
  interface DocProvider {
    InteractiveDocument docOf(ConversationBlip blip);
  }

  private BlipPager(DocumentRegistries registries, ShallowBlipRenderer blipPopulator,
      DocProvider docProvider, DomAsViewProvider views, ModelAsViewProvider viewProvider,
      LogicalPanel logicalPanel) {
    this.registries = registries;
    this.blipPopulator = blipPopulator;
    this.docProvider = docProvider;
    this.views = views;
    this.viewProvider = viewProvider;
    this.logicalPanel = logicalPanel;
  }

  /**
   * Creates a pager.
   */
  public static BlipPager create(final DocumentRegistry<? extends InteractiveDocument> docRegistry,
      DocumentRegistries registries, DomAsViewProvider views, ModelAsViewProvider viewProvider,
      ShallowBlipRenderer blipPopulator, LogicalPanel logicalPanel) {
    DocProvider docProvider = new DocProvider() {
      @Override
      public InteractiveDocument docOf(ConversationBlip blip) {
        return docRegistry.get(blip);
      }
    };

    return new BlipPager(registries, blipPopulator, docProvider, views, viewProvider, logicalPanel);
  }

  /**
   * Moves all the inline replies of a blip to their default-anchor locations.
   */
  private void saveInlineReplies(BlipMetaDomImpl metaDom) {
    // Iteration is done via ids, in order to identify the thread to get the
    // inline -> default location mapping.

    StringSequence inlineLocators = metaDom.getInlineLocators();
    String inlineId = inlineLocators.getFirst();
    while (inlineId != null) {
      AnchorView inlineUi = views.asAnchor(Document.get().getElementById(inlineId));
      InlineThreadView threadUi = inlineUi.getThread();
      if (threadUi != null) {
        // Move to default location.
        String defaultId = ViewIdMapper.defaultOfInlineAnchor(inlineId);
        AnchorView defaultUi = views.asAnchor(Document.get().getElementById(defaultId));
        inlineUi.detach(threadUi);
        defaultUi.attach(threadUi);
      }

      inlineId = inlineLocators.getNext(inlineId);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void pageIn(ConversationBlip blip) {
    BlipViewImpl<BlipViewDomImpl> blipUi =
        (BlipViewImpl<BlipViewDomImpl>) viewProvider.getBlipView(blip);
    if (blipUi != null) {
      BlipViewDomImpl blipDom = blipUi.getIntrinsic();
      BlipMetaDomImpl metaDom =
          ((BlipMetaViewImpl<BlipMetaDomImpl>) blipUi.getMeta()).getIntrinsic();
      InteractiveDocument doc = docProvider.docOf(blip);
      Registries r = registries.get(blip);

      // Very first thing that must be done is to extract and save the DOM of
      // inline threads, since content-document rendering will blast them away.
      saveInlineReplies(metaDom);

      // Clear content before rendering, so that doodad events caused by rendering
      // apply on a fresh state.
      metaDom.clearContent();
      doc.startRendering(r, logicalPanel);
      metaDom.setContent(
          doc.getDocument().getFullContentView().getDocumentElement().getImplNodelet());
      blipPopulator.render(blip, metaDom);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void pageOut(ConversationBlip blip) {
    BlipViewImpl<BlipViewDomImpl> blipUi =
        (BlipViewImpl<BlipViewDomImpl>) viewProvider.getBlipView(blip);
    if (blipUi != null) {
      BlipViewDomImpl blipDom = blipUi.getIntrinsic();
      BlipMetaDomImpl metaDom =
          ((BlipMetaViewImpl<BlipMetaDomImpl>) blipUi.getMeta()).getIntrinsic();
      InteractiveDocument doc = docProvider.docOf(blip);

      // TODO: remove meta instead
      doc.stopRendering();
      metaDom.setContent(null);
    }
  }
}
