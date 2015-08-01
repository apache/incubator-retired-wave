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

package org.waveprotocol.wave.client.editor.impl;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;

/**
 * A view that removes elements that do not correspond to
 * a wrapper ContentNode
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class StrippingHtmlView extends HtmlViewImpl {

  /**
   * @param documentElement The root element
   */
  public StrippingHtmlView(Element documentElement) {
    super(documentElement);
  }

  /**
   * Remove the given node (leaving its children in the dom) if
   * it does not correspond to a wrapper ContentNode
   * @param node
   * @return true if the node was removed, false if not.
   */
  private boolean maybeStrip(Node node) {
    if (node == null || DomHelper.isTextNode(node)) return false;

    Element element = node.cast();
    if (!NodeManager.hasBackReference(element)) {
      Node n;
      while ((n = element.getFirstChild()) != null) {
        element.getParentNode().insertBefore(n, element);
      }
      element.removeFromParent();
      return true;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public Node getFirstChild(Node node) {
    Node n;
    while (maybeStrip(n = node.getFirstChild())) { }
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public Node getLastChild(Node node) {
    Node n;
    while (maybeStrip(n = node.getLastChild())) { }
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public Node getNextSibling(Node node) {
    Node n;
    while (maybeStrip(n = node.getNextSibling())) { }
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public Node getPreviousSibling(Node node) {
    Node n;
    while (maybeStrip(n = node.getPreviousSibling())) { }
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public Node getVisibleNode(Node node) {
    Node next;
    while (true) {
      next = node.getParentElement();
      if (maybeStrip(node) == false) {
        return node;
      }
      node = next;
    }
  }

}
