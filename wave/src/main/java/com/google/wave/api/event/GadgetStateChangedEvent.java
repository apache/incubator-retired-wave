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

import java.util.HashMap;
import java.util.Map;

/**
 * Event triggered when the state of a gadget has changed.
 */
public class GadgetStateChangedEvent extends AbstractEvent {

  /** The index of the gadget that changed in the document. */
  private final Integer index;

  /** The old state of the gadget. */
  private Map<String, String> oldState;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet where this event occurred.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param blipId the id of the submitted blip.
   * @param index the index of the gadget that changed in the document.
   * @param oldState the old state of the gadget.
   */
  public GadgetStateChangedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String blipId, int index, Map<String, String> oldState) {
    super(EventType.GADGET_STATE_CHANGED, wavelet, bundle, modifiedBy, timestamp, blipId);
    this.index = index;
    this.oldState = new HashMap<String, String>(oldState);
  }

  /**
   * Constructor for deserialization.
   */
  GadgetStateChangedEvent() {
    this.index = null;
    this.oldState = null;
  }

  /**
   * Returns the index of the gadget that changed in the document.
   *
   * @return the index of the gadget that changed in the document.
   */
  public Integer getIndex() {
    return index;
  }

  /**
   * Returns the old state of the gadget.
   *
   * @return the old state of the gadget.
   */
  public Map<String, String> getOldState() {
    return oldState;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static GadgetStateChangedEvent as(Event event) {
    if (!(event instanceof GadgetStateChangedEvent)) {
      return null;
    }
    return GadgetStateChangedEvent.class.cast(event);
  }
}
