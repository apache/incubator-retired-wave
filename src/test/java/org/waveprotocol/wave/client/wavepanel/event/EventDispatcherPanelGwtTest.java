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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.wavepanel.event.EventDispatcherPanel.HandlerCollection;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link EventDispatcherPanel}.
 *
 */

@SuppressWarnings("unchecked") // Arrays.asList() of generics.
public class EventDispatcherPanelGwtTest extends GWTTestCase {

  // Mock event type
  static class MyEvent {
  }

  // Mock handler type
  static class MyHandler {
  }

  /**
   * Mock handler collection that records handler invocations, and can also be
   * programmed with behavior.
   */
  static class MockHandlers extends HandlerCollection<MyEvent, MyHandler> {
    private final List<Pair<MyHandler, Element>> invoked =
        new ArrayList<Pair<MyHandler, Element>>();
    private final Set<MyHandler> propagationStoppers = new HashSet<MyHandler>();

    MockHandlers(Element top) {
      super(top, "myevent");
    }

    @Override
    void registerGwtHandler() {
      // Do nothing.
    }

    @Override
    boolean dispatch(MyEvent event, Element context, MyHandler handler) {
      invoked.add(Pair.of(handler, context));
      return propagationStoppers.contains(handler);
    }

    /** Makes a handler stop the propagation of an event. */
    void stopOn(MyHandler handler) {
      propagationStoppers.add(handler);
    }

    /** @return the handler invocations that occurred during dispatch. */
    List<Pair<MyHandler, Element>> getInvoked() {
      return invoked;
    }
  }

  // Some elements in the sample DOM.
  private Element top;
  private Element foo;
  private Element bar;

  // Sample event handlers.
  private MyHandler fooHandler;
  private MyHandler barHandler;

  /** Event handler collection being tested. */
  private MockHandlers handlers;

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.wavepanel.event.Tests";
  }

  @Override
  protected void gwtSetUp() {
    SafeHtml dom = EscapeUtils.fromSafeConstant("" + // \u2620
        "<div id='base' kind='base'>" + // \u2620
        "  <div>" + // \u2620
        "    <div kind='foo' id='foo'>" + // \u2620
        "      <div kind='unused'>" + // \u2620
        "        <div kind='bar' id='bar'>" + // \u2620
        "          <div id='source'></div>" + // \u2620
        "        </div>" + // \u2620
        "      </div>" + // \u2620
        "    </div>" + // \u2620
        "  </div>" + // \u2620
        "</div>");

    top = load(dom);
    foo = Document.get().getElementById("foo");
    bar = Document.get().getElementById("bar");

    // Register some handlers.
    handlers = new MockHandlers(top);
    fooHandler = new MyHandler();
    barHandler = new MyHandler();
    handlers.register("foo", fooHandler);
    handlers.register("bar", barHandler);
  }

  @Override
  protected void gwtTearDown() {
    top.removeFromParent();
  }

  /** Injects some HTML into the DOM. */
  private static Element load(SafeHtml html) {
    Element container = Document.get().createDivElement();
    container.setInnerHTML(html.asString());
    Element content = container.getFirstChildElement();
    RootPanel.get().getElement().appendChild(content);
    return content;
  }

  /** Fires a fake browser event through the handler collection. */
  private void synthesizeEvent() {
    // Synthesize an event.
    Element target = Document.get().getElementById("source");
    handlers.dispatch(new MyEvent(), target);
  }

  public void testDispatchOrder() {
    synthesizeEvent();

    // Verify that both handlers were called in the right order.
    assertEquals(
        Arrays.asList(Pair.of(barHandler, bar), Pair.of(fooHandler, foo)), handlers.getInvoked());
  }

  public void testHandlingStopsBubbling() {
    // Make the barHandler stop bubbling.
    handlers.stopOn(barHandler);
    synthesizeEvent();

    // Verify that both handlers were called in the right order.
    assertEquals(Arrays.asList(Pair.of(barHandler, bar)), handlers.getInvoked());
  }

  public void testGlobalDispatch() {
    MyHandler globalHandler = new MyHandler();
    handlers.register(null, globalHandler);
    synthesizeEvent();

    // Verify that global handler is invoked last.
    List<?> invoked = handlers.getInvoked();
    assertEquals(Pair.of(globalHandler, top), invoked.get(invoked.size() - 1));
  }

  public void testGlobalAndTopKindDoNotInterfere() {
    MyHandler topHandler = new MyHandler();
    MyHandler globalHandler = new MyHandler();
    handlers.register("base", topHandler);
    handlers.register(null, globalHandler);
    synthesizeEvent();

    // Verify that top handler is the last invoked kind handler, then the global
    // handler.
    List<Pair<MyHandler, Element>> invoked = handlers.getInvoked();
    List<Pair<MyHandler, Element>> end = invoked.subList(invoked.size() - 2, invoked.size());
    assertEquals(Arrays.asList(Pair.of(topHandler, top), Pair.of(globalHandler, top)), end);
  }

  public void testKindHandlerStoppingBubblingAvoidsGlobalHandler() {
    MyHandler globalHandler = new MyHandler();
    handlers.register(null, globalHandler);
    handlers.stopOn(fooHandler);

    // Verify that global handlers is not invoked.
    assertFalse(handlers.getInvoked().contains(globalHandler));
  }

  public void testEventOnTopElementIsDispatched() {
    MyHandler topHandler = new MyHandler();
    handlers.register("base", topHandler);
    handlers.dispatch(new MyEvent(), top);

    assertEquals(Collections.singletonList(Pair.of(topHandler, top)), handlers.getInvoked());
  }

  public void testMultipleGlobalHandlersThrowsException() {
    handlers.register(null, new MyHandler());
    try {
      handlers.register(null, new MyHandler());
      fail();
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  public void testMultipleHandlersOfSameKindThrowsException() {
    try {
      handlers.register("foo", new MyHandler());
      fail();
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

}
