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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Wraps a {@link FieldDescriptor} to expose type-only information for
 * stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Type {

  private final FieldDescriptor field;
  private final String templateName;
  private final MessageProperties extraProperties;
  private Message messageType;

  public Type(FieldDescriptor field, String templateName, MessageProperties extraProperties) {
    this.field = field;
    this.templateName = templateName;
    this.extraProperties = extraProperties;
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
  public String getJavaType(boolean hasInt52Ext) {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "boolean";
      case BYTE_STRING:
        return "Blob";
      case DOUBLE:
        return "double";
      case ENUM:
        return field.getEnumType().getName();
      case FLOAT:
        return "float";
      case INT:
        return "int";
      case LONG:
        return hasInt52Ext && extraProperties.getUseInt52() ? "double" : "long";
      case MESSAGE:
        return getMessage().getJavaType();
      case STRING:
        return "String";
      default:
        throw new UnsupportedOperationException("Unsupported field type " + field.getJavaType());
    }
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
  public String getCapJavaType(boolean hasInt52Ext) {
    return Util.capitalize(getJavaType(hasInt52Ext));
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
  public String getBoxedJavaType(boolean hasInt52Ext) {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "Boolean";
      case DOUBLE:
        return "Double";
      case FLOAT:
        return "Float";
      case INT:
        return "Integer";
      case LONG:
        return hasInt52Ext && extraProperties.getUseInt52() ? "Double" : "Long";
      default:
        return getJavaType(hasInt52Ext);
    }
  }

  /**
   * Gets the default value of the field (null for objects, zero, false, etc).
   *
   * @return the "default value" of the field
   */
  public String getDefaultValue() {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "false";
      case BYTE_STRING:
        return "null";
      case DOUBLE:
        return "0.0";
      case ENUM:
        return field.getEnumType().getName() + ".UNKNOWN";
      case FLOAT:
        return "0.0f";
      case INT:
        return "0";
      case LONG:
        return "0L";
      case MESSAGE:
        return "null";
      case STRING:
        return "null";
      default:
        throw new UnsupportedOperationException("Unsupported field type " + field.getJavaType());
    }
  }

  /**
   * @return this type as a message.
   */
  public Message getMessage() {
    if (messageType == null) {
      messageType = adapt(field.getMessageType());
    }
    return messageType;
  }

  /**
   * @return whether the field is a message
   */
  public boolean isMessage() {
    return field.getType().equals(FieldDescriptor.Type.MESSAGE);
  }

  /**
   * @return whether the field is an enum
   */
  public boolean isEnum() {
    return field.getType().equals(FieldDescriptor.Type.ENUM);
  }

  /**
   * @return whether the field is a byte string.
   */
  public boolean isBlob() {
    return field.getType().equals(FieldDescriptor.Type.BYTES);
  }

  /**
   * @return whether the field type is a Java primitive
   */
  public boolean isPrimitive() {
    switch (field.getJavaType()) {
      case BOOLEAN:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
    }
  }

  private Message adapt(Descriptor d) {
    return new Message(d, templateName, extraProperties);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof Type) {
      Type t = (Type) o;
      if (field.getType() == t.field.getType()) {
        switch (field.getType()) {
          case MESSAGE:
            return field.getMessageType().equals(t.field.getMessageType());
          case ENUM:
            return field.getEnumType().equals(t.field.getEnumType());
          default:
            return true;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    switch (field.getType()) {
      case MESSAGE:
        return field.getMessageType().hashCode();
      case ENUM:
        return field.getEnumType().hashCode();
      default:
        return field.getType().hashCode();
    }
  }
}
