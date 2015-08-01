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

package org.waveprotocol.wave.client.common.scrub;

import com.google.gwt.http.client.URL;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;

/**
 * Helper for scrubbing URLs
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class Scrub {

  /** If true, then we scrub URLs */
  private static boolean enableScrubbing = false;

  public static void setEnableScrubbing(final boolean enableScrubbing) {
    Scrub.enableScrubbing = enableScrubbing;
  }

  /** Scrubbing prefix */
  public static final String REFERRER_SCRUBBING_URL =
      "http://www.google.com/url?sa=D&q=";

  /**
   * Scrub a url if scrubbing is turned on
   *
   * Does not scrub urls with leading hashes
   *
   * @param url
   * @return The scrubbed version of the url, if it's not already scrubbed
   */
  public static String scrub(String url) {
    if (enableScrubbing) {
      if (url.startsWith("#") || url.startsWith(REFERRER_SCRUBBING_URL)) {
        // NOTE(user): The caller should be responsible for url encoding if
        // neccessary. There is no XSS risk here as it is a fragment.
        return url;
      } else {
        String x = REFERRER_SCRUBBING_URL + URL.encodeComponent(url);
        return x;
      }
    } else {
      // If we are not scrubbing the url, then we still need to sanitize it,
      // to protect against e.g. javascript.
      String sanitizedUri = EscapeUtils.sanitizeUri(url);
      return sanitizedUri;
    }
  }
}
