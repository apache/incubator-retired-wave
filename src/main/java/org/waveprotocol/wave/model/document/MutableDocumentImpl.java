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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.Locator;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.Nindo.Builder;
import org.waveprotocol.wave.model.document.operation.algorithm.AnnotationsNormalizer;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.UncheckedDocOpBuffer;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.parser.XmlParserFactory;
import org.waveprotocol.wave.model.document.parser.XmlPullParser;
import org.waveprotocol.wave.model.document.util.AnnotationBuilder;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DomOperationUtil;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the MutableDocument interface.
 *
 * TODO(danilatos): Change the implementation to act directly on the WaveDoc,
 * and sourcing ops. This is more efficient than converting points to locators,
 * which will just be wastefully converted back again internally in the WaveDoc.
 * There's a lot of innefficiencies now, which we can clear up, but ok for now
 * because they'll exercise our new code.
 *
 */
@SuppressWarnings("deprecation")
public class MutableDocumentImpl<N, E extends N, T extends N> implements MutableDocument<N,E,T> {

  protected final OperationSequencer<Nindo> sequencer;

  private final ReadableWDocument<N, E, T> doc;

  /**
   * @param sequencer
   * @param document
   */
  public MutableDocumentImpl(OperationSequencer<Nindo> sequencer,
      ReadableWDocument<N, E, T> document) {
    this.sequencer = sequencer;
    this.doc  = document;
  }

  protected void begin() {
    sequencer.begin();
  }

  protected void end() {
    sequencer.end();
  }

  private void consume(Builder builder) {
    sequencer.consume(builder.build());
  }

  private void consume(Nindo op) {
    sequencer.consume(op);
  }

  /**
   * Deletes a content element. Executes the delete in this document, and
   * places the caret in the deleted element's spot. Finally triggers
   * a single operation event matching the executed delete.
   *
   * @param element the element to delete
   */
  public void deleteNode(E element) {
    try {
      begin();

      consume(deleteElement(element, at(Locator.before(doc, element))));

    } finally {
      end();
    }
  }


  /**
   * Deletes all children of a content element. Executes the delete in
   * this document, and places the caret in the emptied element. Finally
   * triggers a single operation event matching the executed delete.
   *
   * @param element the element to delete
   */
  public void emptyElement(E element) {
    try {
      begin();

      // Construct and apply delete operation
      consume(emptyElement(element, at(Locator.start(doc, element))));

    } finally {
      end();
    }
  }

  @Override
  public void insertText(int location, String text) {
    Preconditions.checkPositionIndex(location, size());

    // TODO(danilatos): Get the schema constraints from the document
    // and use the corresponding permitted characters from there.
    // text = getPermittedCharactersForPoint(point).convertString(text);

    try {
      begin();
      consume(insertText(text, at(location)));
    } finally {
      end();
    }
  }

  /**
   * Inserts text at the given point. Triggers a single operation event matching the
   * executed insertion.
   *
   * @param point Point at which to insert text
   * @param text Text to insert
   */
  public void insertText(Point<N> point, String text) {
    Point.checkPoint(this, point, "MutableDocumentImpl.insertText");
    insertText(doc.getLocation(point), text);
  }

  @Override
  public E appendXml(XmlStringBuilder xml) {
    return insertXml(Point.<N>end(doc.getDocumentElement()), xml);
  }

  /**
   * Parses the xml and appends it to an existing builder.
   *
   * NOTE(user): Find a better place for this utility method. Where's the
   * lowest level intersection of XmlStringBuilder and Nindo.Builder?
   *
   * @param xml
   * @param builder
   */
  public static void appendXmlToBuilder(XmlStringBuilder xml, Builder builder) {
    String xmlString = xml.getXmlString();
    try {
      XmlPullParser parser = XmlParserFactory.unbuffered(xmlString);
      while (parser.hasNext()) {
        parser.next();
        switch (parser.getCurrentType()) {
          case TEXT:
            builder.characters(parser.getText());
            break;
          case START_ELEMENT:
            builder.elementStart(parser.getTagName(), new AttributesImpl(CollectionUtils
                .newJavaMap(parser.getAttributes())));
            break;
          case END_ELEMENT:
            builder.elementEnd();
            break;
        }
      }
    } catch (XmlParseException e) {
      throw new IllegalArgumentException("Ill-formed xml: " + xmlString, e);
    }
  }

