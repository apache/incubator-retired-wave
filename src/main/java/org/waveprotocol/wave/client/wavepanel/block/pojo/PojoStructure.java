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


package org.waveprotocol.wave.client.wavepanel.block.pojo;

import org.waveprotocol.wave.client.paging.AbstractTreeNode;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Exposes a view structure backed by basic objects.
 *
 */
public final class PojoStructure implements BlockStructure {

  /**
   * Obvious implementation of a node.
   */
  public final class NodeImpl extends AbstractTreeNode<NodeImpl> implements Node {
    private final String id;
    private final NodeType type;

    private NodeImpl(String id, NodeType type) {
      this.id = id;
      this.type = type;
    }

    @Override
    protected NodeImpl self() {
      return this;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public NodeType getType() {
      return type;
    }

    public NodeImpl createChild(String id, NodeType type) {
      return append(createNode(id, type));
    }

    StringBuffer buildString(StringBuffer b) {
      b.append(type.name());
      b.append("(");
      b.append(id);
      b.append(")");
      NodeImpl c = getFirstChild();
      if (c != null) {
        b.append(" { ");
        c.buildString(b);
        c = c.getNextSibling();
        while (c != null) {
          b.append(", ");
          b.append(c);
          c = c.getNextSibling();
        }
        b.append(" } ");
      }
      return b;
    }

    @Override
    public String toString() {
      return buildString(new StringBuffer()).toString();
    }
  }

  private final StringMap<NodeImpl> nodes = CollectionUtils.createStringMap();
  private final NodeImpl root = createNode("root", NodeType.ROOT);

  private PojoStructure() {
  }

  public static PojoStructure create() {
    return new PojoStructure();
  }

  private NodeImpl createNode(String id, NodeType type) {
    NodeImpl node = new NodeImpl(id, type);
    nodes.put(id, node);
    return node;
  }

  @Override
  public NodeImpl getRoot() {
    return root;
  }

  @Override
  public NodeImpl getNode(String id) {
    return nodes.get(id);
  }

  @Override
  public String toString() {
    return root != null? root.toString() : "";
  }
}
