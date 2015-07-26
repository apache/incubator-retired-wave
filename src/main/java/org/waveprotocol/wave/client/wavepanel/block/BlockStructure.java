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


package org.waveprotocol.wave.client.wavepanel.block;

import org.waveprotocol.wave.client.paging.TreeNode;
import org.waveprotocol.wave.client.render.ConversationRenderer;

/**
 * Exposes, as an object tree, the parts of the structure implicitly exposed by
 * a {@link ConversationRenderer} that are relevant for paging.
 *
 */
public interface BlockStructure {
  /** The kind of a node in the view. */
  public enum NodeType {
    BLIP, THREAD, CONVERSATION, PARTICIPANTS, ROOT, META
  }

  /**
   * A node in the view.
   */
  public interface Node extends TreeNode {
    /** @return a identifier of this node, unqiue within this tree. */
    String getId();

    /** @return the kind of view object represented by this node. */
    NodeType getType();

    //
    // Covariant overrides.
    //

    @Override
    Node getFirstChild();

    @Override
    Node getLastChild();

    @Override
    Node getNextSibling();

    @Override
    Node getParent();

    @Override
    Node getPreviousSibling();
  }

  /** @return the root node of this view structure. */
  Node getRoot();

  /** @return the node identified by {@code id}, or {@code null}. */
  Node getNode(String id);
}
