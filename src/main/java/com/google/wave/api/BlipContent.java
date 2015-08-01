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

import java.io.Serializable;

/**
 * An abstract class that models a blip content.
 *
 * @see Element
 * @see Plaintext
 */
public abstract class BlipContent implements Serializable {

  /**
   * Returns the textual representation of of this blip content, for example,
   * if it's an element, it returns a space.
   *
   * @return the textual representation.
   */
  public abstract String getText();

  /**
   * Returns this blip content as an element.
   *
   * @return an instance of {@link Element}, or {@code null} if it is not an
   *     {@link Element}.
   */
  public Element asElement() {
    if (!(this instanceof Element)) {
      return null;
    }
    return Element.class.cast(this);
  }

  /**
   * Returns this blip content as a plain-text.
   *
   * @return an instance of {@link Plaintext}, or {@code null} if it is not a
   *     {@link Plaintext}.
   */
  public Plaintext asPlaintext() {
    if (!(this instanceof Plaintext)) {
      return null;
    }
    return Plaintext.class.cast(this);
  }
}
