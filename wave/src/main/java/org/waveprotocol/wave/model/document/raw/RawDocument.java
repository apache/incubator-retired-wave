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

package org.waveprotocol.wave.model.document.raw;

import org.waveprotocol.wave.model.document.DocumentFactory;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.util.OffsetList;

import java.util.Map;

/**
 * This represents a DOM Document that holds a list of actions that can be done
 * on a Node, Element and TextNode.
 *
 * The interface is largely isomorphic to the W3C DOM, with some exceptions.
 *
 * Most importantly, nodes may not exist unattached to the DOM. That is, they
 * are created in place, and once removed from their parent, they are to be
 * considered destroyed.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author alexmah@google.com (Alex Mah)
 *
 * @param <N> Node
 * @param <E> Element
 * @param <T> Text
 */
public interface RawDocument<N, E extends N, T extends N> extends ReadableDocument<N, E, T>,
    TextNodeOrganiser<T> {

  /**
   * Creates an element node in place with the given tag name and attributes.
   *
   * @param tagName The tag name of the node to create.
   * @param attributes The initial attributes of the node (must not be null)
   * @param parent The new node's parent
   * @param nodeAfter The new node's next sibling (or null if it is to be last
   *        child of its parent)
   * @return The new element node.
   */
  E createElement(String tagName, Map<String, String> attributes, E parent, N nodeAfter);

  /**
   * Creates a text node in place that contains the given text
   *
   * @param data the text contained in the new text node.
   * @param parent The new node's parent
   * @param nodeAfter The new node's next sibling (or null if it is to be last
   *        child of its parent)
   * @return The new text node.
   */
  T createTextNode(String data, E parent, N nodeAfter);

  /**
   * Inserts the newChild before the refChild that the parent contains.
   *
   * @param parent Parent of refChild
   * @param newChild The node to insert
   * @param refChild The reference node.
   * @return The newly inserted child.
   */
  N insertBefore(E parent, N newChild, N refChild);

  /**
   * Inserts a range of nodes before a reference node.
   *
   * @param parent Parent of refChild
   * @param from The first node in the range to insert (inclusive)
   * @param to The last node in the range to insert (exclusive)
   * @param refChild The reference node.
   * @return The newly inserted child.
   */
  N insertBefore(E parent, N from, N to, N refChild);

  /**
   * Removes the oldChild from parent. The removed node cannot be assumed to be
   * usable any more.
   *
   * @param parent Parent of oldChild.
   * @param oldChild The node to remove.
   */
  void removeChild(E parent, N oldChild);

  /**
   * Sets the attribute on a given element.
   *
   * @param element The element to set the attribute on
   * @param name The name of the attribute. Cannot be null.
   * @param value Value of the attribute. Cannot be null.
   */
  void setAttribute(E element, String name, String value);

  /**
   * Removes the attribute from the element.
   *
   * @param element The Element to remove the attribute.
   * @param name The name of the attribute. Cannot be null. If attribute does
   *        not exist, nothing happens.
   */
  void removeAttribute(E element, String name);

  /**
   * Appends the given string to the textNode.
   *
   * @param textNode The text node to append new text to the back.
   * @param arg The new text to append.
   */
  void appendData(T textNode, String arg);

  /**
   * Inserts the given text into an offset in the TextNode.
   *
   * @param textNode The text node to insert the text.
   * @param offset The offset in the text node to insert. If offset > textNode's
   *        text length, the new text is append to the back.
   * @param arg The text to insert.
   */
  void insertData(T textNode, int offset, String arg);

  /**
   * Deletes text from the given text node in the specified range
   *
   * @param textNode The text node to delete text from
   * @param offset The offset of the start of deletion
   * @param count The number of characters to delete
   */
  void deleteData(T textNode, int offset, int count);

  /**
   * Gets the container used to index the given DOM node.
   *
   * @param domNode The DOM node.
   * @return The indexing node.
   */
  OffsetList.Container<N> getIndexingContainer(N domNode);

  /**
   * Registers the container used to index the given DOM node.
   *
   * @param domNode The DOM node.
   * @param indexingContainer The indexing container.
   */
  void setIndexingContainer(N domNode, OffsetList.Container<N> indexingContainer);

  /**
   * Specialization of {@link DocumentFactory} for {@link RawDocument}.
   *
   * @param <D> document type produced
   */
  interface Factory<D extends RawDocument<?,?,?>> extends DocumentFactory<D> {
  }

  /**
   * Specialization of {@link ReadableDocument.Provider} for {@link RawDocument}.
   *
   * @param <D> document type produced
   */
  interface Provider<D extends RawDocument<?, ?, ?>>
      extends ReadableDocument.Provider<D>, RawDocument.Factory<D> {}
}
