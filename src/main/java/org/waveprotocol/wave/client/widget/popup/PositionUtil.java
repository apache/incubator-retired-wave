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

import com.google.gwt.user.client.Window;

/**
 * Utility class to bound a popup's co-ordinates so that they fit within the window.
 *
 */
public class PositionUtil {
  private static int SCREEN_EDGE_PADDING_PIXELS = 16;

  /**
   * Restrict a popup's position to stay within the screen horizontally.
   *
   * @param x The proposed x position of the popup.
   * @param width The width of the popup.
   * @return The s position at which the popup should appear.
   */
  public static int boundToScreenHorizontal(int x, int width) {
    return Math.min(Math.max(x, 0), Window.getClientWidth() - width - SCREEN_EDGE_PADDING_PIXELS);
  }

  /**
   * Restrict a popup's position to stay within the screen vertically.
   *
   * @param y The proposed y position of the popup.
   * @param height The height of the popup.
   * @return The y position at which the popup should appear.
   */
  public static int boundToScreenVertical(int y, int height) {
    return Math.min(Math.max(y, 0), Window.getClientHeight() - height - SCREEN_EDGE_PADDING_PIXELS);
  }

  /**
   * Restricts a popup's height to fit within the screen.
   *
   * @param height The proposed height of the popup.
   * @return The height that the popup need to have to fit inside the screen.
   */
  public static int boundHeightToScreen(int height) {
    return Math.min(height, Window.getClientHeight() - 2 * SCREEN_EDGE_PADDING_PIXELS);
  }
}
