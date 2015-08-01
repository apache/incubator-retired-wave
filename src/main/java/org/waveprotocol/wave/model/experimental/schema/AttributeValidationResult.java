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

package org.waveprotocol.wave.model.experimental.schema;

/**
 * The result of the validation of attributes.
 *
 */
public abstract class AttributeValidationResult {

  /**
   * The type of the validation result.
   */
  public enum Type {
    VALID,
    ATTRIBUTE_NOT_ALLOWED,
    INVALID_ATTRIBUTE_VALUE,
    MISSING_REQUIRED_ATTRIBUTE,
    REMOVING_REQUIRED_ATTRIBUTE
  }

  /**
   * A result indicating the existence of an attribute that is not allowed.
   */
  public static final class AttributeNotAllowed extends AttributeValidationResult {

    private final String name;

    AttributeNotAllowed(String name) {
      this.name = name;
    }

    @Override
    public Type getType() {
      return Type.ATTRIBUTE_NOT_ALLOWED;
    }

    @Override
    public AttributeNotAllowed asAttributeNotAllowed() {
      return this;
    }

    /**
     * Returns the name of the disallowed attribute that was encountered.
     */
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Attribute not allowed: " + name;
    }

  }

  /**
   * A result indicating that an attribute has an invalid value.
   */
  public static final class InvalidAttributeValue extends AttributeValidationResult {

    private final String name;
    private final String value;

    InvalidAttributeValue(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public Type getType() {
      return Type.INVALID_ATTRIBUTE_VALUE;
    }

    @Override
    public InvalidAttributeValue asInvalidAttributeValue() {
      return this;
    }

    /**
     * Returns the name of the attribute with the invalid value.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the invalid attribute value.
     */
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return "Invalid value for attribute " + name + ": " + value;
    }

  }

  /**
   * A result indicating that a required attribute is missing.
   */
  public static final class MissingRequiredAttribute extends AttributeValidationResult {

    private final String name;

    MissingRequiredAttribute(String name) {
      this.name = name;
    }

    @Override
    public Type getType() {
      return Type.MISSING_REQUIRED_ATTRIBUTE;
    }

    @Override
    public MissingRequiredAttribute asMissingRequiredAttribute() {
      return this;
    }

    /**
     * Returns the name of the missing required attribute.
     */
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Required attribute missing: " + name;
    }

  }

  /**
   * A result indicating that an attribute update attempts to remove a required attribute.
   */
  public static final class RemovingRequiredAttribute extends AttributeValidationResult {

    private final String name;

    RemovingRequiredAttribute(String name) {
      this.name = name;
    }

    @Override
    public Type getType() {
      return Type.REMOVING_REQUIRED_ATTRIBUTE;
    }

    @Override
    public RemovingRequiredAttribute asRemovingRequiredAttribute() {
      return this;
    }

    /**
     * Returns the name the of required attribute whose removal was attempted.
     */
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Attempt to remove required attribute: " + name;
    }

  }

  /**
   * A result indicating that nothing invalid was encountered.
   */
  public static final AttributeValidationResult VALID = new AttributeValidationResult() {

    @Override
    public Type getType() {
      return Type.VALID;
    }

    @Override
    public String toString() {
      return "Valid";
    }

  };

  private AttributeValidationResult() {}

  /**
   * Returns the type of the validation result.
   */
  public abstract Type getType();

  /**
   * Returns this object if it is an <code>AttributeNotAllowed</code>, or null
   * otherwise.
   */
  public AttributeNotAllowed asAttributeNotAllowed() {
    return null;
  }

  /**
   * Returns this object if it is an <code>InvalidAttributeValue</code>, or null
   * otherwise.
   */
  public InvalidAttributeValue asInvalidAttributeValue() {
    return null;
  }

  /**
   * Returns this object if it is an <code>MissingRequiredAttribute</code>, or
   * null otherwise.
   */
  public MissingRequiredAttribute asMissingRequiredAttribute() {
    return null;
  }

  /**
   * Returns this object if it is an <code>RemovingRequiredAttribute</code>, or
   * null otherwise.
   */
  public RemovingRequiredAttribute asRemovingRequiredAttribute() {
    return null;
  }

}
