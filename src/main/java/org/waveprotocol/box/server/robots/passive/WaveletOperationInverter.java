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

package org.waveprotocol.box.server.robots.passive;

import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperationVisitor;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.SubmitBlip;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationVisitor;

/**
 * Approximately inverts an applied wavelet operation.
 *
 * The only contract this inverter provides is that:
 *
 * <pre class="code">invert(o) ; o == id </pre>
 *
 * i.e., given any state {@code s} that was reached by an operation {@code o},
 * the "previous" state {@code s'} can be constructed {@code s' = invert(o)(s)}
 * such that applying {@code o} to it will return to the given state {@code s}.
 *
 * This inverter does not accept operations that do not appear in a wavelet's
 * history (e.g., {@link VersionUpdateOp}).
 *
 * TODO(anorth): Move to {@link org.waveprotocol.wave.model.operation.wave}.
 *
 * @author hearnden@google.com (David Hearnden)
 */
final class WaveletOperationInverter implements WaveletOperationVisitor {

  /**
   * Inverts a blip operation, ignoring metadata.
   */
  final static class BlipOperationInverter implements BlipOperationVisitor {
    private final WaveletOperationContext reverseContext;
    private BlipOperation inverse;

    BlipOperationInverter(WaveletOperationContext reverseContext) {
      this.reverseContext = reverseContext;
    }

    static BlipOperation invert(WaveletOperationContext reverseContext, BlipOperation op) {
      return new BlipOperationInverter(reverseContext).visit(op);
    }

    private BlipOperation visit(BlipOperation op) {
      op.acceptVisitor(this);
      return inverse;
    }

    @Override
    public void visitBlipContentOperation(BlipContentOperation op) {
      inverse = new BlipContentOperation(reverseContext, DocOpInverter.invert(op.getContentOp()));
    }

    @Override
    public void visitSubmitBlip(SubmitBlip op) {
      inverse = new SubmitBlip(reverseContext);
    }
  }

  private final WaveletOperationContext reverseContext;
  private WaveletOperation inverse;

  WaveletOperationInverter(WaveletOperationContext reverseContext) {
    this.reverseContext = reverseContext;
  }

  /**
   * Inverts an operation.
   */
  static WaveletOperation invert(WaveletOperation op) {
    WaveletOperationContext forwardContext = op.getContext();
    WaveletOperationContext reverseContext = new WaveletOperationContext( //
        forwardContext.getCreator(),
        // Lie, and keep the same modification time.
        forwardContext.getTimestamp(),
        // Correctly invert the version increment.
        -forwardContext.getVersionIncrement(),
        // Lie again, and report the hashed version as the same as after op.
        // This makes it out of sync with the version number.
        forwardContext.getHashedVersion());
    return new WaveletOperationInverter(reverseContext).visit(op);
  }

  private WaveletOperation visit(WaveletOperation op) {
    op.acceptVisitor(this);
    return inverse;
  }

  @Override
  public void visitWaveletBlipOperation(WaveletBlipOperation op) {
    inverse = new WaveletBlipOperation(
        op.getBlipId(), BlipOperationInverter.invert(reverseContext, op.getBlipOp()));
  }

  @Override
  public void visitVersionUpdateOp(VersionUpdateOp op) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitAddParticipant(AddParticipant op) {
    inverse = new RemoveParticipant(reverseContext, op.getParticipantId());
  }

  @Override
  public void visitRemoveParticipant(RemoveParticipant op) {
    inverse = new AddParticipant(reverseContext, op.getParticipantId());
  }

  @Override
  public void visitNoOp(NoOp op) {
    inverse = new NoOp(reverseContext);
  }
}
