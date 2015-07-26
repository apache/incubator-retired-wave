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
package org.waveprotocol.box.expimp;

import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.LinkedList;
import java.util.List;

/**
 * Converts domain names in exported data.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class DomainConverter {

  /**
   * Replaces domain of WaveId.
   *
   * @param waveId source waveId
   * @param waveDomain target wave domain
   * @return wave id with waveDomain if waveDomain is not null, otherwise source wave id.
   */
  public static WaveId convertWaveId(WaveId waveId, String waveDomain) {
    if (waveDomain != null) {
      return WaveId.of(waveDomain, waveId.getId());
    }
    return waveId;
  }

  /**
   * Replaces domain of WaveletId.
   *
   * @param waveletId source waveletId
   * @param waveDomain target wave domain
   * @return wavelet id with waveDomain if waveDomain is not null, otherwise source wavelet id.
   */
  public static WaveletId convertWaveletId(WaveletId waveletId, String waveDomain) throws InvalidParticipantAddress {
    if (waveDomain != null) {
      if (IdUtil.isUserDataWavelet(waveletId)) {
        String sourceParticipant = IdUtil.getUserDataWaveletAddress(waveletId);
        String targetParticipant = convertParticipantId(sourceParticipant, waveDomain).toString();
        String targetWaveletId = IdUtil.join(IdUtil.USER_DATA_WAVELET_PREFIX, targetParticipant);
        return WaveletId.of(waveDomain, targetWaveletId);
      }
      return WaveletId.of(waveDomain, waveletId.getId());
    }
    return waveletId;
  }

  /**
   * Replaces domain in deltas for Add/Remove participant operations.
   *
   * @param delta source delta
   * @param waveDomain target wave domain
   * @return delta for waveDomain if WaveDoamin is not null, otherwise source delta.
   * @throws InvalidParticipantAddress if there is a problem with deserialization of participant id.
   */
  public static ProtocolWaveletDelta convertDelta(ProtocolWaveletDelta delta,
      String waveDomain) throws InvalidParticipantAddress {
    if (waveDomain != null) {
      ProtocolWaveletDelta.Builder newDelta = ProtocolWaveletDelta.newBuilder(delta);
      ParticipantId author = convertParticipantId(delta.getAuthor(), waveDomain);
      newDelta.setAuthor(author.getAddress());
      for (int i = 0; i < delta.getOperationCount(); i++) {
        ProtocolWaveletOperation op = delta.getOperation(i);
        ProtocolWaveletOperation.Builder newOp = ProtocolWaveletOperation.newBuilder(op);
        if (op.hasAddParticipant()) {
          convertAddParticipantOperation(newOp, op, waveDomain);
        } else if (op.hasRemoveParticipant()) {
          convertRemoveParticipantOperation(newOp, op, waveDomain);
        }
        // TODO(user) release convert for other operations.
        newDelta.setOperation(i, newOp);
      }
      return newDelta.build();
    } else {
      return delta;
    }
  }

  /**
   * Converts all history deltas.
   *
   * @param deltas source deltas
   * @param waveDomain target wave domain
   * @return deltas for waveDomain if WaveDoamin is not null, otherwise source deltas.
   * @throws InvalidParticipantAddress if there is a problem with deserialization of participant id.
   */
  public static List<ProtocolWaveletDelta> convertDeltas(List<ProtocolWaveletDelta> deltas,
      String waveDomain) throws InvalidParticipantAddress {
    if (waveDomain != null) {
      List<ProtocolWaveletDelta> targetDeltas = new LinkedList<ProtocolWaveletDelta>();
      for (ProtocolWaveletDelta delta : deltas) {
        targetDeltas.add(DomainConverter.convertDelta(delta, waveDomain));
      }
      return targetDeltas;
    } else {
      return deltas;
    }
  }

  /**
   * Converts adding participant operation.
   */
  private static void convertAddParticipantOperation(ProtocolWaveletOperation.Builder newOperation,
      ProtocolWaveletOperation operation, String domain) throws InvalidParticipantAddress {
    ParticipantId participant = convertParticipantId(operation.getAddParticipant(), domain);
    newOperation.setAddParticipant(participant.getAddress());
  }

  /**
   * Converts removal participant operation.
   */
  private static void convertRemoveParticipantOperation(ProtocolWaveletOperation.Builder newOperation,
      ProtocolWaveletOperation operation, String domain) throws InvalidParticipantAddress {
    ParticipantId participant = convertParticipantId(operation.getRemoveParticipant(), domain);
    newOperation.setRemoveParticipant(participant.getAddress());
  }

  /**
   * Replaces participant domain.
   */
  private static ParticipantId convertParticipantId(String participant, String domain)
      throws InvalidParticipantAddress {
    return ParticipantId.of(ParticipantId.of(participant).getName(), domain);
  }
}
