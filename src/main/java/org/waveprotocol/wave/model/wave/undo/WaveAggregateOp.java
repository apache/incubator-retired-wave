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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An aggregate operation similar to @see AggregateOperation, but with an
 * additional field to specify the creator of its component ops.
 *
 */
class WaveAggregateOp {
  /**  List of op, creator pairs */
  private final List<OpCreatorPair> opPairs;

  private static class OpCreatorPair {
    final ParticipantId creator;
    final AggregateOperation op;

    OpCreatorPair(AggregateOperation op, ParticipantId creator) {
      Preconditions.checkNotNull(op, "op must be non-null");
      Preconditions.checkNotNull(creator, "creator must be non-null");
      this.op = op;
      this.creator = creator;
    }
  }

  /**
   * Constructs a WaveAggregateOp from a Wavelet op.
   * @param op
   */
  static WaveAggregateOp createAggregate(WaveletOperation op) {
    Preconditions.checkNotNull(op, "op must be non-null");
    Preconditions.checkNotNull(op.getContext(), "context must be non-null");

    ParticipantId creator = op.getContext().getCreator();
    AggregateOperation aggOp = AggregateOperation.createAggregate(op);

    return new WaveAggregateOp(aggOp, creator);
  }

  /**
   * Compose a list of WaveAggregateOps.
   *
   * NOTE(user): Consider adding some checks for operations that span different
   * creators, i.e. a compose of addParticipant(personA) by creator1 and
   * creator2 should be invalid.
   *
   * @param operations
   */
  static WaveAggregateOp compose(List<WaveAggregateOp> operations) {
    return new WaveAggregateOp(composeDocumentOps(flatten(operations)));
  }

  /**
   * Transform the given operations.
   *
   * @param clientOp
   * @param serverOp
   *
   * @throws TransformException
   */
  static OperationPair<WaveAggregateOp> transform(WaveAggregateOp clientOp,
      WaveAggregateOp serverOp) throws TransformException {
    // This gets filled with transformed server ops.
    List<OpCreatorPair> transformedServerOps = new ArrayList<OpCreatorPair>();
    // This starts with the original client ops, and gets transformed with each server op.
    List<OpCreatorPair> transformedClientOps = new ArrayList<OpCreatorPair>(clientOp.opPairs);

    for (OpCreatorPair sPair : serverOp.opPairs) {
      transformedServerOps.add(transformAndUpdate(transformedClientOps, sPair));
    }

    return new OperationPair<WaveAggregateOp>(new WaveAggregateOp(transformedClientOps),
        new WaveAggregateOp(transformedServerOps));
  }

  private static void maybeCollectOps(List<AggregateOperation> ops, ParticipantId creator,
      List<OpCreatorPair> dest) {
    if (ops != null && ops.size() > 0) {
      assert creator != null;
      dest.add(new OpCreatorPair(AggregateOperation.compose(ops), creator));
    }
  }

  private static List<OpCreatorPair> composeDocumentOps(List<OpCreatorPair> ops) {
    List<OpCreatorPair> ret = new ArrayList<OpCreatorPair>();
    ParticipantId currentCreator = null;
    List<AggregateOperation> currentOps = null;

    // Group sequences of ops under same creator.
    for (OpCreatorPair op : ops) {
      if (!op.creator.equals(currentCreator)) {
        // If the creator is different, compose and finish with the current
        // group, and start the next group.
        maybeCollectOps(currentOps, currentCreator, ret);
        currentOps = null;
        currentCreator = op.creator;
      }

      if (currentOps == null) {
        currentOps =  new ArrayList<AggregateOperation>();
      }
      currentOps.add(op.op);
    }
    // Collect the last batch of ops.
    maybeCollectOps(currentOps, currentCreator, ret);

    return ret;
  }

  /**
   * Flatten a sequence of WaveAggregate operations into a list of
   * OpCreatorPairs.
   *
   * @param operations
   */
  private static List<OpCreatorPair> flatten(List<WaveAggregateOp> operations) {
    List<OpCreatorPair> ret = new ArrayList<OpCreatorPair>();
    for (WaveAggregateOp aggOp : operations) {
      ret.addAll(aggOp.opPairs);
    }
    return ret;
  }

  /**
   * Transform stream S against streamC, updating streamC and returning the
   * transform of s.
   *
   * @param streamC
   * @param s
   * @throws TransformException
   */
  private static OpCreatorPair transformAndUpdate(List<OpCreatorPair> streamC, OpCreatorPair s)
      throws TransformException {
    // Makes a copy of streamC and clear the original, so that it can be filled with the
    // transformed version.
    List<OpCreatorPair> streamCCopy = new ArrayList<OpCreatorPair>(streamC);
    streamC.clear();

    for (OpCreatorPair c : streamCCopy) {
      OperationPair<OpCreatorPair> transformed = transform(c, s);
      streamC.add(transformed.clientOp());
      s = transformed.serverOp();
    }

    return s;
  }

