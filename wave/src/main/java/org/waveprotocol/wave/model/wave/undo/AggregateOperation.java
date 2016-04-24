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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An aggregate operation, built up from wavelet operations.
 *
 */
final class AggregateOperation {

  private static final class DocumentOperations {

    final String id;
    final DocOpList operations;

    DocumentOperations(String id, DocOpList operations) {
      this.id = id;
      this.operations = operations;
    }

  }

  private static final Comparator<ParticipantId> participantComparator =
      new Comparator<ParticipantId>() {

    @Override
    public int compare(ParticipantId o1, ParticipantId o2) {
      return o1.getAddress().compareTo(o2.getAddress());
    }

  };

  /**
   * Creates an aggregate operation from a <code>CoreWaveletOperation</code>.
   *
   * @param operation The wavelet operation whose behaviour the aggregate
   *        operation should have.
   * @return The aggregate operation.
   */
  static AggregateOperation createAggregate(CoreWaveletOperation operation) {
    if (operation instanceof CoreWaveletDocumentOperation) {
      return new AggregateOperation((CoreWaveletDocumentOperation) operation);
    } else if (operation instanceof CoreRemoveParticipant) {
      return new AggregateOperation((CoreRemoveParticipant) operation);
    } else if (operation instanceof CoreAddParticipant) {
      return new AggregateOperation((CoreAddParticipant) operation);
    }
    assert operation instanceof CoreNoOp;
    return new AggregateOperation();
  }

  /**
   * Creates an aggregate operation from a <code>WaveletOperation</code>.
   *
   * @param operation The wavelet operation whose behaviour the aggregate
   *        operation should have.
   * @return The aggregate operation.
   */
  static AggregateOperation createAggregate(WaveletOperation operation) {
    if (operation instanceof WaveletBlipOperation) {
      return new AggregateOperation((WaveletBlipOperation) operation);
    } else if (operation instanceof RemoveParticipant) {
      return new AggregateOperation((RemoveParticipant) operation);
    } else if (operation instanceof AddParticipant) {
      return new AggregateOperation((AddParticipant) operation);
    }
    assert operation instanceof NoOp : "Operation is an unhandled type: " + operation.getClass();
    return new AggregateOperation();
  }

  private static DocOpList invert(DocOpList docOpList) {
    return new DocOpList.Singleton(DocOpInverter.invert(docOpList.composeAll()));
  }

  /**
   * Composes the given aggregate operations.
   *
   * @param operations The aggregate operations to compose.
   * @return The composition of the given operations.
   */
  static AggregateOperation compose(Iterable<AggregateOperation> operations) {
    // NOTE(user): It's possible to replace the following two sets with a single map.
    Set<ParticipantId> toRemove = new TreeSet<ParticipantId>(participantComparator);
    Set<ParticipantId> toAdd = new TreeSet<ParticipantId>(participantComparator);
    Map<String, DocOpList> docOps = new TreeMap<String, DocOpList>();
    for (AggregateOperation operation : operations) {
      for (ParticipantId participant : operation.participantsToRemove) {
        if (toAdd.contains(participant)) {
          toAdd.remove(participant);
        } else {
          toRemove.add(participant);
        }
      }
      for (ParticipantId participant : operation.participantsToAdd) {
        if (toRemove.contains(participant)) {
          toRemove.remove(participant);
        } else {
          toAdd.add(participant);
        }
      }
      for (DocumentOperations documentOps : operation.docOps) {
        DocOpList docOpList = docOps.get(documentOps.id);
        if (docOpList != null) {
          docOps.put(documentOps.id, docOpList.concatenateWith(documentOps.operations));
        } else {
          docOps.put(documentOps.id, documentOps.operations);
        }
      }
    }
    return new AggregateOperation(
        new ArrayList<ParticipantId>(toRemove),
        new ArrayList<ParticipantId>(toAdd),
        mapToList(docOps));
  }

