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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.ElementManager;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.util.ConcurrentSet;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for painting annotations that need simple stylistic renderings.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class AnnotationPainter {

  /**
   * Property on the ContentDocument to indicate isEditing state;
   *
   * If non-null the editor is in editing mode, else it in non-editing mode.
   */
  public static final Property<Boolean> DOCUMENT_MODE =
      Property.immutable("doc_mode");

  public static <N, E extends N, T extends N> boolean isEditing(LocalDocument<N, E, T> doc) {
    return isInEditingDocument(doc, doc.getDocumentElement());
  }

  public static <E> boolean isInEditingDocument(ElementManager<E> mgr, E element) {
    return Boolean.TRUE.equals(mgr.getProperty(AnnotationPainter.DOCUMENT_MODE, element));
  }

  /**
   * Max "units of work" per render pass, before we defer. We want this to be
   * a fair bit bigger than 1, because of the startup cost before we actually
   * start doing said units of work.
   */
  private static final int MAX_RUN_ITERATIONS = 80;

  private static final int MANY_ITERATIONS = 2000;

  /**
   * Per-document paint worker
   *
   * Public API allows registering of per-document functions & keys, as opposed
   * to global ones on the annotation painter.
   */
  public static class DocPainter<N, E extends N, T extends N> {

    // protect the task from the public api as a member variable
    private final Scheduler.IncrementalTask task = new Scheduler.IncrementalTask() {
      public boolean execute() {
        return doRun(MAX_RUN_ITERATIONS);
      }
    };

    // Aliases for parts of the bundle we need.
    private final LocalDocument<N, E, T> localDoc;
    private final LocationMapper<N> mapper;
    private final TextNodeOrganiser<T> textNodeOrganiser;
    private final ReadableDocumentView<N, E, T> persistentView;
    private final ReadableDocumentView<N, E, T> hardView;
    private final MutableAnnotationSet.Local localAnnotations;

    private final PainterRegistry paintRegistry;

    // State vars. Reinitialised on each call to execute().
    private HashMap<String, Object> currentValues;
    private int startLocation, endLocation;
    private int chunkEnd;
    private AnnotationCursor cursor;
    private ReadableStringSet nextChangingKeys;
    private Map<String, String> renderAttrs;

    private boolean dead = false;

    Map<String, Object> boundaryBefore;
    Map<String, Object> boundaryAfter;

    private DocPainter(DocumentContext<N, E, T> bundle, PainterRegistry paintRegistry) {
      localDoc = bundle.annotatableContent();
      mapper = bundle.locationMapper();
      textNodeOrganiser = bundle.textNodeOrganiser();
      persistentView = bundle.persistentView();
      hardView = bundle.hardView();
      localAnnotations = bundle.localAnnotations();
      this.paintRegistry = paintRegistry;
    }

    private boolean doRun(final int maxIterations) {
      if (dead) {
        return false;
      }

      int docSize = mapper.size();
      int maybeLocation = localAnnotations.firstAnnotationChange(
          0, docSize, REPAINT_KEY, null);

      if (maybeLocation == -1) {
        maybeScheduledPainters.remove(this);
        return false;
      }

      Point<N> point = mapper.locate(maybeLocation);
      E containingAnnotator = getAnnotatingElement(point.getCanonicalNode());

      N startNode;

      if (containingAnnotator != null) {

        // TODO(danilatos): Optimise by using equality comparison with an end node, but
        // be careful because sometimes the code below skips several nodes at a time, so
        // maybe still do a location comparison in those cases.
        N first = persistentView.getFirstChild(containingAnnotator);
        N last = persistentView.getLastChild(containingAnnotator);

        assert first != null : "We're supposed to be in this node, so it has at least one child";

        startLocation = mapper.getLocation(first);
        // Mark the entire range covered by the current bit of paint, in case we want to change
        // it and its range exceeds the currently marked repaint range.
        localAnnotations.setAnnotation(startLocation, mapper.getLocation(
            Point.after(persistentView, last)), REPAINT_KEY, "y");

        startNode = containingAnnotator;
      } else {
        startNode = ensureNodeBoundary(point);
        startLocation = maybeLocation;
      }

      // Node we are up to, our "iterator" value.
      N currentNode = startNode;

      int remainingIterations = maxIterations;

      endLocation = getEnd(startLocation, docSize, REPAINT_KEY, "y");

      ReadableStringSet allKeys = getKeys();
      nextChangingKeys = allKeys;
      chunkEnd = startLocation;
      currentValues = new HashMap<String, Object>();
      cursor = localAnnotations.annotationCursor(startLocation, endLocation, allKeys);
      progress();

      N chunkEndNode = ensureNodeBoundary(mapper.locate(chunkEnd)); // exclusive

      E lastBoundaryElement = null;
      while (true) {

        int currentLocation = currentNode == null ? docSize
            : DocHelper.getFilteredLocation(mapper, persistentView,
                Point.before(localDoc, currentNode));

        while (chunkEnd <= currentLocation && chunkEnd < endLocation) {
          Point<N> boundaryPoint = mapper.locate(chunkEnd);
          progress();
          chunkEndNode = ensureNodeBoundary(mapper.locate(chunkEnd));

          // Do boundary rendering
          E boundaryParent;
          N boundaryNodeAfter;
          // Convert a text point to a parent/nodeAfter pair
          if (boundaryPoint.isInTextNode()) {
            T textNode = hardView.asText(boundaryPoint.getContainer());
            boolean isAtStartOfTextNode = boundaryPoint.getTextOffset() == 0;
            assert isAtStartOfTextNode ||
                boundaryPoint.getTextOffset() == localDoc.getLength(textNode)
                    : "Boundary point not at node boundary! "
                      + localDoc.getData(textNode) + ":" + boundaryPoint.getTextOffset();
            // (a) NOTE(danilatos): This slicing (and in the else block) is so that we
            // can make the assumption later on, see corresponding (a) below.
            boundaryNodeAfter = localDoc.transparentSlice(
                isAtStartOfTextNode ? textNode : hardView.getNextSibling(textNode));
            boundaryParent = boundaryNodeAfter != null
                ? localDoc.getParentElement(boundaryNodeAfter)
                : hardView.getParentElement(textNode);
          } else {
            boundaryNodeAfter = boundaryPoint.getNodeAfter();
            if (boundaryNodeAfter != null) {
              boundaryNodeAfter = localDoc.transparentSlice(boundaryNodeAfter);
            }
            boundaryParent = boundaryNodeAfter != null
                ? localDoc.getParentElement(boundaryNodeAfter)
                : localDoc.asElement(boundaryPoint.getContainer());
          }
          // maybe create a boundary element
          lastBoundaryElement = getBoundaryElement(boundaryParent, boundaryNodeAfter);

          if (chunkEnd == currentLocation) {
            break;
          }
        }

        if (currentLocation >= endLocation || remainingIterations <= 0) {
          localAnnotations.setAnnotation(startLocation, currentLocation, REPAINT_KEY, null);
          // TODO(danilatos): Conditionally break only if i >= maxIterations, otherwise
          // find next range to repaint.
          break;
        }

        N next;
        E element = localDoc.asElement(currentNode);
        if (element == null) {
          // Wrap adjacent text nodes up
          N fromIncl = currentNode;
          N toExcl = fromIncl;
          while (localDoc.asText(toExcl = localDoc.getNextSibling(toExcl)) != null) {
            if (localDoc.isSameNode(chunkEndNode, toExcl)) {
              break;
            }
          }
          if (renderAttrs.size() > 0) {
            next = getNextNode(wrap(renderAttrs, fromIncl, toExcl));
          } else {
            next = toExcl != null ? toExcl : getNextNode(localDoc.getParentElement(currentNode));
          }
        } else if (isPaintElement(element)) {
          // TODO(danilatos): passing a transparent element to a regular traversal method.
          // This will currently work, but we might get exceptions thrown if we add that
          // to the behaviour of filtered view. The intended behaviour here is that
          // we get the last visible node that is a child of element, or null if none.
          N firstPersistentChild = hardView.getFirstChild(element);
          if (firstPersistentChild == null) {
            next = getFirstNode(element);
            localDoc.transparentUnwrap(element);
          } else {
            // Again, same here
            N lastPersistentChild = hardView.getLastChild(element);
            int annotatorEnd = mapper.getLocation(Point.after(persistentView, lastPersistentChild));

            if (annotatorEnd > chunkEnd || !rendersSame(renderAttrs, element)) {
              // If this node is stale, or
              // if this node was annotating a range further than the next render change,
              // then it must be removed and be replaced by smaller bits. We also need to
              // mark its encompassing range as to-be-repainted.
              next = getFirstNode(element);
              localDoc.transparentUnwrap(element);
              localAnnotations.setAnnotation(currentLocation, annotatorEnd, REPAINT_KEY, "y");
            } else {
              // Otherwise, its range is done, so just skip it
              next = getNextNode(element);

              // (a) Assumption about boundary elements not being inside paint elements
              // allows us to safely skip over the current element, a nice optimisation.
              // This will not be as easy later when we have prioritised paint nodes.
              // See corresponding (a) above for why we can make this assumption.
            }
          }
        } else if (isBoundaryElement(element)) {
          next = getNextNode(element);

          // If it's a boundary element, we want to strip it out, unless it's one we just
          // made. Ones we made earlier are further back, so it shouldn't be possible that
          // we've come across one of those. It might not even be possible that we even
          // come across the one we just made...
          // TODO(danilatos): Test if this check is necessary (and/or sufficient...)
          if (element != lastBoundaryElement) {
            localDoc.transparentDeepRemove(element);
          }
        } else {
          next = DocHelper.getNextNodeDepthFirst(localDoc, element, null, true);
        }

        currentNode = next;
        remainingIterations--;
      }

      return true;
    }

    private void progress() {
      ReadableStringSet changingKeys;

      final int start = chunkEnd;

      if (!cursor.hasNext()) {
        chunkEnd = endLocation;
        changingKeys = null;
      } else {
        changingKeys = cursor.nextLocation();
        chunkEnd = cursor.currentLocation();
      }

      // TODO(danilatos): More efficient, too much hashmap munging.
      boundaryBefore = new HashMap<String, Object>();
      boundaryAfter = new HashMap<String, Object>();

      nextChangingKeys.each(new Proc() {
        @Override
        public void apply(String key) {
          Object newValue = localAnnotations.getAnnotation(start, key);

          boundaryBefore.put(key, currentValues.get(key));
          boundaryAfter.put(key, newValue);

          if (newValue == null) {
            currentValues.remove(key);
          } else {
            currentValues.put(key, newValue);
          }
        }
      });

      nextChangingKeys = changingKeys;
      computeRenderAttrs();
    }

    private int getEnd(int start, int end, String key, Object fromValue) {
      int ret = localAnnotations.firstAnnotationChange(start, end, key, fromValue);
      return ret == -1 ? end : ret;
    }

    private boolean rendersSame(Map<String, String> attrs, E annotatingElement) {
      return attrs != null && attrs.equals(localDoc.getAttributes(annotatingElement));
    }

    private N getNextNode(N node) {
      return DocHelper.getNextNodeDepthFirst(localDoc, node, null, false);
    }

    private N getFirstNode(N node) {
      return DocHelper.getNextNodeDepthFirst(localDoc, node, null, true);
    }

    /**
     * Ensures the given point is at a node boundary, possibly splitting a text
     * node in order to do so, in which case a new point is returned.
     *
     * @param point
     * @return a point at the same place as the input point, guaranteed to be at
     *         a node boundary.
     */
    private N ensureNodeBoundary(Point<N> point) {
      return DocHelper.ensureNodeBoundaryReturnNextNode(point, localDoc, textNodeOrganiser);
    }

    private E wrap(Map<String, String> attrs, N fromIncl, N toExcl)  {
      E el = localDoc.transparentCreate(paintRegistry.getPaintTagName(), attrs,
          localDoc.getParentElement(fromIncl), fromIncl);
      localDoc.transparentMove(el, fromIncl, toExcl, null);
      return el;
    }

    private boolean isPaintElement(E element) {
      return localDoc.getTagName(element).equals(paintRegistry.getPaintTagName());
    }

    private boolean isBoundaryElement(E element) {
      return localDoc.getTagName(element).equals(paintRegistry.getBoundaryTagName());
    }

    private E getAnnotatingElement(N node) {
      for (E parent = localDoc.getParentElement(node); parent != null;
          parent = localDoc.getParentElement(parent)) {
        if (isPaintElement(parent)) {
          return parent;
        }
      }

      return null;
    }

    private void computeRenderAttrs() {
      renderAttrs = new HashMap<String, String>();

      boolean isEditing = isEditing(localDoc);
      for (PaintFunction func : paintRegistry.getPaintFunctions()) {
        // TODO(danilatos): Make this better by hiding keys the function did not
        // register for, and making the input map unchangeable by the fucntion.
        renderAttrs.putAll(func.apply(currentValues, isEditing));
      }
    }

    /**
     * Maybe create a boundary element at the given point.
     *
     * @param parent
     * @param nodeAfter
     * @return a boundary rendering element, or null if none needed here
     */
    private E getBoundaryElement(E parent, N nodeAfter) {
      // The current parent our boundary functions are putting children in
      E currentParent = parent;
      // The boundary element. We lazily create it; the first time a function
      // returns non-null, we create the element, put it in place, put the
      // function's returned element as a child, and make the boundary element
      // the current parent, so subsequent elements go into this container.
      E boundaryContainerElement = null;

      boolean isEditing = isEditing(localDoc);
      for (BoundaryFunction func : paintRegistry.getBoundaryFunctions()) {
        E result = func.apply(localDoc, currentParent, nodeAfter, boundaryBefore, boundaryAfter,
            isEditing);
        if (result != null && boundaryContainerElement == null) {
          boundaryContainerElement = localDoc.transparentCreate(paintRegistry.getBoundaryTagName(),
              Collections.<String, String>emptyMap(), currentParent, nodeAfter);
          currentParent = boundaryContainerElement;
          nodeAfter = null;
          localDoc.transparentMove(currentParent, result, localDoc.getNextSibling(result), null);
          PersistentContent.makeDeepTransparent(localDoc, boundaryContainerElement);
        }
      }

      return boundaryContainerElement;
    }

    private ReadableStringSet getKeys() {
      return paintRegistry.getKeys();
    }
  }

  private static final Property<AnnotationPainter> PAINTER_PROP =
      Property.mutable("annotation-painter");

  private static final String REPAINT_KEY = Annotations.makeUniqueLocal("paint");

  private static final Property<DocPainter<?,?,?>> DOC_PAINTER_PROP =
      Property.mutable("doc-annotation-painter");

  /**
   * Function for mapping any annotation key-value pairs to paint render attributes
   */
  public static interface PaintFunction {
    Map<String, String> apply(Map<String, Object> from, boolean isEditing);
  }

  /**
   * Function for mapping any change in annotation key-value pairs to boundary
   * elements to be inserted in the html (tracked by wrapper nodse, of course)
   */
  // TODO(danilatos): Also handle things like specifying left/right border on an
  // adjacent paint element? Or is that more a paint style implemented cleverly?
  public static interface BoundaryFunction {
    /**
     * Callback for rendering boundary elements.
     *
     * Must only create an element at the given point. Must return the created element,
     * or null if none created.
     *
     * @param localDoc
     * @param parent parent of to-be-created element
     * @param nodeAfter next sibling of to-be-created-element
     * @param before map of annotation values before the boundary
     * @param after map of annotation values after the boundary
     * @return the created element, or null if nothing created
     */
    // TODO(danilatos): This is a potentially dangerous method if implemented incorrectly.
    // Find a way to make it safe without the API becoming too cumbersome.
    <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent, N nodeAfter,
        Map<String, Object> before, Map<String, Object> after, boolean isEditing);
  }

  private static final ConcurrentSet<DocPainter<?, ?, ?>> maybeScheduledPainters
      = ConcurrentSet.create();

  private final TimerService scheduler;

  /**
   * @param scheduler Used for asynchronously repainting
   */
  public AnnotationPainter(TimerService scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Same as {@link #scheduleRepaint(DocumentContext, int, int)}, but attempts
   * to find a painter for the given document context, and will only schedule a
   * repaint if it finds one.
   */
  public static <N, E extends N, T extends N> void maybeScheduleRepaint(
      DocumentContext<N, E, T> bundle, int start, int end) {

    AnnotationPainter painter = bundle.elementManager().getProperty(
        PAINTER_PROP, bundle.document().getDocumentElement());

    if (painter != null) {
      painter.scheduleRepaint(bundle, start, end);
    }
  }

  private static <N, E extends N, T extends N> void setPainterProp(DocumentContext<N, E, T> bundle,
      AnnotationPainter painter) {
    E docElement = bundle.document().getDocumentElement();
    bundle.elementManager().setProperty(PAINTER_PROP, docElement, painter);
  }

  /**
   * Don't use this unless you are EditorImpl code. Using it indiscriminantly
   * can cause lots of problems and bugs.
   */
  public static <N, E extends N, T extends N> boolean repaintNow(DocumentContext<N, E, T> bundle) {
    E docElement = bundle.document().getDocumentElement();

    DocPainter<?, ?, ?> docPainter = bundle.elementManager().getProperty(
        DOC_PAINTER_PROP, docElement);

    if (docPainter != null) {
      return docPainter.doRun(MAX_RUN_ITERATIONS);
    } else {
      return false;
    }
  }

  /**
   * Don't use this unless you are playback code. Using it indiscriminantly
   * can cause lots of problems and bugs.
   */
  public static void hackFlush() {
    maybeScheduledPainters.lock();
    try {
      for (DocPainter<?, ?, ?> docPainter : maybeScheduledPainters) {
        flush(docPainter);
      }
    } finally {
      maybeScheduledPainters.unlock();
    }
  }

  /**
   * Flushes any painting scheduled for a document.
   *
   * @param context  document to paint
   */
  public static <N> void flush(DocumentContext<N, ?, ?> context) {
    flush(getDocPainter(context));
  }

  /**
   * Runs a painter until completion.
   *
   * @param painter painter to run
   */
  private static void flush(DocPainter<?, ?, ?> painter) {
    while (painter.doRun(MANY_ITERATIONS)) { }
  }

  private void schedule(DocPainter<?, ?, ?> docPainter) {
    scheduler.schedule(docPainter.task);
    maybeScheduledPainters.add(docPainter);
  }

  /**
   * Mark a region of the document as stale and in need of a repaint
   *
   * @param bundle everything we need to know about the current document
   * @param start as per defined annotation range semantics
   * @param end as per defined annotation range semantics
   */
  public <N, E extends N, T extends N> void scheduleRepaint(
      DocumentContext<N, E, T> bundle, int start, int end) {
    setPainterProp(bundle, this);

    // Expand re-render range by 3, so that boundary decorators will get rendered if at paint
    // range boundaries. (Maybe do a nicer way later).
    // HACK(user): Turns out 1 wasn't enough, not sure why but seems like the
    // boundary node is not directly next to the annotated region, work out
    // exactly what number to use here.
    int size = bundle.document().size();
    end = Math.min(size, end + 3);
    start = Math.max(0, start - 3);

    if (start == end) {
      if (start == 0) {
        if (end < size) {
          end++;
        }
      } else {
        start--;
      }
    }

    assert start >= 0 && end >= start && size >= end;

    bundle.localAnnotations().setAnnotation(start, end, REPAINT_KEY, "y");
    DocPainter<?, ?, ?> docPainter = getDocPainter(bundle);
    schedule(docPainter);
  }

  /**
   * Return the doc painter for the given document context, perhaps creating
   * it if one did not already exist
   *
   * @param bundle document context
   * @return doc painter for given context
   */
  public static <N, E extends N, T extends N> DocPainter<?, ?, ?> getDocPainter(
      DocumentContext<N, E, T> bundle) {
    E docElement = bundle.document().getDocumentElement();
    DocPainter<?, ?, ?> docPainter = bundle.elementManager().getProperty(
        DOC_PAINTER_PROP, docElement);
    // HACK(user): Initializing this is tricky. We set this property in
    // EditorImpl as soon as we have a document element. However, the document
    // element is only constructed when we consume the initial operations.
    // The initial operations trigger annotation handling which expect the
    // DocPainter to be set. Thus, we lazily create a temporary DocPainter,
    // until the real one is ready.
    if (docPainter == null) {
      docPainter = new DocPainter<N, E, T>(bundle, Editor.ROOT_PAINT_REGISTRY);
      bundle.elementManager().setProperty(DOC_PAINTER_PROP, docElement, docPainter);
    }
    return docPainter;
  }

  public static <N, E extends N, T extends N> void createAndSetDocPainter(
      DocumentContext<N, E, T> bundle, PainterRegistry painterRegistry) {
    E docElement = bundle.document().getDocumentElement();

    DocPainter<?, ?, ?> existing = bundle.elementManager().getProperty(
        DOC_PAINTER_PROP, docElement);

    // Cleanup existing if exists
    if (existing != null) {
      existing.dead = true;
    }

    DocPainter<?, ?, ?> docPainter = new DocPainter<N, E, T>(bundle, painterRegistry);
    bundle.elementManager().setProperty(DOC_PAINTER_PROP, docElement, docPainter);
  }

  public static <N, E extends N, T extends N> void clearDocPainter(
      DocumentContext<N, E, T> bundle) {

    DocPainter<?, ?, ?> existing = bundle.elementManager().getProperty(
        DOC_PAINTER_PROP, bundle.document().getDocumentElement());

    // Cleanup existing if exists
    if (existing != null) {
      existing.dead = true;
    }
  }

  /**
   * Register paint rendering behaviour
   *
   * @param dependentKeys
   * @param function
   * @deprecated
   */
  @Deprecated
  public void registerPaintFunctionz(ReadableStringSet dependentKeys, PaintFunction function) {
    Editor.ROOT_PAINT_REGISTRY.registerPaintFunction(dependentKeys, function);
  }

  /**
   * Register boundary rendering behaviour
   *
   * @param dependentKeys
   * @param function
   * @deprecated
   */
  @Deprecated
  public void registerBoundaryFunctionz(
      ReadableStringSet dependentKeys, BoundaryFunction function) {
    Editor.ROOT_PAINT_REGISTRY.registerBoundaryFunction(dependentKeys, function);
  }
}
