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

package org.waveprotocol.wave.client.editor.keys;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.editor.EditorAction;
import org.waveprotocol.wave.client.editor.EditorContext;

import junit.framework.TestCase;

/**
 * Small tests for testing that a single key binding registry works in isolation.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class KeyBindingRegistryTest extends TestCase {
  /** Test that everything is clear after construction and in the NONE case. */
  public void testEmptyCase() {
    // Check that key combinations are not bound.
    assertEquals(0, new KeyBindingRegistry().getBoundKeyCombos().size());
    assertEquals(0, KeyBindingRegistry.NONE.getBoundKeyCombos().size());
  }

  /** Ensure correct action after a combination is registered. */

  public void testAddKeyCombo() {
    KeyCombo combo = KeyCombo.CTRL_A;
    EditorActionTracker action = new EditorActionTracker();

    KeyBindingRegistry registry = new KeyBindingRegistry();
    registry.registerAction(combo, action);

    assertTrue(registry.hasAction(combo));
    assertTrue(registry.getBoundKeyCombos().contains(combo));
    assertEquals(registry.getAction(combo), action);
    action.assertExecuteCount(0);
  }

  /** Ensure that getting an unknown combination produces null actions. */

  public void testUnknownComboAndRemove() {
    KeyCombo combo = KeyCombo.CTRL_ENTER;
    KeyBindingRegistry registry = new KeyBindingRegistry();

    assertFalse(registry.hasAction(combo));
    assertFalse(registry.getBoundKeyCombos().contains(combo));
    assertNull(registry.getAction(combo));

    // check adding and removing is still unknown.
    registry.registerAction(combo, new EditorActionTracker());
    registry.removeAction(combo);

    assertFalse(registry.hasAction(combo));
    assertFalse(registry.getBoundKeyCombos().contains(combo));
    assertNull(registry.getAction(combo));
  }

  /** Ensure that reassigning a combination means that new calls are to the new action. */

  public void testReregisterOverwrites() {
    KeyCombo combo = KeyCombo.BACKSPACE;
    KeyBindingRegistry registry = new KeyBindingRegistry();

    EditorActionTracker action1 = new EditorActionTracker();
    EditorActionTracker action2 = new EditorActionTracker();

    // should start out uncalled.
    action1.assertExecuteCount(0);
    action2.assertExecuteCount(0);

    // bind to action 1 and execute.
    registry.registerAction(combo, action1);
    registry.getAction(combo).execute(null);

    // only the first should be called
    action1.assertExecuteCount(1);
    action2.assertExecuteCount(0);

    // rebind to action 2 and execute.
    registry.registerAction(combo, action2);
    registry.getAction(combo).execute(null);

    // only the second should be called since last check.
    action1.assertExecuteCount(1);
    action2.assertExecuteCount(1);
  }

  /** Make sure that clearing combos actually deregisters them. */
  public void testClearRegistry() {
    KeyCombo combo = KeyCombo.BACKSPACE;
    KeyBindingRegistry registry = new KeyBindingRegistry();
    EditorActionTracker action = new EditorActionTracker();

    registry.registerAction(combo, action);
    assertTrue(registry.hasAction(combo));
    assertTrue(registry.getBoundKeyCombos().contains(combo));

    registry.clear();
    assertFalse(registry.hasAction(combo));
    assertFalse(registry.getBoundKeyCombos().contains(combo));
    assertNull(registry.getAction(combo));
  }

  /** Utility action that checks to see how often it is called. */
  public static class EditorActionTracker implements EditorAction {
    int executeCount = 0;

    @Override
    public void execute(EditorContext context) {
      executeCount ++;
    }

    public void assertExecuteCount(int count) {
      KeyBindingRegistryTest.assertEquals(count, executeCount);
    }
  }
}
