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
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.RemovedAuthorException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * The class for transforming operations as in the Jupiter system.
 *
 * The Jupiter algorithm takes 2 operations S and C and produces S' and C'.
 * Where the operations, S + C' = C + S'
 *
 * @author zdwang@google.com (David Wang)
 */
public class Transform {

  //
  // NOTE(user): There are many instanceof and short-lived objects in this implementation.
  //   If this becomes a problem in the client, then this can be rewritten as a static visitor
  //   tree with no instanceof or new object creation.
  //

  /**
   * Transforms a pair of operations.
   *
   * @param clientOp The client's operation.
   * @param serverOp The server's operation.
   * @return The resulting transformed client and server operations.
   * @throws TransformException if a problem was encountered during the
   *         transformation.
   */
  public static OperationPair<WaveletOperation> transform(WaveletOperation clientOp,
      WaveletOperation serverOp) throws TransformException {
    // TODO(user): This is a provisional implementation. This should be
    // rewritten using visitors.
    Preconditions.checkNotNull(clientOp, "Null client operation");
    Preconditions.checkNotNull(serverOp, "Null server operation");
    if (clientOp instanceof WaveletBlipOperation && serverOp instanceof WaveletBlipOperation) {
      WaveletBlipOperation clientWaveBlipOp = (WaveletBlipOperation) clientOp;
      WaveletBlipOperation serverWaveBlipOp = (WaveletBlipOperation) serverOp;
      if (clientWaveBlipOp.getBlipId().equals(serverWaveBlipOp.getBlipId())) {
        // Transform blip operations
        BlipOperation clientBlipOp = clientWaveBlipOp.getBlipOp();
        BlipOperation serverBlipOp = serverWaveBlipOp.getBlipOp();
        OperationPair<BlipOperation> transformedBlipOps = transform(clientBlipOp, serverBlipOp);
        clientOp = new WaveletBlipOperation(clientWaveBlipOp.getBlipId(),
            transformedBlipOps.clientOp());
        serverOp = new WaveletBlipOperation(serverWaveBlipOp.getBlipId(),
            transformedBlipOps.serverOp());
      } else {
        // Different blips don't conflict; use identity transform below
      }
    } else {
      if (serverOp instanceof RemoveParticipant) {
        RemoveParticipant serverRemoveOp = (RemoveParticipant) serverOp;
        checkParticipantRemoval(serverRemoveOp, clientOp);
        if (clientOp instanceof RemoveParticipant) {
          RemoveParticipant clientRemoveOp = (RemoveParticipant) clientOp;
          if (clientRemoveOp.getParticipantId().equals(serverRemoveOp.getParticipantId())) {
            clientOp = new NoOp(clientRemoveOp.getContext());
            serverOp = new NoOp(serverRemoveOp.getContext());
          }
        } else if (clientOp instanceof AddParticipant) {
          checkParticipantRemovalAndAddition(serverRemoveOp, (AddParticipant) clientOp);
        }
      } else if (serverOp instanceof AddParticipant) {
        AddParticipant serverAddOp = (AddParticipant) serverOp;
        if (clientOp instanceof AddParticipant) {
          AddParticipant clientAddOp = (AddParticipant) clientOp;
          if (clientAddOp.getParticipantId().equals(serverAddOp.getParticipantId())) {
            clientOp = new NoOp(clientAddOp.getContext());
            serverOp = new NoOp(serverAddOp.getContext());
          }
        } else if (clientOp instanceof RemoveParticipant) {
          checkParticipantRemovalAndAddition((RemoveParticipant) clientOp, serverAddOp);
        }
      }
    }
    // Apply identity transform by default
    return new OperationPair<WaveletOperation>(clientOp, serverOp);
  }

  /**
   * Transforms a pair of blip operations.
   *
   * @param clientOp
   * @param serverOp
   * @return the transformed pair.
   * @throws TransformException
   */
  public static OperationPair<BlipOperation> transform(BlipOperation clientOp,
      BlipOperation serverOp) throws TransformException {
    if (clientOp instanceof BlipContentOperation && serverOp instanceof BlipContentOperation) {
      BlipContentOperation clientBlipContentOp = (BlipContentOperation) clientOp;
      BlipContentOperation serverBlipContentOp = (BlipContentOperation) serverOp;
      DocOp clientContentOp = clientBlipContentOp.getContentOp();
      DocOp serverContentOp = serverBlipContentOp.getContentOp();
      OperationPair<? extends DocOp> transformedDocOps =
          Transformer.transform(clientContentOp, serverContentOp);
      clientOp = new BlipContentOperation(clientBlipContentOp.getContext(),
          transformedDocOps.clientOp());
      serverOp = new BlipContentOperation(serverBlipContentOp.getContext(),
          transformedDocOps.serverOp());
    } else {
      // All other blip-op pairs have identity transforms for now.
    }

    // Apply identity transform by default
    return new OperationPair<BlipOperation>(clientOp, serverOp);
  }

  /**
   * Checks to see if a participant has issued an operation that is concurrent
   * with an operation to remove that participant, and throws an exception if
   * such is the case.
   *
   * TODO(user): revisit this, see bug 2594800
   *
   * @param removeParticipant The operation to remove a participant.
   * @param operation The operation to check.
   * @throws RemovedAuthorException if the wavelet operation was issued by the
   *         participant being removed.
   */
  private static void checkParticipantRemoval(RemoveParticipant removeParticipant,
      WaveletOperation operation) throws RemovedAuthorException {
    ParticipantId participantId = removeParticipant.getParticipantId();
    if (participantId.equals(operation.getContext().getCreator())) {
      throw new RemovedAuthorException(participantId.getAddress());
    }
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
  private static void checkParticipantRemovalAndAddition(RemoveParticipant removeParticipant,
      AddParticipant addParticipant) throws TransformException {
    ParticipantId participantId = removeParticipant.getParticipantId();
    if (participantId.equals(addParticipant.getParticipantId())) {
      throw new TransformException("Transform error involving participant: " +
          participantId.getAddress());
    }
  }

}
