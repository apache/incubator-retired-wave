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

package org.waveprotocol.box.webclient.widget.frame;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.common.ImplPanel;

import java.util.Collections;
import java.util.Iterator;

/**
 * A frame around a widget.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class FramedPanel extends Composite implements HasWidgets {

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** Default background images */
    @Source("panel_n.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeNorth();

    @Source("panel_ne.png")
    ImageResource chromeNorthEast();

    @Source("panel_e.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource chromeEast();

    @Source("panel_se.png")
    ImageResource chromeSouthEast();

    @Source("panel_s.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeSouth();

    @Source("panel_sw.png")
    ImageResource chromeSouthWest();

    @Source("panel_w.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource chromeWest();

    @Source("panel_nw.png")
    ImageResource chromeNorthWest();

    /** CSS */
    @Source("FramedPanel.css")
    Css css();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    // Background images for the bread crumb.
    String north();

    String northEast();

    String east();

    String southEast();

    String south();

    String southWest();

    String west();

    String northWest();

    /** Whole frame. */
    String frame();

    /** Element containing the content. */
    String contentContainer();
  }

  @UiField(provided = true)
  final static Css css = GWT.<Resources> create(Resources.class).css();

  static {
    StyleInjector.inject(css.getText(), true);
  }

  interface Binder extends UiBinder<ImplPanel, FramedPanel> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  /** This widget's implementation. */
  private final ImplPanel self;
  /** Element to which contents are to be attached. */
  @UiField
  Element contentContainer;
  /** Element containing the title-bar text. */
  @UiField
  Element title;
  /** The widget in this frame, if there is one. */
  private Widget contents;

  /**
   * Creates a framed panel.
   */
  public FramedPanel() {
    initWidget(self = BINDER.createAndBindUi(this));
  }

  /**
   * Creates a framed panel around a widget.
   */
  public static FramedPanel of(Widget w) {
    FramedPanel frame = new FramedPanel();
    frame.add(w);
    return frame;
  }

  /**
   * Sets the text in the title bar.
   */
  public void setTitleText(String text) {
    title.setInnerText(text);
  }

  @Override
  public void add(Widget w) {
    Preconditions.checkState(contents == null);
    self.add(w, contentContainer);
    contents = w;
  }

  @Override
  public boolean remove(Widget w) {
    if (w == contents) {
      clear();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void clear() {
    if (contents != null) {
      contents.removeFromParent();
      contents = null;
    }
  }

  @Override
  public Iterator<Widget> iterator() {
    return Collections.singleton(contents).iterator();
  }
}
