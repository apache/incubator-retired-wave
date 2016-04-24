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

import java.util.ArrayList;
import java.util.List;

/**
 * Event triggered when participants are added and/or removed from a wavelet.
 */
public class WaveletParticipantsChangedEvent extends AbstractEvent {

  /** The newly added participants. */
  private final List<String> participantsAdded;

  /** The removed participants. */
  private final List<String> participantsRemoved;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet in which the participants were added or removed.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param rootBlipId the root blip id of the wavelet where this event occurs.
   * @param participantsAdded the added participants.
   * @param participantsRemoved the removed participants.
   */
  public WaveletParticipantsChangedEvent(Wavelet wavelet, EventMessageBundle bundle,
      String modifiedBy, Long timestamp, String rootBlipId, List<String> participantsAdded,
      List<String> participantsRemoved) {
    super(EventType.WAVELET_PARTICIPANTS_CHANGED, wavelet, bundle, modifiedBy, timestamp,
        rootBlipId);
    this.participantsAdded = new ArrayList<String>(participantsAdded);
    this.participantsRemoved = new ArrayList<String>(participantsRemoved);
  }

  /**
   * Constructor for deserialization.
   */
  WaveletParticipantsChangedEvent() {
    this.participantsAdded = null;
    this.participantsRemoved = null;
  }

  /**
   * Returns a list of the new participants.
   *
   * @return the added participants.
   */
  public List<String> getParticipantsAdded() {
    return participantsAdded;
  }

  /**
   * Returns a list of the removed participants.
   *
   * @return the removed participants.
   */
  public List<String> getParticipantsRemoved() {
    return participantsRemoved;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static WaveletParticipantsChangedEvent as(Event event) {
    if (!(event instanceof WaveletParticipantsChangedEvent)) {
      return null;
    }
    return WaveletParticipantsChangedEvent.class.cast(event);
  }
}
