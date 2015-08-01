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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.client.gadget.GadgetLog.log;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;

/**
 * Class for embedding IFrame-based gadgets in the editor.
 *
 */
public class GadgetRenderer extends GwtRenderingMutationHandler {
  private static final Property<GadgetWidget> GADGET_WIDGET = Property.immutable("GadgetWidget");

  private static final Locale CURRENT_LOCALE = new SessionLocale(GWT.isClient()? LocaleInfo.getCurrentLocale().getLocaleName(): "");

  private final WaveletName waveletName;

  private final ConversationBlip blip;

  private final ObservableSupplementedWave supplement;

  private final ProfileManager profileManager;

  private final String loginName;

  /**
   * Constructor
   */
  public GadgetRenderer(WaveletName waveletName, ConversationBlip blip,
      ObservableSupplementedWave supplement, ProfileManager profileManager, String loginName) {
    super(Flow.USE_WIDGET);
    this.waveletName = waveletName;
    this.blip = blip;
    this.supplement = supplement;
    this.profileManager = profileManager;
    this.loginName = loginName;
  }

  @Override
  protected Widget createGwtWidget(Renderable element) {
    log("GadgetRenderer createGwtWidget");
    GadgetWidget widget = GadgetWidget.createGadgetWidget(
        // HACK(danilatos): Temporary cast  TODO(vadimg) eliminate dep on ContentElement
        // for the gadget widget
        (ContentElement) element, waveletName, blip, supplement,
        profileManager, CURRENT_LOCALE, loginName);
    element.setProperty(GADGET_WIDGET, widget);
    return widget.getWidget();
  }

  @Override
  public void onActivationStart(ContentElement element) {
    getWidget(element).createWidget();
    // fanoutAttrs
    fanoutChildren(element);
    onDescendantsMutated(element);
  }

  public GadgetWidget getWidget(ContentElement e) {
    return e.getProperty(GADGET_WIDGET);
  }

  @Override
  public void onChildAdded(ContentElement element, ContentNode child) {
    getWidget(element).onChildAdded(child);
    super.onChildAdded(element, child);
  }

  @Override
  public void onAttributeModified(ContentElement element, String name,
      String oldValue, String newValue) {
    getWidget(element).onAttributeModified(name, newValue);
    super.onAttributeModified(element, name, oldValue, newValue);
  }

  @Override
  public void onChildRemoved(ContentElement element, ContentNode child) {
    getWidget(element).onRemovingChild(child);
    super.onChildRemoved(element, child);
  }

  @Override
  public void onDescendantsMutated(ContentElement element) {
    getWidget(element).onDescendantsMutated();
    super.onDescendantsMutated(element);
  }

  @Override
  public void onRemovedFromParent(ContentElement element, ContentElement newParent) {
    if (newParent == null) {
      getWidget(element).setInactive();
    }
    super.onRemovedFromParent(element, newParent);
  }
}
