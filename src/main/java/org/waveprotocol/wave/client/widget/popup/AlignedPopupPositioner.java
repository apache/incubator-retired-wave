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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.RootPanel;

import org.waveprotocol.wave.client.common.util.UserAgent;

/**
 * Popup positioner that places the popup above the relative element, aligned to
 * its left side if there is space on the right, or it's right side if there
 * isn't.
 * <p>
 * Note that this positioner does not always align popups correctly on Firefox,
 * because of its sub-pixel rendering aliasing problems.
 */
public final class AlignedPopupPositioner implements RelativePopupPositioner {

  // TODO(user): Expose these constants on the resource bundle of the chrome
  // images.

  /**
   * Distance from the bottom edge of the north chrome frame to the top of the
   * blue border line in it.
   */
  private final static int NORTH_CHROME_OFFSET_PX = 3;

  /**
   * Distance from the top edge of the south chrome frame to the bottom of the
   * blue border line in it.
   */
  private final static int SOUTH_CHROME_OFFSET_PX = 3;

  /**
   * Distance from the left edge of the east chrome frame to the right of the
   * blue border line in it. Non-private only for tests.
   */
  private final static int EAST_CHROME_OFFSET_PX = 1;

  /**
   * Distance from the right edge of the west chrome frame to the left of the
   * blue border line in it. Non-private only for tests.
   */
  private final static int WEST_CHROME_OFFSET_PX = 1;

  /**
   * Desired vertical space between the visual top of the popup (i.e., the blue
   * border in the north chrome image) and the bottom of the element against
   * which it is positioned. If this value is 0, the popup should appear flush
   * against its relative element.
   */
  private final static int VERTICAL_OFFSET_PX = 5;

  /**
   * Offset that shifts the alignment edge of the relative element's left
   * border-edge. This is so that the popup may be left-aligned to some point
   * within the relative element (e.g., if the relative element is an image, and
   * the alignment point is some visual part of that image).
   */
  public static class Insets {
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    /** No extra insets. Use an element's border box as the alignment box. */
    // Firefox's sub-pixel rendering causes havoc with pixel-based alignment
    // strategies. The Firefox insets come from experimental verification of
    // what is required to get border-box alignent in most cases.
    public final static Insets NONE =
        UserAgent.isFirefox() ? Insets.of(0, -1, 0, 1) : Insets.of(0, 0, 0, 0);

    private Insets(int top, int right, int bottom, int left) {
      this.top = top;
      this.right = right;
      this.bottom = bottom;
      this.left = left;
    }

    public static Insets of(int top, int right, int bottom, int left) {
      return new Insets(top, right, bottom, left);
    }
  }

  //
  // The box from which the absolute positions of elements are measured is different on different
  // browsers, due to the use of margins on the body element (NOTE: the "absolute positions" of an
  // element, as reported by getAbsoluteX, are the positions of its border edge; i.e., adding
  // margins to an element changes its "absolute position", but padding and borders do not).
  //
  // IE measures absolute offsets from the top-left of the entire window:
  //  __________________________________
  // |          |         |             |
  // |         top        |             |
  // |        __V_________|__________   |
  // |-left->|            | top      |  |
  // |       |        ____V____      |  |
  // |-----left----->|_________|     |  |
  // |-----------------right-->      |  |
  // |       |                       |  |
  // |----------------------right--->|  |
  // |       |_______________________|  |
  // |                                  |
  // |__________________________________|
  //
  // Safari measures the positions from the top-left of the body's border edge
  // (i.e., body margins do not affect top-left positions of anything, but body borders do).
  //  ____________________________________
  // |   ______________________________   |
  // |  |                   | top      |  |
  // |  |               ____V____      |  |
  // |  |----left----->|_________|     |  |
  // |  |-----------------right->      |  |
  // |  |______________________________|  |
  // |____________________________________|
  //
  // We treat the body's border edge as the constraining frame, so therefore we normalize the
  // reported positions by subtracting the top/left position of the body element.
  // Note that the position of the popup is set using absolute positioning, which positions the
  // entire popup box (i.e., its margin edge) within the context of the body element's padding
  // edge.  We hope that nobody puts padding on the body, so we can use the border-edge as the
  // positioning context.
  //

  /**
   * Horizontal alignment strategies.
   */
  enum Horizontal {
    /** Aligns the left of the popup to the left of the relative. */
    LEFT {
      @Override
      public int getLeft(int relLeft, int relRight, int popupWidth) {
        return relLeft + WEST_CHROME_OFFSET_PX;
      }

    },
    /** Aligns the right of the popup to the right of the relative. */
    RIGHT {
      @Override
      public int getLeft(int relLeft, int relRight, int popupWidth) {
        return relRight - popupWidth - EAST_CHROME_OFFSET_PX;
      }
    };

    abstract int getLeft(int relLeft, int relRight, int popupWidth);
  }

