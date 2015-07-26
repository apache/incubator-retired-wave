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

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;

import java.util.List;

/**
 * Wraps a {@link EnumDescriptor} with methods suitable for stringtemplate.
 *
 * Called ProtoEnum rather than Enum to avoid java.lang namespace conflict.
 *
 * @author kalman@google.com (Benjamnin Kalman)
 */
public final class ProtoEnum {

  private final EnumDescriptor descriptor;
  private final String templateName;
  private final MessageProperties extra;

  private String fullName;
  private String fullJavaType;

  public ProtoEnum(EnumDescriptor descriptor, String templateName, MessageProperties extra) {
    this.descriptor = descriptor;
    this.templateName = templateName;
    this.extra = extra;
  }

  /**
   * Returns the enum, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender = "Gender"</li>
   * </ul>
   *
   * @return the name of the enum
   */
  public String getName() {
    return descriptor.getName();
  }

  /**
   * Returns the short name of the Java type generated for this enum, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender = "Gender"</li>
   * </ul>
   *
   * @return the name of the java type of the enum
   */
  public String getJavaType() {
    return getName();
  }

  /**
   * Returns the fully-qualified name of the enum in abstract space. For
   * example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender =
   * "org.waveprotocol.pst.examples.Person.Gender"</li>
   * </ul>
   *
   * @return the name of the enum
   */
  public String getFullName() {
    if (fullName == null) {
      fullName = getContainingMessage().getFullName() + "." + getName();
    }
    return fullName;
  }

  /**
   * Returns the fully-qualified name of the Java type for this enum.
   * example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender =
   * "org.waveprotocol.pst.examples.Person.Gender"</li>
   * </ul>
   *
   * @return the name of the enum
   */
  public String getFullJavaType() {
    if (fullJavaType == null) {
      fullJavaType = getContainingMessage().getFullJavaType() + "." + getName();
    }
    return fullJavaType;
  }

  /**
   * Returns the qualified type of the protobuf enum, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person =
   *     "org.waveprotocol.pst.examples.Example1.Person"</li>
   * </ul>
   *
   * @return the full type of the protocol buffer enum
   */
  public String getProtoType() {
    return getContainingMessage().getProtoType() + "." + getName();
  }

  /**
   * Returns the enum values, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender = [MALE, FEMALE, OTHER]</li>
   * </ul>
   *
   * @return the enum values
   */
  public List<EnumValue> getValues() {
    List<EnumValue> enums = Lists.newArrayList();
    for (EnumValueDescriptor evd : descriptor.getValues()) {
      enums.add(new EnumValue(evd));
    }
    return enums;
  }

  private Message getContainingMessage() {
    return new Message(descriptor.getContainingType(), templateName, extra);
  }
}