  private static OperationPair<OpCreatorPair> transform(OpCreatorPair c, OpCreatorPair s)
      throws TransformException {
    OperationPair<AggregateOperation> transformed = AggregateOperation.transform(c.op, s.op);
    return new OperationPair<OpCreatorPair>(new OpCreatorPair(transformed.clientOp(), c.creator),
        new OpCreatorPair(transformed.serverOp(), s.creator));
  }

  @VisibleForTesting
  WaveAggregateOp(AggregateOperation op, ParticipantId creator) {
    opPairs = Collections.singletonList(new OpCreatorPair(op, creator));
  }

  private WaveAggregateOp(List<OpCreatorPair> pairs) {
    Preconditions.checkNotNull(pairs, "pairs must be non-null");
    this.opPairs = pairs;
  }

  /**
   * @return wavelet operations corresponding to this WaveAggregateOp.
   */
  public List<WaveletOperation> toWaveletOperations() {
    return toWaveletOperationsWithVersions(0, null);
  }

  /**
   * Special case where we populate the last op in the list with the given versions.
   * This is necessary to preserve the WaveletOperationContext from the server.
   *
   * @param versionIncrement
   * @param hashedVersion
   */
  public List<WaveletOperation> toWaveletOperationsWithVersions(long versionIncrement,
      HashedVersion hashedVersion) {
    List<WaveletOperation> ret = new ArrayList<WaveletOperation>();
    for (int i = 0; i < opPairs.size(); ++i) {
      OpCreatorPair pair = opPairs.get(i);
      boolean isLastOfOuter = (i == opPairs.size() - 1);

      List<CoreWaveletOperation> coreWaveletOperations = pair.op.toCoreWaveletOperations();

      for (int j = 0; j < coreWaveletOperations.size(); ++j) {
        boolean isLast = isLastOfOuter && (j == coreWaveletOperations.size() - 1);
        WaveletOperationContext opContext =
            contextForCreator(pair.creator, versionIncrement, hashedVersion, isLast);
        WaveletOperation waveletOps =
            coreWaveletOpsToWaveletOps(coreWaveletOperations.get(j), opContext);
        ret.add(waveletOps);
      }
    }
    return ret;
  }

  WaveAggregateOp invert() {
    List<OpCreatorPair> invertedPairs = new ArrayList<OpCreatorPair>();
    for (OpCreatorPair pair : opPairs) {
      invertedPairs.add(new OpCreatorPair(pair.op.invert(), pair.creator));
    }
    Collections.reverse(invertedPairs);
    return new WaveAggregateOp(invertedPairs);
  }

  private WaveletOperation coreWaveletOpsToWaveletOps(CoreWaveletOperation op,
      WaveletOperationContext context) {
    if (op instanceof CoreRemoveParticipant) {
      ParticipantId participantId = ((CoreRemoveParticipant) op).getParticipantId();
      return new RemoveParticipant(context, participantId);
    } else if (op instanceof CoreAddParticipant) {
      ParticipantId participantId = ((CoreAddParticipant) op).getParticipantId();
      return new AddParticipant(context, participantId);
    } else if (op instanceof CoreWaveletDocumentOperation) {
      CoreWaveletDocumentOperation waveletDocOp = (CoreWaveletDocumentOperation) op;
      String documentId = waveletDocOp.getDocumentId();
      DocOp operation = waveletDocOp.getOperation();
      return new WaveletBlipOperation(documentId, new BlipContentOperation(context, operation));
    }

    throw new RuntimeException("unhandled operation type");
  }

  /**
   * @param creator
   * @param isLastOfSeq
   * @param hashedVersion
   * @param versionIncrement
   * @return a WaveletOperationContext with the specified participant as the
   *         creator.
   */
  private WaveletOperationContext contextForCreator(ParticipantId creator, long versionIncrement,
      HashedVersion hashedVersion, boolean isLastOfSeq) {
    if (isLastOfSeq) {
      return new WaveletOperationContext(creator, System.currentTimeMillis(), versionIncrement,
          hashedVersion);
    } else {
      // NOTE(user): The timestamp and version field are not relevant in the
      // client for outgoing ops, but may need to be filled out properly on the
      // server.
      return new WaveletOperationContext(creator, System.currentTimeMillis(), 0L);
    }
  }
}
