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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;

/**
 * Renderer for <template> nodes. Creates the top-level widget.
 *
 * @author jasvir@google.com (Jasvir Nagra)
 * @author ihab@google.com (Ihab Awad)
 * @author stay@google.com (Mike Stay)
 */
final class TemplateNodeMutationHandler extends GwtRenderingMutationHandler {

  interface PluginContextFactory {
    PluginContext create(ContentElement doodad);
  }

  /** Cajoler used to serve content into each doodad. */
  private final CajolerFacade cajoler;

  /** Factory for context objects for each doodad. */
  private final PluginContextFactory contextFactory;

  private TemplateNodeMutationHandler(CajolerFacade cajoler, PluginContextFactory contextFactory) {
    // TODO(ihab): The flow is not statically known for the arbitrary plugin
    // case. Discuss implications with danilatos@.
    super(Flow.INLINE);
    this.cajoler = cajoler;
    this.contextFactory = contextFactory;
  }

  /**
   * Creates a renderer for &lt;template&gt; doodads.
   */
  static TemplateNodeMutationHandler create() {
    final PartIdFactory partIdFactory = SessionPartIdFactory.get();
    CajolerFacade cajoler = CajolerFacade.instance();
    final JavaScriptObject taming = cajoler.getTaming();
    PluginContextFactory defaultFactory = new PluginContextFactory() {
      @Override
      public PluginContext create(ContentElement doodad) {
        return new PluginContext(doodad, partIdFactory, taming);
      }
    };
    return new TemplateNodeMutationHandler(cajoler, defaultFactory);
  }

  @Override
  protected Widget createGwtWidget(Renderable element) {
    HTML widget = new HTML();
    element.setProperty(HtmlTemplate.TEMPLATE_WIDGET, widget);

    // Do the things that the doodad API should be doing by default.
    // ContentElement attempts this, and fails, so we have to do this ourselves.
    widget.getElement().getStyle().setProperty("whiteSpace", "normal");
    widget.getElement().getStyle().setProperty("lineHeight", "normal");

    return widget;
  }

  @Override
  public void onActivatedSubtree(ContentElement doodad) {
    super.onActivatedSubtree(doodad);
    PluginContext pluginContext = contextFactory.create(doodad);
    doodad.setProperty(HtmlTemplate.TEMPLATE_PLUGIN_CONTEXT, pluginContext);
    cajoler.instantiateDoodadInHTML(
        doodad.getProperty(HtmlTemplate.TEMPLATE_WIDGET),
        pluginContext,
        doodad.getAttribute(HtmlTemplate.TEMPLATE_URL_ATTR));
    pluginContext.onActivated();
  }

  @Override
  public void onChildAdded(ContentElement htmlTemplate, ContentNode child) {
    super.onChildAdded(htmlTemplate, child);
    getContext(htmlTemplate).onHtmlTemplateChildAdded(child);
  }

  @Override
  public void onChildRemoved(ContentElement htmlTemplate, ContentNode child) {
    super.onChildRemoved(htmlTemplate, child);
    getContext(htmlTemplate).onHtmlTemplateChildRemoved(child);
  }

  @Override
  public void onDeactivated(ContentElement htmlTemplate) {
    super.onDeactivated(htmlTemplate);
    getContext(htmlTemplate).onDeactivated();
    htmlTemplate.setProperty(HtmlTemplate.TEMPLATE_PLUGIN_CONTEXT, null);
  }

  private PluginContext getContext(ContentElement experimental) {
    return experimental.getProperty(HtmlTemplate.TEMPLATE_PLUGIN_CONTEXT);
  }
}