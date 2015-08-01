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

package org.waveprotocol.box.webclient.stat.dialog;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import org.waveprotocol.box.stat.StatService;

import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.client.widget.dialog.DialogBox;
import org.waveprotocol.wave.client.widget.dialog.DialogBox.DialogButton;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * Dialog to show client and server profiling statistics.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class StatDialog {
  static private final String ID_ENABLE = "enable";
  static private final String ID_DISABLE = "disable";
  static private final String ID_CLEAR = "clear";

  private boolean isClient;
  private UniversalPopup popup;
  private HTMLPanel htmlPanel;
  private DialogButton targetButton;
  private DialogButton exitButton;

  static public void show() {
    StatDialog dialog = new StatDialog();
    dialog.showClientStatistic();
  }

  public StatDialog() {
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    popup = PopupFactory.createPopup(
        Document.get().getElementById("app"), new CenterPopupPositioner(), chrome, true);
    htmlPanel = new HTMLPanel("");
    htmlPanel.addDomHandler(new ClickHandler(){

      @Override
      public void onClick(ClickEvent event) {
        Element e = event.getNativeEvent().getEventTarget().cast();
        if (e.getTagName().toLowerCase().equals("a")) {
          event.preventDefault();
          if (isClient) {
            if (ID_ENABLE.equals(e.getId()) || ID_DISABLE.equals(e.getId())) {
              Timing.setEnabled(!Timing.isEnabled());
              showClientStatistic();
            } else if (ID_CLEAR.equals(e.getId())) {
              Timing.clearStatistics();
              showClientStatistic();
            }
          } else {
            String href = e.getPropertyString("href");
            int index = href.lastIndexOf('/');
            if (index != -1) {
              showUrl(StatService.STAT_URL + href.substring(index+1));
            }
          }
        }
      }
    }, ClickEvent.getType());

    ScrollPanel scroll = new ScrollPanel(htmlPanel);
    scroll.setSize(RootPanel.get().getOffsetWidth()-100 + "px",
            RootPanel.get().getOffsetHeight()-200 + "px");
    targetButton = new DialogBox.DialogButton("", new Command() {

      @Override
      public void execute() {
        if (isClient) {
          showServerStatistic();
        } else {
          showClientStatistic();
        }
      }
    });
    exitButton = new DialogBox.DialogButton("Exit", new Command() {

      @Override
      public void execute() {
        popup.hide();
      }
    });
    DialogBox.create(popup, "", scroll,
        new DialogBox.DialogButton[] { targetButton, exitButton });
  }

  private void showClientStatistic() {
    isClient = true;
    popup.getTitleBar().setTitleText("Client statistic");
    String control =
        (Timing.isEnabled()?
        "<a id=\"" + ID_DISABLE + "\" href>Disable profiling</a>":
        "<a id=\"" + ID_ENABLE + "\" href>Enable profiling</a>");
    String clear = "<a id=\"" + ID_CLEAR + "\" href>Clear</a>";
    show(control + " | " + clear + Timing.renderGlobalStatistics());
    targetButton.setTitle("Server statistic");
    popup.show();
  }

  private void showServerStatistic() {
    isClient = false;
    popup.getTitleBar().setTitleText("Server statistic");
    showUrl(StatService.STAT_URL);
    targetButton.setTitle("Client statistic");
    popup.show();
  }

  private void clear() {
    htmlPanel.getElement().setInnerHTML("");
  }

  private void showUrl(String url) {
    clear();
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);

    requestBuilder.setCallback(new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {
        show(response.getText());
      }

      @Override
      public void onError(Request request, Throwable ex) {
        Window.alert(ex.getMessage());
      }
    });
    try {
      requestBuilder.send();
    } catch (RequestException ex) {
      Window.alert(ex.getMessage());
    }
  }

  private void show(final String html) {
    htmlPanel.getElement().setInnerHTML("<div style='padding: 10px'>" + html + "</div>");
  }
}
