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

package org.waveprotocol.box.webclient.widget.error;

import org.waveprotocol.wave.client.common.safehtml.SafeHtml;


/**
 * UI for an error indicator.
 */
interface ErrorIndicatorView {

  /**
   * Listener interface for UI gestures.
   */
  interface Listener {
    void onShowDetailClicked();
  }

  /**
   * Binds this view to a listener.
   */
  void init(Listener listener);

  /**
   * Releases this view from its listener.
   */
  void reset();

  /**
   * Sets the rendering of the stack trace.
   */
  void setStack(SafeHtml stack);

  /**
   * Sets the rendering of a known issue.
   */
  void setBug(SafeHtml bug);

  /**
   * Shows the 'show detail' link.
   */
  void showDetailLink();

  /**
   * Hides the 'show detail' link.
   */
  void hideDetailLink();

  /**
   * Expands the detail box.
   */
  void expandDetailBox();

  /**
   * Collapses the detail box.
   */
  void collapseDetailBox();
}
