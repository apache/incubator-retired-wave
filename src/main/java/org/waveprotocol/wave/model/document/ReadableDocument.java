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

import org.waveprotocol.wave.model.document.util.DocumentProvider;

import java.util.Map;

/**
 * This interface allows us to easily create various views of a document without having to
 * wrap the nodes for each view.
 *
 * It uses generics so that the user of this interface can decide what are the concrete types.
 *
 * Contract Notes:
 * <ul>
 * <li>Empty text nodes are invalid</li>
 * </ul>
 *
 * @author danilatos@google.com (Dan Danilatos)
 *
 * @param <N> The type of Node
 * @param <E> The type of Element
 * @param <T> The type of TextNode
 */
public interface ReadableDocument<N,E extends N,T extends N> {

  /**
   * This is a convenience attribute that allows direct access to the child
   * node that is the document element of the document.
   *
   * @return The single root element of the document.
   */
  E getDocumentElement();

  /**
   * Gets the element node that is the parent of the given node.
   *
   * @param node The node whose parent is to be obtained.
   * @return The parent of the given node.
   */
  E getParentElement(N node);

  /**
   * Gets the type of the given node. The possible return values are defined in
   * org.waveprotocol.wave.model.document.indexed.NodeType.
   *
   * @param node The node whose type is to be obtained.
   * @return The type of the given node.
   */
  short getNodeType(N node);

  /**
   * Gets the first child of the given node.
   *
   * @param node The node whose first child is to be obtained.
   * @return The first child of the given node.
   */
  N getFirstChild(N node);

  /**
   * Gets the last child of the given node.
   *
   * @param node The node whose last child is to be obtained.
   * @return The last child of the given node.
   */
  N getLastChild(N node);

  /**
   * Gets the previous sibling of the given node.
   *
   * @param node The node whose previous sibling is to be obtained.
   * @return The previous sibling of the given node.
   */
  N getPreviousSibling(N node);

  /**
   * Gets the next sibling of the given node.
   *
   * @param node The node whose next sibling is to be obtained.
   * @return The next sibling of the given node.
   */
  N getNextSibling(N node);

  /**
   * Determines whether two nodes are the same node.
   * TODO(danilatos): Maybe can we get rid of this?
   * Point's equals() method isn't safe if we can't assume .equals() is
   * enough to compare node equality. For simplicity I've also made
   * Point's isIn() method just use .equals() for equality.
   *
   * @param node
   * @param other
   * @return True either if node and other are both null or if they are the same
   *         node.
   */
  boolean isSameNode(N node, N other);

  /**
   * Gets the tag name of the given element node.
   *
   * @param element The element whose tag name is to be obtained.
   * @return The tag name of the given element.
   */
  String getTagName(E element);

  /**
   * Gets the attribute of the given element node which has the given name.
   *
   * @param element The element node whose attribute is to be obtained.
   * @param name The name of the attribute to be obtain.
   * @return The attribute of the given element node which has the given name.
   */
  String getAttribute(E element, String name);

  /**
   * Gets the attributes of the given element node. It is undefined whether or
   * not this map is a live map
   *
   * @param element The element node whose attributes are to be obtained.
   * @return A map that maps the names of the attributes of an element node to
   *         their corresponding value.
   */
  Map<String,String> getAttributes(E element);

  /**
   * Gets the character data of a text node.
   *
   * @param textNode The text node whose character data is to be obtained.
   * @return The character data of the given text node.
   */
  String getData(T textNode);

  /**
   * Gets the length of the character data of a text node.
   *
   * @param textNode The text node, the length of whose character data is to be
   *        obtained.
   * @return The length of the character data of the given text node.
   */
  int getLength(T textNode);

  /**
   * Casts this node to be an element node if it is an element node, returning
   * null if this node is not an element node.
   *
   * @param node The node to be cast to an element node.
   * @return This node as an element node, or null if this node is not an
   *         element node.
   */
  E asElement(N node);

  /**
   * Casts this node to be a text node if it is a text node, returning null if
   * this node is not a text node.
   *
   * @param node The node to be cast to a text node.
   * @return This node as a text node, or null if this node is not a text node.
   */
  T asText(N node);

  /**
   * Specialization of {@link DocumentProvider} for {@link ReadableDocument}.
   *
   * @param <D> document type produced
   */
  interface Provider<D extends ReadableDocument<?,?,?>> extends DocumentProvider<D> {}
}
