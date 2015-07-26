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

package org.waveprotocol.wave.client.widget.profile;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.client.widget.profile.i18n.ProfilePopupMessages;

/**
 * Widget implementation of a profile card.
 *
 */
public final class ProfilePopupWidget extends Composite
    implements ProfilePopupView, PopupEventListener {

  interface Binder extends UiBinder<ImplPanel, ProfilePopupWidget> {
  }

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("ProfilePopupWidget.css")
    Style style();
  }

  interface Style extends CssResource {
    String self();

    String state();

    String avatar();

    String details();

    String name();

    String extra();

    String label();

    String separator();

    String controls();

    String button();
  }

  private final static Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  final static Style style = GWT.<Resources> create(Resources.class).style();

  static {
    // StyleInjector's default behaviour of deferred injection messes up
    // popups, which do synchronous layout queries for positioning. Therefore,
    // we force synchronous injection.
    StyleInjector.inject(style.getText(), true);
  }

  @UiField
  ImplPanel self;
  @UiField
  ImageElement avatar;
  @UiField
  Element name;
  @UiField
  Element address;
  @UiField
  Element controls;

  /** Popup containing this widget. */
  private final UniversalPopup popup;

  /** Optional listener for view events. */
  private Listener listener;

  /**
   * Creates a profile card.
   */
  public ProfilePopupWidget(Element relative, RelativePopupPositioner positioner) {
    initWidget(BINDER.createAndBindUi(this));

    // Wrap in a popup.
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    popup = PopupFactory.createPopup(relative, positioner, chrome, true);
    popup.add(this);
    popup.addPopupEventListener(this);
  }

  @Override
  public void init(Listener listener) {
    Preconditions.checkState(this.listener == null);
    Preconditions.checkArgument(listener != null);
    this.listener = listener;
  }

  @Override
  public void reset() {
    Preconditions.checkState(this.listener != null);
    this.listener = null;
    avatar.setSrc(null);
    name.setInnerText(null);
    address.setInnerText(null);
    for (Widget child : self.getChildren()) {
      child.removeFromParent();
    }
  }

  public void show() {
    popup.show();
  }

  public void hide() {
    popup.hide();
  }

  @Override
  public void addButton(SafeHtml label, ClickHandler handler) {
    Button button = new Button(label.asString(), handler);
    button.setStyleName(style.button());
    self.add(button, controls);
  }

  @Override
  public void setAvatarUrl(String url) {
    avatar.setSrc(url);
  }

  @Override
  public void setName(String name) {
    this.name.setInnerText(name);
  }

  @Override
  public void setAddress(String address) {
    this.address.setInnerText(address);
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    if (listener != null) {
      listener.onHide();
    }
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    if (listener != null) {
      listener.onShow();
    }
  }
}
