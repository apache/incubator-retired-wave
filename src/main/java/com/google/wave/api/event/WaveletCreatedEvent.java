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

package com.google.wave.api.event;

import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;

/**
 * Event triggered when a new wavelet is created. This event is only triggered
 * if the robot creates a new wavelet and can be used to initialize the newly
 * created wave. Wavelets created by other participants remain invisible to the
 * robot until the robot is added to the wave in which case
 * {@link WaveletSelfAddedEvent} is triggered.
 */
public class WaveletCreatedEvent extends AbstractEvent {

  /** The message that was passed into the wavelet create operation. */
  private final String message;

  /** The id of the new wave. */
  private final String waveId;

  /** The id of the new wavelet. */
  private final String waveletId;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet in which this event was triggered.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param rootBlipId the root blip id of the new wavelet.
   * @param message the message that was passed into the wavelet create
   *     operation.
   * @param waveId the id of the new wave.
   * @param waveletId the id of the new wavelet.
   */
  public WaveletCreatedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String rootBlipId, String message, String waveId, String waveletId) {
    super(EventType.WAVELET_CREATED, wavelet, bundle, modifiedBy, timestamp, rootBlipId);
    this.message = message;
    this.waveId = waveId;
    this.waveletId = waveletId;
  }

  /**
   * Constructor for deserialization.
   */
  WaveletCreatedEvent() {
    this.message = null;
    this.waveId = null;
    this.waveletId = null;
  }

  /**
   * Returns the message that was passed into the wavelet create operation.
   *
   * @return the message that was passed into the wavelet create operation.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the id of the new wave.
   *
   * @return the id of the new wave.
   */
  public String getWaveId() {
    return waveId;
  }

  /**
   * Returns the id of the new wavelet.
   *
   * @return the id of the new wavelet.
   */
  public String getWaveletId() {
    return waveletId;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static WaveletCreatedEvent as(Event event) {
    if (!(event instanceof WaveletCreatedEvent)) {
      return null;
    }
    return WaveletCreatedEvent.class.cast(event);
  }
}
