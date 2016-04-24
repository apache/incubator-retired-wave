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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry.HasHandlers;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.indexed.NodeType;
import org.waveprotocol.wave.model.document.util.ElementManager;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Content Element.
 *
 * See {@link ContentDocument} for more...
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author lars@google.com (Lars Rasmussen)
 */
public class ContentElement extends ContentNode implements Doc.E, HasHandlers, HasImplNodelets {

  /**
   * We mark nodelets at the top of a complex implementation tree with
   * this, so we can optimise traversal in the filtered view.
   */
  public static final String COMPLEX_IMPLEMENTATION_MARKER =
      NodeManager.getNextMarkerName("cim");

  /**
   * ContentElement's manager for non-persistent properties
   */
  public static final ElementManager<ContentElement> ELEMENT_MANAGER =
      new ElementManager<ContentElement>() {
    public <T> void setProperty(Property<T> property, ContentElement element, T value) {
      element.setProperty(property, value);
    }

    public <T> T getProperty(Property<T> property, ContentElement element) {
      return element.getProperty(property);
    }

    public boolean isDestroyed(ContentElement element) {
      return !element.isContentAttached();
    }
  };

  private final String tagName;
  private ContentNode firstChild = null;
  private ContentNode lastChild = null;
  private final StringMap<String> attributes = CollectionUtils.createStringMap();
  private final IntMap<Object> transientData = CollectionUtils.createIntMap();

  private Element containerNodelet = null;

  /**
   * Some common attribute names. By default, 'name' serves to identify in particular
   * form elements, and 'submit' to identify (by name) an element that should receive
   * a click event if the element containing the submit attribute is the target of
   * a submit event. See {@code Button} and {@code Input} for examples.
   */
  public static final String NAME = "name";
  public static final String SUBMIT = "submit";

  public ContentElement(String tagName, Element nodelet, ExtendedClientDocumentContext bundle) {
    this(tagName, bundle, true);
    setImplNodelets(nodelet, nodelet);
    init(Collections.<String, String>emptyMap());
  }

  /**
   * Constructor that does not initialize the impl nodelet.
   * @param bundle
   * @param initLater
   */
  public ContentElement(String tagName,
      ExtendedClientDocumentContext bundle, boolean initLater) {
    super(null, bundle);
    assert initLater == true;
    this.tagName = tagName;
  }

  @Override
  public void setImplNodelets(Element domImplNodelet, Element containerNodelet) {
    //TODO(danilatos): redundant setImplNodelet?
    setImplNodeletInner(domImplNodelet);
    setContainerNodelet(containerNodelet);

    if (domImplNodelet != null) {
      walkImpl(domImplNodelet);

      // In a non-empty region between two text-editable regions (i.e., the region between an
      // element's implNodelet and its declared container nodelet for child implNodelets), we turn
      // white-space back to normal.
      if (containerNodelet != domImplNodelet) {
        // TODO(danilatos): allow renderers to have non-normal whitespace through CSS
        // HACK(user): can't use setProperty due to assertCamelCase:
        JsoView.as(domImplNodelet.getStyle()).setString("white-space", "normal");
        if (containerNodelet != null) {
          // HACK(user): can't use setProperty due to assertCamelCase:
          JsoView.as(domImplNodelet.getStyle()).setString("white-space", "prewrap");
          containerNodelet.setInnerHTML("");
        }
      }
    }

    assert getImplNodelet() == null || NodeManager.getBackReference(getImplNodelet()) == this;
  }

  @Override
  public void setBothNodelets(Element implAndContainerNodelet) {
    setImplNodelets(implAndContainerNodelet, implAndContainerNodelet);
  }

