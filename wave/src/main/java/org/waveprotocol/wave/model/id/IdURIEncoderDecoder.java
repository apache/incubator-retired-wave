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

import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.PercentEncoderDecoder;

/**
 * This class is used to generate URI for ids.
 *
 * A wavelet name is expressible as a URI. The domain qualifying the wavelet id
 * is used as the host part (since this is where the wavelet is hosted). The
 * wave id is used as the first path element. If the wave domain does not match
 * the wavelet domain the wave domain service provider id precedes the wave id
 * token followed by a '!' delimeter. The wavelet id is a final path element.
 *
 * @see URIEncoderDecoder for percent escaping scheme.
 *
 *
 * Examples:
 *
 * wave://wave.com/w+4ks3/conversation+root
 *
 * wave://tudalin.lv/profile+bob@tudalin.lv/profile+root
 *
 * wave://privatereply.com/wave.com!w+4Kl2/conversation+3sG7
 *
 *
 */
public class IdURIEncoderDecoder {

  /** Used to encode parts of URI that needs to be escaped */
  private final URIEncoderDecoder encoder;

  /**
   * @param percentEncoder An encoder able to percent encode strings.
   */
  public IdURIEncoderDecoder(PercentEncoderDecoder percentEncoder) {
    this.encoder = new URIEncoderDecoder(percentEncoder);
  }

  /**
   * @param name The WaveletName to serialise. The domain name in WaveletId and WaveId
   *    is assumed to not have any funny characters that need escaping. The local parts (getId())
   *    of the wavelet id and wave id are assumed to have been "~" escaped where,
   *    "!", "~" are prefixed with "~". "+" is "~" escaped when not used as a separator
   *    token.
   * @return URI representation of the WaveletName.
   *
   * @throws EncodingException This can happen if the values in name are not valid UTF-16.
   */
  public String waveletNameToURI(WaveletName name) throws EncodingException {
    return IdConstants.WAVE_URI_SCHEME + "://" +  waveletNameToURIPath(name);
  }

  /**
   * @param name The WaveletName to serialise. The domain name in WaveletId and WaveId
   *    is assumed to not have any funny characters that need escaping. The local parts (getId())
   *    of the wavelet id and wave id are assumed to have been "~" escaped where,
   *    "!", "~" are prefixed with "~". "+" is "~" escaped when not used as a separator
   *    token.
   * @return path part of URI representation of the WaveletName.
   *
   * @throws EncodingException This can happen if the values in name are not valid UTF-16.
   */
  public String waveletNameToURIPath(WaveletName name) throws EncodingException {
    WaveId waveId = name.waveId;
    WaveletId waveletId = name.waveletId;
    String waveDomain = waveId.getDomain();

    if (waveletId.getDomain().equals(waveDomain)) {
      waveDomain = "";
    } else {
      waveDomain += "!";
    }

    return waveletId.getDomain() + "/" + waveDomain + encoder.encode(waveId.getId()) +
        "/" + encoder.encode(waveletId.getId());
  }


  /**
   * @param serialisedForm The serialised URI form of the wavelet name.
   * @return The WaveletName that is deserialised
   *
   * @throws EncodingException if decoding fails. e.g. the hex values following percent in
   *    encodedValue cannot be interpreted as valid UTF-8 or if a percent is not followed
   *    by a hex value.
   */
  public WaveletName uriToWaveletName(String serialisedForm) throws EncodingException {
    String prefix = IdConstants.WAVE_URI_SCHEME + "://";
    if (!serialisedForm.startsWith(prefix)) {
      throw new IllegalArgumentException("Invalid scheme for the wavelet name URI: " +
          serialisedForm);
    }
    return uriPathToWaveletName(serialisedForm.substring(prefix.length()));
  }

  /**
   * @param serialisedForm The serialised URI path part only of the wavelet name.
   * @return The WaveletName that is deserialised
   * @throws EncodingException
   *
   * @throws EncodingException if decoding fails. e.g. the hex values following percent in
   *    encodedValue cannot be interpreted as valid UTF-8 or if a percent is not followed
   *    by a hex value.
   */
  public WaveletName uriPathToWaveletName(String serialisedForm) throws EncodingException {
    String[] parts = serialisedForm.split("/");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Wavelet name URI path expected in the format of" +
          " <wavelet domain>/[<wave domain>!]<wave id>/<wavelet id> but got: " +
          serialisedForm);
    }

    WaveletId waveletId = WaveletId.ofLegacy(parts[0], encoder.decode(parts[2]));

    String[] waveIdParts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        '!', parts[1]);
    WaveId waveId;
    if (waveIdParts.length == 1) {
      waveId = WaveId.ofLegacy(waveletId.getDomain(), encoder.decode(waveIdParts[0]));
    } else if (waveIdParts.length == 2) {
      waveId = WaveId.ofLegacy(waveIdParts[0], encoder.decode(waveIdParts[1]));
    } else {
      throw new IllegalArgumentException("Wave id in URI path is invalid. Expected the format" +
          " [<wave domain>!]<wave id> but got: " + parts[1]);
    }

    return WaveletName.of(waveId, waveletId);
  }
}
