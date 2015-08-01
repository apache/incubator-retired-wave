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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.content.AgentAdapter;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentRawDocument;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler.Flow;
import org.waveprotocol.wave.client.editor.testing.TestEditors;

import java.util.Collections;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class GwtRenderingMutationHandlerGwtTest extends GWTTestCase {

  private ContentDocument doc;
  private ComplexPanel panel;
  private GwtRenderingMutationHandler handler;
  private ContentElement root;
  private ContentRawDocument rawDoc;

  public void testDeferredHandlerMustHaveFlow() {
    deferredHandler(Flow.INLINE);
    AgentAdapter e1 = elem("ab");

    deferredHandler(Flow.USE_WIDGET);
    try {
      e1 = elem("ab");
      fail("USE_WIDGET should not be permitted with a null initial domImpl");
    } catch (RuntimeException e) {
      // ok
    }
  }

  @SuppressWarnings("static")
  public void testRegistersContainerNodelet() {
    complexHandler(Flow.INLINE);

    AgentAdapter e1 = elem("ab");
    Widget a = handler.getGwtWidget(e1);
    com.google.gwt.user.client.Element b = a.getElement();
    Node c = b.getFirstChild();
    Element d = e1.getContainerNodelet();
    assertSame(c,
        d);

  }

  @SuppressWarnings("static")
  public void testReceivesNewWidgetSafely() {
    simpleHandler(Flow.INLINE);

    AgentAdapter e1 = elem("ab");
    assertEquals("ab", ((Label) handler.getGwtWidget(e1)).getText());
    assertNull(e1.getContainerNodelet());
    assertEquals(1, panel.getWidgetCount());
    assertSame(e1.getImplNodelet(), handler.getGwtWidget(e1).getElement().getParentElement());


    Label label2 = new Label("hi");
    handler.receiveNewGwtWidget(e1, label2);
    assertSame(label2, handler.getGwtWidget(e1));
    assertSame(e1.getImplNodelet(), label2.getElement().getParentElement());

    Label label3 = new Label("there");
    handler.receiveNewGwtWidget(e1, label3);
    assertSame(label3, handler.getGwtWidget(e1));
    assertSame(e1.getImplNodelet(), label3.getElement().getParentElement());
    assertEquals(1, panel.getWidgetCount());
    assertSame(panel, handler.getGwtWidget(e1).getParent());

    // logical detach automatically
    rawDoc.removeChild(root, e1);
    assertEquals(0, panel.getWidgetCount());
  }


  @SuppressWarnings("static")
  public void testReceivesNewWidgetWithContainerNodeletSafely() {
    complexHandler(Flow.INLINE);

    assertEquals(0, panel.getWidgetCount());
    AgentAdapter e1 = elem("ab");
    assertEquals(1, panel.getWidgetCount());
    assertSame(e1.getImplNodelet(), handler.getGwtWidget(e1).getElement().getParentElement());
    assertSame(handler.getGwtWidget(e1).getElement().getFirstChild(),
        e1.getContainerNodelet());

    assertSame(panel, handler.getGwtWidget(e1).getParent());

    Widget w3 = createComplexWidget();
    handler.receiveNewGwtWidget(e1, w3);
    assertSame(w3, handler.getGwtWidget(e1));
    assertSame(e1.getImplNodelet(), w3.getElement().getParentElement());
    assertSame(w3.getElement().getFirstChild(), e1.getContainerNodelet());
    assertEquals(1, panel.getWidgetCount());
    assertSame(panel, handler.getGwtWidget(e1).getParent());

    // logical detach automatically
    rawDoc.removeChild(root, e1);
    assertEquals(0, panel.getWidgetCount());
  }


  @SuppressWarnings("static")
  private void simpleHandler(final Flow flow) {
    handler = new GwtRenderingMutationHandler(flow) {
      @Override
      protected Widget createGwtWidget(Renderable element) {
        return new Label(element.getTagName());
      }
    };

    initDoc();
  }

  @SuppressWarnings("static")
  private void deferredHandler(final Flow flow) {
    handler = new GwtRenderingMutationHandler(flow) {
      @Override
      protected Widget createGwtWidget(Renderable element) {
        return null;
      }
    };

    initDoc();
  }

  @SuppressWarnings("static")
  private void complexHandler(final Flow flow) {
    handler = new GwtRenderingMutationHandler(flow) {
      @Override
      protected Widget createGwtWidget(Renderable element) {
        return createComplexWidget();
      }

      @Override
      protected Element getContainerNodelet(Widget w) {
        return w.getElement().getFirstChild().<Element>cast();
      }
    };

    initDoc();
  }

  @SuppressWarnings("static")
  private FlowPanel createComplexWidget() {
    FlowPanel panel = new FlowPanel();
    FlowPanel child = new FlowPanel();
    panel.add(child);
    return panel;
  }

  @SuppressWarnings("static")
  private void initDoc() {
    Editor.ROOT_HANDLER_REGISTRY.registerRenderingMutationHandler("ab", handler);
    Editor.ROOT_HANDLER_REGISTRY.registerRenderingMutationHandler("cd", handler);
    doc = TestEditors.createTestDocument();
    rawDoc = doc.debugGetRawDocument();
    root = rawDoc.getDocumentElement();
    panel = (EditorImpl) doc.getContext().editing().editorContext();
  }

  @SuppressWarnings("static")
  private AgentAdapter elem(String tagName) {
    return (AgentAdapter) doc.getAnnotatableContent().transparentCreate(tagName,
        Collections.<String, String>emptyMap(),
        doc.getAnnotatableContent().getDocumentElement(), null);
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.gwt.Tests";
  }
}
