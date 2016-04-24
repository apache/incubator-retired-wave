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

package org.waveprotocol.wave.client.editor;


/**
 * Temporary interface in the spirit of the agent-style interfaces to be created,
 * equivalent to MutatingNode
 *
 * @param <N>
 * @param <E>
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface NodeMutationHandler<N, E extends N> {

  /**
   * This method is called in two situations: when a new element is being
   * created, or when the handler is being enabled on a document, in which case
   * it will be called for all existing elements when the handler has been
   * registered for.
   *
   * In the case where it is a new element, the element will already have all
   * its attributes initialised. In the case where the element already existed,
   * it may also already have children. In these circumstances the corresponding
   * mutation events such as
   * {@link #onAttributeModified(Object, String, String, String)} and
   * {@link #onChildAdded(Object, Object)} will not be called.
   *
   * This method may also be called for rendering handlers after a repair is
   * triggered.
   *
   * No guarantees are provided as to when it will NOT be called - it might be
   * called at any time. The guarantees are for when it will at least be called.
   *
   * If child nodes are also to have this method called on them, it is called
   * first for the parent node. Contrast with {@link #onActivatedSubtree(Object)}
   *
   * @param element Element to inspect
   */
  void onActivationStart(E element);

  /**
   * Called in addition to {@link #onActivationStart(Object)}, after it has been
   * called for child nodes.
   *
   * E.g. order of calling: parent.onActivationStart(), child.onActivationStart(),
   * child.onActivatedSubtree(), parent.onActivatedSubtree()
   *
   * @param element
   */
  void onActivatedSubtree(E element);

  /**
   * Called when the handler is disabled for the given element. The handler is
   * either being removed from the document, or the element is being destroyed.
   *
   * @param element
   */
  void onDeactivated(E element);

  /**
   * Called when this node has been added to a parent.
   *
   * @param oldParent The previous parent this node was attached to (null if none)
   */
  void onAddedToParent(E element, E oldParent);

  /**
   * Called when this node IS ABOUT TO BE removed from its parent.
   * Note, however, that the html implementation will already have been
   * affected.
   *
   * @param newParent the new parent we intend to move it to (null if none)
   */
  void onRemovedFromParent(E element, E newParent);

  /**
   * Called when a child HAS BEEN added to this element
   *
   * @param child The node that was added
   */
  void onChildAdded(E element, N child);

  /**
   * Called when a child IS ABOUT TO BE removed from this element
   * Note, however, that the html implementation will already have been
   * affected. (However, the exact state of the html should be treated
   * as being undefined)
   *
   * @param child The node that was removed
   */
  void onChildRemoved(E element, N child);

  /**
   * Called when an attribute on this element has been modified.
   *
   * Not called for new elements, see {@link #onActivationStart(Object)}
   *
   * @param name Name of the attribute
   * @param oldValue the old value
   * @param newValue the new value
   */
  void onAttributeModified(E element, String name,
      String oldValue, String newValue);

  /**
   * Called after all children have been removed from a node.
   *
   * TODO(user, danilatos): it'd be nice if we could call this
   * when constructing an empty element as well such that Paragraph
   * needn't call it during its constructor.
   */
  void onEmptied(E element);

  /**
   * Called when one or more descendants of this element has changed.
   *
   * NOTE(user): this method is typically called several times
   * when, for example, a single operation is applied.
   */
  void onDescendantsMutated(E element);
}
