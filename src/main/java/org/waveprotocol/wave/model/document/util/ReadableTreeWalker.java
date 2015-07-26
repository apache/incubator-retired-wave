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
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * Depth-first (pre-order) iteration walker of a subtree within a readable document.
 * Checks information about each node in turn, then moves to the next node automatically.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class ReadableTreeWalker<N, E extends N, T extends N> {
  /** Document the subtree resides in. */
  protected final ReadableDocument<N, E, T> document;

  /** Iterator used to walk over the nodes. */
  protected final Iterator<N> nodeWalker;

  /** Current node we are at in the subtree. */
  protected N currentNode;

  /**
   * Creates the tree walker by forming an iterator along all nodes in a subtree.
   */
  public ReadableTreeWalker(ReadableDocument<N, E, T> document, N rootNode) {
    this(document, DocIterate.iterate(document, rootNode,
        DocHelper.getNextOrPrevNodeDepthFirst(document, rootNode, null, false, true),
        DocIterate.<N, E, T>forwardDepthFirstIterator()));
  }

  /**
   * Creates the tree walker, taking the document it walks plus an iterator for the path.
   */
  public ReadableTreeWalker(ReadableDocument<N, E, T> document, Iterable<N> nodeIterable) {
    this.document = document;
    this.nodeWalker = nodeIterable.iterator();
  }

  /**
   * Checks whether the current walk is the correct type of element.
   * @throws IllegalStateException if the element does not match the expected state.
   * @return the checked element.
   */
  public E checkElement(String tagName, Map<String, String> attributes) {
    Preconditions.checkState(nodeWalker.hasNext(),
        "Tree Walker: no more nodes to walk, element expected");

    progress();

    E element = document.asElement(currentNode);

    Preconditions.checkState(element != null,
        "Tree Walker: At text node, element expected");
    Preconditions.checkState(document.getTagName(element).equals(tagName),
        "Tree Walker: Incorrect tag name");
    Preconditions.checkState(ValueUtils.equal(document.getAttributes(element), attributes),
        "Tree Walker: Incorrect attributes");

    return element;
  }

  /**
   * Checks whether the current walk is a text node with the right data.
   * @throws IllegalStateException if the text node does not match the expected state.
   * @return the checked text node
   */
  public T checkTextNode(String data) {
    Preconditions.checkState(nodeWalker.hasNext(),
        "Tree Walker: no more nodes to walk, text node expected");

    progress();

    T textNode = document.asText(currentNode);

    Preconditions.checkState(textNode != null,
        "Tree Walker: At element, text node expected");
    Preconditions.checkState(document.getData(textNode).equals(data),
        "Tree Walker: Incorrect text node data");

    return textNode;
  }

  /**
   * Makes sure that the entire tree has been walked.
   * @throws IllegalStateException if we are not at the end of the path.
   * @return whether we have walked to the next node outside the tree.
   */
  public boolean checkComplete() {
    Preconditions.checkState(!nodeWalker.hasNext(),
        "Tree Walker: Did not end walk at the correct node");
    return !nodeWalker.hasNext();
  }

  /** Performs a walk against a document subtree, returns true if all checks pass. */
  public boolean checkWalk(ReadableDocument<N, E, T> doc, N root) {
    // curry in the forwards depth first iterator.
    N stopAt = DocHelper.getNextOrPrevNodeDepthFirst(doc, root, null, false, true);
    return checkWalk(doc, DocIterate.iterate(doc, root, stopAt,
        DocIterate.<N, E, T>forwardDepthFirstIterator()));
  }

  /** Performs a walk against an iterable collection of nodes, returns true if all checks pass. */
  public boolean checkWalk(ReadableDocument<N, E, T> doc, Iterable<N> nodes) {
    try {
      for (N node : nodes) {
        E elt = doc.asElement(node);
        if (elt != null) {
          checkElement(doc.getTagName(elt), doc.getAttributes(elt));
        } else {
          checkTextNode(doc.getData(doc.asText(node)));
        }
      }
      return checkComplete();
    } catch (IllegalStateException e) {
      return false; // assertion failed.
    }
  }

  /** Utility to move the node one along. */
  protected void progress() {
    currentNode = nodeWalker.next();
  }
}
