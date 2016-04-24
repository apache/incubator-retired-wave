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

package org.waveprotocol.wave.model.wave.opbased;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.SubmitBlip;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;

import java.util.Collections;
import java.util.Set;

/**
 * Implements the {@code Blip} interface by translating its methods into
 * operation objects which are applied to a {@link BlipData}. Refer to comment
 * in {@link OpBasedWavelet} for an explanation of how the adapters fit
 * together.
 *
 * NOTE(user): this adapter consumes operations produced locally by the blip
 * content's document adapter by boxing it as a blip operation and passing it to
 * the wavelet adapter. The second boxing from a blip op to a wavelet op is done
 * here rather than in the wavelet adapter because this adapter already knows
 * the blip id, and means that the wavelet adapter can have a single sink for
 * all (boxed) blip ops, rather than a sink per blip.
 *
 * @see OpBasedWavelet
 */
public class OpBasedBlip implements Blip {

  /** Primitive-view of the adapted blip. */
  private final BlipData blip;

  /** Adapter of the wavelet in which this blip appears, with which this adapter collaborates. */
  private final OpBasedWavelet wavelet;

  /** Sink to which produced operations are sent for remote notification. */
  private final SilentOperationSink<WaveletBlipOperation> outputSink;

  /**
   * Creates a blip adapter.
   *
   * @param blip        primitive-view of a blip
   * @param wavelet     adapter of the wavelet in which the primitive blip appears
   * @param outputSink  sink to which operations produced by this adapters should be sent.
   */
  public OpBasedBlip(BlipData blip, OpBasedWavelet wavelet,
      SilentOperationSink<WaveletBlipOperation> outputSink) {
    this.blip = blip;
    this.wavelet = wavelet;
    this.outputSink = outputSink;
    blip.getContent().init(new SilentOperationSink<DocOp>() {
      public void consume(DocOp op) {
        OpBasedBlip.this.consume(op);
      }
    });
  }

  /**
   * Implements the strategy for consuming operations sent to this adapter from
   * a document adapter, after the operation has already been applied locally. A
   * received operation is boxed as a blip operation, then used to update the
   * primitive blip, then boxed as a wave operation and sent to the wavelet
   * adapter.
   */
  private void consume(DocOp op) {
    // Box as blip op, and update local blip
    BlipContentOperation blipOp = new BlipContentOperation(wavelet.createContext(), op);
    blipOp.update(OpBasedBlip.this.blip);
    // Box as wavelet op, and pass to wavelet adapter
    outputSink.consume(new WaveletBlipOperation(getId(), blipOp));
  }

  /**
   * Applies the op to the adapted wavelet and then outputs the op for remote notification
   *
   * @param op The op to apply
   */
  private void applyAndSend(BlipOperation op) {
    // Apply locally
    try {
      op.apply(blip);
    } catch (OperationException e) {
      wavelet.handleException(e);
      return;
    }
    // Pass to wave
    outputSink.consume(new WaveletBlipOperation(blip.getId(), op));
  }

  //
  // Mutator to operation translations
  //


  @Override
  public void submit() {
    applyAndSend(new SubmitBlip(wavelet.createContext()));
  }

  //
  // Adapted accessors
  //

  /**
   * Adapts the primitive-blip's document-operation sink as a {@link MutableDocument}.
   */
  @Override
  public Document getContent() {
    return blip.getContent().getMutableDocument();
  }

  //
  // Vanilla accessors.
  //

  @Override
  public OpBasedWavelet getWavelet() {
    return wavelet;
  }

  @Override
  public ParticipantId getAuthorId() {
    return blip.getAuthor();
  }

  @Override
  public Set<ParticipantId> getContributorIds() {
    return Collections.unmodifiableSet(blip.getContributors());
  }

  @Override
  public Long getLastModifiedTime() {
    return blip.getLastModifiedTime();
  }

  @Override
  public Long getLastModifiedVersion() {
    return blip.getLastModifiedVersion();
  }

  @Override
  public String getId() {
    return blip.getId();
  }

  @Override
  public int hashCode() {
    return 37 + blip.getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof Blip)) {
      return false;
    } else {
      return getId().equals(((Blip) obj).getId());
    }
  }

  @Override
  public String toString() {
    return "OpBasedBlip { " + blip + " }";
  }
}
