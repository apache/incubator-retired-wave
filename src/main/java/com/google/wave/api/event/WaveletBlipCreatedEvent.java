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

import com.google.wave.api.Blip;
import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;

/**
 * Event triggered when a new blip is created.
 */
public class WaveletBlipCreatedEvent extends AbstractEvent {

  /** The id of the new blip. */
  private final String newBlipId;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet in which this new blip was created.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that created this new blip.
   * @param timestamp the timestamp of this event.
   * @param rootBlipId the root blip id of the wavelet where this event occurs.
   * @param newBlipId the id of the new blip.
   */
  public WaveletBlipCreatedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String rootBlipId, String newBlipId) {
    super(EventType.WAVELET_BLIP_CREATED, wavelet, bundle, modifiedBy, timestamp, rootBlipId);
    this.newBlipId = newBlipId;
  }

  /**
   * Constructor for deserialization.
   */
  WaveletBlipCreatedEvent() {
    this.newBlipId = null;
  }

  /**
   * Returns the id of the new blip.
   *
   * @return the id of the new blip.
   */
  public String getNewBlipId() {
    return newBlipId;
  }

  /**
   * Returns the new blip.
   *
   * @return the new blip.
   */
  public Blip getNewBlip() {
    return wavelet.getBlip(newBlipId);
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static WaveletBlipCreatedEvent as(Event event) {
    if (!(event instanceof WaveletBlipCreatedEvent)) {
      return null;
    }
    return WaveletBlipCreatedEvent.class.cast(event);
  }
}
