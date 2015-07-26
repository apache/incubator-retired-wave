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

package org.waveprotocol.box.webclient.widget.loading;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

/**
 * A loading indicator widget.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class LoadingIndicator {

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    @Source("loading.gif")
    ImageResource loading();

    /** CSS */
    @Source("Loading.css")
    Css css();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    String loading();

    String loadingImage();
  }

  @UiField(provided = true)
  final static Css css = GWT.<Resources> create(Resources.class).css();

  static {
    StyleInjector.inject(css.getText(), true);
  }

  interface Binder extends UiBinder<Element, LoadingIndicator> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  /** Loading indicator. */
  private final Element loading;

  /**
   * Creates a framed panel.
   */
  public LoadingIndicator() {
    loading = BINDER.createAndBindUi(this);
  }

  public Element getElement() {
    return loading;
  }
}
