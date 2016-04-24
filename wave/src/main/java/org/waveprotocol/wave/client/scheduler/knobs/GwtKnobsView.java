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



package org.waveprotocol.wave.client.scheduler.knobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;

/**
 * GWT widget implementation of a knob view.
 *
 * The view is a panel, and child {@link KnobView} components are added to
 * this panel when created.
 *
 */
public final class GwtKnobsView extends FlowPanel implements KnobsView {

  public interface Resources extends ClientBundle {
    @Source("knobs.css")
    Css css();
  }

  public interface Css extends CssResource {
    String knobPanel();
    String knob();
    String knobLabel();
    String enabled();
    String disabled();
    String knobDetails();
    String currentTitle();
    String oldTitle();
    String knobContainer();
  }

  /** The singleton instance of resources. */
  final static Css CSS = GWT.<Resources>create(Resources.class).css();

  static {
    StyleInjector.inject(CSS.getText(), true);
  }

  private final Button button = new Button();
  private final FlowPanel knobs = new FlowPanel();

  private boolean isVisible = false;

  /**
   * Creates a knob controller.
   */
  public GwtKnobsView() {
    add(button);
    add(knobs);
    addStyleName(CSS.knobContainer());
    knobs.addStyleName(CSS.knobPanel());

    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        show(!isVisible);
      }
    });

    show(true);
  }

  private void show(boolean isVisible) {
    this.isVisible = isVisible;
    if (isVisible) {
      knobs.getElement().getStyle().clearDisplay();
      button.setText("-");
    } else {
      knobs.getElement().getStyle().setDisplay(Display.NONE);
      button.setText("+");
    }
  }

  /**
   * {@inheritDoc}
   *
   * The new knob-view is created as a component of this knobs-view.
   */
  @Override
  public GwtKnobView create(Priority priority) {
    GwtKnobView component = new GwtKnobView(priority);
    knobs.add(component);
    return component;
  }
}
