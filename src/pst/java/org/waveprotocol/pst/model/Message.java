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

 import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * Wraps a {@link Descriptor} with methods suitable for stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Message {

  private final Descriptor descriptor;
  private final String templateName;
  private final MessageProperties extraProperties;

  // Lazily created.
  private List<Field> fields = null;
  private List<Message> messages = null;
  private List<ProtoEnum> enums = null;
  private List<Message> referencedMessages = null;
  private List<ProtoEnum> referencedEnums = null;
  private String fullName;
  private String fullJavaType;

  public Message(Descriptor descriptor, String templateName, MessageProperties extraProperties) {
    this.descriptor = descriptor;
    this.templateName = templateName;
    this.extraProperties = extraProperties;
  }

  /**
   * Returns the short name of the Java type of this message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = "Person"</li>
   * </ul>
   *
   * @return the name of the protocol buffer message
   */
  public String getName() {
    return descriptor.getName();
  }

  /**
   * Returns the short name of Java type being generated. For example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = <ul>
   *     <li>"PersonMessage" (for template name "message")</li>
   *     <li>"PersonMessageServerImpl" (for template name "messageServerImpl")</li></ul>
   * </li>
   * </ul>
   *
   * @return the name of the Java message
   */
  public String getJavaType() {
    return descriptor.getName() + Util.capitalize(templateName);
  }

  /**
   * Returns the full name of the this message in abstract Java space. For
   * example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = <ul>
   *     <li>"org.waveprotocol.pst.examples.Person"
   *          (for template name "Message" and package suffix "dto")</li></ul>
   * </li>
   * </ul>
   *
   * @return the name of the protocol buffer message
   */
  public String getFullName() {
    if (fullName == null) {
      fullName = getFullName(false);
    }
    return fullName;
  }

  /**
   * Returns the full name of the Java type of this message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = <ul>
   *     <li>"org.waveprotocol.pst.examples.dto.PersonMessage"
   *          (for template name "Message" and package suffix "dto")</li>
   *     <li>"org.waveprotocol.pst.examples.impl.PersonImpl"
   *          (for template name "Impl" and package suffix "impl")</li></ul>
   * </li>
   * </ul>
   *
   * @return the name of the protocol buffer message
   */
  public String getFullJavaType() {
    if (fullJavaType == null) {
      fullJavaType = getFullName(true);
    }
   return fullJavaType;
  }

  /**
   * Gets the fully-qualified name of this message.
   *
   * @param covariant if true, the name refers to the Java type being generated
   *        for this message. Otherwise, the name refers to a
   *        template-independent Java type, which may or may not exist. This is
   *        intended to be used so that the generated Java type for this message
   *        can refer to other Java types derived from this message.
   * @return the fully-qualified name of this message.
   */
  private String getFullName(boolean covariant) {
    String prefix;
    if (descriptor.getContainingType() != null) {
      prefix = adapt(descriptor.getContainingType()).getFullName(covariant);
    } else {
      prefix = covariant ? getPackage() : getPackageBase();
    }

    return prefix + "." + (covariant ? getJavaType() : getName());
  }

  /**
   * Returns the package of the Java messageas the base plus the suffix
   * components of the package, for example given org.waveprotocol.pst.examples.Example1.Person:
   * <ul>
   * <li>Message = "org.waveprotocol.pst.examples"</li>
   * <li>MessageServerImpl (package suffix "server") = "org.waveprotocol.pst.examples.server"</li>
   * <li>MessageClientImpl (package suffix "client") = "org.waveprotocol.pst.examples.client"</li>
   * </ul>
   *
   * @return the Java package of the message
   */
  public String getPackage() {
    String suffix = getPackageSuffix();
    return getPackageBase() + (!Strings.isNullOrEmpty(suffix) ? "." + suffix : "");
  }

  /**
   * Returns the base component of the Java message package, for example, given
   * org.waveprotocol.pst.examples.Example1.Person:
   * <ul>
   * <li>Message = "org.waveprotocol.pst.examples"</li>
   * <li>MessageServerImpl (package suffix "server") = "org.waveprotocol.pst.examples"</li>
   * </ul>
   *
   * @return the base component of the Java package
   */
  public String getPackageBase() {
    String javaPackage = descriptor.getFile().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(javaPackage)) {
      javaPackage = descriptor.getFile().getPackage();
    }
    return javaPackage;
  }

  /**
   * Returns the suffix component of the Java message package, as configured in
   * the message's properties file, for example:
   * <ul>
   * <li>Message = null</li>
   * <li>MessageServerImpl = "server"</li>
   * <li>MessageClientImpl = "client"</li>
   * </ul>
   */
  public String getPackageSuffix() {
    return extraProperties.getPackageSuffix();
  }

  /**
   * @return the filename of the protocol buffer (.proto) file where the message
   *         is defined
   */
  public String getFilename() {
    return descriptor.getFile().getName();
  }

  /**
   * Returns the qualified type of the protobuf message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person =
   *     "org.waveprotocol.pst.examples.Example1.Person"</li>
   * </ul>
   *
   * @return the full type of the protocol buffer message
   */
  public String getProtoType() {
    Deque<String> scopes = Lists.newLinkedList();
    for (Descriptor message = descriptor; message != null; message = message.getContainingType()) {
      scopes.push(message.getName());
    }
    scopes.push(descriptor.getFile().getOptions().getJavaOuterClassname());
    scopes.push(getPackageBase());
    return Joiner.on('.').join(scopes);
  }

  /**
   * @return the fields of the message
   */
  public List<Field> getFields() {
    if (fields == null) {
      ImmutableList.Builder<Field> builder = ImmutableList.builder();
      for (FieldDescriptor fd : descriptor.getFields()) {
        builder.add(new Field(fd, new Type(fd, templateName, extraProperties), extraProperties));
      }
      fields = builder.build();
    }
    return fields;
  }

  /**
   * @return the set of all messages referred to be this message and its nested
   *         messages. Message references are due to message-typed fields.
   */
  public List<Message> getReferencedMessages() {
    if (referencedMessages == null) {
      referencedMessages = Lists.newArrayList();
      for (Descriptor d : collectMessages(descriptor, Sets.<Descriptor>newLinkedHashSet())) {
        referencedMessages.add(adapt(d));
      }
    }
    return referencedMessages;
  }

  /**
   * @return the set of all enums referred to be this message and its nested
   *         messages. Enum references are due to message-typed fields.
   */
  public List<ProtoEnum> getReferencedEnums() {
    if (referencedEnums == null) {
      referencedEnums = Lists.newArrayList();
      for (EnumDescriptor d : collectEnums(descriptor, Sets.<EnumDescriptor> newLinkedHashSet())) {
        referencedEnums.add(adapt(d));
      }
    }
    return referencedEnums;
  }

  /**
   * Collects messages referred to by a message and its nested messages.
   *
   * @return {@code referenced}
   */
  private static Collection<Descriptor> collectMessages(
      Descriptor message, Collection<Descriptor> referenced) {
    for (FieldDescriptor fd : message.getFields()) {
      if (fd.getJavaType() == JavaType.MESSAGE) {
        referenced.add(fd.getMessageType());
      }
    }
    for (Descriptor nd : message.getNestedTypes()) {
      collectMessages(nd, referenced);
    }
    return referenced;
  }

  /**
   * Collects enums referred to by a message and its nested messages.
   *
   * @return {@code referenced}
   */
  private static Collection<EnumDescriptor> collectEnums(
      Descriptor d, Collection<EnumDescriptor> referenced) {
    for (FieldDescriptor fd : d.getFields()) {
      if (fd.getJavaType() == JavaType.ENUM) {
        referenced.add(fd.getEnumType());
      }
    }
    for (Descriptor nd : d.getNestedTypes()) {
      collectEnums(nd, referenced);
    }
    return referenced;
  }

  /**
   * @return the nested messages of the message
   */
  public List<Message> getNestedMessages() {
    if (messages == null) {
      ImmutableList.Builder<Message> builder = ImmutableList.builder();
      for (Descriptor d : descriptor.getNestedTypes()) {
        builder.add(adapt(d));
      }
      messages = builder.build();
    }
    return messages;
  }

  /**
   * @return the nested enums of the message
   */
  public List<ProtoEnum> getNestedEnums() {
    if (enums == null) {
      ImmutableList.Builder<ProtoEnum> builder = ImmutableList.builder();
      for (EnumDescriptor ed : descriptor.getEnumTypes()) {
        builder.add(adapt(ed));
      }
      enums = builder.build();
    }
    return enums;
  }

  /**
   * @return whether this is an inner class
   */
  public boolean isInner() {
    return descriptor.getContainingType() != null;
  }

  private Message adapt(Descriptor d) {
    return new Message(d, templateName, extraProperties);
  }

  private ProtoEnum adapt(EnumDescriptor d) {
    return new ProtoEnum(d, templateName, extraProperties);
  }
}
