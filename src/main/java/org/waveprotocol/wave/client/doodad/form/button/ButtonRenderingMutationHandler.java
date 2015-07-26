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


package org.waveprotocol.wave.client.doodad.form.button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.doodad.form.events.ContentEvents;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;
import org.waveprotocol.wave.client.widget.button.ClickButton;
import org.waveprotocol.wave.client.widget.button.text.TextButton;
import org.waveprotocol.wave.client.widget.button.text.TextButton.TextButtonStyle;

/**
 * Gwt implementation of button rendering/mutation handler
 *
 */
public final class ButtonRenderingMutationHandler extends GwtRenderingMutationHandler {
  public interface Resources extends ClientBundle {
    @Source("Button.css")
    // TODO(danilatos/mtsui): Remove @NotStrict, present because of
    // wave-editor-off prop
    //
    // NOTE(user): We should be able to explicitly mark a class name for
    // exception from obfuscation after we switch over to gwt r6559 (ref
    //
    // TODO(danilatos): This can be done now - make it so!
    @NotStrict
    Css css();
  }

  /** CSS class names used by the button. These are used in Button.css */
  public interface Css extends CssResource {
    String button();
  }

  /** The singleton instance of our CSS resources. */
  private static final Css css;

  static {
    css = GWT.<Resources>create(Resources.class).css();
    // For unit testing using mockito all Gwt.Create() returns mocks.
    // The mock for Resources.class returns null css by default.
    if (css != null) {
      StyleInjector.inject(css.getText(), true);
    }
  }

  public ButtonRenderingMutationHandler() {
    super(Flow.INLINE);
  }

  @Override
  public void onDescendantsMutated(ContentElement element) {
    onContentEventsChanged(element);
    super.onDescendantsMutated(element);
  }

  @Override
  public void onAddedToParent(ContentElement element, ContentElement oldParent) {
    onContentEventsChanged(element);
    super.onAddedToParent(element, oldParent);
  }

  @Override
  public Element getContainerNodelet(Widget widget) {
    return ((TextButton) widget).getMiddle();
  }

  @Override
  protected Widget createGwtWidget(Renderable element) {
    ClickButton buttonLogic = new ClickButton();
    element.setProperty(Button.BUTTON_LOGIC_PROP, buttonLogic);

    TextButton template = new TextButton("", TextButtonStyle.REGULAR_BUTTON, "");
    template.setUiListener(buttonLogic.getUiEventListener());
    buttonLogic.setButtonDisplay(template);
    // Apply some extra css to make it render correctly in the editor.
    template.addStyleName(css.button());
    return template;
  }

  private void onContentEventsChanged(ContentElement element) {
    ClickButton buttonLogic = element.getProperty(Button.BUTTON_LOGIC_PROP);
    buttonLogic.getController().setDisabled(!ContentEvents.isClickingEnabled(element));
  }
}
