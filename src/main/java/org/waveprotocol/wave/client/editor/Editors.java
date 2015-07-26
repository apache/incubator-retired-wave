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

package org.waveprotocol.wave.client.editor;

import static org.waveprotocol.wave.client.editor.Editor.ROOT_HANDLER_REGISTRY;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.doodad.form.FormDoodads;
import org.waveprotocol.wave.client.editor.content.ContentDocElement;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.img.ImgDoodad;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.Caption;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.extract.ImeExtractor;
import org.waveprotocol.wave.client.editor.selection.content.ValidSelectionStrategy;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Constructors and static initialisers for editors
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class Editors {

  private Editors() {
  }

  /**
   * Creates an editor.
   *
   * @return New Editor instance
   */
  public static Editor create() {
    Element e = Document.get().createDivElement();
    e.setClassName("editor");
    return UserAgent.isMobileWebkit() ? // \u2620
        new EditorImplWebkitMobile(true, e) : new EditorImpl(true, e);
  }

  /**
   * Creates a new editor attached to an existing document.
   * {@link Editor#reset() Resetting} this editor will preserve the document to
   * which it is attached.
   *
   * @return New Editor instance
   */
  public static Editor attachTo(ContentDocument doc) {
    Element e = doc.getFullContentView().getDocumentElement().getImplNodelet();
    Preconditions.checkArgument(e != null);
    e = e.getParentElement();
    Preconditions.checkArgument(e != null);
    EditorImpl editor =
        UserAgent.isMobileWebkit() ? new EditorImplWebkitMobile(false, e) : new EditorImpl(
            false, e);
    editor.setContent(doc);
    return editor;
  }

  static {
    // TODO(danilatos): Get rid of this. It initialises a minimal bunch of
    // doodads, but it's kinda hacky and kludgy, and there are better ways.
    // The root registries should always be empty (with perhaps the sole
    // exception of the IME extractor)
    initRootRegistries();
  }

  private static boolean rootRegistriesInitialised;

  public static void initRootRegistries() {
    if (rootRegistriesInitialised) {
      return;
    }
    rootRegistriesInitialised = true;

    // TODO(danilatos/patcoleman): Fix up this kludge
    Editor.TAB_TARGETS.addAll(CollectionUtils.newStringSet(
        Caption.TAGNAME, "profile-field", "text-setting"));

    ImeExtractor.register(ROOT_HANDLER_REGISTRY);
    ContentDocElement.register(ROOT_HANDLER_REGISTRY, ContentDocElement.DEFAULT_TAGNAME);
    Paragraph.register(ROOT_HANDLER_REGISTRY);
    LineRendering.registerLines(ROOT_HANDLER_REGISTRY);

    Caption.register(ROOT_HANDLER_REGISTRY);
    ChunkyElementHandler.register("br", ROOT_HANDLER_REGISTRY);
    AnnotationPaint.register(ROOT_HANDLER_REGISTRY);
    ImgDoodad.register(ROOT_HANDLER_REGISTRY);

    FormDoodads.register(ROOT_HANDLER_REGISTRY);

    // after registries, set selection information:
    ValidSelectionStrategy.registerTagForSelections(
        LineContainers.PARAGRAPH_FULL_TAGNAME, false, Skip.NONE);
    ValidSelectionStrategy.registerTagForSelections(
        AnnotationPaint.SPREAD_FULL_TAGNAME, false, Skip.SHALLOW);
    ValidSelectionStrategy.registerTagForSelections(
        LineContainers.LINE_TAGNAME, true, Skip.DEEP);
  }
}
