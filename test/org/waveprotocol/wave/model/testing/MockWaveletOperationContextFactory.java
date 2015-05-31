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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.operation.wave.AbstractWaveletOperationContextFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * WaveletOperationContext.Factory that supports setting the timestamp
 * and default participant id to use.
 *
 */
public class MockWaveletOperationContextFactory extends AbstractWaveletOperationContextFactory {
  private long timeMillis;
  private ParticipantId participantId;

  @Override
  protected long currentTimeMillis() {
    return timeMillis;
  }

  @Override
  public ParticipantId getParticipantId() {
    return participantId;
  }

  /**
   * Sets the timestamp for future WaveletOperationContext objects generated
   * by this factory.
   */
  public MockWaveletOperationContextFactory setCurrentTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
    return this;
  }

  /**
   * Sets the participant for future WaveletOperationContext objects generated
   * by this factory.
   */
  public MockWaveletOperationContextFactory setParticipantId(ParticipantId participantId) {
    this.participantId = participantId;
    return this;
  }
}
