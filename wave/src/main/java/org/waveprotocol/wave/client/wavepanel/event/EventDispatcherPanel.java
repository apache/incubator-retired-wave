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


package org.waveprotocol.wave.client.wavepanel.event;

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.KIND_ATTRIBUTE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * A panel that enables arbitrary event handling using a single DOM event
 * listener.
 * <p>
 * This panel acts as a sink for all DOM events in its subtree, with a single
 * top-level DOM event listener. Application-level event handlers register
 * themselves with this panel against elements of a particular "kind". When a
 * browser-event occurs, this panel traces up the DOM hierarchy from the source
 * of the event, locating the nearest ancestor with a kind for which an event
 * handler is registered, and dispatches the event to that handler. Dispatching
 * continues up the DOM tree to all such element/handler pairs, until this
 * panel's element is reached, or until a handler declares that propagation
 * should stop. This process is analogous to the native browser mechanism of
 * event bubbling.
 * <p>
 * This dispatch mechanism has some specific advantages and disadvantages.<br/>
 * Advantages:
 * <ul>
 * <li>since it uses only a single listener, with contextualization driven by
 * data in the DOM, it is more appropriate for a page with a server-supplied
 * rendering, since it avoids the cost of traversing the entire DOM in order to
 * hook up individual DOM event listeners;</li>
 * <li>it reduces memory overhead; and</li>
 * <li>finally, it allows UI interaction to occur in a GWT application without
 * using Widgets, which are relatively expensive and heavyweight.</li>
 * </ul>
 *  <br/> Disadvantages:
 * <ul>
 * <li>runtime dispatch cost is slower;</li>
 * <li>mixes state and control by injecting kind values into the DOM;</li>
 * <li>event-handling setup requires global context (this object), in order to
 * register event listeners, rather than being able to setup event-handling
 * directly on a Widget.</li>
 * </ul>
 *
 */
