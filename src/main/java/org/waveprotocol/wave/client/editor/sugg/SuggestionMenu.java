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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEventImpl;
import org.waveprotocol.wave.client.editor.sugg.InteractiveSuggestionsManager.SuggestionMenuHandler;

/**
 * Reasonable implementation of a suggestion drop down menu.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SuggestionMenu extends MenuBar implements Menu {
  /**
   * We want to put the (obfuscated) classname for highlighted items on menu
   * items here. Currently there is no default style (it could be added) but
   * specific things (like spell suggestions) need to be able to refer to this
   * style via inheritance for their specific needs (the spelly menu items have
   * a non-trivial dom & styling)
   */
  public static final SuggestionResources RESOURCES = GWT.create(SuggestionResources.class);
  static {
    StyleInjector.inject(RESOURCES.css().getText(), true);
  }

  private final SuggestionMenuHandler handler;

  /** Default Constructor */
  public SuggestionMenu(SuggestionMenuHandler handler) {
    super(true);
    this.handler = handler;
    // Sink the context menu even so that we can cancel it and sink key presses
    // so we can stop propagation.
    sinkEvents(Event.ONCONTEXTMENU | Event.ONKEYPRESS);
  }

  @Override
  public void clearItems() {
    super.clearItems();
  }

  @Override
  public MenuItem addItem(SafeHtml title, final Command callback) {
    // TODO(danilatos): Make the titles line up
    //return super.addItem((index++) + ". " + title, callback);
    return super.addItem(new MenuItem(title.asString(), true, new Command() {

      @Override
      public void execute() {
        handler.beforeItemClicked();
        callback.execute();
        handler.handleItemSelected();
      }

    }){
      /**
       * Adding a hook so we can add our own type-safe style name to the
       * highlighted item, as opposed to the one they use, which is private
       * and not using style injector.
       */
      @Override
      protected void setSelectionStyle(boolean selected) {
        super.setSelectionStyle(selected);
        // TODO(user): remove the dependency to SpellSuggestion.resources
        if (selected) {
          getElement().addClassName(RESOURCES.css().hover());
        } else {
          getElement().removeClassName(RESOURCES.css().hover());
        }
      }
    });
  }

  @Override
  public MenuItem addItem(String title, final Command callback) {
    return addItem(EscapeUtils.fromString(title), callback);
  }

  @Override
  public void onBrowserEvent(Event event) {
    SignalEvent sEvent = SignalEventImpl.create(event, true);

    if (sEvent != null) {
      handleEventInner(event);
    }

    super.onBrowserEvent(event);
  }

  private void handleEventInner(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCONTEXTMENU:
        event.preventDefault();
        break;
      case Event.ONKEYPRESS:
      case Event.ONKEYDOWN: {
        // NOTE(user): It is necessary to stop propagation on the key events to
        // prevent them from leaking to the blip/wave presenters.
        int keyCode = DOM.eventGetKeyCode(event);

        // Move left/right to previous/next drop down widget
        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
          case KeyCodes.KEY_RIGHT:
            handler.handleLeftRight(keyCode == KeyCodes.KEY_RIGHT);
            event.stopPropagation();
            return;
          case KeyCodes.KEY_ENTER:
            event.stopPropagation();
        }

        if (keyCode >= '1' && keyCode <= '9') {
          // TODO(danilatos): Is this ok? i18n etc?
          int index = keyCode - '1';

          if (index >= 0 && index < getItems().size()) {
            MenuItem item = getItems().get(index);
            if (item == null) {
              return;
            }

            item.getCommand().execute();
            DOM.eventPreventDefault(event);
          }
        }
        break;
      }
      case Event.ONMOUSEOUT: {
        // Need to check that we really seem to have left the menu, as
        // mouse-out events get triggered whenever the mouse moves between
        // selections in the menu.
        EventTarget target = event.getRelatedEventTarget();
        Element targetElement = Element.as(target);
        if (!getElement().isOrHasChild(targetElement)) {
          handler.handleMouseOut();
        }
        break;
      }
      case Event.ONMOUSEOVER: {
        handler.handleMouseOver();
        break;
      }
    }
  }
}
