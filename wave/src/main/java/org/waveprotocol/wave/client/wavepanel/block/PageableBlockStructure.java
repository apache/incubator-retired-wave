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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.MeasurerInstance;
import org.waveprotocol.wave.client.paging.Block;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.Node;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Exposes a view structure as pageable blocks.
 *
 */
public final class PageableBlockStructure {

  /**
   * Wraps a structural node in order to provide behaviour.
   * This is temporary until the pager is flyweighted.
   */
  private class NodeImpl implements Block {
    private final Node source;
    private Element dom;

    public NodeImpl(Node source) {
      this.source = source;
    }

    @Override
    public NodeImpl getParent() {
      return adapt(source.getParent());
    }

    @Override
    public NodeImpl getFirstChild() {
      return adapt(source.getFirstChild());
    }

    @Override
    public NodeImpl getLastChild() {
      return adapt(source.getLastChild());
    }

    @Override
    public NodeImpl getNextSibling() {
      return adapt(source.getNextSibling());
    }

    @Override
    public NodeImpl getPreviousSibling() {
      return adapt(source.getPreviousSibling());
    }

    //
    // Paging nature.
    //

    private Element getElement() {
      if (dom == null) {
        String domId = source.getId();
        dom = Document.get().getElementById(domId);
        if (dom == null) {
          throw new RuntimeException("Block is missing: " + domId);
        }
      }
      return dom;
    }

    @Override
    public double getStart() {
      return MeasurerInstance.get().top(getRoot().getElement(), getElement());
    }

    @Override
    public double getEnd() {
      return MeasurerInstance.get().bottom(getRoot().getElement(), getElement());
    }

    @Override
    public void pageIn() {
      handler.pageIn(source);
    }

    @Override
    public void pageOut() {
      handler.pageOut(source);
    }

    @Override
    public double getChildrenOrigin() {
      return 0;
    }
  }

  public interface PageHandler {
    void pageIn(Node block);
    void pageOut(Node block);
  }

  private final StringMap<NodeImpl> nodes = CollectionUtils.createStringMap();
  private final BlockStructure view;
  private final PageHandler handler;

  private PageableBlockStructure(PageHandler handler, BlockStructure view) {
    this.handler = handler;
    this.view = view;
  }

  public static PageableBlockStructure create(PageHandler handler, BlockStructure view) {
    return new PageableBlockStructure(handler, view);
  }

  private NodeImpl adapt(Node source) {
    if (source == null) {
      return null;
    }
    String id = source.getId();
    NodeImpl block = nodes.get(id);
    if (block == null) {
      block = new NodeImpl(source);
      nodes.put(id, block);
    }
    return block;
  }

  public NodeImpl getRoot() {
    return adapt(view.getRoot());
  }

  public NodeImpl getNode(String id) {
    return adapt(view.getNode(id));
  }
}
