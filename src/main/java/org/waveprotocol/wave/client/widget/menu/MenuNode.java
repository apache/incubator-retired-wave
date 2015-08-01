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
 * Representation of a menu item.
 * This representation is totally independent from its rendering.
 * A collection of <code>MenuNode</code>s is contained inside a <code>MenuBranch</code>.
 * A <code>MenuNode</code> can:
 * <ul>
 * <li>represent a separator symbol inside a menu.
 * <li>be of kind HTML hyperlink, where it is meant to point to a URL.
 * <li>have a sub-menu associated to itself.
 * <li>launch an action when it is being clicked.
 * <li>have a tooltip associated to itself.
 * </ul>
 * <p>
 *
 * @see MenuBranch
 */
public class MenuNode {

  /**
   * This type is different to GWT's command to help the compiler figure out
   * that its execute method is only ever called from one place.
   */
  public interface MenuCommand {
    /**
     * Causes the Command to perform its encapsulated behavior. This function
     * must only be called by TopPanel.RunAsyncCommand to ensure that all menu
     * items end up behind a single runAsync split point.
     */
    void execute();
  }

  /**
   * @see MenuNode#getPopupDebugClassName()
   */
  private final String popupDebugClassName;

  /**
   * @see MenuNode#getCommand()
   */
  private final MenuCommand command;

  /**
   * @see MenuNode#getSubMenu()
   */
  private final MenuBranch subMenu;

  /**
   * @see MenuNode#getText()
   */
  private final String text;

  /**
   * @see MenuNode#isHTML()
   */
  private final boolean isHTML;


  private final String url;
  /**
   * @see MenuNode#url
   */
  private final String anchor;
  /**
   * @see MenuNode#url
   */
  private final String target;

  /**
   * @see MenuNode#isSeparator()
   */
  private final boolean isSeparator;

  /**
   * @see MenuNode#isNew()
   * NOTE(user): This would be final, except we've got enough constructors...
   * Builder pattern anyone?
   */
  private boolean isNew = false;

  /**
   * @see MenuNode#getId()
   */
  private final String id;

  /**
   * Constructs a menu item which is the head of a sub-menu.
   * @param text is the name to be rendered.
   * @param subMenu is the sub-menu attached to this menu item.
   */
  public MenuNode(String text, MenuBranch subMenu) {
    this(text, "", "", "", false, false, null, null, subMenu, null);
  }

  /**
   * Constructs a menu item which is the head of a sub-menu.
   * @param text is the name to be rendered.
   * @param subMenu is the sub-menu attached to this menu item.
   * @param id {@link MenuNode#getId()}
   */
  public MenuNode(String text, MenuBranch subMenu, String id, String popupDebugClassName) {
    this(text, "", "", "", false, false, id, null, subMenu, popupDebugClassName);
  }

  /**
   * Constructs a simple menu item from its name and associated action.
   * @param text is the name to be rendered.
   * @param command is the action to be launched when this item is clicked.
   */
  public MenuNode(String text, MenuCommand command) {
    this(text, "", "", "", false, false, null, command, null, null);
  }

  /**
   * Constructs a menu item of kind hyperlink along with associated action.
   * @param anchor is the name to be rendered as hyperlink.
   * @param url is the URL that the hyperlink is pointing to.
   * @param command is the action to be launched when this item is clicked.
   */
  public MenuNode(String anchor, String url, MenuCommand command) {
    this("", anchor, url, "", true, false, null, command, null, null);
  }

  /**
   * Constructs a menu item that opens the referenced url at the specified target.
   * @param anchor
   * @param url
   */
  public static MenuNode openUrlInNewWindow(String anchor, String url) {
    return new MenuNode("", anchor, url, "_blank", false, false, null,  null, null, null);
  }

  /**
   * Constructs a menu item with all its member variables initially supplied.
   */
  private MenuNode(String text, String anchor, String url, String target, boolean asHTML,
      boolean isSeparator, String id, MenuCommand command, MenuBranch subMenu,
      String popupDebugClassName) {
    this.text = text;
    this.anchor = anchor;
    this.url = url;
    this.target = target;
    this.isHTML = asHTML;
    this.isSeparator = isSeparator;
    this.id = id;
    this.command = command;
    this.subMenu = subMenu;
    this.popupDebugClassName = popupDebugClassName;
  }

  /**
   * Creates a menu item of kind separator symbol.
   */
  public static MenuNode createDivider() {
    return new MenuNode("", "", "", "", false, true, null, null, null, null);
  }

  /**
   * @return {@link MenuCommand} to be launched when this menu item is being clicked.
   * If <code>isSeparator</code> is <code>true</code>, this variable is ignored.
   * If there is a sub-menu, this <code>command</code> variable is automatically created,
   * so that it can trigger the appropriate popup sub-menu.
   */
  public MenuCommand getCommand() {
    return command;
  }

  /**
   * A globally unique identifier for this menu item.
   * With respect to the DOM, this should be added as an id attribute on the anchor.
   *
   * @return The id of the node, or null if no id given.
   */
  public String getId() {
    return id;
  }

  /**
   * debugClassName to be associated to a popup Window if this item ever creates
   * one.
   *
   * @return The debugClassName of the node.
   */
  public String getPopupDebugClassName() {
    return popupDebugClassName;
  }

  /**
   * @return {@link MenuBranch} the optional sub-menu attached to this menu item.
   */
  public MenuBranch getSubMenu() {
    return subMenu;
  }

  /**
   * @return String of the text to be rendered.
   */
  public String getText() {
    return text;
  }

  /**
   * @return boolean equals <code>true</code> when the text member variable has to
   *                 be rendered as HTML.
   */
  public boolean isHTML() {
    return isHTML;
  }

  /**
   * @return the URL to which the MenuNode links.
   * When <code>url</code> is set (which is when it contains at least 1 character):
   * <ul>
   * <li><code>isHTML</code> is automatically set to <code>true</code>.
   * <li><code>text</code> is automatically filled so that it does represent a
   * hyperlink when rendered as HTML.
   * </ul>
   * <p>
   * <code>url</code>, <code>anchor</code> and <code>target</code> go together.
   * <li><code>url</code> combined <code>target</code> is the address pointed by the hyperlink.
   * <li><code>anchor</code> is the name to be rendered for the hyperlink.
   */
  public String getUrl() {
    return url;
  }

  /**
   * @return the name to be rendered for the hyperlink.
   * @see MenuNode#getUrl()
   */
  public String getAnchor() {
    return anchor;
  }

  /**
   * @return the target frame for the address pointed by the hyperlink
   * @see MenuNode#getUrl()
   */
  public String getTarget() {
    return target;
  }

  /**
   * @return boolean equals <code>true</code> when this menu item is just a divider.
   *                 and in that case the other variables are therefore being ignored.
   */
  public boolean isSeparator() {
    return isSeparator;
  }

  /**
   * Sets that the menu node is for a new feature.
   * @return this, for convenience.
   */
  public MenuNode setNew() {
    this.isNew = true;
    return this;
  }

  /**
   * @return boolean <code>true</code> if the menu item is new. This is to
   *     announce new features to the user.
   */
  public boolean isNew() {
    return isNew;
  }
}
