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

package org.waveprotocol.box.server.robots.operations;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Executor of robot operations.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface OperationService {

  /**
   * Tries to execute the operation in the given context.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @throws InvalidRequestException if the operation fails to perform.
   */
  void execute(OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException;
}