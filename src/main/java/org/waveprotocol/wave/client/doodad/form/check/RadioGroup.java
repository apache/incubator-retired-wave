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

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeMutationHandler;
import org.waveprotocol.wave.client.editor.NodeMutationHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.NullRenderer;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Group of radio buttons
 */
public final class RadioGroup {
  private static final String TAGNAME = "radiogroup";

  private static final NodeMutationHandler<ContentNode, ContentElement>
      RADIO_GROUP_MUTATION_HANDLER =
      new NodeMutationHandlerImpl<ContentNode, ContentElement>() {

    @Override
    public void onActivationStart(ContentElement element) {
      RenderingMutationHandler.fanoutAttrs(this, element);
    }

    /**
     * Bring implementation checked state up to date with group's
     * checked attribute
     */
    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
      String newValue) {
      if (CheckConstants.VALUE.equalsIgnoreCase(name)) {
        ContentElement checked = getButtonByName(element, newValue);
        if (checked != null) {
          RadioButton.getImplAsInputElement(checked).setChecked(true);
        }
      }
    }

    /**
     * @param name
     * @return button by name
     * TODO(user): consider gentle error if no button exists,
     * or if button doesn't belong to this group
     */
    private ContentElement getButtonByName(ContentElement element, String name) {
      ContentElement button = element.getElementByName(name);
      return (button != null && RadioButton.isRadioButton(button)) ? button : null;
    }
  };

  /**
   * Registers subclasses
   */
  public static void register(ElementHandlerRegistry handlerRegistry) {
    handlerRegistry.registerRenderer(TAGNAME, NullRenderer.INSTANCE);
    handlerRegistry.registerMutationHandler(TAGNAME, RADIO_GROUP_MUTATION_HANDLER);
  }

  /**
   * @param name
   * @param value
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(String name, String value) {
    return XmlStringBuilder.createEmpty().wrap(
        TAGNAME, ContentElement.NAME, name, CheckConstants.VALUE, value);
  }

  /**
   * @param name
   * @param value
   * @return A content xml string containing a checkbox
   */
  public static XmlStringBuilder constructXml(String name, String value, String submit) {
    return XmlStringBuilder.createEmpty().wrap(TAGNAME,
        ContentElement.NAME, name, CheckConstants.VALUE, value, ContentElement.SUBMIT, submit);
  }

  public static boolean isRadioGroup(ContentElement element) {
    return EditorDocHelper.isNamedElement(element, TAGNAME);
  }

  /**
   * Checks a radio button in the group by setting the content attribute
   */
  public static void check(ContentElement group, ContentElement checkedEntry) {
    group.getMutableDoc().setElementAttribute(group, CheckConstants.VALUE, checkedEntry.getName());
  }
}
