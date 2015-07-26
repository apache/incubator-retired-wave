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

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.v21.EventDataConverterV21;
import com.google.wave.api.data.converter.v22.EventDataConverterV22;

import java.util.NavigableMap;

/**
 * Guice module for setting up the data conversion part of the {@link RobotSerializer}.
 *
 */
public class EventDataConverterModule extends AbstractModule {

  /**
   * @return A singleton instance of a {@link EventDataConverterManager}.
   */
  @Singleton
  @Provides
  static EventDataConverterManager provideEventDataConverterManager() {
    // v0.1 till v0.21 use the same event data converter.
    NavigableMap<ProtocolVersion, EventDataConverter> converters = Maps.newTreeMap();
    EventDataConverterV21 eventDataConverterV21 = new EventDataConverterV21();
    converters.put(ProtocolVersion.V1, eventDataConverterV21);
    converters.put(ProtocolVersion.V2, eventDataConverterV21);
    converters.put(ProtocolVersion.V2_1, eventDataConverterV21);
    converters.put(ProtocolVersion.V2_2, new EventDataConverterV22());
    return new EventDataConverterManager(converters);
  }

  @Override
  protected void configure() {
  }
}
