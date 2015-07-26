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

package org.waveprotocol.box.server.robots;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.wave.api.Attachment;
import com.google.wave.api.Element;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.impl.GsonFactory;
import com.google.wave.api.v2.ElementGsonAdaptorV2;

import java.util.NavigableMap;

/**
 * Guice module for setting up the {@link RobotSerializer}.
 * 
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotSerializerModule extends AbstractModule {

  @Provides
  @Singleton
  @Inject
  RobotSerializer provideRobotSerializer() {
    NavigableMap<ProtocolVersion, Gson> gsons = Maps.newTreeMap();
    Gson gsonForPostV2 = new GsonFactory().create();
    gsons.put(ProtocolVersion.V2_2, gsonForPostV2);
    // Remove lines below if we want to stop support for <0.22
    gsons.put(ProtocolVersion.V2_1, gsonForPostV2);

    GsonFactory factoryForV2 = new GsonFactory();
    ElementGsonAdaptorV2 elementGsonAdaptorV2 = new ElementGsonAdaptorV2();
    factoryForV2.registerTypeAdapter(Element.class, elementGsonAdaptorV2);
    factoryForV2.registerTypeAdapter(Attachment.class, elementGsonAdaptorV2);
    gsons.put(ProtocolVersion.V2, factoryForV2.create());

    return new RobotSerializer(gsons, ProtocolVersion.DEFAULT);
  }

  @Override
  protected void configure() {
  }
}
