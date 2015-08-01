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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collection;

/**
 * GWT widget implementation of a single level UI control.
 * The component is implemented as a label with a click handler.
 *
 */
public final class GwtKnobView extends Composite implements ClickHandler, KnobView {

  private static final String UP_ARROW = "\u25B2";
  private static final String DOWN_ARROW = "\u25BC";

  // UI fields
  private final FlowPanel knobPanel = new FlowPanel();
  private final Label label = new Label();
  private final FlowPanel detailsPanel = new FlowPanel() {
    {
      addDomHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          onDetailsClicked(event.getNativeEvent().getEventTarget().<Element>cast());
        }
      }, ClickEvent.getType());
    }
  };
  private final HTML currentTasks = new HTML();
  private final HTML oldTasks = new HTML();
  private final Button detailsButton = new Button();
  private final Button clearButton = new Button("X");

  /** Level being displayed. */
  private final Priority level;

  /** Listener, set between {@link #init(Listener)} and {@link #reset()}. */
  private Listener listener;

  /**
   * Creates a priority-level controller.
   *
   * @param level     level to control
   */
  GwtKnobView(Priority level) {
    this.level = level;

    initWidget(knobPanel);
    getElement().addClassName(GwtKnobsView.CSS.knob());

    knobPanel.add(label);
    label.addClickHandler(this);
    label.getElement().addClassName(GwtKnobsView.CSS.knobLabel());

    knobPanel.add(detailsButton);
    detailsButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        listener.onDetailsClicked();
      }
    });

    knobPanel.add(clearButton);
    clearButton.setTitle("Clear Details");
    clearButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        listener.onClearClicked();
      }
    });

    knobPanel.add(detailsPanel);
    detailsPanel.setStyleName(GwtKnobsView.CSS.knobDetails());
    detailsPanel.getElement().getStyle().setDisplay(Display.NONE);

    Label currentTitle = new Label("CURRENT JOBS");
    currentTitle.addStyleName(GwtKnobsView.CSS.currentTitle());
    detailsPanel.add(currentTitle);

    detailsPanel.add(currentTasks);

    Label oldTitle = new Label("PREVIOUS JOBS");
    oldTitle.addStyleName(GwtKnobsView.CSS.oldTitle());
    detailsPanel.add(oldTitle);

    detailsPanel.add(oldTasks);
  }

  private void onDetailsClicked(Element targetElement) {
    listener.onToggleTask(targetElement.getInnerText());
  }

  @Override
  public void init(Listener listener) {
    this.listener = listener;
  }

  @Override
  public void reset() {
    this.listener = null;
  }

  @Override
  public void enable() {
    getElement().replaceClassName(GwtKnobsView.CSS.disabled(), GwtKnobsView.CSS.enabled());
  }

  @Override
  public void disable() {
    getElement().replaceClassName(GwtKnobsView.CSS.enabled(), GwtKnobsView.CSS.disabled());
 }

  @Override
  public void showCount(int count) {
    label.setText(level + ": " + count);
  }

  @Override
  public void onClick(ClickEvent event) {
    if (listener != null) {
      listener.onClicked();
    }
  }

  @Override
  public void hideJobs() {
    detailsButton.setText(DOWN_ARROW);
    detailsPanel.getElement().getStyle().setDisplay(Display.NONE);
  }

  @Override
  public void showJobs(Collection<String> currentJobs, Collection<String> oldJobs,
      ReadableStringSet suppressedJobs) {
    detailsButton.setText(UP_ARROW);
    detailsPanel.getElement().getStyle().clearDisplay();

    StringBuilder b = new StringBuilder();
    for (String name : currentJobs) {
      b.append(renderTask(name, suppressedJobs.contains(name)));
      b.append("<br/>");
    }
    currentTasks.setHTML(b.toString());

    b = new StringBuilder();
    for (String name : oldJobs) {
      b.append(renderTask(name, suppressedJobs.contains(name)));
      b.append("<br/>");
    }
    oldTasks.setHTML(b.toString());
  }

  private String renderTask(String name, boolean isSuppressed) {
    // NOTE(danilatos): Sanity check because we are using StringBuilder instead of
    // safe html builder. 'name' is the only "external" variable. I don't expect
    // this check to ever fail given that it should be a class name. This is actually
    // safer than safe html because as of this writing the append text method
    // has a todo for validity checking, but it is unimplemented!?

    Preconditions.checkArgument(!name.contains("<"), "Task name cannot contain HTML");
    return isSuppressed ? "<s>" + name + "</s>" : "<span>" + name + "</span>";
  }
}
