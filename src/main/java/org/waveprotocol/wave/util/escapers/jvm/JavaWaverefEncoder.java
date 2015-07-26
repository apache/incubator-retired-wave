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

package org.waveprotocol.wave.util.escapers.jvm;

import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.model.waveref.WaverefEncoder;
import org.waveprotocol.wave.model.waveref.WaverefEncoder.PercentEncoderDecoder;
import org.waveprotocol.wave.util.escapers.PercentEscaper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Non-GWT-enabled instance of WaverefEncoder for use in the server.
 */
public class JavaWaverefEncoder {

  private static final PercentEscaper pathEscaper =
      new PercentEscaper(PercentEscaper.SAFEPATHCHARS_URLENCODER + "+", false);
  private static final PercentEscaper queryEscaper =
      new PercentEscaper(PercentEscaper.SAFEQUERYSTRINGCHARS_URLENCODER + "+", false);

  public static final WaverefEncoder INSTANCE = new WaverefEncoder(new PercentEncoderDecoder() {
    @Override
    public String decode(String encodedValue) {
      try {
        return URLDecoder.decode(encodedValue, "UTF-8").replaceAll(" ", "+");
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }

    @Override
    public String pathEncode(String decodedValue) {
      return pathEscaper.escape(decodedValue);
    }

    @Override
    public String queryEncode(String decodedValue) {
      return queryEscaper.escape(decodedValue);
    }
  });

  // Disallow construction
  private JavaWaverefEncoder() {
  }

  /** {@link WaverefEncoder#encodeToUriQueryString(String)} */
  public static String encodeToUriQueryString(String str) {
    return INSTANCE.encodeToUriQueryString(str);
  }

  /** {@link WaverefEncoder#encodeToUriPathSegment(String)} */
  public static String encodeToUriPathSegment(String str) {
    return INSTANCE.encodeToUriPathSegment(str);
  }

  /** {@link WaverefEncoder#encodeToUriQueryString(WaveRef)} */
  public static String encodeToUriQueryString(WaveRef ref) {
    return INSTANCE.encodeToUriQueryString(ref);
  }

  /** {@link WaverefEncoder#encodeToUriPathSegment(WaveRef)} */
  public static String encodeToUriPathSegment(WaveRef ref) {
    return INSTANCE.encodeToUriPathSegment(ref);
  }

  /** {@link WaverefEncoder#decodeWaveRefFromPath(String)} */
  public static WaveRef decodeWaveRefFromPath(String path) throws InvalidWaveRefException {
    return INSTANCE.decodeWaveRefFromPath(path);
  }
}
