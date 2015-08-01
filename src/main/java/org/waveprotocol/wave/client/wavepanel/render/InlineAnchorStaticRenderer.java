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

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.GlobalInstaller;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.model.conversation.Blips;

/**
 * Renders an inline anchor as a placeholder element.
 *
 */
// Example:
// <reply id="b+g>e"></reply>
// is rendered as:
// <reply ...>b+g&gt;e</reply>
// The <reply> element is intended to be substituted later for the real UI of
// an anchored inline reply.
public class InlineAnchorStaticRenderer extends RenderingMutationHandler {
  private InlineAnchorStaticRenderer() {
  }

  public static GlobalInstaller installer() {
    return new GlobalInstaller() {
      public void install(org.waveprotocol.wave.client.editor.content.Registries registries) {
        InlineAnchorStaticRenderer renderer = new InlineAnchorStaticRenderer();
        registries.getElementHandlerRegistry().registerRenderingMutationHandler(
            Blips.THREAD_INLINE_ANCHOR_TAGNAME, renderer);
      }
    };
  }

  @Override
  public Element createDomImpl(Renderable element) {
    Element e = Document.get().createElement(Blips.THREAD_INLINE_ANCHOR_TAGNAME);
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
      element.getImplNodelet().setInnerText(id);
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
    throw new UnsupportedOperationException(
        "this render only supports static rendering, not live rendering");
  }
}
