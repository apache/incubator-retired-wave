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

package org.waveprotocol.wave.client.doodad.form.input;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.HandlerReference;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.Property;

/**
 * Password form input field
 *
 * Note that this does not allow concurrent editing of content - the password is stored in
 * a value attribute
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class Password {
  public interface Resources extends ClientBundle {
    @Source("Password.css")
    Css css();

    interface Css extends CssResource {
      String password();
    }
  }

  /***/
  public static final String TAGNAME = "password";

  /***/
  public static final String VALUE = "value";

  /** The singleton instance of our CSS resources. */
  private static final Resources.Css css = GWT.<Resources>create(Resources.class).css();

  /**
   * Registers paragraph handlers for any provided tag names / type attributes.
   */
  public static void register(ElementHandlerRegistry registry) {

    PasswordEventHandler eventHandler = new PasswordEventHandler();
    PasswordRenderer renderer = new PasswordRenderer(eventHandler);
    registry.registerEventHandler(TAGNAME, eventHandler);
    registry.registerRenderingMutationHandler(TAGNAME, renderer);
  }

  /**
   * Register schema + inject stylesheet
   */
  static {
    // For unit testing using mockito all Gwt.Create() returns mocks.
    // The mock for Resources.class returns null css by default.
    if (css != null) {
      StyleInjector.inject(css.getText(), true);
    }
  }

  static class PasswordEventHandler extends NodeEventHandlerImpl {
    boolean isMutatingLocallyToMatchUserInput = false;

    private static final Property<HandlerReference> HANDLE = Property.mutable("handle");

    @Override
    public void onActivated(final ContentElement element) {
      // Since typing in a password field updates its value attribute, not its content, our regular
      // typing extraction stuff does nothing. So here is a poor man's typing extractor just
      // for password fields...
      // NOTE(danilatos): Handling keyup isn't perfect, but should hold for now.
      element.setProperty(HANDLE, DomHelper.registerEventHandler(element.getImplNodelet(), "keyup",
          new JavaScriptEventListener() {
            @Override
            public void onJavaScriptEvent(String name, Event event) {
              handleTyping(element);
            }
          }));
    }

    @Override
    public void onDeactivated(ContentElement element) {
      // Clean up
      element.getProperty(HANDLE).unregister();
    }

    /**
     * Prevent pressing enter
     */
    @Override
    public boolean handleEnter(ContentElement element, EditorEvent event) {
      return true;
    }

    /**
     * Cancels backspace at beginning of line.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleBackspaceAtBeginning(ContentElement p, EditorEvent event) {
      return true;
    }

    /**
     * Cancels delete at end of line.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleDeleteAtEnd(ContentElement p, EditorEvent event) {
      return true;
    }

    /**
     * TODO(danilatos): Have this be called by the central event routing, not the hack
     * dom listeners below?
     *
     * @param p
     */
    public void handleTyping(ContentElement p) {
      isMutatingLocallyToMatchUserInput = true;
      try {
        p.getMutableDoc().setElementAttribute(p, "value",
            p.getImplNodelet().<InputElement>cast().getValue());
      } finally {
        isMutatingLocallyToMatchUserInput = false;
      }
    }
  }

  static class PasswordRenderer extends RenderingMutationHandler {

    private final PasswordEventHandler eventHandler;

    public PasswordRenderer(PasswordEventHandler eventHandler) {
      this.eventHandler = eventHandler;
    }

    @Override
    public Element createDomImpl(Renderable element) {
      Element passwordElement = Document.get().createPasswordInputElement();
      passwordElement.addClassName(css.password());
      return passwordElement;
    }

    @Override
    public void onActivationStart(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      if (VALUE.equals(name) && !eventHandler.isMutatingLocallyToMatchUserInput) {
        element.getImplNodelet().<InputElement>cast().setValue(newValue);
      }
    }
  }
}
