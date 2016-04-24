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

package org.waveprotocol.box.server.robots.util;

import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.BlipContent;
import com.google.wave.api.Range;

import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

/**
 * Provides helper methods for the operation services.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotsUtil {

  @SuppressWarnings("serial")
  public static class RobotRegistrationException extends Exception {

    public RobotRegistrationException (String message) {
      super(message);
    }

    public RobotRegistrationException(String message, Throwable t) {
      super(message, t);
    }
  }

  private static final IdURIEncoderDecoder URI_CODEC = new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionZeroFactoryImpl(
      URI_CODEC);

  /**
   * Creates a new empty robot wavelet data.
   *
   * @param participant the wavelet creator.
   * @param waveletName the wavelet name.
   */
  public static RobotWaveletData createEmptyRobotWavelet(ParticipantId participant,
      WaveletName waveletName) {
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(waveletName);
    ObservableWaveletData emptyWavelet =
        WaveletDataUtil.createEmptyWavelet(waveletName, participant, hashedVersionZero,
            System.currentTimeMillis());
    RobotWaveletData newWavelet = new RobotWaveletData(emptyWavelet, hashedVersionZero);
    return newWavelet;
  }

  /**
   * Copies the content of the source blip into the target blip.
   *
   * @param fromBlip the source blip.
   * @param toBlip the target blip.
   */
  public static void copyBlipContents(Blip fromBlip, Blip toBlip) {
    for (BlipContent blipContent: fromBlip.all().values()) {
      toBlip.append(blipContent);
    }
    for (Annotation annotation : fromBlip.getAnnotations()) {
      Range range = annotation.getRange();
      toBlip.range(range.getStart() + 1, range.getEnd() + 1).annotate(annotation.getName(),
          annotation.getValue());
    }
  }

  private RobotsUtil() {

  }
}
