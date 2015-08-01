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

import com.google.gwt.event.shared.GwtEvent;
import org.waveprotocol.wave.model.wave.ParticipantId;
import java.util.Set;

public class WaveCreationEvent extends GwtEvent<WaveCreationEventHandler> {
  public static final Type<WaveCreationEventHandler> TYPE = new Type<WaveCreationEventHandler>();

  private final Set<ParticipantId> participants;

  public WaveCreationEvent() {
    this.participants = null;
  }

  public WaveCreationEvent(Set<ParticipantId> participants) {
    this.participants = participants;
  }

  @Override
  protected void dispatch(WaveCreationEventHandler handler) {
    handler.onCreateRequest(this, participants);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<WaveCreationEventHandler> getAssociatedType() {
    return TYPE;
  }
}
