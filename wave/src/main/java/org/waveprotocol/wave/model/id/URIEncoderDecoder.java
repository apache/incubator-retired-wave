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

package org.waveprotocol.wave.model.id;


import java.util.HashSet;
import java.util.Set;

/**
 * This class is able to percent escape path components of
 * WaveletId URI, WaveId URI or WaveName URI.
 *
 * This class encode/decode strings in the same way as the "segment/segment-nz"
 * component in "path-absolute" defined in
 * http://tools.ietf.org/html/rfc3986#section-3.3
 *
 * path-absolute have the following syntax
 *
 * path-absolute = "/" [ segment-nz *( "/" segment ) ]
 * segment = *pchar
 * segment-nz = 1*pchar
 * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
 * sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
 * pct-encoded   = "%" HEXDIG HEXDIG
 *
 * This means the following are NOT escaped
 *
 * ":" / "@" / "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "=" /
 * ALPHA / DIGIT / "-" / "." / "_" / "~"
 *
 *
 */
public class URIEncoderDecoder {

  /**
   * Use this exception when the encoding is incorrect during encoding/decoding.
   *
   *
   */
  public static class EncodingException extends Exception {
    public EncodingException(String message) {
      super(message);
    }

    public EncodingException(Throwable ex) {
      super(ex);
    }

    public EncodingException(String message, Throwable ex) {
      super(message, ex);
    }
  }

  /**
   * We need this interface because there is no common library that works in both GWT
   * and on server side java code that can percent encoding.
   *
   * The encoding to used in percent escape is UTF-8, see http://tools.ietf.org/html/rfc3986.
   * Java strings are UTF-16. So it's not trivial to rewrite it again here. Instead
   * we have to inject the percent encoding library at run time.
   *
   * GWT (com.google.gwt.http.client.URL) and std java (java.net.URLEncoder)
   *
   *
   *
   */
  public interface PercentEncoderDecoder {

    /**
     * Returns a string where all characters are encoded by converting it into
     * its UTF-8 encoding and then encoding each of the resulting bytes as a
     * %xx hexadecimal escape sequence.
     *
     * The encoding of the following chars are optional
     *
     * ":" / "@" / "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "=" /
     * ALPHA / DIGIT / "-" / "." / "_" / "~"
     *
     * @param decodedValue value to be encoded. The behaviour is unspecified if
     *    this is not valid UTF-16.
     * @return The encoded value
     *
     * @throws EncodingException if encoding fails. e.g. unable to a find an appropriate
     *   UTF-8 encoder in the system.
     */
    public String encode(String decodedValue) throws EncodingException;

    /**
     * Returns a string where all percent encoded sequences have been converted back
     * to their original character representations. The charset is UTF-8.
     * Note, '+' should not be decoded as a space like in URL.
     *
     * @param encodedValue to be decoded.
     * @return The decoded value
     *
     * @throws EncodingException if decoding fails. e.g. the hex values following percent in
     *    encodedValue cannot be interpreted as valid UTF-8 or if a percent is not followed
     *    by a hex value
     */
    public String decode(String encodedValue) throws EncodingException;
  }

  private static final Set<Character> NOT_ESCAPED = new HashSet<Character>();

  static {
    for (char c = 'a'; c <= 'z'; c++) {
      NOT_ESCAPED.add(c);
    }
    for (char c = 'A'; c <= 'Z'; c++) {
      NOT_ESCAPED.add(c);
    }
    for (char c = '0'; c <= '9'; c++) {
      NOT_ESCAPED.add(c);
    }

    String symbols = ":@!$&'()*+,;=-._~";

    for (int i = 0; i < symbols.length(); i++) {
      NOT_ESCAPED.add(symbols.charAt(i));
    }
  }

  private final PercentEncoderDecoder percentEncoder;

  public URIEncoderDecoder(PercentEncoderDecoder percentEncoder) {
    this.percentEncoder = percentEncoder;
  }

  /**
   * Percent escapes the given string.
   * @param decodedValue is the value that needs to be percent escaped to satisfy the
   *    requirement for "segment/segment-nz" above. The behaviour is unspecified if
   *    this is not valid UTF-16.
   * @return The percent escaped value.
   *
   * @throws EncodingException if encoding fails. e.g. unable to a find an appropriate
   *   UTF-8 encoder in the system.
   */
  public String encode(String decodedValue) throws EncodingException {
    StringBuilder out = new StringBuilder(decodedValue.length());

    for (int i = 0; i < decodedValue.length();) {
      char c = decodedValue.charAt(i);
      if (NOT_ESCAPED.contains(c)) {
        out.append(c);
        i++;
      } else {
        int j = i;
        // convert to external encoding before hex conversion
        do {
          i++;
        } while (i < decodedValue.length() && !NOT_ESCAPED.contains((decodedValue.charAt(i))));

        out.append(percentEncoder.encode(decodedValue.substring(j, i)));
      }
    }

    return out.toString();
  }

  /**
   * @param encodedValue this the percent escaped "segment/segment-nz" value.
   * @return the decoded value.
   *
   * @throws EncodingException if decoding fails. e.g. the hex values following percent in
   *    encodedValue cannot be interpreted as valid UTF-8 or if a percent is not followed
   *    by a hex value.
   */
  public String decode(String encodedValue) throws EncodingException {
    return percentEncoder.decode(encodedValue);
  }
}
