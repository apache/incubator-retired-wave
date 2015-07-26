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

package org.waveprotocol.wave.client.doodad.link;

import org.waveprotocol.wave.client.doodad.suggestion.Suggestion;
import org.waveprotocol.wave.client.doodad.suggestion.SuggestionRenderer;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager.HasSuggestions;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.util.Box;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Collections;
import java.util.Map;

/**
 * Annotation handler for Links.
 *
 * TODO(user): Reenable this for the EditorTestHarness once dependency on
 * Gadgets is cleaned up.
 *
 * TODO(user): Write a test that ensures shift clicking on a link in firefox
 * opens it in a new window. This is prone to breakage if any of the link's
 * ancestors cancel bubble or prevent default on the click event.
 *
 */
public class LinkAnnotationHandler implements AnnotationMutationHandler {

  /**
   * Interface for extending the basic link annotation {@link PaintFunction}
   * behaviour
   */
  public interface LinkAttributeAugmenter {

    /**
     * @param annotations See {@link PaintFunction#apply(Map, boolean)}
     * @param isEditing See {@link PaintFunction#apply(Map, boolean)}
     * @param current The current map to be returned for
     *        {@link PaintFunction#apply(Map, boolean)}. The implementation may
     *        not alter this map. It may either return it as is, or return a new
     *        map with different values.
     * @return the values to use. It is fine to simply return an unmodified
     *         {@code current} or to return a new map
     */
    Map<String, String> augment(
        Map<String, Object> annotations, boolean isEditing, Map<String, String> current);
  }

  /** Set of all link keys */
  private static final ReadableStringSet KEYS = CollectionUtils.newStringSet(Link.LINK_KEYS);
  private static final ReadableStringSet BOUNDARY_KEYS = KEYS;

  /**
   * Create and register a link annotation handler
   *
   * @param registries set of editor registries
   * @param augmenter paint function with wave link handling logic
   */
  @SuppressWarnings("deprecation")
  public static void register(Registries registries,
      LinkAttributeAugmenter augmenter) {
    PainterRegistry painterRegistry = registries.getPaintRegistry();
    LinkAnnotationHandler handler =
        new LinkAnnotationHandler(painterRegistry.getPainter());

    AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();
    annotationRegistry.registerHandler(AnnotationConstants.LINK_PREFIX, handler);
    // Don't register behaviour on the link/auto key, since an external agent
    // puts it there resulting in surprising behaviour mid-typing (e.g. if
    // the text is bold, the bold will suddenly get ended because of the link)
    registerBehaviour(annotationRegistry, AnnotationConstants.LINK_PREFIX);
    registerBehaviour(annotationRegistry, AnnotationConstants.LINK_MANUAL);
    registerBehaviour(annotationRegistry, AnnotationConstants.LINK_WAVE);

    painterRegistry.registerPaintFunction(KEYS, new RenderFunc(augmenter));
    painterRegistry.registerBoundaryFunction(BOUNDARY_KEYS, boundaryFunc);
  }

  private static void registerBehaviour(AnnotationRegistry registry, String prefix) {
    registry.registerBehaviour(prefix,
        new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT) {
      @Override
      public BiasDirection getBias(final StringMap<Object> left, final StringMap<Object> right,
          CursorDirection cursor) {
        final Box<BiasDirection> ret = Box.create(BiasDirection.NEITHER);
        KEYS.each(new Proc() {
          @Override
          public void apply(String key) {
            if (left.get(key) != null) {
              ret.boxed = BiasDirection.RIGHT;
            } else if (right.get(key) != null) {
              ret.boxed = BiasDirection.LEFT;
            }
          }
        });
        return ret.boxed;
      }
      @Override
      public double getPriority() {
        return 10.0; // higher than elements.
      }
    });
  }

  private final AnnotationPainter painter;

  @SuppressWarnings("deprecation")
  public static String getLink(Map<String, Object> map) {
    Object ret = null;
    if (map.containsKey(AnnotationConstants.LINK_PREFIX)) {
      ret = Link.toHrefFromUri((String) map.get(AnnotationConstants.LINK_PREFIX));
    } else if (map.containsKey(AnnotationConstants.LINK_MANUAL)) {
        ret = Link.toHrefFromUri((String) map.get(AnnotationConstants.LINK_MANUAL));
    } else if (map.containsKey(AnnotationConstants.LINK_WAVE)) {
      // This is for backwards compatibility. Stop supporting AnnotationConstants.LINK_WAVE once
      // the data is cleaned.
      ret = Link.toHrefFromUri((String) map.get(AnnotationConstants.LINK_WAVE));
    } else if (map.containsKey(AnnotationConstants.LINK_AUTO)) {
      ret = Link.toHrefFromUri((String) map.get(AnnotationConstants.LINK_AUTO));
    }
    if (ret instanceof String) {
      return (String) ret;
    } else {
      return null;
    }
  }

  private static class RenderFunc implements PaintFunction{
    private final LinkAttributeAugmenter augmenter;

    public RenderFunc(LinkAttributeAugmenter augmenter) {
      this.augmenter = augmenter;
    }

    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      Map<String, String> ret;
      String content = getLink(from);
      if (content != null) {
        ret = Collections.singletonMap(AnnotationPaint.LINK_ATTR, content);
      } else {
        ret = Collections.emptyMap();
      }

      return augmenter.augment(from, isEditing, ret);
    }
  }

  private static final BoundaryFunction boundaryFunc = new BoundaryFunction() {
    public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
        N nodeAfter, Map<String, Object> before, Map<String, Object> after, boolean isEditing) {
      if (!isEditing) {
        return null;
      }

      Attributes attributes = Suggestion.maybeCreateSuggestions(before);

      if (attributes == null || attributes.isEmpty()) {
        return null;
      }

      E e = localDoc.transparentCreate(SuggestionRenderer.FULL_TAGNAME, attributes, parent,
          nodeAfter);
      if (e == null) {
        return null;
      }

      ContentElement contentElement = (ContentElement) e;
      HasSuggestions suggestion =
          contentElement.getProperty(SuggestionRenderer.HAS_SUGGESTIONS_PROP);
      contentElement.getSuggestionsManager().registerElement(suggestion);
      return e;
    }
  };

  /**
   * @param painter painter to use for rendering
   */
  public LinkAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }

  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }
}
