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

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;

/**
 * Utility functions for checking preconditions and throwing appropriate
 * Exceptions.
 *
 * This class mostly mimics com.google.common.base.Preconditions but only
 * implement the parts we need.
 * (we avoid external dependencies from model/)
 * TODO(tobiast): Add tests for this class
 */
public final class Preconditions {
  private Preconditions() {}

  public static void checkElementIndex(int index, int size) {
    assert size >= 0;
    if (index < 0) {
      throw new IndexOutOfBoundsException("index (" + index + ") must not be negative");
    }
    if (index >= size) {
      throw new IndexOutOfBoundsException(
          "index (" + index + ") must be less than size (" + size + ")");
    }
  }

  public static void checkPositionIndex(int index, int size) {
    assert size >= 0;
    if (index < 0) {
      throw new IndexOutOfBoundsException("index (" + index + ") must not be negative");
    }
    if (index > size) {
      throw new IndexOutOfBoundsException(
          "index (" + index + ") must not be greater than size (" + size + ")");
    }
  }

  public static void checkPositionIndexesInRange(int rangeStart, int start, int end, int rangeEnd) {
    assert rangeStart >= 0;
    assert rangeEnd >= rangeStart;

    if (start < rangeStart) {
      throw new IndexOutOfBoundsException("start index (" + start + ") " +
          "must not be less than start of range (" + rangeStart + ")");
    }
    if (end < start) {
      throw new IndexOutOfBoundsException(
          "end index (" + end + ") must not be less than start index (" + start + ")");
    }
    if (end > rangeEnd) {
      throw new IndexOutOfBoundsException(
          "end index (" + end + ") must not be greater than end of range (" + rangeEnd + ")");
    }
  }

  public static void checkPositionIndexes(int start, int end, int size) {
    checkPositionIndexesInRange(0, start, end, size);
  }

  /**
   * Note a failed null check.
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessage error message
   * @throws NullPointerException
   */
  public static void nullPointer(Object errorMessage) {
    throw new NullPointerException("" + errorMessage);
  }

  /**
   * Note a failed null check.
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   */
  public static void nullPointer(String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
  }

  /**
   * Ensures that the specified object is not null.
   *
   * @param x the object to check
   * @param errorMessage error message
   * @return the object if it is not null
   * @throws NullPointerException if {@code x} is null
   */
  public static <T> T checkNotNull(T x, Object errorMessage) {
    if (x == null) {
      throw new NullPointerException("" + errorMessage);
    }
    return x;
  }

  /**
   * Ensures that the specified object is not null.
   *
   * @param x the object to check
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   * @return the object if it is not null
   * @throws NullPointerException if {@code x} is null
   */
  public static <T> T checkNotNull(T x, String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    if (x == null) {
      throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
    }
    return x;
  }

  /**
   * Note an illegal argument
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessage error message
   * @throws IllegalArgumentException
   */
  public static void illegalArgument(Object errorMessage) {
    throw new IllegalArgumentException("" + errorMessage);
  }

  /**
   * Note an illegal argument
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   * @throws IllegalArgumentException
   */
  public static void illegalArgument(String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
  }

  /**
   * Ensures the truth of an condition involving the a parameter to the
   * calling method, but not involving the state of the calling instance.
   *
   * @param condition a boolean expression
   * @param errorMessage error message
   * @throws IllegalArgumentException if {@code condition} is false
   */
  public static void checkArgument(boolean condition, Object errorMessage) {
    if (!condition) {
      throw new IllegalArgumentException("" + errorMessage);
    }
  }

  /**
   * Ensures the truth of an condition involving the a parameter to the
   * calling method, but not involving the state of the calling instance.
   *
   * @param condition a boolean expression
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   * @throws IllegalArgumentException if {@code condition} is false
   */
  public static void checkArgument(boolean condition, String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    if (!condition) {
      throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Note an illegal state.
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessage error message
   * @throws IllegalStateException
   */
  public static void illegalState(Object errorMessage) {
    throw new IllegalStateException("" + errorMessage);
  }

  /**
   * Note an illegal state.
   *
   * Useful when an explicit check was done separately, to avoid building up
   * the error message object or string unecessarily (if profiling reveals a
   * hotspot).
   *
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   * @throws IllegalStateException
   */
  public static void illegalState(String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
  }

  /**
   * Ensures the truth of an condition involving the state of the calling
   * instance, but not involving any parameters to the calling method.
   *
   * @param condition a boolean expression
   * @param errorMessage error message
   * @throws IllegalStateException if {@code condition} is false
   */
  public static void checkState(boolean condition, Object errorMessage) {
    if (!condition) {
      throw new IllegalStateException("" + errorMessage);
    }
  }

  /**
   * Ensures the truth of an condition involving the state of the calling
   * instance, but not involving any parameters to the calling method.
   *
   * @param condition a boolean expression
   * @param errorMessageTemplate a template for the exception message.
   *     The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   * @throws IllegalStateException if {@code condition} is false
   */
  public static void checkState(boolean condition, String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    if (!condition) {
      throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Substitutes each {@code %s} in {@code template} with an argument. These
   * are matched by position - the first {@code %s} gets {@code args[0]}, etc.
   * If there are more arguments than placeholders, the unmatched arguments will
   * be appended to the end of the formatted message in square braces.
   *
   * This method is copied verbatim from com.google.common.base.Preconditions.
   *
   * @param template a non-null string containing 0 or more {@code %s}
   *     placeholders.
   * @param args the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   */
  @VisibleForTesting
  static String format(String template,
      @Nullable Object... args) {
    template = String.valueOf(template); // null -> "null"

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(
        template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template.substring(templateStart, placeholderStart));
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template.substring(templateStart));

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append("]");
    }

    return builder.toString();
  }
}
