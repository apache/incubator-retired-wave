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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Holds a piece of markup to be applied to a blip.
 *
 *  Insert/Append/Replace all can take a {@link Markup} instance instead of
 *  text. The markup should be {@code HTML} and will be converted to wave
 *  in a similar fashion as what happened when {@code HTML} is pasted directly
 *  into a wave.
 */
public class Markup extends BlipContent {

  /** The {@link Pattern} object used to search markup content. */
  private static final Pattern MARKUP_PATTERN = Pattern.compile("\\<.*?\\>");

  /** The {@code HTML} content of this markup. */
  private final String markup;

  /** The plain text version of this markup. */
  private final String plain;

  /**
   * Convenience factory method.
   *
   * @param markup the {@code HTML} content.
   * @return an instance of {@link Markup} that represents the given markup.
   */
  public static Markup of(String markup) {
    return new Markup(markup);
  }

  /**
   * Constructor.
   *
   * @param markup the {@code HTML} content.
   */
  public Markup(String markup) {
    this.markup = markup;
    this.plain = convertToPlainText(markup);
  }

  /**
   * Returns the {@code HTML} content of this markup.
   *
   * @return the {@code HTML} content.
   */
  public String getMarkup() {
    return markup;
  }

  @Override
  public String getText() {
    return plain;
  }

  /**
   * Converts the given {@code HTML} into robot compatible plain text.
   *
   * @param html the text to convert.
   * @return a plain text version of the given html text.
   */
  private static String convertToPlainText(String html) {
    StringBuffer result = new StringBuffer();
    Matcher matcher = MARKUP_PATTERN.matcher(html);
    while (matcher.find()) {
      String replacement = "";
      String tag = matcher.group().substring(1, matcher.group().length() - 1).split(" ")[0];
      if ("p".equals(tag) || "br".equals(tag)) {
        replacement = "\n";
      }
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
