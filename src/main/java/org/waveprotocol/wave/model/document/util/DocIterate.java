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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

import java.util.Iterator;

/**
 * Helpers for iterating through documents
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocIterate {

  /**
   * @param <V> Value to map the nodes to.
   * @param <N> Node type in the document this filter is over.
   * @param <E> Element type in the document this filter is over.
   * @param <T> Text node type in the document this filter is over.
   */
  public interface DocIterationFilter<V, N, E extends N, T extends N> {
    /**
     * @param doc useful to keep most implementations as singletons
     * @param current the current node
     * @param stopAt the node to stop at (exclude)
     * @return the next node in the iteration
     */
    N next(ReadableDocument<N, E, T> doc, N current, N stopAt);

    /**
     * @param doc useful to keep most implementations as singletons
     * @param node
     * @return the value the node should map to
     */
    V value(ReadableDocument<N, E, T> doc, N node);
  }

  /**
   * Iterates using through the document using a DocIterationFilter.
   *
   * @param doc
   * @param startNode
   * @param iterateFunction
   * @param stopAt the exact meaning depends on the given iteration function, in
   *        particular whether the node is excluded on the way in or on the way
   *        out of traversal
   */
  public static <V, N, E extends N, T extends N> Iterable<V> iterate(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt,
      final DocIterationFilter<V, N, E, T> iterateFunction) {
    return new Iterable<V>() {
      @Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          N next = startNode;

          @Override
          public boolean hasNext() {
            return next != null;
          }

          @Override
          public V next() {
            N currentNode = next;
            assert currentNode != null;
            next = iterateFunction.next(doc, currentNode, stopAt);
            return iterateFunction.value(doc, currentNode);
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove");
          }
        };
      }
    };
  }

  /*
   * Filters
   */

  /** Depth-first recursive iterator that navigates forwards or backwards through the tree. */
  static class DeepIteratorFilter implements DocIterationFilter<Object, Object, Object, Object> {
    private final boolean rightwards;
    DeepIteratorFilter(boolean rightwards) {
      this.rightwards = rightwards; // store direction.
    }

    @Override
    public Object next(ReadableDocument<Object, Object, Object> doc,
        Object current, Object stopAt) {
      return DocHelper.getNextOrPrevNodeDepthFirst(doc, current, stopAt, true, rightwards);
    }
    @Override
    public Object value(ReadableDocument<Object, Object, Object> doc, Object node) {
      return node;
    }
  }

  /** Deep Iterator that skips text nodes. */
  static class ElementFilter extends DeepIteratorFilter {
    public ElementFilter(boolean rightwards) {
      super(rightwards);
    }

    @Override
    public Object next(ReadableDocument<Object, Object, Object> doc,
        Object current, Object stopAt) {
      Object next = super.next(doc, current, stopAt);
      while (next != null && doc.asElement(next) == null) {
        next = super.next(doc, next, stopAt);
      }
      return next;
    }

    @Override
    public Object value(ReadableDocument<Object, Object, Object> doc, Object node) {
      Object ret = doc.asElement(super.value(doc, node));
      assert ret != null;
      return ret;
    }
  }

  /**
   * Iteration filter that filters out elements by tag name.
   */
  static class ElementByTagNameFilter extends ElementFilter {
    private final String tagName;

    public ElementByTagNameFilter(String tagName, boolean rightwards) {
      super(rightwards);
      this.tagName = tagName;
    }

    @Override
    public Object next(ReadableDocument<Object, Object, Object> doc,
        Object current, Object stopAt) {
      Object next = super.next(doc, current, stopAt);
      while (next != null && !doc.getTagName(doc.asElement(next)).equals(tagName)) {
        next = super.next(doc, next, stopAt);
      }
      return next;
    }
  }

  /*
   * Overly generic (Object-based) implementations
   * NOTE(patcoleman): where possible , access by the static casting getter methods versions.
   */

  /** Depth first forwards/backwards traversal of the tree nodes. */
  static final DocIterationFilter<Object, Object, Object, Object>
      FORWARD_DEPTH_FIRST_ITERATOR = new DeepIteratorFilter(true);
  static final DocIterationFilter<Object, Object, Object, Object>
      REVERSE_DEPTH_FIRST_ITERATOR = new DeepIteratorFilter(false);

  /** Backwards and forwards element iterator filters. */
  static final DocIterationFilter<Object, Object, Object, Object>
      FORWARD_DEPTH_FIRST_ELEMENT_ITERATOR = new ElementFilter(true);
  static final DocIterationFilter<Object, Object, Object, Object>
      REVERSE_DEPTH_FIRST_ELEMENT_ITERATOR = new ElementFilter(false);

  /*
   * Casting wrappers for typesafe iteration. See funge section at the end for sources.
   * All return DocIterationFilter<?,?,?,?>
   */

  /** @return A Forward depth first iterator bound to an (N, E, T) type tuple. */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N>
      DocIterationFilter<N, N, E, T> forwardDepthFirstIterator() {
    return (DocIterationFilter<N, N, E, T>) fungeForwardDepthFirstIterator();
  }

  /** @return A Backwards depth first iterator bound to an (N, E, T) type tuple. */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N>
      DocIterationFilter<N, N, E, T> reverseDepthFirstIterator() {
    return (DocIterationFilter<N, N, E, T>) fungeReverseDepthFirstIterator();
  }

  /** @return A Forward depth first iterator bound to an (N, E, T) type tuple. */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N>
      DocIterationFilter<E, N, E, T> forwardDepthFirstElementIterator() {
    return (DocIterationFilter<E, N, E, T>) fungeForwardDepthFirstElementIterator();
  }

  /** @return A Backwards depth first iterator bound to an (N, E, T) type tuple. */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N>
      DocIterationFilter<E, N, E, T> reverseDepthFirstElementIterator() {
    return (DocIterationFilter<E, N, E, T>) fungeReverseDepthFirstElementIterator();
  }

  /** @return A Forwards depth first element iterator bound to an (N, E, T) type tuple. */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N>
      DocIterationFilter<E, N, E, T> forwardDepthFirstElementByTagNameIterator(String tag) {
    return (DocIterationFilter<E, N, E, T>) fungeForwardDepthFirstElementTagNameIterator(tag);
  }

  /*
   * Iterators - all of these should return Iterable<?>
   */

  /**
   * Iterates using
   * {@link DocHelper#getNextNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * The parameters to this method match those for that method.
   *
   * @param doc
   * @param startNode
   * @param stopAt
   */
  public static <N, E extends N, T extends N> Iterable<N> deep(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt) {
    return iterate(doc, startNode, stopAt, DocIterate.<N, E, T>forwardDepthFirstIterator());
  }

  /**
   * Iterates using
   * {@link DocHelper#getPrevNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * The parameters to this method match those for that method.
   *
   * @param doc
   * @param startNode
   * @param stopAt
   */
  public static <N, E extends N, T extends N> Iterable<N> deepReverse(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt) {
    return iterate(doc, startNode, stopAt, DocIterate.<N, E, T>reverseDepthFirstIterator());
  }

  /**
   * Same as {@link #deep(ReadableDocument, Object, Object)}, but filters out
   * non-elements
   */
  public static <N, E extends N, T extends N> Iterable<E> deepElements(
      final ReadableDocument<N, E, T> doc, final E startNode, final E stopAt) {
    return iterate(doc, startNode, stopAt, DocIterate.<N, E, T>forwardDepthFirstElementIterator());
  }

  /**
   * Same as {@link #deepReverse(ReadableDocument, Object, Object)}, but filters out
   * non-elements
   */
  public static <N, E extends N, T extends N> Iterable<E> deepElementsReverse(
      final ReadableDocument<N, E, T> doc, final E startNode, final E stopAt) {
    return iterate(doc, startNode, stopAt, DocIterate.<N, E, T>reverseDepthFirstElementIterator());
  }

  /** See {@link #deepElementsWithTagName(ReadableDocument, String, Object, Object)}. */
  public static <N, E extends N, T extends N> Iterable<E> deepElementsWithTagName(
      final ReadableDocument<N, E, T> doc, String tagName) {
    return deepElementsWithTagName(
        doc, tagName, DocHelper.getElementWithTagName(doc, tagName), null);
  }

  /**
   * Same as {@link #deepElements(ReadableDocument, Object, Object)}, but filters out
   * elements that do not match the given tag name.
   *
   * @return an iterable used for iterating matching elements.
   */
  public static <N, E extends N, T extends N> Iterable<E> deepElementsWithTagName(
      final ReadableDocument<N, E, T> doc, String tagName, final E startNode, final E stopAt) {
    return iterate(doc, startNode, stopAt,
        DocIterate.<N, E, T>forwardDepthFirstElementByTagNameIterator(tagName));
  }

  /*
   * Funge methods for unfurling generics, required only for Sun JDK compiler.
   */

  /** @return A Forward depth first filter funged to the type N. */
  @SuppressWarnings("unchecked")
  private static <N> DocIterationFilter<?, N, ?, ?> fungeForwardDepthFirstIterator() {
    return (DocIterationFilter<?, N, ?, ?>) FORWARD_DEPTH_FIRST_ITERATOR;
  }

  /** @return A Backwards depth first filter funged to the type N. */
  @SuppressWarnings("unchecked")
  private static <N> DocIterationFilter<?, N, ?, ?> fungeReverseDepthFirstIterator() {
    return (DocIterationFilter<?, N, ?, ?>) REVERSE_DEPTH_FIRST_ITERATOR;
  }

  /** @return A Forward depth first element filter funged to the type N. */
  @SuppressWarnings("unchecked")
  private static <N> DocIterationFilter<?, N, ?, ?> fungeForwardDepthFirstElementIterator() {
    return (DocIterationFilter<?, N, ?, ?>) FORWARD_DEPTH_FIRST_ELEMENT_ITERATOR;
  }

  /** @return A Backwards depth first element filter funged to the type N. */
  @SuppressWarnings("unchecked")
  private static <N> DocIterationFilter<?, N, ?, ?> fungeReverseDepthFirstElementIterator() {
    return (DocIterationFilter<?, N, ?, ?>) REVERSE_DEPTH_FIRST_ELEMENT_ITERATOR;
  }

  /** @return A Forward depth first element filter by tag name, funged to the type N. */
  @SuppressWarnings("unchecked")
  private static <N> DocIterationFilter<?, N, ?, ?> fungeForwardDepthFirstElementTagNameIterator(
      String tagName) {
    // NOTE(patcoleman): Must remain on two lines so the compiler knows what it's doing.
    // Here we have to funge an ElementByTagName filter into a DocIterationFilter, then funge in
    // the N type, then funge in the remainding generics. Fun.
    DocIterationFilter<Object, Object, Object, Object> filter
        = new ElementByTagNameFilter(tagName, true);
    return (DocIterationFilter<?, N, ?, ?>) filter;
  }
}
