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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.RemovedAuthorException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * The class for transforming operations as in the Jupiter system.
 *
 * The Jupiter algorithm takes 2 operations S and C and produces S' and C'.
 * Where the operations, S + C' = C + S'
 */
public class CoreTransform {

  /**
   * Transforms a pair of operations.
   *
   * @param clientOp The client's operation.
   * @param clientOpAuthor The author of the client's operation.
   * @param serverOp The server's operation.
   * @param serverOpAuthor The author of the server's operation.
   * @return The resulting transformed client and server operations.
   * @throws TransformException if a problem was encountered during the
   *         transformation.
   */
  public static OperationPair<CoreWaveletOperation> transform(CoreWaveletOperation clientOp,
      ParticipantId clientOpAuthor, CoreWaveletOperation serverOp, ParticipantId serverOpAuthor)
      throws TransformException {
    if (clientOp instanceof CoreWaveletDocumentOperation && serverOp instanceof
        CoreWaveletDocumentOperation) {
      CoreWaveletDocumentOperation clientWaveDocOp = (CoreWaveletDocumentOperation) clientOp;
      CoreWaveletDocumentOperation serverWaveDocOp = (CoreWaveletDocumentOperation) serverOp;
      if (clientWaveDocOp.getDocumentId().equals(serverWaveDocOp.getDocumentId())) {
        // Transform document operations
        DocOp clientMutation = clientWaveDocOp.getOperation();
        DocOp serverMutation = serverWaveDocOp.getOperation();
        OperationPair<DocOp> transformedDocOps =
          Transformer.transform(clientMutation, serverMutation);
        clientOp = new CoreWaveletDocumentOperation(clientWaveDocOp.getDocumentId(),
            transformedDocOps.clientOp());
        serverOp = new CoreWaveletDocumentOperation(serverWaveDocOp.getDocumentId(),
            transformedDocOps.serverOp());
      } else {
        // Different documents don't conflict; use identity transform below
      }
    } else {

      if (serverOp instanceof CoreRemoveParticipant) {
        CoreRemoveParticipant serverRemoveOp = (CoreRemoveParticipant) serverOp;
        if (serverRemoveOp.getParticipantId().equals(clientOpAuthor)) {
          // clientOpAuthor has issued a client operation that is concurrent with a server
          // operation to remove clientOpAuthor, hence the client operation is doomed
          throw new RemovedAuthorException(clientOpAuthor.getAddress());
        }
        if (clientOp instanceof CoreRemoveParticipant) {
          CoreRemoveParticipant clientRemoveOp = (CoreRemoveParticipant) clientOp;
          if (clientRemoveOp.getParticipantId().equals(serverRemoveOp.getParticipantId())) {
            clientOp = CoreNoOp.INSTANCE;
            serverOp = CoreNoOp.INSTANCE;
          }
        } else if (clientOp instanceof CoreAddParticipant) {
          checkParticipantRemovalAndAddition(serverRemoveOp, (CoreAddParticipant) clientOp);
        }
      } else if (serverOp instanceof CoreAddParticipant) {
        CoreAddParticipant serverAddOp = (CoreAddParticipant) serverOp;
        if (clientOp instanceof CoreAddParticipant) {
          CoreAddParticipant clientAddOp = (CoreAddParticipant) clientOp;
          if (clientAddOp.getParticipantId().equals(serverAddOp.getParticipantId())) {
            clientOp = CoreNoOp.INSTANCE;
            serverOp = CoreNoOp.INSTANCE;
          }
        } else if (clientOp instanceof CoreRemoveParticipant) {
          checkParticipantRemovalAndAddition((CoreRemoveParticipant) clientOp, serverAddOp);
        }
      }
    }
    // Apply identity transform by default
    return new OperationPair<CoreWaveletOperation>(clientOp, serverOp);
  }

  /**
   * Checks to see if a participant is being removed by one operation and added
   * by another concurrent operation. In such a situation, at least one of the
   * operations is invalid.
   *
   * @param removeParticipant The operation to remove a participant.
   * @param addParticipant The operation to add a participant.
   * @throws TransformException if the same participant is being concurrently
   *         added and removed.
   */
  private static void checkParticipantRemovalAndAddition(CoreRemoveParticipant removeParticipant,
      CoreAddParticipant addParticipant) throws TransformException {
    ParticipantId participantId = removeParticipant.getParticipantId();
    if (participantId.equals(addParticipant.getParticipantId())) {
      throw new TransformException("Transform error involving participant: " +
          participantId.getAddress());
    }
  }

}
