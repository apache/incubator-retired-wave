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

import org.waveprotocol.wave.model.wave.data.WaveletData;

import org.waveprotocol.wave.model.operation.OperationException;

import java.util.Collections;
import java.util.List;

/**
 * Operation class for a no-op.
 *
 */
public final class NoOp extends WaveletOperation {

  private static final int HASH = NoOp.class.getName().hashCode();

  /**
   * Constructs a no-op.
   *
   * @param context   operation context
   */
  public NoOp(WaveletOperationContext context) {
    super(context);
  }

  /**
   * Does nothing.
   */
  @Override
  protected void doApply(WaveletData wave) {
    // do nothing.
  }

  @Override
  public void acceptVisitor(WaveletOperationVisitor visitor) {
    visitor.visitNoOp(this);
  }

  @Override
  public String toString() {
    return "no-op " + suffixForToString();
  }

  @Override
  public int hashCode() {
    /*
     * NOTE(user): We may be able to get rid of this hash function in the
     * future if this class becomes a singleton.
     */
    return HASH;
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    return obj instanceof NoOp;
  }

  @Override
  public List<? extends WaveletOperation> applyAndReturnReverse(WaveletData target)
      throws OperationException {
    WaveletOperation reverse = new NoOp(createReverseContext(target));
    apply(target);
    return Collections.singletonList(reverse);
  }

}
