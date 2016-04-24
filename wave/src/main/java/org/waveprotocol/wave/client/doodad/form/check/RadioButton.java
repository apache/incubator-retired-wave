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

package org.waveprotocol.wave.client.doodad.form.check;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;

import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

public final class RadioButton {
  private static final String TAGNAME = "radio";
  private static final String GROUP = "group";

  private static final RenderingMutationHandler RADIO_BUTTON_MUTATING_RENDERER =
      new RenderingMutationHandler() {

    @Override
    public void onActivationStart(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public Element createDomImpl(Renderable element) {
      // Note(user): IE insists that the input element's name attribute is
      // set at creation to correctly group radio buttons
      InputElement radioInputElement =
          Document.get().createRadioInputElement("xx");
      radioInputElement.setClassName(CheckConstants.css.radio());
      radioInputElement.setTabIndex(0);
      return radioInputElement;
    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      if (GROUP.equalsIgnoreCase(name)) {
        InputElement inputElement = InputElement.as(element.getImplNodelet());
        inputElement.setName(element.getEditorUniqueString() + newValue);
      } else if (ContentElement.NAME.equalsIgnoreCase(name)) {
        EditorStaticDeps.logger.trace().log("myname: " + element.getName());
        element.getImplNodelet().setId(element.getEditorUniqueString() + newValue);
      }

      String groupName = element.getAttribute(GROUP);
      String elementName = element.getName();
      if (groupName != null && elementName != null) {
        EditorStaticDeps.logger.trace().log("myname: " + element.getName());
        ContentElement group = getGroup(element);
        if (group != null) {
          EditorStaticDeps.logger.trace().log(
              "selected: " + group.getAttribute(CheckConstants.VALUE));
          if (elementName != null && elementName.equals(group.getAttribute(CheckConstants.VALUE))) {
            setImplChecked(element, true);
          }
        } else {
          EditorStaticDeps.logger.trace().log("Cannot find associated group");
        }
      }
    }

    void setImplChecked(ContentElement element, boolean checked) {
      getImplAsInputElement(element).setChecked(checked);
    }
  };


  private static final NodeEventHandler RADIO_BUTTON_NODE_EVENT_HANDLER =
    new NodeEventHandlerImpl() {
    @Override
    public boolean handleClick(ContentElement element, EditorEvent event) {
      // Check that the click has checked a previously unchecked button
      boolean isImplChecked = getImplAsInputElement(element).isChecked();
      boolean isContentChecked =
              "true".equalsIgnoreCase(element.getAttribute(CheckConstants.VALUE));
          if (isImplChecked && !isContentChecked) {
        // Now tell group to check this button
        ContentElement group = getGroup(element);
        if (group != null) {
          RadioGroup.check(group, element);
        }
      }
      event.allowBrowserDefault();
      return true;
    }
  };

  public static void register(ElementHandlerRegistry handlerRegistry) {
    handlerRegistry.registerRenderingMutationHandler(TAGNAME, RADIO_BUTTON_MUTATING_RENDERER);
    handlerRegistry.registerEventHandler(TAGNAME, RADIO_BUTTON_NODE_EVENT_HANDLER);
  }

  /**
   * Returns the implNodelet as an InputElement or null.
   */
  public static InputElement getImplAsInputElement(ContentElement element) {
    return InputElement.as(element.getImplNodelet());
  }

  /**
   * @return group this radio button belongs to, or null TODO(user): consider
   *         friendly error if group attribute is missing
   */
  private static ContentElement getGroup(ContentElement element) {
    ContentElement group = element.getElementByName(element.getAttribute(GROUP));
    return RadioGroup.isRadioGroup(group) ? group : null;
  }

  /**
   * @param group
   * @param name
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(String group, String name) {
    return XmlStringBuilder.createEmpty().wrap(
        TAGNAME, GROUP, group, ContentElement.NAME, name);
  }

  /**
   * @param group
   * @param name
   * @param submit
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(
      String group, String name, String submit) {
    return XmlStringBuilder.createEmpty().wrap(
        TAGNAME, GROUP, group, ContentElement.NAME, name, ContentElement.SUBMIT,
        submit);
  }

  public static boolean isRadioButton(ContentElement button) {
    return EditorDocHelper.isNamedElement(button, TAGNAME);
  }
}
