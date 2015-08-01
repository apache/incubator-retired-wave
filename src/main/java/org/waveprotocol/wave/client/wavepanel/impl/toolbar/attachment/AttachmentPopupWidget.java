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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.attachment;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import org.waveprotocol.wave.client.wavepanel.view.AttachmentPopupView;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.media.model.AttachmentId;

/**
 * Widget implementation of the {@link AttachmentPopupView}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class AttachmentPopupWidget extends Composite implements AttachmentPopupView,
    PopupEventListener {

  interface Binder extends UiBinder<HTMLPanel, AttachmentPopupWidget> {
  }

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("AttachmentPopupWidget.css")
    Style style();

    @Source("spinner.gif")
    ImageResource spinner();
  }

  interface Style extends CssResource {

    String self();

    String title();

    String spinnerPanel();

    String spinner();

    String status();

    String error();

    String done();
  }

  private final static Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  final static Style style = GWT.<Resources> create(Resources.class).style();

  private static final String UPLOAD_ACTION_URL = "/attachment/";

  static {
    // StyleInjector's default behaviour of deferred injection messes up
    // popups, which do synchronous layout queries for positioning. Therefore,
    // we force synchronous injection.
    StyleInjector.inject(style.getText(), true);
  }

  @UiField
  FileUpload fileUpload;
  @UiField
  Button uploadBtn;
  @UiField
  FormPanel form;
  @UiField
  Hidden formAttachmentId;
  @UiField
  Hidden formWaveRef;
  @UiField
  HorizontalPanel spinnerPanel;
  @UiField
  Label status;
  @UiField
  Image spinnerImg;

  /** Popup containing this widget. */
  private final UniversalPopup popup;

  /** Optional listener for view events. */
  private Listener listener;

  private AttachmentId attachmentId;
  private String waveRefStr;

  /**
   * Creates link info popup.
   */
  public AttachmentPopupWidget() {
    initWidget(BINDER.createAndBindUi(this));
    // Because we're going to add a FileUpload widget, we'll need to set the
    // form to use the POST method, and multipart MIME encoding.
    form.setEncoding(FormPanel.ENCODING_MULTIPART);
    form.setMethod(FormPanel.METHOD_POST);

    // Add an event handler to the form.
    form.addSubmitHandler(new FormPanel.SubmitHandler() {
      @Override
      public void onSubmit(SubmitEvent event) {
        // No implementation.
      }
    });

    form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
      @Override
      public void onSubmitComplete(SubmitCompleteEvent event) {
        // When the form submission is successfully completed, this
        // event is fired. Assuming the service returned a response of type
        // text/html, we can get the result text here (see the FormPanel
        // documentation for further explanation).
        spinnerImg.setVisible(false);
        String results = event.getResults();
        if (results != null && results.contains("OK")) {
          status.setText("Done!");
          status.addStyleName(style.done());
          listener.onDone(waveRefStr, attachmentId.getId(), fileUpload.getFilename());
          hide();
        } else {
          status.setText("Error!");
          status.addStyleName(style.error());
        }
      }
    });

    uploadBtn.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        String filename = fileUpload.getFilename();
        if (filename.length() == 0) {
          Window.alert("No file to upload!");
        } else {
          spinnerPanel.setVisible(true);
          formAttachmentId.setValue(attachmentId.getId());
          formWaveRef.setValue(waveRefStr);
          form.submit();
        }
      }
    });

    // Wrap in a popup.
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    popup = PopupFactory.createPopup(null, new CenterPopupPositioner(), chrome, true);
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
  }

  @Override
  public void show() {
    Preconditions.checkState(this.attachmentId != null);
    form.setAction(UPLOAD_ACTION_URL + attachmentId.getId());
    spinnerPanel.setVisible(false);
    popup.show();
  }

  @Override
  public void hide() {
    popup.hide();
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    if (listener != null) {
      listener.onShow();
    }
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    if (listener != null) {
      listener.onHide();
    }
  }

  @Override
  public void setAttachmentId(AttachmentId id) {
    attachmentId = id;
  }

  @Override
  public void setWaveRef(String waveRefStr) {
    this.waveRefStr = waveRefStr;
  }
}
