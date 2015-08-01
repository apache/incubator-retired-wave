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

package org.waveprotocol.wave.client.wavepanel.view.dom;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;

/**
 * Helper for implementations of DOM-based views. In particular, provides
 * implementations of {@link Object#hashCode()} and
 * {@link Object#equals(Object)}.
 *
 */
public final class DomViewHelper {

  private DomViewHelper() {
  }

  /**
   * Defines the equality of DOM-based views (determined solely by the equality
   * of HTML ids).
   *
   * @see Object#equals(Object)
   */
  public static boolean equals(DomView src, Object tgt) {
    if (src == tgt) {
      return true;
    } else if (!(tgt instanceof DomView)) {
      return false;
    } else {
      return src.getId().equals(((DomView) tgt).getId());
    }
  }

  /**
   * Defines the hash code of DOM-based views (determined solely by the hashcode
   * of the HTML id).
   *
   * @see Object#hashCode()
   */
  public static int hashCode(DomView src) {
    return 37 + src.getId().hashCode();
  }

  public static Element load(String baseId, Component c) {
    Element e = Document.get().getElementById(c.getDomId(baseId));
    if (e == null) {
      throw new RuntimeException("Component not found: " + c);
    }
    return e;
  }

  public static Element getBefore(Element container, Element ref) {
    Preconditions.checkArgument(ref == null || ref.getParentElement().equals(container));
    if (ref == null) {
      return getLastChildElement(container);
    } else {
      return getPreviousSiblingElement(ref);
    }
  }

  public static Element getAfter(Element container, Element ref) {
    Preconditions.checkArgument(ref == null || ref.getParentElement().equals(container));
    if (ref == null) {
      return container.getFirstChildElement();
    } else {
      return ref.getNextSiblingElement();
    }
  }

  public static void attachAfter(Element container, Element ref, Element target) {
    Preconditions.checkArgument(ref == null || ref.getParentElement().equals(container));
    Preconditions.checkArgument(target.getParentElement() == null);
    if (ref == null) {
      container.insertFirst(target);
    } else {
      container.insertAfter(target, ref);
    }
  }

  public static void attachBefore(Element container, Element ref, Element target) {
    Preconditions.checkArgument(ref == null || ref.getParentElement().equals(container));
    Preconditions.checkArgument(target.getParentElement() == null);
    if (ref == null) {
      container.appendChild(target);
    } else {
      container.insertBefore(target, ref);
    }
  }

  public static void detach(Element container, Element target) {
    Preconditions.checkArgument(target != null && target.getParentElement().equals(container));
    target.removeFromParent();
  }

  //
  // Helpers, making up for deficiencies in GWT's DOM API.
  //

  public static native Element getLastChildElement(Element elem) /*-{
    var child = elem.lastChild;
    while (child && child.nodeType != 1) {
      child = child.previousSibling;
    }
    return child;
  }-*/;

  public static native Element getPreviousSiblingElement(Element elem) /*-{
    var sib = elem.previousSibling;
    while (sib && sib.nodeType != 1) {
      sib = sib.previousSibling;
    }
    return sib;
  }-*/;
}
