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


/**
 * Serialises and deserialises wave ids and wavelet ids to and from
 * the format &lt;domain&gt;!&lt;id&gt;.
 *
 * This serialiser does not support methods for (de-)serialising wavelet names.
 *
 * @author zdwang@google.com (David Wang)
 */
public class LegacyIdSerialiser implements IdSerialiser {

  public static final LegacyIdSerialiser INSTANCE = new LegacyIdSerialiser();

  /** Separates a wave id from a wavelet id in serialised form. */
  public static final char PART_SEPARATOR = '!';

  @Override
  public String serialiseWaveId(WaveId waveId) {
    return waveId.getDomain() + LegacyIdSerialiser.PART_SEPARATOR + waveId.getId();
  }

  @Override
  public String serialiseWaveletId(WaveletId waveletId) {
    return waveletId.getDomain() + LegacyIdSerialiser.PART_SEPARATOR + waveletId.getId();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public String serialiseWaveletName(WaveletName name) {
    throw new UnsupportedOperationException("No legacy serialization for wavelet names");
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        LegacyIdSerialiser.PART_SEPARATOR, serialisedForm);
    if ((parts.length != 2) || parts[0].isEmpty() || parts[1].isEmpty()) {
      throw new InvalidIdException(serialisedForm,
          "Wave id must be of the form <domain>" + LegacyIdSerialiser.PART_SEPARATOR + "<id>");
    }
    try {
      return WaveId.ofLegacy(parts[0], parts[1]);
    } catch (IllegalArgumentException ex) {
      throw new InvalidIdException(serialisedForm, ex.getMessage());
    }
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        LegacyIdSerialiser.PART_SEPARATOR, serialisedForm);
    if ((parts.length != 2) || parts[0].isEmpty() || parts[1].isEmpty()) {
      throw new InvalidIdException(serialisedForm,
          "Wavelet id must be of the form <domain>" + LegacyIdSerialiser.PART_SEPARATOR + "<id>");
    }
    try {
      return WaveletId.ofLegacy(parts[0], parts[1]);
    } catch (IllegalArgumentException ex) {
      throw new InvalidIdException(serialisedForm, ex.getMessage());
    }
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public WaveletName deserialiseWaveletName(String serializedForm) {
    throw new UnsupportedOperationException("No legacy serialization for wavelet names");
  }

  private LegacyIdSerialiser() {
  }
}
