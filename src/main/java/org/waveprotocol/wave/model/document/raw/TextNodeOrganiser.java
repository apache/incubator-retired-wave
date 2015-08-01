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

/**
 * Interface for making changes to text nodes, such that the XML itself is not
 * affected.
 *
 * @param <T> Text node type
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// TODO(danilatos): Consider merging this interface elsewhere.
public interface TextNodeOrganiser<T> {

  /**
   * Splits the given text node at the given offset, and return the resulting
   * second node.
   *
   * If offset is zero, no split occurs, and the given text node is returned. If
   * the offset is greater than, or equal to, the length of the given text node,
   * null will be returned.
   *
   * @param textNode
   * @param offset
   * @return the resulting second node, or null if the split failed
   */
  T splitText(T textNode, int offset);

  /**
   * Merges secondSibling and its previous sibling, which must exist and be a
   * text node.
   *
   * The merge may fail, if it is not possible for the text node to merge with
   * its previous sibling, even if it too is a text node (perhaps because this
   * is a filtered view of the document, and in some other view they are not
   * siblings).
   *
   * @param secondSibling
   * @return the first sibling, which is now merged with secondSibling, or null
   *         if the merge failed.
   */
  T mergeText(T secondSibling);
}
