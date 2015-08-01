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

/**
 * FormElement unit tests.
 *
 * @author scovitz@google.com (Seth Covitz)
 */
public class FormElementRobotTest extends TestCase {

  private static final String CHECKED = "checked";
  private static final String DEFAULT = "default";
  private static final String DEFAULT_VALUE = "defaultValue";
  private static final String EMPTY_STRING = "";
  private static final String MY_BUTTON = "myButton";
  private static final String MY_CHECK = "myCheck";
  private static final String NAME = "name";
  private static final String VALUE = "value";

  public void testTypeConstructor() {
    FormElement formElement = new FormElement(ElementType.BUTTON);
    assertEquals(ElementType.BUTTON, formElement.getType());
    // without null check
    assertEquals(EMPTY_STRING, formElement.getProperty(NAME));
    assertEquals(EMPTY_STRING, formElement.getProperty(DEFAULT_VALUE));
    assertEquals(EMPTY_STRING, formElement.getProperty(VALUE));
    // with null check
    assertEquals(EMPTY_STRING, formElement.getName());
    assertEquals(EMPTY_STRING, formElement.getDefaultValue());
    assertEquals(EMPTY_STRING, formElement.getValue());
  }

  public void testTypeConstructorNonFormElement() {
    FormElement formElement = new FormElement(ElementType.GADGET);
    // This is currently allowed as FormElement is mostly a convenience
    // wrapper around an Element.
    assertEquals(ElementType.GADGET, formElement.getType());
  }

  public void testTypeAndNameConstructor() {
    FormElement formElement = new FormElement(ElementType.BUTTON, MY_BUTTON);
    assertEquals(ElementType.BUTTON, formElement.getType());
    // without null check
    assertEquals(MY_BUTTON, formElement.getProperty(NAME));
    assertEquals(EMPTY_STRING, formElement.getProperty(DEFAULT_VALUE));
    assertEquals(EMPTY_STRING, formElement.getProperty(VALUE));
    // with null check
    assertEquals(MY_BUTTON, formElement.getName());
    assertEquals(EMPTY_STRING, formElement.getDefaultValue());
    assertEquals(EMPTY_STRING, formElement.getValue());
  }

  public void testTypeNameAndDefaultValueConstructor() {
    FormElement formElement = new FormElement(ElementType.CHECK, MY_CHECK, CHECKED);
    assertEquals(ElementType.CHECK, formElement.getType());
    // without null check
    assertEquals(MY_CHECK, formElement.getProperty(NAME));
    assertEquals(CHECKED, formElement.getProperty(DEFAULT_VALUE));
    // NOTE: Value initialized to Default Value with this Constructor.
    assertEquals(CHECKED, formElement.getProperty(VALUE));
    // with null check
    assertEquals(MY_CHECK, formElement.getName());
    assertEquals(CHECKED, formElement.getDefaultValue());
    assertEquals(CHECKED, formElement.getValue());
  }

  public void testFullArgumentConstructor() {
    FormElement formElement = new FormElement(ElementType.INPUT, NAME, DEFAULT, VALUE);
    assertEquals(ElementType.INPUT, formElement.getType());
    // without null check
    assertEquals(NAME, formElement.getProperty(NAME));
    assertEquals(DEFAULT, formElement.getProperty(DEFAULT_VALUE));
    assertEquals(VALUE, formElement.getProperty(VALUE));
    // with null check
    assertEquals(NAME, formElement.getName());
    assertEquals(DEFAULT, formElement.getDefaultValue());
    assertEquals(VALUE, formElement.getValue());
  }

  public void testCopyConstructor() {
    FormElement formElement = new FormElement(ElementType.INPUT, NAME, DEFAULT, VALUE);
    FormElement formElement2 = new FormElement(formElement);
    assertEquals(ElementType.INPUT, formElement2.getType());
    // without null check
    assertEquals(NAME, formElement2.getProperty(NAME));
    assertEquals(DEFAULT, formElement2.getProperty(DEFAULT_VALUE));
    assertEquals(VALUE, formElement2.getProperty(VALUE));
    // with null check
    assertEquals(NAME, formElement2.getName());
    assertEquals(DEFAULT, formElement2.getDefaultValue());
    assertEquals(VALUE, formElement2.getValue());
  }

  public void testGettersAndSetters() {
    FormElement formElement = new FormElement(ElementType.BUTTON);
    formElement.setName(NAME);
    assertEquals(NAME, formElement.getName());
    formElement.setDefaultValue(DEFAULT);
    assertEquals(DEFAULT, formElement.getDefaultValue());
    formElement.setValue(VALUE);
    assertEquals(VALUE, formElement.getValue());
    // Verify no cross property pollution.
    assertEquals(DEFAULT, formElement.getDefaultValue());
    assertEquals(DEFAULT, formElement.getDefaultValue());
    assertEquals(VALUE, formElement.getValue());
  }
}
