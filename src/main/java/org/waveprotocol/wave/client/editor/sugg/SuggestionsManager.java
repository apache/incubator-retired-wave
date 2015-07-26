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
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Interface for managing ContentElements with suggestion lists
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface SuggestionsManager {

  /**
   * Resets the manager, clearing all registered elements
   */
  public void clear();

  /**
   * Hides any visible suggestion menu. Implementations may choose not to
   * actually hide the menu, depending on other user actions
   *
   * @param callback Command to execute when the menu actually closes
   */
  public void hideSuggestions(Command callback);

  /**
   * Registers a ContentElement as having a suggestions menu (currently for suggestions)
   *
   * @param element The element which requires a suggestions menu
   */
  void registerElement(HasSuggestions element);

  /**
   * Show the suggestions popup for the given object
   * @param suggestable Object which has suggestions
   */
  public void showSuggestionsFor(HasSuggestions suggestable);

  /**
   * Given some point, show the nearest suggestions menu
   *
   * @param location
   * @return true if something was shown
   */
  boolean showSuggestionsNearestTo(Point<ContentNode> location);

  /**
   * Implemented by ContentElements that have a suggestions menu
   *
   * @author danilatos@google.com (Daniel Danilatos)
   */
  public interface HasSuggestions {

    /**
     * @return True if the suggestion should be accessible from keyboard
     *   hotkeys. False if it should be skipped over.
     */
    boolean isAccessibleFromKeyboard();

    /**
     * Called when the suggestions menu has been requested for this object, so the
     * object can populate the menu with items.
     *
     * @param menu
     */
    void populateSuggestionMenu(Menu menu);

    /**
     * Called when the menu is shown
     */
    void handleShowSuggestionMenu();

    /**
     * Called when the menu is closed
     */
    void handleHideSuggestionMenu();

    /**
     * @return The element upon which the suggestion should appear
     */
    ContentElement getSuggestionElement();
  }
}
