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

package org.waveprotocol.wave.client.autohide;

import com.google.gwt.dom.client.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoHiders are registered by entities that want to be automatically hidden
 * when the user performs a particular global UI action, such as clicking
 * outside the containing element of the entity in question.
 *
 * An AutoHider is registered if it is being applied to incoming events. It is
 * possible to have an AutoHider that exists, but is not being used to respond
 * to incoming events.
 *
 */
public class AutoHider implements Hideable {
  /** How sensitive a Hideable is autohide triggers. */
  public enum KeyBehavior {
    DO_NOT_HIDE_ON_ANY_KEY,
    HIDE_ON_ESCAPE,
    /** "Hide on any key" also includes mouse-wheel events as "key event". */
    HIDE_ON_ANY_KEY
  }

  /**
   * The thing that gets hidden when an appropriate event is detected.
   */
  private final Hideable hideable;

  /**
   * Whether or not to hide when a click event occurs outside a given element.
   */
  private final boolean hideOnOutsideClick;

  /**
   * Whether or not to hide when any key (or just the escape key) is pressed.
   */
  private final KeyBehavior keyBehavior;

  /**
   * Whether or not to hide when the window is resized.
   */
  private final boolean hideOnWindowResize;

  /**
   * Whether or not to hide when moving in browser history (e.g., back button).
   */
  private final boolean hideOnHistoryEvent;

  /**
   * Whether or not this AutoHider has been registered and is listening to
   * events at the event preview level.
   */
  private boolean isRegistered = false;

  /**
   * Used to determine whether which parent elements are considered 'inside' the thing being hidden,
   * and so clicks must not be contained in these to be considered 'outside' and enough to hide it.
   */
  private final List<Element> insideElements = new ArrayList<Element>();

  /**
   * @param hideable Gets hidden on appropriate event.
   * @param hideOnOutsideClick Should a click outside the entity cause an auto-hide?
   * @param hideOnWindowResize Should resizing the window cause an auto-hide?
   * @param hideOnHistoryEvent Should moving back or forward in browser history cause an auto-hide?
   * @param keyBehavior Should pressing a key cause an auto-hide?
   */
  public AutoHider(Hideable hideable,
                   boolean hideOnOutsideClick,
                   boolean hideOnWindowResize,
                   boolean hideOnHistoryEvent,
                   KeyBehavior keyBehavior) {
    this.hideable = hideable;
    this.hideOnOutsideClick = hideOnOutsideClick;
    this.hideOnWindowResize = hideOnWindowResize;
    this.hideOnHistoryEvent = hideOnHistoryEvent;
    this.keyBehavior = keyBehavior;
  }

  /**
   * @return Whether or not we should hide when any key is pressed.
   */
  public boolean shouldHideOnAnyKey() {
    return keyBehavior == KeyBehavior.HIDE_ON_ANY_KEY;
  }

  /**
   * @return Whether or not we should hide when the escape key is pressed.
   */
  public boolean shouldHideOnEscape() {
    return keyBehavior == KeyBehavior.HIDE_ON_ESCAPE || keyBehavior == KeyBehavior.HIDE_ON_ANY_KEY;
  }

  /**
   * @return Whether or not we should hide when a click occurs outside of the
   *         entity.
   */
  public boolean shouldHideOnOutsideClick() {
    return hideOnOutsideClick;
  }

  /**
   * @return Whether or not we should hide when the window is resized.
   */
  public boolean shouldHideOnWindowResize() {
    return hideOnWindowResize;
  }

  /**
   * @return Whether or not we should hide when moving in browser history
   */
  public boolean shouldHideOnHistoryEvent() {
    return hideOnHistoryEvent;
  }

  /**
   * @return Whether or not this AutoHider is registered.
   */
  public boolean isRegistered() {
    return isRegistered;
  }

  /**
   * Whitelists an element so that clicking on it does not hide the item this hider is for.
   *
   * @param element The element to use to determine whether click events are
   *        inside or outside.
   */
  public void ignoreHideClickFor(Element element) {
    this.insideElements.add(element);
  }

  /**
   * @param isRegistered Whether or not this AutoHider is registered.
   */
  public void setRegistered(boolean isRegistered) {
    this.isRegistered = isRegistered;
  }

  @Override
  public void hide() {
    hideable.hide();
  }

  @Override
  public boolean isShowing() {
    return hideable.isShowing();
  }

  /**
   * @param target An element.
   * @return {@code true} if the given element is considered to be inside the
   *         entity.
   */
  public boolean doesContain(Element target) {
    for (Element element : insideElements) {
      if (element.isOrHasChild(target)) {
        return true;
      }
    }
    return false;
  }
}
