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

package org.waveprotocol.wave.client.widget.popup.desktopchrome;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;

/**
 * Window borders for desktop using declarative UI.
 */
// TODO(user) Replace with UIObject once GWT supports it.
public class DesktopPopupChrome extends Composite implements PopupChrome {

  interface Resources extends ClientBundle {
    /** CSS class names used by DesktopPopupChrome. These are used in DesktopPopupChrome.css */
    interface Css extends CssResource {
      // chrome names
      String north();
      String northEast();
      String eastNorthEast();
      String east();
      String southEast();
      String south();
      String southWest();
      String west();
      String northWest();

      // state names
      String titled();
    }

    @Source("DesktopPopupChrome.css")
    Css css();

    /** Default background images */
    @Source("popup_n.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource chromeNorth();

    @Source("popup_ne.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthEast();

    @Source("popup_ene.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeEastNorthEast();

    @Source("popup_e.png")
    @ImageOptions(repeatStyle=RepeatStyle.Vertical, flipRtl=true)
    ImageResource chromeEast();

    @Source("popup_se.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeSouthEast();

    @Source("popup_s.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource chromeSouth();

    @Source("popup_sw.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeSouthWest();

    @Source("popup_w.png")
    @ImageOptions(repeatStyle=RepeatStyle.Vertical, flipRtl=true)
    ImageResource chromeWest();

    @Source("popup_nw.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthWest();

    /** Titled background images */
    @Source("popup_n_titled.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource chromeNorthTitled();

    @Source("popup_ne_titled.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthEastTitled();

    @Source("popup_nw_titled.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthWestTitled();

  }

  /** The singleton instance of our resources. */
  private static final Resources RESOURCES = GWT.create(Resources.class);

  /**
   * Inject the CSS once.
   */
  static {
    StyleInjector.inject(RESOURCES.css().getText());
  }

  interface Binder extends UiBinder<HTMLPanel, DesktopPopupChrome> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField HTMLPanel frame;

  public DesktopPopupChrome() {
    initWidget(BINDER.createAndBindUi(this));
  }

  @Override
  public void enableTitleBar() {
    frame.addStyleName(RESOURCES.css().titled());
  }

  @Override
  public Widget getChrome() {
    return this;
  }
}
