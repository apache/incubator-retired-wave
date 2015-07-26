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

import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;

/**
 * Show the popup in the center of the screen.
 *
 */
public class CenterPopupPositioner implements RelativePopupPositioner {

  /** Default height offset in PX. */
  private static final int MIN_OFFSET_HEIGHT_DEFAULT = 0;

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPopupPositionAndMakeVisible(Element relative, final Element p) {
    ScheduleCommand.addCommand(new Scheduler.Task() {
      @Override
      public void execute() {
        p.getStyle().setLeft((RootPanel.get().getOffsetWidth() - p.getOffsetWidth()) / 2, Unit.PX);
        int height = PositionUtil.boundHeightToScreen(p.getOffsetHeight());
        int top = (RootPanel.get().getOffsetHeight() - height) / 2;
        // Prevent negative top position.
        p.getStyle().setTop(Math.max(top, MIN_OFFSET_HEIGHT_DEFAULT), Unit.PX);
        p.getStyle().setHeight(height, Unit.PX);
        p.getStyle().setVisibility(Visibility.VISIBLE);
      }
    });
  }
}
