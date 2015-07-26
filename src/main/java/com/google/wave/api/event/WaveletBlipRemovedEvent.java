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
 * Event triggered when a blip is removed.
 */
public class WaveletBlipRemovedEvent extends AbstractEvent {

  /** The id of the removed blip. */
  private final String removedBlipId;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet in which this blip was removed.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that removed this blip.
   * @param timestamp the timestamp of this event.
   * @param rootBlipId the root blip id of the wavelet where this event occurs.
   * @param removedBlipId the id of the removed blip.
   */
  public WaveletBlipRemovedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String rootBlipId, String removedBlipId) {
    super(EventType.WAVELET_BLIP_REMOVED, wavelet, bundle, modifiedBy, timestamp, rootBlipId);
    this.removedBlipId = removedBlipId;
  }

  /**
   * Constructor for deserialization.
   */
  WaveletBlipRemovedEvent() {
    this.removedBlipId = null;
  }

  /**
   * Returns the id of the removed blip.
   *
   * @return the id of the removed blip.
   */
  public String getRemovedBlipId() {
    return removedBlipId;
  }

  /**
   * Returns the removed blip.
   *
   * @return the removed blip.
   */
  public Blip getRemovedBlip() {
    return wavelet.getBlip(removedBlipId);
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static WaveletBlipRemovedEvent as(Event event) {
    if (!(event instanceof WaveletBlipRemovedEvent)) {
      return null;
    }
    return WaveletBlipRemovedEvent.class.cast(event);
  }
}