  @Override
  public E insertXml(Point<N> point, XmlStringBuilder xml) {
    Point.checkPoint(this, point, "MutableDocumentImpl.insertXml");
    try {
      begin();
      int where = doc.getLocation(point);
      Builder builder = at(where);
      appendXmlToBuilder(xml, builder);
      consume(builder);
      return Point.elementAfter(this, doc.locate(where));
    } finally {
      end();
    }
  }

  @Override
  public Range deleteRange(int start, int end) {
    Preconditions.checkPositionIndexes(start, end, size());
    // TODO(davidbyttow/danilatos): Handle this more efficiently.
    PointRange<N> range = deleteRange(doc.locate(start), doc.locate(end));
    return new Range(doc.getLocation(range.getFirst()), doc.getLocation(range.getSecond()));
  }

  /**
   * Deletes a content range. Executes the delete in this document, and
   * places the caret in the deleted element's spot. Finally triggers
   * a single operation event matching the executed delete.
   *
   * @param start
   * @param end
   * @return The point where it would place the cursor
   */
  public PointRange<N> deleteRange(Point<N> start, Point<N> end) {
    Point.checkPoint(this, start, "MutableDocumentImpl.deleteRange start point");
    Point.checkPoint(this, end, "MutableDocumentImpl.deleteRange end point");

    int startLocation = doc.getLocation(start);
    int endLocation = doc.getLocation(end);

    if (startLocation > endLocation) {
      throw new IllegalArgumentException("MutableDocumentImpl.deleteRange: start is after end");
    }

    if (startLocation == endLocation) {
      return new PointRange<N>(start, end);
    }

    try {
      begin();

      // TODO(danilatos): Re-implement by traversing through and building just
      // one large mutation, including joins where appropriate.

      // TODO(danilatos): Add some "find common parent" functionality to the
      // lookup tree, where it can be implemented more efficiently than here.

      // The start point we want to return will always be dependent on
      // the original start location int value.
      // The end point, however, might change, so store it here.
      Point<N> newEndPoint = null;

      if (doc.isSameNode(
          Point.enclosingElement(this, start.getContainer()),
          Point.enclosingElement(this, end.getContainer()))) {
        // simple case, the two points are in the same element
        consume(deleteRangeInternal(startLocation, endLocation));
        // in this case, the end point == start point
        newEndPoint = doc.locate(startLocation);
      } else {
        // complicated case, the two points cross element boundaries
        E startEl = Point.enclosingElement(doc, start.getContainer());
        E endEl = Point.enclosingElement(doc, end.getContainer());

        // TODO(danilatos): Check if GWT's LinkedHashSet works and use that
        // instead of an array. This should be ok for now, I don't expect the
        // list to grow more than about size 3 in practice, so contains
        // should be fast.
        List<E> startAncestors = new ArrayList<E>();
        for (E el = startEl; el != null; el = doc.getParentElement(el)) {
          startAncestors.add(el);
        }

        // traverse up, to the midpoint, deleting content on the left of the end point
        E rightEl, prevRightEl;
        // this is our index into the left-side ancestors where we will start deletions on
        // that side. it's minus-one because the last one is the common ancestor, which is
        // handled in this right-side-handling loop.
        int commonAncestorIndexMinusOne = -2;
        boolean needToDoLeftmostDeleteSeparately = true;
        for (rightEl = endEl, prevRightEl = endEl; ; rightEl = doc.getParentElement(rightEl)) {
          boolean atCommonAncestor = startAncestors.contains(rightEl);

          int s; // start of this deletion section
          if (atCommonAncestor) {
            // If at common ancestor of start and end points
            commonAncestorIndexMinusOne = startAncestors.indexOf(rightEl) - 1;
            // If there are ranges on the left of the common ancestor
            needToDoLeftmostDeleteSeparately = commonAncestorIndexMinusOne >= 0;
            s = needToDoLeftmostDeleteSeparately
                // if there are, this is a range in the middle
                ? Locator.after(doc, startAncestors.get(commonAncestorIndexMinusOne))
                // otherwise it's right at the start
                : startLocation;
          } else {
            // If we're not at the common ancestor, it's a lot simpler.
            s = Locator.start(doc, rightEl);
          }
          int e = rightEl == endEl ? endLocation : Locator.before(doc, prevRightEl);
          consume(deleteRangeInternal(s, e));

          // if this is the first iteration, save the new end point after we've deleted
          // the rightmost sub-range of content
          if (newEndPoint == null) {
            newEndPoint = doc.locate(s);
          }

          prevRightEl = rightEl;

          if (atCommonAncestor) {
            break;
          }
        }

        assert commonAncestorIndexMinusOne != -2;

        // traverse down, deleting content on the right of the start point.
        // as well as it being minus-one, we also skip i == 0, because that
        // needs different logic.
        for (int i = commonAncestorIndexMinusOne; i > 0; i--) {
          consume(deleteRangeInternal(
              Locator.after(doc, startAncestors.get(i - 1)),
              Locator.end(doc, startAncestors.get(i))));
        }

        // here is said different logic
        if (needToDoLeftmostDeleteSeparately) {
          // leftmost delete
          consume(deleteRangeInternal(startLocation, Locator.end(doc, startEl)));
        }
      }

      // Maybe place caret where delete took place
      Point<N> startPoint = doc.locate(startLocation);

      // Our useful return value
      return new PointRange<N>(startPoint, newEndPoint);

    } finally {
      end();
    }
  }

