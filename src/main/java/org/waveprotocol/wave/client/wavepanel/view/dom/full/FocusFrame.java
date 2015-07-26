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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.wavepanel.view.FocusFrameView;

/**
 * Focus frame DOM implementation.
 *
 */
public final class FocusFrame implements FocusFrameView {

  public interface CssEditingResource extends CssResource {
    String editing();
  }

  @UiTemplate("FocusFrameIE.ui.xml")
  interface IeBinder extends UiBinder<DivElement, FocusFrame> {

    /** Resources used by the IE rendering. */
    public interface Resources extends ClientBundle {
      @Source("FocusFrameIE.css")
      Css css();

      //
      // Normal mode chrome
      //
      @Source("frame/nw.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeNorthWest();

      @Source("frame/n.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource chromeNorth();

      @Source("frame/ne.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeNorthEast();

      @Source("frame/w.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
      ImageResource chromeWest();

      @Source("frame/e.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
      ImageResource chromeEast();

      @Source("frame/sw.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeSouthWest();

      @Source("frame/s.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource chromeSouth();

      @Source("frame/se.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeSouthEast();

      //
      // Normal mode chrome
      //
      @Source("frame/nw_edit.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeNorthWestEdit();

      @Source("frame/n_edit.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource chromeNorthEdit();

      @Source("frame/ne_edit.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeNorthEastEdit();

      @Source("frame/w_edit.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
      ImageResource chromeWestEdit();

      @Source("frame/e_edit.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
      ImageResource chromeEastEdit();

      @Source("frame/sw_edit.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeSouthWestEdit();

      @Source("frame/s_edit.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource chromeSouthEdit();

      @Source("frame/se_edit.png")
      @ImageOptions(flipRtl = true)
      ImageResource chromeSouthEastEdit();
    }

    /** CSS for this widget. */
    public interface Css extends CssEditingResource {
      // Button categories
      String editorButton();

      String doneButton();

      String draftCheckbox();

      String draftLabel();

      // Chrome states focused
      String editing();

      String display();

      // Chrome classes
      String northWest();

      String north();

      String northEast();

      String west();

      String east();

      String southEast();

      String south();

      String southWest();

      // Keyboard shortcut text that appear on button
      String shortcutButtonLabel();
    }

    @UiField(provided = true)
    Resources res = GWT.create(Resources.class);

    IeBinder INSTANCE = GWT.create(IeBinder.class);
  }

  @UiTemplate("FocusFrame.ui.xml")
  interface Css3Binder extends UiBinder<DivElement, FocusFrame> {
    interface Resources extends ClientBundle {
      @Source("FocusFrame.css")
      Css css();
    }

    /** CSS for this widget. */
    public interface Css extends CssEditingResource {
      String focus();
    }

    Resources res = GWT.create(Resources.class);

    Css3Binder INSTANCE = GWT.create(Css3Binder.class);
  }

  private static final CssEditingResource css =
      UserAgent.isIE() ? IeBinder.res.css() : Css3Binder.res.css();
  private static final UiBinder<DivElement, FocusFrame> BINDER =
      UserAgent.isIE() ? IeBinder.INSTANCE : Css3Binder.INSTANCE;

  static {
    StyleInjector.inject(css.getText(), true);
  }

  @UiField
  DivElement frame;
  private final Element element;

  /**
   * Creates a blip frame.
   */
  public FocusFrame() {
    element = BINDER.createAndBindUi(this);
  }

  public Element getElement() {
    return element;
  }

  @Override
  public void setEditing(boolean editing) {
    if (editing) {
      frame.addClassName(css.editing());
    }
    else {
      frame.removeClassName(css.editing());
    }
  }
}