//
// Example: (not in Javadoc, because Google's auto-formatter kills it)
//
// <div onclick="handle()"> <-- 2. Bubbling brings the event to this panel
// ..<div>
// ....<div kind="blip"> <-- 3. This panel dispatches to a "blip" handler
// ......<div></div> <-- 1. Click event occurs on this element
// ....</div>
// ..</div>
// </div>
//
public final class EventDispatcherPanel extends ComplexPanel
    implements EventHandlerRegistry, LogicalPanel {

  /**
   * A collection of handlers for a particular event type. This collection
   * registers itself for GWT events, and dispatches them to registered handlers
   * based on kind.
   *
   * @param <E> event type
   * @param <W> wave handler for that event type
   */
  @VisibleForTesting
  static abstract class HandlerCollection<E, W> {
    /** Top element of the panel (where dispatch stops). */
    private final Element top;

    /** Name of the event type, for error reporting. */
    private final String typeName;

    /** Registered handlers, indexed by kind. */
    private final StringMap<W> waveHandlers = CollectionUtils.createStringMap();

    /** Optional global handler for this event type. */
    private W globalHandler;

    /** True iff this collection has registered itself for GWT events. */
    private boolean registered;

    HandlerCollection(Element top, String typeName) {
      this.top = top;
      this.typeName = typeName;
    }

    /**
     * Installs the appropriate GWT event handlers for this event type,
     * forwarding events to {@link #dispatch(Object, Element)}.
     */
    abstract void registerGwtHandler();

    /**
     * Invokes a handler with a given event.
     *
     * @param event event that occurred
     * @param context kind-annotated element associated with the event
     * @param handler kind-registered handler
     * @return true if the event should not propagate to other handlers.
     */
    abstract boolean dispatch(E event, Element context, W handler);

    /**
     * Registers an event handler for elements of a particular kind.
     *
     * @param kind element kind for which events are to be handled, or {@code
     *        null} to handle global events
     * @param handler handler for the events
     */
    void register(String kind, W handler) {
      if (kind == null) {
        // Global handler.
        if (globalHandler != null) {
          throw new IllegalStateException(
              "Feature conflict on UI: " + kind + " with event: " + typeName);
        }
        globalHandler = handler;
      } else {
        if (waveHandlers.containsKey(kind)) {
          throw new IllegalStateException(
              "Feature conflict on UI: " + kind + " with event: " + typeName);
        }
        waveHandlers.put(kind, handler);
      }
      if (!registered) {
        registerGwtHandler();
        registered = true;
      }
    }

    /**
     * Dispatches an event through this handler collection.
     *
     * @param event event to dispatch
     * @param target target element of the event
     * @return true if a handled, false otherwise.
     */
    boolean dispatch(E event, Element target) {
      while (target != null) {
        if (target.hasAttribute(KIND_ATTRIBUTE)) {
          W handler = waveHandlers.get(target.getAttribute(KIND_ATTRIBUTE));
          if (handler != null) {
            if (dispatch(event, target, handler)) {
              return true;
            }
          }
        }
        target = !target.equals(top) ? target.getParentElement() : null;
      }
      return dispatchGlobal(event);
    }

    /**
     * Dispatches an event to the global handler for this event type.
     *
     * @param event event to dispatch
     * @return true if handled, false otherwise.
     */
    boolean dispatchGlobal(E event) {
      if (globalHandler != null) {
        return dispatch(event, top, globalHandler);
      } else {
        return false;
      }
    }
  }

  /**
   * Handler collection for click events.
   */
  private final class ClickHandlers // \u2620
      extends HandlerCollection<ClickEvent, WaveClickHandler> implements ClickHandler {
    ClickHandlers() {
      super(getElement(), "click");
    }

    @Override
    void registerGwtHandler() {
      addDomHandler(this, ClickEvent.getType());
    }

    @Override
    boolean dispatch(ClickEvent event, Element context, WaveClickHandler handler) {
      return handler.onClick(event, context);
    }

    @Override
    public void onClick(ClickEvent event) {
      if (dispatch(event, event.getNativeEvent().getEventTarget().<Element>cast())) {
        event.stopPropagation();
      }
    }
  }

  /**
   * Handler collection for double-click events.
   */
  private final class DoubleClickHandlers // \u2620
      extends HandlerCollection<DoubleClickEvent, WaveDoubleClickHandler>
      implements DoubleClickHandler {
    DoubleClickHandlers() {
      super(getElement(), "double-click");
    }

    @Override
    void registerGwtHandler() {
      addDomHandler(this, DoubleClickEvent.getType());
    }

    @Override
    boolean dispatch(DoubleClickEvent event, Element context, WaveDoubleClickHandler handler) {
      return handler.onDoubleClick(event, context);
    }

    @Override
    public void onDoubleClick(DoubleClickEvent event) {
      if (dispatch(event, event.getNativeEvent().getEventTarget().<Element>cast())) {
        event.stopPropagation();
      }
    }
  }

  /**
   * Handler collection for mousedown events.
   */
  private final class MouseDownHandlers // \u2620
      extends HandlerCollection<MouseDownEvent, WaveMouseDownHandler> implements MouseDownHandler {
    MouseDownHandlers() {
      super(getElement(), "mousedown");
    }

    @Override
    void registerGwtHandler() {
      addDomHandler(this, MouseDownEvent.getType());
    }

    @Override
    boolean dispatch(MouseDownEvent event, Element context, WaveMouseDownHandler handler) {
      return handler.onMouseDown(event, context);
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
      if (dispatch(event, event.getNativeEvent().getEventTarget().<Element>cast())) {
        event.stopPropagation();
      }
    }
  }

  /**
   * Handler collection for change vents.
   */
  private final class ChangeHandlers
      extends HandlerCollection<ChangeEvent, WaveChangeHandler> implements ChangeHandler {
    ChangeHandlers() {
      super(getElement(), "change");
    }

    @Override
    void registerGwtHandler() {
      addDomHandler(this, ChangeEvent.getType());
    }


    @Override
    boolean dispatch(ChangeEvent event, Element context, WaveChangeHandler handler) {
      Timer timer = null;
      if (Timing.isEnabled()) {
        Timing.enterScope();
        timer = Timing.start("Mouse event dispatch");
      }
      try {
        return handler.onChange(event, context);
      } finally {
        if (timer != null) {
          Timing.stop(timer);
          Timing.exitScope();
        }
      }
    }

    @Override
    public void onChange(ChangeEvent event) {
      if (dispatch(event, event.getNativeEvent().getEventTarget().<Element>cast())) {
        event.stopPropagation();
      }
    }
  }

  private final DoubleClickHandlers doubleClickHandlers;
  private final ClickHandlers clickHandlers;
  private final MouseDownHandlers mouseDownHandlers;
  private final ChangeHandlers changeHandlers;

  EventDispatcherPanel(Element baseElement) {
    setElement(baseElement);

    // Must construct the handler collections after calling setElement().
    doubleClickHandlers = new DoubleClickHandlers();
    clickHandlers = new ClickHandlers();
    mouseDownHandlers = new MouseDownHandlers();
    changeHandlers = new ChangeHandlers();
  }

  /**
   * Creates an EventDispatcherPanel.
   */
  public static EventDispatcherPanel create() {
    return new EventDispatcherPanel(Document.get().createDivElement());
  }

  /**
   * Creates an EventDispatcherPanel on an existing element. If the element is
   * part of a larger GWT widget structure, consider see
   * {@link #inGwtContext(Element, LogicalPanel)}.
   *
   * @param element element to become the panel
   */
  public static EventDispatcherPanel of(Element element) {
    EventDispatcherPanel panel = new EventDispatcherPanel(element);
    RootPanel.detachOnWindowClose(panel);
    panel.onAttach();
    return panel;
  }

  /**
   * Creates an EventDispatcherPanel on an existing element in an existing GWT
   * widget structure.
   *
   * @param element element to be wrapped
   * @param container panel to adopt the widgetification of {@code element}
   */
  public static EventDispatcherPanel inGwtContext(Element element, LogicalPanel container) {
    Preconditions.checkArgument(container != null);
    EventDispatcherPanel panel = new EventDispatcherPanel(element);
    container.doAdopt(panel);
    return panel;
  }

  @Override
  public void registerDoubleClickHandler(String kind, WaveDoubleClickHandler handler) {
    doubleClickHandlers.register(kind, handler);
  }

  @Override
  public void registerClickHandler(String kind, WaveClickHandler handler) {
    clickHandlers.register(kind, handler);
  }

  @Override
  public void registerMouseDownHandler(String kind, WaveMouseDownHandler handler) {
    mouseDownHandlers.register(kind, handler);
  }

  @Override
  public void registerChangeHandler(String kind, WaveChangeHandler handler) {
    changeHandlers.register(kind, handler);
  }

  @Override
  public void doAdopt(Widget child) {
    getChildren().add(child);
    adopt(child);
  }

  @Override
  public void doOrphan(Widget child) {
    orphan(child);
    getChildren().remove(child);
  }
}
