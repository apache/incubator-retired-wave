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

package org.waveprotocol.wave.client.widget.button;

/**
 * Simplified (compared to the GWT version) listener interface for mouse events -
 * this interface doesn't depend on GWT.
 *
 */
public interface MouseListener {
  /**
   * A {@link MouseListener} that does nothing when it receives events.
   */
  public static final MouseListener NO_OP = new MouseListener() {
    @Override
    public void onMouseUp() { }

    @Override
    public void onMouseLeave() { }

    @Override
    public void onMouseEnter() { }

    @Override
    public void onMouseDown() { }

    @Override
    public void onClick() { }
  };

  /**
   * Called when the mouse is pressed over some widget.
   */
  void onMouseDown();

  /**
   * Called when the mouse enters some widget.
   */
  void onMouseEnter();

  /**
   * Called when the mouse leaves some widget.
   */
  void onMouseLeave();

  /**
   * Called when the mouse is released over some widget.
   */
  void onMouseUp();

  /**
   * Called when the mouse is clicked.
   */
  void onClick();
}
