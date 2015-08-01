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

import com.google.protobuf.Descriptors.EnumValueDescriptor;

/**
 * Wraps a {@link EnumValueDescriptor} with methods suitable for stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class EnumValue {

  private final EnumValueDescriptor descriptor;

  public EnumValue(EnumValueDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  /**
   * Gets the name of the enum value, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender.MALE = "MALE".</li>
   * </ul>
   *
   * @return the name of the enum value
   */
  public String getName() {
    return descriptor.getName();
  }

  /**
   * Gets the number of the enum value, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender.MALE = 1.</li>
   * </ul>
   *
   * @return the name of the enum value
   */
  public int getNumber() {
    return descriptor.getNumber();
  }
}
