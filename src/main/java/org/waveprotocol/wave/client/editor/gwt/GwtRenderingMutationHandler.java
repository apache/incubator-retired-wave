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

package org.waveprotocol.wave.client.editor.gwt;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * TODO: this logic should be moved to an event handler instead of a renderer
 *
 *  Renderers for doodads containing GWT widgets MUST extend or delegate to this
 * class, if they want to be logically attached (in order to receive dom
 * events).
 *
 *  Subclasses must: - Either implement {@link
 * #createGwtWidget(org.waveprotocol.wave.client.editor.content.Renderer.Renderable)}
 * and always return a new widget - Or, if null is ever returned from that
 * method, for example to delay creating the widget, then when the widget is
 * finally created, {@link #receiveNewGwtWidget(ContentElement, Widget)} or its
 * variant must be called.
 *
 *  The mutation handling implementation listens to element removal to perform
 * logical widget cleanup - this is also important, and they must be delegated
 * to if overridden.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class GwtRenderingMutationHandler extends RenderingMutationHandler {

  /**
   * GWT widget associated with element
   */
  private static final Property<Widget> WIDGET = Property.immutable("widget");

  /**
   * Logical GWT parent of the GWT widget
   */
  private static final Property<LogicalPanel> LOGICAL_PANEL = Property.immutable("parent");

  /**
   * The way doodads should be rendered in the document
   */
  public enum Flow {
    /** css inline */
    INLINE {
      @Override Element createContainer() {
        return Document.get().createSpanElement();
      }
    },
    /** css block  */
    BLOCK {
      @Override Element createContainer() {
        return Document.get().createDivElement();
      }
    },
    /** Use the widget directly */
    USE_WIDGET {
      @Override
      Element createContainer() {
        throw new AssertionError("No container for Replace");
      }
    };
    abstract Element createContainer();
  }

  /**
   * How the rendered doodads should flow
   */
  private final Flow flow;

  /**
   * @param flow the GWT widget is by default wrapped by a single html node, and
   *        this parameter indicates what type of flow behaviour the wrapper
   *        html node should exhibit.
   */
  public GwtRenderingMutationHandler(Flow flow) {
    this.flow = flow;
  }


  /////////////////////////////////////////////
  // Public API

  /**
   * Associates a widget with an element, replacing the existing widget if any,
   * and performing necessary GWT book-keeping.
   *
   * Useful for delayed widget attachment (if the widget is added to an element
   * after some time), or to replace an existing widget.
   *
   * If null is passed, the widget is removed and cleanup is performed
   *
   * @param element
   * @param widget
   */
  public final void receiveNewGwtWidget(ContentElement element, Widget widget) {
    Element nodelet = element.getImplNodelet();
    Element parent = flow == Flow.USE_WIDGET ? null : nodelet;
    receiveNewGwtWidget(element, widget, parent);
  }

  /**
   * Same as {@link #receiveNewGwtWidget(ContentElement, Widget)}, but allows
   * specifying an arbitrary physical attach point, in case the default wrapper
   * span was replaced with something else and the widget belongs in a different
   * location within the HTML rendering of the ContentElement
   *
   * @param element
   * @param widget
   * @param physicalParent
   */
  public final void receiveNewGwtWidget(ContentElement element, Widget widget,
      Element physicalParent) {
    maybeLogicalDetach(element);
    disassociateWidget(element);

    if (widget != null) {
      associateWidget(element, widget, physicalParent);
      maybeLogicalAttach(element);
    }
  }


  /////////////////////////////////////////////
  // Subclassing API

  /**
   * Called to create the GWT Widget to be associated with the element, and
   * perform any other miscellaneous initialisation.
   *
   * If null is returned, then it is the responsibility of the subclass to
   * call {@link #associateWidget(Renderable, Widget, Element)}
   *
   * @param element element to associate the widget with
   * @return the newly created widget
   */
  protected abstract Widget createGwtWidget(Renderable element);

  /**
   * Default behaviour is to not have a container nodelet, since child elements
   * are probably there for state, not for rendering.
   */
  protected Element getContainerNodelet(Widget w) {
    return null;
  }

  /**
   * @param element
   * @return the GWT Widget for the given element
   */
  @SuppressWarnings("unchecked")
  public static <T extends Widget> T getGwtWidget(ContentElement element) {
    return (T) element.getProperty(WIDGET);
  }


  /////////////////////////////////////////////
  // Methods called by the core

  /**
   * Override {@link #createGwtWidget(Renderable)} to create your widget
   */
  @Override
  public final Element createDomImpl(Renderable element) {
    Widget w = createGwtWidget(element);

    Element implNodelet;
    Element attachNodelet;
    if (flow == Flow.USE_WIDGET) {
      Preconditions.checkState(w != null, "Cannot have null widget with USE_WIDGET");
      implNodelet = w.getElement();
      attachNodelet = null;
    } else {
      implNodelet = flow.createContainer();
      attachNodelet = implNodelet;
    }

    DomHelper.setContentEditable(implNodelet, false, false);
    DomHelper.makeUnselectable(implNodelet);
    if (w != null) {
      associateWidget(element, w, attachNodelet);
    }

    return implNodelet;
  }

  /**
   * Sets the logical panel in the GWT widget hierarchy that should be the
   * parent of the widget associated with the given element.
   *
   * A null parent may be given to indicate the element is no longer rendered
   * in a DOM with GWT widgets.
   *
   * @param element
   * @param parent
   */
  public final void setLogicalPanel(ContentElement element, LogicalPanel parent) {
    LogicalPanel existingParent = element.getProperty(LOGICAL_PANEL);

    if (existingParent == null && parent == null) {
      return; // Nothing to do
    }

    Preconditions.checkState(existingParent == null || parent == null,
        "setLogicalPanel called for an element that already has it");

    if (element.isContentAttached()) {
      if (parent == null) {
        assert existingParent != null && parent == null;
        maybeLogicalDetach(element);
        element.setProperty(LOGICAL_PANEL, null);
      } else {
        // The preconditions check should guard against this being
        // called for already-attached elements.
        assert existingParent == null && parent != null;
        element.setProperty(LOGICAL_PANEL, parent);
        maybeLogicalAttach(element);
      }
    }
  }

  @Override
  public void onActivationStart(ContentElement element) {
    maybeLogicalAttach(element);
  }

  /**
   * Cleans up for when the handler is unattached from an element.
   */
  @Override
  public void onDeactivated(ContentElement element) {
    receiveNewGwtWidget(element, null);
    setLogicalPanel(element, null);
  }

  /////////////////////////////////////////////

  private void maybeLogicalAttach(ContentElement element) {
    Widget w = getGwtWidget(element);
    LogicalPanel p = getLogicalPanel(element);
    if (p != null && w != null) {
      p.doAdopt(w);
    }
  }

  private void maybeLogicalDetach(ContentElement element) {
    Widget w = getGwtWidget(element);
    LogicalPanel p = getLogicalPanel(element);
    if (p != null && w != null && w.getParent() != null) {
      p.doOrphan(w);
    }
  }

  /**
   * Assigns a widget to an element and optionally attaches it to a parent html
   * node. Does not do any GWT logical attachment
   *
   * @param element the element the widget is associated with
   * @param w the widget to insert into the logical hierarchy
   * @param physicalParent the physical place in the dom to attach the widget.
   *        May be null, in which case no physical attachment will take place.
   */
  private void associateWidget(Renderable element, Widget w,
      Element physicalParent) {
    element.setProperty(WIDGET, w);
    if (physicalParent != null) {
      physicalParent.appendChild(w.getElement());
    }
    element.setAutoAppendContainer(getContainerNodelet(w));
  }

  /**
   * The opposite of {@link #associateWidget(Renderable, Widget, Element)}
   *
   * @param element
   */
  private void disassociateWidget(ContentElement element) {
    Widget old = getGwtWidget(element);
    if (old != null) {
      old.getElement().removeFromParent();
      element.setProperty(WIDGET, null);
    }
    assert element.getProperty(WIDGET) == null;
  }

  private LogicalPanel getLogicalPanel(ContentElement element) {
    return element.getProperty(LOGICAL_PANEL);
  }
}
