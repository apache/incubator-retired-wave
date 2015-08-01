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

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Map;

/**
 * Abstract class for registering and accessing {@link OperationService} to
 * execute operations for use in the Robot APIs. Note that this class is not
 * thread safe.
 *
 * Implementations of this class are expected to define the way and moment when
 * operations are registered.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public abstract class AbstractOperationServiceRegistry implements OperationServiceRegistry {

  private static Log LOG = Log.get(AbstractOperationServiceRegistry.class);

  private Map<OperationType, OperationService> operationMap = Maps.newHashMap();

  public AbstractOperationServiceRegistry() {
  }

  @Override
  public final OperationService getServiceFor(OperationType opType) throws InvalidRequestException {
    OperationService service = operationMap.get(opType);
    if (service == null) {
      throw new InvalidRequestException("No OperationService found for " + opType);
    }
    return service;
  }

  /**
   * Registers the {@link OperationService} for the given {@link OperationType}.
   *
   * @param operation the type of the operation to register for
   * @param service the {@link OperationService} to be registered
   */
  protected final void register(OperationType operation, OperationService service) {
    // Do a put first so we can make it use a concurrent map if needed.
    OperationService oldValue = operationMap.put(operation, service);
    if (oldValue != null) {
      LOG.warning("The OperationService for " + operation.name() + " was overwritten");
    }
  }
}
