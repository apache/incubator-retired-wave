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

package org.waveprotocol.wave.client.wavepanel.impl.edit;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.Measurer;
import org.waveprotocol.wave.client.common.util.MeasurerInstance;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.client.scroll.DomScrollPanel;
import org.waveprotocol.wave.client.scroll.Extent;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.dom.TopConversationDomImpl;
import org.waveprotocol.wave.client.wavepanel.view.impl.TopConversationViewImpl;
import org.waveprotocol.wave.model.util.IntRange;

/**
 * Ensures that the selection is always in the viewport.
 * <p>
 * This feature only works if the underlying view implementation is DOM-based.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class KeepFocusInView implements EditorUpdateListener, EditSession.Listener {
  /**
   * Buffer size from the top and bottom of the viewport used as selection
   * boundaries.
   */
  private static final int PAD_PX = 50;

  /** Edit session, whose events activate and deactivate this feature. */
  private final EditSession edit;
  /** Panel in which this feature is active. */
  private final WavePanelImpl waveUi;
  /** Measurer used to measure sizes of DOM elements. */
  private final Measurer measurer = MeasurerInstance.get();

  private Element viewport;
  private DomScrollPanel scroller;

  KeepFocusInView(EditSession edit, WavePanelImpl waveUi) {
    this.edit = edit;
    this.waveUi = waveUi;
  }

  /**
   * Installs this feature.
   */
  public static void install(EditSession edit, WavePanelImpl panel) {
    // It might be feasible to turn this feature off for Firefox, since it has
    // native support for keeping the focus in view. However, Firefox only does
    // the bare minimum, keeping the focus right at the viewport edges.
    new KeepFocusInView(edit, panel).init();
  }

  public void init() {
    edit.addListener(this);
  }

  public void destroy() {
    edit.removeListener(this);
  }

  @Override
  public void onSessionStart(Editor editor, BlipView blipUi) {
    viewport = hackExtractScrollElement(waveUi.getContents());
    scroller = DomScrollPanel.create(viewport);
    editor.addUpdateListener(this);
  }

  @Override
  public void onSessionEnd(Editor editor, BlipView blipUi) {
    editor.removeUpdateListener(this);
    scroller = null;
    viewport = null;
  }

  private boolean isEditing() {
    return viewport != null;
  }

  @Override
  public void onUpdate(EditorUpdateEvent event) {
    // Check isEditing to prevent code from running during session teardown.
    if (isEditing() && event.selectionCoordsChanged()) {
      // First use a non-invasive approach for discovering the focus location.
      // This query does not mutate the DOM, so will not force a layout cycle.
      IntRange r = NativeSelectionUtil.getFocusBounds();
      if (r != null) {
        double focusStartInScreen = r.getFirst();
        double focusEndInScreen = r.getSecond();
        double viewportStartInScreen = measurer.top(null, viewport);
        double viewportEndInScreen = measurer.bottom(null, viewport);
        if (viewportStartInScreen < focusStartInScreen - PAD_PX
            && focusEndInScreen + PAD_PX < viewportEndInScreen) {
          // All ok.
          return;
        }
      }

      // The fast path failed. Try a more invasive method. This query does
      // mutate the DOM, so the subsequent measurement queries will force
      // synchronous layout, which can be slow.
      OffsetPosition p = NativeSelectionUtil.slowGetPosition();
      if (p != null && p.offsetParent != null) {
        Extent viewportInContent = scroller.getViewport();
        double focusInViewport = measurer.top(viewport, p.offsetParent) + p.top;
        double focusInContent = focusInViewport + viewportInContent.getStart();
        if (focusInContent - PAD_PX < viewportInContent.getStart()) {
          scroller.moveTo(focusInContent - PAD_PX);
        } else if (focusInContent + PAD_PX > viewportInContent.getEnd()) {
          scroller.moveTo(focusInContent + PAD_PX - viewportInContent.getSize());
        } else {
          // All ok.
          return;
        }
      }

      // No other options left. Maybe selection doesn't exist?
    }
  }

  /**
   * Cracks open a conversation UI object, and rips out the scrollable DOM
   * element from it.
   */
  //
  // This method is a hack because the view interfaces are designed to hide all
  // DOM concerns, so that presentation code is independent of a DOM-based
  // implementation (e.g., so that it is server-side renderable, runnable in
  // tests, runnable on other view implementations, etc). However, since focus
  // and selection are essentially DOM-based concerns, this feature does not
  // make sense to run in any other environment other than a DOM based one.
  //
  private static Element hackExtractScrollElement(TopConversationView waveUi) {
    @SuppressWarnings("unchecked")
    TopConversationViewImpl<TopConversationDomImpl> waveUiImpl =
        (TopConversationViewImpl<TopConversationDomImpl>) waveUi;
    return waveUiImpl.getIntrinsic().getThreadContainer();
  }
}