  void init(Map<String, String> attributes) {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      // Set the attributes directly without triggering events during init()n
      this.attributes.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Mark implementation elements that aren't transparent as part of a
   * a complex implementation structure.
   *
   * @param element
   */
  public static void walkImpl(Element element) {
    for (Node n = element.getFirstChild(); n != null;) {
      if (DomHelper.isTextNode(n)) {
        n = n.getNextSibling();
      } else {
        Element e = n.cast();
        if (!NodeManager.isTransparent(e)) {
          e.setPropertyBoolean(COMPLEX_IMPLEMENTATION_MARKER, true);
        }
        walkImpl(e);
        n = n.getNextSibling();
      }
    }
  }

  /**
   * Get the handler of the given type for this node
   *
   * @param <T>
   * @param handlerType
   * @return The handler, or null if none exists for this node
   */
  public <T> T getHandler(Class<T> handlerType) {
    throw new UnsupportedOperationException("getHandler only implemented for AgentAdapter for now");
  }

  /**
   * Gets a transient property on the element.
   * @param <T>
   * @param property
   */
  @SuppressWarnings("unchecked")
  public final <T> T getProperty(Property<T> property) {
    return (T) transientData.get(property.getId());
  }

  /**
   * Sets a transient property on the element.
   * @param <T>
   * @param property
   * @param value
   */
  public final <T> void setProperty(Property<T> property, T value) {
    transientData.put(property.getId(), value);
  }

  /** {@inheritDoc} */
  @Override
  public Element getImplNodelet() {
    return (Element) super.getImplNodelet();
  }

  /**
   * Also affects the container nodelet. If the current container nodelet is the
   * same as the current impl nodelet, the new container will be the same as the new
   * impl nodelet. If it is null, it will stay null. Other scenarios are not supported
   *
   * @deprecated Use {@link #setImplNodelets(Element, Element)} instead of this method.
   */
  @Override
  @Deprecated // Use #setImplNodelets(impl, container) instead
  public void setImplNodelet(Node nodelet) {
    Preconditions.checkNotNull(nodelet,
        "Null nodelet not supported with this deprecated method, use setImplNodelets instead");
    Preconditions.checkState(containerNodelet == null || containerNodelet == getImplNodelet(),
        "Cannot set only the impl nodelet if the container nodelet is different");
    Preconditions.checkArgument(!DomHelper.isTextNode(nodelet),
        "element cannot have text implnodelet");

    Element element = nodelet.cast();

    if (this.containerNodelet != null) {
      setContainerNodelet(element);
    }
    setImplNodeletInner(element);
  }

  private void setImplNodeletInner(Element newNodelet) {
    swapNodelet(getImplNodelet(), newNodelet);
    super.setImplNodelet(newNodelet);
  }

  @Override
  public Element setAutoAppendContainer(Element containerNodelet) {
    setContainerNodelet(containerNodelet);
    return containerNodelet;
  }

  void setContainerNodelet(Element newNodelet) {
    if (newNodelet == containerNodelet) {
      return;
    }

    // We want the new container nodelet to get everything that was in the old
    // container nodelet, and nothing more
    if (newNodelet != null) {
      // Remove any existing junk
      DomHelper.emptyElement(newNodelet);

      // Copy children from old container
      for (ContentNode node = getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.isTextNode()) {
          node.normaliseImpl();
        }
        Node implNodelet = node.getImplNodelet();
        if (implNodelet != null) {
          newNodelet.appendChild(implNodelet);
        }
      }
    }

    // If the container nodelet is the same as the impl nodelet, don't specify it as an
    // old nodelet, because that would clear the backreference on a node still in use.
    swapNodelet(containerNodelet == getImplNodelet() ? null : containerNodelet, newNodelet);
    this.containerNodelet = newNodelet;
  }

  void swapNodelet(Element oldNodelet, Element newNodelet) {
    if (oldNodelet != null) {
      NodeManager.setBackReference(oldNodelet, null);
    }
    if (newNodelet != null) {
      NodeManager.setBackReference(newNodelet, this);
    }
  }

  @Override
  void breakBackRef(boolean recurse) {
    swapNodelet(getImplNodelet(), null);
    swapNodelet(getContainerNodelet(), null);

    if (recurse) {
      for (ContentNode n = getFirstChild(); n != null; n = n.getNextSibling()) {
        n.breakBackRef(true);
      }
    }
  }

  @Override
  public Element getContainerNodelet() {
    return containerNodelet;
  }

  /** Return the element's tag name */
  @Override
  public final String getTagName() {
    return tagName;
  }

  @Override
  public ContentElement asElement() {
    return this;
  }

  @Override
  public ContentTextNode asText() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final ContentNode getFirstChild() {
    return firstChild;
  }

  /** {@inheritDoc} */
  @Override
  public final ContentNode getLastChild() {
    return lastChild;
  }

  /**
   * TODO(danilatos): This is a mutability leak, make it return a readonly StringMap
   *
   * @return The attributes of this element in their optimised map form
   */
  public final StringMap<String> getAttributes() {
    return attributes;
  }

  void setFirstChild(ContentNode child) {
    firstChild = child;
  }
  void setLastChild(ContentNode child) {
    lastChild = child;
  }

  /**
   * Get the value of the given attribute name.
   * @return the value, or null if not present
   */
  public final String getAttribute(String name) {
    return attributes.get(name);
  }

  /**
   * @param name
   * @return true if the element has the given attribute
   */
  public final boolean hasAttribute(String name) {
    return attributes.containsKey(name);
  }