  @Override
  public void moveSiblings(Point<N> destination, N from, N toExcl) {
    assert from != null;

    // step 0: position calculations:
    final int removeStart = doc.getLocation(Point.before(doc, from));
    int removeEnd = toExcl == null ? doc.getLocation(Point.end((N) doc.getParentElement(from)))
        : doc.getLocation(Point.before(doc, toExcl));
    int removeSize = removeEnd - removeStart;
    assert removeSize > 0;

    // comparing to where it's moving to:
    int addPosition = doc.getLocation(destination);
    if (addPosition >= removeEnd) {
      addPosition -= removeSize; // new insert should shift down
    }
    int remainder = doc.size() - removeSize - addPosition; // remainder after the insert

    // step 1: initialise the operations for the DOM and annotations:
    DocOpBuffer domOp = new DocOpBuffer();
    final AnnotationsNormalizer<DocOp> annotOp =
      new AnnotationsNormalizer<DocOp>(new UncheckedDocOpBuffer());
    if (addPosition > 0) {
      annotOp.retain(addPosition);
      domOp.retain(addPosition);
    }

    // step 2: fill in the DOM operations:
    for (N at = from; at != toExcl; at = doc.getNextSibling(at)) {
      DomOperationUtil.buildDomInitializationFromSubtree(doc, at, domOp);
    }

    // step 3: form the annotatinos
    AnnotationInterval<String> last = null;
    doc.knownKeys().each(new Proc() { // start all the keys
        @Override public void apply(String key) {
          annotOp.startAnnotation(key, null, doc.getAnnotation(removeStart, key));
        }
      });
    for (AnnotationInterval<String> interval : // update along the way
        doc.annotationIntervals(removeStart, removeEnd, null)) {
      interval.diffFromLeft().each(new ProcV<Object>() {
        @Override public void apply(String key, Object value) {
          assert value == null || value instanceof String;
          if (value != null) {
            annotOp.startAnnotation(key, null, (String) value);
          } else {
            annotOp.endAnnotation(key);
          }
        }
      });
      annotOp.retain(interval.length());
      last = interval;
    }
    doc.knownKeys().each(new Proc() { // close all the keys
      @Override public void apply(String key) {
        annotOp.endAnnotation(key);
      }
    });

    // step 4: close them out:
    if (remainder > 0) {
      domOp.retain(remainder);
      annotOp.retain(remainder);
    }

    // step 5: remove the entire range
    deleteRange(removeStart, removeEnd);

    // step 6: compose then consume the insertion op:
    DocOp atomicInsert;
    try {
      atomicInsert = Composer.compose(domOp.finish(), annotOp.finish());
      hackConsume(Nindo.fromDocOp(atomicInsert, true));
    } catch (OperationException e) {
      // should never happen, we constructed composable ops
    }
  }