  /**
   * Transforms the given aggregate operations.
   *
   * @param clientOp The client operation to transform.
   * @param serverOp The server operation to transform.
   *
   * @return The transform of the two operations.
   * @throws TransformException
   */
  static OperationPair<AggregateOperation> transform(AggregateOperation clientOp,
      AggregateOperation serverOp) throws TransformException {
    List<ParticipantId> clientParticipantsToRemove = new ArrayList<ParticipantId>();
    List<ParticipantId> serverParticipantsToRemove = new ArrayList<ParticipantId>();
    List<ParticipantId> clientParticipantsToAdd = new ArrayList<ParticipantId>();
    List<ParticipantId> serverParticipantsToAdd = new ArrayList<ParticipantId>();
    List<DocumentOperations> clientDocOps = new ArrayList<DocumentOperations>();
    List<DocumentOperations> serverDocOps = new ArrayList<DocumentOperations>();
    removeCommonParticipants(clientOp.participantsToRemove, serverOp.participantsToRemove,
        clientParticipantsToRemove, serverParticipantsToRemove);
    removeCommonParticipants(clientOp.participantsToAdd, serverOp.participantsToAdd,
        clientParticipantsToAdd, serverParticipantsToAdd);
    transformDocumentOperations(clientOp.docOps, serverOp.docOps,
        clientDocOps, serverDocOps);
    AggregateOperation transformedClientOp = new AggregateOperation(
        clientParticipantsToRemove, clientParticipantsToAdd, clientDocOps);
    AggregateOperation transformedServerOp = new AggregateOperation(
        serverParticipantsToRemove, serverParticipantsToAdd, serverDocOps);
    return new OperationPair<AggregateOperation>(transformedClientOp, transformedServerOp);
  }

  private static List<DocumentOperations> mapToList(Map<String, DocOpList> map) {
    List<DocumentOperations> list = new ArrayList<DocumentOperations>();
    for (Map.Entry<String, DocOpList> entry : map.entrySet()) {
      list.add(new DocumentOperations(entry.getKey(), entry.getValue()));
    }
    return list;
  }

  static private void removeCommonParticipants(List<ParticipantId> ids1, List<ParticipantId> ids2,
      List<ParticipantId> outputIds1, List<ParticipantId> outputIds2) {
    int index = 0;
    outerLoop:
    for (ParticipantId id1 : ids1) {
      while (index < ids2.size()) {
        ParticipantId id2 = ids2.get(index);
        int comparison = participantComparator.compare(id1, id2);
        if (comparison < 0) {
          break;
        }
        ++index;
        if (comparison > 0) {
          outputIds2.add(id2);
        } else {
          continue outerLoop;
        }
      }
      outputIds1.add(id1);
    }
    for (; index < ids2.size(); ++index) {
      outputIds2.add(ids2.get(index));
    }
  }

  static private void transformDocumentOperations(
      List<DocumentOperations> clientOps,
      List<DocumentOperations> serverOps,
      List<DocumentOperations> transformedClientOps,
      List<DocumentOperations> transformedServerOps) throws TransformException {
    int index = 0;
    outerLoop:
    for (DocumentOperations fromClient : clientOps) {
      while (index < serverOps.size()) {
        DocumentOperations fromServer = serverOps.get(index);
        int comparison = fromClient.id.compareTo(fromServer.id);
        if (comparison < 0) {
          break;
        }
        ++index;
        if (comparison > 0) {
          transformedServerOps.add(fromServer);
        } else {
          DocOp clientOp = fromClient.operations.composeAll();
          DocOp serverOp = fromServer.operations.composeAll();
          OperationPair<DocOp> transformedOps = Transformer.transform(clientOp, serverOp);
          transformedClientOps.add(new DocumentOperations(fromClient.id,
              new DocOpList.Singleton(transformedOps.clientOp())));
          transformedServerOps.add(new DocumentOperations(fromClient.id,
              new DocOpList.Singleton(transformedOps.serverOp())));
          continue outerLoop;
        }
      }
      transformedClientOps.add(fromClient);
    }
    for (; index < serverOps.size(); ++index) {
      transformedServerOps.add(serverOps.get(index));
    }
  }

  private final List<ParticipantId> participantsToRemove;
  private final List<ParticipantId> participantsToAdd;
  private final List<DocumentOperations> docOps;

  private AggregateOperation(List<ParticipantId> toRemove, List<ParticipantId> toAdd,
      List<DocumentOperations> docOps) {
    participantsToRemove = toRemove;
    participantsToAdd = toAdd;
    this.docOps = docOps;
  }

  /**
   * Constructs an aggregate operation that does nothing.
   */
  AggregateOperation() {
    participantsToRemove = Collections.emptyList();
    participantsToAdd = Collections.emptyList();
    docOps = Collections.emptyList();
  }

