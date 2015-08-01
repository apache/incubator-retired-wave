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

package org.waveprotocol.wave.client.editor.content.misc;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

import org.waveprotocol.wave.client.common.scrub.Scrub;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ClientDocumentContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.EventHandler;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.MutationHandler;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import java.util.HashSet;
import java.util.Set;

/**
 * Renderer for the bits of paint that spread over text
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
class AnnotationSpreadRenderer extends RenderingMutationHandler {

  private static final int NOTIFY_SCHEDULE_DELAY_MS = 200;

  private static final int MOUSE_LISTENER_EVENTS = Event.MOUSEEVENTS | Event.ONCLICK;

  private final Set<ContentElement> mutatedElements = new HashSet<ContentElement>();

  private final Task mutationNotificationTask = new Task() {
    @Override
    public void execute() {
      for (ContentElement element : mutatedElements) {
        ClientDocumentContext context = element.getContext();
        if (!context.editing().hasEditor()) {
          continue;
        }

        MutationHandler handler = getMutationHandler(element);
        if (handler != null) {
          handler.onMutation(element);
        }
      }
      mutatedElements.clear();
    }
  };

  private static MutationHandler getMutationHandler(ContentElement element) {
    String handlerId = element.getAttribute(AnnotationPaint.MUTATION_LISTENER_ATTR);
    return handlerId == null ? null : AnnotationPaint.mutationHandlerRegistry.get(handlerId);
  }

  @Override
  public void onActivationStart(ContentElement element) {
    fanoutAttrs(element);
  }

  @Override
  public void onAttributeModified(final ContentElement element, String name,
      String oldValue, final String newValue) {
    if (name.equals(AnnotationPaint.LINK_ATTR)) {
      // NOTE(user): This is a special case, because it replaces the DOM node,
      // we must reapply all the attributes.
      maybeConvertToAnchor(element, newValue != null);
      element.getAttributes().each(new ProcV<String>() {
        @Override
        public void apply(String key, String value) {
          applyAttribute(element, key, value);
        }
      });
    } else {
      applyAttribute(element, name, newValue);
    }
  }

  private void applyAttribute(ContentElement element, String name, String newValue) {
    // NOTE(user): If an link attribute is added, then handle specially,
    // otherwise treat as style attribute.
    Element implNodelet = element.getImplNodelet();
    if (name.equals(AnnotationPaint.LINK_ATTR)) {
      if (newValue != null) {
        String scrubbedValue = Scrub.scrub(newValue);
        implNodelet.setAttribute("href", scrubbedValue);
        if (scrubbedValue.startsWith("#")) {
          implNodelet.removeAttribute("target");
        } else {
          implNodelet.setAttribute("target", "_blank");
        }
      } else {
        implNodelet.removeAttribute("href");
      }
    } else if (name.equals(AnnotationPaint.MOUSE_LISTENER_ATTR)) {
      updateEventHandler(element, newValue);
    } else {
      try {
        implNodelet.getStyle().setProperty(name, newValue);
      } catch (RuntimeException e) {
        // NOTE(user): some property value are invalid, try catch them and ignores them.
        EditorStaticDeps.logger.error().log("Failed to set CSS property " + name +
          " -> " + newValue);
      }
    }
  }

  private void updateEventHandler(final ContentElement element, String eventHandlerId) {
    Element implNodelet = element.getImplNodelet();
    final EventHandler handler =
        eventHandlerId == null ? null : AnnotationPaint.eventHandlerRegistry.get(eventHandlerId);
    if (handler != null) {
      DOM.sinkEvents(DomHelper.castToOld(implNodelet), MOUSE_LISTENER_EVENTS);
      DOM.setEventListener(DomHelper.castToOld(implNodelet), new EventListener() {
        @Override
        public void onBrowserEvent(Event event) {
          handler.onEvent(element, event);
        }
      });
    } else {
      removeListener(DomHelper.castToOld(implNodelet));
    }
  }

  private static Element createHtml(boolean isAnchor) {
    Element e = isAnchor
        ? Document.get().createAnchorElement()
        : Document.get().createSpanElement();

    // Prevents some browsers (to my knowledge, currently just Webkit)
    // from removing empty elements from the dom too much
    if (UserAgent.isWebkit()) {
      e.setAttribute("x", "y");
    }

    return e;
  }

  /**
   * Switches the impl nodelet to and from an anchor element.
   *
   * This is to avoid using anchor elements unless we actually need to render a link. Links
   * generally have strange behaviours in various browsers, and need special (often inefficient)
   * code to deal with them, so the fewer the better.
   *
   * @param toAnchor if true, convert to an anchor, otherwise, convert to a span.
   */
  private void maybeConvertToAnchor(ContentElement element, boolean toAnchor) {
    Element nodelet = element.getImplNodelet();
    boolean isAnchor = nodelet.getTagName().equalsIgnoreCase("a");
    if (isAnchor != toAnchor) {
      removeListener(DomHelper.castToOld(nodelet));
      Element newNodelet = createHtml(toAnchor);

      DomHelper.replaceElement(nodelet, newNodelet);
      element.setBothNodelets(newNodelet);
    }
  }

  @Override
  public void onRemovedFromParent(ContentElement element, ContentElement newParent) {
    if (newParent != null) {
      return;
    }
    removeListener(DomHelper.castToOld(element.getImplNodelet()));
    super.onRemovedFromParent(element, newParent);
  }

  private void removeListener(com.google.gwt.user.client.Element implNodelet) {
    DOM.setEventListener(implNodelet, null);
    DOM.sinkEvents(implNodelet, DOM.getEventsSunk(implNodelet) & ~MOUSE_LISTENER_EVENTS);
  }

  @Override
  public Element createDomImpl(Renderable element) {
    return element.setAutoAppendContainer(createHtml(false));
  }

  private void scheduleMutationNotification(ContentElement element) {
    MutationHandler handler = getMutationHandler(element);
    if (handler != null) {
      mutatedElements.add(element);
    }

    Scheduler scheduler = SchedulerInstance.get();
    if (!scheduler.isScheduled(mutationNotificationTask)) {
      scheduler.scheduleDelayed(Priority.MEDIUM, mutationNotificationTask,
          NOTIFY_SCHEDULE_DELAY_MS);
    }
  }

  @Override
  public void onDescendantsMutated(ContentElement element) {
    scheduleMutationNotification(element);
  }
}
