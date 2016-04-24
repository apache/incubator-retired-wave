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

package org.waveprotocol.wave.concurrencycontrol.common;

import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.Transform;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Operations sequences representing a client and a server delta.
 *
 * @author zdwang@google.com (David Wang)
 */
public class DeltaPair {
  private final List<WaveletOperation> client;
  private final List<WaveletOperation> server;

  /**
   * Create a delta pair with a client and server op.
   * @param client
   * @param server
   */
  public DeltaPair(Iterable<WaveletOperation> client, Iterable<WaveletOperation> server) {
    this.client = CollectionUtils.newArrayList(client);
    this.server = CollectionUtils.newArrayList(server);
  }

  /**
   * Transforms this pair of deltas to a new pair of deltas.
   * If 2 deltas are the same, they nullify each other.
   *
   * The caller is responsible for setting the version on the result deltas.
   *
   * @return never null
   * @throws TransformException
   */
  public DeltaPair transform() throws TransformException {
    // If 2 deltas are the same, they nullify each other.
    if (areSame(client, server)) {
      List<WaveletOperation> outServer = CollectionUtils.newArrayList();
      // Produce version update ops for the server ops, as version on the server
      // has increased even though nullified on the client.
      Iterator<WaveletOperation> serverOpItr = server.iterator();
      while (serverOpItr.hasNext()) {
        WaveletOperation serverOp = serverOpItr.next();
        outServer.add(serverOp.createVersionUpdateOp(serverOp.getContext().getVersionIncrement(),
            serverOp.getContext().getHashedVersion()));

      }
      return new DeltaPair(Arrays.<WaveletOperation> asList(), outServer);
    }

    List<WaveletOperation> outServer = CollectionUtils.newArrayList();
    List<WaveletOperation> outClient = CollectionUtils.newArrayList(client);

    for (WaveletOperation serverOp : server) {
      WaveletOperation newServerOp = serverOp;
      List<WaveletOperation> tempClientDelta = CollectionUtils.newArrayList();

      for (WaveletOperation clientOp : outClient) {
        OperationPair<WaveletOperation> operationPair =
            Transform.transform(clientOp, newServerOp);
        clientOp = operationPair.clientOp();
        newServerOp = operationPair.serverOp();
        tempClientDelta.add(clientOp);
      }
      outClient = tempClientDelta;
      outServer.add(newServerOp);
    }
    return new DeltaPair(outClient, outServer);
  }

  /**
   * @return Held client delta.
   */
  public List<WaveletOperation> getClient() {
    return client;
  }

  /**
   * @return Held server delta.
   */
  public List<WaveletOperation> getServer() {
    return server;
  }

  /**
   * Checks whether two operation sequences are the same.
   */
  public static boolean areSame(List<WaveletOperation> deltaA, List<WaveletOperation> deltaB) {
    if (deltaA.size() != deltaB.size()) {
      return false;
    }

    for (int i = 0; i < deltaA.size(); ++i) {
      if (!matchOperations(deltaA.get(i), deltaB.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares operations by testing author and actual operation.
   */
  private static boolean matchOperations(WaveletOperation a, WaveletOperation b) {
    return a.getContext().getCreator().equals(b.getContext().getCreator()) &&
           a.equals(b);
  }
}
