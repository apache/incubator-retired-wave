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

package org.waveprotocol.wave.client.wavepanel.impl.edit;

import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;

/**
 * Curries the actions from {@link Actions} with blip/thread context from the
 * focus frame.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class FocusedActions {
  private final FocusFramePresenter focus;
  private final Actions actions;

  /**
   * Implements the wave panel's editing UI actions.
   *
   * @param focus focus-frame feature
   * @param actions context-free actions
   */
  public FocusedActions(FocusFramePresenter focus, Actions actions) {
    this.focus = focus;
    this.actions = actions;
  }

  void startEditing() {
    BlipView blipUi = getBlipContext();
    if (blipUi != null) {
      actions.startEditing(blipUi);
    }
  }

  void reply() {
    BlipView blipUi = getBlipContext();
    if (blipUi != null) {
      actions.reply(blipUi);
    }
  }

  void addContinuation() {
    ThreadView threadUi = getThreadContext();
    if (threadUi != null) {
      actions.addContinuation(threadUi);
    }
  }

  void deleteBlip() {
    BlipView blipUi = getBlipContext();
    if (blipUi != null) {
      actions.delete(blipUi);
    }
  }

  void deleteThread() {
    ThreadView threadUi = getThreadContext();
    if (threadUi != null) {
      actions.delete(threadUi);
    }
  }

  private BlipView getBlipContext() {
    return focus.getFocusedBlip();
  }

  private ThreadView getThreadContext() {
    return parentOf(getBlipContext());
  }

  private static ThreadView parentOf(BlipView blip) {
    return blip != null ? blip.getParent() : null;
  }
}
