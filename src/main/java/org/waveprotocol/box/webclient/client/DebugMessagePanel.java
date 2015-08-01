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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.events.ClientEvents;
import org.waveprotocol.wave.client.events.DebugMessageEvent;
import org.waveprotocol.wave.client.events.DebugMessageEventHandler;

import java.util.Date;

public class DebugMessagePanel extends Composite {
  interface Binder extends UiBinder<Widget, DebugMessagePanel> {
  }

  interface Style extends CssResource {
    String entry();

    String info();

    String severe();
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField
  HTMLPanel panel;

  @UiField
  Style style;

  public DebugMessagePanel() {
    initWidget(BINDER.createAndBindUi(this));
  }

  public void enable() {
    ClientEvents.get().addDebugMessageHandler(new DebugMessageEventHandler() {
      @Override
      public void onDebugMessage(DebugMessageEvent event) {
        StringBuilder message = new StringBuilder();
        message.append(new Date());
        message.append("[").append(event.getSeverity().toString()).append("] ");
        message.append(event.getMessage()).append("\n");
        Throwable error = event.getError();
        while (error != null) {
          message.append(error.getMessage()).append("\n");
          for (StackTraceElement elt : error.getStackTrace()) {
            message.append("  ").append(elt.getClassName()).append(".").append(
                elt.getMethodName()).append(" (").append(elt.getFileName()).append(
                ":").append(elt.getLineNumber()).append(")\n");
          }
          error = error.getCause();
          if (error != null) {
            message.append("Caused by: ");
          }
        }

        SpanElement elt = Document.get().createSpanElement();
        elt.setInnerText(message.toString());
        switch (event.severity) {
          case SEVERE:
            elt.setClassName(style.entry() + " " + style.severe());
            break;
          case INFO:
            elt.setClassName(style.entry() + " " + style.info());
            break;
        }

        panel.getElement().appendChild(elt);
        elt.scrollIntoView();
      }
    });
  }
}
