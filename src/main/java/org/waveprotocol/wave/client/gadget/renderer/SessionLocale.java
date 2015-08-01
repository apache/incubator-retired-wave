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

package org.waveprotocol.wave.client.gadget.renderer;

/**
 * Locale implementation for Wave Session.
 *
 */
public class SessionLocale implements Locale {
  /**
   * RFC 3066 pattern for language (primary subtag).
   */
  private static final String LANG_PATTERN = "[a-zA-Z]{1,8}";

  /**
   * RFC 3066 pattern for subtag.
   */
  private static final String COUNTRY_PATTERN = "[a-zA-Z0-9]{1,8}";

  private static final String DEFAULT_LANG = "en";

  private static final String DEFAULT_COUNTRY = "ALL";

  private final String localeString;

  /**
   * Constructs a locale instance based on given session.
   *
   * NOTE(user): Do not access session.getLocale() in constructor to avoid
   * issues with wavepanel tests.
   *
   * @param localeString the locale string representation (ex: "en_GB", "de_DE").
   *
   */
  public SessionLocale(String localeString) {
    this.localeString = localeString;
  }

  private String[] splitSessionLocale() {
    return localeString.split("[_-]", 3);
  }

  @Override
  public String getCountry() {
    String[] split = splitSessionLocale();
    if ((split.length > 1) && split[1].matches(COUNTRY_PATTERN)) {
      return split[1];
    } else {
      return DEFAULT_COUNTRY;
    }
  }

  @Override
  public String getLanguage() {
    String[] split = splitSessionLocale();
    if ((split.length > 0) && split[0].matches(LANG_PATTERN)) {
      return split[0];
    } else {
      return DEFAULT_LANG;
    }
  }
}
