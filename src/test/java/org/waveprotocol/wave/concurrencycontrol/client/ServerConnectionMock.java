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

import junit.framework.Assert;

import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * This mocks a server connection which we can pretend things were received on the wire.
 *
 * @author zdwang@google.com (David Wang)
 */
public class ServerConnectionMock implements ServerConnection {
  private ServerConnectionListener listener;
  private ServerMock serverMock;

  private final List<WaveletDelta> ghostDeltas = CollectionUtils.newArrayList();
  private final List<WaveletDelta> sentDeltas = CollectionUtils.newArrayList();
  private final List<TransformedWaveletDelta> receivedDeltas = CollectionUtils.newArrayList();
  private final List<HashedVersion> reconnectSignatures = CollectionUtils.newArrayList();

  private boolean isOpen = true;

  /** If all the deltas to be sent should be turned into ghost deltas */
  private boolean ghostSend = false;

  /**
   * Send data to server mock if we have one.
   */
  public void send(WaveletDelta delta) {
    if (ghostSend) {
      ghostDeltas.add(delta);
    } else {
      sentDeltas.add(delta);
      if (serverMock != null) {
        serverMock.receive(this, delta);
      }
    }
  }

  /**
   * @param ghostSend if true, all deltas in send() are turned into ghosts. i.e. not sent to server
   */
  public void setGhostSend(boolean ghostSend) {
    this.ghostSend = ghostSend;
  }

  /**
   * Send all the ghost deltas to the server
   */
  public void sendGhosts() {
    for (WaveletDelta delta : ghostDeltas) {
      if (serverMock != null) {
        // No call back for ack.
        serverMock.receive(null, delta);
      }
    }
    ghostDeltas.clear();
  }

  /**
   * Pretend we got the delta on the wire.
   */
  public void triggerServerDelta(TransformedWaveletDelta delta) throws TransformException,
      OperationException {
    receivedDeltas.add(delta);
    if (listener != null) {
      listener.onServerDelta(delta);
    }
  }

  /**
   * Pretend we got the deltas on the wire.
   */
  public void triggerServerDeltas(List<TransformedWaveletDelta> deltas) throws TransformException,
      OperationException {
    receivedDeltas.addAll(deltas);
    if (listener != null) {
      listener.onServerDeltas(deltas);
    }
  }

  /**
   * Pretend we got the deltas on the wire.
   */
  public void triggerServerDeltas(TransformedWaveletDelta[] deltas) throws TransformException,
      OperationException {
    List<TransformedWaveletDelta> received = CollectionUtils.newArrayList();
    for (TransformedWaveletDelta delta : deltas) {
      receivedDeltas.add(delta);
      received.add(delta);
    }
    if (listener != null) {
      listener.onServerDeltas(received);
    }
  }

  /**
   * Triggers an ack.
   * @param numDeltasApplied
   * @param signature
   * @throws TransformException
   * @throws OperationException
   */
  public void triggerServerSuccess(int numDeltasApplied, HashedVersion signature)
      throws TransformException, OperationException {
    if (listener != null) {
      listener.onSuccess(numDeltasApplied, signature);
    }
  }

  /**
   * Triggers a commit.
   * @param version
   * @throws TransformException
   * @throws OperationException
   */
  public void triggerServerCommit(long version)
      throws TransformException, OperationException {
    if (listener != null) {
      listener.onCommit(version);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isOpen() {
    return isOpen;
  }

  /**
   * Set what {@link #isOpen()} should return.
   */
  public void setOpen(boolean open) {
    isOpen = open;
  }

  public void reconnect(List<HashedVersion> signatures) throws ChannelException {
    reconnectSignatures.addAll(signatures);
    if (serverMock != null) {
      try {
        serverMock.reOpen(this, signatures);
      } catch (TransformException e) {
        e.printStackTrace();
        Assert.fail(e.getMessage());
      } catch (OperationException e) {
        e.printStackTrace();
        Assert.fail(e.getMessage());
      }
    }
  }


  // //////////////////////
  // Below are auto generated methods
  // //////////////////////

  /**
   * @return the listener
   */
  public ServerConnectionListener getListener() {
    return listener;
  }

  /**
   * @param listener the listener to set
   */
  public void setListener(ServerConnectionListener listener) {
    this.listener = listener;
  }

  /**
   * @return the serverMock
   */
  public ServerMock getServerMock() {
    return serverMock;
  }

  /**
   * @param serverMock the serverMock to set
   */
  public void setServerMock(ServerMock serverMock) {
    this.serverMock = serverMock;
  }

  /**
   * @return the sentDeltas
   */
  public List<WaveletDelta> getSentDeltas() {
    return sentDeltas;
  }

  /**
   * @return the receivedDeltas
   */
  public List<TransformedWaveletDelta> getReceivedDeltas() {
    return receivedDeltas;
  }

  /**
   * @return the reconnectSignatures
   */
  public List<HashedVersion> getReconnectSignatures() {
    return reconnectSignatures;
  }

  /**
   * Pretend server opened the connection at the given startSignature and tells of the last
   * signature.
   *
   * @param startSignature
   * @param endSignature
   */
  public void triggerOnOpen(HashedVersion startSignature, HashedVersion endSignature)
      throws ChannelException {
    listener.onOpen(startSignature, endSignature);
  }

  @Override
  public String debugGetProfilingInfo() {
    return null;
  }
}
