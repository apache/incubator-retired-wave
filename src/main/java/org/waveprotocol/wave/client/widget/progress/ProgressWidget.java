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

package org.waveprotocol.wave.client.widget.progress;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.user.client.ui.Widget;

/**
 * Display a progress bar to indicate progress
 *
 *
 * NOTE(macpherson): This should cease to be a widget and should be inlined into thumbnails.
 */
public class ProgressWidget extends Widget {
  /**
   * Interface for acquiring the resources used by ProgressWidget.
   */
  interface ProgressResources extends ClientBundle {
    interface Css extends CssResource {
      String bar();
      String groove();
    }

    @Source("ProgressWidget.css")
    Css css();

    @Source("progress_mini_bar.gif")
    DataResource barImage();

    @Source("progress_mini_groove.gif")
    DataResource grooveImage();
  }

  /** The singleton instance of our resources */
  private static final ProgressResources PROGRESS_RESOURCES = GWT.create(ProgressResources.class);

  /** The top-level element */
  private final DivElement element = Document.get().createDivElement();

  /** The bar element */
  private final DivElement bar = Document.get().createDivElement();

  /** The groove element */
  private final DivElement groove = Document.get().createDivElement();

  /** The currently displayed progress in the progress bar, between 0-1 inclusive */
  private double currentProgress;

  /** Create a progress bar */
  public ProgressWidget() {
    PROGRESS_RESOURCES.css().ensureInjected();
    bar.setClassName(PROGRESS_RESOURCES.css().bar());
    groove.setClassName(PROGRESS_RESOURCES.css().groove());
    element.appendChild(bar);
    element.appendChild(groove);
    setElement(element);
    setValue(0.0);
  }

  /**
   * Update the current progress displayed in the progress bar.
   *
   * @param value Must be a number between 0 and 1.
   */
  public void setValue(double value) {
    currentProgress = Math.max(0, Math.min(value, 1));
    bar.getStyle().setWidth(currentProgress * 100, Style.Unit.PCT);
    groove.getStyle().setWidth((1 - currentProgress) * 100, Style.Unit.PCT);
  }

  /** Return the current displayed progress */
  public double getValue() {
    return currentProgress;
  }
}
