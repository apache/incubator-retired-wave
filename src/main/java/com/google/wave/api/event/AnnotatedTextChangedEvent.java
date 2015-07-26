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
 * Event triggered when text with an annotation has changed.
 */
public class AnnotatedTextChangedEvent extends AbstractEvent {

  /** The name of the annotation. */
  private final String name;

  /** The value of the annotation that changed. */
  private final String value;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet where this event occurred.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param blipId the id of the blip that holds the text.
   * @param name the name of the annotation.
   * @param value the value of the annotation that changed.
   */
  public AnnotatedTextChangedEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String blipId, String name, String value) {
    super(EventType.ANNOTATED_TEXT_CHANGED, wavelet, bundle, modifiedBy, timestamp, blipId);
    this.name = name;
    this.value = value;
  }

  /**
   * Constructor for deserialization.
   */
  AnnotatedTextChangedEvent() {
    this.name = null;
    this.value = null;
  }

  /**
   * Returns the name of the annotation.
   *
   * @return the name of the annotation.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the value of the annotation that changed.
   *
   * @return the value of the annotation that changed.
   */
  public String getValue() {
    return value;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static AnnotatedTextChangedEvent as(Event event) {
    if (!(event instanceof AnnotatedTextChangedEvent)) {
      return null;
    }
    return AnnotatedTextChangedEvent.class.cast(event);
  }
}
