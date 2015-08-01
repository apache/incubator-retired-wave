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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;


/**
 * Helper methods
 *
 * Some adapted from UIElement, so the interface could do with increasing consistency
 *
 * TODO(danilatos,user): Clean up / organise methods in this class
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DomHelper {

  /**
   * Describes the editability of an element, ignoring its context (ancestor nodes, etc).
   */
  public enum ElementEditability {
    /** The element is definitely editable */
    EDITABLE,
    /** The element is not editable */
    NOT_EDITABLE,
    /** The element is "neutral", which means its editability is inherited */
    NEUTRAL
  }

  /** Webkit editability controlling css property */
  public static final String WEBKIT_USER_MODIFY = "-webkit-user-modify";

  /**
   * Interface for receiving low-level javascript events
   */
  public interface JavaScriptEventListener {

    /**
     * @param name The event name, without any leading "on-" prefix
     * @param event The native event object
     */
    void onJavaScriptEvent(String name, Event event);
  }

  private DomHelper() {}

  /**
   * Return true if the element is a text box
   * @param element
   * @return true if the element is a text box
   */
  public static boolean isTextBox(Element element) {
    return "input".equalsIgnoreCase(element.getTagName())
        && "text".equalsIgnoreCase(element.getAttribute("type"));
  }

  /**
   * @param element
   * @param styleName
   * @return true if the element or an ancestor has the given stylename
   */
  public static boolean hasStyleOrAncestorHasStyle(Element element, String styleName) {
    while (element != null) {
      if (element.getClassName().indexOf(styleName) >= 0) {
        return true;
      }
      element = element.getParentElement();
    }
    return false;
  }

  /**
   * Cast to old-style Element.
   *
   * TODO(danilatos): Deprecate this method when GWT has updated everything to not require
   * the old style Element.
   *
   * @param element new style element
   * @return old style element
   */
  public static com.google.gwt.user.client.Element castToOld(Element element) {
    return element.cast();
  }

  /**
   * Create a div with the given style name set. Convenience method because
   * this is such a common task
   * @param styleName
   * @return The created div element
   */
  public static DivElement createDivWithStyle(String styleName) {
    DivElement d = Document.get().createDivElement();
    d.setClassName(styleName);
    return d;
  }

  /**
   * Focus the element, if possible
   * @param element
   */
  public static void focus(Element element) {
    // NOTE(user): This may not work for divs, rather use getFocusImplForPanel
    //               for divs.
    try {
      FocusImpl.getFocusImplForWidget().focus(castToOld(element));
    } catch (Exception e) {
      // Suppress null pointer condition
    }
  }

  /**
   * Blur the element, if possible
   * @param element
   *
   * NOTE(user): Dan thinks this method should be deprecated, but is not
   *               sure why... Dan, please update once you remember.
   */
  public static void blur(Element element) {
    FocusImpl.getFocusImplForWidget().blur(castToOld(element));
  }

  /**
   * Sets display:none on the given element if isVisible is false, and clears
   * the display css property if isVisible is true.
   *
   * This idiom is commonly switched on a boolean, so this method takes care of
   * the 5 lines of boilerplate.
   *
   * @param element
   * @param isVisible
   */
  public static void setDisplayVisible(Element element, boolean isVisible) {
    if (isVisible) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }

  /**
   * Finds the index of an element in its parent's list of child elements.
   * This is not the same as {@link #findChildIndex(Node)}, since it ignores
   * non-element nodes. It is in line with the element-only view of a collection
   * of children exposed by {@link Element#getFirstChildElement()} and
   * {@link Element#getNextSiblingElement()}.
   *
   * @param child  an element
   * @return the index of {@code child}, or -1 if {@code child} is not a child
   *   of its parent.
   * @see #findChildIndex(Node)
   */
  public static final int findChildElementIndex(Element child) {
    Element parent = child.getParentElement();
    Element e = parent.getFirstChildElement();
    int i = 0;
    while (e != null) {
      if (e.equals(child)) {
        return i;
      } else {
        e = e.getNextSiblingElement();
        i++;
      }
    }
    return -1;
  }

  /**
   * Wrap at least one node
   * @param with The element in which to wrap the nodes
   * @param from First node to wrap
   * @param toExcl Node after end of wrap range
   */
  public static void wrap(Element with, Node from, Node toExcl) {
    from.getParentNode().insertBefore(with, from);
    moveNodes(with, from, toExcl, null);
  }

  /**
   * @param element The element to unwrap. If not attached, does nothing.
   */
  public static void unwrap(Element element) {
    if (element.hasParentElement()) {
      moveNodes(element.getParentElement(),
          element.getFirstChild(), null, element.getNextSibling());
      element.removeFromParent();
    }
  }

  /**
   * Insert before, but for a range of adjacent siblings
   *
   * TODO(danilatos): Apparently safari and firefox let you do this in one
   *   go using ranges, which could be a lot faster than iterating manually.
   *   Create a deferred binding implementation.
   * @param parent
   * @param from
   * @param toExcl
   * @param refChild
   */
  public static void moveNodes(Element parent, Node from, Node toExcl, Node refChild) {
    for (Node n = from; n != toExcl; ) {
      Node m = n;
      n = n.getNextSibling();
      parent.insertBefore(m, refChild);
    }
  }

  /**
   * Remove nodes in the given range from the DOM
   * @param from
   * @param toExcl
   */
  public static void removeNodes(Node from, Node toExcl) {
    if (from == null || !from.hasParentElement()) {
      return;
    }
    for (Node n = from; n != toExcl && n != null;) {
      Node r = n;
      n = n.getNextSibling();
      r.removeFromParent();
    }
  }

  /**
   * Remove all children from an element
   * @param element
   */
  public static void emptyElement(Element element) {
    while (element.getFirstChild() != null) {
      element.removeChild(element.getFirstChild());
    }
  }

  /**
   * Ensures the given container contains exactly one child, the given one.
   * Provides the important property that if the container is already the parent
   * of the given child, then the child is not removed and re-added, it is left
   * there; any siblings, if present, are removed.
   *
   * @param container
   * @param child
   */
  public static void setOnlyChild(Element container, Node child) {
    if (child.getParentElement() != container) {
      // simple case
      emptyElement(container);
      container.appendChild(child);
    } else {
      // tricky case - avoid removing then re-appending the same child
      while (child.getNextSibling() != null) {
        child.getNextSibling().removeFromParent();
      }
      while (child.getPreviousSibling() != null) {
        child.getPreviousSibling().removeFromParent();
      }
    }
  }

  /**
   * Swaps out the old element for the new element.
   * The old element's children are added to the new element
   *
   * @param oldElement
   * @param newElement
   */
  public static void replaceElement(Element oldElement, Element newElement) {

    // TODO(danilatos): Profile this to see if it is faster to move the nodes first,
    // and then remove, or the other way round. Profile and optimise some of these
    // other methods too. Take dom mutation event handlers being registered into account.

    if (oldElement.hasParentElement()) {
      oldElement.getParentElement().insertBefore(newElement, oldElement);
      oldElement.removeFromParent();
    }

    DomHelper.moveNodes(newElement, oldElement.getFirstChild(), null, null);
  }

  /**
   * Make an element editable or not
   *
   * @param element
   * @param whiteSpacePreWrap Whether to additionally turn on the white space
   *   pre wrap property. If in doubt, set to true. This is what we use for
   *   the editor. So for any concurrently editable areas and such, we must
   *   use true. If false, does nothing (it does not clear the property).
   * @param isEditable
   * @return the same element for convenience
   */
  public static Element setContentEditable(Element element, boolean isEditable,
      boolean whiteSpacePreWrap) {
    if (UserAgent.isSafari()) {
      // We MUST use the "plaintext-only" variant to prevent nasty things like
      // Apple+B munging our dom without giving us a key event.

      // Assertion in GWT stuffs this up... fix GWT, in the meantime use a string map
      //      element.getStyle().setProperty("-webkit-user-modify",
      //          isEditable ? "read-write-plaintext-only" : "read-only");

      JsoView.as(element.getStyle()).setString("-webkit-user-modify",
          isEditable ? "read-write-plaintext-only" : "read-only");
    } else {
      element.setAttribute("contentEditable", isEditable ? "true" : "false");
    }

    if (whiteSpacePreWrap) {
      // More GWT assertion fun!
      JsoView.as(element.getStyle()).setString("white-space", "pre-wrap");
    }

    return element;
  }

  /**
   * Checks whether the given DOM element is editable, either explicitly or
   * inherited from its ancestors.
   * @param e Element to check
   */
  public static boolean isEditable(Element e) {
    // special early-exit for problematic shadow dom:
    if (isUnreadable(e)) {
      return true;
    }

    Element docElement = Document.get().getDocumentElement();
    do {
      ElementEditability editability = getElementEditability(e);
      if (editability == ElementEditability.NEUTRAL) {
        if (e == docElement) {
          return false;
        }
        e = e.getParentElement();
      } else {
        return editability == ElementEditability.EDITABLE ? true : false;
      }
    } while (e != null);

    // NOTE(danilatos): We didn't hit the body. The only way I know that this can happen
    // is if the browser gave us a text node from its SHADOW dom, e.g. in a text box,
    // which doesn't have any text node children. I've observed the parent of this text node
    // to be reported as a div, and the parent of that div to be null.
    return true;
  }

  public static ElementEditability getElementEditability(Element elem) {
    // NOTE(danilatos): This is not necessarily accurate in 100% of situations, with weird
    // combinations of editability/enabled etc attributes and tagnames...

    String tagName = null;
    try {
      tagName = elem.getTagName();
    } catch (Exception exception) {
      // Couldn't get access to the tag name for some reason (see b/2314641).
    }

    if (tagName != null) {
      tagName = tagName.toLowerCase();
      if (tagName.equals("input") || tagName.equals("textarea")) {
        return ElementEditability.EDITABLE;
      }
    }

    return getContentEditability(elem);
  }

  /**
   * @param element
   * @return editability in terms of content-editable only (ignore tag names)
   */
  public static ElementEditability getContentEditability(Element element) {
    String editability = null;
    if (UserAgent.isSafari()) {
      JsoView style = JsoView.as(element.getStyle());
      editability = style.getString(WEBKIT_USER_MODIFY);
      if ("read-write-plaintext-only".equalsIgnoreCase(editability) ||
          "read-write".equalsIgnoreCase(editability)) {
        return ElementEditability.EDITABLE;
      } else if (editability != null && !editability.isEmpty()) {
        return ElementEditability.NOT_EDITABLE;
      }

      // NOTE(danilatos): The css property overrides the contentEditable attribute.
      // Still keep going just to check the content editable prop, if no css property set.
    }
    try {
      editability = element.getAttribute("contentEditable");
    } catch (JavaScriptException e) {
      String elementString = "<couldn't get element string>";
      String elementTag = "<couldn't get element tag>";
      try {
        elementString = element.toString();
      } catch (Exception exception) { }
      try {
        elementTag = element.getTagName();
      } catch (Exception exception) { }

      StringBuilder sb = new StringBuilder();
      sb.append("Couldn't get the 'contentEditable' attribute for element '");
      sb.append(elementString).append("' tag name = ").append(elementTag);
      throw new RuntimeException(sb.toString(), e);
    }
    if (editability == null || editability.isEmpty()) {
      return ElementEditability.NEUTRAL;
    } else {
      return "true".equalsIgnoreCase(editability)
          ? ElementEditability.EDITABLE : ElementEditability.NOT_EDITABLE;
    }
  }

  /**
   * Sets the spell check attribute on the element.
   * @param enabled  true to enable spell check, false to disable.
   */
  public static void setNativeSpellCheck(Element element, boolean enabled) {
    element.setAttribute("spellcheck", enabled ? "true" : "false");
  }

  /**
   * Makes an element, and all its descendant elements, unselectable.
   */
  public static void makeUnselectable(Element e) {
    if (UserAgent.isIE()) {
      e.setAttribute("unselectable", "on");
      e = e.getFirstChildElement();
      while (e != null) {
        makeUnselectable(e);
        e = e.getNextSiblingElement();
      }
    }
  }

  /**
   * Used to remove event handlers from elements
   *
   * @see DomHelper#registerEventHandler(Element, String, JavaScriptEventListener)
   */
  public static final class HandlerReference extends JavaScriptObject {

    /***/
    protected HandlerReference() {}

    /**
     * Unregister a handler registered with
     * {@link #registerEventHandler(Element, String, JavaScriptEventListener)} or
     * {@link #registerEventHandler(Element, String, boolean, JavaScriptEventListener)}
     *
     * @return true if the handler was unregistered, false if unregister had
     *   already been called.
     */
    public native boolean unregister() /*-{
      var el = this.$el;
      if (el == null) {
        return false;
      }

      if (el.removeEventListener) {
        el.removeEventListener(this.$ev, this, this.$cp);
      } else if (el.detachEvent) {
        el.detachEvent('on' + this.$ev, this);
      } else {
        el['on' + this.$ev] = null;
      }

      this.$ev = null;
      return true;
    }-*/;
  }

  /**
   * A set of {@link HandlerReference} for when registering and unregistering a
   * handler on multiple events at once.
   */
  public static final class HandlerReferenceSet {
    public IdentitySet<HandlerReference> references = CollectionUtils.createIdentitySet();

    public void unregister() {
      Preconditions.checkState(references != null, "References already unregistered");
      references.each(new IdentitySet.Proc<HandlerReference>() {
        @Override
        public void apply(HandlerReference ref) {
          ref.unregister();
        }
      });
      references = null;
    }
  }

  /**
   * A low level way to register event handlers on dom elements. This differs
   * from sinkEvents in that it has nothing to do with widgets, and also allows
   * specifying any event name as a string.
   *
   * NOTE(danilatos): Care must be taken when using this low-level technique,
   * you will need to handle your own cleanup to avoid memory leaks.
   *
   * @param el The dom element on which to listen to events
   * @param eventName The name of the event, without any "on-" prefix
   * @param listener
   * @return a handler to be used with de-registering
   */
  public static HandlerReference registerEventHandler(Element el,
      String eventName, JavaScriptEventListener listener) {
    return registerEventHandler(el, eventName, false, listener);
  }

  // TODO(danilatos): Split the implementation out into browser-specific versions

  /**
   * Same as {@link #registerEventHandler(Element, String, JavaScriptEventListener)}
   * except provides the (non-cross-browser) capture parameter
   */
  public static native HandlerReference registerEventHandler(Element el,
      String eventName, boolean capture, JavaScriptEventListener listener) /*-{

    var func = $entry(function(e) {
      var evt = e || $wnd.event;
      listener.
          @org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener::onJavaScriptEvent(Ljava/lang/String;Lcom/google/gwt/user/client/Event;)
          (eventName, evt);
    });

    if (el.addEventListener) {
        el.addEventListener(eventName, func, capture);
    } else if (el.attachEvent) {
        el.attachEvent('on' + eventName, func);
    } else {
        el['on' + eventName.toLowerCase()] = func;
    }

    // Setup handler reference object
    func.$ev = eventName;
    func.$cp = capture;
    func.$el = el;
    return func;
  }-*/;

  /**
   * Registers a listener for multiple browser events in one go
   *
   * @param el element to listen on
   * @param eventNames set of events
   * @param listener
   * @return a reference set to be used for unregistering the handler for all
   *         events in one go
   */
  public static HandlerReferenceSet registerEventHandler(final Element el,
      ReadableStringSet eventNames, final JavaScriptEventListener listener) {
    Preconditions.checkArgument(!eventNames.isEmpty(), "registerEventHandler: Event set is empty");
    final HandlerReferenceSet referenceSet = new HandlerReferenceSet();
    eventNames.each(new StringSet.Proc() {
      @Override
      public void apply(String eventName) {
        referenceSet.references.add(registerEventHandler(el, eventName, listener));
      }
    });
    return referenceSet;
  }

  /**
   * @return true if it is an element
   */
  public static boolean isElement(Node n) {
    return n.getNodeType() == Node.ELEMENT_NODE;
  }

  /**
   * @return true if it is a text node
   */
  public static boolean isTextNode(Node n) {
    return n.getNodeType() == Node.TEXT_NODE;
  }

  /**
   * Finds the index of an element among its parent's children, including
   * text nodes.
   * @param toFind the node to retrieve the index for
   * @return index of element
   *
   * TODO(danilatos): This could probably be done faster with
   * a binary search using text ranges.
   * TODO(lars): adapt to non standard browsers.
   * TODO(lars): is there a single js call that does this?
   */
  public static native int findChildIndex(Node toFind) /*-{
    var parent = toFind.parentNode;
    var count = 0, child = parent.firstChild;
    while (child) {
      if (child == toFind)
        return count;
      if (child.nodeType == 1 || child.nodeType == 3)
        ++count;
      child = child.nextSibling;
    }

    return -1;
  }-*/;

  /**
   * The last child of element this element. If there is no such element, this
   * returns null.
   */
  // GWT forgot to add Element.getLastChildElement(), to be symmetric with
  // Element.getFirstChildElement().
  public static native Element getLastChildElement(Element elem) /*-{
    var child = elem.lastChild;
    while (child && child.nodeType != 1)
      child = child.previousSibling;
    return child;
  }-*/;  
  
  /**
   * Gets a list of descendants of e that match the given class name.
   *
   * If the browser has the native method, that will be called. Otherwise, it
   * traverses descendents of the given element and returns the list of elements
   * with matching classname.
   *
   * @param e
   * @param className
   */
  public static NodeList<Element> getElementsByClassName(Element e, String className) {
    if (QuirksConstants.SUPPORTS_GET_ELEMENTS_BY_CLASSNAME) {
      return getElementsByClassNameNative(e, className);
    } else {
      NodeList<Element> all = e.getElementsByTagName("*");
      if (all == null) {
        return null;
      }
      JsArray<Element> ret = JavaScriptObject.createArray().cast();
      for (int i = 0; i < all.getLength(); ++i) {
        Element item = all.getItem(i);
        if (className.equals(item.getClassName())) {
          ret.push(item);
        }
      }
      return ret.cast();
    }
  }

  private static native NodeList<Element> getElementsByClassNameNative(
      Element e, String className) /*-{
    return e.getElementsByClassName(className);
  }-*/;


  /**
   * Checks whether the properties of given node cannot be accessed (by testing the nodeType).
   *
   * It is sometimes the case where we need to access properties of a Node, but the properties
   * on that node are not readable (for example, a shadow node like a div created to hold the
   * selection within an input field).
   *
   * In these cases, when the javascript cannot access the node's properties, any attempt to do
   * so may cause an internal permissions exception. This method swallows the exception and uses
   * its existence to indicate whether or not the node is actually readable.
   *
   * @param n Node to check
   * @return Whether or not the node can have properties read.
   */
  public static boolean isUnreadable(Node n) {
    try {
      n.getNodeType();
      return false;
    } catch (RuntimeException e) {
      return true;
    }
  }

  /**
   * Converts a nodelet/offset pair to a Point of Node.
   * Just a simple mapping, it is agnostic to inconsistencies, filtered views, etc.
   * @param node
   * @param offset
   * @return html node point
   */
  public static Point<Node> nodeOffsetToNodeletPoint(Node node, int offset) {
    if (isTextNode(node)) {
      return Point.inText(node, offset);
    } else {
      Element container = node.<Element>cast();
      return Point.inElement(container, nodeAfterFromOffset(container, offset));
    }
  }

  /**
   * Given a node/offset pair, return the node after the point.
   *
   * @param container
   * @param offset
   */
  public static Node nodeAfterFromOffset(Element container, int offset) {
    return offset >= container.getChildCount() ? null : container.getChild(offset);
  }
}
