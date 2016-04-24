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

/**
 * A class that models a plain-text content of a blip.
 */
public class Plaintext extends BlipContent {

  /** The text content. */
  private final StringBuilder text;

  /**
   * Convenience factory method.
   *
   * @param text the text to construct the {@link Plaintext} object.
   * @return an instance of {@link Plaintext} that represents the given string.
   */
  public static Plaintext of(String text) {
    return new Plaintext(text);
  }

  /**
   * Constructor.
   *
   * @param text the text content.
   */
  public Plaintext(String text) {
    this.text = new StringBuilder(text);
  }

  /**
   * Appends the given text to this {@link Plaintext} instance.
   *
   * @param text the text to be appended.
   * @return an instance of this {@link Plaintext}, for chaining.
   */
  public Plaintext append(String text) {
    this.text.append(text);
    return this;
  }

  @Override
  public String getText() {
    return text.toString();
  }

  @Override
  public int hashCode() {
    return text.toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (getClass() != o.getClass()) {
      return false;
    }

    Plaintext other = (Plaintext) o;
    if (text == null && other.text == null) {
      return true;
    }

    if ((text == null && other.text != null) || (text != null && other.text == null)) {
      return false;
    }

    return text.toString().equals(other.text.toString());
  }
}