  /**
   * Vertical alignment strategies.
   */
  enum Vertical {
    /** Aligns the top of the popup to the bottom of the relative. */
    BOTTOM {
      @Override
      public int getTop(int relTop, int relBottom, int popupHeight) {
        return relBottom + NORTH_CHROME_OFFSET_PX + VERTICAL_OFFSET_PX;
      }
    },
    /** Aligns the bottom of the popup to the top of the relative. */
    TOP {
      @Override
      public int getTop(int relTop, int relBottom, int popupHeight) {
        return relTop - popupHeight - SOUTH_CHROME_OFFSET_PX - VERTICAL_OFFSET_PX;
      }
    },
    ;

    abstract int getTop(int relTop, int relBottom, int popupHeight);
  }

  /** An above-left positioner. */
  public static final RelativePopupPositioner ABOVE_LEFT = new AlignedPopupPositioner(
      Insets.NONE, Horizontal.LEFT, Horizontal.RIGHT, Vertical.TOP, Vertical.BOTTOM);

  /** A below-left positioner. */
  public static final RelativePopupPositioner BELOW_LEFT = new AlignedPopupPositioner(
      Insets.NONE, Horizontal.LEFT, Horizontal.RIGHT, Vertical.BOTTOM, Vertical.TOP);

  /** A below-right positioner. */
  public static final RelativePopupPositioner BELOW_RIGHT = new AlignedPopupPositioner(
      Insets.NONE, Horizontal.RIGHT, Horizontal.LEFT, Vertical.BOTTOM, Vertical.TOP);

  /** An above-right positioner. */
  public static final RelativePopupPositioner ABOVE_RIGHT = new AlignedPopupPositioner(
      Insets.NONE, Horizontal.RIGHT, Horizontal.LEFT, Vertical.TOP, Vertical.BOTTOM);

  private final Insets insets;
  private final Horizontal primaryHorz;
  private final Horizontal secondaryHorz;
  private final Vertical primaryVert;
  private final Vertical secondaryVert;

  /**
   * Creates a positioner.
   *
   * @param insets insets from the border edge of the relative element, against
   *        which the popup is aligned.
   * @param primaryHorz preferred horizontal alignment
   * @param secondaryHorz alternative horizontal alignment, in case the primary
   *        would position the popup off screen
   * @param primaryVert preferred vertical alignment
   * @param secondaryVert alternative vertical alignment, in case the primary
   *        would position the popup off screen
   */
  public AlignedPopupPositioner(Insets insets, Horizontal primaryHorz, Horizontal secondaryHorz,
      Vertical primaryVert, Vertical secondaryVert) {
    this.insets = insets;
    this.primaryHorz = primaryHorz;
    this.secondaryHorz = secondaryHorz;
    this.primaryVert = primaryVert;
    this.secondaryVert = secondaryVert;
  }

  @Override
  public void setPopupPositionAndMakeVisible(Element relative, Element popup) {
    int bodyLeft = RootPanel.get().getElement().getAbsoluteLeft();
    int bodyRight = RootPanel.get().getElement().getAbsoluteRight();
    int bodyTop = RootPanel.get().getElement().getAbsoluteTop();
    int bodyBottom = RootPanel.get().getElement().getAbsoluteBottom();
    int relLeft = relative.getAbsoluteLeft() - bodyLeft + insets.left;
    int relRight = relative.getAbsoluteRight() - bodyLeft - insets.right;
    int relTop = relative.getAbsoluteTop() - bodyTop + insets.top;
    int relBottom = relative.getAbsoluteBottom() - bodyTop - insets.bottom;
    int popupWidth = popup.getOffsetWidth();
    int popupHeight = popup.getOffsetHeight();

    int left = primaryHorz.getLeft(relLeft, relRight, popupWidth);
    int right = left + popupWidth + EAST_CHROME_OFFSET_PX;
    if (left < bodyLeft || right > bodyRight) {
      // Primary alignment strategy failed. Try secondary.
      int secondaryLeft = secondaryHorz.getLeft(relLeft, relRight, popupWidth);
      int secondaryRight = secondaryLeft + popupWidth + EAST_CHROME_OFFSET_PX;
      if (secondaryLeft < bodyLeft || secondaryRight > bodyRight) {
        // Secondary alignment strategy also failed. Use clipped primary.
        left = PositionUtil.boundToScreenHorizontal(left, right - left);
      } else {
        left = secondaryLeft;
      }
    }

    int top = primaryVert.getTop(relTop, relBottom, popupHeight);
    int bottom = top + popupHeight + SOUTH_CHROME_OFFSET_PX;
    if (top < bodyTop || bottom > bodyBottom) {
      // Primary alignment strategy failed. Try secondary.
      int secondaryTop = secondaryVert.getTop(relTop, relBottom, popupHeight);
      int secondaryBottom = secondaryTop + popupHeight + SOUTH_CHROME_OFFSET_PX;
      if (secondaryTop < bodyTop || secondaryBottom > bodyBottom) {
        // Secondary alignment strategy also failed. Use clipped primary.
        top = PositionUtil.boundToScreenVertical(top, bottom - top);
      } else {
        top = secondaryTop;
      }
    }

    popup.getStyle().setLeft(left, Unit.PX);
    popup.getStyle().setTop(top, Unit.PX);
    popup.getStyle().setVisibility(Visibility.VISIBLE);
  }
}
