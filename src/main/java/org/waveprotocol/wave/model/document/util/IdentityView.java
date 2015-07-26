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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Convenience class, designed for subclassing to reduce boilerplate from
 * delegation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class IdentityView<N, E extends N, T extends N>
    implements ReadableDocumentView<N, E, T> {

  protected final ReadableDocument<N, E, T> inner;

  protected IdentityView(ReadableDocument<N, E, T> inner) {
    Preconditions.checkNotNull(inner, "IdentityView (or subclass): " +
        "Inner document may not be null!");
    this.inner = inner;
  }

  @Override
  public E asElement(N node) {
    return inner.asElement(node);
  }

  @Override
  public T asText(N node) {
    return inner.asText(node);
  }

  @Override
  public String getAttribute(E element, String name) {
    return inner.getAttribute(element, name);
  }

  @Override
  public Map<String, String> getAttributes(E element) {
    return inner.getAttributes(element);
  }

  @Override
  public String getData(T textNode) {
    return inner.getData(textNode);
  }

  @Override
  public E getDocumentElement() {
    return inner.getDocumentElement();
  }

  @Override
  public int getLength(T textNode) {
    return inner.getLength(textNode);
  }

  @Override
  public short getNodeType(N node) {
    return inner.getNodeType(node);
  }

  @Override
  public String getTagName(E element) {
    return inner.getTagName(element);
  }

  @Override
  public boolean isSameNode(N node, N other) {
    return inner.isSameNode(node, other);
  }

  @Override
  public N getFirstChild(N node) {
    return inner.getFirstChild(node);
  }

  @Override
  public N getLastChild(N node) {
    return inner.getLastChild(node);
  }

  @Override
  public N getNextSibling(N node) {
    return inner.getNextSibling(node);
  }

  @Override
  public E getParentElement(N node) {
    return inner.getParentElement(node);
  }

  @Override
  public N getPreviousSibling(N node) {
    return inner.getPreviousSibling(node);
  }

  @Override
  public N getVisibleNode(N node) {
    return node;
  }

  @Override
  public N getVisibleNodeFirst(N node) {
    return node;
  }

  @Override
  public N getVisibleNodeLast(N node) {
    return node;
  }

  @Override
  public N getVisibleNodeNext(N node) {
    return node;
  }

  @Override
  public N getVisibleNodePrevious(N node) {
    return node;
  }

  @Override
  public void onBeforeFilter(Point<N> at) {
    // do nothing.
  }
}
