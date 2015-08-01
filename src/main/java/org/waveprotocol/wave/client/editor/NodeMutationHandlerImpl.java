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
 * Empty NodeMutationHandler implementation.
 *
 * @param <N> Node
 * @param <E> Element
 *
 */
public class NodeMutationHandlerImpl<N, E extends N> implements NodeMutationHandler<N, E> {

  @Override
  public void onActivationStart(E element) {
  }

  @Override
  public void onActivatedSubtree(E element) {
  }

  @Override
  public void onDeactivated(E element) {
  }

  @Override
  public void onAddedToParent(E element, E oldParent) {
  }

  @Override
  public void onChildAdded(E element, N child) {
  }

  @Override
  public void onDescendantsMutated(E element) {
  }

  @Override
  public void onEmptied(E element) {
  }

  @Override
  public void onAttributeModified(E element, String name, String oldValue, String newValue) {
  }

  @Override
  public void onChildRemoved(E element, N child) {
  }

  @Override
  public void onRemovedFromParent(E element, E newParent) {
  }
}
