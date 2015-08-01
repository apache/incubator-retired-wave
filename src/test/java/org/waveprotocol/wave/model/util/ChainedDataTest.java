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

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class ChainedDataTest extends TestCase {

  private final Object p1 = "a";
  private final Object p2 = "b";
  private final Object p3 = "c";
  private final Object p4 = "d";

  /**
   * Miscellaneous tests that depend on the previous state for convenience
   */
  public void testMiscellaneous() {
    ChainedHashSet<Object> r = new ChainedHashSet<Object>();

    assertTrue(r.inspect().isEmpty());

    // Test we get back the paint function we registered
    r.modify().add(p1);
    assertSame(p1, r.inspect().iterator().next());

    ChainedHashSet<Object> r2 = r.createExtension();

    // Test the extension contains the function
    assertSame(p1, r2.inspect().iterator().next());
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    // Test propagation occurs
    r.modify().add(p2);
    assertFalse(r.debugGetVersion() == r2.debugGetKnownParentVersion());
    assertTrue(r2.inspect().contains(p2));
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    ChainedHashSet<Object> r3 = r2.createExtension();
    // ensure cache is filled
    r3.inspect();

    // Test propagation occurs two levels deep after caching
    r.modify().add(p3);
    assertFalse(r.debugGetVersion() == r2.debugGetKnownParentVersion());
    assertTrue(r2.inspect().contains(p3));
    assertEquals(r.debugGetVersion(), r2.debugGetKnownParentVersion());

    assertFalse(r2.debugGetVersion() == r3.debugGetKnownParentVersion());
    assertTrue(r3.inspect().contains(p3));
    assertEquals(r2.debugGetVersion(), r3.debugGetKnownParentVersion());
  }

  public void testPropagatesDeeply() {
    ChainedHashSet<Object> r1 = new ChainedHashSet<Object>();
    ChainedHashSet<Object> r2 = r1.createExtension();
    ChainedHashSet<Object> r3 = r2.createExtension();

    r1.modify().add(p1);
    assertTrue(r3.inspect().contains(p1));

    r1.modify().add(p2);
    assertTrue(r3.inspect().contains(p2));
  }

  public void testInitialState() {
    ChainedHashSet<Object> r = new ChainedHashSet<Object>();

    ChainedHashSet<Object> r2 = r.createExtension();
    r2.modify().add(p2);
    assertTrue(r2.inspect().contains(p2));

    r2.modify().add(p4);
    assertTrue(r2.inspect().contains(p4));
    r2.modify().add(p3);
    assertTrue(r2.inspect().contains(p3));
  }

  public void testFreezing() {
    ChainedHashSet<Object> r1 = ChainedHashSet.emptyRoot().createExtension();
    ChainedHashSet<Object> r2 = r1.createExtension();

    r2.freeze();
    r1.modify().add(p1);
    r2.modify().add(p2);
    assertTrue(r2.inspect().isEmpty());
    r2.unfreeze();
    assertTrue(r2.inspect().contains(p1));
    assertTrue(r2.inspect().contains(p2));

    r1.freeze();
    r1.modify().add(p3);
    assertFalse(r2.inspect().contains(p3));
    r1.unfreeze();
    assertTrue(r2.inspect().contains(p3));
  }

  public void testNotModifiableWhereSupported() {
    ChainedHashSet<Object> r1 = ChainedHashSet.emptyRoot().createExtension();
    try {
      r1.inspect().add(p1);
      fail("Cannot modify inspection version");
    } catch (UnsupportedOperationException e) {
      // OK
    }
  }
}
