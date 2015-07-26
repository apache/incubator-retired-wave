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

import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;

/**
 * View interface for the presentation of a toolbar.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface ToolbarView {
  /**
   * Creates a new click button and adds it to the toolbar.
   *
   * @return The new button.
   */
  ToolbarClickButton addClickButton();

  /**
   * Creates a new toggle button and adds it to the toolbar.
   *
   * @return The new toggle button.
   */
  ToolbarToggleButton addToggleButton();

  /**
   * Creates a new empty submenu and adds it to the toolbar.
   *
   * @return The new submenu.
   */
  SubmenuToolbarView addSubmenu();

  /**
   * Creates a new group within this toolbar.
   *
   * @return The new button group.
   */
  ToolbarView addGroup();

  /**
   * Clears all buttons, submenus, and groups.
   */
  void clearItems();
}
