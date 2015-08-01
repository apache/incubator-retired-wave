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

package org.waveprotocol.wave.model.waveref;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;


/**
 * Implementation of an escaping scheme for encoding and decoding waverefs into
 * URI path segments and URI query strings
 *
 * @author meade@google.com <Edwina Mead>
 */
public class WaverefEncoder {

  /** Represents the different escapers available for use. */
  private enum EscaperType {
    PATH, QUERY;
  }

  /**
   * We need this interface because there is no common library that works in
   * both GWT and on server side java code that can percent encoding.
   *
   * GWT (com.google.gwt.http.client.URL) and std java
   * (c.g.common.base.PercentEscaper)
   */
  public interface PercentEncoderDecoder {

    /**
     * Returns a string where all characters are encoded by converting it into
     * its UTF-8 encoding and then encoding each of the resulting bytes as a
     * %xx hexadecimal escape sequence.
     * Safe characters should be -_.!~*'()@:$&,;=+ (ALPHA) (DIGIT)
     *
     * @param decodedValue value to be encoded. The behaviour is unspecified if
     *    this is not valid UTF-16.
     * @return The encoded value
     */
    public String pathEncode(String decodedValue);

    /**
     * Returns a string where all characters are encoded by converting it into
     * its UTF-8 encoding and then encoding each of the resulting bytes as a
     * %xx hexadecimal escape sequence.
     * Safe characters should be -_.!~*'()@:$ ,; /?:+ (ALPHA) (DIGIT)
     *
     * @param decodedValue value to be encoded. The behaviour is unspecified if
     *    this is not valid UTF-16.
     * @return The encoded value
     */
    public String queryEncode(String decodedValue);

    /**
     * Returns a string where all percent encoded sequences have been converted back
     * to their original character representations. The charset is UTF-8.
     * Note, '+' should not be decoded as a space like in URL.
     *
     * @param encodedValue to be decoded.
     * @return The decoded value
     */
    public String decode(String encodedValue);
  }

  private final PercentEncoderDecoder encoderDecoder;

  /**
   * Creates a waveref encoder.
   *
   * @param decoder delegate for %-encoding
   */
  public WaverefEncoder(PercentEncoderDecoder decoder) {
    this.encoderDecoder = decoder;
  }

  /**
   * Converts a waveref object to a URI path segment for use in a permalink to a
   * wave, for example: "example.com/w+abcd/~/conv+root/b+1234
   *
   * @param ref the waveref to get converted to a uri path
   * @return the escaped path string
   */
  public String encodeToUriPathSegment(WaveRef ref) {
    return encode(ref, EscaperType.PATH);
  }

  /**
   * Converts a WaveRef object to a URI query string to be used in an url
   *
   * @param ref the waveref to get converted to a URI query string
   * @return the escaped URI query string
   */
  public String encodeToUriQueryString(WaveRef ref) {
    return encode(ref, EscaperType.QUERY);
  }

  /**
   * Percent escapes the given string to be usable in an URI path segment. eg.
   * the last part of http://wave.google.com/waveref/google.com/w+1234
   * Safe non-alphanumeric characters are "-_.!~*'()@:$&,;=+"
   *
   * @param str The string to be escaped
   * @return the percent escaped string
   */
  public String encodeToUriPathSegment(String str) {
    return encode(str, EscaperType.PATH);
  }

  /**
   * Percent escapes the given string to to be usable in an URI query string.
   * eg. the last part of
   * http://wave.google.com/?google.com/w+1234
   * Safe non-alphanumeric characters are "-_.!~*'()@:$ ,; /?:+"
   *
   * @param str The string to be escaped
   * @return the percent escaped string
   */
  public String encodeToUriQueryString(String str) {
    return encode(str, EscaperType.QUERY);
  }

