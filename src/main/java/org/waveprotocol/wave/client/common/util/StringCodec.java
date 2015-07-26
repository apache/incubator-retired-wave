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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.core.client.GWT;


/**
 * Provides cross-platform encoding and decoding of arbitrary strings.
 *
 */
public interface StringCodec {
  /**
   * A basic codec that moves the string to an alphabet that does not include
   * commas. Decoding restores the original string. This allows commas to be
   * used to mark structure (like Godel numbering).
   */
  public StringCodec INSTANCE = GWT.isScript() ? new JsCodec() : new JavaCodec();

  /**
   * Encodes a string. This method provides the contract that, for any input
   * string:
   * <ul>
   * <li>decode(encode(s)) == s; and</li>
   * <li>encode(s) contains no characters from the set free()</li>
   * </ul>
   *
   * @param s string to encode
   * @return encoding of {@code s}
   */
  String encode(String s);

  /**
   * Decodes a string encoded by {@link #encode(String)}.
   *
   * @param s string to decode
   * @return decoding of {@code s}
   */
  String decode(String s);

  /** The characters that are removed from the alphabet encoded strings. */
  String free();

  //
  // Default implementations below.
  //
  // Simple quotation logic:
  // 1. & --> && (promotes & as the quote char)
  // 2. , --> &c (removes commas from alphabet)
  //
  // Unquotation is the inverse:
  // 1. &c --> , (reinserts commas into alphabet)
  // 2. && --> & (demotes & from being the quote char)
  //
  //

  final class JsCodec implements StringCodec {
    @Override
    public String free() {
      return ",";
    }

    @Override
    public native String encode(String s) /*-{
      return s.replace(/&/g, "&&").replace(/,/g, "&c");
    }-*/;

    @Override
    public native String decode(String s) /*-{
      return s.replace(/&c/g, ",").replace(/&&/g, "&");
    }-*/;
  }

  final class JavaCodec implements StringCodec {
    @Override
    public String free() {
      return ",";
    }

    @Override
    public String encode(String s) {
      return s.replace("&", "&&").replace(",", "&c");
    }

    @Override
    public String decode(String s) {
      return s.replace("&c", ",").replace("&&", "&");
    }
  }
}
