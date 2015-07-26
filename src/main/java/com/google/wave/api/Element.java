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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Elements are non-text content within a document. What the represent is
 * generally abstracted from the Robot. Although a Robot can query the
 * properties of an element it can only interact with the specific types that
 * the element represents.
 */
public class Element extends BlipContent implements Serializable {

  /** The type of an element. */
  private final ElementType type;

  /** A map of properties representing details of the element. */
  private final Map<String, String> properties;

  /**
   * Creates an element of the given type.
   *
   * @param type the type of element to construct.
   */
  public Element(ElementType type) {
    this.type = type;
    this.properties = new HashMap<String, String>();
  }

  /**
   * Constructs an Element of the given type with an initial set of properties.
   *
   * @param type the type of element to construct.
   * @param properties the properties of the element.
   */
  public Element(ElementType type, Map<String, String> properties) {
    this.type = type;
    this.properties = properties;
  }

  /**
   * Returns the type of the element.
   *
   * @return the type of the element.
   */
  public ElementType getType() {
    return type;
  }

  /**
   * Returns the map of properties for this element.
   *
   * @return the map of properties for this element.
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Creates/replaces a property with the given to a new value.
   *
   * @param name the name of the property to create/replace.
   * @param value the value to be set on this property.
   */
  public void setProperty(String name, String value) {
    this.properties.put(name, value);
  }

  /**
   * Returns the named property of this element.
   *
   * @param name the name of the property.
   * @return the value of the property or null if the property was not found.
   */
  public String getProperty(String name) {
    return getProperty(name, null);
  }

  /**
   * Returns the named property of this element, or the default value if the
   * property was not found.
   *
   * @param name the name of the property.
   * @param defaultValue the default value of the property.
   * @return the value of the property or the default value if the property was
   *     not found.
   */
  public String getProperty(String name, String defaultValue) {
    Object property = this.properties.get(name);
    if (property != null) {
      // We can't safely assume that the property is of type String, so we need
      // to call toString().
      return property.toString();
    }
    return defaultValue;
  }

  /**
   * Returns whether this element is a form element.
   *
   * @return true if the element is a form element, false otherwise.
   */
  public boolean isFormElement() {
    return FormElement.getFormElementTypes().contains(type);
  }

  /**
   * Returns whether this element is a gadget.
   *
   * @return true if the element is a gadget, false otherwise.
   */
  public boolean isGadget() {
    return type == ElementType.GADGET;
  }

  /**
   * Returns whether this element is an inline blip.
   *
   * @return true if the element is an inline blip, false otherwise.
   */
  public boolean isInlineBlip() {
    return type == ElementType.INLINE_BLIP;
  }

  /**
   * Returns whether this element is an image.
   *
   * @return true if the element is an image, false otherwise.
   */
  public boolean isImage() {
    return type == ElementType.IMAGE;
  }

  /**
   * Returns whether this element is an attachment.
   *
   * @return true if the element is an attachment, false otherwise.
   */
  public boolean isAttachment() {
    return type == ElementType.ATTACHMENT;
  }

  @Override
  public String toString() {
    return "{'type':'" + type + "','properties':" + properties + "}";
  }

  @Override
  public String getText() {
    return type == ElementType.LINE ? "\n" : " ";
  }
}