  /**
   * Converts a waveref object to a URI path segment for use in a permalink to a
   * wave, or to a URI query string.
   *
   * @param ref the waveref to get converted to a uri path or uri query string
   * @param escaperType the type of escaping to be used, PATH or QUERY
   * @return the percent escaped string
   */
  private String encode(WaveRef ref, EscaperType escaperType) {
    StringBuilder result =
        new StringBuilder(encode(ref.getWaveId().getDomain(), escaperType));
    result.append("/").append(encode(ref.getWaveId().getId(), escaperType));
    if (ref.hasWaveletId()) {
      if(ref.getWaveletId().getDomain().equals(ref.getWaveId().getDomain())) {
        result.append("/~");
      } else {
        result.append("/").append(encode(ref.getWaveletId().getDomain(), escaperType));
      }
      result.append("/").append(encode(ref.getWaveletId().getId(), escaperType));
    }
    if (ref.hasDocumentId()) {
      result.append("/").append(encode(ref.getDocumentId(), escaperType));
    }
    return result.toString();
  }

  /**
   * Percent escapes the given wave domain or wave ID to a string usable in an
   * URI path segment. eg. the last part of
   * http://wave.google.com/waveref/google.com/w+1234 Safe non-alphanumeric for
   * path segments are "-_.!~*'()@:$&,;=+" and for query strings, they are
   * "-_.!~*'()@:$ ,; /?:+"
   *
   * @param str The string to be escaped
   * @param escaperType the type of escaping to be used, PATH or QUERY
   * @return the percent escaped string
   */
  private String encode(String str, EscaperType escaperType) {
    if (escaperType == EscaperType.PATH) {
      return encoderDecoder.pathEncode(str);
    } else {
      return encoderDecoder.queryEncode(str);
    }
  }

  /**
   * Takes a path segment which includes the domain and wave ID,and optionally
   * the wavelet domain + ID and the blip ID, and converts it to a waveref
   * object. Further tokens after 5 are silently ignored. Examples of valid URI
   * path segments are: example.com/w+abcd example.com/w+abcd/~/conv+root
   * example.com/w+abcd/~/conv+root/b+45kg
   *
   * @param path The path containing the domain and wave ID.
   * @return The corresponding WaveRef object, or null if the path was invalid.
   * @throws InvalidWaveRefException If the path contains less than 2 tokens or 3 tokens.
   */
  public WaveRef decodeWaveRefFromPath(String path) throws InvalidWaveRefException {
    String[] tokens = path.split("/");
    if (tokens.length < 2 || tokens.length == 3) {
      throw new InvalidWaveRefException(path,
          "Invalid number of tokens in path given to decodeWaveRefFromPath");
    }

    if (tokens[0].length() == 0 || tokens[1].length() == 0) {
      throw new InvalidWaveRefException(path, "The wave domain and the" +
          "wave Id must not be empty.");
    }

    String waveDomain = decode(tokens[0].toLowerCase());
    String waveIdStr = decode(tokens[1]);

    WaveId waveId = null;

    try {
      waveId = WaveId.of(waveDomain, waveIdStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidWaveRefException(path, "Invalid WaveID:" + e.getMessage());
    }

    if (tokens.length == 2) {
      return WaveRef.of(waveId);
    }

    if (tokens[2].length() == 0 || tokens[3].length() == 0) {
      throw new InvalidWaveRefException(path, "The wavelet domain and the" +
      "wavelet Id must not be empty.");
    }

    String waveletDomain = decode(tokens[2]);
    if (waveletDomain.equals("~")) {
      waveletDomain = waveDomain;
    }
    String waveletIdStr = decode(tokens[3]);
    WaveletId waveletId = null;
    try {
      waveletId = WaveletId.of(waveletDomain, waveletIdStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidWaveRefException(path, "Invalid WaveletID", e);
    }

    if (tokens.length == 4) {
      return WaveRef.of(waveId, waveletId);
    } else {
      String documentId = tokens[4];
      // When there is a specification for document IDs, validate here.
      return WaveRef.of(waveId, waveletId, documentId);
    }
  }

  /**
   * Percent-decodes a US-ASCII string into a Unicode string.
   *
   * @param string a percent-encoded US-ASCII string
   * @return the decoded string
   */
  String decode(String string) throws InvalidWaveRefException {
    try {
      return encoderDecoder.decode(string);
    } catch (IllegalArgumentException e) {
      throw new InvalidWaveRefException(string, "Failed to decode", e);
    }
  }
}
