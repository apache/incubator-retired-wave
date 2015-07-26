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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.AnnotationMutationHandler;

import java.util.Iterator;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class AnnotationRegistryTest extends TestCase {
  private static class Dummy implements AnnotationMutationHandler {
    public <N, E extends N, T extends N> void handleAnnotationChange(
        DocumentContext<N, E, T> bundle, int start, int end, String key, Object newValue) {
      // no-op
    }
  }
  private final Dummy a = new Dummy(), b = new Dummy(), c = new Dummy();
  private final Dummy d = new Dummy(), e = new Dummy(), f = new Dummy();

  private final AnnotationRegistryImpl registry =
      new AnnotationRegistryImpl();

  public void testValidatesPrefix() {
    assertInvalidPrefix("/asdf");
    assertInvalidPrefix("asdf/");
    assertInvalidPrefix(Annotations.TRANSIENT + "asdf");
    assertInvalidPrefix("");

    // now make sure these don't throw exceptions
    registry.registerHandler("a", a);
    registry.registerHandler("asdf", a);
    registry.registerHandler("asdf/def", a);
    registry.registerHandler("asdf/asdf/wef", a);
    registry.registerHandler("asdf/234/df83/d", a);
    registry.registerHandler(Annotations.LOCAL + "a", a);
  }

  public void testMatchesOnPrefixReturnedInPreOrder() {
    // random order, increases chance of catching bug
    registry.registerHandler("a/b/c", c);
    registry.registerHandler("a", a);
    registry.registerHandler("a/b/c/d", d);
    registry.registerHandler("a/b", b);

    checkIterator(registry.getHandlers("a/b/c/d"), a, b, c, d);
  }

  public void testSimultaneousIteratorsWork() {
    // random order, increases chance of catching bug
    registry.registerHandler("a/b/c", c);
    registry.registerHandler("a", a);
    registry.registerHandler("a/b/c/d", d);
    registry.registerHandler("a/b", b);

    Iterator<AnnotationMutationHandler> it1 = registry.getHandlers("a/b/c/d");
    Iterator<AnnotationMutationHandler> it2 = registry.getHandlers("a/b/c/d");
    assertSame(a, it1.next());
    assertSame(b, it1.next());
    assertSame(a, it2.next());
    assertSame(c, it1.next());
    assertSame(b, it2.next());
    assertSame(c, it2.next());
    assertSame(d, it2.next());
    assertFalse(it2.hasNext());
    assertSame(d, it1.next());
    assertFalse(it1.hasNext());

  }

  public void testMatchesOnlyOnPrefix() {
    registry.registerHandler("a", a);
    registry.registerHandler("a/b", b);
    registry.registerHandler("a/b/c", c);
    registry.registerHandler("a/b/d", d);
    registry.registerHandler("a/bb/d", d);
    registry.registerHandler("a/x/e", e);
    registry.registerHandler("a/x/f", f);
    registry.registerHandler("x/y/b", b);

    checkIterator(registry.getHandlers("blah"));
    checkIterator(registry.getHandlers("blah/bleh"));
    checkIterator(registry.getHandlers("a"), a);
    checkIterator(registry.getHandlers("aa"));
    checkIterator(registry.getHandlers("a/b/c/x"), a, b, c);
    checkIterator(registry.getHandlers("a/b"), a, b);
    checkIterator(registry.getHandlers("a/b/d"), a, b, d);
    checkIterator(registry.getHandlers("a/bb/d"), a, d);
    checkIterator(registry.getHandlers("a/x/f/y"), a, f);
    checkIterator(registry.getHandlers("x/y/bb"));
    checkIterator(registry.getHandlers("x/y/b"), b);
  }

  public void testReRegisteringOverrides() {
    registry.registerHandler("a", a);
    registry.registerHandler("a/b", b);
    registry.registerHandler("a/b/c", c);
    AnnotationRegistryImpl child = registry.createExtension();
    child.registerHandler("a/b/d", d);
    checkIterator(child.getHandlers("a/b/c"), a, b, c);
    checkIterator(child.getHandlers("a/b/d"), a, b, d);
    child.registerHandler("a/b", e);
    checkIterator(child.getHandlers("a/b/c"), a, e, c);
    checkIterator(child.getHandlers("a/b/d"), a, e, d);
  }

  private void assertInvalidPrefix(String prefix) {
    try {
      registry.registerHandler(prefix, a);
      fail("Did not throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }
  }

  private void checkIterator(Iterator<AnnotationMutationHandler> it, Dummy ... dummies) {
    for (int i = 0; i < dummies.length; i++) {
      assertSame("Fail on dummy " + i, dummies[i], it.next());
    }
    assertFalse(it.hasNext());
  }
}
