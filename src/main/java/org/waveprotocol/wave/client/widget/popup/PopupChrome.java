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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.user.client.ui.Widget;

/**
 * The PopupChrome interface provides UniversalPopups with a way to manipulate
 * their appearance.
 *
 */
public interface PopupChrome {
  /**
   * Called when the UniversalPopup that is wrapped by this Chrome adds a title bar.
   */
  void enableTitleBar();

  /**
   * @return a widget that provides chrome to its parent widget.
   */
  Widget getChrome();
}
