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

package org.waveprotocol.wave.client.doodad.title;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;

import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collections;
import java.util.Map;

/**
 * Handler for title annotation.
 *
 * Delegates to the painter to do rendering.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public class TitleAnnotationHandler implements AnnotationMutationHandler {

  private static final String PREFIX = "conv";

  /** Map of annotations for the title paint renderer. */
  private final static Map<String, String> PAINT_PROPERTIES = Collections.singletonMap(
      "fontWeight", "bold");

  private static final String KEY = "conv/title";
  private static final ReadableStringSet KEYS = CollectionUtils.newStringSet(KEY);

  private final AnnotationPainter painter;

  private static final PaintFunction renderFunc = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      if (from.get(KEY) != null) {
        return PAINT_PROPERTIES;
      } else {
        return Collections.emptyMap();
      }
    }
  };

  /**
   * @param painter painter to use for rendering
   */
  public TitleAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }

  /**
   * Creates and registers a title annotation handler
   *
   * @param registries registry to register on
   * @return the new handler
   */
  public static void register(Registries registries) {
    PainterRegistry painterRegistry = registries.getPaintRegistry();
    TitleAnnotationHandler handler = new TitleAnnotationHandler(painterRegistry.getPainter());
    registries.getAnnotationHandlerRegistry().registerHandler(PREFIX, handler);
    registries.getAnnotationHandlerRegistry().registerBehaviour(PREFIX,
        new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
    painterRegistry.registerPaintFunction(KEYS, renderFunc);
  }

  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }
}
