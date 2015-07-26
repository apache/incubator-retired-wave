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

package org.waveprotocol.wave.client.gadget.renderer;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Interface for objects that receive Gadget RPC calls.
 *
 */
public interface GadgetRpcListener {
  /**
   * Sets Gadget title.
   *
   * @param title the title.
   */
  public void setTitle(String title);

  /**
   * Sets Gadget user preferences.
   *
   * @param keyValue a sequence of key-value preference pairs.
   */
  public void setPrefs(String ... keyValue);

  /**
   * Sets the Gadget's IFrame height.
   *
   * @param height the height.
   */
  public void setIframeHeight(String height);

  /**
   * Sets the Gadget's IFrame width.
   *
   * @param width the width.
   */
  public void setIframeWidth(String width);

  /**
   * Navigate to a given site. TODO(user): This is a placeholder: update
   * implementation.
   *
   * @param url destination URL.
   */
  public void requestNavigateTo(String url);

  /**
   * Updates the shared Podium state.
   *
   * @param state serialized Podium state.
   */
  public void updatePodiumState(String state);

  /**
   * Informs the gadget container that the gadget is wave-enabled and requests
   * the container to send wave-specific initialization.
   *
   * @param waveApiVersion the version of the Wave API that the gadget is using.
   */
  public void waveEnable(String waveApiVersion);

  /**
   * Informs the gadget container about the gadget-initiated state change. The
   * state parameter is in the form of delta that contains only the key-value
   * pairs for the values that should be updated. The container has to apply the
   * delta to the current state and send a new updated state back to the gadget.
   * The gadget's internal state will be updated and rendered only after the
   * container sends the new state to the gadget.
   *
   * @param delta a set of key-value pairs to modify the state.
   */
  public void waveGadgetStateUpdate(JavaScriptObject delta);

  /**
   * Similar to waveGadgetStateUpdate, but for per-user state.
   *
   * @param delta a set of key-value pairs to modify the state.
   */
  public void wavePrivateGadgetStateUpdate(JavaScriptObject delta);

  /**
   * Requests to output a log message.
   *
   * @param message log message to output.
   */
  public void logMessage(String message);

  /**
   * Sets a snippet visible in the wave digest.
   *
   * @param snippet text to be added as a snippet to wave digest.
   */
  public void setSnippet(String snippet);
}
