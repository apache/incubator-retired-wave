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

package org.waveprotocol.wave.model.document.raw.impl;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.indexed.NodeType;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.util.ElementManager;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mimics a DOM Element node.
 *
 */
public final class Element extends Node implements Doc.E {

  private final String tagName;
  private Map<String,String> backingAttributeMap;
  private Map<String,String> publicAttributeMap;

  private Map<Integer, Object> properties;


  /**
   * Element's manager for non-persistent properties
   */
  public static final ElementManager<Element> ELEMENT_MANAGER =
      new ElementManager<Element>() {
    public <T> void setProperty(Property<T> property, Element element, T value) {
      element.setProperty(property, value);
    }

    public <T> T getProperty(Property<T> property, Element element) {
      return element.getProperty(property);
    }

    public boolean isDestroyed(Element element) {
      return !element.isContentAttached();
    }
  };

  /**
   * Constructs an Element node with the given tag name.
   *
   * @param tagName The tag name the new Element node should have.
   */
  public Element(String tagName) {
    this.tagName = tagName;
  }

  public <T> void setProperty(Property<T> property, T value) {
    getTransientData().put(property.getId(), value);
  }

  @SuppressWarnings("unchecked")
  public <T> T getProperty(Property<T> property) {
    return (T) getTransientData().get(property.getId());
  }

  private Map<Integer, Object> getTransientData() {
    if (properties == null) {
      properties = new HashMap<Integer, Object>();
    }
    return properties;
  }

  public boolean isContentAttached() {
    return getParentElement() == null ? false : getParentElement().isContentAttached();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getNodeType() {
    return NodeType.ELEMENT_NODE;
  }

  /**
   * Gets the attributes of this node.
   *
   * @return A map that maps attribute names to attribute values for this node's
   *         attributes.
   */
  public Map<String,String> getAttributes() {
    if (publicAttributeMap == null) {
      constructMaps();
    }
    return publicAttributeMap;
  }

  /**
   * Inserts the given node as a child of this node, at the point before the
   * given reference node. newChild must not be in the path between this element
   * and the root.
   *
   * @param newChild The node to insert.
   * @param refChild The node before which to perform the insertion.
   * @return The inserted node.
   */
  public Node insertBefore(Node newChild, Node refChild) {
    // NOTE(user): please make sure these assertion is not converted back to precondition check
    // they create significant slow down.
    assert refChild == null || refChild.getParentElement() == this :
        "insertBefore: refChild is not child of parent";
    assert !newChild.isOrIsAncestorOf(this) :
        "insertBefore: newChild is or is an ancestor of parent!";
    if (newChild.parent != null) {
      newChild.parent.removeChild(newChild);
    }
    Node childBefore;
    if (refChild != null) {
      childBefore = refChild.previousSibling;
      refChild.previousSibling = newChild;
    } else {
      childBefore = lastChild;
      lastChild = newChild;
    }
    if (childBefore != null) {
      childBefore.nextSibling = newChild;
    } else {
      firstChild = newChild;
    }
    newChild.parent = this;
    newChild.previousSibling = childBefore;
    newChild.nextSibling = refChild;
    return newChild;
  }

  /**
   * Removes a child from this node.
   *
   * @param oldChild The child to remove.
   * @return The removed node.
   */
  public Node removeChild(Node oldChild) {
    if (oldChild.getParentElement() != this) {
      throw new IllegalArgumentException("removeChild: oldChild is not child of parent");
    }
    Node siblingBefore = oldChild.previousSibling;
    Node siblingAfter = oldChild.nextSibling;
    if (siblingBefore != null) {
      siblingBefore.nextSibling = siblingAfter;
    } else {
      firstChild = siblingAfter;
    }
    if (siblingAfter != null) {
      siblingAfter.previousSibling = siblingBefore;
    } else {
      lastChild = siblingBefore;
    }
    oldChild.previousSibling = null;
    oldChild.nextSibling = null;
    oldChild.parent = null;
    return oldChild;
  }

  /**
   * Gets this node's tag name.
   *
   * @return The tag name of this node.
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * Gets an attribute of this node.
   *
   * @param name The name of the attribute to get.
   * @return The value of the attribute, or null if the attribute does not
   *         exist. Note that this does not mimic the DOM specification exactly.
   */
  public String getAttribute(String name) {
    return backingAttributeMap == null ? null : getBackingAttributeMap().get(name);
  }

  /**
   * Sets an attribute of this node.
   *
   * @param name The name of the attribute to set.
   * @param value The value that the attribute should have.
   */
  public void setAttribute(String name, String value) {
    getBackingAttributeMap().put(name, value);
  }

  /**
   * Removes an attribute from this node.
   *
   * @param name The name of the attribute to remove.
   */
  public void removeAttribute(String name) {
    getBackingAttributeMap().remove(name);
  }

  /** {@inheritDoc} */
  @Override
  public int calculateSize() {
    int size = 2;
    for (Node n = getFirstChild(); n != null; n = n.getNextSibling()) {
      size += n.calculateSize();
    }
    return size;
  }

  @Override
  public Element asElement() {
    return this;
  }

  @Override
  public Text asText() {
    return null;
  }

  /**
   * For debugging purposes only, very inefficient implementation!
   */
  @Override
  public String toString() {
    return XmlStringBuilder.createNode(
        RawDocumentImpl.BUILDER.create("x", Attributes.EMPTY_MAP), this).toString();
  }

  private Map<String,String> getBackingAttributeMap() {
    if (backingAttributeMap == null) {
      constructMaps();
    }
    return backingAttributeMap;
  }

  private void constructMaps() {
    backingAttributeMap = new TreeMap<String,String>();
    publicAttributeMap = Collections.unmodifiableMap(backingAttributeMap);
  }
}