  ///// CONTENT

  /**
   * Set an attribute. Does not affect the html implementation.
   * @param name
   * @param value
   */
  void setAttribute(String name, String value) {
    assert value != null : "Do not set an attribute to null, use removeAttribute instead";
    String old = attributes.get(name);
    attributes.put(name, value);
    notifyAttributeModified(name, old, value);
  }

  /**
   * Remove the given attribute if present. Does not affect the html
   * @param name
   */
  void removeAttribute(String name) {
    String old = attributes.get(name);
    attributes.remove(name);
    notifyAttributeModified(name, old, null);
  }

  /**
   * Same semantics as the corresponding DOM method
   * @param newChild
   * @param refChild
   * @param affectImpl Don't touch the html if this is false
   * @return The new child for convenience
   */
  ContentNode insertBefore(ContentNode newChild, ContentNode refChild, boolean affectImpl) {
    return insertBefore(newChild, newChild.getNextSibling(), refChild, affectImpl, null);
  }

  ContentNode insertBefore(ContentNode fromIncl, ContentNode toExcl,
      ContentNode refChild, boolean affectImpl, ContentRawDocument.Factory factory) {
    if (fromIncl == toExcl) {
      // Early exit if nothing to do
      return fromIncl;
    }

    if (refChild != null && refChild.getParentElement() != this) {
      throw new IllegalArgumentException("insertBefore: refChild is not child of parent");
    }
    if (fromIncl.isOrIsAncestorOf(this)) {
      throw new IllegalArgumentException("insertBefore: fromIncl is or is an ancestor of parent!");
    }
    if (toExcl != null && toExcl.getParentElement() != fromIncl.getParentElement()) {
      throw new IllegalArgumentException("insertBefore: toExcl does not have the same " +
          "parent as fromIncl!");
    }


    //TODO(danilatos): Ensure this works when from == toExcl

    //TODO(danilatos): Test cases for appending a MetaElement with a null implNodelet

    assert (refChild == null || refChild.getParentElement() == this);

    ContentElement oldParent = fromIncl.getParentElement();
    Element oldContainerNodelet = oldParent != null ? oldParent.getContainerNodelet() : null;

    ContentNode newChild = fromIncl;
    ContentNode prev;
    if (refChild == null) {
      prev = getLastChild();
      setLastChild(newChild);
    } else {
      prev = refChild.getPreviousSibling();
      refChild.setPrev(newChild);
    }

    List<ContentNode> movedNodes = new ArrayList<ContentNode>();

    while (true) {
      movedNodes.add(newChild);

      ContentNode next = newChild.getNextSibling();

      newChild.removeFromShadowTree();

      if (prev == null) {
        setFirstChild(newChild);
      } else {
        prev.setNext(newChild);
      }

      newChild.setNext(refChild);
      newChild.setPrev(prev);
      newChild.setParent(this);

      prev = newChild;

      if (next == toExcl) {
        if (refChild == null) {
          setLastChild(newChild);
        } else {
          refChild.setPrev(newChild);
        }
        break;
      }

      newChild = next;
    }

    // activation of new nodes before notifications & html impl business
    if (factory != null) {
      assert toExcl == null && fromIncl.isElement() && movedNodes.get(0) == fromIncl;
      factory.setupBehaviour(fromIncl.asElement());
    }

    // html updates
    if (affectImpl) {
      implInsertBefore(this, fromIncl, prev.getNextSibling(), refChild, oldContainerNodelet);
    }

    // notifications
    if (oldParent != null) {
      oldParent.notifyChildrenMutated();
      for (ContentNode node : movedNodes) {
        node.notifyRemovedFromParent(oldParent, this);
      }
    }
    for (ContentNode node : movedNodes) {
      node.notifyAddedToParent(oldParent, false);
    }

    notifyChildrenMutated();

    return fromIncl;
  }

  void reInsertImpl() {
    implInsertBefore(this, getFirstChild(), null, null, getContainerNodelet());
  }

  /**
   * Same semantics as the corresponding DOM method
   * @param oldChild Child node to remove
   * @param affectImpl Don't touch the html if this is false
   */
  void removeChild(ContentNode oldChild, boolean affectImpl) {
    removeChildren(oldChild, oldChild.getNextSibling(), affectImpl);
  }

