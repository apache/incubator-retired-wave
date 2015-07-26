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

/**
 * Filtered view implementation. Works by composing a view of the document, and
 * filters out elements based on what the method {@link #getSkipLevel(Object)},
 * says about them. Subclass to implement that method.
 * TODO(danilatos): Change to composition of a getSkipLevel interface rather than
 * using inheritance.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public abstract class FilteredView<N, E extends N, T extends N>
    extends IdentityView<N, E, T>
    implements ReadableDocumentView<N, E, T> {

  /**
   * @author danilatos@google.com (Daniel Danilatos)
   */
  public enum Skip {
    /** Do not skip */
    NONE,
    /** Skip, but traverse into children */
    SHALLOW,
    /** Skip including entire subtree rooted at node */
    DEEP,
    /** Invalid - e.g. in the html outside the document */
    INVALID
  }

  protected abstract Skip getSkipLevel(N node);

  /**
   * Constructor
   * @param innerView View we are boxing
   */
  public FilteredView(ReadableDocument<N, E, T> innerView) {
    super(innerView);
  }

  @Override
  public N getFirstChild(N node) {
    N find = inner.getFirstChild(node);
    return getNextVisibleNodeDepthFirst(find, node, true);
  }

  @Override
  public N getLastChild(N node) {
    N find = inner.getLastChild(node);
    return getPreviousVisibleNodeDepthFirst(find, node, true);
  }

  @Override
  public N getNextSibling(N node) {
    E parent = getParentElement(node);
    N find = getNextNodeDepthFirst(inner, node, parent, false);
    return getNextVisibleNodeDepthFirst(find, parent, true);
  }

  @Override
  public N getPreviousSibling(N node) {
    E parent = getParentElement(node);
    N find = getPrevNodeDepthFirst(inner, node, parent, false);
    return getPreviousVisibleNodeDepthFirst(find, parent, true);
  }

  @Override
  public E getParentElement(N node) {
    E element = inner.getParentElement(node);

    while (element != null) {
      switch (getSkipLevel(element)) {
        case NONE:
          return element;
        case SHALLOW:
          element = inner.getParentElement(element);
          break;
        case DEEP:
          // Shouldn't really happen, but better handle it
          element = inner.getParentElement(element);
          break;
        case INVALID:
          return null;
        default:
          throw new RuntimeException("Unimplemented");
      }
    }
    return null;
  }

  @Override
  public N getVisibleNodeNext(N node) {
    return getNextVisibleNodeDepthFirst(node, null, false);
  }

  @Override
  public N getVisibleNodePrevious(N node) {
    return getPreviousVisibleNodeDepthFirst(node, null, false);
  }

  @Override
  public N getVisibleNodeFirst(N node) {
    return getNextVisibleNodeDepthFirst(node, null, true);
  }

  @Override
  public N getVisibleNodeLast(N node) {
    return getPreviousVisibleNodeDepthFirst(node, null, true);
  }

  @Override
  public N getVisibleNode(N node) {
    if (node == null) {
      return node;
    }
    switch (getSkipLevel(node)) {
      case NONE:
        return node;
      case SHALLOW:
      case DEEP:
        return getParentElement(node);
      case INVALID:
        return null;
      default:
        throw new RuntimeException("Unimplemented");
    }
  }

  @Override
  public void onBeforeFilter(Point<N> at) {
    // default = do nothing.
  }

  // Helpers

  private N getNextVisibleNodeDepthFirst(N find, N stopAt, boolean enterFirst) {
    boolean enter = enterFirst;
    while (find != null) {
      switch (getSkipLevel(find)) {
        case NONE:
          return find;
        case SHALLOW:
          find = getNextNodeDepthFirst(inner, find, stopAt, enter);
          break;
        case DEEP:
          find = getNextNodeDepthFirst(inner, find, stopAt, false);
          break;
        case INVALID:
          return null;
        default:
          throw new RuntimeException("Unimplemented");
      }
      enter = true;
    }
    return null;
  }

  private N getPreviousVisibleNodeDepthFirst(N find, N stopAt, boolean enterFirst) {
    boolean enter = enterFirst;
    while (find != null) {
      switch (getSkipLevel(find)) {
        case NONE:
          return find;
        case SHALLOW:
          find = getPrevNodeDepthFirst(inner, find, stopAt, enter);
          break;
        case DEEP:
          find = getPrevNodeDepthFirst(inner, find, stopAt, false);
          break;
        case INVALID:
          return null;
        default:
          throw new RuntimeException("Unimplemented");
      }
      enter = true;
    }
    return null;
  }


  private N getNextNodeDepthFirst(
      ReadableDocument<N, E, T> doc, N start, N stopAt, boolean enter) {
    return getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, true);
  }

  private N getPrevNodeDepthFirst(
      ReadableDocument<N, E, T> doc, N start, N stopAt, boolean enter) {
    return getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, false);
  }

  protected N getNextOrPrevNodeDepthFirst(
      ReadableDocument<N, E, T> doc, N start, N stopAt, boolean enter, boolean rightwards) {
    return DocHelper.getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, rightwards);
  }
}
