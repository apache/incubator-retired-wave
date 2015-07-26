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
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.JsoView;

import org.waveprotocol.wave.model.document.util.ElementStyleView;
import org.waveprotocol.wave.model.document.util.Point;

import java.util.Collections;
import java.util.Map;

/**
 * Presents a simple view of a HTML document where you can walk through every node.
 * It is rooted at a specific element (i.e. it can be a subtree of a full document).
 * However the results of attempting to traverse outside the document are undefined.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class HtmlViewImpl implements HtmlView, ElementStyleView<Node, Element, Text> {

  /**
   * Singleton instance, if you don't care about not having a document element defined.
   */
  public static final HtmlViewImpl INSTANCE = new HtmlViewImpl(null);

  Element documentElement;

  /**
   * @param documentElement Root element for this "document"
   */
  public HtmlViewImpl(Element documentElement) {
    this.documentElement = documentElement;
  }

  /** {@inheritDoc} */
  public Element asElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE ? node.<Element>cast() : null;
  }

  /** {@inheritDoc} */
  public Text asText(Node node) {
    return DomHelper.isTextNode(node) ? node.<Text>cast() : null;
  }

  /** {@inheritDoc} */
  public Map<String, String> getAttributes(Element element) {
    return Collections.emptyMap();
  }

  /** {@inheritDoc} */
  public String getData(Text textNode) {
    return textNode.getData();
  }

  /** {@inheritDoc} */
  public Element getDocumentElement() {
    return documentElement;
  }

  /** {@inheritDoc} */
  public Node getFirstChild(Node node) {
    return node.getFirstChild();
  }

  /** {@inheritDoc} */
  public Node getLastChild(Node node) {
    return node.getLastChild();
  }

  /** {@inheritDoc} */
  public int getLength(Text textNode) {
    return textNode.getLength();
  }

  /** {@inheritDoc} */
  public Node getNextSibling(Node node) {
    return node.getNextSibling();
  }

  /** {@inheritDoc} */
  public short getNodeType(Node node) {
    return node.getNodeType();
  }

  /** {@inheritDoc} */
  public Element getParentElement(Node node) {
    return node.getParentElement();
  }

  /** {@inheritDoc} */
  public Node getPreviousSibling(Node node) {
    return node.getPreviousSibling();
  }

  /** {@inheritDoc} */
  public String getTagName(Element element) {
    return element.getTagName();
  }

  /** {@inheritDoc} */
  public String getAttribute(Element element, String name) {
    return name.equals("class") ? element.getClassName() : element.getAttribute(name);
  }

  /** {@inheritDoc} */
  public String getStylePropertyValue(Element element, String name) {
    // HACK(user): can't use setProperty due to assertCamelCase:
    return JsoView.as(element.getStyle()).getString(name);
  }

  /** {@inheritDoc} */
  public boolean isSameNode(Node node, Node other) {
    // TODO(danilatos): Use .equals or isSameNode for nodelets in nodemanager,
    // typing extractor, etc.
    return node == other || (node != null && node.equals(other));
  }

  @Override
  public void onBeforeFilter(Point<Node> at) {
  }

  /** {@inheritDoc} */
  public Node getVisibleNode(Node node) {
    return node;
  }

  /** {@inheritDoc} */
  public Node getVisibleNodeFirst(Node node) {
    return node;
  }

  /** {@inheritDoc} */
  public Node getVisibleNodeLast(Node node) {
    return node;
  }

  /** {@inheritDoc} */
  public Node getVisibleNodeNext(Node node) {
    return node;
  }

  /** {@inheritDoc} */
  public Node getVisibleNodePrevious(Node node) {
    return node;
  }
}
