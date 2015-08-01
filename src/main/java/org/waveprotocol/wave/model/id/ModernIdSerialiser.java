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

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Id serialiser for spec-conforming identifiers.
 *
 * This serialiser throws a runtime exception if an attempt is made to serialise
 * a non-conforming id. This check may be removed once it's no longer possible
 * to construct one.
 *
 * @author anorth@google.com (Alex North)
 */
public class ModernIdSerialiser implements IdSerialiser {

  public static final ModernIdSerialiser INSTANCE = new ModernIdSerialiser();
  private static final String SEP = "/";
  private static final String ELIDE = "~";

  @Override
  public String serialiseWaveId(WaveId id) {
    // These checks will be unnecessary once it's impossible to construct
    // a wave id that violates them.
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, id.getDomain()),
        "Invalid domain %s", id.getDomain());
    Preconditions.checkArgument(WaveIdentifiers.isValidIdentifier(id.getId()),
        "Invalid id %s", id.getId());
    return new StringBuilder(id.getDomain()).append(SEP).append(id.getId()).toString();
  }

  @Override
  public String serialiseWaveletId(WaveletId id) {
    // These checks will be unnecessary once it's impossible to construct
    // a wavelet id that violates them.
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, id.getDomain()),
        "Invalid domain %s", id.getDomain());
    Preconditions.checkArgument(WaveIdentifiers.isValidIdentifier(id.getId()),
        "Invalid id %s", id.getId());
    return new StringBuilder(id.getDomain()).append(SEP).append(id.getId()).toString();
  }

  @Override
  public String serialiseWaveletName(WaveletName name) {
    StringBuilder b = new StringBuilder(serialiseWaveId(name.waveId)).append(SEP);
    if (name.waveletId.getDomain().equals(name.waveId.getDomain())) {
      b.append(ELIDE);
    } else {
      b.append(name.waveletId.getDomain());
    }
    b.append(SEP).append(name.waveletId.getId());
    return b.toString();
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException {
    String[] tokens = serialisedForm.split(SEP);
    if (tokens.length != 2) {
      throw new InvalidIdException(serialisedForm,
          "Required 2 '/'-separated tokens in serialised wave id: " + serialisedForm);
    }
    return WaveId.ofChecked(tokens[0], tokens[1]);
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException {
    String[] tokens = serialisedForm.split(SEP);
    if (tokens.length != 2) {
      throw new InvalidIdException(serialisedForm,
          "Required 2 '/'-separated tokens in serialised wavelet id: " + serialisedForm);
    }
    return WaveletId.ofChecked(tokens[0], tokens[1]);
  }

  @Override
  public WaveletName deserialiseWaveletName(String serialisedForm) throws InvalidIdException {
    if (serialisedForm.endsWith("/")) {
      throw new InvalidIdException(serialisedForm, "Serialised wavelet name had trailing '/'");
    }
    String[] tokens = serialisedForm.split(SEP);
    if (tokens.length != 4) {
      throw new InvalidIdException(serialisedForm,
          "Required 4 '/'-separated tokens in serialised wavelet name: " + serialisedForm);
    }
    if (tokens[2].equals(tokens[0])) {
      throw new InvalidIdException(serialisedForm,
          "Serialised wavelet name had un-normalised domains");
    }
    if (tokens[2].equals(ELIDE)) {
      tokens[2] = tokens[0];
    }
    return WaveletName.of(WaveId.ofChecked(tokens[0], tokens[1]),
        WaveletId.ofChecked(tokens[2], tokens[3]));
  }

  private ModernIdSerialiser() {
  }
}
