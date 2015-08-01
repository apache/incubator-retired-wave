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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class PainterRegistryTest extends TestCase {

  private final PaintFunction p1 = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      throw new AssertionError("Not implemented");
    }
  };
  private final PaintFunction p2 = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      throw new AssertionError("Not implemented");
    }
  };
  private final PaintFunction p3 = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      throw new AssertionError("Not implemented");
    }
  };
  private final PaintFunction p4 = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {
      throw new AssertionError("Not implemented");
    }
  };
  private final BoundaryFunction b1 = new BoundaryFunction() {
    @Override
    public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
        N nodeAfter, Map<String, Object> before, Map<String, Object> after, boolean isEditing) {
      throw new AssertionError("Not implemented");
    }
  };

  /**
   * Miscellaneous tests that depend on the previous state for convenience
   */
  public void testMiscellaneous() {
    PainterRegistryImpl r = new PainterRegistryImpl("a", "b", new AnnotationPainter(null));

    assertTrue(r.getPaintFunctions().isEmpty());

    // Test we get back the function we registered
    r.registerPaintFunction(CollectionUtils.newStringSet("a"), p1);
    assertSame(p1, r.getPaintFunctions().iterator().next());

    PainterRegistryImpl r2 = (PainterRegistryImpl) r.createExtension();

    // Test the extension contains the function
    assertSame(p1, r2.getPaintFunctions().iterator().next());
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    // Test propagation occurs
    r.registerPaintFunction(CollectionUtils.newStringSet("b"), p2);
    assertFalse(r.debugGetVersion() == r2.debugGetKnownParentVersion());
    assertTrue(r2.getPaintFunctions().contains(p2));
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    PainterRegistryImpl r3 = (PainterRegistryImpl) r2.createExtension();
    // ensure cache is filled
    r3.getPaintFunctions();

    // Test propagation occurs two levels deep after caching
    r.registerPaintFunction(CollectionUtils.newStringSet("c"), p3);
    assertFalse(r.debugGetVersion() == r2.debugGetKnownParentVersion());
    assertTrue(r2.getPaintFunctions().contains(p3));
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    assertFalse(r2.debugGetVersion() == r3.debugGetKnownParentVersion());
    assertTrue(r3.getPaintFunctions().contains(p3));
    assertEquals(r2.debugGetVersion(), r3.debugGetKnownParentVersion());
  }

  public void testPropagatesDeeply() {
    PainterRegistryImpl r1 = new PainterRegistryImpl("a", "b", new AnnotationPainter(null));
    PainterRegistryImpl r2 = (PainterRegistryImpl) r1.createExtension();
    PainterRegistryImpl r3 = (PainterRegistryImpl) r2.createExtension();

    r1.registerBoundaryFunction(CollectionUtils.newStringSet("a"), b1);
    assertTrue(r3.getBoundaryFunctions().contains(b1));

    r1.registerPaintFunction(CollectionUtils.newStringSet("b"), p1);
    assertTrue(r3.getPaintFunctions().contains(p1));
  }

  public void testInitialState() {
    PainterRegistryImpl r = new PainterRegistryImpl("a", "b", new AnnotationPainter(null));

    PainterRegistryImpl r2 = (PainterRegistryImpl) r.createExtension();
    r2.registerPaintFunction(CollectionUtils.newStringSet("b"), p2);
    assertTrue(r2.getPaintFunctions().contains(p2));
    assertTrue(r2.getKeys().contains("b"));

    r2.registerPaintFunction(CollectionUtils.newStringSet("d"), p4);
    assertTrue(r2.getPaintFunctions().contains(p4));
    assertTrue(r2.getKeys().contains("d"));
    r2.registerBoundaryFunction(CollectionUtils.newStringSet("e"), b1);
    assertTrue(r2.getBoundaryFunctions().contains(b1));
    assertTrue(r2.getKeys().contains("e"));

  }
}
