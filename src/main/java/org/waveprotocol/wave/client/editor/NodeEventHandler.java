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

package org.waveprotocol.wave.client.editor;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;

/**
 * Interface of user event handlers for ContentElements. All the events here
 * match a user action (keypress, mouse click, etc.) but have already been
 * processed by Editor to relate to the content DOM tree rather than the HTML
 * implementation.
 *
 * Typically, nodes will handle the event by using the MutableDocument to
 * effect change to the content DOM.
 *
 * Each handler returns a boolean: true if the event was handled by this
 * method, false if it was not handled. If false is returned, default handling
 * logic may execute (see
 * {@link org.waveprotocol.wave.client.editor.content.NodeEventRouter}. In some
 * contexts, such as with navigation events, it is possible to allow the default
 * browser behaviour - in this case call event.allowBrowserDefault()
 *
 * The handle methods below are always called before the browser's native
 * editor has handled the event. If all the nodes asked by Editor to handle a
 * given event returns false, then Editor lets the native editor handle the
 * event.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface NodeEventHandler {

  /**
   * Called when the event handler has become active for the given element.
   *
   * It should attach any DOM event listeners, etc. at this stage.
   *
   * @param element
   */
  public void onActivated(ContentElement element);

  /**
   * Called when the event handler has been deactivated for the given element.
   *
   * It should dettach any DOM event listeners, etc. at this stage.
   *
   * @param element
   */
  public void onDeactivated(ContentElement element);

  /**
   * Handles an enter/return key from inside the node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleEnter(ContentElement element, EditorEvent event);

  /**
   * Handles a backspace that occurred with the caret immediately after this
   * node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event);

  /**
   * Handles a backspace that occurred with the caret immediately at the
   * beginning of the node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleBackspaceAtBeginning(ContentElement element, EditorEvent event);

  /**
   * Handles a backspace that occurred with the caret past the beginning of the
   * node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleBackspaceNotAtBeginning(ContentElement element, EditorEvent event);

  /**
   * Handles a delete that occurred with the caret inside this node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleDelete(ContentElement element, EditorEvent event);

  /**
   * Handles a delete that occurred with the caret immediately before this node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event);

  /**
   * Handles a delete that occurred with the caret immediately at the end of the
   * node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleDeleteAtEnd(ContentElement element, EditorEvent event);

  /**
   * Handles a delete that occurred with the caret before the end of the node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleDeleteNotAtEnd(ContentElement element, EditorEvent event);

  /**
   * Handles a left arrow that occurred with the caret inside this node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleLeft(ContentElement element, EditorEvent event);

  /**
   * Handles a left arrow that occurred with the caret immediately at the
   * beginning of the node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleLeftAfterNode(ContentElement element, EditorEvent event);

  /**
   * Handles a left arrow that occurred with the caret immediately after this
   * node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event);

  /**
   * Handles a right arrow that occurred with the caret inside this node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleRight(ContentElement element, EditorEvent event);

  /**
   * Handles a right arrow that occurred with the caret immediately before this
   * node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleRightBeforeNode(ContentElement element, EditorEvent event);

  /**
   * Handles a right arrow that occurred with the caret immediately at the end
   * of the node.
   *
   * @param event
   * @return true if handled
   */
  public boolean handleRightAtEnd(ContentElement element, EditorEvent event);

  /**
   * Handles a click
   *
   * @param event The click event
   * @return true if handled
   */
  public boolean handleClick(ContentElement element, EditorEvent event);

}
