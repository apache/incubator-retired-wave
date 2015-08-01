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

import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * A single node + offset point in HTML
 *
 * TODO(danilatos): Convert to node-node style for element containers (a la Point and ContentPoint)
 *
 */
public class HtmlPoint {

  /**
   * The node enclosing the point
   */
  private Node node;

  /**
   * The offset into the node
   */
  private int offset;

  /**
   * @param node
   * @param offset
   */
  public HtmlPoint(Node node, int offset) {
    super();
    assert offset >= 0;
    this.node = node;
    this.offset = offset;
  }

  /**
   * @return node
   */
  public Node getNode() {
    return node;
  }

  /**
   * @param node
   */
  public void setNode(Node node) {
    this.node = node;
  }

  /**
   * @return offset
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @param offset
   */
  public void setOffset(int offset) {
    assert offset >= 0;
    this.offset = offset;
  }

  /**
   * Convert the HtmlPoint to Point.
   */
  public Point<Node> toPoint() {
    assert offset >= 0 && offset < node.getChildCount();
    return DomHelper.nodeOffsetToNodeletPoint(node, offset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return HTMLPretty.printSingleLine(node, new PointRange<Node>(toPoint()));
  }
}
