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

package org.waveprotocol.wave.model.operation.core;

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

public abstract class CoreWaveletOperation implements Operation<CoreWaveletData> {

  // Package visibility so that the set of subtypes is fixed.
  CoreWaveletOperation() {}

  /**
   * This method delegates the operation logic to {@link #doApply(CoreWaveletData)}.
   */
  public final void apply(CoreWaveletData wavelet) throws OperationException {
    // Execute subtype logic first, because if the subtype logic throws an exception, we must
    // leave this wrapper untouched as though the operation never happened. The subtype is
    // responsible for making sure if they throw an exception they must leave themselves in a
    // state as if the op never happened.
    doApply(wavelet);
  }

  /**
   * Applies this operation's logic to a given wavelet. This method can be
   * arbitrarily overridden by subclasses.
   *
   * @param wavelet wavelet on which this operation is to apply itself
   * @throws OperationException
   */
  protected abstract void doApply(CoreWaveletData wavelet) throws OperationException;

  /**

   * Get the inverse of the operation, such that any {@CoreWaveletData} object applying this
   * operation followed by its inverse will remain unchanged.
   *
   * @return the inverse of this operation
   */
  public abstract CoreWaveletOperation getInverse();
}
