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
 * Display that acts as a composite, wrapping multiple child displays,
 *   and keeping their states in sync.
 * @author patcoleman@google.com (Pat Coleman)
 */
public class CompositeButtonDisplay implements ButtonDisplay {
  /** The child displays this is a composite for */
  private final ButtonDisplay[] children;

  /**
   * Initialises the composite, taking all displays it is to sync together
   * @param displays Displays to sync
   */
  public CompositeButtonDisplay(ButtonDisplay... displays) {
    children = displays;
  }

  @Override
  public void setState(ButtonState state) {
    for (ButtonDisplay display : children) {
      display.setState(state);
    }
  }

  @Override
  public void setUiListener(MouseListener mouseListener) {
    for (ButtonDisplay display : children) {
      display.setUiListener(mouseListener);
    }
  }

  @Override
  public void setTooltip(String tooltip) {
    for (ButtonDisplay display : children) {
      display.setTooltip(tooltip);
    }
  }

  @Override
  public void setText(String text) {
    for (ButtonDisplay display : children) {
      display.setText(text);
    }
  }
}
