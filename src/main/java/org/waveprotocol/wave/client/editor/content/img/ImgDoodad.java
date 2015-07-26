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

package org.waveprotocol.wave.client.editor.content.img;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;


/**
 * Temporary class to support rendering of wave-xml 'img' tags into html 'img' tags.
 * NOTE(patcoleman): The img tags in wave documents are marked to be removed, please use the
 * thumbnail doodad if possible.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class ImgDoodad {
  /** Util, so private constructor. */
  private ImgDoodad() { }

  private static String TAGNAME = "img";
  private static ReadableStringSet PERMITTED_ATTRIBUTES =
      CollectionUtils.newStringSet("alt", "height", "width", "src"); // @see SchemaConstraints

  private static ImgRenderer RENDERER = new ImgRenderer();

  /** Register the renderer and mutation handler. */
  public static void register(ElementHandlerRegistry registry) {
    registry.registerRenderingMutationHandler(TAGNAME, RENDERER);
    registry.registerEventHandler(TAGNAME, ChunkyElementHandler.get());
  }

  /**
   * Html renderer that renders the tags as identical, uneditable html 'img' tags.
   */
  private static class ImgRenderer extends RenderingMutationHandler {

    @Override
    public Element createDomImpl(Renderable element) {
      // implementation is to use img tag, set to uneditable so it can't be resized locally
      Element imgTag = Document.get().createImageElement();
      DomHelper.setContentEditable(imgTag, false, false);
      return imgTag;
    }

    @Override
    public void onActivationStart(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(ContentElement element,
        String name, String oldValue, String newValue) {
      if (PERMITTED_ATTRIBUTES.contains(name)) {
        if (newValue == null) {
          element.getImplNodelet().removeAttribute(name);
        } else {
          // NOTE(patcoleman): just copying the given url to display an externally hosted
          // image is bad...hence this class should be removed ASAP.
          element.getImplNodelet().setAttribute(name, newValue);
        }
      }
    }
  }
}
