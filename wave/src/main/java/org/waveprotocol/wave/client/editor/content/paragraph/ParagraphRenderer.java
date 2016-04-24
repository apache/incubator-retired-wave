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

package org.waveprotocol.wave.client.editor.content.paragraph;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Alignment;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Direction;
import org.waveprotocol.wave.client.scheduler.FinalTaskRunner;
import org.waveprotocol.wave.client.scheduler.FinalTaskRunnerImpl;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Logic for rendering paragraphs. Exact HTML style decisions are deferred to a
 * {@link ParagraphHtmlRenderer}
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ParagraphRenderer extends RenderingMutationHandler {

  public static ParagraphRenderer create(String nodeletTagname) {
    return new ParagraphRenderer(new DefaultParagraphHtmlRenderer(nodeletTagname));
  }

  private final IdentitySet<ContentElement> elementsToRender =
      CollectionUtils.createIdentitySet();

  private final OrderedListRenumberer renumberer;

  private final FinalTaskRunner runner;

  private final Scheduler.Task batchRenderCommand = new Scheduler.Task() {
    @Override public void execute() {
      while (!elementsToRender.isEmpty()) {
        ContentElement e = elementsToRender.someElement();
        elementsToRender.remove(e);
        render(e);
      }

      renumberer.renumberAll();
    }

    private void render(ContentElement p) {
      if (p.getImplNodelet() == null) {
        // bail if no impl nodelet, the node might be shelved
        return;
      }

      int indent = Paragraph.getIndent(p.getAttribute(Paragraph.INDENT_ATTR));
      p.getContext().beginDeferredMutation();
      htmlRenderer.updateRendering(p,
          p.getAttribute(Paragraph.SUBTYPE_ATTR),
          p.getAttribute(Paragraph.LIST_STYLE_ATTR),
          indent,
          Alignment.fromValue(p.getAttribute(Paragraph.ALIGNMENT_ATTR)),
          Direction.fromValue(p.getAttribute(Paragraph.DIRECTION_ATTR)));
      p.getContext().endDeferredMutation();
    }
  };

  private final ParagraphHtmlRenderer htmlRenderer;

  public ParagraphRenderer(ParagraphHtmlRenderer htmlRenderer) {
    this(htmlRenderer, new OrderedListRenumberer(htmlRenderer), new FinalTaskRunnerImpl());
  }

  @VisibleForTesting
  ParagraphRenderer(ParagraphHtmlRenderer htmlRenderer,
      OrderedListRenumberer renumberer, FinalTaskRunner finalTaskRunner) {
    Preconditions.checkNotNull(htmlRenderer, "Null html renderer");
    this.htmlRenderer = htmlRenderer;
    this.renumberer = renumberer;
    this.runner = finalTaskRunner;
  }

  @Override
  public final Element createDomImpl(Renderable element) {
    return htmlRenderer.createDomImpl(element);
  }

  @Override
  public void onAttributeModified(ContentElement p, String name,
      String oldValue, String newValue) {
    // In case we're in non-render mode
    if (p.getImplNodelet() == null && !renumberer.updateHtmlEvenWhenNullImplNodelet) {
      return;
    }

    if (Paragraph.SUBTYPE_ATTR.equals(name) || Paragraph.LIST_STYLE_ATTR.equals(name)) {
      ParagraphBehaviour b = ParagraphBehaviour.of(newValue);
      scheduleRenderUpdate(p);
      scheduleRenumber(p, p.getAttribute(Paragraph.INDENT_ATTR), false);
    } else if (Paragraph.INDENT_ATTR.equals(name)) {
      scheduleRenderUpdate(p);
      scheduleRenumber(p, oldValue, false);
    } else if (Paragraph.ALIGNMENT_ATTR.equals(name)) {
      scheduleRenderUpdate(p);
    } else if (Paragraph.DIRECTION_ATTR.equals(name)) {
      scheduleRenderUpdate(p);
    }
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void onChildAdded(ContentElement p, ContentNode child) {
    ParagraphHelper.INSTANCE.onChildAdded(child.getImplNodelet(), p.getImplNodelet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onChildRemoved(ContentElement p, ContentNode child) {
    ParagraphHelper.INSTANCE.onRemovingChild(child.getImplNodelet(), p.getImplNodelet());
  }

  @Override
  public void onAddedToParent(ContentElement element, ContentElement oldParent) {
    scheduleRenumber(element, element.getAttribute(Paragraph.INDENT_ATTR), false);
  }

  @Override
  public void onRemovedFromParent(ContentElement element, ContentElement newParent) {
    scheduleRenumber(element, null, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEmptied(ContentElement p) {
    maybeHandleEmptied(p);
  }

  private void scheduleRenderUpdate(ContentElement p) {
    assert p != null;
    elementsToRender.add(p);

    scheduleBatchRender();
  }

  private void scheduleRenumber(ContentElement p, String oldIndent, boolean asRemoved) {
    if (asRemoved) {
      renumberer.markRemoved(p);
    } else {
      renumberer.markDirty(p, oldIndent);
    }

    if (renumberer.renumberNeeded()) {
      scheduleBatchRender();
    }
  }

  private void scheduleBatchRender() {
    runner.scheduleFinally(batchRenderCommand);
  }

  private void maybeHandleEmptied(ContentElement p) {
    if (p.getImplNodelet().getChildCount() == 0) {
      ParagraphHelper.INSTANCE.onEmpty(p.getImplNodelet());
    }
  }

  @Override
  public void onActivatedSubtree(ContentElement p) {
    fanoutAttrs(p);
    if (p.getImplNodelet() != null) {
      ParagraphHelper.INSTANCE.onRepair(p);
    }
  }

}
