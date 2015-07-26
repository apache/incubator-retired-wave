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
 * General wavelet identifier encoder/decoder that decodes both modern and
 * legacy serialised identifiers, attempting the former first, and configurably
 * encodes to either the modern or legacy serialisation.
 *
 * @author anorth@google.com (Alex North)
 */
public class DualIdSerialiser implements IdSerialiser {

  public static final DualIdSerialiser MODERN = new DualIdSerialiser(true);
  public static final DualIdSerialiser LEGACY = new DualIdSerialiser(false);

  private final boolean toModern;

  /**
   * Creates a new serialiser.
   *
   * @param toModern whether to serialise to modern or legacy form
   */
  private DualIdSerialiser(boolean toModern) {
    this.toModern = toModern;
  }

  @Override
  public String serialiseWaveId(WaveId id) {
    return toModern ? ModernIdSerialiser.INSTANCE.serialiseWaveId(id)
        : LegacyIdSerialiser.INSTANCE.serialiseWaveId(id);
  }

  @Override
  public String serialiseWaveletId(WaveletId id) {
    return toModern ? ModernIdSerialiser.INSTANCE.serialiseWaveletId(id)
        : LegacyIdSerialiser.INSTANCE.serialiseWaveletId(id);
  }

  @Override
  public String serialiseWaveletName(WaveletName name) {
    return toModern ? ModernIdSerialiser.INSTANCE.serialiseWaveletName(name)
        : LegacyIdSerialiser.INSTANCE.serialiseWaveletName(name);
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveId(serialisedForm);
    } catch (InvalidIdException e) {
      return LegacyIdSerialiser.INSTANCE.deserialiseWaveId(serialisedForm);
    }
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveletId(serialisedForm);
    } catch (InvalidIdException e) {
      return LegacyIdSerialiser.INSTANCE.deserialiseWaveletId(serialisedForm);
    }
  }

  @Override
  public WaveletName deserialiseWaveletName(String serialisedForm) {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveletName(serialisedForm);
    } catch (InvalidIdException e) {
      return LegacyIdSerialiser.INSTANCE.deserialiseWaveletName(serialisedForm);
    }
  }
}
