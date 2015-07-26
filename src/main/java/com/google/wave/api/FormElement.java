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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Form Elements allow users and robots to build forms for other users to
 * interact with. For each element you can specify its type, name, a default
 * value and a label. The current value of the element is stored with in.
 */
public class FormElement extends Element {

  public static final String DEFAULT_VALUE = "defaultValue";
  public static final String NAME = "name";
  public static final String VALUE = "value";

  private static final List<ElementType> FORM_ELEMENT_TYPES = new ArrayList<ElementType>(8);
  static {
    Collections.addAll(FORM_ELEMENT_TYPES, ElementType.BUTTON, ElementType.CHECK,
        ElementType.INPUT, ElementType.PASSWORD, ElementType.LABEL, ElementType.RADIO_BUTTON,
        ElementType.RADIO_BUTTON_GROUP, ElementType.TEXTAREA);
  }

  /**
   * Constructs a form element of the given type.
   */
  public FormElement(ElementType type) {
    this(type, "", "", "");
  }

  /**
   * Constructs a form element given a type and name.
   */
  public FormElement(ElementType type, String name) {
    this(type, name, "", "");
  }

  /**
   * Constructs a form element given a type, name and default value.
   */
  public FormElement(ElementType type, String name, String defaultValue) {
    this(type, name, defaultValue, defaultValue);
  }

  /**
   * Creates a copy of an existing form element.
   */
  public FormElement(FormElement formElement) {
    this(formElement.getType(),
        formElement.getName(),
        formElement.getDefaultValue(),
        formElement.getValue());
  }

  /**
   * Constructs a form element specifying all fields.
   */
  public FormElement(ElementType type, String name, String defaultValue, String value) {
    super(type);
    setProperty(NAME, name);
    setProperty(DEFAULT_VALUE, defaultValue);
    setProperty(VALUE, value);
  }

  /**
   * Constructs a form element with a given set of properties.
   *
   * @param type the type of the form element.
   * @param properties the properties of the form element.
   */
  public FormElement(ElementType type, Map<String, String> properties) {
    super(type, properties);
  }

  /**
   * Returns the name of the form element.
   *
   * @return the name of the form element.
   */
  public String getName() {
    return getPropertyNullCheck(NAME);
  }

  private String getPropertyNullCheck(String name) {
    String property = getProperty(name);
    return property == null ? "" : property;
  }

  /**
   * Sets the name of the form element.
   *
   * @param name the new name of the form element.
   */
  public void setName(String name) {
    setProperty(NAME, name);
  }

  /**
   * Returns the default value of the form element.
   *
   * @return the default value.
   */
  public String getDefaultValue() {
    return getPropertyNullCheck(DEFAULT_VALUE);
  }

  /**
   * Sets the default value of the form element. The default value is used
   * to initialize the form element and to test whether or not it has been
   * modified.
   *
   * @param defaultValue the new default value of the form element.
   */
  public void setDefaultValue(String defaultValue) {
    setProperty(DEFAULT_VALUE, defaultValue);
  }

  /**
   * Returns the current value of the form element.
   *
   * @return the current value of the form element.
   */
  public String getValue() {
    return getPropertyNullCheck(VALUE);
  }

  /**
   * Sets the value of the form element.
   *
   * @param value the new value of the form element.
   */
  public void setValue(String value) {
    setProperty(VALUE, value);
  }

  /**
   * Returns a collection of form element types.
   *
   * @return a collection of form element types.
   */
  public static Collection<ElementType> getFormElementTypes() {
    return Collections.unmodifiableCollection(FORM_ELEMENT_TYPES);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given default value.
   *
   * @param defaultValue the default value to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByDefaultValue(String defaultValue) {
    return Restriction.of(DEFAULT_VALUE, defaultValue);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given value.
   *
   * @param value the value to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByValue(String value) {
    return Restriction.of(VALUE, value);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given name.
   *
   * @param name the name to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByName(String name) {
    return Restriction.of(NAME, name);
  }
}