  /**
   * Remove a contiguous range of adjacent siblings, rather than just one
   *
   * Is to removeChild as the ranged version of insertBefore is to the regular version
   *
   * @param fromIncl
   * @param toExcl
   * @param affectImpl
   */
  void removeChildren(ContentNode fromIncl, ContentNode toExcl, boolean affectImpl) {

    if (fromIncl.getParentElement() != this) {
      throw new IllegalArgumentException("removeChild: fromIncl is not child of parent");
    }
    if (toExcl != null && toExcl.getParentElement() != this) {
      throw new IllegalArgumentException("removeChild: toExcl is not child of parent");
    }

    removeChildrenInner(fromIncl, toExcl, affectImpl);

    // Now propagate the post-event info - only do this for the outermost removed node.
    notifyChildrenMutated();
    if (getFirstChild() == null) {
      notifyEmptied();
    }
  }

  void removeChildrenInner(ContentNode fromIncl, ContentNode toExcl, boolean affectImpl) {
    List<ContentNode> removedNodes = new ArrayList<ContentNode>();
    for (ContentNode node = fromIncl; node != toExcl; ) {
      // Recurse
      ContentNode nodeFirstChild = node.getFirstChild();
      if (nodeFirstChild != null) {
        // NOTE(danilatos): possible optimisation is to unconditionally pass false
        // for affectImpl - need to consider all scenarios to ensure no strange behaviour.
        node.asElement().removeChildrenInner(nodeFirstChild, null, affectImpl);
      }

      ContentNode oldChild = node;
      node = node.getNextSibling();

      if (affectImpl) {
        Node nodelet = oldChild.normaliseImpl();
        if (nodelet != null) {
          // removeFromParent() checks if parent is null
          nodelet.removeFromParent();
        }
      }

      removedNodes.add(oldChild);
      oldChild.removeFromShadowTree();
      oldChild.clearNodeLinks();
      oldChild.breakBackRef(true);
    }

    for (ContentNode node : removedNodes) {
      node.notifyRemovedFromParent(this, null);
    }
  }

  ///////

