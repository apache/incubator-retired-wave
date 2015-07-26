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

package org.waveprotocol.wave.client.editor.content.misc;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;

import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.DocumentContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for regular style annotations.
 *
 * Delegates to the painter to do rendering.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class StyleAnnotationHandler implements AnnotationMutationHandler {

  /**
   * Handy method for getting the style suffix, given a full annotation key
   * @param key
   * @return style suffix
   */
  public static final String suffix(String key) {
    return key.substring(AnnotationConstants.STYLE_PREFIX.length() + 1);
  }

  /**
   * Handy method for getting the full annotation key, given a style suffix
   * @param suffix
   * @return full annotation key
   */
  public static final String key(String suffix) {
    return AnnotationConstants.STYLE_PREFIX + "/" + suffix;
  }

  /**
   * Create and register a style annotation handler
   *
   * @param registries registry to register on
   * @return the new handler
   */
  public static void register(Registries registries) {
    PainterRegistry painterRegistry = registries.getPaintRegistry();
    StyleAnnotationHandler handler = new StyleAnnotationHandler(painterRegistry.getPainter());
    registries.getAnnotationHandlerRegistry().registerHandler(AnnotationConstants.STYLE_PREFIX, handler);
    registries.getAnnotationHandlerRegistry().registerBehaviour(AnnotationConstants.STYLE_PREFIX,
        new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
    painterRegistry.registerPaintFunction(AnnotationConstants.STYLE_KEYS, renderFunc);
  }

  private final AnnotationPainter painter;

  private static final PaintFunction renderFunc = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      Map<String, String> map = new HashMap<String, String>();
      for (Map.Entry<String, Object> entry : from.entrySet()) {
        if (entry.getKey().startsWith(AnnotationConstants.STYLE_PREFIX + "/")) {
          map.put(suffix(entry.getKey()), (String) entry.getValue());
        }
      }
      return map;
    }
  };

  /**
   * @param painter painter to use for rendering
   */
  public StyleAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }

  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }
}
