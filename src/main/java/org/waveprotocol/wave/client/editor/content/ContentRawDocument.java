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

import org.waveprotocol.wave.client.editor.EditorStaticDeps;

import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.util.OffsetList;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of {@link RawDocument} for ContentNodes
 *
 * Allows modifying of just the content wrapper dom, or both the wrapper and
 * html dom. In the latter case, consitency between the two is assumed (However
 * if inconsistency is detected, it is silently dealt with internally)
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ContentRawDocument extends FullContentView implements
    RawDocument<ContentNode, ContentElement, ContentTextNode> {

  // NOTE(danilatos): Very soon we will only need the setupBehaviour method,
  // we can construct the nodes directly.
  interface Factory {
    ContentTextNode createText(String text);
    ContentElement createElement(String tagName, Map<String, String> attributes);

    void setupBehaviour(ContentElement element);
  }

  private final Factory factory;

  private final ContentElement rootElement;

  private boolean affectImpl = false;

  /**
   * @param rootElement The root element of this document
   */
  public ContentRawDocument(ContentElement rootElement, Factory factory) {
    this.rootElement = rootElement;
    this.factory = factory;
  }

  /**
   * Set the state to the default, which is to equivalently affect the HTML
   * implementation at the same time as the wrapper dom, when mutator methods
   * are called.
   */
  public void setAffectHtml() {
    Preconditions.checkState(!affectImpl, "Already affectImpl is true");
    affectImpl = true;
  }

  /**
   * @return The current affect html state
   */
  public boolean getAffectHtml() {
    return affectImpl;
  }

  /**
   * Set the state to not touch the HTML implementation when mutator methods are
   * called.
   */
  public void clearAffectHtml() {
    Preconditions.checkState(affectImpl, "Already not affectImpl");
    affectImpl = false;
  }

  /**
   * Same as {@link #createElement(String, Map, ContentElement, ContentNode)},
   * except no starting attributes
   */
  public ContentElement createElement(String tagName,
      ContentElement parent, ContentNode nodeAfter) {
    return createElement(tagName, Collections.<String,String>emptyMap(),
        parent, nodeAfter);
  }

  /** {@inheritDoc} */
  public ContentElement createElement(String tagName, Map<String, String> attributes,
      ContentElement parent, ContentNode nodeAfter) {

    ContentElement newElement = factory.createElement(tagName, attributes);
    EditorStaticDeps.startIgnoreMutations();
    try {
      insertBefore(parent, newElement, null, nodeAfter, factory);
      return newElement;
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public ContentTextNode createTextNode(String data,
      ContentElement parent, ContentNode nodeAfter) {

    ContentTextNode newTextNode = factory.createText(data);

    EditorStaticDeps.startIgnoreMutations();
    try {
      insertBefore(parent, newTextNode, nodeAfter);
      return newTextNode;
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }

  }

  /** {@inheritDoc} */
  public ContentNode insertBefore(ContentElement parent, ContentNode newChild,
      ContentNode refChild) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      return parent.insertBefore(newChild, refChild, affectImpl);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public ContentNode insertBefore(ContentElement parent, ContentNode from, ContentNode to,
      ContentNode refChild) {
    return insertBefore(parent, from, to, refChild, null);
  }

  /**
   * @param factory for activation. Only used during node construction, to make
   *        sure the nodes get activated. It has to be threaded through because
   *        we want onActivated to get called at exactly the right time, just
   *        after the nodes get attached to the DOM but BEFORE onAddedToParent
   *        gets called. For just moving existing nodes around the DOM, we just
   *        want to pass null for the factory.
   */
  private ContentNode insertBefore(ContentElement parent, ContentNode from, ContentNode to,
      ContentNode refChild, Factory factory) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      return parent.insertBefore(from, to, refChild, affectImpl, factory);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void insertData(ContentTextNode textNode, int offset, String arg) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      textNode.insertData(offset, arg, affectImpl);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void deleteData(ContentTextNode textNode, int offset, int count) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      textNode.deleteData(offset, count, affectImpl);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void removeAttribute(ContentElement element, String name) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      element.removeAttribute(name);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void removeChild(ContentElement parent, ContentNode oldChild) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      parent.removeChild(oldChild, affectImpl);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void setAttribute(ContentElement element, String name, String value) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      element.setAttribute(name, value);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public ContentTextNode splitText(ContentTextNode textNode, int offset) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      return textNode.splitText(offset, affectImpl);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public ContentTextNode mergeText(ContentTextNode secondSibling) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      // TODO(danilatos): Proper implementation
      return null;
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  public void appendData(ContentTextNode textNode, String arg) {
    EditorStaticDeps.startIgnoreMutations();
    try {
      insertData(textNode, getData(textNode).length(), arg);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /** {@inheritDoc} */
  @Override
  public ContentElement getDocumentElement() {
    return rootElement;
  }

  /** {@inheritDoc} */
  public OffsetList.Container<ContentNode> getIndexingContainer(ContentNode domNode) {
    return domNode.getIndexingContainer();
  }

  /** {@inheritDoc} */
  public void setIndexingContainer(ContentNode domNode,
      OffsetList.Container<ContentNode> indexingContainer) {
    domNode.setIndexingContainer(indexingContainer);
  }
}
