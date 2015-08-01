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

package org.waveprotocol.wave.model.operation.wave;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Creates operation contexts with no version information.
 * Implementers must implement the abstract getParticipantId() and
 * currentTimeMillis() methods.
 *
 */
public abstract class AbstractWaveletOperationContextFactory
    implements WaveletOperationContext.Factory {

  /** @return the default operation creator. */
  protected abstract ParticipantId getParticipantId();

  /** @return the timestamp to use in created contexts. */
  protected abstract long currentTimeMillis();

  @Override
  public WaveletOperationContext createContext() {
    return createContext(getParticipantId());
  }

  @Override
  public WaveletOperationContext createContext(ParticipantId creator) {
    // NOTE(zdwang): The final version of the operation cannot be determined at
    // the time it's created. We can only infer its final version once the
    // server acks it. Hence we use a version increment of 0.
    return new WaveletOperationContext(creator, currentTimeMillis(), 0L);
  }
}
