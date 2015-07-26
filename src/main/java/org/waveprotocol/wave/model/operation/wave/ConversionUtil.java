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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Methods for converting between {@link CoreWaveletOperation}s and
 * {@link WaveletOperation}s.
 *
 */
public final class ConversionUtil {

  /**
   * Converts a {@link CoreWaveletOperation} to a {@link WaveletOperation}.
   *
   * @param context the operation's context
   * @param coreOp the operation
   * @return a {@link WaveletOperation} with the given context, representing the
   *         action of the given {@link CoreWaveletOperation}
   */
  public static WaveletOperation fromCoreWaveletOperation(
      WaveletOperationContext context, CoreWaveletOperation coreOp) {
    if (coreOp instanceof CoreRemoveParticipant) {
      ParticipantId participantId = ((CoreRemoveParticipant) coreOp).getParticipantId();
      return new RemoveParticipant(context, participantId);
    } else if (coreOp instanceof CoreAddParticipant) {
      ParticipantId participantId = ((CoreAddParticipant) coreOp).getParticipantId();
      return new AddParticipant(context, participantId);
    } else if (coreOp instanceof CoreWaveletDocumentOperation) {
      CoreWaveletDocumentOperation waveletDocOp = (CoreWaveletDocumentOperation) coreOp;
      return new WaveletBlipOperation(waveletDocOp.getDocumentId(),
          new BlipContentOperation(context, waveletDocOp.getOperation()));
    } else if (coreOp instanceof CoreNoOp) {
      return new NoOp(context);
    } else {
      throw new IllegalArgumentException("unknown operation type");
    }
  }

  /**
   * Converts a {@link WaveletOperation} to a {@link CoreWaveletOperation}.
   *
   * @param op the operation
   * @return a {@link CoreWaveletOperation} representing the action of the given
   *         {@link WaveletOperation}
   */
  public static CoreWaveletOperation toCoreWaveletOperation(WaveletOperation op) {
    if (op instanceof RemoveParticipant) {
      ParticipantId participantId = ((RemoveParticipant) op).getParticipantId();
      return new CoreRemoveParticipant(participantId);
    } else if (op instanceof AddParticipant) {
      ParticipantId participantId = ((AddParticipant) op).getParticipantId();
      return new CoreAddParticipant(participantId);
    } else if (op instanceof WaveletBlipOperation) {
      WaveletBlipOperation waveletBlipOp = (WaveletBlipOperation) op;
      BlipOperation blipOp = waveletBlipOp.getBlipOp();
      if (blipOp instanceof BlipContentOperation) {
        DocOp contentOp = ((BlipContentOperation) blipOp).getContentOp();
        return new CoreWaveletDocumentOperation(waveletBlipOp.getBlipId(), contentOp);
      } else if (blipOp instanceof SubmitBlip) {
        // There is no "core" submit operation, they are ignored by translating
        // them into no-ops
        return CoreNoOp.INSTANCE;
      } else {
        throw new IllegalArgumentException("unknown blip operation type");
      }
    } else if (op instanceof NoOp) {
      return CoreNoOp.INSTANCE;
    } else {
      throw new IllegalArgumentException("unknown wavelet operation type");
    }
  }

  private ConversionUtil() {} // prevent instantiation
}
