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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Panel;

/**
 * Creates popups by deferring to a late-bound provider which may
 * change depending on platform.
 *
 */
public class PopupFactory {

  private static PopupProvider provider;

  // Private constructor to prevents instantiation
  private PopupFactory() {
  }

  /**
   * Returns the singleton popup provider.
   */
  public static PopupProvider getProvider() {
    if (provider == null) {
      provider = GWT.create(PopupProvider.class);
    }
    return provider;
  }

  /**
   * Create a popup panel
   *
   * @param relative   Optional reference element to pass to positioner.
   * @param positioner The positioner to use when this popup is shown.
   *        May be ignored on some platforms.
   * @param chrome The widget providing chrome for this popup.
   * @param autoHide If true, clicking outside the popup will cause the popup to hide itself.
   */
  public static UniversalPopup createPopup(Element relative, RelativePopupPositioner positioner,
      PopupChrome chrome, boolean autoHide) {
    return getProvider().createPopup(relative, positioner, chrome, autoHide);
  }

  /**
   * Creates popups suitable for current platform.
   */
  public static void setRootPanel(Panel rootPanel) {
    getProvider().setRootPanel(rootPanel);
  }
}
