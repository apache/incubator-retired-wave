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
 * A view of a submenu toolbar; the union of a toolbar (for adding buttons) and
 * a button display (for being added to a toolbar).
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface SubmenuToolbarView extends ToolbarView, ToolbarButtonView {
  /**
   * A listener to submenu show/hide events so that users of the submenu can
   * dynamically generate content when shown.
   */
  interface Listener {
    /**
     * Called when the submenu is shown.
     */
    void onSubmenuShown();

    /**
     * Called when the submenu is hidden.
     */
    void onSubmenuHidden();
  }

  /**
   * Sets the listener to submenu show/hide events.
   */
  void setListener(Listener listener);
}
