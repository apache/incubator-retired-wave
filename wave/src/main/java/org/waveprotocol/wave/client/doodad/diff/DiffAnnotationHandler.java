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

package org.waveprotocol.wave.client.doodad.diff;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter.DeleteInfo;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collections;
import java.util.Map;

/**
 * Defines behaviour for rendering diffs
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DiffAnnotationHandler implements AnnotationMutationHandler {
  /** Colour used for diff hilights of new content. */
  private static final String HILIGHT_COLOUR = "yellow";

  /** Annotation key prefix. */
  public static final String PREFIX = DiffHighlightingFilter.DIFF_KEY;

  /** Set of annotation keys that the paint function is interested in. */
  private final static ReadableStringSet PAINT_KEYS =
      CollectionUtils.newStringSet(DiffHighlightingFilter.DIFF_INSERT_KEY);

  /** Set of annotation keys that the boundary function is interested in. */
  private final static ReadableStringSet BOUNDARY_KEYS =
    CollectionUtils.newStringSet(DiffHighlightingFilter.DIFF_DELETE_KEY);

  /** Map of annotations for the diff paint renderer. */
  private final static Map<String, String> PAINT_PROPERTIES =
      Collections.singletonMap("backgroundColor", HILIGHT_COLOUR);

  /**
   * Create and register a style annotation handler
   *
   * @param annotationRegistry registry to register on
   * @param painterRegistry painter registry to use for rendering
   */
  public static void register(AnnotationRegistry annotationRegistry,
      PainterRegistry painterRegistry) {

    painterRegistry.registerPaintFunction(PAINT_KEYS, paintFunc);
    painterRegistry.registerBoundaryFunction(BOUNDARY_KEYS, boundaryFunc);

    annotationRegistry.registerHandler(PREFIX,
        new DiffAnnotationHandler(painterRegistry.getPainter()));
  }

  /** Painter to access regional repainting of diff areas. */
  private final AnnotationPainter painter;

  /**
   * Paint function for normal diffs, sets the background colour of the new
   * content .
   */
  private static final PaintFunction paintFunc = new PaintFunction() {
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      if (from.get(DiffHighlightingFilter.DIFF_INSERT_KEY) != null) {
        return PAINT_PROPERTIES;
      } else {
        return Collections.emptyMap();
      }
    }
  };

  /** Paint function for diff deletions. */
  private static final BoundaryFunction boundaryFunc = new BoundaryFunction() {
        public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
        N nodeAfter, Map<String, Object> before, Map<String, Object> after, boolean isEditing) {
      Object obj = after.get(DiffHighlightingFilter.DIFF_DELETE_KEY);
      if (obj != null) {
        // HACK(danilatos): Assume the elements are of this implementation.
        assert obj instanceof DeleteInfo : "delete key's value must be a DeleteInfo";

        // find the element, then set internal deleted content in the DOM
        E elt = localDoc.transparentCreate(DiffDeleteRenderer.FULL_TAGNAME,
            Collections.<String,String>emptyMap(), parent, nodeAfter);

        DiffDeleteRenderer.getInstance().setInnards((ContentElement) elt,
            ((DeleteInfo) obj).getDeletedHtmlElements());
        return elt;
      } else {
        return null;
      }
    }
  };

  /**
   * Construct the handler, registering its rendering functions with the painter.
   * @param painter painter to use for rendering
   */
  public DiffAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }

  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }
}
