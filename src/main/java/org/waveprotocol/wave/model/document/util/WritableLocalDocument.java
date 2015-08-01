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

import java.util.Map;

/**
 * Write-only part of the {@link LocalDocument} interface.
 *
 * TODO(danilatos): First cut interface. Document thoroughly once it has
 * stabilised.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public interface WritableLocalDocument<N, E extends N, T extends N> extends ElementManager<E> {

  /**
   * Sets an attribute on a transparent node.
   *
   * May not be used on a persistent node.
   *
   * @param name
   * @param value
   */
  void transparentSetAttribute(E element, String name, String value);

  /**
   * Create an additional local text node
   */
  T transparentCreate(String text, E parent, N nodeAfter);

  /**
   * Create an additional local element
   */
  E transparentCreate(String tagName, Map<String, String> attributes, E parent, N nodeAfter);

  /**
   * Remove a local element, reparenting its children to the removed element's
   * parent
   *
   * @param element
   */
  void transparentUnwrap(E element);

  /**
   * Move some dom around. Will throw a runtime exception if the change would
   * affect the persistent view.
   *
   * @param newParent
   * @param fromIncl
   * @param toExcl
   * @param refChild
   */
  void transparentMove(E newParent, N fromIncl, N toExcl, N refChild);

  /**
   * Remove a subtree. All nodes in the subtree must be local.
   *
   * @param node
   */
  void transparentDeepRemove(N node);

  /**
   * Slice upwards through transparent nodes until we find a non-transparent nodes
   *
   * @param splitAt The node at which to split
   * @return The new "split point" after slicing upwards (so, either the original
   *   splitAt node, or the second half of the topmost split transparent node)
   */
  N transparentSlice(N splitAt);

  /**
   * Marks a node to be persisted only when its needed (defined as its position being filtered).
   * NOTE(patcoleman): This must be given a local node, with no children persisted.
   *
   * @param localNode The node to promoted.
   */
  void markNodeForPersistence(N localNode, boolean lazy);

  /**
   * Check whether the given (non-null) node is transparent = local only.
   */
  boolean isTransparent(N node);
}
