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

package org.waveprotocol.wave.model.adt;

import junit.framework.TestCase;

/**
 * Tests for the singleton interface.
 *
 * @author anorth@google.com (Alex North)
 */
public abstract class ObservableSingletonTestBase extends TestCase {

  /**
   * A mock singleton listener.
   */
  protected static class Listener implements ObservableSingleton.Listener<Integer> {
    boolean valueChanged = false;
    Integer oldValue = null;
    Integer newValue = null;

    public Listener() {
    }

    @Override
    public void onValueChanged(Integer oldValue, Integer newValue) {
      assertFalse("Unexpected value changed event", this.valueChanged);
      this.valueChanged = true;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    public void verifyValueChanged(Integer oldValue, Integer newValue) {
      assertTrue(valueChanged);
      assertEquals(oldValue, this.oldValue);
      assertEquals(newValue, this.newValue);
      reset();
    }

    public void verifyNoEvent() {
      assertFalse(valueChanged);
    }

    private void reset() {
      valueChanged = false;
      oldValue = null;
      newValue = null;
    }
  }

  /**
   * Creates an empty singleton.
   */
  protected abstract ObservableSingleton<Integer, String> createSingleton();

  private ObservableSingleton<Integer, String> target;
  private Listener listener;

  @Override
  public void setUp() {
    target = createSingleton();
    listener = new Listener();
  }

  public void testEmptySingletonHasNoValue() {
    assertNoValue(target);
  }

  public void testSetSetsValue() {
    target.set("42");
    assertValue(target, 42);
  }

  public void testSetReplacesValue() {
    target.set("42");
    target.set("43");
    assertValue(target, 43);
  }

  public void testClearRemovesValue() {
    target.set("42");
    target.clear();
    assertNoValue(target);
  }

  public void testSetAfterClearSetsValue() {
    target.set("42");
    target.clear();
    target.set("43");
    assertValue(target, 43);
  }

  // Events.

  public void testClearEmptyGeneratesNoEvent() {
    target.addListener(listener);
    target.clear();
    listener.verifyNoEvent();
  }

  public void testSetInitialValueGeneratesEvent() {
    target.addListener(listener);
    target.set("42");
    listener.verifyValueChanged(null, 42);
  }

  public void testReplaceValueGeneratesEvent() {
    target.set("42");
    target.addListener(listener);
    target.set("43");
    listener.verifyValueChanged(42, 43);
  }

  public void testSetSameValueGeneratesNoEvent() {
    target.set("42");
    target.addListener(listener);
    target.set("42");
    listener.verifyNoEvent();
  }

  public void testClearGeneratesEvent() {
    target.set("42");
    target.addListener(listener);
    target.clear();
    listener.verifyValueChanged(42, null);
  }

  protected static void assertNoValue(Singleton<Integer, String> target) {
    assertFalse(target.hasValue());
    assertNull(target.get());
  }

  protected static void assertValue(Singleton<Integer, String> target, int expected) {
    assertTrue(target.hasValue());
    assertEquals(new Integer(expected), target.get());
  }
}
