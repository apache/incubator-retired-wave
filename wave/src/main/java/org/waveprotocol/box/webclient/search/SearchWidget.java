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

package org.waveprotocol.box.webclient.search;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

import org.waveprotocol.wave.client.common.util.QuirksConstants;

/**
 * Widget implementation of the search area.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class SearchWidget extends Composite implements SearchView, ChangeHandler {

  /** Resources used by this widget. */
  interface Resources extends ClientBundle {
    /** CSS */
    @Source("Search.css")
    Css css();
  }

  interface Css extends CssResource {
    String self();
    String search();
    String query();
    String searchButton();
    String searchButtonsPanel();
    String searchboxContainer();
  }

  @UiField(provided = true)
  final static Css css = SearchPanelResourceLoader.getSearch().css();

  interface Binder extends UiBinder<HTMLPanel, SearchWidget> {
  }

  private final static Binder BINDER = GWT.create(Binder.class);

  private final static String DEFAULT_QUERY = "";

  @UiField
  TextBox query;
  @UiField
  Button searchButtonShared;
  @UiField
  Button searchButtonAll;
  @UiField
  Button searchButtonInbox;

  private Listener listener;

  /**
   *
   */
  public SearchWidget() {
    initWidget(BINDER.createAndBindUi(this));
    if (QuirksConstants.SUPPORTS_SEARCH_INPUT) {
      query.getElement().setAttribute("type", "search");
      query.getElement().setAttribute("results", "10");
      query.getElement().setAttribute("autosave", "QUERY_AUTO_SAVE");
    }
    query.addChangeHandler(this);
  }

  @Override
  public void init(Listener listener) {
    Preconditions.checkState(this.listener == null);
    Preconditions.checkArgument(listener != null);
    this.listener = listener;
  }

  @Override
  public void reset() {
    Preconditions.checkState(listener != null);
    listener = null;
  }

  @Override
  public String getQuery() {
    return query.getValue();
  }

  @Override
  public void setQuery(String text) {
    query.setValue(text);
  }

  @Override
  public void onChange(ChangeEvent event) {
    if (query.getValue() == null || query.getValue().isEmpty()) {
      query.setText(DEFAULT_QUERY);
    }
    onQuery();
  }
  
  private void onQuery() {
    if (listener != null) {
      listener.onQueryEntered();
    }
  }
  
  @UiHandler("searchButtonShared")
  public void onHandleShared(ClickEvent event) {
    setQuery("with:@");
    onQuery();
  }
  
  @UiHandler("searchButtonAll")
  public void onHandleAll(ClickEvent event) {
    setQuery("");
    onQuery();
  }
  
  @UiHandler("searchButtonInbox")
  public void onHandleInbox(ClickEvent event) {
    setQuery("in:inbox");
    onQuery();
  }
}
