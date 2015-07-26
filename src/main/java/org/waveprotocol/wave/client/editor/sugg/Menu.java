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

package org.waveprotocol.wave.client.editor.sugg;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;

/**
 * Abstract interface for interacting with a Menu.
 * Useful e.g. for when a menu is needed but we do not want to
 * subclass MenuBar directly.
 *
 * Add methods as needed...
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Menu {

  /**
   * Add an item to the menu.
   *
   * @param title Title of the item (which will be interpreted as plain text and
   *        HTML-escaped if necessary)
   * @param callback Code to execute when the item is selected
   * @return A MenuItem representing the added item
   */
  MenuItem addItem(String title, Command callback);

  /**
   * Add an item to the menu.
   *
   * @param title Title of the item, as a {@link SafeHtml}
   * @param callback Code to execute when the item is selected
   * @return A MenuItem representing the added item
   */
  MenuItem addItem(SafeHtml title, Command callback);
}
