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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Displays popups on a mobile client.
 *
 *
 * TODO(user) rename this class SmallScreenPopupProvider
 */
public class MobilePopupProvider implements PopupProvider {
  /** The panel to which popups are added */
  private Panel root = RootPanel.get();

  /**
   * {@inheritDoc}
   */
  public void setRootPanel(Panel root) {
    this.root = root;
  }

  /**
   * {@inheritDoc}
   *
   * @param positioner Ignored on mobile.
   * @param chrome Ignored on mobile.
   * @param autoHide Ignored on mobile.
   */
  public UniversalPopup createPopup(Element reference, RelativePopupPositioner positioner,
      PopupChrome chrome, boolean autoHide) {
    return new MobileUniversalPopup(root);
  }
}
