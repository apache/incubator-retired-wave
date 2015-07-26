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

package org.waveprotocol.wave.client.doodad.link;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.util.WaveRefConstants;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.id.DualIdSerialiser;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;

import javax.annotation.Nullable;

/**
 * Link utilities
 *
 */
public final class Link {

  private static final ReadableStringSet WEB_SCHEMES = CollectionUtils.newStringSet(
      "http", "https", "ftp", "mailto");
  /**
   * http://en.wikipedia.org/wiki/Fragment_identifier
   * http://tools.ietf.org/html/rfc3986#section-3.5
   * fragment    = *( pchar / "/" / "?" )
   */
  private static final String COMMON_REGEX = "[\\w\\-:@!\\$&\'\\(\\)\\*\\+,;=\\/\\?\\.]+";
  private static final String FRAGMENT_URI_REGEX = "#(" + COMMON_REGEX + "|$)";
  private static final String QUERY_REGEX = "(\\?" + COMMON_REGEX +"|)($|"+ FRAGMENT_URI_REGEX + ")";

  private static final String INVALID_LINK_MSG =
      "Invalid link. Should either be a web url\n" +
      "or, a Wave ref in the form: wave://example.com/w+1234/~/conv+root/b+abcd\n" +
      "or be a valid serialized wave id";

  public static class InvalidLinkException extends Exception {
    public InvalidLinkException(String message, Throwable cause) {
      super(message, cause);
    }
    public InvalidLinkException(String message) {
      super(message);
    }
    public InvalidLinkException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Array of all link keys
   *
   * NOTE(user): Code may implicitly depend on the order of these keys. We
   * should expose LINK_KEYS as a set, and have ORDERED_LINK_KEYS for code that
   * relies on specific ordering.
   */
  public static final String[] LINK_KEYS = {
    AnnotationConstants.LINK_PREFIX,
    AnnotationConstants.LINK_AUTO,
    AnnotationConstants.LINK_MANUAL,
    AnnotationConstants.LINK_WAVE};

  private Link() {
  }

  /**
   * Adapts the value of manual/link to the actual value that browser will use
   * to navigate
   *
   * @param uri
   * @return actual value that will be used by browser, or null if no link
   *         should be rendered.
   */
  public static @Nullable String toHrefFromUri(String uri) {
    // First, normalise it in case we have junk data.
    // We can optionally replace this step with something that just
    // returns null if the uri is not supported.
    try {
      uri = normalizeLink(uri);
    } catch (InvalidLinkException e1) {
      return null;
    }

    // If it's a wave link, use # for local navigation
    if (uri.startsWith(WaveRefConstants.WAVE_URI_PREFIX)) {
      try {
        WaveRef ref = GwtWaverefEncoder.decodeWaveRefFromPath(
            uri.substring(WaveRefConstants.WAVE_URI_PREFIX.length()));

        return "#" + GwtWaverefEncoder.encodeToUriPathSegment(ref);

      } catch (InvalidWaveRefException e) {
        return null;
      }
    }

    assert uri.matches(QUERY_REGEX) || EscapeUtils.extractScheme(uri) != null;

    // Otherwise, just return the given link.
    return uri;
  }

  /**
   * @return best guess link annotation value from an arbitrary string. feeding
   *         the return value back through this method should always return the
   *         input.
   * @throws InvalidLinkException
   */
  @SuppressWarnings("deprecation")
  public static String normalizeLink(String rawLinkValue) throws InvalidLinkException {
    Preconditions.checkNotNull(rawLinkValue);
    rawLinkValue = rawLinkValue.trim();
    String[] parts = splitUri(rawLinkValue);
    String scheme = parts != null ? parts[0] : null;

    // Normal web url
    if (rawLinkValue.matches(QUERY_REGEX) || (scheme != null && WEB_SCHEMES.contains(scheme))) {
      return rawLinkValue;
    }

    // Try to interpret a wave URI or naked waveid/waveref
    try {
      // NOTE(danilatos): Pasting in the raw serialized form of a wave ref is
      // not supported here. In practice this doesn't really matter.
      WaveRef ref;
      if (WaveRefConstants.WAVE_SCHEME.equals(scheme)) {
        ref = GwtWaverefEncoder.decodeWaveRefFromPath(parts[1]);
      } else if (scheme == null) {
        ref = inferWaveRef(rawLinkValue);
      } else if (WaveRefConstants.WAVE_SCHEME_OLD.equals(scheme)) {
        ref = inferWaveRef(parts[1]);
      } else {
        // Scheme is not a regular web scheme nor a wave scheme.
        throw new InvalidLinkException("Unsupported URL scheme: " + scheme);
      }
      return WaveRefConstants.WAVE_URI_PREFIX + GwtWaverefEncoder.encodeToUriPathSegment(ref);
    } catch (InvalidWaveRefException e) {
      throw new InvalidLinkException(INVALID_LINK_MSG, e);
    }
  }

  /**
   * Splits a URI string into its scheme and suffix components, if it matches.
   *
   * @return [scheme, suffix] for scheme://suffix, or null if it doesn't match.
   */
  private static String[] splitUri(String uri) {
    int sepLength = "://".length();
    String scheme = EscapeUtils.extractScheme(uri);
    if (scheme == null || uri.length() <= scheme.length() + sepLength) {
      return null;
    }
    return new String[] {scheme, uri.substring(scheme.length() + sepLength)};
  }

  private static WaveRef inferWaveRef(String rawString) throws InvalidWaveRefException {
    try {
      return GwtWaverefEncoder.decodeWaveRefFromPath(rawString);
    } catch (InvalidWaveRefException e) {
      // Let's try decoding it as a serialized wave id instead
      try {
        return WaveRef.of(DualIdSerialiser.MODERN.deserialiseWaveId(rawString));
      } catch (InvalidIdException e1) {
        // Didn't work. Just re-throw the original exception
        throw e;
      }
    }
  }
}
