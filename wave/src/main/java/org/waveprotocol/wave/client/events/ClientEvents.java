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

package org.waveprotocol.wave.client.events;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

public class ClientEvents {
  private static final ClientEvents INSTANCE = GWT.create(ClientEvents.class);

  public static ClientEvents get() {
    return INSTANCE;
  }

  private final HandlerManager handlerManager = new HandlerManager(this);

  public HandlerRegistration addDebugMessageHandler(
      DebugMessageEventHandler handler) {
    return handlerManager.addHandler(DebugMessageEvent.TYPE, handler);
  }

  public HandlerRegistration addNetworkStatusEventHandler(
      NetworkStatusEventHandler handler) {
    return handlerManager.addHandler(NetworkStatusEvent.TYPE, handler);
  }

  public HandlerRegistration addWaveCreationEventHandler(
      WaveCreationEventHandler handler) {
    return handlerManager.addHandler(WaveCreationEvent.TYPE, handler);
  }

  public HandlerRegistration addWaveOpenEventHandler(
      WaveOpenEventHandler handler) {
    return handlerManager.addHandler(WaveOpenEvent.TYPE, handler);
  }

  public HandlerRegistration addWaveSelectionEventHandler(
      WaveSelectionEventHandler handler) {
    return handlerManager.addHandler(WaveSelectionEvent.TYPE, handler);
  }

  public void fireEvent(GwtEvent<?> event) {
    handlerManager.fireEvent(event);
  }
}
