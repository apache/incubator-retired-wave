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
import com.google.wave.api.NonJsonSerializable;
import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;

/**
 * The base class for all events.
 */
public abstract class AbstractEvent implements Event {

  /** The wavelet in which this event occurs. */
  @NonJsonSerializable protected final Wavelet wavelet;

  /** The type of the event. */
  private final EventType type;

  /** The id of the participant that triggered this event. */
  private final String modifiedBy;

  /** The timestamp of the event. */
  private final long timestamp;

  /**
   * The id of the blip in which this event occurs, or the root blip id in the
   * case of a wavelet event.
   */
  private final String blipId;

  /**
   * The message bundle this event belongs to.
   */
  @NonJsonSerializable private final EventMessageBundle bundle;

  /**
   * Constructor.
   *
   * @param eventType the type of the event.
   * @param wavelet the wavelet in which this event occurs.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   */
  protected AbstractEvent(EventType eventType, Wavelet wavelet, EventMessageBundle bundle,
      String modifiedBy, long timestamp, String blipId) {
    this.type = eventType;
    this.wavelet = wavelet;
    this.modifiedBy = modifiedBy;
    this.timestamp = timestamp;
    this.blipId = blipId;
    this.bundle = bundle;
  }

  /**
   * Constructor for deserialization.
   */
  protected AbstractEvent() {
    this(null, null, null, null, -1, null);
  }

  @Override
  public EventType getType() {
    return type;
  }

  @Override
  public Wavelet getWavelet() {
    return wavelet;
  }

  @Override
  public Blip getBlip() {
    return wavelet.getBlip(blipId);
  }

  @Override
  public String getModifiedBy() {
    return modifiedBy;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public EventMessageBundle getBundle() {
    return bundle;
  }
}
