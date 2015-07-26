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

package org.waveprotocol.pst.testing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates random protocol buffers with all fields given a value.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class RandomProtobufGenerator<E extends GeneratedMessage> {

  private static final int MAX_LIST_LENGTH = 10;
  private static final int MAX_STRING_LENGTH = 10;

  private final Random random;
  private final GeneratedMessage instance;
  private final Map<Descriptor, GeneratedMessage> moreInstances;

  private RandomProtobufGenerator(Random random, GeneratedMessage instance,
      Map<Descriptor, GeneratedMessage> moreInstances) {
    this.random = random;
    this.instance = instance;
    this.moreInstances = moreInstances;
  }

  /**
   * Creates a random protobuf generator, for a main type (instance) and a list
   * of any extra instances that will be needed to generate that instance.
   *
   * @param random random number generator
   * @param instance the protobuf instance used as a template to generate more
   *        random protobuf
   * @param moreInstances protobuf templates for any inner messages the
   *        protobuf depends on
   * @return the random protobuf generator
   */
  public static <T extends GeneratedMessage> RandomProtobufGenerator<T> create(Random random,
      GeneratedMessage instance, GeneratedMessage... moreInstances) {
    // NOTE: it would be nice to determine this internally e.g. through (java or proto) reflection.
    Map<Descriptor, GeneratedMessage> moreInstancesMap = Maps.newHashMap();
    for (GeneratedMessage gm : moreInstances) {
      moreInstancesMap.put(gm.getDescriptorForType(), gm);
    }
    return new RandomProtobufGenerator<T>(random, instance, moreInstancesMap);
  }

  /**
   * Generates a random protocol buffer, filling in all fields and giving
   * repeated values 0..n items.
   */
  public E generate() {
    return generate(0);
  }

  /**
   * Generates a random protocol buffer, filling in all required fields but
   * with a p chance of not setting an optional field and p chance of having
   * an empty repeated field.
   */
  @SuppressWarnings("unchecked")
  public E generate(double p) {
    Builder builder = instance.newBuilderForType();
    Descriptor descriptor = instance.getDescriptorForType();
    for (FieldDescriptor field : descriptor.getFields()) {
      if (!field.isRequired() && random.nextDouble() < p) {
        continue;
      }
      builder.setField(field, getRandomValue(field, p));
    }
    return (E) builder.build();
  }

  private Object getRandomValue(FieldDescriptor field, double p) {
    if (field.isRepeated()) {
      List<Object> values = Lists.newArrayList();
      for (int i = 0, length = random.nextInt(MAX_LIST_LENGTH); i < length; i++) {
        values.add(getRandomSingleValue(field, p));
      }
      return values;
    } else {
      return getRandomSingleValue(field, p);
    }
  }

  private Object getRandomSingleValue(FieldDescriptor field, double p) {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return random.nextBoolean();
      case BYTE_STRING:
        return getRandomByteString();
      case DOUBLE:
        return random.nextDouble();
      case ENUM:
        return getRandomEnum(field.getEnumType());
      case FLOAT:
        return random.nextFloat();
      case INT:
        return random.nextInt();
      case LONG:
        return random.nextLong();
      case MESSAGE:
        return getRandomMessage(field, p);
      case STRING:
        return getRandomString();
      default:
        return null;
    }
  }

  private ByteString getRandomByteString() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return ByteString.copyFrom(bytes);
  }

  private EnumValueDescriptor getRandomEnum(EnumDescriptor enumD) {
    List<EnumValueDescriptor> values = enumD.getValues();
    return values.get(random.nextInt(values.size()));
  }

  private GeneratedMessage getRandomMessage(FieldDescriptor field, double p) {
    GeneratedMessage instance = moreInstances.get(field.getMessageType());
    if (instance == null) {
      throw new IllegalArgumentException("Couldn't find instance for message "
          + field.getMessageType().getFullName());
    }
    return new RandomProtobufGenerator<GeneratedMessage>(random, instance, moreInstances)
        .generate(p);
  }

  private String getRandomString() {
    String alphabet = "abc{}[]<>\\\"'";
    StringBuilder s = new StringBuilder();
    for (int i = 0, length = random.nextInt(MAX_STRING_LENGTH); i < length; i++) {
      s.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return s.toString();
  }
}
