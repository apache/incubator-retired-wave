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
 * Event triggered when a form button is clicked.
 */
public class FormButtonClickedEvent extends AbstractEvent {

  /** The name of the button that was clicked. */
  private final String buttonName;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet where this event occurred.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param blipId the id of the submitted blip.
   * @param buttonName the name of the button that was clicked.
   */
  public FormButtonClickedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String blipId, String buttonName) {
    super(EventType.FORM_BUTTON_CLICKED, wavelet, bundle, modifiedBy, timestamp, blipId);
    this.buttonName = buttonName;
  }

  /**
   * Constructor for deserialization.
   */
  FormButtonClickedEvent() {
    this.buttonName = null;
  }

  /**
   * Returns the name of the button that was clicked.
   *
   * @return the name of the button that was clicked.
   */
  public String getButtonName() {
    return buttonName;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static FormButtonClickedEvent as(Event event) {
    if (!(event instanceof FormButtonClickedEvent)) {
      return null;
    }
    return FormButtonClickedEvent.class.cast(event);
  }
}