  @Override
  public void setElementAttribute(E element, String name, String value) {
    String currentValue = getAttribute(element, name);
    if ((value == null && currentValue == null) || (value != null && value.equals(currentValue))) {
      // Redundant.  Do nothing, no operation.
      return;
    }
    try {
      begin();
      consume(setAttribute(name, value, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  @Override
  public void setElementAttributes(E element, Attributes attrs) {
    Preconditions.checkArgument(element != getDocumentElement(), "Cannot touch root element");
    try {
      begin();
      consume(setAttributes(attrs, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  @Override
  public void updateElementAttributes(E element, Map<String, String> attrs) {
    Preconditions.checkArgument(element != getDocumentElement(), "Cannot touch root element");
    try {
      begin();
      consume(updateAttributes(attrs, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  @Override
  public E createChildElement(E parent, String tag, Map<String, String> attrs) {
    return createElement(Point.<N>end(parent), tag, attrs);
  }

  @Override
  public E createElement(Point<N> point, String tagName, Map<String, String> attributes) {
    // TODO(danilatos): Validate point is in document. indexed doc should throw an exception
    // when calling getLocation anyway.
    Preconditions.checkNotNull(tagName, "createElement: tagName must not be null");
    Point.checkPoint(this, point, "MutableDocumentImpl.createElement");
    try {
      begin();

      int location = doc.getLocation(point);
      consume(createElement(tagName, new AttributesImpl(attributes), at(location)));

      Point<N> result = doc.locate(location);

      return doc.asElement(result.isInTextNode()
          ? doc.getNextSibling(result.getContainer()) : result.getNodeAfter());
    } finally {
      end();
    }
  }

  @Override
  public void setAnnotation(int start, int end, String key, String value) {
    Annotations.checkPersistentKey(key);
    Preconditions.checkPositionIndexes(start, end, doc.size());
    if (start == end) {
      return;
    }
    try {
      begin();
      consume(Nindo.setAnnotation(start, end, key, value));
    } finally {
      end();
    }

  }

  @Override
  public void resetAnnotation(int start, int end, String key, String value) {
    Annotations.checkPersistentKey(key);
    Preconditions.checkPositionIndexes(start, end, doc.size());
    try {
      begin();
      Builder b = new Builder();
      if (start > 0) {
        b.startAnnotation(key, null);
        b.skip(start);
      }
      if (start != end) {
        b.startAnnotation(key, value);
        b.skip(end - start);
      }
      if (doc.size() != end) {
        b.startAnnotation(key, null);
        b.skip(doc.size() - end);
      }
      b.endAnnotation(key);
      consume(b.build());
    } finally {
      end();
    }
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<RangedValue<String>> values) {
    if (rangeStart == rangeEnd) {
      // Nothing to do
      return;
    }

    Preconditions.checkPositionIndexes(rangeStart, rangeEnd, doc.size());
    AnnotationBuilder<N, E, T> ab = new AnnotationBuilder<N, E, T>(doc, rangeStart, rangeEnd, key);

    for (RangedValue<String> annotation : values) {
      int start = annotation.start;
      int end = annotation.end;

      Preconditions.checkPositionIndexesInRange(ab.getCurrentPos(), start, end, rangeEnd);

      if (start == end) {
        // nothing to do
        continue;
      }

      ab.setUpTo(null, start);
      ab.setUpTo(annotation.value, end);
    }

    ab.setUpTo(null, rangeEnd);

    if (ab.getDirty()) {
      try {
        begin();
        consume(ab.build());
      } finally {
        end();
      }
    }
  }

  // Helper DSL methods for building operations
  // at() creates a builder with cursor at the given location
  // Each other method does something convenient and returns the given builder

  private Builder at(int location) {
    Builder b = new Builder();
    if (location > 0) {
      b.skip(location);
    }
    return b;
  }

  private Builder createElement(String tagName, Attributes attributes, Builder builder) {
    builder.elementStart(tagName, attributes);
    builder.elementEnd();
    return builder;
  }

  private Builder insertText(String text, Builder builder) {
    builder.characters(text);
    return builder;
  }

  private Builder deleteElement(E element, Builder builder) {
    builder.deleteElementStart();
    emptyElement(element, builder);
    builder.deleteElementEnd();
    return builder;
  }

  private Builder emptyElement(E element, Builder builder) {
    for (N node = doc.getFirstChild(element); node != null; node = doc.getNextSibling(node)) {
      E elChild = doc.asElement(node);
      if (elChild != null) {
        deleteElement(elChild, builder);
      } else {
        builder.deleteCharacters(doc.getData(doc.asText(node)).length());
      }
    }
    return builder;
  }

  private Builder setAttribute(String name, String value, Builder builder) {
    builder.updateAttributes(Collections.singletonMap(name, value));
    return builder;
  }

  private Builder setAttributes(Attributes attrs, Builder builder) {
    builder.replaceAttributes(attrs);
    return builder;
  }

  private Builder updateAttributes(Map<String, String> attrs, Builder builder) {
    builder.updateAttributes(attrs);
    return builder;
  }

  private Builder deleteRangeInternal(int startLocation, int endLocation) {
    // TODO(danilatos): Delete this method when deleting is redone efficiently

    Builder builder = at(startLocation);
    Point<N> start = doc.locate(startLocation);
    Point<N> end = doc.locate(endLocation);

    assert doc.isSameNode(
        Point.enclosingElement(doc, start.getContainer()),
        Point.enclosingElement(doc, end.getContainer()))
        : "Range must be within a single element";

    N node;
    if (start.isInTextNode()) {
      if (doc.isSameNode(start.getContainer(), end.getContainer())) {
        int size = end.getTextOffset() - start.getTextOffset();
        if (size > 0) {
          builder.deleteCharacters(size);
        }
        return builder;
      } else {
        int size = doc.getLength(doc.asText(start.getContainer())) - start.getTextOffset();
        node = doc.getNextSibling(start.getContainer());
        if (size > 0) {
          builder.deleteCharacters(size);
        }
      }
    } else {
      node = start.getNodeAfter();
    }

    N stop;
    if (end.isInTextNode()) {
      stop = end.getContainer();
    } else {
      stop = end.getNodeAfter();
    }

    while (node != stop) {
      N next = doc.getNextSibling(node);
      T text = doc.asText(node);
      if (text != null) {
        builder.deleteCharacters(doc.getData(text).length());
      } else {
        deleteElement(doc.asElement(node), builder);
      }
      node = next;
    }

    if (end.isInTextNode()) {
      int size = end.getTextOffset();
      if (size > 0) {
        builder.deleteCharacters(size);
      }
    }

    return builder;
  }

  @Override
  public void with(Action actionToRunWithDocument) {
    actionToRunWithDocument.exec(this);
  }

  @Override
  public <V> V with(Method<V> methodToRunWithDocument) {
    return methodToRunWithDocument.exec(this);
  }

  public void hackConsume(Nindo op) {
    begin();
    try {
      consume(op);
    } finally {
      end();
    }
  }

  // Eclipse-generated delegator methods

  @Override
  public int size() {
    return doc.size();
  }

  @Override
  public E asElement(N node) {
    return doc.asElement(node);
  }

  @Override
  public T asText(N node) {
    return doc.asText(node);
  }

  @Override
  public Map<String, String> getAttributes(E element) {
    return doc.getAttributes(element);
  }

  @Override
  public String getData(T textNode) {
    return doc.getData(textNode);
  }

  @Override
  public E getDocumentElement() {
    return doc.getDocumentElement();
  }

  @Override
  public N getFirstChild(N node) {
    return doc.getFirstChild(node);
  }

  @Override
  public N getLastChild(N node) {
    return doc.getLastChild(node);
  }

  @Override
  public int getLength(T textNode) {
    return doc.getLength(textNode);
  }

  @Override
  public N getNextSibling(N node) {
    return doc.getNextSibling(node);
  }

  @Override
  public short getNodeType(N node) {
    return doc.getNodeType(node);
  }

  @Override
  public E getParentElement(N node) {
    return doc.getParentElement(node);
  }

  @Override
  public N getPreviousSibling(N node) {
    return doc.getPreviousSibling(node);
  }

  @Override
  public String getTagName(E element) {
    return doc.getTagName(element);
  }

  @Override
  public boolean isSameNode(N node, N other) {
    return doc.isSameNode(node, other);
  }

  @Override
  public String getAttribute(E element, String name) {
    return doc.getAttribute(element, name);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    return doc.firstAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public int lastAnnotationChange(int start, int end, String key, String fromValue) {
    return doc.lastAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public String getAnnotation(int start, String key) {
    return doc.getAnnotation(start, key);
  }

  @Override
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    return doc.annotationCursor(start, end, keys);
  }

  @Override
  public Point<N> locate(int location) {
    return doc.locate(location);
  }

  @Override
  public int getLocation(N node) {
    return doc.getLocation(node);
  }

  @Override
  public int getLocation(Point<N> point) {
    Preconditions.checkNotNull(point, "getLocation: Null point");
    return doc.getLocation(point);
  }

  @Override
  public void forEachAnnotationAt(int location, ReadableStringMap.ProcV<String> callback) {
    doc.forEachAnnotationAt(location, callback);
  }

  @Override
  public Iterable<AnnotationInterval<String>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    return doc.annotationIntervals(start, end, keys);
  }

  @Override
  public Iterable<RangedAnnotation<String>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    return doc.rangedAnnotations(start, end, keys);
  }

  @Override
  public ReadableStringSet knownKeys() {
    return doc.knownKeys();
  }

  @Override
  public DocInitialization toInitialization() {
    return doc.toInitialization();
  }

  @Override
  public String toXmlString() {
    return doc.toXmlString();
  }

  @Override
  public String toDebugString() {
    return doc.toDebugString();
  }

  @Override
  public String toString() {
    return "MutableDI@" + Integer.toHexString(System.identityHashCode(this))
      + "[" + toDebugString() + "]";
  }
}
