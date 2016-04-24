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

package org.waveprotocol.wave.client.editor.content;

/**
 * Interface of DOM mutation events on ContentNodes. ContentNode
 * implements all these with empty methods such that subclasses can
 * override only those they wish to actually implement.
 *
 * NOTE: these methods are not safe for re-entrancy; implementers should
 * *not* manipulate the content dom directly, but rather use deferred
 * commands or such.
 *
 */
public interface MutatingNode<N,E extends N> {

  /**
   * Called when this node has been added to a parent.
   *
   * @param oldParent The previous parent this node was attached to (null if none)
   */
  public void onAddedToParent(E oldParent);

  /**
   * Called when this node IS ABOUT TO BE removed from its parent.
   * Note, however, that the html implementation will already have been
   * affected.
   *
   * @param newParent the new parent we intend to move it to (null if none)
   */
  public void onRemovedFromParent(E newParent);

  /**
   * Called when a child HAS BEEN added to this element
   *
   * @param child The node that was added
   */
  public void onChildAdded(N child);

  /**
   * Called when a child IS ABOUT TO BE removed from this element
   * Note, however, that the html implementation will already have been
   * affected. (However, the exact state of the html should be treated
   * as being undefined)
   *
   * @param child The node that was removed
   */
  public void onChildRemoved(N child);

  /**
   * Called when an attribute on this element has been modified.
   *
   * @param name Name of the attribute
   * @param oldValue the old value
   * @param newValue the new value
   */
  public void onAttributeModified(String name, String oldValue, String newValue);
  /**
   * Called after all children have been removed from a node.
   *
   * TODO(user, danilatos): it'd be nice if we could call this
   * when constructing an empty element as well such that Paragraph
   * needn't call it during its constructor.
   */
  public void onEmptied();

  /**
   * Called when one or more descendants of this element has changed.
   *
   * NOTE(user): this method is typically called several times
   * when, for example, a single operation is applied.
   */
  public void onDescendantsMutated();
}
