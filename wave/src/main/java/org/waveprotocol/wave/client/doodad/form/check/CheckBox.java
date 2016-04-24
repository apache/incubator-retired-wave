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

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.SpanElement;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/** Doodad definition for a checkbox which stores its value in the doc. */
public class CheckBox {
  private static final String TAGNAME = "check";


  private static final NodeEventHandler CHECKBOX_NODE_EVENT_HANDLER = new NodeEventHandlerImpl() {
    @Override
    public boolean handleClick(ContentElement element, EditorEvent event) {
      setChecked(element, !getChecked(element));
      event.allowBrowserDefault();
      return true;
    }
  };

  /**
   * Registers subclasses
   */
  public static void register(ElementHandlerRegistry handlerRegistry) {
    RenderingMutationHandler renderingMutationHandler =
        CheckBoxRendereringMutationHandler.getInstance();
    handlerRegistry.registerRenderingMutationHandler(TAGNAME, renderingMutationHandler);
    handlerRegistry.registerEventHandler(TAGNAME, CHECKBOX_NODE_EVENT_HANDLER);
  }

  /**
   * @param name
   * @param value
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(String name, boolean value) {
    return XmlStringBuilder.createEmpty().wrap(
        TAGNAME, ContentElement.NAME, name, CheckConstants.VALUE, String.valueOf(value));
  }

  /**
   * @param name
   * @param value
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(String name, String submit, boolean value) {
    return XmlStringBuilder.createEmpty().wrap(TAGNAME, ContentElement.SUBMIT,
        submit, ContentElement.NAME, name, CheckConstants.VALUE, String.valueOf(value));
  }

  private static class CheckBoxRendereringMutationHandler extends RenderingMutationHandler {
    private static CheckBoxRendereringMutationHandler instance;
    public static RenderingMutationHandler getInstance() {
      if (instance == null) {
        instance = new CheckBoxRendereringMutationHandler();
      }
      return instance;
    }

    @Override
    public Element createDomImpl(Renderable element) {
      InputElement inputElem = Document.get().createCheckInputElement();
      inputElem.setClassName(CheckConstants.css.check());

      // Wrap in non-editable span- Firefox does not fire events for checkboxes
      // inside contentEditable region.
      SpanElement nonEditableSpan = Document.get().createSpanElement();
      DomHelper.setContentEditable(nonEditableSpan, false, false);
      nonEditableSpan.appendChild(inputElem);

      return nonEditableSpan;
    }

    @Override
    public void onActivationStart(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      if (CheckConstants.VALUE.equalsIgnoreCase(name)) {
        updateCheckboxDom(element, getChecked(element));
      }
    }

    private void updateCheckboxDom(ContentElement checkbox, boolean isChecked) {
      Element implNodelet = checkbox.getImplNodelet();
      InputElement checkboxElem = (InputElement) implNodelet.getFirstChild();
      checkboxElem.setChecked(isChecked);
    }
  }

  /**
   * @return the value of the checkbox.
   * @param checkbox
   */
  public static boolean getChecked(ContentElement checkbox) {
    Preconditions.checkArgument(isCheckBox(checkbox), "Argument is not a checkbox");
    return Boolean.valueOf(checkbox.getAttribute(CheckConstants.VALUE));
  }

  /**
   * Sets the value of the checkbox
   * @param checkbox
   * @param checkValue
   */
  public static void setChecked(ContentElement checkbox, boolean checkValue) {
    Preconditions.checkArgument(isCheckBox(checkbox), "Argument is not a checkbox");
    checkbox.getMutableDoc().setElementAttribute(checkbox, CheckConstants.VALUE,
        String.valueOf(checkValue));
  }

  /**
   * @param element
   * @return true iff the element is a checkbox element
   */
  public static boolean isCheckBox(ContentElement element) {
    return TAGNAME.equalsIgnoreCase(element.getTagName());
  }

  /** Utility class */
  private CheckBox() {
  }
}
