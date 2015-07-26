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

import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.getAfter;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.getBefore;
import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.load;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.COLLAPSED_ATTRIBUTE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.COLLAPSED_VALUE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.TOTAL_BLIPS_ATTRIBUTE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.UNREAD_BLIPS_ATTRIBUTE;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.CollapsibleBuilder.Components;

/**
 * DOM implementation of an inline thread.
 *
 */
final class CollapsibleDomImpl implements DomView {
  /** The DOM element of this view. */
  private final Element self;

  /** The HTML id of {@code self}. */
  private final String id;
  
  /** The CSS classes used to manipulate style based on state changes. */
  private final CollapsibleBuilder.Css css;

  //
  // UI fields for both intrinsic and structural elements.
  // Element references are loaded lazily and cached.
  //

  private Element toggle;
  private Element dropContainer;
  private Element chrome;
  private Element countTotal;
  private Element countUnread;

  /**
   * Indicates if this thread has been repaired. Since some HTML parsers rip
   * block elements (divs) out of inline elements (spans), the DOM structure of
   * this view gets broken when transmitted via remote rendering. Repairing this
   * view just means restoring the structure that the browser destroyed.
   */
  private boolean isRepaired;

  CollapsibleDomImpl(Element e, String id, CollapsibleBuilder.Css css) {
    this.self = e;
    this.id = id;
    this.css = css;
  }

  public static CollapsibleDomImpl of(Element e, CollapsibleBuilder.Css css) {
    return new CollapsibleDomImpl(e, e.getId(), css);
  }

  public static CollapsibleDomImpl ofToggle(Element e, CollapsibleBuilder.Css css) {
    return of(Document.get().getElementById(Components.TOGGLE.getBaseId(e.getId())), css);
  }

  public boolean isCollapsed() {
    // Because of client/server CSS mismatches, we can not use css class names
    // to communicate state information. This only leaves the possibility of
    // explicit attributes.
    return COLLAPSED_VALUE.equals(self.getAttribute(COLLAPSED_ATTRIBUTE));
  }

  public boolean isRead() {
    boolean read = true;
    if (self.hasAttribute(UNREAD_BLIPS_ATTRIBUTE)) {
      String val = self.getAttribute(UNREAD_BLIPS_ATTRIBUTE);
      int unread = Integer.parseInt(val);
      read = unread <= 0;
    }
    return read;
  }

  public void setCollapsed(boolean collapsed) {
    if (collapsed) {
      self.setAttribute(COLLAPSED_ATTRIBUTE, COLLAPSED_VALUE);

      // Webkit's incremental layout is incorrect, so we have to kick it a bit.
      if (UserAgent.isWebkit()) {
        self.getStyle().setDisplay(Display.INLINE_BLOCK);
        // Force layout.
        self.getOffsetParent();
        // Revert to CSS display property (layed out correctly now).
        self.getStyle().clearDisplay();
      }
    } else {
      self.removeAttribute("c");
    }
    updatedCssClassNames();
  }

  /**
   * Helper method to update the proper CSS class names on the toggle and
   * chrome elements after a state change.
   */
  private void updatedCssClassNames() {
    String readStateClass = " " + (isRead() ? css.read() : css.unread());
    String collapsedStateClass = " " + (isCollapsed() ? css.collapsed() : css.expanded());

    getToggle().setClassName(css.toggle() + collapsedStateClass + readStateClass);
    getChrome().setClassName(css.chrome() + collapsedStateClass);
    getDropContainer().setClassName(css.dropContainer() + collapsedStateClass);
  }


  //
  // Structure exposed for external control.
  //

  private Element getToggle() {
    return toggle == null ? toggle = load(id, Components.TOGGLE) : toggle;
  }

  private Element getDropContainer() {
    return dropContainer == null
        ? dropContainer = load(id, Components.DROP_CONTAINER) : dropContainer;
  }

  public Element getChrome() {
    return chrome == null ? chrome = load(id, Components.CHROME) : chrome;
  }

  public Element getCountTotal() {
    return countTotal == null ? countTotal = load(id, Components.COUNT_TOTAL) : countTotal;
  }

  public Element getCountUnread() {
    return countUnread == null ? countUnread = load(id, Components.COUNT_UNREAD) : countUnread;
  }

  public void setTotalBlipCount(int totalBlipCount) {
    self.setAttribute(TOTAL_BLIPS_ATTRIBUTE, "" + totalBlipCount);
    getCountTotal().setInnerText("" + totalBlipCount);
  }

  public void setUnreadBlipCount(int unreadBlipCount) {
    self.setAttribute(UNREAD_BLIPS_ATTRIBUTE, "" + unreadBlipCount);
    Element unread = getCountUnread();
    unread.setInnerText("(" + unreadBlipCount + ")");
    if (unreadBlipCount > 0) {
      unread.getStyle().clearDisplay();
    } else {
      unread.getStyle().setDisplay(Display.NONE);
    }
    updatedCssClassNames();
  }

  public void remove() {
    getElement().removeFromParent();
  }

  /**
   * Re-assembles this thread's DOM after the expected browser munging from
   * static parsing.
   */
  private void maybeRepair() {
    if (!isRepaired) {
      self.appendChild(getChrome());
      isRepaired = true;
    }
  }

  //
  // DomView nature.
  //

  @Override
  public Element getElement() {
    maybeRepair();
    return self;
  }

  @Override
  public String getId() {
    return id;
  }

  //
  // Structure.
  //

  Element getBlipBefore(Element ref) {
    return getBefore(getChrome(), ref);
  }

  Element getBlipAfter(Element ref) {
    return getAfter(getChrome(), ref);
  }

  //
  // Equality.
  //

  @Override
  public boolean equals(Object obj) {
    return DomViewHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return DomViewHelper.hashCode(this);
  }
}
