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

package org.waveprotocol.box.server.persistence.protos;

import com.google.common.collect.ImmutableList;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * This class is used to serialize and deserialize {@link TransformedWavelwetDelta}
 * and {@link ProtoTransformedWavelwetDelta}
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class ProtoDeltaStoreDataSerializer {

  /**
   * Serialize a {@link TransformedWaveletDelta} into a {@link ProtoTransformedWaveletDelta}
   */
  public static ProtoTransformedWaveletDelta serialize(TransformedWaveletDelta delta) {
    ProtoTransformedWaveletDelta.Builder builder = ProtoTransformedWaveletDelta.newBuilder();
    builder.setAuthor(delta.getAuthor().getAddress());
    builder.setResultingVersion(
        CoreWaveletOperationSerializer.serialize(delta.getResultingVersion()));
    builder.setApplicationTimestamp(delta.getApplicationTimestamp());
    for (WaveletOperation op : delta) {
      builder.addOperation(CoreWaveletOperationSerializer.serialize(op));
    }
    return builder.build();
  }

  /**
   * Deserialize a {@link ProtoTransformedWaveletDelta} into a {@link TransformedWaveletDelta}
   */
  public static TransformedWaveletDelta deserialize(ProtoTransformedWaveletDelta delta) {
    long applicationTimestamp = delta.getApplicationTimestamp();
    HashedVersion resultingVersion =
        CoreWaveletOperationSerializer.deserialize(delta.getResultingVersion());
    ParticipantId author = ParticipantId.ofUnsafe(delta.getAuthor());
    ImmutableList.Builder<WaveletOperation> operations = ImmutableList.builder();
    int numOperations = delta.getOperationCount();
    for (int i = 0; i < numOperations; i++) {
      WaveletOperationContext context;
      if (i == numOperations - 1) {
        context = new WaveletOperationContext(author, applicationTimestamp, 1, resultingVersion);
      } else {
        context = new WaveletOperationContext(author, applicationTimestamp, 1);
      }
      operations.add(CoreWaveletOperationSerializer.deserialize(delta.getOperation(i), context));
    }
    return new TransformedWaveletDelta(author, resultingVersion, applicationTimestamp, operations.build());
  }
}
