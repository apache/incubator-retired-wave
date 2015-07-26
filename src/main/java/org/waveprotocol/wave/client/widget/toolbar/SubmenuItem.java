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

package org.waveprotocol.wave.client.widget.toolbar;

import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;

/**
 * An item in a submenu, which notifies its "parent" of actions (i.e. a
 * propagated click) and changes to state (i.e. enabled / disabled / made
 * invisible).
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface SubmenuItem {
  /**
   * A parent of the item; notified of child events.
   */
  interface Parent {
    /**
     * Notifies the parent that an action was performed.
     */
    void onActionPerformed();

    /**
     * Notifies parent that one of its child items has changed its property.
     */
    void onChildStateChanged(SubmenuItem item, ToolbarButtonView.State newState);
  }

  /**
   * Sets the parent of this hierarchical item.
   */
  void setParent(Parent parent);

  /**
   * Clears the parent of this hierarchical item.
   */
  void clearParent();
}
