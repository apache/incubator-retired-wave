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

package org.waveprotocol.wave.client.common.safehtml;

// NOTE: In the near future, the files in this package will be open sourced as
// part of a different project. Do not rely on them staying here.

/**
 * Utility class containing static methods for escaping and sanitizing strings.
 */
// TODO(user): The naming of this class and the methods herein isn't exactly
// consistent anymore; clean this up.
public final class EscapeUtils {

  private static final String HTML_ENTITY_REGEX = "[a-z]+|#[0-9]+|#x[0-9a-fA-F]+";

  public static final SafeHtml EMPTY_SAFE_HTML = new SafeHtmlString("");

  // prevent instantiation
  private EscapeUtils() {
  }

  /**
   * Returns a SafeHtml constructed from a safe string, i.e. without escaping the string.
   */
  public static SafeHtml fromSafeConstant(String s) {
    return new SafeHtmlString(s);
  }

  /**
   * Returns a SafeHtml constructed from a plain string that does not contain any HTML markup.
   */
  public static SafeHtml fromPlainText(String s) {
    // TODO(user) assert that there are no HTML elements in the string
    // TODO(user) verify that this is actually faster than calling htmlEscape()
    return new SafeHtmlString(s);
  }

  /**
   * Returns a SafeHtml containing the escaped string.
   */
  public static SafeHtml fromString(String s) {
    return new SafeHtmlString(htmlEscape(s));
  }

  /**
   * HTML-escapes a string.
   *
   * @param s the string to be escaped
   * @return the input string, with all occurrences of HTML meta-characters replaced with their
   *         corresponding HTML Entity References
   */
  public static String htmlEscape(String s) {
    // TODO(user): GWT does not seem to have java.util.regex, so leave this out for now.
    /*
    if (!HTML_META_CHARS.matcher(s).find()) {
      // short cirquit and bail out if no work to be done, without allocating objects.
      return s;
    }
    */

    // TODO(user): maybe do some benchmarking and work out if this is the most efficient way to go
    // about escaping.
    return s.replaceAll("&", "&amp;")
        .replaceAll("\"", "&quot;")
        .replaceAll("\'", "&#39;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
  }

  /**
   * HTML-escapes a string, but does not double-escape HTML-entities already present in the string.
   *
   * @param text the string to be escaped
   * @return the input string, with all occurrences of HTML meta-characters replaced with their
   *         corresponding HTML Entity References, with the exception that ampersand characters are
   *         not double-escaped if they form the start of an HTML Entity Reference
   */
  public static String htmlEscapeAllowEntities(String text) {
    StringBuilder escaped = new StringBuilder();

    boolean firstSegment = true;
    for (String segment : text.split("&", -1)) {
      if (firstSegment) {
        // The first segment is never part of an entity reference, so we always escape it.
        // Note that if the input starts with an ampersand, we will get an empty segment
        // before that.
        firstSegment = false;
        escaped.append(htmlEscape(segment));
        continue;
      }

      int entityEnd = segment.indexOf(';');
      if (entityEnd > 0 &&
          segment.substring(0, entityEnd).matches(HTML_ENTITY_REGEX)) {
        // Append the entity without escaping.
        escaped.append("&")
            .append(segment.substring(0, entityEnd + 1));

        // Append the rest of the segment, escaped.
        escaped.append(htmlEscape(segment.substring(entityEnd + 1)));
      } else {
        // The segment did not start with an entity reference, so escape the whole segment.
        escaped.append("&amp;")
            .append(htmlEscape(segment));
      }
    }

    return escaped.toString();
  }

  /*
   * Methods to validate/sanitize URIs.
   */

  // TODO(user): Figure out if GWT supports some parsed representation of URIs,
  // and add equivalent methods that operate on those rather than string (which
  // would likely be more efficient in cases where URIs are constructed with a
  // common base). I tried java.net.URI, but alas it's not supported at this
  // time.

  /**
   * Extracts the scheme of a URI.
   *
   * @param uri the URI to extract the scheme from
   * @return the URI's scheme, or {@code null} if the URI does not have one
   */
  public static String extractScheme(String uri) {
    int colonPos = uri.indexOf(':');
    if (colonPos < 0) {
      return null;
    }
    String scheme = uri.substring(0, colonPos);
    if (scheme.indexOf('/') >= 0 || scheme.indexOf('#') >= 0) {
      // The URI's prefix up to the first ':' contains other URI special
      // chars, and won't be interpreted as a scheme.
      // TODO(user): Consider basing this on URL#isValidProtocol or similar;
      // however I'm worried that being too strict here will effectively
      // allow dangerous schemes accepted in loosely parsing browsers.
      return null;
    }
    return scheme;
  }

  /**
   * Determines if a {@link String} is safe to use as the value of a URI-valued
   * HTML attribute such as {@code src} or {@code href}.
   *
   * <p>In this context, a URI is safe if it can be established that using it as
   * the value of a URI-valued HTML attribute such as {@code src} or {@code
   * href} cannot result in script execution. Specifically, this method deems a
   * URI safe if it either does not have a scheme, or its scheme is one of
   * {@code http, https, ftp, mailto}.
   *
   * @param uri the URI to validate
   * @return {@code true} if {@code uri} is safe in the above sense; {@code
   *         false} otherwise
   */
  public static boolean isSafeUri(String uri) {
    String scheme = extractScheme(uri);
    return (scheme == null
            || "http".equalsIgnoreCase(scheme)
            || "https".equalsIgnoreCase(scheme)
            || "mailto".equalsIgnoreCase(scheme)
            || "ftp".equalsIgnoreCase(scheme));
  }

  /**
   * Sanitizes a URI.
   *
   * <p>This method returns the URI provided if it is safe to use as the the
   * value of a URI-valued HTML attribute according to {@link #isSafeUri}, or
   * the URI "{@code #}" otherwise.
   *
   * @param uri the URI to sanitize.
   */
  public static String sanitizeUri(String uri) {
    if (isSafeUri(uri)) {
      return uri;
    } else {
      return "#";
    }
  }
}
