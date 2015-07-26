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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetCategoryType;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetInfo;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.i18n.GadgetCategoryMessages;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.i18n.GadgetSelectorMessages;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.TitleBar;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

import java.util.List;

/**
 * Selector for gadgets, allowing selection from a list and entering a custom
 * gadget URL.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class GadgetSelectorWidget extends Composite implements
    org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.Listener {
  public interface Listener {
    void onSelect(String url);
  }

  interface Binder extends UiBinder<ImplPanel, GadgetSelectorWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  private static final GadgetCategoryMessages categoryMessages = GWT.create(GadgetCategoryMessages.class);
  private static final GadgetSelectorMessages messages = GWT.create(GadgetSelectorMessages.class);

  @UiField ImplPanel self;
  @UiField DockLayoutPanel dockPanel;
  @UiField TextBox gadgetUrl;
  @UiField Button useCustom;
  @UiField FlowPanel options;
  @UiField TextBox gadgetFilter;
  @UiField ListBox categoryDropBox;

  private Listener listener;
  private GadgetInfoProvider gadgetInfoProvider;
  private GadgetInfoWidget selectedWidget = null;
  private String categoryFilter = GadgetCategoryType.ALL.getType();

  public GadgetSelectorWidget(GadgetInfoProvider provider) {
    initWidget(self = BINDER.createAndBindUi(this));
    gadgetInfoProvider = provider;
    gadgetInfoProvider.setListener(this);
    gadgetInfoProvider.startLoadingGadgetList();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Shows in a popup, and returns the popup.
   */
  public UniversalPopup showInPopup() {
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    UniversalPopup popup = PopupFactory.createPopup(
        null, new CenterPopupPositioner(), chrome, true);

    TitleBar titleBar = popup.getTitleBar();
    titleBar.setTitleText(messages.selectGadget());
    popup.add(GadgetSelectorWidget.this);

    popup.show();

    setFocusAndHeight();
    setupEventHandlers();

    return popup;
  }

  private void setFocusAndHeight() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        gadgetFilter.setFocus(true);
        dockPanel
            .setHeight((GadgetSelectorWidget.this.getParent()
                .getElement().getOffsetHeight() - 20) + "px");
      }
    });
  }

  private void setupEventHandlers() {
    // Handle live filtering of the inserted text.
    // (TODO: need to do it in a KeyUpHandler since change handler does fire
    // in response to character deletion, and the text is not changed until KeyUp)
    gadgetFilter.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        filter(gadgetFilter.getText());
      }
    });

    // Handle enter key event to select the filtered gadget.
    gadgetFilter.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        filter(gadgetFilter.getText());
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER && selectedWidget != null) {
          select(selectedWidget.getGadgetUrl());
        }
      }
    });

    // Handle enter key on the url textbox to select the inserted gadget url.
    gadgetUrl.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        String gadgetUrlText = gadgetUrl.getText();
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER && !gadgetUrlText.equals("")) {
          select(gadgetUrlText);
        }
      }
    });

    // Handle category filtering events.
    categoryDropBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        categoryFilter = categoryDropBox.getValue(categoryDropBox.getSelectedIndex());
        filter(gadgetFilter.getText());
      }
    });
  }

  public void clear() {
    options.clear();
  }

  public void addFeaturedOptions() {
    // Add the full gadget list to this widget.
    filter("");

    // Add the categories to the category drop box.
    for (GadgetCategoryType category : GadgetCategoryType.values()) {
      categoryDropBox.addItem(getCategoryName(category), category.getType());
    }
  }

  private String getCategoryName(GadgetCategoryType category) {
    switch (category) {
      case ALL: return categoryMessages.all();
      case GAME: return categoryMessages.game();
      case IMAGE: return categoryMessages.image();
      case MAP: return categoryMessages.map();
      case MUSIC: return categoryMessages.music();
      case OTHER: return categoryMessages.other();
      case PRODUCTIVITY: return categoryMessages.productivity();
      case SEARCH: return categoryMessages.search();
      case TEAM: return categoryMessages.team();
      case TIME: return categoryMessages.time();
      case TRAVEL: return categoryMessages.travel();
      case UTILITY: return categoryMessages.utility();
      case VIDEO: return categoryMessages.video();
      case VOICE: return categoryMessages.voice();
      case VOTING: return categoryMessages.voting();
      default: return category.getType();
    }
  }

  /**
   * Adds one gadget to the gadget list.
   *
   * @param gadgetInfo the gadget info to add to the list.
   */
  private void addGadgetInfo(final GadgetInfo gadgetInfo) {
    GadgetInfoWidget option = new GadgetInfoWidget();
    option.setTitle(gadgetInfo.getName());
    option.setImage(gadgetInfo.getImageUrl());
    option.setDescription(gadgetInfo.getDescription());
    option.setAuthor(gadgetInfo.getAuthor());
    option.setGadgetUrl(gadgetInfo.getGadgetUrl());
    option.setCategory1(gadgetInfo.getPrimaryCategory());
    option.setCategory2(gadgetInfo.getSecondaryCategory());
    option.setListener(new GadgetInfoWidget.Listener() {
      @Override public void onSelect() {
        select(gadgetInfo.getGadgetUrl());
      }

      @Override
      public void onMouseOver(GadgetInfoWidget widget) {
        if (selectedWidget != null) {
          selectedWidget.unMark();
        }
        selectedWidget = widget;
        widget.mark();
      }

      @Override
      public void onMouseOut(GadgetInfoWidget widget) {
        widget.unMark();
      }
    });

    options.add(option);
  }

  public void filter(String filterText) {
    if (selectedWidget != null) {
      selectedWidget.unMark();
      selectedWidget = null;
    }
    clear();
    List<GadgetInfo> gadgetList =
        gadgetInfoProvider.getGadgetInfoList(filterText, categoryFilter);

    for (GadgetInfo g : gadgetList) {
      addGadgetInfo(g);
    }

    // Mark the top filtered gadget as selected.
    if (options.getWidgetCount() > 0 && !filterText.equals("")) {
      GadgetInfoWidget widget = (GadgetInfoWidget)options.getWidget(0);
      selectedWidget = widget;
      selectedWidget.mark();
    }
  }

  @UiHandler("useCustom")
  void onClickCustom(ClickEvent event) {
    select(gadgetUrl.getText());
  }

  private void select(String url) {
    if (listener != null) {
      listener.onSelect(url);
    }
  }

  @Override
  public void onUpdate() {
    filter("");
  }
}
