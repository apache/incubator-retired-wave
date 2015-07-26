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

package org.waveprotocol.wave.concurrencycontrol.client;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.DocOp.IsDocOp;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple client mock that stores all the calls.
 *
 * @author zdwang@google.com (David Wang)
 */
public class ClientMock implements ConcurrencyControl.ConnectionListener {
  private int numOperationsReceived = 0;
  private final ArrayList<WaveletOperation> serverOperations = new ArrayList<WaveletOperation>();
  private final ArrayList<WaveletOperation> clientOperations = new ArrayList<WaveletOperation>();

  private final SuperSink doc;
  private final ConcurrencyControl cc;

  // Client ops are compared via context, so we need to create a participant to identify
  // client ops.
  private final ParticipantId participantId;

  private final ServerConnectionMock connection;

  /**
   * Construct a mock without any docs.
   */
  public ClientMock(ConcurrencyControl cc) {
    this(cc, null, null, null);
  }

  /**
   * Given the doc and applier, this mock will try to apply the client ops on
   * trigger and apply the server ops on receive.
   *
   * @param doc
   */
  public ClientMock(ConcurrencyControl cc, SuperSink doc, ParticipantId participantId,
      ServerConnectionMock connection) {
    this.cc = cc;
    this.doc = doc;
    this.participantId = participantId;
    this.connection = connection;
  }

  private void onServerOperation(WaveletOperation operation) {
    serverOperations.add(operation);
    try {
      if (doc != null) {
        applyOperation(operation);
      }
    } catch (OperationException e) {
      throw new RuntimeException("OperationException", e);
    }
  }

  /**
   * Gets the operations from concurrency control.
   */
  public void receiveServerOperations() {
    WaveletOperation op;
    while ((op = cc.receive()) != null) {
      onServerOperation(op);
    }
  }

  /**
   * Gets the reconnection versions from concurrency control.
   */
  public List<HashedVersion> getReconnectionVersions() {
    return cc.getReconnectionVersions();
  }

  /**
   * @return Operations we got from {@link #onServerOperation(WaveletOperation)}.
   */
  public ArrayList<WaveletOperation> getServerOperations() {
    return serverOperations;
  }

  /**
   * Clears stored ops we got from {@link #onServerOperation(WaveletOperation)}.
   */
  public void clearServerOperations() {
    serverOperations.clear();
  }

  /**
   * Applies an insertion to the document.
   */
  public void doInsert(int offset, String chars) throws OperationException {
    Nindo nindo = Nindo.insertCharacters(offset, chars);
    DocOp op = doc.consumeAndReturnInvertible(nindo);

    BlipContentOperation blipOp = new BlipContentOperation(
        new WaveletOperationContext(participantId, 0L, 1), op);
    WaveletBlipOperation wop = new WaveletBlipOperation("blip id", blipOp);
    clientOperations.add(wop);
  }

  /**
   * Pretend a client did the given op, so that when we call flush, the operation will
   * be ejected.
   */
  public void addClientOperation(WaveletOperation operation) throws OperationException {
    clientOperations.add(operation);
    if (doc != null) {
      applyOperation(operation);
    }
  }

  /**
   * Applies the given operation to the doc.
   *
   * @param operation
   * @throws OperationException
   */
  private void applyOperation(WaveletOperation operation) throws OperationException {
    if (operation instanceof WaveletBlipOperation) {
      WaveletBlipOperation waveOp = (WaveletBlipOperation) operation;
      if (waveOp.getBlipOp() instanceof BlipContentOperation) {
        BlipContentOperation blipOp = (BlipContentOperation) waveOp.getBlipOp();
        doc.consume(blipOp.getContentOp());
      }
    }
  }

  /**
   * Flush the client's operation to cc.
   */
  public void flush() throws TransformException {
    cc.onClientOperations(clientOperations.toArray(new WaveletOperation[] {}));

    clientOperations.clear();
  }

  /**
   * @return the doc
   */
  public IsDocOp getDoc() {
    return doc;
  }

  /**
   * @return participant id for this client.
   */
  public ParticipantId getParticipantId() {
    return participantId;
  }

  /**
   * Get connection to the server.
   */
  public ServerConnectionMock getConnection() {
    return connection;
  }

  @Override
  public void onOperationReceived() {
    numOperationsReceived++;
  }

  /**
   * @return the count of events from cc that we have received
   */
  public int getNumOpsReceived() {
    return numOperationsReceived;
  }

  /**
   * Resets the number of ops received and the recovery failure flag.
   */
  public void clearEvents() {
    numOperationsReceived = 0;
  }
}
