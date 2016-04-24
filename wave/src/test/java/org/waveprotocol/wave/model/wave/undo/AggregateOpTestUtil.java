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

package org.waveprotocol.wave.model.wave.undo;

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.List;

/**
 * Convenience methods for AggregateOps
 *
 */
public class AggregateOpTestUtil {
  static AggregateOperation compose(AggregateOperation ...ops) {
    return AggregateOperation.compose(Arrays.asList(ops));
  }

  static AggregateOperation removeParticipant(String participantId) {
    return new AggregateOperation(new CoreRemoveParticipant(new ParticipantId(participantId)));
  }

  static AggregateOperation addParticipant(String participantId) {
    return new AggregateOperation(new CoreAddParticipant(new ParticipantId(participantId)));
  }

  static AggregateOperation insert(String id, int location, int size) {
    return new AggregateOperation(new CoreWaveletDocumentOperation(id, new DocOpBuilder()
        .retain(location)
        .characters("a")
        .retain(size - location)
        .build()));
  }

  static AggregateOperation delete(String id, int location, int size) {
    return new AggregateOperation(new CoreWaveletDocumentOperation(id, new DocOpBuilder()
        .retain(location)
        .deleteCharacters("a")
        .retain(size - location)
        .build()));
  }

   static boolean areEqual(AggregateOperation aggregateOp1,
      AggregateOperation aggregateOp2) {
    List<CoreWaveletOperation> ops1 = aggregateOp1.toCoreWaveletOperations();
    List<CoreWaveletOperation> ops2 = aggregateOp2.toCoreWaveletOperations();
    if (ops1.size() != ops2.size()) {
      return false;
    }
    for (int i = 0; i < ops1.size(); ++i) {
      if (!ops1.get(i).equals(ops2.get(i))) {
        return false;
      }
    }
    return true;
  }
}
