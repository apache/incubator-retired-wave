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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEventImpl;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects auto-hide events by installing an event preview.
 *
 * This registrar handles multiple simultaneous AutoHiders by treating the ones
 * opened later as 'higher up' than the ones opened earlier, and so events that
 * cause auto-hides will be routed to the higher AutoHiders first. This
 * behaviour can be changed in the future by introducing a more sophisticated
 * structure to store the AutoHiders in and, for instance, having each AutoHider
 * declaring a parent AutoHider.
 *
 */
public class EventPreviewAutoHiderRegistrar implements AutoHiderRegistrar, NativePreviewHandler,
ResizeHandler, ValueChangeHandler<String> {
  /**
   * List of AutoHiders that currently need to be considered when interpreting
   * incoming events.
   */
  private final List<AutoHider> autoHiders = new ArrayList<AutoHider>();

  /**
   * Used to deregister this object from the event preview when there are no
   * registered AutoHiders.
   */
  private HandlerRegistration eventPreviewRegistration;

  private HandlerRegistration onResizeRegistration;

  private HandlerRegistration onHistoryRegistration;

  @Override
  public void registerAutoHider(final AutoHider autoHider) {
    autoHider.setRegistered(true);
    autoHiders.add(autoHider);

    if (eventPreviewRegistration == null) {
      eventPreviewRegistration = Event.addNativePreviewHandler(this);
    }

    if (onResizeRegistration == null) {
      onResizeRegistration = Window.addResizeHandler(this);
    }

    if (onHistoryRegistration == null) {
      onHistoryRegistration = History.addValueChangeHandler(this);
    }
  }

  @Override
  public void deregisterAutoHider(AutoHider autoHider) {
    autoHiders.remove(autoHider);
    autoHider.setRegistered(false);

    if (autoHiders.isEmpty()) {
      if (eventPreviewRegistration != null) {
        eventPreviewRegistration.removeHandler();
        eventPreviewRegistration = null;
      }
      if (onResizeRegistration != null) {
        onResizeRegistration.removeHandler();
        onResizeRegistration = null;
      }
      if (onHistoryRegistration!= null) {
        onHistoryRegistration.removeHandler();
        onHistoryRegistration = null;
      }
    }
  }

  @Override
  public void onPreviewNativeEvent(NativePreviewEvent previewEvent) {
    if (autoHiders.isEmpty()) {
      return;
    }

    // TODO(danilatos,user,user): Push signal down a layer - clean this up.
    Event event = Event.as(previewEvent.getNativeEvent());
    int lowLevelType = event.getTypeInt();

    // TODO(danilatos): Insert this logic deeply rather than
    // sprinkling it in event handlers. Also the return value
    // of onEventPreview is the reverse of signal handlers.
    SignalEvent signal = SignalEventImpl.create(event, false);
    if (signal == null) {
      return;
    }

    // Key events (excluding escape) and mousewheel events use hideTopmostAutoHiderForKeyEvent
    if (lowLevelType == Event.ONMOUSEWHEEL || signal.isKeyEvent()) {
      if (hideTopmostAutoHiderForKeyEvent(false)) {
        // TODO(user): We don't call previewEvent.cancel() here, since for the floating-buttons
        // menu we want, for example, space-bar to still shift focus to the next blip.
        // The to-do is to audit the previewEvent.cancel call below and see why it's there (and if
        // it's not needed, eliminate it).
        return;
      }
    }

    // Pressing escape at any time causes us to close and discard the event.
    if (signal.getKeySignalType() == KeySignalType.NOEFFECT &&
        event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
      if (hideTopmostAutoHiderForKeyEvent(true)) {
        previewEvent.cancel();
        return;
      }
    }

    // Click events and mouse-wheel events that fall through use hideAllAfter.
    if (lowLevelType == Event.ONMOUSEDOWN || lowLevelType == Event.ONMOUSEWHEEL) {
      hideAllAfter(signal.getTarget());
    }

    // Otherwise we don't do anything and the event continues as usual.
  }

  /**
   * Causes all AutoHiders after the one that contains the given element to hide.
   *
   * @param target An element.
   */
  private void hideAllAfter(Element target) {
    List<AutoHider> toHide = new ArrayList<AutoHider>();
    for (int i = autoHiders.size() - 1; i >= 0; i--) {
      AutoHider autoHider = autoHiders.get(i);
      if (autoHider.doesContain(target)) {
        break;
      }
      toHide.add(autoHider);
    }

    for (AutoHider autoHider : toHide) {
      autoHider.hide();
    }
  }

  /**
   * Hides the topmost AutoHider that is supposed to hide on key events.
   */
  private boolean hideTopmostAutoHiderForKeyEvent(boolean keyIsEscape) {
    for (int i = autoHiders.size() - 1; i >= 0; i--) {
      AutoHider autoHider = autoHiders.get(i);
      if (autoHider.shouldHideOnAnyKey() || (keyIsEscape && autoHider.shouldHideOnEscape())) {
        autoHider.hide();
        return true;
      }
    }
    return false;
  }

  @Override
  public void onResize(ResizeEvent event) {
    List<AutoHider> toHide = new ArrayList<AutoHider>();
    for (AutoHider autoHider : autoHiders) {
      if (autoHider.shouldHideOnWindowResize()) {
        toHide.add(autoHider);
      }
    }

    for (AutoHider autoHider : toHide) {
      autoHider.hide();
    }
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    List<AutoHider> toHide = new ArrayList<AutoHider>();
    for (AutoHider autoHider : autoHiders) {
      if (autoHider.shouldHideOnHistoryEvent()) {
        toHide.add(autoHider);
      }
    }

    for (AutoHider autoHider : toHide) {
      autoHider.hide();
    }
  }
}
