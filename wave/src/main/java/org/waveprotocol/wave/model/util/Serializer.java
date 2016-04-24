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

package org.waveprotocol.wave.model.util;


/**
 * Encodes and decodes values to and from strings.
 *
 * @param <T> value type
 */
public interface Serializer<T> {
  /**
   * A serializer for string values.
   */
  public final static Serializer<String> STRING = new Serializer<String>() {
    @Override
    public String fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public String fromString(String s, String defaultValue) {
      return s != null ? s : defaultValue;
    }

    @Override
    public String toString(String x) {
      return x;
    }
  };

  /**
   * A serializer for long values.
   */
  public final static Serializer<Long> LONG = new Serializer<Long>() {
    @Override
    public Long fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public Long fromString(String s, Long defaultValue) {
      return (s != null)
          ? Long.valueOf(Long.parseLong(s))
          : defaultValue;
    }

    @Override
    public String toString(Long x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for integer values.
   */
  public final static Serializer<Integer> INTEGER = new Serializer<Integer>() {
    @Override
    public Integer fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public Integer fromString(String s, Integer defaultValue) {
      return (s != null)
          ? Integer.valueOf(Integer.parseInt(s))
          : defaultValue;
    }

    @Override
    public String toString(Integer x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for boolean values.
   */
  public final static Serializer<Boolean> BOOLEAN = new Serializer<Boolean>() {
    @Override
    public Boolean fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public Boolean fromString(String s, Boolean defaultValue) {
      return (s != null)
          ? Boolean.valueOf(Boolean.parseBoolean(s))
          : defaultValue;
    }

    @Override
    public String toString(Boolean x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for double values.
   */
  public final static Serializer<Double> DOUBLE = new Serializer<Double>() {
    @Override
    public Double fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public Double fromString(String s, Double defaultValue) {
      return (s != null)
          ? Double.valueOf(Double.parseDouble(s))
          : defaultValue;
    }

    @Override
    public String toString(Double x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * Skeleton support for serlialization of enums. Concrete classes can be
   * created by constructing an instance and passing the class of the enum to
   * the constructor.
   *
   * @param <E> The actual type of the enum.
   */
  // TODO(user): This was made non-final so it could be overriden by the
  // AttachmentDocumentWrapper. Switch it back to final, and find a better
  // way to get the desired behaviour.
  public static class EnumSerializer<E extends Enum<E>> implements Serializer<E> {
    private final Class<E> enumClass;

    public EnumSerializer(Class<E> enumClass) {
      this.enumClass = enumClass;
    }

    @Override
    public E fromString(String s) {
      return fromString(s, null);
    }

    @Override
    public E fromString(String s, E defaultValue) {
      return (s != null) ? E.valueOf(enumClass, s) : defaultValue;
    }

    @Override
    public String toString(E x) {
      return (x != null) ? x.name() : null;
    }
  }

  /**
   * Encodes a value as a string. If the value is null, the result will be null.
   *
   * @param x value to encode
   * @return string representation of {@code x}, or null.
   */
  String toString(T x);

  /**
   * Decodes a value from a string. If the string is null, the result will be
   * null.
   *
   * @param s string representation of a value
   * @return value represented by {@code s}, or null.
   */
  T fromString(String s);

  /**
   * Decodes a value from a string. If the string is null, the provided default
   * value will be returned.
   *
   * @param s string representation of a value
   * @param defaultValue value to return if s is null.
   * @return value represented by {@code s}, or defaultValue.
   */
  T fromString(String s, T defaultValue);
}
