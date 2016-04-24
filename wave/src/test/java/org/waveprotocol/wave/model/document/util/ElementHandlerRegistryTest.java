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

package org.waveprotocol.wave.model.document.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry.HasHandlers;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.Renderer;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class ElementHandlerRegistryTest extends TestCase {

  private final Renderer a = mock(Renderer.class);
  private final Renderer b = mock(Renderer.class);
  private final Renderer c = mock(Renderer.class);
  private final Renderer d = mock(Renderer.class);

  private final NodeEventHandler e = mock(NodeEventHandler.class);
  private final NodeEventHandler f = mock(NodeEventHandler.class);
  private final NodeEventHandler g = mock(NodeEventHandler.class);
  private final NodeEventHandler h = mock(NodeEventHandler.class);

  private final HasHandlers el1 = mock(HasHandlers.class);
  private final HasHandlers el2 = mock(HasHandlers.class);
  private final HasHandlers el3 = mock(HasHandlers.class);
  {
    when(el1.getTagName()).thenReturn("x");
    when(el2.getTagName()).thenReturn("y");
    when(el3.getTagName()).thenReturn("z");
  }


  public void testRegister() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.registerRenderer("x", a);
    r1.registerEventHandler("y", f);
    assertSame(a, r1.getRenderer(el1));
    assertSame(f, r1.getEventHandler(el2));

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r1.registerEventHandler("y", h);
    assertSame(h, r1.getEventHandler(el2));

    // Check overriding in a child registry
    r2.registerRenderer("x", b);
    r2.registerEventHandler("y", g);
    assertSame(b, r2.getRenderer(el1));
    assertSame(g, r2.getEventHandler(el2));

    // Check propagation
    r1.registerRenderer("z", c);
    assertSame(c, r2.getRenderer(el3));
  }

  public void testConcurrent() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();
    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the child registry with concurrent propagation
    r2.registerRenderer("x", a);
    r2.registerRenderer("x", b);
    r1.registerRenderer("x", c);
    r1.registerRenderer("x", d);
    assertSame(b, r2.getRenderer(el1));

  }

  public void testOverrideDifferentTypes() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.registerRenderer("x", a);
    r1.registerEventHandler("x", e);

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r2.registerEventHandler("x", h);
    assertSame(h, r2.getEventHandler(el1));
    assertSame(a, r2.getRenderer(el1));
  }

  public void testDoubleRegister() {
    RenderingMutationHandler rmh = mock(RenderingMutationHandler.class);
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();
    r1.registerRenderingMutationHandler("x", rmh);

    assertSame(rmh, r1.getRenderer(el1));
    assertSame(rmh, r1.getMutationHandler(el1));
  }
}
