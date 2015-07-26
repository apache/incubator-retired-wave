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

import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * This allows a class to listen for operations from the server arriving from
 * the ServerConnection. A class that implements this interface would register
 * with a ServerConnection to receive the operations.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface ServerConnectionListener {

  /**
   * Called when the wave is (re-)opened and before receiving deltas from the
   * server.
   *
   * @param connectVersion The signature of the wave at the version at which the
   *        wave is opened (non-null).
   * @param currentVersion The signature of the wave at the server's current
   *        version (non-null).
   */
  public void onOpen(HashedVersion connectVersion, HashedVersion currentVersion)
      throws ChannelException;

  /**
   * Called when a delta is received from the server.
   *
   * @param delta
   * @throws TransformException There is an operation in the process of
   *         transforming the received operation.
   * @throws OperationException Failed to apply the server operation to the
   *         client's editor.
   */
  public void onServerDelta(TransformedWaveletDelta delta) throws TransformException,
      OperationException;

  /**
   * Called when deltas are received from the server. This is significant
   * because, having a list of deltas means that if the client needs to resend
   * deltas to the server using the CVS model, it only needs to send them at the
   * end of all the transformation, rather than once per server delta which is
   * wasteful as we can already tell if there are more server messages, they
   * will be nacked.
   *
   * @param deltas
   * @throws TransformException There is an operation in the process of
   *         transforming the received operations.
   * @throws OperationException Failed to apply the server operation to the
   *         client's editor.
   */
  public void onServerDeltas(List<TransformedWaveletDelta> deltas) throws TransformException,
      OperationException;

  /**
   * Called when acknowledgement is received from the server for an operation
   * issued by this client.
   *
   * @param operationsApplied Number of operations to ack.
   * @param signature The signature after the client operations being applied on
   *        the server.
   * @throws TransformException There is an operation in the process of
   *         transforming any existing operations.
   * @throws OperationException
   */
  public void onSuccess(int operationsApplied, HashedVersion signature) throws TransformException,
      OperationException;

  /**
   * Called when the server advertises that the delta at some version has been
   * committed to persistent storage. The client can clear any cache that is
   * used for recovery before that version.
   */
  public void onCommit(long version);
}
