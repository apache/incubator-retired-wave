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

import java.util.HashMap;
import java.util.Map;

/**
 * The types of events.
 */
public enum EventType {
  WAVELET_BLIP_CREATED(WaveletBlipCreatedEvent.class),
  WAVELET_BLIP_REMOVED(WaveletBlipRemovedEvent.class),
  WAVELET_PARTICIPANTS_CHANGED(WaveletParticipantsChangedEvent.class),
  WAVELET_SELF_ADDED(WaveletSelfAddedEvent.class),
  WAVELET_SELF_REMOVED(WaveletSelfRemovedEvent.class),
  WAVELET_TITLE_CHANGED(WaveletTitleChangedEvent.class),
  WAVELET_CREATED(WaveletCreatedEvent.class),
  WAVELET_FETCHED(WaveletFetchedEvent.class),
  WAVELET_TAGS_CHANGED(WaveletTagsChangedEvent.class),

  BLIP_CONTRIBUTORS_CHANGED(BlipContributorsChangedEvent.class),
  BLIP_SUBMITTED(BlipSubmittedEvent.class),
  DOCUMENT_CHANGED(DocumentChangedEvent.class),
  FORM_BUTTON_CLICKED(FormButtonClickedEvent.class),
  GADGET_STATE_CHANGED(GadgetStateChangedEvent.class),
  ANNOTATED_TEXT_CHANGED(AnnotatedTextChangedEvent.class),

  OPERATION_ERROR(OperationErrorEvent.class),
  UNKNOWN(null);

  private static final Map<Class<?>, EventType> REVERSE_LOOKUP_MAP =
      new HashMap<Class<?>, EventType>();

  static {
    for (EventType eventType : EventType.values()) {
      REVERSE_LOOKUP_MAP.put(eventType.getClazz(), eventType);
    }
  }

  /** The class that represents this event. */
  private final Class<? extends Event> clazz;

  /**
   * Constructor.
   *
   * @param clazz the class that represents this event.
   */
  private EventType(Class<? extends Event> clazz) {
    this.clazz = clazz;
  }

  /**
   * Returns the class that represents this event type.
   *
   * @return the class that represents this event type.
   */
  public Class<? extends Event> getClazz() {
    return clazz;
  }

  /**
   * Converts a string into an {@link EventType} ignoring case in the process.
   *
   * @param name the name of the event type.
   * @return the converted event type.
   */
  public static EventType valueOfIgnoreCase(String name) {
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }

  /**
   * Returns an {@link EventType} enumeration that has the given class. If no
   * match is found, UNKNOWN is returned.
   *
   * @param clazz The class that represents an event type.
   * @return An {@link EventType} that has the given class.
   */
  public static EventType fromClass(Class<?> clazz) {
    if (!REVERSE_LOOKUP_MAP.containsKey(clazz)) {
      return UNKNOWN;
    }
    return REVERSE_LOOKUP_MAP.get(clazz);
  }
}
