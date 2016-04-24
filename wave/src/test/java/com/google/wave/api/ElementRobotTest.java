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

package com.google.wave.api;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Element unit tests.
 *
 * @author scovitz@google.com (Seth Covitz)
 */
public class ElementRobotTest extends TestCase {

  private static final String NAME = "name";
  private static final String VALUE = "value";

  public void testElementTypeConstruction() {
    Element element = new Element(ElementType.BUTTON);
    assertEquals(ElementType.BUTTON, element.getType());
    assertEquals(0, element.getProperties().size());
  }

  public void testElementTypeAndPropertyConstruction() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(NAME, VALUE);

    Element element = new Element(ElementType.GADGET, properties);
    assertEquals(ElementType.GADGET, element.getType());
    assertEquals(1, element.getProperties().size());
    assertEquals(VALUE, element.getProperty(NAME));
  }

  public void testIsFormElement() {
    assertTrue(new Element(ElementType.BUTTON).isFormElement());
    assertTrue(new Element(ElementType.CHECK).isFormElement());
    assertTrue(new Element(ElementType.INPUT).isFormElement());
    assertTrue(new Element(ElementType.PASSWORD).isFormElement());
    assertTrue(new Element(ElementType.LABEL).isFormElement());
    assertTrue(new Element(ElementType.RADIO_BUTTON).isFormElement());
    assertTrue(new Element(ElementType.RADIO_BUTTON_GROUP).isFormElement());
    assertTrue(new Element(ElementType.TEXTAREA).isFormElement());
    assertFalse(new Element(ElementType.GADGET).isFormElement());
    assertFalse(new Element(ElementType.INLINE_BLIP).isFormElement());
    assertFalse(new Element(ElementType.IMAGE).isFormElement());
    assertFalse(new Element(ElementType.ATTACHMENT).isFormElement());
  }

  public void testIsGadget() {
    assertFalse(new Element(ElementType.BUTTON).isGadget());
    assertFalse(new Element(ElementType.CHECK).isGadget());
    assertFalse(new Element(ElementType.INPUT).isGadget());
    assertFalse(new Element(ElementType.PASSWORD).isGadget());
    assertFalse(new Element(ElementType.LABEL).isGadget());
    assertFalse(new Element(ElementType.RADIO_BUTTON).isGadget());
    assertFalse(new Element(ElementType.RADIO_BUTTON_GROUP).isGadget());
    assertFalse(new Element(ElementType.TEXTAREA).isGadget());
    assertTrue(new Element(ElementType.GADGET).isGadget());
    assertFalse(new Element(ElementType.INLINE_BLIP).isGadget());
    assertFalse(new Element(ElementType.IMAGE).isGadget());
    assertFalse(new Element(ElementType.ATTACHMENT).isGadget());
  }

  public void testIsInlineBlip() {
    assertFalse(new Element(ElementType.BUTTON).isInlineBlip());
    assertFalse(new Element(ElementType.CHECK).isInlineBlip());
    assertFalse(new Element(ElementType.INPUT).isInlineBlip());
    assertFalse(new Element(ElementType.PASSWORD).isInlineBlip());
    assertFalse(new Element(ElementType.LABEL).isInlineBlip());
    assertFalse(new Element(ElementType.RADIO_BUTTON).isInlineBlip());
    assertFalse(new Element(ElementType.RADIO_BUTTON_GROUP).isInlineBlip());
    assertFalse(new Element(ElementType.TEXTAREA).isInlineBlip());
    assertFalse(new Element(ElementType.GADGET).isInlineBlip());
    assertTrue(new Element(ElementType.INLINE_BLIP).isInlineBlip());
    assertFalse(new Element(ElementType.IMAGE).isInlineBlip());
    assertFalse(new Element(ElementType.ATTACHMENT).isInlineBlip());
  }

  public void testIsImage() {
    assertFalse(new Element(ElementType.BUTTON).isImage());
    assertFalse(new Element(ElementType.CHECK).isImage());
    assertFalse(new Element(ElementType.INPUT).isImage());
    assertFalse(new Element(ElementType.PASSWORD).isImage());
    assertFalse(new Element(ElementType.LABEL).isImage());
    assertFalse(new Element(ElementType.RADIO_BUTTON).isImage());
    assertFalse(new Element(ElementType.RADIO_BUTTON_GROUP).isImage());
    assertFalse(new Element(ElementType.TEXTAREA).isImage());
    assertFalse(new Element(ElementType.GADGET).isImage());
    assertFalse(new Element(ElementType.INLINE_BLIP).isImage());
    assertFalse(new Element(ElementType.ATTACHMENT).isImage());
    assertTrue(new Element(ElementType.IMAGE).isImage());
  }

  public void testIsAttachment() {
    assertFalse(new Element(ElementType.BUTTON).isAttachment());
    assertFalse(new Element(ElementType.CHECK).isAttachment());
    assertFalse(new Element(ElementType.INPUT).isAttachment());
    assertFalse(new Element(ElementType.PASSWORD).isAttachment());
    assertFalse(new Element(ElementType.LABEL).isAttachment());
    assertFalse(new Element(ElementType.RADIO_BUTTON).isAttachment());
    assertFalse(new Element(ElementType.RADIO_BUTTON_GROUP).isAttachment());
    assertFalse(new Element(ElementType.TEXTAREA).isAttachment());
    assertFalse(new Element(ElementType.GADGET).isAttachment());
    assertFalse(new Element(ElementType.INLINE_BLIP).isAttachment());
    assertFalse(new Element(ElementType.IMAGE).isAttachment());
    assertTrue(new Element(ElementType.ATTACHMENT).isAttachment());
  }

  // Test for http://b/2133741 - ClassCastException in Element.getProperty().
  public void testGetNonStringProperty() throws Exception {
    Element element = createElementWithProperty(ElementType.IMAGE, "width", 5);
    assertEquals("5", element.getProperty("width"));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Element createElementWithProperty(ElementType type, String key, Object value) {
    Map properties = new HashMap();
    properties.put(key, value);

    return new Element(type, properties);
  }
}
