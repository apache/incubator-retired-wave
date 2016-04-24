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

import org.waveprotocol.wave.client.common.util.VolatileComparable;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Pretty;
import org.waveprotocol.wave.model.util.OffsetList;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Content node. Base class for ContentTextNode and ContentElement.
 *
 * TODO(danilatos): Thoroughly update the javadoc for this class
 *
 * See {@link ContentDocument} for more...
 *
 * In this context, the word "nodelet" is used to refer to the JSO dom nodes, to
 * avoid confusion with the word "node", which when unqualified, refers to
 * subclasses of ContentNode.
 *
 * The handleXXX methods with boolean return values are called to see if this
 * node will handle a given event. If the method handles the event, it will
 * return true, and no further processing of the event is needed. Otherwise, it
 * will return false. The methods may trigger an operation event if they handle
 * the browser event. (They might not, for example to simply cause the browser
 * event to be ignored, and prevent the default handling by returning true)
 *
 * TODO(danilatos): Extract out an interface for these methods.
 *
 * NOTE(danilatos): By default, here and in subclasses, if a method's name is
 * ambiguous as to whether it refers to the content or the html, it refers to
 * the html.
 *
 */
public abstract class ContentNode implements Doc.N,
    VolatileComparable<ContentNode>, MutatingNode<ContentNode, ContentElement> {

  /**
   * Debug logger
   */
  protected static LoggerBundle logger = new DomLogger("editor-node");

  private final ExtendedClientDocumentContext context;

  private Node implNodelet;
  private ContentElement parent = null;
  private ContentNode next = null;
  private ContentNode prev = null;
  protected static final int MAX_REPAIR_ATTEMPTS = 50;

  private OffsetList.Container<ContentNode> indexingContainer;

  /**
   * @param implNodelet The wrapped nodelet
   * @param context
   */
  public ContentNode(Node implNodelet, ExtendedClientDocumentContext context) {
    this.context = context;
    this.implNodelet = implNodelet;
  }

  /**
   * @return The top-level wrapped implementation html nodelet. It might be null
   *         (either because we are halfway through repairing, or because this
   *         ContentNode is a meta-node that has no corresponding HTML
   *         implementation)
   */
  public Node getImplNodelet() {
    return this.implNodelet;
  }

  /**
   * Same as {@link #getImplNodelet()}, but traverses the filtered view righwards
   * until it finds a wrapper that actually has an impl nodelet, if the first
   * doesn't.
   */
  public Node getImplNodeletRightwards() {
    return getImplNodeletRightwards(null);
  }

  /**
   * Same as {@link #getImplNodeletRightwards()} but with early exit
   * @param toExcl Stop here if reached
   */
  public Node getImplNodeletRightwards(ContentNode toExcl) {
    // TODO(danilatos): This implementation will skip over an html-only node
    // in the midst of other impl nodelets. This might not be desirable in
    // some contexts...
    assert isContentAttached();
    ContentNode node = this;
    Node nodelet = null;
    ContentView renderedContent = getRenderedContentView();
    while (node != toExcl) {
      nodelet = node.getImplNodelet();
      if (nodelet != null) {
        break;
      }
      node = renderedContent.getNextSibling(node);
    }
    return nodelet;
  }

  /**
   * Same as {@link #getImplNodeletRightwards()}, but starts from the next
   * sibling of this ContentNode
   */
  public Node getNextImplNodeletRightwards() {
    return getNextImplNodeletRightwards(null);
  }

  /**
   * Same as {@link #getNextImplNodeletRightwards()} but with early exit
   * @param toExcl Stop here if reached
   */
  public Node getNextImplNodeletRightwards(ContentNode toExcl) {
    ContentNode next = getNextSibling();
    return next == null ? null : next.getImplNodeletRightwards(toExcl);
  }

  public Node normaliseImpl() {
    return getImplNodelet();
  }

  void setImplNodelet(Node nodelet) {
    implNodelet = nodelet;
  }

  void breakBackRef(boolean recurse) {
  }

  /**
   * TODO(danilatos): Use something other than this method to determine this
   * @return whether a node is persistent.
   */
  public boolean isPersistent() {
    return getIndexingContainer() != null;
  }

  /**
   * @see ReadableDocument#getParentElement(Object)
   */
  public ContentElement getParentElement() {
    return parent;
  }

  /**
   * @see ReadableDocument#getNextSibling(Object)
   */
  public ContentNode getNextSibling() {
    return next;
  }

  /**
   * @see ReadableDocument#getPreviousSibling(Object)
   */
  public ContentNode getPreviousSibling() {
    return prev;
  }

  /**
   * @see ReadableDocument#getFirstChild(Object)
   */
  public ContentNode getFirstChild() {
    return null;
  }

  /**
   * @see ReadableDocument#getLastChild(Object)
   */
  public ContentNode getLastChild() {
    return null;
  }

  /** package private setter, used by ContentElement */
  void setNext(ContentNode next) {
    this.next = next;
  }

  /** package private setter, used by ContentElement */
  void setPrev(ContentNode prev) {
    this.prev = prev;
  }

  /** package private setter, used by ContentElement */
  void setParent(ContentElement parent) {
    this.parent = parent;
  }

  /** package private getter, used by ContentRawDocument */
  OffsetList.Container<ContentNode> getIndexingContainer() {
    return indexingContainer;
  }

  /** package private setter, used by ContentRawDocument */
  void setIndexingContainer(OffsetList.Container<ContentNode> container) {
    indexingContainer = container;
  }

  /**
   * Package private, used by ContentElement
   * Removes from the wrapper structure
   * Does not affect the underlying dom node
   * Does not affect its own pointers
   * Does not affect its relationship with its children, if any
   */
  final void removeFromShadowTree() {
    if (prev == null) {
      if (parent != null) {
        parent.setFirstChild(next);
      }
    } else {
      prev.next = next;
    }
    if (next == null) {
      if (parent != null) {
        parent.setLastChild(prev);
      }
    } else {
      next.prev = prev;
    }
  }

  /**
   * Sets its prev, next and parent pointers to null
   * Does not affect underlying dom node
   * Does not affect neighbours
   * Does not affect relationship with children, if any
   */
  final void clearNodeLinks() {
    prev = next = parent = null;
  }

  /**
   * @see ReadableDocument#getNodeType(Object)
   */
  public abstract short getNodeType();

  /**
   * @return true if this node is an element
   */
  public abstract boolean isElement();

  /**
   * @return true if this node is a text node
   */
  public abstract boolean isTextNode();

  /**
   * @return the node as a text node if it is one, null otherwise
   */
  public abstract ContentTextNode asText();

  /**
   * @return the node as an element if it is one, null otherwise
   */
  public abstract ContentElement asElement();

  /**
   * @return true if this node is in the rendered view
   */
  public boolean isRendered() {
    // This logic might need updating at some point.
    // Currently, if a node is lacking an impl nodelet, then we treat it
    // as unrendered. Text nodes are an exception, because they often
    // lack an impl nodelet because of typing extraction & zipping.
    return getImplNodelet() != null || (isTextNode() && getParentElement().isRendered());
  }

  /** Package private low level functionality */
  final ExtendedClientDocumentContext getExtendedContext() {
    return context;
  }

  /**
   * @return the document context this node is a part of
   */
  public final ClientDocumentContext getContext() {
    return context;
  }

  /**
   * Exposed for subclasses.
   */
  public final HtmlView getFilteredHtmlView() {
    return context.rendering().getFilteredHtmlView();
  }

  /**
   * Exposed for subclasses.
   */
  public ContentView getRenderedContentView() {
    return context.rendering().getRenderedContentView();
  }

  /**
   * Exposed for subclasses.
   */
  public final CMutableDocument getMutableDoc() {
    return context.document();
  }

  /**
   * Exposed for subclasses
   *
   * DO NOT expose getAggressiveSelectionHelper() !
   * It could cause all kinds of problems with interleaved application of ops.
   */
  public final SelectionHelper getSelectionHelper() {
    return context.editing().getSelectionHelper();
  }

  // Package private
  final Repairer getRepairer() {
    return context.rendering().getRepairer();
  }

  // Package private
  final TypingExtractor getTypingExtractor() {
    return context.editing().getTypingExtractor();
  }

  /**
   * Exposed for subclasses
   */
  public final LocationMapper<ContentNode> getLocationMapper() {
    return context.locationMapper();
  }

  /**
   * Exposed for subclasses
   */
  public final boolean inEditMode() {
    return context.isEditing();
  }

  /**
   * Exposed for subclasses
   */
  public final String getEditorUniqueString() {
    return context.getDocumentId();
  }

  /**
   * Exposed for subclasses
   */
  public final SuggestionsManager getSuggestionsManager() {
    return context.editing().getSuggestionsManager();
  }

  /**
   * Exposed for subclasses
   */
  public final ContentElement getElementByName(String name) {
    return context.getElementByName(name);
  }

  /**
   * Check if the HTML for this element is "OK" (where OK loosely means
   * "doesn't need fixing").
   * @return true if html impl is correct & attached
   */
  public abstract boolean isConsistent();

  /**
   * Throw away and redo the html implementation (drastic repair mechanism)
   */
  public abstract void revertImplementation();

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    name = name.substring(name.lastIndexOf('.') + 1);
    String nodeletString = "destroyed";
    try {
      // Note(user): this toString can fail for text nodes that IE's editor has deleted
      nodeletString = getImplNodelet() == null ? "null" :
          new Pretty<Node>().print(context.rendering().getFullHtmlView(), getImplNodelet());
    } catch (Throwable t) {
    }
    String contentString = new Pretty<ContentNode>().print(
        FullContentView.INSTANCE, this);
    return name + ": " + contentString + " / " + nodeletString;
  }

  //////// MUTATING NODE

  /**
   * {@inheritDoc}
   */
  public void onAddedToParent(ContentElement previousParent) {}

  /**
   * {@inheritDoc}
   */
  public void onRemovedFromParent(ContentElement newParent) {}

  /**
   * {@inheritDoc}
   */
  public void onChildAdded(ContentNode child) {}

  /**
   * {@inheritDoc}
   */
  public void onChildRemoved(ContentNode child) {}

  /**
   * {@inheritDoc}
   */
  public void onAttributeModified(String name, String oldValue, String newValue) {}

  /**
   * {@inheritDoc}
   */
  public void onDescendantsMutated() {}

  /**
   * {@inheritDoc}
   */
  public void onEmptied() {}

  void rethrowOrNoteErrorOnMutation(RuntimeException e) {
    try {
      assert false;
    } catch (AssertionError ae) {
      // assertions turned on - re-throw unconditionally
      throw e;
    }
    if (LogLevel.showErrors()) {
      throw e;
    } else {
      noteErrorOnMutationEvent(e);
    }
  }

  /**
   * This must be called after this node is added to a parent.
   * Calls onAddedToParent, onChildAdded on all appropriate nodes.
   */
  protected final void notifyAddedToParent(ContentElement oldParent,
      boolean notifyMutatedUpwards) {
    try {
      // TODO(danilatos, lars): Order of these? does it matter?
      onAddedToParent(oldParent);
      ContentElement parent = getParentElement();
      parent.onChildAdded(this);
    } catch (RuntimeException e) {
      rethrowOrNoteErrorOnMutation(e);
    }
    if (notifyMutatedUpwards) {
      parent.notifyChildrenMutated();
    }
  }

  /**
   * This must be called after this node is removed from a parent. Calls
   * onRemovingFromParent, onRemovingChild
   *
   * Does NOT call onDescendantsMutated
   *
   * @param oldParent the parent this node is being removed from
   * @param newParent the parent this node is being moved to, if any. null if it
   *        is being removed from the DOM altogether
   */
  protected final void notifyRemovedFromParent(ContentNode oldParent, ContentElement newParent) {
    try {
      onRemovedFromParent(newParent);
      oldParent.onChildRemoved(this);
    } catch (RuntimeException e) {
      rethrowOrNoteErrorOnMutation(e);
    }
  }

  /**
   * Gracefully handle any errors when changing the underlying HTML dom.
   * This should always be used and exception guards should be placed around
   * code that mutates the HTML, wherever exceptions could cause document
   * corruption.
   * @param e The exception thrown
   */
  void noteErrorWithImplMutation(Exception e) {
    // TODO(danilatos, mtsui): Better handling, see why we are throwing
    // exceptions in the first place and what sorts of exceptions.
    logger.error().log(e + " Scheduling revert.");
    ScheduleCommand.addCommand(new Task() {
      public void execute() {
        getRepairer().revert(Point.inElement(getParentElement(), ContentNode.this), null);
      }
    });
  }

  /**
   * Gracefully handle any errors thrown by external code in a mutation handler.
   * This should always be used and exception guards should be placed around
   * code that calls the notifyXXX methods, wherever exceptions could cause document
   * corruption.
   * @param e The exception thrown
   */
  void noteErrorOnMutationEvent(Exception e) {
    // TODO(danilatos): Better handling
    logger.error().log(
          "noteErrorOnMutationEvent: " + e);
    // For debug builds, fail here rather than trying to recover.
    assert false : "noteErrorOnMutationEvent: " + e;
  }

  ///// IMPL helpers

  /**
   * Non static versions. Separation exists purely because the exception guarding
   * requires the "this" context, but we'd like to enforce a static
   * context for the meat of the implementation.
   */
  void implInsertBefore(ContentElement parent,
      ContentNode from, ContentNode to, ContentNode refChild, Element oldContainerNodelet) {
    try {
      staticImplInsertBefore(parent, from, to, refChild, oldContainerNodelet);
    } catch (RuntimeException e) {
      e.printStackTrace();
      // Safe to swallow the exception, the impl mutation code does not
      // transitively affect external state.
      noteErrorWithImplMutation(e);
    }
  }

  /**
   * Do not use these directly, they are used by the non-static equivalents
   *
   * Parameters correspond to parameters of
   * @see RawDocument#insertBefore(Object, Object, Object, Object)
   */
  private static void staticImplInsertBefore(ContentElement parent,
      ContentNode from, ContentNode toExcl, ContentNode refChild, Element oldContainerNodelet) {
    Preconditions.checkArgument(toExcl == null
        || toExcl.getParentElement() == from.getParentElement(),
        "invalid toExcl");

    Element containerNodelet = parent.getContainerNodelet();
    if (containerNodelet != null) {
      Node implRef = null;
      // Don't use getImplNodeletRightwards(), it's too clever
      for (ContentNode node = refChild; node != null; node = node.getNextSibling()) {
        if (node.getImplNodelet() != null) {
          Preconditions.checkState(node.getImplNodelet().hasParentElement(),
              "implNodelet not attached");
          implRef = node.getImplNodelet();
          break;
        }
      }

      if (implRef != null) {
        assert implRef.getParentElement() == containerNodelet;
        // Be robust if assertions are off
        containerNodelet = implRef.getParentElement();
        if (containerNodelet == null) {
          return;
        }
      }

      for (ContentNode node = from; node != toExcl; node = node.getNextSibling()) {
        if (node.isTextNode()) {
          ((ContentTextNode) node).normaliseImpl();
        }
        Node nodelet = node.getImplNodelet();
        if (nodelet != null) {
          containerNodelet.insertBefore(nodelet, implRef);
        }
      }
    } else {
      if (oldContainerNodelet != null) {
        for (ContentNode node = from; node != toExcl; node = node.getNextSibling()) {
          Node nodelet = node.getImplNodelet();
          if (nodelet != null) {
            nodelet.removeFromParent();
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  public boolean isComparable() {
    // TODO(danilatos): Is there a more robust measure, whilst remaining efficient?
    return isContentAttached();
  }

  /**
   * Comparison is based on position in the tree.
   *
   * TODO(danilatos): Use our new indexing scheme to compare instead??
   *
   * WARNING(danilatos): This is a dynamic property! Be careful when you use it.
   * TODO(danilatos): Investigate if it's better to not implement the comparator
   * interface to prevent accidental inappropriate use, but just have this
   * method implemented for when needed directly.
   * {@link #isComparable()} will return false when this node cannot be compared
   * to other nodes.
   *
   * {@inheritDoc}
   */
  public int compareTo(ContentNode other) {
    // TODO(danilatos): Room for some optimisation in this method.
    // Could probably do most (not all) cases with some kind of text range
    // comparison.
    // http://developer.mozilla.org/en/docs/DOM:range.compareBoundaryPoints

    if (!isComparable() || !other.isComparable()) {
      throw new IllegalArgumentException("Cannot compare unattached nodes");
    }

    // Map of elements in the ancestor path -> child of said parent in ancestor path
    Map<ContentNode, ContentNode> ancestors =
        new HashMap<ContentNode, ContentNode>();

    ContentNode minePrev = null, theirsPrev = null;
    ContentNode mine = this, theirs = other;

    // Check if the same
    if (mine == theirs || mine.equals(theirs)) {
      return 0;
    }

    // Go up one level if text nodes, to avoid placing them as keys in the map
    if (mine instanceof ContentTextNode) {
      minePrev = mine;
      mine = mine.getParentElement();
    }

    if (theirs instanceof ContentTextNode) {
      theirsPrev = theirs;
      theirs = theirs.getParentElement();
    }

    // Populate my ancestor chain
    while (mine != null) {
      ancestors.put(mine, minePrev);
      minePrev = mine;
      mine = mine.getParentElement();
    }

    // Find nearest common ancestor
    ContentNode nca = theirs;
    while (!ancestors.containsKey(nca)) {
      theirsPrev = nca;
      assert nca != null : "Incomparable nodes!";
      nca = nca.getParentElement();
    }

    minePrev = ancestors.get(nca);
    if (minePrev == null) {
      return -1;
    }
    if (theirsPrev == null) {
      return 1;
    }
    // We assume that they are not equal, if we're up to here.
    for (ContentNode search = minePrev; search != null; search = search.getPreviousSibling()) {
      if (search.equals(theirsPrev)) {
        return 1;
      }
    }
    return -1;
  }

  /**
   * TODO(danilatos): A more robust way to see if the node still "exists".
   * w.r.t. the content representation.
   * We also need to clean these up when they are removed...
   *
   * @return true if the node is attached to our content tree
   */
  public boolean isContentAttached() {
    ContentElement e = isTextNode() ? getParentElement() : (ContentElement) this;
    ContentElement root = getMutableDoc().getDocumentElement();
    while (e != root) {
      if (e == null) {
        return false;
      }
      e = e.getParentElement();
    }
    return true;
  }

  /**
   * @return true if the node is still attached w.r.t. the html implementation
   */
  public boolean isImplAttached() {
    // TODO(danilatos): Implement this as doing a filtered search up the filtered html tree
    Node nodelet = getImplNodelet();
    return nodelet != null && nodelet.hasParentElement();
  }

  /**
   * Assert that node is healthy
   */
  public void debugAssertHealthy() {
    // Assert that implNodelet points back to this
//    Assert.assertEquals("Backref should be to wrapping ContentNode",
//        this, ContentNode.getContentNode(implNodelet));
  }

  /**
   * @param other
   * @return true if this node is equal to or is an ancestor of other
   */
  boolean isOrIsAncestorOf(ContentNode other) {
    while (other != null) {
      if (this == other) {
        return true;
      }
      other = other.getParentElement();
    }
    return false;
  }
}
