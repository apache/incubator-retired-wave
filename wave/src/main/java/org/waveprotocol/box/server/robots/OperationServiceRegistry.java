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

package org.waveprotocol.box.server.robots;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.operations.OperationService;

/**
 * Registry for accessing an {@link OperationService} to execute operations in
 * the Robot APIs.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface OperationServiceRegistry {

  /**
   * Retrieves the {@link OperationService} for the given {@link OperationType}.
   *
   * @param opType the type of operation to retrieve the service for
   * @return {@link OperationService} registered for the given
   *         {@link OperationType}
   * @throws InvalidRequestException if no {@link OperationService} could be
   *         found.
   */
  OperationService getServiceFor(OperationType opType) throws InvalidRequestException;
}
