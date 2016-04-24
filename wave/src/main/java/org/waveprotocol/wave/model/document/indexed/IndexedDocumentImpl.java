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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet.AnnotationEndEvent;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet.AnnotationEvent;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet.AnnotationStartEvent;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.Automatons;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.NindoValidator;
import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;
import org.waveprotocol.wave.model.document.operation.algorithm.AnnotationsNormalizer;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.operation.impl.UncheckedDocOpBuffer;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.util.AnnotationIntervalImpl;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DocOpScrub;
import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.RangedAnnotationImpl;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.OpCursorException;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.util.Box;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.EvaluableOffsetList;
import org.waveprotocol.wave.model.util.OffsetList;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An implementation of IndexedDocument with an associative operator for
 * hashing.
 *
 * TODO(user): This doesn't yet do proper error checking. We need to
 * implement proper error checking.
 *
 * TODO(user): Write more tests for this class.
 *
 * TODO(user): Optimise skip for small skips.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author alexmah@google.com (Alexandre Mah)
 *
 * @param <N> The type of DOM nodes.
 * @param <E> The type of DOM Element nodes.
 * @param <T> The type of DOM Text nodes.
 * @param <V> The type of result that the document evaluates to.
 */
public class IndexedDocumentImpl<N, E extends N, T extends N, V>
    implements IndexedDocument<N, E, T>, Validator {

  /**
   * Whether to perform validation on consumed ops and nindos
   *
   * This should be on, except if
   * - profiling shows that it is a bottleneck AND we have no bugs
   * - a test case wants to explicitly validate separately to do better error reporting
   */
  public static boolean performValidation = true;

  /**
   * A node-finding action that returns a point representing the location at
   * which the action is performed.
   *
   * The "natural" bias is to prefer text node point to parent-nodeAfter points,
   * and to prefer text node ends to text node beginnings. This results in things
   * working more smoothly with typing and annotations.
   *
   * TODO(danilatos): Separate this concern out?
   */
  private final OffsetList.LocationAction<N, Point<N>> pointFinder
      = new OffsetList.LocationAction<N, Point<N>>() {

    public Point<N> performAction(OffsetList.Container<N> container, int offset) {
      N domNode = container.getValue();
      Point<N> maybeAdjust;
      if (domNode == null) {
        if (container == offsetList.sentinel()) {
          return Point.<N>end(substrate.getDocumentElement());
        }
        E element = getParentOf(container);
        maybeAdjust = maybeTextNodeEnd(getLastChild(element));
        return maybeAdjust != null ? maybeAdjust : Point.<N>end(element);
      } else if (substrate.asElement(domNode) != null) {
        assert offset == 0;
        maybeAdjust = maybeTextNodeEnd(getPreviousSibling(domNode));
        return maybeAdjust != null ? maybeAdjust : Point.before(substrate, domNode);
      } else {
        if (offset == 0) {
          maybeAdjust = maybeTextNodeEnd(getPreviousSibling(domNode));
          return maybeAdjust != null ? maybeAdjust : Point.inText(domNode, offset);
        } else {
          return Point.inText(domNode, offset);
        }
      }
    }

    private Point<N> maybeTextNodeEnd(N node) {
      T textNode = asText(node);
      return textNode == null ? null : Point.<N>inText(textNode, getLength(textNode));
    }

  };

  /**
   * An action that, when performed at a location in this document, will set the
   * currentContainer and currentOffset fields of this object to correspond to
   * the location.
   */
  private final OffsetList.LocationAction<N,Void> locationUpdater =
      new OffsetList.LocationAction<N, Void>() {

    public Void performAction(OffsetList.Container<N> container, int offset) {
      currentContainer = container;
      currentOffset = offset;
      currentParent = getParentOf(container);
      return null;
    }

  };

  /**
   * For handling the DOM structure of this document.
   */
  private final RawDocument<N, E, T> substrate;

  /**
   * An offset list for tracking the offsets of parts of the document.
   */
  private final EvaluableOffsetList<N, V> offsetList;

  /**
   * The current location of the pointer in the document.
   */
  private int currentLocation;

  /**
   * The container where the current location resides.
   */
  private OffsetList.Container<N> currentContainer;

  /**
   * The offset of the current location in the current container.
   */
  private int currentOffset;

  /**
   * The parent of the current DOM node. This may become inaccurate when the
   * deletion of an element is in progress.
   */
  private E currentParent;

  /**
   * The current depth of structural modifications.
   */
  private int deletionDepth;

  /**
   * Annotation set datastructure to delegate to
   */
  private final RawAnnotationSet<Object> annotations;

  /**
   * Schema constraints
   */
  private final DocumentSchema schemaConstraints;

  /**
   * Automaton interface for validity checking
   */
  private final AutomatonDocument autoDoc = Automatons.fromReadable(this);

  /**
   * @param substrate raw dom document to use
   * @param rawAnnotations raw annotations to use
   */
  public IndexedDocumentImpl(RawDocument<N, E, T> substrate,
      RawAnnotationSet<Object> rawAnnotations, DocumentSchema constraints) {
    Preconditions.checkNotNull(constraints,
        "Null schema not allowed, use DocumentSchema.NO_SCHEMA_CONSTRAINTS");
    this.schemaConstraints = constraints;

    annotations = rawAnnotations != null
        ? rawAnnotations : new StubModifiableAnnotations<Object>();

    this.substrate = substrate;
    offsetList = new EvaluableOffsetList<N, V>(null);

    assert size() == 0;

    indexChildren(substrate.getDocumentElement());

    resetLocation();

    if (offsetList.size() > 0) {
      annotations.begin();
      annotations.insert(offsetList.size());
      annotations.finish();
    }

    assert substrate.getFirstChild(substrate.getDocumentElement()) != null || size() == 0;
  }


  /**
   * Indexes an element and its contents.
   *
   * @param element The element to index.
   */
  private void indexElement(E element) {
    OffsetList.Container<N> sentinel = offsetList.sentinel();
    insertBefore(sentinel, element, 1);
    indexChildren(element);
    sentinel.insertBefore(null, 1);
  }

  private void indexChildren(E element) {
    OffsetList.Container<N> sentinel = offsetList.sentinel();
    for (N child = substrate.getFirstChild(element); child != null;
        child = substrate.getNextSibling(child)) {
      E childElement = substrate.asElement(child);
      if (childElement != null) {
        indexElement(childElement);
      } else {
        T childText = substrate.asText(child);
        insertBefore(sentinel, childText, substrate.getLength(childText));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public Point<N> locate(int location) {
    Preconditions.checkPositionIndex(location, offsetList.size());
    return offsetList.performActionAt(location, pointFinder);
  }

  /**
   * {@inheritDoc}
   */
  public int getLocation(N node) {
    Preconditions.checkNotNull(node, "Cannot get the location of a null node");
    OffsetList.Container<N> indexingContainer = substrate.getIndexingContainer(node);
    if (indexingContainer == null) {
      throw new IllegalArgumentException("getLocation: node has no indexing container - " + node);
    }
    if (indexingContainer.getNextContainer() == null) {
      throw new IllegalArgumentException("getLocation: node probably removed from DOM - " + node);
    }
    return indexingContainer.offset();
  }

  /**
   * {@inheritDoc}
   */
  public int getLocation(Point<N> point) {
    Preconditions.checkNotNull(point, "Cannot get the location of a null point");
    Point.checkPoint(this, point, "IndexedDocumentImpl#getLocation");
    if (point.isInTextNode()) {
      return getLocation(point.getContainer()) + point.getTextOffset();
    } else {
      N nodeAfter = point.getNodeAfter();
      if (nodeAfter == null) {
        return getLastLocationIn(point.getContainer());
      } else {
        return getLocation(nodeAfter);
      }
    }
  }

  /**
   * Gets the last location inside an element node.
   *
   * @param node An element node.
   * @return The last location inside an element node.
   */
  private int getLastLocationIn(N node) {
    N lastChild = substrate.getLastChild(node);
    if (lastChild != null) {
      if (substrate.asElement(lastChild) != null) {
        return getLastLocationIn(lastChild) + 1;
      } else {
        OffsetList.Container<N> indexingContainer = substrate.getIndexingContainer(lastChild);
        return indexingContainer.offset() + indexingContainer.size();
      }
    } else if (node == getDocumentElement()) {
      return size();
    } else {
      return substrate.getIndexingContainer(node).offset() + 1;
    }
  }

  private void moveToCurrentLocation() {
    offsetList.performActionAt(currentLocation, locationUpdater);
  }

  private final InvertibleCursor invertibleCursor = new InvertibleCursor();

  private boolean inconsistent = false;

  @Override
  public void consume(DocOp op) throws OperationException {
    consume(op, performValidation);
  }

  public void consume(DocOp op, boolean validate) throws OperationException {
    checkConsistent();

    if (validate) {
      maybeThrowOperationExceptionFor(op);
    }

    beginChange();

    try {
      op.apply(invertibleCursor);
    } catch (OpCursorException e) {
      throw new OperationException(e.getMessage(), e);
    }

    endChange();
  }

  public void maybeThrowOperationExceptionFor(DocOp op) throws OperationException {
    if (!DocOpValidator.validate(null, schemaConstraints, autoDoc, op).isValid()) {
      // Validate again to collect diagnostics (more expensive)
      ViolationCollector vc = new ViolationCollector();
      DocOpValidator.validate(vc, schemaConstraints, autoDoc, op);

      throw new OperationException(vc);
    }
  }

  private final NonInvertibleCursor nindoCursor = new NonInvertibleCursor();

  public DocOp consumeAndReturnInvertible(Nindo op) throws OperationException {
    return consumeAndReturnInvertible(op, performValidation);
  }

  public DocOp consumeAndReturnInvertible(Nindo op, boolean validate)
      throws OperationException {
    checkConsistent();

    if (validate) {
      maybeThrowOperationExceptionFor(op);
    }

    nindoCursor.begin2();

    try {
      op.apply(nindoCursor);
    } catch (OpCursorException e) {
      throw new OperationException(e.getMessage(), e);
    }

    return nindoCursor.finish2();
  }

  @Override
  public void maybeThrowOperationExceptionFor(Nindo op) throws OperationException {
    ViolationCollector vc = NindoValidator.validate(this, op, schemaConstraints);
    if (!vc.isValid()) {
      // TODO(danilatos): reconcile the two validation methods
      throw new OperationException(vc);
    }
  }

  private void checkConsistent() {
    Preconditions.checkState(!inconsistent, "The document is not in a consistent state");
  }

  private void beginChange() {

    beforeBegin();

    inconsistent = true;

    resetLocation();
    annotations.begin();
  }

  private void endChange() throws OperationException {
    if (currentLocation != size()) {
      throw new OperationException("Operation size does not match document size " +
          "[operation size:" + currentLocation + "] [doc size:" + size() + "]");
    }

    annotations.finish();
    checkSizeConsistency("finish");
    inconsistent = false;

    afterFinish();
  }

  private class InvertibleCursor implements DocOpCursor {

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      annotations.skip(1);

      E node = substrate.asElement(currentContainer.getValue());
      for (int i = 0; i < attrUpdate.changeSize(); i++) {
        String name = attrUpdate.getChangeKey(i);
        String newValue = attrUpdate.getNewValue(i);
        if (newValue != null) {
          substrate.setAttribute(node, name, newValue);
        } else {
          substrate.removeAttribute(node, name);
        }
      }
      ++currentLocation;
      currentContainer = currentContainer.getNextContainer();
      currentParent = node;

      onModifyAttributes(currentParent, attrUpdate);
    }

    @Override
    public void deleteCharacters(String chars) {
      assert chars.length() > 0;
      annotations.delete(chars.length());
      doDeleteCharacters(chars.length());

      onDeleteCharacters(currentLocation, chars);
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      annotations.skip(1);

      E node = substrate.asElement(currentContainer.getValue());
//      Map<String, String> oldAttributes = substrate.getAttributes(node);
      // Iterate over oldAttributes here, not attributeMap, since we are modifying
      // the map underlying the latter.
      for (Map.Entry<String, String> attribute : oldAttrs.entrySet()) {
        String key = attribute.getKey();
        if (!newAttrs.containsKey(key)) {
          substrate.removeAttribute(node, key);
        }
      }
      for (Map.Entry<String, String> attribute : newAttrs.entrySet()) {
        if (attribute.getValue() == null) {
          throw new OpCursorException("Null attribute value in setAttributes");
        }
        substrate.setAttribute(node, attribute.getKey(), attribute.getValue());
      }
      ++currentLocation;
      currentContainer = currentContainer.getNextContainer();
      currentParent = node;

      onModifyAttributes(currentParent, oldAttrs, newAttrs);
    }

    @Override
    public void retain(int itemCount) {
      assert itemCount > 0;
      checkRetain(itemCount);

      annotations.skip(itemCount);

      currentLocation += itemCount;
      moveToCurrentLocation();
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      for (int i = 0; i < map.endSize(); i++) {
        doEndAnnotation(map.getEndKey(i));
      }
      for (int i = 0; i < map.changeSize(); i++) {
        doStartAnnotation(map.getChangeKey(i), map.getNewValue(i));
      }
    }

    @Override
    public void characters(String characters) {
      doCharacters(characters);
    }

    @Override
    public void elementEnd() {
      doElementEnd();
    }

    @Override
    public void elementStart(String tagName, Attributes attributes) {
      doElementStart(tagName, attributes);
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {

      E nodeToDelete = substrate.asElement(currentContainer.getValue());
      if (nodeToDelete == null) {
        throw new OpCursorException("No element to delete at the current location.");
      }
      if (deletionDepth == 0) {
        substrate.removeChild(currentParent, nodeToDelete);
      }

      annotations.delete(1);
      deleteCurrentContainer();

      ++deletionDepth;

      onDeleteElementStart(currentLocation, nodeToDelete);
    }

    public void deleteElementEnd() {
      onDeleteElementEnd();

      annotations.delete(1);
      deleteCurrentContainer();

      --deletionDepth;
    }
  }

  public class NonInvertibleCursor implements NindoCursor {
    private AnnotationsNormalizer<DocOp> builder;
    int sizeDiffSoFar;

    StringMap<String> requestedValues = CollectionUtils.createStringMap();
    StringSet requestedKeys = CollectionUtils.createStringSet();

    StringMap<String> newValues = CollectionUtils.createStringMap();

    StringSet endKeys = CollectionUtils.createStringSet();

    StringMap<String> deletionValues = CollectionUtils.createStringMap();

    boolean didSomethingOtherThanDeletionSinceAnnotationBoundary = false;

    private void begin2() {
      beginChange();
      builder = new AnnotationsNormalizer<DocOp>(
          performValidation ? new DocOpBuffer() : new UncheckedDocOpBuffer());
      sizeDiffSoFar = 0;
      deletionValues.clear();
    }

    private DocOp finish2() throws OperationException {
      endChange();
      return builder.finish();
    }

    @Override
    public void begin() {
    }

    @Override
    public void finish() {
      int remaining = size() - currentLocation;
      closeEndKeys();
      if (remaining > 0) {
        builder.retain(remaining);
      }
      currentLocation = size();
    }

    public void startAnnotation(String key, String value) {

      if (endKeys.contains(key)) {
        assert !didSomethingOtherThanDeletionSinceAnnotationBoundary
            : "Key: " + key + " endKeys: " + endKeys.toString();

        endKeys.remove(key);
      }

      doStartAnnotation(key, value);

      requestedValues.put(key, value);
      requestedKeys.add(key);

      newValues.put(key, value);

      didSomethingOtherThanDeletionSinceAnnotationBoundary = false;
      //System.out.println("  ---> " + requestedValues + ",   " + newValues + ",   " + endKeys);
    }

    public void endAnnotation(String key) {
      requestedValues.remove(key);
      requestedKeys.remove(key);

      newValues.remove(key);

      if (deletionValues.containsKey(key)) {
        endKeys.add(key);
      }

      doEndAnnotation(key);
      didSomethingOtherThanDeletionSinceAnnotationBoundary = false;
      //System.out.println("  ---> " + requestedValues + ",   " + newValues + ",   " + endKeys);
    }

    public void skip(int itemCount) {
      assert itemCount > 0;
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;
      checkRetain(itemCount);

      moveAndUpdateAnnotations(itemCount);

      moveToCurrentLocation();
    }

    private void moveAndUpdateAnnotations(int itemCount) {
      // TODO(danilatos): Some redundant updates most likely,
      // because the annotation tree will report its change
      // events. We still need beginUpdate for the
      // annotations which DONT change, to avoid losing
      // them in the operation. Would only matter for transform.
      beginUpdate();

      annotations.skip(itemCount);

      final int finalLocation = currentLocation + itemCount;

      if (!requestedValues.isEmpty()) {
        // HACK(danilatos): Skip doesn't return anything, so do it a slow
        // and annoying way for now.

        final List<AnnotationEvent> events = new ArrayList<AnnotationEvent>();
        final Box<ReadableStringMap<String>> annotations = Box.create();
        int currentLocationBackup = currentLocation;
        final StringSet open = CollectionUtils.createStringSet();
        for (AnnotationInterval<String> i :
            annotationIntervals(currentLocation, finalLocation, requestedKeys)) {
          currentLocation = i.start();
          annotations.boxed = i.annotations();
          requestedValues.each(new ProcV<String>() {
            public void apply(String key, String value) {
              String oldVal = annotations.boxed.get(key, null);
              if (ValueUtils.notEqual(value, oldVal)) {
                events.add(new AnnotationStartEvent(currentLocation, key, oldVal));
                open.add(key);
              } else if (open.contains(key)) {
                events.add(new AnnotationEndEvent(currentLocation, key));
                //assert open.contains(key);
                open.remove(key);
              }
            }
          });
        }
        open.each(new Proc() {
          @Override
          public void apply(String key) {
            events.add(new AnnotationEndEvent(finalLocation, key));
          }
        });
        currentLocation = currentLocationBackup;

        for (AnnotationEvent ev : events) {
          // Uncomment this when not using the above hack
          // int eventLocation = ev.index + sizeDiffSoFar;
          int eventLocation = ev.index;
          if (eventLocation > currentLocation) {

            builder.retain(eventLocation - currentLocation);
            currentLocation = eventLocation;
          }

          if (ev.getEndKey() != null) {
            maybeRenewAnnotation(ev.getEndKey());
            builder.endAnnotation(ev.getEndKey());
          } else {
            String changeKey = ev.getChangeKey();
            builder.startAnnotation(changeKey,
                ev.getChangeOldValue(), requestedValues.get(changeKey, null));
          }
        }
      }

      if (currentLocation < finalLocation) {
        builder.retain(finalLocation - currentLocation);
        currentLocation = finalLocation;
      }

      assert currentLocation == finalLocation;
    }

    private void beginInsert() {
      //System.out.print("INSERT " + requestedValues + ",   " + newValues + ",   " + endKeys);
      //System.out.print(",   " + deletionValues);
      newValues.each(new ProcV<String>() {
        @Override
        public void apply(String key, String value) {
          builder.startAnnotation(key, annotations.getInherited(key), value);
        }
      });
      newValues.clear();

      deletionValues.clear();
      deletionValues.putAll(requestedValues);

      closeEndKeys();
      //System.out.print(" ---> " + requestedValues + ",   " + newValues + ",   " + endKeys);
      //System.out.println(",   " + deletionValues);
    }


    private void beginUpdate() {
      //System.out.print("UPDATE/SKIP " + requestedValues + ",   " + newValues + ",   " + endKeys);
      //System.out.print(",   " + deletionValues);
      requestedValues.each(new ProcV<String>() {
        @Override
        public void apply(String key, String value) {
          String current = (String) annotations.getAnnotation(currentLocation, key);
          builder.startAnnotation(key, current, value);
        }
      });
      newValues.clear();

      deletionValues.clear();
      deletionValues.putAll(requestedValues);

      closeEndKeys();
      //System.out.print(" ---> " + requestedValues + ",   " + newValues + ",   " + endKeys);
      //System.out.println(",   " + deletionValues);
    }

    private void closeEndKeys() {
      endKeys.each(new Proc() {
        @Override
        public void apply(String key) {
          builder.endAnnotation(key);
        }
      });
      endKeys.clear();
    }


    public void elementStart(String tagName, Attributes attributes) {
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;
      beginInsert();

      doElementStart(tagName, attributes);

      builder.elementStart(tagName, attributes);
      sizeDiffSoFar++;
    }

    public void characters(String characters) {
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;
      beginInsert();
      doCharacters(characters);
      builder.characters(characters);
      sizeDiffSoFar += characters.length();
    }

    public void elementEnd() {
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;
      beginInsert();
      doElementEnd();
      builder.elementEnd();
      sizeDiffSoFar++;
    }

    public void updateAttributes(Map<String, String> attributes) {
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;

      beginUpdate();

      String[] triples = new String[attributes.size() * 3];

      E node = substrate.asElement(currentContainer.getValue());
      Map<String, String> oldAttributes = substrate.getAttributes(node);
      int i = 0;
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        String name = attribute.getKey();
        String newValue = attribute.getValue();
        triples[i] = name;
        triples[i + 1] = oldAttributes.get(name);
        triples[i + 2] = newValue;
        if (newValue != null) {
          substrate.setAttribute(node, name, newValue);
        } else {
          substrate.removeAttribute(node, name);
        }
        i += 3;
      }

      currentLocation++;
      currentContainer = currentContainer.getNextContainer();
      currentParent = node;
      annotations.skip(1);

      AttributesUpdateImpl attrUpdate = new AttributesUpdateImpl(triples);
      builder.updateAttributes(attrUpdate);
      onModifyAttributes(currentParent, attrUpdate);
    }

    public void replaceAttributes(Attributes newAttrs) {
      didSomethingOtherThanDeletionSinceAnnotationBoundary = true;

      beginUpdate();

      E node = substrate.asElement(currentContainer.getValue());
      Attributes oldAttributes = new AttributesImpl(substrate.getAttributes(node));
      // Iterate over oldAttributes here, not attributeMap, since we are modifying
      // the map underlying the latter.
      for (Map.Entry<String, String> attribute : oldAttributes.entrySet()) {
        String key = attribute.getKey();
        if (!newAttrs.containsKey(key)) {
          substrate.removeAttribute(node, attribute.getKey());
        }
      }
      for (Map.Entry<String, String> attribute : newAttrs.entrySet()) {
        if (attribute.getValue() == null) {
          throw new OpCursorException("Null attribute value in setAttributes");
        }
        substrate.setAttribute(node, attribute.getKey(), attribute.getValue());
      }

      currentLocation++;
      currentContainer = currentContainer.getNextContainer();
      currentParent = node;
      annotations.skip(1);

      builder.replaceAttributes(oldAttributes, newAttrs);
      onModifyAttributes(currentParent, oldAttributes, newAttrs);
    }

    public void deleteElementStart() {
      E nodeToDelete = substrate.asElement(currentContainer.getValue());
      if (nodeToDelete == null) {
        throw new OpCursorException("No element to delete at the current location.");
      }
      String tagName = substrate.getTagName(nodeToDelete);
      Attributes attributes = new AttributesImpl(substrate.getAttributes(nodeToDelete));
      if (deletionDepth == 0) {
        substrate.removeChild(currentParent, nodeToDelete);
      }

      doSingleDelete(tagName, attributes);

      deleteCurrentContainer();

      deletionDepth++;
      sizeDiffSoFar--;

      onDeleteElementStart(currentLocation, nodeToDelete);
    }

    public void deleteElementEnd() {
      onDeleteElementEnd();

      doSingleDelete(null, null);

      deleteCurrentContainer();

      deletionDepth--;
      sizeDiffSoFar--;
    }

    private void doSingleDelete(String tagName, Attributes attrs) {
      List<AnnotationEvent> events = deleteAnnotations(1);

      boolean moved = false;
      int check = 0;

      for (AnnotationEvent ev : events) {
        int eventLocation = ev.index;
        if (eventLocation > currentLocation && !moved) {
          moved = true;
          buildDelete(tagName, attrs);
        }

        assert eventLocation == currentLocation + (moved ? 1 : 0)
            : currentLocation + " " + moved + " " + ev + " in " + events ;

        if (ev.getEndKey() != null) {
          assert moved;
          maybeRenewAnnotation(ev.getEndKey());
          builder.endAnnotation(ev.getEndKey());
          check--;
        } else {
          assert !moved;
          String changeKey = ev.getChangeKey();
          builder.startAnnotation(changeKey,
              ev.getChangeOldValue(), getLeftNeighbourAnnotation(changeKey));
          check++;
        }

      }
      assert check == 0;

      if (!moved) {
        buildDelete(tagName, attrs);
      }
    }

    private void buildDelete(String tagName, Attributes attrs) {
      if (tagName != null) {
        builder.deleteElementStart(tagName, attrs);
      } else {
        builder.deleteElementEnd();
      }
    }

    public void deleteCharacters(int deletionSize) {
      List<AnnotationEvent> events = deleteAnnotations(deletionSize);

      String oldChars = doDeleteCharacters(deletionSize);

      int finalLocation = currentLocation;

      int index = 0;
      int newIndex = -1;
      StringMap<String> check = CollectionUtils.createStringMap();

      for (AnnotationEvent ev : events) {
        int eventLocation = ev.index;
        newIndex = eventLocation - currentLocation;
        if (newIndex > index) {
          builder.deleteCharacters(oldChars.substring(index, newIndex));
          index = newIndex;
        }

        if (ev.getEndKey() != null) {
          maybeRenewAnnotation(ev.getEndKey());
          builder.endAnnotation(ev.getEndKey());

          assert check.containsKey(ev.getEndKey()) : "key: "  + ev.getEndKey() + ", " + check;
          check.remove(ev.getEndKey());
        } else {
          String changeKey = ev.getChangeKey();
          builder.startAnnotation(changeKey,
              ev.getChangeOldValue(), getLeftNeighbourAnnotation(changeKey));

          check.put(ev.getChangeKey(), ev.getChangeOldValue());
        }
      }

      assert check.isEmpty();

      if (index < deletionSize) {
        builder.deleteCharacters(oldChars.substring(index, deletionSize));
      }

      sizeDiffSoFar -= deletionSize;

      onDeleteCharacters(currentLocation, oldChars);
    }

    /**
     * @return annotation events for the deletion
     */
    // HACK(danilatos): This code is inefficient and does not belong in indexed document.
    private List<AnnotationEvent> deleteAnnotations(int size) {
      final List<AnnotationEvent> events = new ArrayList<AnnotationEvent>();

      final StringSet open = CollectionUtils.createStringSet();
      final StringMap<String> deletionInherit = CollectionUtils.createStringMap();
      deletionValues.each(new ReadableStringMap.ProcV<String>() {
        @Override
        public void apply(String key, String value) {
          deletionInherit.put(key, value);
        }
      });

      final int start = currentLocation;
      final int end = currentLocation + size;

      for (AnnotationInterval<String> i : annotationIntervals(start, end, null)) {
        assert i.end() > start;
        assert i.start() < end;
        final int realStart = Math.max(start, i.start());
        deletionInherit.each(new ReadableStringMap.ProcV<String>() {
          @Override
          public void apply(String key, String value) {
            events.add(
                new AnnotationStartEvent(realStart, key, getAnnotation(realStart, key)));
            open.add(key);
          }
        });
        i.annotations().each(new ReadableStringMap.ProcV<String>() {
          @Override
          public void apply(String key, String value) {
            if (!deletionInherit.containsKey(key)) {
              events.add(
                  new AnnotationStartEvent(realStart, key, value));
              open.add(key);
            }
          }
        });
      }

      // Produce endAnnotation calls for every annotation at the end.
      open.each(new StringSet.Proc() {
        @Override
        public void apply(String key) {
          events.add(new AnnotationEndEvent(end, key));
        }
      });

      annotations.delete(size);
      return events;
    }

    private void maybeRenewAnnotation(String key) {
      if (!newValues.containsKey(key) && requestedValues.containsKey(key)) {
        newValues.put(key, requestedValues.get(key));
      }
    }

    private String getLeftNeighbourAnnotation(String key) {
      if (deletionValues.containsKey(key)) {
        return deletionValues.get(key);
      }
      return currentLocation != 0
          ? (String) annotations.getAnnotation(currentLocation - 1, key) : null;
    }
  }

  private void checkRetain(int count) {
    if (currentLocation + count > size()) {
      throw new OpCursorException("Retain past end of document [location:" +
          (currentLocation + count) + "] [doc size:" + size() + "]");
    }
  }

  private void doElementStart(String tagName, Attributes attributes) {
    splitCurrent();
    E newElement = substrate.createElement(tagName, attributes,
        currentParent, currentContainer.getValue());
    insertBefore(currentContainer, newElement, 1);

    currentContainer.insertBefore(null, 1);
    annotations.insert(1);

    currentContainer = currentContainer.getPreviousContainer();
    currentParent = newElement;
    ++currentLocation;

    onElementStart(newElement);
  }

  private void doCharacters(String characters) {
    annotations.insert(characters.length());

    OffsetList.Container<N> previousNode = null;
    T previousTextNode = null;

    if (currentOffset == 0) {
      previousNode = currentContainer.getPreviousContainer();
      previousTextNode = substrate.asText(previousNode.getValue());
    }

    // Prefer appending data to previous text node
    if (previousTextNode != null) {
      substrate.appendData(previousTextNode, characters);
      previousNode.increaseSize(characters.length());
    } else {
      N domNode = currentContainer.getValue();
      T textNode = substrate.asText(domNode);

      if (textNode != null) {
        substrate.insertData(textNode, currentOffset, characters);
        currentContainer.increaseSize(characters.length());
        currentOffset += characters.length();
      } else {
        T newTextNode = substrate.createTextNode(characters, currentParent,
            currentContainer.getValue());
        insertBefore(currentContainer, newTextNode, characters.length());
      }
    }

    onCharacters(currentLocation, characters);

    currentLocation += characters.length();
  }

  private String doDeleteCharacters(int deletionSize) {
   StringBuilder textBuilder = new StringBuilder();
    if (deletionDepth <= 0) {
      T textNode = substrate.asText(currentContainer.getValue());
      int currentLength = substrate.getLength(textNode);
      if (currentOffset + deletionSize < currentLength) {
        textBuilder.append(
            substrate.getData(textNode).substring(currentOffset, currentOffset + deletionSize));
        substrate.deleteData(textNode, currentOffset, deletionSize);
        currentContainer.increaseSize(-deletionSize);
      } else {
        if (currentOffset > 0) {
          int amountToDelete = currentLength - currentOffset;
          textBuilder.append(substrate.getData(textNode).substring(currentOffset));
          substrate.deleteData(textNode, currentOffset, amountToDelete);
          currentContainer.increaseSize(-amountToDelete);
          deletionSize -= amountToDelete;
          currentContainer = currentContainer.getNextContainer();
          currentOffset = 0;
        }
        while (deletionSize > 0) {
          textNode = substrate.asText(currentContainer.getValue());
          currentLength = substrate.getLength(textNode);
          if (currentLength <= deletionSize) {
            textBuilder.append(substrate.getData(textNode));
            substrate.removeChild(currentParent, textNode);
            deleteCurrentContainer();
            deletionSize -= currentLength;
          } else {
            textBuilder.append(substrate.getData(textNode).substring(0, deletionSize));
            substrate.deleteData(textNode, 0, deletionSize);
            currentContainer.increaseSize(-deletionSize);
            break;
          }
        }
      }
    } else {
      while (deletionSize > 0) {
        T textNode = substrate.asText(currentContainer.getValue());
        assert textNode != null;
        if (deletionSize < substrate.getLength(textNode)) {
          // TODO(user): See if we can get rid of this cruft.
          textBuilder.append(substrate.getData(textNode).substring(0, deletionSize));
          substrate.deleteData(textNode, 0, deletionSize);
          currentContainer.increaseSize(-deletionSize);
          break;
        } else {
          textBuilder.append(substrate.getData(textNode));
          deletionSize -= currentContainer.size();
          deleteCurrentContainer();
        }
      }
    }

    return textBuilder.toString();
  }

  private void doElementEnd() {
    onElementEnd();
    annotations.insert(1);

    ++currentLocation;
    currentContainer = currentContainer.getNextContainer();
    currentParent = substrate.getParentElement(currentParent);
  }


  private void doStartAnnotation(String key, String value) {
    if (Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Cannot access local annotations");
    }
    annotations.startAnnotation(key, value);
  }

  private void doEndAnnotation(String key) {
    if (Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Cannot access local annotations");
    }
    annotations.endAnnotation(key);
  }

  private void resetLocation() {
    currentLocation = 0;
    currentOffset = 0;
    currentParent = substrate.getDocumentElement();
    currentContainer = offsetList.firstContainer();
  }

  /**
   * Deletes the current container.
   */
  private void deleteCurrentContainer() {
    OffsetList.Container<N> nextContainer = currentContainer.getNextContainer();
    currentContainer.remove();
    currentContainer = nextContainer;
  }

  /**
   * Gets the parent of the DOM node associated with the given container.
   *
   * TODO(user): Consider some efficiency improvements.
   *
   * @param container The container.
   * @return The parent of the DOM node associated with the given container.
   */
  private E getParentOf(OffsetList.Container<N> container) {
    N domNode = container.getValue();
    if (domNode != null) {
      return substrate.getParentElement(domNode);
    }
    int counter = 0;
    container = container.getPreviousContainer();
    while (container.getValue() == null) {
      ++counter;
      container = container.getPreviousContainer();
    }
    domNode = container.getValue();
    E parent = substrate.asElement(domNode);
    if (parent == null) {
      parent = substrate.getParentElement(domNode);
    }
    for (int i = 0; i < counter; ++i) {
      parent = substrate.getParentElement(parent);
    }
    return parent;
  }

  /**
   * Splits the current container at the current offset.
   */
  private void splitCurrent() {
    if (currentOffset != 0) {
      T domNode = substrate.asText(currentContainer.getValue());
      T newDomNode = substrate.splitText(domNode, currentOffset);
      assert newDomNode != null;
      currentContainer = currentContainer.split(currentOffset, newDomNode);
      substrate.setIndexingContainer(newDomNode, currentContainer);
      currentOffset = 0;
    }
  }

  /**
   * Inserts a DOM node into the indexing structure.
   *
   * @param container The container before which to insert.
   * @param domNode The DOM node to insert.
   * @param size The size of the inserted node.
   */
  private void insertBefore(OffsetList.Container<N> container, N domNode, int size) {
    substrate.setIndexingContainer(domNode, container.insertBefore(domNode, size));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "IndexedDI@" + Integer.toHexString(System.identityHashCode(this))
        + "[" + toDebugString() + "]";
  }


  @Override
  public String toDebugString() {
    try {
      return DocOpUtil.toXmlString(DocOpScrub.maybeScrub(toInitialization()));
    } catch (RuntimeException e) {
      if (!DocOpScrub.shouldScrubByDefault()) {
        try {
          return "#<NO ANNOTATIONS>: " + XmlStringBuilder.innerXml(this) + "# (" + e + ")";
        } catch (RuntimeException e2) {
          return "#<!SUPER BROKEN># (" + e + ")";
        }
      } else {
        return "#<!BROKEN># (" + e + ")";
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getData(T textNode) {
    return substrate.getData(textNode);
  }

  /**
   * {@inheritDoc}
   */
  public N getFirstChild(N node) {
    return substrate.getFirstChild(node);
  }

  /**
   * {@inheritDoc}
   */
  public N getLastChild(N node) {
    return substrate.getLastChild(node);
  }

  /**
   * {@inheritDoc}
   */
  public int getLength(T textNode) {
    return substrate.getLength(textNode);
  }

  /**
   * {@inheritDoc}
   */
  public N getNextSibling(N node) {
    return substrate.getNextSibling(node);
  }

  /**
   * {@inheritDoc}
   */
  public short getNodeType(N node) {
    return substrate.getNodeType(node);
  }

  /**
   * {@inheritDoc}
   */
  public E getParentElement(N node) {
    return substrate.getParentElement(node);
  }

  /**
   * {@inheritDoc}
   */
  public N getPreviousSibling(N node) {
    return substrate.getPreviousSibling(node);
  }

  /**
   * {@inheritDoc}
   */
  public E getDocumentElement() {
    return substrate.getDocumentElement();
  }

  /**
   * {@inheritDoc}
   */
  public E asElement(N node) {
    return substrate.asElement(node);
  }

  /**
   * {@inheritDoc}
   */
  public T asText(N node) {
    return substrate.asText(node);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isSameNode(N node, N other) {
    return substrate.isSameNode(node, other);
  }

  /**
   * {@inheritDoc}
   */
  public Map<String,String> getAttributes(E element) {
    return substrate.getAttributes(element);
  }

  /**
   * {@inheritDoc}
   */
  public String getTagName(E element) {
    return substrate.getTagName(element);
  }

  /**
   * {@inheritDoc}
   */
  public String getAttribute(E element, String name) {
    return substrate.getAttribute(element, name);
  }

  @Override
  public int size() {
    checkSizeConsistency("size");
    return offsetList.size();
  }

  private void checkSizeConsistency(String msgPrefix) {
    // TODO(danilatos/ohler): Make this an assert once we are more confident
    if (annotations != null && offsetList != null && offsetList.size() != annotations.size()) {
      throw new RuntimeException(msgPrefix +
          ": Document and annotations have inconsistent size: " +
          offsetList.size() + " vs " + annotations.size() + ", respectively");
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getAnnotation(int start, String key) {
    if (Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Cannot access local annotations");
    }
    return (String) annotations.getAnnotation(start, key);
  }

  /**
   * {@inheritDoc}
   */
  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    if (Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Cannot access local annotations");
    }
    return annotations.firstAnnotationChange(start, end, key, fromValue);
  }

  /**
   * {@inheritDoc}
   */
  public int lastAnnotationChange(int start, int end, String key, String fromValue) {
    if (Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Cannot access local annotations");
    }
    return annotations.lastAnnotationChange(start, end, key, fromValue);
  }

  void checkValidPersistentKeys(ReadableStringSet keys) {
    keys.each(new Proc() {
      @Override
      public void apply(String key) {
        Annotations.checkPersistentKey(key);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  public T splitText(T textNode, int offset) {
    if (inconsistent) {
      throw new IllegalStateException("Cannot splitText() during a modification");
    }

    if (offset == 0) {
      return textNode;
    } else if (offset >= substrate.getLength(textNode)) {
      return null;
    }

    currentLocation = getLocation(textNode) + offset;
    offsetList.performActionAt(currentLocation, locationUpdater);
    splitCurrent();
    return asText(currentContainer.getValue());
  }

  /**
   * {@inheritDoc}
   */
  public T mergeText(T secondSibling) {
    if (inconsistent) {
      throw new IllegalStateException("Cannot mergeText() during a modification");
    }

    currentLocation = getLocation(secondSibling);
    offsetList.performActionAt(currentLocation, locationUpdater);
    T mergedNode = substrate.mergeText(secondSibling);

    if (mergedNode != null) {
      OffsetList.Container<N> previous = currentContainer.getPreviousContainer();
      currentContainer.increaseSize(previous.size());
      previous.remove();
      substrate.setIndexingContainer(mergedNode, currentContainer);
    }

    return mergedNode;
  }

  @Override
  public DocInitialization asOperation() {

    if (size() == 0) {
      return EmptyDocument.EMPTY_DOCUMENT;
    }

    DocOp domOp = serializeDom();

    DocOp annotationsOp = serializeAnnotations();

    try {
      final DocOp bothOps;
      if (performValidation) {
        bothOps = Composer.compose(domOp, annotationsOp);
      } else {
        bothOps = Composer.composeUnchecked(domOp, annotationsOp);
      }
      DocInitialization initialisation = DocOpUtil.asInitialization(bothOps);
      assert DocOpValidator.validate(null, schemaConstraints, initialisation).isValid();

      return initialisation;
    } catch (OperationException e) {
      throw new OperationRuntimeException("Bug either in indexed document or the composer", e);
    }
  }

  @Override
  public DocInitialization toInitialization() {
    return asOperation();
  }

  private DocOp serializeDom() {
    DocOpBuilder b = new DocOpBuilder();
    int depth = 0;
    for (N node : offsetList) {
      if (node != null) {
        T textNode = substrate.asText(node);
        if (textNode != null) {
          b.characters(substrate.getData(textNode));
        } else {
          E elementNode = substrate.asElement(node);
          b.elementStart(substrate.getTagName(elementNode),
              new AttributesImpl(substrate.getAttributes(elementNode)));
          depth++;
        }
      } else {
        // To avoid the sentinel
        depth--;
        if (depth >= 0) {
          b.elementEnd();
        }
      }
    }

    DocOp domOp = b.buildUnchecked();
    assert DocOpValidator.isWellFormed(null, domOp);
    return domOp;
  }

  private DocOp serializeAnnotations() {
    final AnnotationsNormalizer<DocOp> b =
        new AnnotationsNormalizer<DocOp>(new UncheckedDocOpBuffer());

    AnnotationInterval<Object> last = null;
    for (AnnotationInterval<Object> i : annotations.annotationIntervals(0, size(), knownKeys())) {
      i.diffFromLeft().each(new ProcV<Object>() {
        @Override
        public void apply(String key, Object value) {
          assert value == null || value instanceof String;
          if (value != null) {
            b.startAnnotation(key, null, (String) value);
          } else {
            b.endAnnotation(key);
          }
        }
      });
      b.retain(i.length());
      last = i;
    }
    if (size() > 0) {
      last.annotations().each(new ProcV<Object>() {
          @Override
          public void apply(String key, Object value) {
  //          assert value != null;
            b.endAnnotation(key);
          }
        });
    }
    DocOp annotationsOp = b.finish();
    assert DocOpValidator.isWellFormed(null, annotationsOp);
    return annotationsOp;
  }

  @Override
  public StringSet knownKeys() {
    final StringSet knownKeys = CollectionUtils.createStringSet();
    annotations.knownKeysLive().each(new Proc() {
      @Override
      public void apply(String key) {
        if (!Annotations.isLocal(key)) {
          knownKeys.add(key);
        }
      }
    });
    return knownKeys;
  }

  /**
   * Evaluate the document using the associative operator.
   *
   * @return The result of the evaluation
   */
  protected V evaluate() {
    return offsetList.evaluate();
  }

  /**
   * @return the "currentParent" element to which children are being added
   */
  private E getCurrentParent() {
    return currentParent;
  }

  /**
   * @return the current int location
   */
  private int getCurrentLocation() {
    return currentLocation;
  }

  /**
   * @return the "currentNode" we are in or before
   */
  protected N getCurrentNode() {
    return currentContainer.getValue();
  }

  @Override
  public void forEachAnnotationAt(int location,
      final ReadableStringMap.ProcV<String> callback) {
    annotations.forEachAnnotationAt(location, new ReadableStringMap.ProcV<Object>() {
      @Override
      public void apply(String key, Object value) {
        if (!Annotations.isLocal(key)) {
          assert value == null || value instanceof String;
          callback.apply(key, (String) value);
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    if (keys == null) {
      keys = knownKeys();
    } else {
      checkValidPersistentKeys(keys);
    }
    return annotations.annotationCursor(start, end, keys);
  }

  @Override
  public Iterable<AnnotationInterval<String>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {

    if (keys == null) {
      keys = knownKeys();
    } else {
      checkValidPersistentKeys(keys);
    }

    final Iterable<AnnotationInterval<Object>> iterable =
        annotations.annotationIntervals(start, end, keys);
    return new Iterable<AnnotationInterval<String>>() {
      @Override
      public Iterator<AnnotationInterval<String>> iterator() {
        final Iterator<AnnotationInterval<Object>> iterator = iterable.iterator();
        return new Iterator<AnnotationInterval<String>>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          // SuppressWarnings because of conversion from RSMap<Object> to <String>, already checked
          // keys are persistent keys using checkValidPersistentKeys(), and this class has
          // an invariant that such keys only have string values. (Local annotation set views
          // disallow setting any values for non-local keys).
          @SuppressWarnings("unchecked")
          @Override
          public AnnotationInterval<String> next() {
            AnnotationInterval<Object> rawInterval = iterator.next();
            int start = rawInterval.start();
            int end = rawInterval.end();
            ReadableStringMap annotations = rawInterval.annotations();
            ReadableStringMap diffFromLeft = rawInterval.diffFromLeft();
            return new AnnotationIntervalImpl<String>(start, end, annotations, diffFromLeft);
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }
    };
  }

  @Override
  public Iterable<RangedAnnotation<String>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    if (keys == null) {
      keys = knownKeys();
    } else {
      checkValidPersistentKeys(keys);
    }
    final Iterable<RangedAnnotation<Object>> iterable =
        annotations.rangedAnnotations(start, end, keys);
    return new Iterable<RangedAnnotation<String>>() {
      @Override
      public Iterator<RangedAnnotation<String>> iterator() {
        final Iterator<RangedAnnotation<Object>> iterator = iterable.iterator();
        return new Iterator<RangedAnnotation<String>>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public RangedAnnotation<String> next() {
            RangedAnnotation<Object> rawRange = iterator.next();
            String key = rawRange.key();
            // Safe cast because already checked with checkValidPersistentKeys();
            String value = (String) rawRange.value();
            int start = rawRange.start();
            int end = rawRange.end();
            return new RangedAnnotationImpl<String>(key, value, start, end);
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }
    };
  }

  @Override
  public String toXmlString() {
    return DocOpUtil.toXmlString(asOperation());
  }

  protected void beforeBegin() {
  }

  protected void afterFinish() {
  }

  protected void onElementStart(E element) {
  }

  protected void onElementEnd() {
  }

  protected void onDeleteElementStart(int location, E element) {
  }

  protected void onDeleteElementEnd() {
  }

  protected void onModifyAttributes(E element, Attributes oldAttributes, Attributes newAttributes) {
  }

  protected void onModifyAttributes(E element, AttributesUpdate update) {
  }

  protected void onCharacters(int location, String characters) {
  }

  protected void onDeleteCharacters(int location, String characters) {
  }

  public DocumentSchema getSchema() {
    return schemaConstraints;
  }
}