  // The "Core" operations are simpler variants of the regular operations,
  // used in the open source org.waveprotocol federation implementation.

  /**
   * Constructs an aggregate operation that has the same behaviour as a
   * <code>CoreWaveletDocumentOperation</code>.
   *
   * @param waveletDocumentOperation The wavelet document operation.
   */
  AggregateOperation(CoreWaveletDocumentOperation waveletDocumentOperation) {
    participantsToRemove = Collections.emptyList();
    participantsToAdd = Collections.emptyList();
    docOps = Collections.singletonList(
        new DocumentOperations(
            waveletDocumentOperation.getDocumentId(),
            new DocOpList.Singleton(waveletDocumentOperation.getOperation())));
  }

  /**
   * Constructs an aggregate operation that has the same behaviour as a
   * <code>CoreRemoveParticipant</code>.
   *
   * @param removeParticipant
   */
  AggregateOperation(CoreRemoveParticipant removeParticipant) {
    participantsToRemove = Collections.singletonList(removeParticipant.getParticipantId());
    participantsToAdd = Collections.emptyList();
    docOps = Collections.emptyList();
  }

  /**
   * Constructs an aggregate operation that has the same behaviour as an
   * <code>CoreAddParticipant</code>.
   *
   * @param addParticipant
   */
  AggregateOperation(CoreAddParticipant addParticipant) {
    participantsToRemove = Collections.emptyList();
    participantsToAdd = Collections.singletonList(addParticipant.getParticipantId());
    docOps = Collections.emptyList();
  }

  /**
   * Constructs an aggregate operation that has the same behaviour as a
   * <code>WaveletBlipOperation</code>.
   *
   * @param op The wavelet blip operation.
   */
  AggregateOperation(WaveletBlipOperation op) {
    participantsToRemove = Collections.emptyList();
    participantsToAdd = Collections.emptyList();
    if (op.getBlipOp() instanceof BlipContentOperation) {
    docOps = Collections.singletonList(
        new DocumentOperations(
            op.getBlipId(),
            new DocOpList.Singleton(((BlipContentOperation) op.getBlipOp()).getContentOp())));
    } else {
      docOps = Collections.emptyList();
    }
  }

  /**
   * Constructs an aggregate operation that has the same behaviour as a
   * <code>RemoveParticipant</code>.
   *
   * @param removeParticipant
   */
  AggregateOperation(RemoveParticipant removeParticipant) {
    ParticipantId participant = new ParticipantId(
        removeParticipant.getParticipantId().getAddress());
    participantsToRemove = Collections.singletonList(participant);
    participantsToAdd = Collections.emptyList();
    docOps = Collections.emptyList();
  }

  /**
   * Constructs an aggregate operation that has the same behaviour as an
   * <code>AddParticipant</code>.
   *
   * @param addParticipant
   */
  AggregateOperation(AddParticipant addParticipant) {
    ParticipantId participant = new ParticipantId(addParticipant.getParticipantId().getAddress());
    participantsToRemove = Collections.emptyList();
    participantsToAdd = Collections.singletonList(participant);
    docOps = Collections.emptyList();
  }

  /**
   * Inverts this aggregate operation.
   *
   * @return this aggregate operation.
   */
  AggregateOperation invert() {
    List<DocumentOperations> invertedDocOps = new ArrayList<DocumentOperations>(docOps.size());
    for (DocumentOperations operations : docOps) {
      invertedDocOps.add(new DocumentOperations(operations.id, invert(operations.operations)));
    }
    return new AggregateOperation(participantsToAdd, participantsToRemove, invertedDocOps);
  }

  /**
   * Creates a list of wavelet operations representing the behaviour of this
   * aggregate operation.
   *
   * @return The list of wavelet operations representing the behaviour of this
   *         aggregate operation.
   */
  List<CoreWaveletOperation> toCoreWaveletOperations() {
    List<CoreWaveletOperation> operations = new ArrayList<CoreWaveletOperation>();
    for (ParticipantId participant : participantsToRemove) {
      operations.add(new CoreRemoveParticipant(participant));
    }
    for (DocumentOperations documentOps : docOps) {
      operations.add(new CoreWaveletDocumentOperation(documentOps.id,
          documentOps.operations.composeAll()));
    }
    for (ParticipantId participant : participantsToAdd) {
      operations.add(new CoreAddParticipant(participant));
    }
    return operations;
  }
}
