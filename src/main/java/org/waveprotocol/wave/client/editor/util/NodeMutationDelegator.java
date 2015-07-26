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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.client.editor.NodeMutationHandler;

/**
 * Convenience class that delegates to another implementation.
 *
 * Useful for composition.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class NodeMutationDelegator<N, E extends N> implements NodeMutationHandler<N, E> {

  /**
   * Handler to delegate to
   */
  protected final NodeMutationHandler<N, E> delegate;

  /**
   * @param delegate Handler to delegate to
   */
  public NodeMutationDelegator(NodeMutationHandler<N, E> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onActivationStart(E element) {
    delegate.onActivationStart(element);
  }

  @Override
  public void onActivatedSubtree(E element) {
    delegate.onActivatedSubtree(element);
  }

  @Override
  public void onDeactivated(E element) {
    delegate.onDeactivated(element);
  }

  @Override
  public void onAddedToParent(E element, E oldParent) {
    delegate.onAddedToParent(element, oldParent);
  }

  @Override
  public void onChildAdded(E element, N child) {
    delegate.onChildAdded(element, child);
  }

  @Override
  public void onDescendantsMutated(E element) {
    delegate.onDescendantsMutated(element);
  }

  @Override
  public void onEmptied(E element) {
    delegate.onEmptied(element);
  }

  @Override
  public void onAttributeModified(E element, String name, String oldValue, String newValue) {
    delegate.onAttributeModified(element, name, oldValue, newValue);
  }

  @Override
  public void onChildRemoved(E element, N child) {
    delegate.onChildRemoved(element, child);
  }

  @Override
  public void onRemovedFromParent(E element, E newParent) {
    delegate.onRemovedFromParent(element, newParent);
  }
}
