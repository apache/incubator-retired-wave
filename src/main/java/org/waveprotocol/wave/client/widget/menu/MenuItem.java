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

package org.waveprotocol.wave.client.widget.menu;

/**
 * An item in a menu.
 *
 */
public interface MenuItem {
  /**
   * Sets the item to be enabled or disabled (greyed out).
   *
   * @param enabled Whether to set the item to be enabled or disabled.
   */
  void setEnabled(boolean enabled);

  /**
   * Sets the item's enabledness to the state it was in at creation.
   */
  void resetEnabled();

  /**
   * Sets whether this object is visible.
   *
   * @param visible {code true} to show the object, {@code false} to hide it
   */
  void setVisible(boolean visible);

  /**
   * Change the label.
   *
   * @param label {code String} label
   */
  void setText(String label);

  /**
   * Window to {@link com.google.gwt.user.client.ui.UIObject#setDebugClass(String)}.
   *
   * The existence of this method is required by an existing class.
   *
   * @param debugClass
   */
  // TODO(user): Rename back to setDebugClass once a final setDebugClass method no
  //               longer exists in UIObject, i.e. when new GWT jars are rolled.
  void setDebugClassTODORename(String debugClass);
}
