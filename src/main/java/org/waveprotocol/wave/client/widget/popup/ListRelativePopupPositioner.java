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
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.RootPanel;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;

/**
 * Class for positioning a popup relative to an element residing in a horizontal or vertical list.
 *
 */
public class ListRelativePopupPositioner implements RelativePopupPositioner {
  /** Offset to use when positioning popup */
  private static final int PIXEL_OFFSET = 10;

  /** Strategy instance for vertical-list positioning. */
  public static final ListRelativePopupPositioner VERTICAL = new ListRelativePopupPositioner(false);

  /** Strategy instance for horizontal-list positioning. */
  public static final ListRelativePopupPositioner HORIZONTAL
      = new ListRelativePopupPositioner(true);

  private final boolean inHorizontalList;

  /**
   * Create a new ListRelativePopupPostioner.
   *
   * @param inHorizontalList True if the relative element is in a horizontal list.
   */
  private ListRelativePopupPositioner(boolean inHorizontalList) {
    this.inHorizontalList = inHorizontalList;
  }

  /**
   * {@inheritDoc}
   */
  public void setPopupPositionAndMakeVisible(final Element relative, final Element p) {
    ScheduleCommand.addCommand(new Scheduler.Task() {
      public void execute() {
        Style s = p.getStyle();
        int horizontalCenter = RootPanel.get().getOffsetWidth() / 2;
        int verticalCenter = RootPanel.get().getOffsetHeight() / 2;
        int left = relative.getAbsoluteLeft();
        int right = relative.getAbsoluteRight();
        int top = relative.getAbsoluteTop();
        int bottom = relative.getAbsoluteBottom();
        if (inHorizontalList) {
          // Place popup above or below relative
          if (right > horizontalCenter) {
            // Place popup left of relative's right
            s.setRight(RootPanel.get().getOffsetWidth() - right + PIXEL_OFFSET, Unit.PX);
            if (top < verticalCenter) {
              // Place popup below bottom of relative
              s.setTop(bottom, Unit.PX);
            } else {
              // Place popup above top of relative
              s.setBottom(RootPanel.get().getOffsetHeight() - top, Unit.PX);
            }
          } else {
            // Place popup right of relative's left
            s.setLeft(left + PIXEL_OFFSET, Unit.PX);
            if (top < verticalCenter) {
              // Place popup below bottom of relative
              s.setTop(bottom, Unit.PX);
            } else {
              // Place popup above top of relative
              s.setBottom(RootPanel.get().getOffsetHeight() - top, Unit.PX);
            }
          }
        } else {
          // Place popup on left or right side of relative
          if (right > horizontalCenter) {
            // Place popup left of relative's left
            s.setRight(RootPanel.get().getOffsetWidth() - left, Unit.PX);
            if (top < verticalCenter) {
              // Place popup below top of relative
              s.setTop(top + PIXEL_OFFSET, Unit.PX);
            } else {
              // Place popup above bottom of relative
              s.setBottom(RootPanel.get().getOffsetHeight() - bottom + PIXEL_OFFSET, Unit.PX);
            }
          } else {
            // Place popup right of relative's right
            s.setLeft(right, Unit.PX);
            if (top < verticalCenter) {
              // Place popup below top of relative
              s.setTop(top + PIXEL_OFFSET, Unit.PX);
            } else {
              // Place popup above bottom of relative
              s.setBottom(RootPanel.get().getOffsetHeight() - bottom + PIXEL_OFFSET, Unit.PX);
            }
          }
        }
        p.getStyle().setVisibility(Visibility.VISIBLE);
      }
    });
  }
}
