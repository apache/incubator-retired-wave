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

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.DeltaPair;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ConcurrencyControl implements ServerConnectionListener {
  /**
   * The size and result version of an acknowledged delta.
   */
  private static final class AckInfo {
    /** Number of ops acknowledged. */
    final int numOps;
    /** Version of the wavelet after the acknowledged ops. */
    final HashedVersion ackedVersion;

    AckInfo(int numOps, HashedVersion ackedVersion) {
      this.numOps = numOps;
      this.ackedVersion = ackedVersion;
    }
  }

  /**
   * A client delta with the server acknowledgement.
   */
  private static final class AckedDelta {
    /** The delta sent to the server. */
    final WaveletDelta delta;
    /** The server's acknowledgement. */
    final AckInfo ack;

    AckedDelta(WaveletDelta delta, AckInfo ack) {
      this.delta = delta;
      this.ack = ack;
    }
  }

  /**
   * A listener for server operations.
   */
  public interface ConnectionListener {
    /** Called when a server operation is received. */
    void onOperationReceived();
  }

  private final UnsavedDataListener.UnsavedDataInfo unsavedDataInfo =
      new UnsavedDataListener.UnsavedDataInfo() {
        @Override
        public int inFlightSize() {
          return (unacknowledged != null) ? unacknowledged.size() : 0;
        }

        @Override
        public int estimateUnacknowledgedSize() {
          return clientOperationQueue.estimateSize() + inFlightSize();
        }

        @Override
        public int estimateUncommittedSize() {
          int ackedButUncommitted = 0;
          for (AckInfo ack : acks) {
            ackedButUncommitted += ack.numOps;
          }
          return ackedButUncommitted + estimateUnacknowledgedSize();
        }

        @Override
        public long laskAckVersion() {
          if (!acks.isEmpty()) {
            return acks.getLast().ackedVersion.getVersion();
          }
          return lastCommitVersion();
        }

        @Override
        public long lastCommitVersion() {
          return lastCommitVersion;
        }

        @Override
        public String getInfo() {
          return serverConnection.debugGetProfilingInfo() + "\n ====== CC Info ====== \n"
              + ConcurrencyControl.this;
        }
      };

  private final LoggerBundle logger;

  /**
   * The hash from the server before any the inferredServerPath.
   */
  private HashedVersion startSignature;

  /**
   * This marks the end of the intial sequence of deltas the server sends to
   * the client when we open a connection to the server.
   */
  private HashedVersion endOfStartingDelta;

  /**
   * This is the server's path inferred by the client. It contains deltas that
   * was sent by the client and acked by server. The list is cleared when any
   * server delta is received since we can't currently recover past a server
   * delta.
   */
  private final LinkedList<AckedDelta> inferredServerPath = CollectionUtils.newLinkedList();

  /**
   * Acknowledgments received for deltas not yet committed, in contiguous
   * version order. Unlike {@link #inferredServerPath} items are not cleared
   * when server deltas are received, but when commits are received.
   */
  private final LinkedList<AckInfo> acks = CollectionUtils.newLinkedList();

  /**
   * Latest committed version.
   */
  private long lastCommitVersion = 0;

  /**
   * This is the delta in the inferredServerPath that is not yet acked by the
   * server. Any new operations coming from the client will be queued in other
   * deltas after this delta in the inferredServerPath.
   *
   * If null, nothing is in flight to the server.
   */
  private WaveletDelta unacknowledged;

  /** Queue of operations from the client not yet sent to the server. */
  private final OperationQueue clientOperationQueue = new OperationQueue();

  /**
   * The operations that have been received from the server and have been transformed
   * against any relevant client operations but have not yet been received by the wave
   * client. This is needed to allow the wave client to grab operations from
   * CC when ever it's ready.
   */
  private List<WaveletOperation> serverOperations = CollectionUtils.newLinkedList();

  /**
   * A listener for this class to broadcast its transformed server ops.
   */
  private ConnectionListener clientListener = null;

  /**
   * The connection to the server where we can send client operations.
   */
  private ServerConnection serverConnection;

  /**
   * For pause sending to the server. This is needed when we tell the client to
   * flush its operations and we want to send to the server when all the
   * operations are flushed.
   */
  private boolean pauseSendForFlush = false;

  /**
   * Listener for unsaved data info.
   */
  private UnsavedDataListener unsavedDataListener;

  /**
   * Constructs a client side concurrency control module. The class must be
   * {@link #initialise(ServerConnection, ConnectionListener) initialised}
   * before use.
   * @param logger a Logger to use for trace output
   * @param startSignature the version/hash at which to begin connecting
   */
  public ConcurrencyControl(LoggerBundle logger, HashedVersion startSignature) {
    Preconditions.checkNotNull(startSignature, "startSignature cannot be null");
    this.logger = logger;
    this.startSignature = startSignature;
  }

  /**
   * Initialises the server connection and client listener for this connection. This
   * method must be invoked exactly once.
   *
   * @param serverConnection connection to which to send deltas
   * @param clientListener listener for inbound deltas
   */
  public void initialise(ServerConnection serverConnection, ConnectionListener clientListener) {
    Preconditions.checkNotNull(clientListener, "CC initialised with null connection listener");
    Preconditions.checkNotNull(serverConnection, "CC initialised with null server connection");
    this.clientListener = clientListener;
    this.serverConnection = serverConnection;
  }

  /**
   * Sets the listener for unsaved data info.
   */
  public void setUnsavedDataListener(UnsavedDataListener udl) {
    unsavedDataListener = udl;
  }

  /**
   * Closes this concurrency control.
   */
  public void close() {
    if (unsavedDataListener != null) {
      unsavedDataListener.onClose(everythingIsCommitted());
    }
    if (!clientOperationQueue.isEmpty()) {
      logger.error().log("Concurrency control closed with pending operations. Data has been lost");
    }
  }

  @Override
  public void onOpen(HashedVersion connectVersion, HashedVersion currentVersion)
      throws ChannelException {
    if ((connectVersion == null) || (currentVersion == null) || (connectVersion.getVersion() < 0)
        || (currentVersion.getVersion() < connectVersion.getVersion())) {
      throw new ChannelException("ConcurrencyControl onOpen received invalid versions, "
          + "connect version: " + connectVersion + ", current version: " + currentVersion,
          Recoverable.NOT_RECOVERABLE);
    }

    // Try to recover from where we were.
    // Find the point in the inferred server path to start resending pending
    // deltas to the server.
    int startResend = -1;
    if (startSignature.equals(connectVersion)) {
      startResend = 0;
    } else {
      int i = 0;
      Iterator<AckedDelta> iter = inferredServerPath.iterator();
      while (iter.hasNext()) {
        if (connectVersion.equals(iter.next().ack.ackedVersion)) {
          startResend = i + 1;
          break;
        }
        i++;
      }
    }

    if (startResend < 0) {
      // No matching signatures.
      throw new ChannelException(
          "Failed to recover from reconnection to server. No matching signatures. "
              + "[Received startSignature:" + connectVersion + " endSignature:" + currentVersion
              + "] " + this, Recoverable.NOT_RECOVERABLE);
    } else if (startResend < inferredServerPath.size()) {
      // Found a matching signature.
      mergeToClientQueue(startResend);
    } else if (startResend == inferredServerPath.size() && connectVersion.equals(currentVersion)) {
      // Matched all signatures and we are also the end of the server operations.
      // We are short circuiting, because we know that the server definitely have not got
      // our unacknowledged delta since we are at the end of its signatures.
      mergeToClientQueue(startResend);
    } else {
      // All the signatures matched, that means we need to compare server operations when
      // we get them.
      logger.trace().log("All signatures in CC queue matched on reconnection to server.");
    }
    forgetAcksAfter(startSignature.getVersion());
    this.endOfStartingDelta = currentVersion;
    sendDelta();
  }

  /**
   * Push all deltas at startResend and after back into clientOperationQueue,
   * discarding their ack info.
   *
   * @param startMerge The starting index to the delta in the
   *        {@link #inferredServerPath} to start to merge into clientOperationQueue.
   */
  private void mergeToClientQueue(int startMerge) {
    List<WaveletDelta> deltas = CollectionUtils.newArrayList();
    // Use the version at the resend
    if (startMerge < inferredServerPath.size()) {
      Iterator<AckedDelta> iter = inferredServerPath.listIterator(startMerge);
      while (iter.hasNext()) {
        deltas.add(iter.next().delta);
        iter.remove();
      }
    }

    if (unacknowledged != null) {
      deltas.add(unacknowledged);
      unacknowledged = null;
    }

    Collections.reverse(deltas);
    for (WaveletDelta delta : deltas) {
      clientOperationQueue.insertHead(delta);
    }
  }

  /**
   * Forgets about any acknowledgments after some version.
   */
  private void forgetAcksAfter(long version) {
    Iterator<AckInfo> ackItr = acks.iterator();
    while (ackItr.hasNext()) {
      if (ackItr.next().ackedVersion.getVersion() > version) {
        ackItr.remove();
      }
    }
  }

  /**
   * Packages up transformed client operations as a delta, and sends it to the
   * server. A send does not occur if there are unacknowledged deltas.
   */
  private void sendDelta() {
    if (!isReadyToSend()) {
      return;
    }

    if (unacknowledged != null) {
      logger.trace().log("Unacknowledged delta, expected to be applied at version ",
          unacknowledged.getTargetVersion().getVersion());
      return;
    }

    if (clientOperationQueue.isEmpty()) {
      logger.trace().log("Nothing to send");
      // Since the outgoing queue might have been transformed away, we need to reset
      // the estimated queue.
      triggerUnsavedDataListener();
      return;
    }

    // If we are sending something then we have inferred our location on the server path
    endOfStartingDelta = null;

    List<WaveletOperation> ops = clientOperationQueue.take();
    unacknowledged = new WaveletDelta(ops.get(0).getContext().getCreator(),
        getLastSignature(), ops);

    if (logger.isModuleEnabled() && logger.trace().shouldLog()) {
      logger.trace().log("Sending delta to server with last known server version " +
          unacknowledged.getTargetVersion(), unacknowledged);
    }

    serverConnection.send(unacknowledged);
    triggerUnsavedDataListener();
  }

  /**
   * Transform all the operation in the incoming server delta against all the
   * operation in {@link #unacknowledged} and {@link #clientOperationQueue}
   * before notifying the client.
   *
   * Also keep track of the transformed client operation so that we can infer
   * the server's OT path.
   *
   * Assumption:
   * <ul>
   * <li>serverDelta.getVersion() == unacknowledged.getVersion()</li>
   * <li>clientOps will never skip a version</li>
   * <li>serverDelta.getSignature() is never null</li>
   * </ul>
   *
   * @throws TransformException
   * @throws OperationException
   */
  private void transformOperationsAndNotify(TransformedWaveletDelta serverDelta) throws
      TransformException, OperationException {
    // Sanity check
    long latestVersion = inferredServerPath.size() > 0 ?
        inferredServerPath.getLast().ack.ackedVersion.getVersion() :
        startSignature.getVersion();
    if (serverDelta.getAppliedAtVersion() < latestVersion) {
      throw new OperationException("Server sent a delta containing a version that is older than " +
          "the version at end of inferred server path. [Received serverDelta" + serverDelta +
          "] " + this);
    }

    if (detectEchoBack(serverDelta)) {
      return;
    }

    // Clear inferred server path as we can not recover past a server Delta.
    inferredServerPath.clear();
    startSignature = serverDelta.getResultingVersion();

    // Transform against any unacknowledged ops.
    List<WaveletOperation> transformedServerDelta = serverDelta;

    if (unacknowledged != null) {
      if (serverDelta.getAppliedAtVersion() != unacknowledged.getTargetVersion().getVersion()) {
        throw new TransformException(
            "Cannot accept server version newer than unacknowledged. server version:"
                + serverDelta.getAppliedAtVersion() + " unacknowledged version:"
                + unacknowledged.getTargetVersion() + ". [Received serverDelta:" + serverDelta
                + "] " + this);
      }

      DeltaPair transformedPair = (new DeltaPair(unacknowledged, serverDelta)).transform();
      // The ops of the server delta are transformed, all metadata remains.
      transformedServerDelta = transformedPair.getServer();
      // The unacknowledged delta must have applied after the server delta.
      unacknowledged = new WaveletDelta(unacknowledged.getAuthor(),
          serverDelta.getResultingVersion(), transformedPair.getClient());
    }

    // Transform against any queued ops
    transformedServerDelta = clientOperationQueue.transform(transformedServerDelta);

    // Notify server-operation listeners.
    for (WaveletOperation serverOp : transformedServerDelta) {
      serverOperations.add(serverOp);
    }
  }

  /**
   * Detect if the whole delta is an echo back. If it is then take it as if it was an ack.
   * Echo back is a result of reconnection/recovery.
   * @return If the delta is a echo back.
   * @throws TransformException
   */
  private boolean detectEchoBack(TransformedWaveletDelta serverDelta) throws TransformException {
    // We have got all the initial list of operations. So do nothing.
    if (endOfStartingDelta == null
        || endOfStartingDelta.getVersion() <= serverDelta.getAppliedAtVersion()) {
      return false;
    }

    // Check to see if we are getting a delta that was sent by us, in case the
    // server echos back our own delta from a recovery scenario.
    if (unacknowledged != null && DeltaPair.areSame(serverDelta, unacknowledged)) {
      // If we completely match then take it as an ack.
      onSuccess(serverDelta.size(), serverDelta.getResultingVersion());
      return true;
    }

    // We've just got to the end of the initial list of operations
    // and there is no match. That means we need to merge the unacknowledged
    // ops and resend again.
    if (endOfStartingDelta.equals(serverDelta.getResultingVersion())) {
      mergeToClientQueue(inferredServerPath.size());
    }
    return false;
  }

  @Override
  public void onSuccess(int opsApplied, HashedVersion signature) throws TransformException {

    if (unacknowledged == null) {
      // Note: An ACK will only occur before echoBack delta.
      throw new TransformException("Got ACK from server, but we had not sent anything. " + this);
    }

    if (unacknowledged.getResultingVersion() != signature.getVersion()) {
      throw new TransformException("Got ACK from server, but we don't have the same version. " +
          "Client expects new version " + unacknowledged.getResultingVersion() +
          " and " + unacknowledged.size() + " acked ops, " +
          " Server acked " + opsApplied + ", new version " + signature.getVersion()  + ". " +
          "[Received signature:" + signature + "] [Received opsApplied:" + opsApplied + "] " +
          this);
    }

    if (opsApplied != unacknowledged.size()) {
      throw new TransformException("Unable to accept ACK of different number of operations than "
          + "client issued. Client sent = " + unacknowledged.size() + " Server acked = "
          + opsApplied + ". " + this);
    }

    if (!acks.isEmpty()) {
      Preconditions.checkState(
          signature.getVersion() > acks.getLast().ackedVersion.getVersion(),
          "Unexpected ack for version " + signature.getVersion() + " less than last acked version "
              + acks.getLast().ackedVersion);
    }

    // We know the server has done this now.
    if (unacknowledged.getTargetVersion().getVersion() < startSignature.getVersion()) {
      logger.error().log(
          "unexpected ack for version " + unacknowledged.getTargetVersion()
              + " before start version " + startSignature.getVersion() + ". [Received signature:"
              + signature + "] [Received opsApplied:" + opsApplied + "] " + this);
    }

    // Remember delta as received by the server (unless it transformed away).
    AckInfo ack = new AckInfo(opsApplied, signature);
    if (unacknowledged.size() > 0) {
      inferredServerPath.add(new AckedDelta(unacknowledged, ack));
    }
    acks.add(ack);

    // We now need to tell the client model about how the server applied the operation by
    // faking a server operation which contains the version number.
    makeFakeServerOpsFromAckAndNotify(signature);

    // Mark nothing in flight.
    unacknowledged = null;

    triggerUnsavedDataListener();

    // Send any pending client operations as a new delta.
    sendDelta();
  }

  /**
   * We have just been acked, let's make a fake noop operation to tell the client about the
   * server version and last modified time.
   *
   * Assumption:
   *   Server applied the ops starting at unacknowledged.getVersion()
   * @throws TransformException
   */
  private void makeFakeServerOpsFromAckAndNotify(HashedVersion ackedSignature)
      throws TransformException {
    List<WaveletOperation> versionOps = CollectionUtils.newArrayList();

    // All unacknowledged ops are assumed to be acked now. Ops in unacknowledged
    // are transformed against any server ops received prior to the ack. We now
    // create a version update op for each unacknowledged op. The last one also
    // includes the acked signature.
    Iterator<WaveletOperation> opItr = unacknowledged.iterator();
    while (opItr.hasNext()) {
      WaveletOperation op = opItr.next();
      HashedVersion signedVersion = opItr.hasNext() ? null : ackedSignature;
      versionOps.add(op.createVersionUpdateOp(1, signedVersion));
    }

    // Transform against any queued ops. Note server ops even when they are nullified will
    // leave behind a version updating op. i.e. the total number of server ops will never change.
    clientOperationQueue.transform(versionOps);

    // Notify server-operation listeners.
    for (WaveletOperation serverOp : versionOps) {
      serverOperations.add(serverOp);
      logger.trace().log("Fake version update op ", serverOp);
    }

    if (!serverOperations.isEmpty()) {
      onOperationReceived();
    }
  }

  // TODO(zdwang): Remove one of onServerDelta() to have a single interface.
  @Override
  public void onServerDelta(TransformedWaveletDelta delta) throws TransformException,
      OperationException {
    onServerDeltas(Collections.singletonList(delta));
  }

  @Override
  public void onServerDeltas(List<TransformedWaveletDelta> deltas) throws TransformException,
      OperationException {
    if (deltas.isEmpty()) {
      logger.error().log("Unexpected empty deltas.");
      return;
    }

    logger.trace().log("Server deltas received: ");
    logger.trace().log(deltas.toArray());

    for (TransformedWaveletDelta delta : deltas) {
      transformOperationsAndNotify(delta);
    }

    if (!serverOperations.isEmpty()) {
      onOperationReceived();
    }

    // Re-send any pending client operations.
    sendDelta();
  }

  /**
   * Returns a list of signature information providing versions on the inferred
   * server path, suitable for reconnection.
   *
   * The start signature is always a reconnection version, even if it's zero and
   * we never even sent an op to the server so that the server sends us ops
   * rather than a clobbering snapshot.
   */
  public List<HashedVersion> getReconnectionVersions() {
    ArrayList<HashedVersion> signatures = CollectionUtils.newArrayList();
    signatures.add(startSignature);
    for (AckedDelta d : inferredServerPath) {
      signatures.add(d.ack.ackedVersion);
    }
    return signatures;
  }

  /**
   * Tests whether or not a delta can be sent.
   *
   * @return {@code true} if and only if the buffered client operations can and
   *         should be sent to the server as a delta.
   */
  private boolean isReadyToSend() {
    boolean ready = !pauseSendForFlush && serverConnection.isOpen();
    if (!ready) {
      logger.trace().log("Not ready to send, pauseSendForFlush is ", pauseSendForFlush,
          " server connection ", serverConnection.isOpen() ? "IS" : "is NOT", " open");
    }
    return ready;
  }

  /**
   * Queues the client operations, and sends them to the server as a delta at
   * the first opportunity. Will call any registered UnsavedDataListeners before
   * returning.
   *
   * @param operations the operations to send, all of which must specify a creator
   */
  public void onClientOperations(WaveletOperation operations[]) throws TransformException {
    DeltaPair transformedPair =
        (new DeltaPair(Arrays.asList(operations), serverOperations)).transform();
    serverOperations = transformedPair.getServer();

    for (WaveletOperation o : transformedPair.getClient()) {
      clientOperationQueue.add(o);
    }
    triggerUnsavedDataListener();
    sendDelta();
  }

  @Override
  public void onCommit(long committedVersion) {
    // Remove old cache.
    while (!inferredServerPath.isEmpty()) {
      AckedDelta d = inferredServerPath.getFirst();
      if (d.delta.getResultingVersion() <= committedVersion) {
        startSignature = d.ack.ackedVersion;
        inferredServerPath.removeFirst();
      } else {
        break;
      }
    }

    // Remove from acked-but-uncommitted.
    while (!acks.isEmpty() && acks.getFirst().ackedVersion.getVersion() <= committedVersion) {
      acks.remove();
    }

    lastCommitVersion = committedVersion;
    logger.trace().log("onCommit: version =", committedVersion, " serverpathsize =",
        inferredServerPath.size(), " any unacknowledged ? ", (unacknowledged == null));
    triggerUnsavedDataListener();
  }

  private void onOperationReceived() {
    boolean oldPauseValue = pauseSendForFlush;
    // Pause sending first as we want to gather all the client ops in case
    // the client does multiple operations.
    pauseSendForFlush = true;
    if (clientListener != null) {
      clientListener.onOperationReceived();
    }
    pauseSendForFlush = oldPauseValue;
  }

  /**
   * Receive (transformed) server operation from the wave server, if any is available.
   *
   * @return the next server operation, if any is received from the server (and makes it
   *         through transformation), null otherwise
   */
  public WaveletOperation receive() {
    if (serverOperations.isEmpty()) {
      return null;
    } else {
      WaveletOperation op = serverOperations.remove(0);
      logger.trace().log("Processing server op ", op);
      return op;
    }
  }

  /**
   * Peek at the next (transformed) server operation from the wave server, if any is available.
   *
   * @return the next server operation, if any is received from the server (and makes it
   *         through transformation), null otherwise
   */
  public WaveletOperation peek() {
    return serverOperations.isEmpty() ? null : serverOperations.get(0);
  }

  /**
   * @return last received signature.
   */
  private HashedVersion getLastSignature() {
    return inferredServerPath.size() == 0 ? startSignature
        : inferredServerPath.getLast().ack.ackedVersion;
  }

  /** True if nothing is queued or in flight or uncommitted. */
  private boolean everythingIsCommitted() {
    return acks.isEmpty() && (unacknowledged == null);
  }

  private void triggerUnsavedDataListener() {
    if (unsavedDataListener != null) {
      unsavedDataListener.onUpdate(unsavedDataInfo);
    }
  }

  @Override
  public String toString() {
    // Space before \n in case some logger swallows the newline.
    return "Client CC State = " +
        "[startSignature:" + startSignature + "] \n" +
        "[endOfStartingDelta:" + endOfStartingDelta + "] \n" +
        "[lastCommittedVersion: " + lastCommitVersion + "] \n" +
        "[inferredServerPath:" + inferredServerPath + "] \n" +
        "[unacknowledged:" + unacknowledged + "] \n" +
        "[clientOperationQueue:" + clientOperationQueue + "] \n" +
        "[serverOperations:" + serverOperations + "] \n";
  }
}
