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
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.EditorRuntimeException;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.HtmlPoint;
import org.waveprotocol.wave.client.editor.content.TransparentManager;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.extract.Repairer;

import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * A class that manages the connection from html nodes to wrapper nodes.
 * Contains various utility methods to convert html-node-things (nodes, points)
 * to wrapper equivalents.
 *
 * There are some methods that deal with old-style node/offset inputs, and
 * convert them to new-style points. TODO(danilatos): Eradicate all use of
 * the node/offset idiom, and replace it with node/node idiom.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class NodeManager {

  private static final String BACKREF_NAME = getNextMarkerName("cn");
  // TODO(danilatos): Consider having the transparent manager say whether
  // a node is shallow/deep etc.
  private static final String TRANSPARENCY = getNextMarkerName("tl");
  private static final String TRANSPARENT_BACKREF = getNextMarkerName("tb");
  /**
   * @see #setMayContainSelectionEvenWhenDeep(Element, boolean)
   */
  private static final String MAY_CONTAIN_SELECTION = getNextMarkerName("mcs");

  /**
   * NOTE(danilatos): Initialising this to zero causes it to be re-initialised to
   * zero a few times for no apparent reason! Why??? Should not static initialising
   * code be only called once!? (This appears to happen with eclipse hosted mode..)
   */
  private static int markerIndex;

  /**
   * Because of the proliferation of markers, backreference properties etc,
   * and the need to keep them short, there is a danger of collision. Therefore,
   * use this method to statically initialise any property names.
   * @param info For debugging purposes. A couple of characters should suffice.
   *    This is ignored when debug is off.
   * @return A globally unique marker property name.
   */
  public static String getNextMarkerName(String info) {
    markerIndex++;
    if (LogLevel.showDebug()) {
      return "_x_" + markerIndex + "_" + info;
    } else {
      // TODO(danilatos): Ensure this is unique enough to be safe?
      return "_x" + markerIndex;
    }
  }

  /**
   * NOTE(danilatos): This is preliminary
   * @param element Element
   * @return True if it is an intentional transparent node
   */
  public static boolean isTransparent(Element element) {
    return element.getPropertyObject(TRANSPARENCY) != null;
  }

  /**
   * NOTE(danilatos): This is preliminary
   * Mark the element as a transparent node with the given skip level
   * @param element
   * @param skipType
   */
  public static void setTransparency(Element element, Skip skipType) {
    element.setPropertyObject(TRANSPARENCY, skipType);
  }

  /**
   * @param element
   * @return the transparency level of the given element, or null if it is not
   *   a transparent node
   */
  public static Skip getTransparency(Element element) {
    return (Skip) element.getPropertyObject(TRANSPARENCY);
  }

  /**
   * @param element
   * @return The transparent node manager for the given element, or null if
   *   none.
   */
  @SuppressWarnings("unchecked")
  public static TransparentManager<Element> getTransparentManager(Element element) {
    return (TransparentManager<Element>)element.getPropertyObject(TRANSPARENT_BACKREF);
  }

  /**
   *
   * @param element
   * @param manager
   */
  public static void setTransparentBackref(Element element,
      TransparentManager<?> manager) {
    element.setPropertyObject(TRANSPARENT_BACKREF, manager);
  }

  private final HtmlView filteredHtmlView;

  private final ContentView renderedContentView;

  private final Repairer repairer;

  /**
   */
  public NodeManager(HtmlView filteredHtmlView, ContentView renderedContentView,
      Repairer repairer) {
    this.filteredHtmlView = filteredHtmlView;
    this.renderedContentView = renderedContentView;
    this.repairer = repairer;
  }

  /**
   * General convenience method. Given any html node, attempts to find its wrapper node
   * @param node
   * @return wrapper node
   * @throws HtmlInserted
   * @throws HtmlMissing
   */
  public ContentNode findNodeWrapper(Node node) throws HtmlInserted, HtmlMissing {
    return findNodeWrapper(node, null);
  }

  /** TODO(danilatos,user) Bring back early exit & clean up this interface */
  public ContentNode findNodeWrapper(Node node, Element earlyExit)
      throws HtmlInserted, HtmlMissing {
    if (node == null) {
      return null;
    }
    if (DomHelper.isTextNode(node)) {
      return findTextWrapper(node.<Text>cast(), false);
    } else {
      return findElementWrapper(node.<Element>cast(), earlyExit);
    }
  }

  private <T extends ContentNode> T nullifyIfWrongDocument(T node) {
    if (node != null && node.getRenderedContentView() == renderedContentView) {
      return node;
    } else {
      return null;
    }
  }

  /**
   * Given an html element, finds the wrapper (may traverse upwards).
   * @param element An html element
   * @return The wrapper, if found, null otherwise.
   */
  public ContentElement findElementWrapper(Element element) {
    if (element == null) {
      return null;
    }
    HtmlView filteredHtml = filteredHtmlView;
    Element visibleElement = filteredHtml.getVisibleNode(element).cast();
    return visibleElement != null
        ? nullifyIfWrongDocument(NodeManager.getBackReference(visibleElement)) : null;
  }

  /** TODO(danilatos,user) Bring back early exit & clean up this interface */
  public ContentElement findElementWrapper(Element element, Element earlyExit) {
    // TODO(danilatos): Bring back the optimisation for IE where it stops at the
    // editor decorator element, rather than traversing up.
    return findElementWrapper(element);
  }

  /**
   * Given a text nodelet, finds the ContentTextNode wrapper if possible.
   * It also optionally can attempt to repair any trivial problems it finds
   * (such as a nodelet being replaced by an equivalent nodelet).
   *
   * The task of finding the wrapper for a text node is a fairly complex one,
   * given that:
   *   1) There may be more than one text nodelet per wrapper content text node
   *      (Reason: We cannot rely on text nodelets not to randomly be split in
   *      various scenarios, and it also makes decorating with transparent nodes
   *      easier)
   *   2) We do not save backreferences from text nodelets
   *      (Reason: IE doesn't let you, and text nodelets are unreliable anyway
   *      TODO(danilatos): Try to use backreferences in Safari/FF as an
   *      optimisation)
   *
   * There are three general types of scenarios:
   *   1) Consistent: Everything is consistent and we find the wrapper without
   *      problem.
   *   2) Normal Inconsistent: Expected inconsistencies which arise
   *      from basic typing. Some we can deal with here, others we throw an
   *      exception which the typing extractor can deal with.
   *   3) Abnormal Inconsistent: Something weird has happened and we probably
   *      need to invoke a repairing mechanism. These scenarios are also
   *      treated by throwing an inconsistency exception.
   * I have a more detailed list of letter-indexed scenarios in my notebook,
   * which I refer to in the code. TODO(danilatos): Put diagrams in google
   * docs or something
   *
   * Warning: Assumes the text node is attached. TODO(danilatos): Remove this
   * assumption?
   *
   * @param target The nodelet we are trying to find a wrapper for.
   * @param attemptRepair
   *            If true, the method will attempt trivial repairs to
   *            inconsistencies where possible. Trivial probably means no new
   *            nodes being created or destroyed, just re-assignments of
   *            starting impl nodeletes in ContentTextNodes. This may still
   *            leave the content and html out of sync, but the dom mapping will
   *            be valid. Set to false to just throw an exception instead.
   * @return The wrapping text node, or null if there is no corresponding one
   *      in this document and it's not an inconsistency problem.
   * @throws HtmlInserted
   * @throws HtmlMissing
   *
   * Note that we do not actually check text value inconsistencies in this
   * method, only tree structure inconsistencies.
   */
  // IMPORTANT: All return values must be wrapped in a call to nullifyIfWrongDocument()
  public ContentTextNode findTextWrapper(Text target, boolean attemptRepair)
      throws HtmlInserted, HtmlMissing {

    if (target == null) {
      return null;
    }

    // TODO(danilatos): Optimise this for the general case

    /*
     * Basic strategy: We go backwards to the start of our run of text nodes,
     * from there up into the wrapper world, then scan rightwards across both
     * doms to find the matching pair, using current as our cursor into the html
     * dom, and possibleOwnerNode as our cursor into the wrapper dom.
     */

    ContentView renderedContent = renderedContentView;
    HtmlView filteredHtml = filteredHtmlView;

    // The element before all previous text node siblings of target,
    // or null if no such element
    Node nodeletBeforeTextNodes;
    ContentNode wrapperBeforeTextNodes;

    // Our cursors
    Text current = target;
    ContentNode possibleOwnerNode;

    // Go leftwards to find the start of the text nodelet sequence
    for (nodeletBeforeTextNodes = filteredHtml.getPreviousSibling(target);
         nodeletBeforeTextNodes != null;
         nodeletBeforeTextNodes = filteredHtml.getPreviousSibling(nodeletBeforeTextNodes)) {
      Text maybeText = filteredHtml.asText(nodeletBeforeTextNodes);
      if (maybeText == null) {
        break;
      }
      current = maybeText;
    }

    Element parentNodelet = filteredHtml.getParentElement(target);
    if (parentNodelet == null) {
      throw new RuntimeException(
          "Somehow we are asking for the wrapper of something not in the editor??");
    }
    ContentElement parentElement = NodeManager.getBackReference(parentNodelet);

    // Find our foothold in wrapper land
    if (nodeletBeforeTextNodes == null) {
      // reached the beginning
      wrapperBeforeTextNodes = null;
      possibleOwnerNode = renderedContent.getFirstChild(parentElement);
    } else {
      // reached an element
      wrapperBeforeTextNodes = NodeManager.getBackReference(
          nodeletBeforeTextNodes.<Element>cast());
      possibleOwnerNode = renderedContent.getNextSibling(wrapperBeforeTextNodes);
    }

    // Scan to find a matching pair
    while (true) {
      // TODO(danilatos): Clarify and possibly reorganise this loop
      // TODO(danilatos): Write more unit tests to thoroughly cover all scenarios

      if (possibleOwnerNode == null) {
        // Scenario (D)
        throw new HtmlInserted(
              Point.inElement(parentElement, (ContentNode) null),
              Point.start(filteredHtml, parentNodelet)
            );
      }

      ContentTextNode possibleOwner;
      try {
        possibleOwner = (ContentTextNode) possibleOwnerNode;
      } catch (ClassCastException e) {
        if (possibleOwnerNode.isImplAttached()) {
          // Scenario (C)
          throw new HtmlInserted(
                Point.inElement(parentElement, possibleOwnerNode),
                Point.inElementReverse(filteredHtml, parentNodelet, nodeletBeforeTextNodes)
              );
        } else {
          // Scenario (A)
          // Not minor, an element has gone missing
          throw new HtmlMissing(possibleOwnerNode, parentNodelet);
        }
      }

      ContentNode nextNode = renderedContent.getNextSibling(possibleOwner);
      if (nextNode != null && !nextNode.isImplAttached()) {
        // Scenario (E)
        throw new HtmlMissing(nextNode, parentNodelet);
      }

      if (current != possibleOwner.getImplNodelet()) {
        // Scenario (B)
        if (attemptRepair) {
          possibleOwner.setTextNodelet(current);
          return nullifyIfWrongDocument(possibleOwner);
        } else {
          // TODO(danilatos): Ensure repairs handle nodes on either
          // side, as this is kind of a "replace" error
          throw new HtmlInserted(
              Point.inElement(parentElement, possibleOwner),
              Point.inElement(parentNodelet, current));
        }
      }

      Node nextNodelet = nextNode == null ? null : nextNode.getImplNodelet();

      while (current != nextNodelet && current != null) {
        // TODO(danilatos): Fix up every where in the code to use .equals
        // for GWT DOM.
        if (current == target) {
          return nullifyIfWrongDocument(possibleOwner);
        }
        current = filteredHtml.getNextSibling(current).cast();
      }

      possibleOwnerNode = nextNode;
    }

  }

  /**
   * Converts a nodelet/offset pair to a Point of ContentNode.
   * Does its best in the face of adversity to yield reasonably accurate
   * results, but may throw inconsistency exceptions.
   *
   * @param node
   * @param offset
   * @return content node point
   * @throws HtmlInserted
   * @throws HtmlMissing
   */
  public Point<ContentNode> nodeOffsetToWrapperPoint(Node node, int offset) throws HtmlInserted,
      HtmlMissing {
    if (DomHelper.isTextNode(node)) {
      return textNodeToWrapperPoint(node.<Text> cast(), offset);
    } else {
      Element container = node.<Element> cast();
      return elementNodeToWrapperPoint(container, DomHelper.nodeAfterFromOffset(container, offset));
    }
  }

  private Point<ContentNode> textNodeToWrapperPoint(Text text, int offset)
      throws HtmlInserted, HtmlMissing {

    if (getTransparency(text.getParentElement()) == Skip.DEEP) {
      Element e = text.getParentElement();
      return elementNodeToWrapperPoint(e.getParentElement(), e);
    } else {
      ContentTextNode textNode = findTextWrapper(text, true);
      return Point.<ContentNode> inText(textNode, offset + textNode.getOffset(text));
    }
  }

  private Point<ContentNode> elementNodeToWrapperPoint(Element container, Node nodeAfter)
      throws HtmlInserted, HtmlMissing {
    HtmlView filteredHtml = filteredHtmlView;
    // TODO(danilatos): Generalise/abstract this idiom
    if (filteredHtml.getVisibleNode(nodeAfter) != nodeAfter) {
      nodeAfter = filteredHtml.getNextSibling(nodeAfter);
      if (nodeAfter != null) {
        container = nodeAfter.getParentElement();
      }
    }
    return Point.inElement(findElementWrapper(container), findNodeWrapper(nodeAfter));
  }

  /**
   * Converts a nodelet to a Point of contentNode
   *
   * @param nodelet
   * @return content node point
   * @throws HtmlInserted
   * @throws HtmlMissing
   */
  public Point<ContentNode> nodeletPointToWrapperPoint(Point<Node> nodelet) throws HtmlInserted,
      HtmlMissing {
    if (nodelet.isInTextNode()) {
      return textNodeToWrapperPoint(nodelet.getContainer().<Text> cast(), nodelet.getTextOffset());
    } else {
      return elementNodeToWrapperPoint(nodelet.getContainer().<Element> cast(), nodelet
          .getNodeAfter());
    }
  }

  /**
   * Convert a wrapper point to an HtmlPoint
   * @param point
   * @return the converted point.
   */
  public HtmlPoint wrapperPointToHtmlPoint(Point<ContentNode> point) {
    if (point.isInTextNode()) {
      return wrapperTextPointToHtmlPoint(point);
    } else {
      return nodeletPointToHtmlPoint(wrapperElementPointToNodeletPoint(point));
    }
  }

  /**
   * @param point
   * @return point of nodelet
   */
  public Point<Node> wrapperPointToNodeletPoint(Point<ContentNode> point) {
    if (point.isInTextNode()) {
      HtmlPoint output = wrapperTextPointToHtmlPoint(point);
      return Point.inText(output.getNode(), output.getOffset());
    } else {
      return wrapperElementPointToNodeletPoint(point);
    }
  }

  private HtmlPoint wrapperTextPointToHtmlPoint(Point<ContentNode> point) {
    HtmlPoint output = new HtmlPoint(null, 0);
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        ((ContentTextNode) point.getContainer()).findNodeletWithOffset(
            point.getTextOffset(), output);
        break;
      } catch (HtmlMissing e) {
        repairer.handle(e);
      }
    }
    if (output.getNode() == null) {
      // TODO(danilatos): Something sensible
      throw new EditorRuntimeException("Don't know what to do with this - offset too big? point: "
          + point);
    }
    return output;
  }

  Point<Node> wrapperElementPointToNodeletPoint(Point<ContentNode> point) {
    Node nodeletAfter;

    // TODO(danilatos): return null for points that don't correspond to a location
    // in the html.

    if (point.getNodeAfter() == null) {
      // Scan backwards over trailing, non-implemented html (such as the spacer BR
      // for paragraphs) until we reach the last nodelet that has a wrapper -
      // then go one forward again and that's the nodeAfter we want to use.
      // E.g. if the content point is at the end of a paragraph, we want the nodelet
      // point to be the paragraph nodelet and the nodeAfter to be the spacer br,
      // if it is present.
      // TODO(danilatos): Clean this up & make more efficient.
      Element container = ((ContentElement) point.getContainer()).getContainerNodelet();

      // NOTE(danilatos): This could be null because the doodad is handling its own
      // rendering explicitly, so there is no clear mapping to the corresponding html
      // point.
      if (container == null) {
        return null;
      }

      Node lastWrappedNodelet = container.getLastChild();
      while (lastWrappedNodelet != null && !DomHelper.isTextNode(lastWrappedNodelet)
          && NodeManager.getBackReference(lastWrappedNodelet.<Element>cast()) == null) {
        lastWrappedNodelet = lastWrappedNodelet.getPreviousSibling();
      }
      nodeletAfter = lastWrappedNodelet == null
          ? container.getFirstChild() : lastWrappedNodelet.getNextSibling();
    } else {
      nodeletAfter = point.getNodeAfter().getImplNodeletRightwards();
    }

    if (nodeletAfter == null) {
      ContentElement visibleNode = renderedContentView.getVisibleNode(
          point.getContainer()).asElement();
      assert visibleNode != null;

      Element el = visibleNode.getContainerNodelet();
      return el != null ? Point.<Node>inElement(el, null) : null;
    } else {
      Node parentNode = nodeletAfter.getParentNode();
      // NOTE(danilatos): Instead, getImplNodeletRightwards(), (or the use of
      // some other utility method) should probably instead be guaranteeing nodeletAfter
      // being attached (visibly rendered), otherwise it should be null.
      // For now, we instead check that it has a parent (it might not in both
      // normal and abnormal circumstances).
      return parentNode != null ? Point.inElement(parentNode, nodeletAfter) : null;
    }
  }

  /**
   * @param point point of node
   * @return htmlpoint
   */
  public static HtmlPoint nodeletPointToHtmlPoint(Point<Node> point) {
    if (point.isInTextNode()) {
      return new HtmlPoint(point.getContainer(), point.getTextOffset());
    } else {
      return point.getNodeAfter() == null
          ? new HtmlPoint(point.getContainer(), point.getContainer().getChildCount())
          : new HtmlPoint(point.getContainer(), DomHelper.findChildIndex(point.getNodeAfter()));
    }
  }

  /**
   * Puts a back reference to the element into the nodelet.
   * @param nodelet The element to store the back reference. Must not be null.
   * @param element The element to reference.
   */
  public static void setBackReference(Element nodelet, ContentElement element) {
    nodelet.setPropertyObject(BACKREF_NAME, element);
  }

  /**
   * @param nodelet Gets the ContentElement reference stored in this node.
   * @return null if no back reference.
   */
  public static ContentElement getBackReference(Element nodelet) {
    return nodelet == null ? null : (ContentElement) nodelet.getPropertyObject(BACKREF_NAME);
  }

  /**
   * Check if an element has a back reference. No smartness or searching upwards.
   * @param nodelet
   * @return true if the given element has a back reference
   */
  public static boolean hasBackReference(Element nodelet) {
    return nodelet == null ? false : nodelet.getPropertyObject(BACKREF_NAME) != null;
  }

  /**
   * Converts the given point to a parent-nodeAfter point, splitting a
   * text node if necessary.
   *
   * @param point
   * @return a point at the same location, between node boundaries
   */
  public static Point.El<Node> forceElementPoint(Point<Node> point) {
    Point.El<Node> elementPoint = point.asElementPoint();
    if (elementPoint != null) {
      return elementPoint;
    }
    Element parent;
    Node nodeAfter;
    Text text = point.getContainer().cast();
    parent = text.getParentElement();
    int offset = point.getTextOffset();
    if (offset == 0) {
      nodeAfter = text;
    } else if (offset == text.getLength()) {
      nodeAfter = text.getNextSibling();
    } else {
      nodeAfter = text.splitText(offset);
    }
    return Point.inElement(parent, nodeAfter);
  }

  /**
   * Sets or clears the value returned by
   * {@link #mayContainSelectionEvenWhenDeep(Element)}
   */
  public static void setMayContainSelectionEvenWhenDeep(Element e, boolean may) {
    e.setPropertyBoolean(MAY_CONTAIN_SELECTION, may);
  }

  /**
   * We usually ignore deep nodes completely when it comes to the selection - if
   * the selection is in a deep node, we usually report there is no selection.
   *
   * If this method returns true, we should make an exception, and report the
   * selection as just outside this node (perhaps recursing as necessary).
   */
  public static boolean mayContainSelectionEvenWhenDeep(Element e) {
    return e.getPropertyBoolean(MAY_CONTAIN_SELECTION);
  }
}
