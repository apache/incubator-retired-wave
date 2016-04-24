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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry that allows actions to be bound to particular key combinations.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class KeyBindingRegistry {
  /** Set up a key binding set with nothing bound. */
  public static final KeyBindingRegistry NONE = new KeyBindingRegistry();

  /** Maps the key combinations to the action performed when pressed. */
  // TODO(patcoleman) - optimise to IdentityMap when testable
  private final Map<KeyCombo, EditorAction> bindings = new HashMap<KeyCombo, EditorAction>();

  /** Empty constructor, does nothing. */
  public KeyBindingRegistry() {
    // NO-OP
  }

  /** Check to see whether the registry knows about a particular combination. */
  public boolean hasAction(KeyCombo combo) {
    return bindings.containsKey(combo);
  }

  /** Retrieve the action bound to this combination. */
  public EditorAction getAction(KeyCombo combo) {
    return bindings.get(combo);
  }

  /** Retrieve the set of bound key combinations. */
  public Set<KeyCombo> getBoundKeyCombos() {
    return bindings.keySet();
  }

  /** Register an action to be bound to a given combination. */
  public void registerAction(KeyCombo combo, EditorAction action) {
    bindings.put(combo, action);
  }

  /** Unbind an action. */
  public void removeAction(KeyCombo combo) {
    bindings.remove(combo);
  }

  /** Unbind all actions. */
  public void clear() {
    bindings.clear();
  }
}
