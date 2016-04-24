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

package org.waveprotocol.wave.client.editor.selection.html;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.debug.logger.BufferedLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.util.IntRange;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Document selection methods.
 *
 */
public class NativeSelectionUtil {
  /**
   * For notification before/after transient DOM mutations, which can be
   * ignored.
   */
  public static interface MutationListener {
    void startTransientMutations();
    void endTransientMutations();
  }

  public static class NoOpMutationListener implements MutationListener {
    @Override
    public void endTransientMutations() {
    }

    @Override
    public void startTransientMutations() {
    }
  }

  /**
   * Debug logger for selection package
   */
  static final LoggerBundle LOG = new BufferedLogger("selection");

  /** Browser-specific implementation of getting the content/range of the selection. */
  private static final SelectionImpl impl;

  /** Browser-specific implementation for getting the x/y position of the selection. */
  private static final SelectionCoordinatesHelper coordinateGetter;

  /**
   * Create browser specific selection implementation.
   */
  static {
    if (UserAgent.isIE()) {
      impl = new SelectionImplIE();
      coordinateGetter = new SelectionCoordinatesHelperIEImpl();
    } else if (UserAgent.isMobileWebkit()) {
      // TODO(patcoleman/mtsui/macpherson): adapt to perform as desired on browsers:
      impl = new SelectionImplDisabled();
      coordinateGetter = new SelectionCoordinatesHelperDisabled();
    } else {
      // avoid casting:
      SelectionImplW3C w3cImpl = new SelectionImplW3C();
      impl = w3cImpl;
      coordinateGetter = new SelectionCoordinatesHelperW3C(new NativeSelectionUtil.MutationListener() {
        @Override
        public void startTransientMutations() {
          transientMutationListener.startTransientMutations();
        }

        @Override
        public void endTransientMutations() {
          transientMutationListener.endTransientMutations();
        }
      });
    }
  }

  /**
   * Set this in the editor to prevent it from receiving to transient dom
   * mutation events. This value should never be null.
   */
  private static MutationListener transientMutationListener = new NoOpMutationListener();

  /**
   * Whether or not we are caching the selection currently
   * This should only be done for the duration of a single event handler
   */
  private static boolean caching = false;

  /**
   * The cached calculated selection
   */
  private static FocusedPointRange<Node> cache = null;

  /**
   * Registers a listener to be notified when transient mutations are about to
   * happen.
   *
   * @param mutationListener
   */
  public static void setTransientMutationListener(
      NativeSelectionUtil.MutationListener mutationListener) {
    Preconditions.checkNotNull(mutationListener, "null mutationListener");
    transientMutationListener = mutationListener;
  }

  /**
   * Turning selection caching on
   */
  public static void cacheOn() {
    caching = true;
  }

  /**
   * Turn selection caching off, and clear the cache
   */
  public static void cacheOff() {
    caching = false;
    cache = null;
  }

  /**
   * Just clear the cache
   */
  public static void cacheClear() {
    cache = null;
  }

  /**
   * TODO(user): Handle multiple selections in the document.
   *
   * @return The current selection, or null if nothing is
   * currently selected. Note that the Elements in the range
   * are references to the actual elements in the DOM; not
   * clones.
   */
  public static FocusedPointRange<Node> get() {
    if (caching) {
      if (cache == null) {
        cache = impl.get();
      }
      return cache;
    } else {
      return impl.get();
    }
  }

  /**
   * Same as {@link #get()}, but returns an ordered range instead.
   *
   * Ordered means the anchor before, or the same as, the focus
   */
  public static PointRange<Node> getOrdered() {
    return impl.getOrdered();
  }

  /**
   * @return true if the selection is currently ordered. Ordered means the
   *         anchor before, or the same as, the focus
   */
  public static boolean isOrdered() {
    return impl.isOrdered();
  }

  /**
   * Gets the position of the current selection.
   *
   * WARNING(mtsui): SLOW!!! on Firefox and Chrome as it forces a redraw.
   *
   * Use isFocusInBounds if possible.
   *
   * @return the Position of the current selection, or null if there is no
   *         selection.
   */
  public static OffsetPosition slowGetPosition() {
    return coordinateGetter.getFocusPosition();
  }

  public static OffsetPosition slowGetAnchorPosition() {
    return coordinateGetter.getAnchorPosition();
  }

  /**
   * Sets selection to a range
   *
   * @param range
   */
  public static void set(FocusedPointRange<Node> range) {
    cache = null;
    impl.set(range.getAnchor(), range.getFocus());
  }

  /**
   * Sets selection to two html points
   *
   * @param anchor
   * @param focus
   */
  public static void set(Point<Node> anchor, Point<Node> focus) {
    cache = null;
    impl.set(anchor, focus);
  }

  /**
   * Sets selection to caret
   *
   * @param caret
   */
  public static void setCaret(Point<Node> caret) {
    cache = null;
    set(caret, caret);
  }

  /**
   * Clears selection
   */
  public static void clear() {
    cache = null;
    impl.clear();
  }

  /**
   * Saves the selection internally in a manner optimised for each browser
   */
  public static void saveSelection() {
    impl.saveSelection();
  }

  /**
   * Restores the selection saved with {@link #saveSelection()}
   *
   * Behaviour is undefined if the DOM has been changed since the selection
   * was saved.
   */
  public static void restoreSelection() {
    impl.restoreSelection();
  }

  /**
   * Fast implementation to check if there is a selection or not.
   * @return true if there is a selection
   */
  public static boolean selectionExists() {
    return impl.selectionExists();
  }

  /**
   * Gets the y-bounds of the cursor position in absolute coordinates.
   *
   * This is faster than {@link #slowGetPosition()}, but returns a range that
   * contains the cursor, rather than the exact cursor location.
   */
  public static IntRange getFocusBounds() {
    return coordinateGetter.getFocusBounds();
  }

  /**
   * @return The currently active element.
   */
  public static native Element getActiveElement() /*-{
    return $doc.activeElement;
  }-*/;

}
