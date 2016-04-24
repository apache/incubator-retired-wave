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

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;

/**
 * A full, read-only content view, with singleton
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FullContentView implements ContentView {
  /** Singleton, lacking a document element */
  public static final ContentView INSTANCE = new FullContentView();

  @Override
  public short getNodeType(ContentNode node) {
    return node.getNodeType();
  }

  @Override
  public String getData(ContentTextNode textNode) {
    return textNode.getData();
  }

  @Override
  public ContentElement getDocumentElement() {
    throw new RuntimeException("This is not implemented because the view is meant "
        + "as a generic singleton");
  }

  @Override
  public ContentNode getFirstChild(ContentNode node) {
    return node.getFirstChild();
  }

  @Override
  public ContentNode getLastChild(ContentNode node) {
    return node.getLastChild();
  }

  @Override
  public int getLength(ContentTextNode textNode) {
    return textNode.getLength();
  }

  @Override
  public ContentNode getNextSibling(ContentNode node) {
    return node.getNextSibling();
  }

  @Override
  public ContentElement getParentElement(ContentNode node) {
    return node.getParentElement();
  }

  @Override
  public ContentNode getPreviousSibling(ContentNode node) {
    return node.getPreviousSibling();
  }

  @Override
  public ContentElement asElement(ContentNode node) {
    return node != null ? node.asElement() : null;
  }

  @Override
  public ContentTextNode asText(ContentNode node) {
    return node != null ? node.asText() : null;
  }

  @Override
  public boolean isSameNode(ContentNode node, ContentNode other) {
    return node == other;
  }

  @Override
  public Map<String, String> getAttributes(ContentElement element) {
    return CollectionUtils.newJavaMap(element.getAttributes());
  }

  @Override
  public String getAttribute(ContentElement element, String name) {
    return element.getAttribute(name);
  }

  @Override
  public String getTagName(ContentElement element) {
    return element.getTagName();
  }

  @Override
  public void onBeforeFilter(Point<ContentNode> at) {
    // NO-OP
  }

  @Override
  public ContentNode getVisibleNode(ContentNode node) {
    return node;
  }

  @Override
  public ContentNode getVisibleNodeFirst(ContentNode node) {
    return node;
  }

  @Override
  public ContentNode getVisibleNodeLast(ContentNode node) {
    return node;
  }

  @Override
  public ContentNode getVisibleNodeNext(ContentNode node) {
    return node;
  }

  @Override
  public ContentNode getVisibleNodePrevious(ContentNode node) {
    return node;
  }
}
