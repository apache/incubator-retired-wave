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

package com.google.wave.api.data.converter;

import com.google.wave.api.ProtocolVersion;

import java.util.Map.Entry;
import java.util.NavigableMap;

/**
 * A simple utility class that manages the {@link EventDataConverter} for
 * various protocol versions.
 *
 */
public class EventDataConverterManager {

  /** A map of protocol version to {@link EventDataConverter}. */
  private final NavigableMap<ProtocolVersion, EventDataConverter> eventDataConverters;

  /**
   * Constructor.
   *
   * @param eventDataConverters a map of {@link EventDataConverter}.
   */
  public EventDataConverterManager(
      NavigableMap<ProtocolVersion, EventDataConverter> eventDataConverters) {
    this.eventDataConverters = eventDataConverters;
  }

  /**
   * Returns an instance of an {@link EventDataConverter} for the given
   * protocol version.
   *
   * @param protocolVersion the protocol version.
   * @return an instance of an {@link EventDataConverter}, or {@code null} if
   *     there is no converter for the given version.
   */
  public EventDataConverter getEventDataConverter(ProtocolVersion protocolVersion) {
    // Get the latest instance of {@link EventDataConverter} for a protocol
    // version that is less than or equal to the given version, as not every
    // protocol version will have a versioned {@link EventDataConverter}.
    Entry<ProtocolVersion, EventDataConverter> entry = eventDataConverters.floorEntry(
        protocolVersion);
    if (entry == null) {
      return null;
    }
    return entry.getValue();
  }
}
