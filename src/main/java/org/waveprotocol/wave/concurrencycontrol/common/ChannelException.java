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

package org.waveprotocol.wave.concurrencycontrol.common;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;


/**
 * Represents failure within a communication channel or stack.
 *
 * @author anorth@google.com (Alex North)
 */
public class ChannelException extends Exception {
  private final Recoverable isRecoverable;
  private final WaveId waveId;
  private final WaveletId waveletId;
  private final ResponseCode responseCode;

  /**
   * Creates a new exception
   * @param responseCode ResponseCode identifying the problem
   * @param message description of the failure
   * @param cause exception causing the failure
   * @param isRecoverable whether recovery is possible
   * @param waveId id of the associated wave (may be null)
   * @param waveletId id of the associated wavelet (may be null)
   */
  public ChannelException(ResponseCode responseCode, String message, Throwable cause,
      Recoverable isRecoverable, WaveId waveId, WaveletId waveletId) {
    super(message, cause);
    this.isRecoverable = isRecoverable;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.responseCode = responseCode;
  }

  /**
   * Simpler constructor for when there is no known error code, usually used
   * when some sanity check failed or other low-level problem occurred
   * (therefore it will be treated as {@link ResponseCode#INTERNAL_ERROR}
   *
   * @see #ChannelException(ResponseCode, String, Throwable, Recoverable, WaveId,
   *      WaveletId)
   */
  public ChannelException(String message, Recoverable isRecoverable) {
    this(ResponseCode.INTERNAL_ERROR, message, null, isRecoverable, null, null);
  }

  /**
   * Whether recovery by reconnection is possible.
   */
  public Recoverable getRecoverable() {
    return isRecoverable;
  }

  /**
   * Gets the associated wave id (may be null).
   */
  public WaveId getWaveId() {
    return waveId;
  }

  /**
   * Gets the associated wavelet id (may be null).
   */
  public WaveletId getWaveletId() {
    return waveletId;
  }

  /**
   * Gets the identified cause
   */
  public ResponseCode getResponseCode() {
    return responseCode;
  }

  @Override
  public String toString() {
    // Space before \n in case some logger swallows the newline.
    return super.toString() + ", \nwaveId: " + waveId + " waveletId: " + waveletId
        + ", isRecoverable: " + isRecoverable + ", responseCode: " + responseCode;
  }
}
