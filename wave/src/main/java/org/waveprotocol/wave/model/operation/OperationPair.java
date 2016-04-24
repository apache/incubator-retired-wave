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

package org.waveprotocol.wave.model.operation;

/**
 * A pair of operations.
 */
public final class OperationPair<O> {

  private final O clientOp;
  private final O serverOp;

  /**
   * Constructs an OperationPair from a client operation and a server operation.
   *
   * @param clientOp The client's operation.
   * @param serverOp The server's operation.
   */
  public OperationPair(O clientOp, O serverOp) {
    this.clientOp = clientOp;
    this.serverOp = serverOp;
  }

  /**
   * @return The client's operation.
   */
  public O clientOp() {
    return clientOp;
  }

  /**
   * @return The server's operation.
   */
  public O serverOp() {
    return serverOp;
  }

}
