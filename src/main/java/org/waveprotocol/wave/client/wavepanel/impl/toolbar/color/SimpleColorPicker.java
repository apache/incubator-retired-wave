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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.color;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * The Class SimpleColorPicker implements a color picker similar to the Google docs one.
 */
public class SimpleColorPicker extends AbstractColorPicker {

  /** The Constant COLS. */
  private static final int COLS = 10;

  /** The Constant ROWS. */
  private static final int ROWS = 8;

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    @Source("SimpleColorPicker.css")
    Style style();
  }


  /**
   * The Interface Style.
   */
  interface Style extends CssResource {

    String grid();

    String firstRow();

    String simplecolorbutton();

  }

  /** The Constant COLORS. */
  private static final String[] COLORS = new String[] { "rgb(0, 0, 0)", "rgb(67, 67, 67)",
      "rgb(102, 102, 102)", "rgb(153, 153, 153)", "rgb(183, 183, 183)", "rgb(204, 204, 204)",
      "rgb(217, 217, 217)", "rgb(239, 239, 239)", "rgb(243, 243, 243)", "rgb(255, 255, 255)",
      "rgb(152, 0, 0)", "rgb(255, 0, 0)", "rgb(255, 153, 0)", "rgb(255, 255, 0)", "rgb(0, 255, 0)",
      "rgb(0, 255, 255)", "rgb(74, 134, 232)", "rgb(0, 0, 255)", "rgb(153, 0, 255)", "rgb(255, 0, 255)",
      "rgb(230, 184, 175)", "rgb(244, 204, 204)", "rgb(252, 229, 205)", "rgb(255, 242, 204)",
      "rgb(217, 234, 211)", "rgb(208, 224, 227)", "rgb(201, 218, 248)", "rgb(207, 226, 243)",
      "rgb(217, 210, 233)", "rgb(234, 209, 220)", "rgb(221, 126, 107)", "rgb(234, 153, 153)",
      "rgb(249, 203, 156)", "rgb(255, 229, 153)", "rgb(182, 215, 168)", "rgb(162, 196, 201)",
      "rgb(164, 194, 244)", "rgb(159, 197, 232)", "rgb(180, 167, 214)", "rgb(213, 166, 189)",
      "rgb(204, 65, 37)", "rgb(224, 102, 102)", "rgb(246, 178, 107)", "rgb(255, 217, 102)",
      "rgb(147, 196, 125)", "rgb(118, 165, 175)", "rgb(109, 158, 235)", "rgb(111, 168, 220)",
      "rgb(142, 124, 195)", "rgb(194, 123, 160)", "rgb(166, 28, 0)", "rgb(204, 0, 0)",
      "rgb(230, 145, 56)", "rgb(241, 194, 50)", "rgb(106, 168, 79)", "rgb(69, 129, 142)",
      "rgb(60, 120, 216)", "rgb(61, 133, 198)", "rgb(103, 78, 167)", "rgb(166, 77, 121)",
      "rgb(133, 32, 12)", "rgb(153, 0, 0)", "rgb(180, 95, 6)", "rgb(191, 144, 0)", "rgb(56, 118, 29)",
      "rgb(19, 79, 92)", "rgb(17, 85, 204)", "rgb(11, 83, 148)", "rgb(53, 28, 117)", "rgb(116, 27, 71)",
      "rgb(91, 15, 0)", "rgb(102, 0, 0)", "rgb(120, 63, 4)", "rgb(127, 96, 0)", "rgb(39, 78, 19)",
      "rgb(12, 52, 61)", "rgb(28, 69, 135)", "rgb(7, 55, 99)", "rgb(32, 18, 77)", "rgb(76, 17, 48)" };

  /** The Constant CELL_SIZE defines the size of each color button. */
  private static final String CELL_SIZE = "13px";

  /** The Constant style. */
  final static Style style = GWT.<Resources> create(Resources.class).style();

  /**
   * Instantiates a new simple color picker.
   *
   * @param colopicker the colopicker
   */
  public SimpleColorPicker(ComplexColorPicker colopicker) {
    super(colopicker);
    style.ensureInjected();

    final Grid grid = new Grid(ROWS, COLS);
    grid.setCellSpacing(0);
    grid.getRowFormatter().getElement(0).addClassName(style.firstRow());
    grid.getRowFormatter().getElement(1).addClassName(style.firstRow());
    int row;
    int col;
    int num = 0;
    for (final String c : COLORS) {
      row = num / COLS;
      col = num % COLS;
      grid.setWidget(row, col, createCell(c));
      num++;
    }
    grid.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        Cell cell = grid.getCellForEvent(event);
        if (cell != null) {
          String color = COLORS[cell.getRowIndex() * COLS + cell.getCellIndex()];
          onColorChoose(color);
        }
      }
    });
    grid.addStyleName(style.grid());
    initWidget(grid);
  }

  /**
   * Creates the cell for each color.
   *
   * @param color the color
   * @return the widget
   */
  private Widget createCell(final String color) {
    final PushButton button = new PushButton();
    button.setStylePrimaryName(style.simplecolorbutton());
    button.setSize(CELL_SIZE, CELL_SIZE);
    button.getElement().getStyle().setBackgroundColor(color);
    return button;
  }

}
