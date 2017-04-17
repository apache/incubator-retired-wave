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

package org.waveprotocol.box.server.util.testing;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Commonly used constants for unit testing. Some constants taken from
 * previously existing test cases.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
// TODO(Michael): Maybe move this class to the libraries repository/branch.
public interface TestingConstants {
  public static final String BLIP_ID = "b+blip";

  public static final String MESSAGE = "The quick brown fox jumps over the lazy dog";

  public static final String MESSAGE2 = "Why's the rum gone?";

  public static final String MESSAGE3 = "There is no spoon";

  public static final String DOMAIN = "host.com";

  public static final String OTHER_USER_NAME = "other";

  public static final String OTHER_USER = OTHER_USER_NAME + "@" + DOMAIN;

  public static final ParticipantId OTHER_PARTICIPANT = new ParticipantId(OTHER_USER);

  public static final int PORT = 9876;

  public static final String USER_NAME = "user";

  public static final String USER = USER_NAME + "@" + DOMAIN;

  public static final char[] PASSWORD = "password".toCharArray();

  public static final ParticipantId PARTICIPANT = new ParticipantId(USER);

  public static final WaveId WAVE_ID = WaveId.of(DOMAIN, "w+wave");

  public static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "wavelet");

  public static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
}