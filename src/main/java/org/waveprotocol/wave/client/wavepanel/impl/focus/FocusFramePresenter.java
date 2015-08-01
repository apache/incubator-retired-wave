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

package org.waveprotocol.wave.client.wavepanel.impl.focus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.scroll.SmartScroller;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.FocusFrameView;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ValueUtils;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Presents the focus frame, and exposes an API for controlling it.
 *
 */
public final class FocusFramePresenter
    implements BlipEditStatusListener, SourcesEvents<FocusFramePresenter.Listener>, WavePanelImpl.LifecycleListener {

  public interface Listener {
    void onFocusMoved(BlipView oldUi, BlipView newUi);
  }

  public interface FocusOrder {
    BlipView getNext(BlipView current);

    BlipView getPrevious(BlipView current);
  }

  public interface FrameKeyHandler {
    boolean onKeySignal(KeyCombo key, BlipView context);
  }

  /** Focus frame UI. */
  private final FocusFrameView view;

  /** Thing that knows how to move around views. */
  private final ViewTraverser traverser;

  /** Listeners. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /** Scroller. */
  private final SmartScroller<? super BlipView> scroller;

  /** Order generator, optionally installed. */
  private FocusOrder order;

  /** Blip that currently has the focus frame. May be {@code null}. */
  private BlipView blip;

  /**
   * Creates a focus-frame presenter.
   */
  @VisibleForTesting
  FocusFramePresenter(
      FocusFrameView view, SmartScroller<? super BlipView> scroller, ViewTraverser traverser) {
    this.view = view;
    this.scroller = scroller;
    this.traverser = traverser;
  }

  //
  // Wave panel lifecycle.
  //

  @Override
  public void onInit() {
  }

  @Override
  public void onReset() {
    blip = null;
  }

  //
  // Focus movement.
  //

  /**
   * Puts the focus frame on a blip.
   */
  public void focus(BlipView blip) {
    Preconditions.checkState(scroller != null);
    Preconditions.checkArgument(blip != null);
    focus(blip, true);
  }

  /**
   * Puts the focus frame on a blip, without forcing it into view.
   */
  public void focusWithoutScroll(BlipView blip) {
    Preconditions.checkState(scroller != null);
    Preconditions.checkArgument(blip != null);
    focus(blip, false);
  }

  /**
   * Moves the focus frame to the previous blip in the vertical ordering, if
   * there is one. If there is no previous blip, this method does nothing.
   */
  public void moveUp() {
    BlipView prev = blip != null ? traverser.getPrevious(blip) : null;
    if (prev != null) {
      focus(prev, true);
    }
  }

  /**
   * Moves the focus frame to the next blip in the vertical ordering, if there
   * is one. If there is no next blip, this method does nothing.
   */
  public void moveDown() {
    BlipView next = blip != null ? traverser.getNext(blip) : null;
    if (next != null) {
      focus(next, true);
    }
  }

  /**
   * Sets the blip that has the focus frame. If {@code blip} is null, the focus
   * frame is removed.
   */
  private void focus(BlipView blip, boolean scroll) {
    if (!ValueUtils.equal(this.blip, blip)) {
      BlipView oldUi = this.blip;
      BlipView newUi = blip;

      // Scroll first, before layout gets invalidated.
      if (newUi != null && scroll) {
        scroller.moveTo(newUi);
      }

      detachChrome();
      this.blip = blip;
      attachChrome();

      fireOnFocusMoved(oldUi, newUi);
    }
  }

  private void detachChrome() {
    if (this.blip != null) {
      this.blip.getMeta().removeFocusChrome(view);
    }
  }

  private void attachChrome() {
    if (this.blip != null) {
      this.blip.getMeta().placeFocusFrame(view);
    }
  }

  //
  // Events.
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnFocusMoved(BlipView oldUi, BlipView newUi) {
    for (Listener listener : listeners) {
      listener.onFocusMoved(oldUi, newUi);
    }
  }

  //
  // Focus ordering / processing.
  //

  /**
   * Moves to the next blip as defined by an attached
   * {@link #setOrder(FocusOrder) ordering}, if there is one.
   */
  public void focusNext() {
    // Real condition is that blip != null implies scroller != null.
    Preconditions.checkState(blip == null || scroller != null);
    if (blip != null && order != null) {
      BlipView next = order.getNext(blip);
      if (next != null) {
        focus(next);
      }
      else {
        focusPrevious();
      }
    }
  }

  /**
   * Moves to the previous blip as defined by an attached
   * {@link #setOrder(FocusOrder) ordering}, if there is one.
   */
  public void focusPrevious() {
    // Real condition is that blip != null implies scroller != null.
    Preconditions.checkState(blip == null || scroller != null);
    if (blip != null && order != null) {
      BlipView next = order.getPrevious(blip);
      if (next != null) {
        focus(next);
      }
    }
  }

  /**
   * Specifies the orderer to use when moving to the next and previous
   * interesting blips.
   */
  public void setOrder(FocusOrder order) {
    this.order = order;
  }

  /** @return the view that is currently focused. */
  public BlipView getFocusedBlip() {
    return blip;
  }

  /**
   * Sets the blip style depending on if is editing or not.
   *
   * @param editing the new editing
   */
  public void setEditing(boolean editing) {
    view.setEditing(editing);
  }
}
