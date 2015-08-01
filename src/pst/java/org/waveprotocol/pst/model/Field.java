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

package org.waveprotocol.pst.model;

import com.google.protobuf.Descriptors.FieldDescriptor;

import org.waveprotocol.protobuf.Extensions;

/**
 * Wraps a {@link FieldDescriptor} with methods suitable for stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Field {

  private final FieldDescriptor field;
  private final Type type;
  private final MessageProperties properties;

  public Field(FieldDescriptor field, Type type, MessageProperties properties) {
    this.field = field;
    this.type = type;
    this.properties = properties;
  }

  /**
   * Returns the type of the field as the Java type, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "int"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as the Java type
   */
  public String getJavaType() {
    return type.getJavaType(isInt52());
  }

  /**
   * Returns the type of the field as the Java type capitalized, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "Int"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as the Java type
   */
  public String getCapJavaType() {
    return type.getCapJavaType(isInt52());
  }

  /**
   * Returns the type of the field as the boxed Java type, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "Integer"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as a boxed Java type
   */
  public String getBoxedJavaType() {
    return type.getBoxedJavaType(isInt52());
  }

  /**
   * Returns the message type of the field without template suffix, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address =
   *     "Address" (regardless of template name)</li>
   * </ul>
   *
   * @return the message type of the field without template suffix
   */
  public String getMessageType() {
    return type.getMessage().getName();
  }

  /**
   * Gets the type of this field.
   *
   * @return the type of this field.
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the name of the field as uncapitalizedCamelCase, for example
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "firstName"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "age"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.lucky_numbers = "luckyNumbers"</li>
   * </ul>
   *
   * @return the name of the field as uncapitalizedCamelCase
   */
  public String getName() {
    return Util.uncapitalize(getCapName());
  }

  /**
   * Returns the name of the field as CapitalizedCamelCase, for example
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "FirstName"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "age"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.lucky_numbers = "LuckyNumbers"</li>
   * </ul>
   *
   * @return the name of the field as CapitalizedCamelCase
   */
  public String getCapName() {
    StringBuilder result = new StringBuilder();
    for (String s : getNameParts()) {
      result.append(Util.capitalize(s));
    }
    return result.toString();
  }

  private String[] getNameParts() {
    // Assumes that the field is separated by underscores... not sure if this
    // is always the case.
    return field.getName().split("_");
  }

  /**
   * Returns the number of the field, for example
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = 1</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = 4</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.lucky_numbers = 5</li>
   * </ul>
   *
   * @return the number of the field
   */
  public int getNumber() {
    return field.getNumber();
  }

  /**
   * Gets the default value of the field (null for objects, empty strings/arrays, zero, false, etc).
   *
   * @return the "default value" of the field
   */
  public String getDefaultValue() {
    return type.getDefaultValue();
  }

  /**
   * Gets the name of a Java getter for this field.
   *
   * @return the name of a Java getter for this field.
   */
  public String getGetter() {
    return "get" + getCapName();
  }

  /**
   * Gets the name of a Java setter for this field.
   *
   * @return the name of a Java getter for this field.
   */
  public String getSetter() {
    return "set" + getCapName();
  }

  /**
   * Gets whether the field is of type int52. This means that although the
   * field's native type is int64, only 52 bits of information are used.
   *
   * @return whether the field is a 52-bit integer
   */
  public boolean isInt52() {
    return properties.getUseInt52() //
        && field.getOptions().hasExtension(Extensions.int52)
        && field.getOptions().getExtension(Extensions.int52);
  }

  /**
   * Gets whether the field is of type long (int64). This means that the field
   * may use up to 64 bits of information.
   *
   * @return whether the field is a long (64-bit integer)
   */
  public boolean isLong() {
    return field.getJavaType() == FieldDescriptor.JavaType.LONG && !isInt52();
  }

  //
  // These map directly to the .proto definitions (except for isPrimitive, but that's pretty
  // self explanatory).
  //

  /**
   * @return whether the field is required
   */
  public boolean isRequired() {
    return field.isRequired();
  }

  /**
   * @return whether the field is optional
   */
  public boolean isOptional() {
    return field.isOptional();
  }

  /**
   * @return whether the field is repeated
   */
  public boolean isRepeated() {
    return field.isRepeated();
  }

  /**
   * @return whether the field is a message
   */
  public boolean isMessage() {
    return type.isMessage();
  }

  /**
   * @return whether the field is an enum
   */
  public boolean isEnum() {
    return type.isEnum();
  }

  /**
   * @return whether the field type is a Java primitive
   */
  public boolean isPrimitive() {
    return type.isPrimitive();
  }

  /**
   * @return whether the field type is a data blob.
   */
  public boolean isBlob() {
    return type.isBlob();
  }

  /**
   * @return whether the field type is a Java primitive and not repeated
   */
  public boolean isPrimitiveAndNotRepeated() {
    // NOTE: If stringtemplate could handle statements like
    //   $if (f.primitive && !f.repeated)$
    // then this method would be unnecessary.  However, from what I can tell, it can't.
    return isPrimitive() && !isRepeated();
  }
}