  /** {@inheritDoc} */
  @Override
  public final short getNodeType() {
    return NodeType.ELEMENT_NODE;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isElement() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTextNode() {
    return false;
  }

  /**
   * To "zip" is to take two filtered-equivalent trees, one content, one html,
   * and setup the back references between each. This often happens when the
   * html changes for some reason, we change the content independently to match,
   * then we go through and "zip" the two subtrees together again. The assumption
   * is, of course, that they match in their filtered views.
   *
   * @param from first node that might need zipping
   * @param to last node that might need zipping.
   * @param notifyIfSplit we will return true if we split this node
   * @return true if notifyIfSplit is affected
   */
  public boolean zipChildren(ContentNode from, ContentNode to, Node notifyIfSplit) {
    ContentView renderedContent = getRenderedContentView();

    if (from != null) {
      from = renderedContent.getPreviousSibling(from);
    }

    return zipChildrenExcludingFrom(from, to, notifyIfSplit);
  }

  /**
   * Same as {@link #zipChildren(ContentNode, ContentNode, Node)}, except that the
   * "from" parameter is exclusive.
   *
   * @param from
   * @param to
   * @param notifyIfSplit we will return true if we split this node
   * @return true if notifyIfSplit is affected
   */
  public boolean zipChildrenExcludingFrom(ContentNode from, ContentNode to, Node notifyIfSplit) {
    EditorStaticDeps.startIgnoreMutations();
    try {

      boolean ret = false;
      ContentView renderedContent = getRenderedContentView();
      HtmlView filteredHtml = getFilteredHtmlView();

      ContentNode node = from;
      Node nodelet;
      if (node == null) {
        node = renderedContent.getFirstChild(this);
        nodelet = filteredHtml.getFirstChild(getImplNodelet());
      } else {
        nodelet = node.getImplNodelet();
      }

      while (node != null) {

        if (node.getImplNodelet() != nodelet) {
          node.setImplNodelet(nodelet);
        }

        if (DomHelper.isTextNode(nodelet)) {
          String target = ((ContentTextNode) node).getData();
          String txt = nodelet.<Text>cast().getData();
          String nodeletData = txt;
          int left = target.length() - txt.length();
          while (left > 0) {
            nodelet = filteredHtml.getNextSibling(nodelet);
            assert DomHelper.isTextNode(nodelet) : "Some random element!";
            nodeletData = nodelet.<Text>cast().getData();
            // TODO(danilatos): Is a StringBuilder more efficient here? On average, how many
            // string concatenations are expected?
            txt += nodeletData;
            left -= nodeletData.length();
          }
          assert target.equals(ContentTextNode.getNodeValueFromHtmlString(
                  txt.substring(0, target.length()))) : "Content & html text don't match!";
          if (left < 0) {
            if (nodelet.equals(notifyIfSplit)) {
              ret = true;
            }
            nodelet.<Text>cast().splitText(nodeletData.length() + left);
          }
        }

        nodelet = filteredHtml.getNextSibling(nodelet);
        node = renderedContent.getNextSibling(node);

        if (node == to) {
          break;
        }

        // must both be null
        assert (node == null) == (nodelet == null) : "Content & Html don't match!";
      }

      // set the next one as well
      // TODO(danilatos): Talk to alex about behaviour or operations applying in between text nodes,
      // how they often don't modify the node we'd prefer.
      if (node != null && node.isTextNode()) {
        node.setImplNodelet(nodelet);
      }

      return ret;
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isConsistent() {
    // TODO(danilatos): More rigorous?
    return getImplNodelet() == null || isImplAttached();
  }

  /**
   * Reverts the HTML implementation to match the content. Recurses.
   */
  @Override
  public void revertImplementation() {
    // TODO(danilatos): Detect nodelets that don't need reverting, to avoid
    // unecessary discarding and re-creation of element objects

    EditorStaticDeps.startIgnoreMutations();
    try {
      // reset children
      ContentView renderedContent = getRenderedContentView();
      for (ContentNode node = renderedContent.getFirstChild(this); node != null;
          node = renderedContent.getNextSibling(node)) {
        node.revertImplementation();
      }

      reattachImplChildren();
      onRepair();
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /**
   * Called after the node is reverted, in case any custom handling is needed.
   */
  protected void onRepair() {

  }

   /**
    * Add back in the impl nodelets of the children.
    *
    * Override this to provide specific functionality as needed. For example,
    * ensuring certain children live in certain specific locations of the
    * doodad's dom.
    */
  protected void reattachImplChildren() {
    ContentView renderedContent = getRenderedContentView();
    Element container = getContainerNodelet();
    if (container != null) {
      while (container.getFirstChild() != null) {
        container.getFirstChild().removeFromParent();
      }
      for (ContentNode node = renderedContent.getFirstChild(this); node != null;
          node = renderedContent.getNextSibling(node)) {
        container.appendChild(node.getImplNodelet());
      }
    } else {
      EditorStaticDeps.logger.error().log(
          "You need to override this method for your doodad: " + tagName);
    }
  }

  /**
   * Override this method to provide additional checks/repairs to the
   * implementation. Can even return a new implNodelet if desired.
   * By default, just assumes the current nodelet is fine and returns it.
   * NOTE: Do not recurse, just do a shallow fix.
   * @return repaired nodelet
   */
  protected Element revertImplNodelet() {
    return getImplNodelet();
  }

  /**
   * Action to perform on an element
   */
  public interface Action {
    /** Run action on paragraph */
    void execute(ContentElement e);
  }

  /**
   * This must be called whenever this element's children have mutated.
   * Calls onDescendantsMutated() on this node and all ancestors.
   */
  public final void notifyChildrenMutated() {
    ContentElement element = this;
    while (element != null) {
      try {
        element.onDescendantsMutated();
      } catch (RuntimeException e) {
        rethrowOrNoteErrorOnMutation(e);
      }
      element = element.getParentElement();
    }
  }

  /**
   * This must be called whenever an attribute is modified on this element
   * Calls onAttributeModified() on this node, then onDescendantsMutated()
   * on this node and all ancestors.
   */
  protected final void notifyAttributeModified(String name,
      String oldValue, String newValue) {
    try {
      onAttributeModified(name, oldValue, newValue);
    } catch (RuntimeException e) {
      rethrowOrNoteErrorOnMutation(e);
    }
    if (getParentElement() != null) {
      getParentElement().notifyChildrenMutated();
    }
  }

  protected final void notifyEmptied() {
    try {
      onEmptied();
    } catch (RuntimeException e) {
      rethrowOrNoteErrorOnMutation(e);
    }
  }

  /**
   * @return true if element has name attribute
   */
  public boolean hasName() {
    return attributes.containsKey(NAME);
  }

  /**
   * @return value of name attribute
   */
  public String getName() {
    return getAttribute(NAME);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void debugAssertHealthy() {

    // Assert we have an element impl nodelet
    assert getImplNodelet().getNodeType() == 1 :
        "ContentElement's implNodelet should be an element";

    // Assert all children are healthy, and appropriately attached
    Element container = getContainerNodelet();
    for (ContentNode child = getFirstChild(); child != null; child = child.getNextSibling()) {
      child.debugAssertHealthy();
      assert container.equals(child.getImplNodelet().getParentElement()) :
          "Child's attach nodelet should have correct parent nodelet";
    }

    super.debugAssertHealthy();
  }
}
