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
 * Event triggered when contributors are added and/or removed from a blip.
 */
public class BlipContributorsChangedEvent extends AbstractEvent {

  /** The newly added contributors. */
  private final List<String> contributorsAdded;

  /** The removed contributors. */
  private final List<String> contributorsRemoved;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet where this event occurred.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param blipId the id of the blip in which the the contributors were added
   *     and/or removed from.
   * @param contributorsAdded the added contributors.
   * @param contributorsRemoved the removed contributors.
   */
  public BlipContributorsChangedEvent(Wavelet wavelet, EventMessageBundle bundle,
      String modifiedBy, Long timestamp, String blipId, List<String> contributorsAdded,
      List<String> contributorsRemoved) {
    super(EventType.BLIP_CONTRIBUTORS_CHANGED, wavelet, bundle, modifiedBy, timestamp, blipId);
    this.contributorsAdded = new ArrayList<String>(contributorsAdded);
    this.contributorsRemoved = new ArrayList<String>(contributorsRemoved);
  }

  /**
   * Constructor for deserialization.
   */
  BlipContributorsChangedEvent() {
    this.contributorsAdded = null;
    this.contributorsRemoved = null;
  }

  /**
   * Returns a list of the new contributors.
   *
   * @return the added contributors.
   */
  public List<String> getContributorsAdded() {
    return contributorsAdded;
  }

  /**
   * Returns a list of the removed contributors.
   *
   * @return the removed contributors.
   */
  public List<String> getContributorsRemoved() {
    return contributorsRemoved;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static BlipContributorsChangedEvent as(Event event) {
    if (!(event instanceof BlipContributorsChangedEvent)) {
      return null;
    }
    return BlipContributorsChangedEvent.class.cast(event);
  }
}
