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


package org.waveprotocol.wave.client.paging;

/**
 * A node in a tree. The types are intended to be covariant and homogenous.
 *
 * Note: the only reason this is not done with generic covariance ({@code
 * TreeNode&lt;T extends TreeNode&lt;T&gt;&gt;} is because GWT does not support
 * generic JSOs yet.
 *
 */
public interface TreeNode {

  /** @return the first child of this block. */
  TreeNode getFirstChild();

  /** @return the last child of this block. */
  TreeNode getLastChild();

  /** @return the next sibling of this block. */
  TreeNode getNextSibling();

  /** @return the previous sibling of this block. */
  TreeNode getPreviousSibling();

  /** @return the parent of this block. */
  TreeNode getParent();
}
