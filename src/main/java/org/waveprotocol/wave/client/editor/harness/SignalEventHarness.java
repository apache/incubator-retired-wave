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

package org.waveprotocol.wave.client.editor.harness;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEventImpl;
import org.waveprotocol.wave.client.editor.testtools.TestConstants;
import org.waveprotocol.wave.client.editor.testtools.TestConstants.EventInfo;

/**
 * Little test harness that reports signal events
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// TODO(danilatos): Move this out of the editor package
public class SignalEventHarness extends Composite {

  private final DivElement eventTestLog = Document.get().createDivElement();

  public SignalEventHarness() {
    Button clearButton = new Button("Clear", new ClickHandler() {
      public void onClick(ClickEvent e) {
        eventTestLog.setInnerHTML("");
      }
    });
    clearButton.getElement().setId(TestConstants.CLEAR_EVENT_LOG);
    FlowPanel eventMain = new FlowPanel();
    TextArea eventText = new TextArea();
    eventText.getElement().setId(TestConstants.EVENT_INPUT);
    eventTestLog.setId(TestConstants.EVENT_SIGNAL_LOG);
    eventTestLog.getStyle().setBorderColor("black");
    eventTestLog.getStyle().setBorderWidth(1, Unit.PX);
    eventTestLog.getStyle().setBorderStyle(BorderStyle.SOLID);
    for (String event : new String[] {"keydown", "keypress", "keyup"}) {
      DomHelper.registerEventHandler(eventText.getElement(), event, new JavaScriptEventListener() {
        public void onJavaScriptEvent(String name, Event event) {
          SignalEvent signal = SignalEventImpl.create(event, true);
          if (signal == null) {
            return;
          }

          addInfo(EventInfo.TYPE, name);
          addInfo(EventInfo.KEYSIGNAL, signal.getKeySignalType().toString());
          addInfo(EventInfo.KEYCODE, Integer.toString(signal.getKeyCode()));
          addInfo(EventInfo.GETSHIFT, signal.getShiftKey() + "");
          addInfo(EventInfo.GETALT, signal.getAltKey() + "");
          addInfo(EventInfo.GETCTRL, signal.getCtrlKey() + "");
          addInfo(EventInfo.GETMETA, signal.getMetaKey() + "");
          addInfo(EventInfo.GETCOMMAND, signal.getCommandKey() + "");

          eventTestLog.appendChild(Document.get().createBRElement());
        }

        private void addInfo(EventInfo info, String value) {
          SpanElement el = Document.get().createSpanElement();
          el.addClassName(info.className());
          el.setInnerText(value);
          eventTestLog.appendChild(Document.get().createTextNode(" " + info + ":"));
          eventTestLog.appendChild(el);
        }
      });
    }
    eventMain.add(eventText);
    eventMain.add(clearButton);
    eventMain.getElement().appendChild(eventTestLog);

    initWidget(eventMain);
  }
}
