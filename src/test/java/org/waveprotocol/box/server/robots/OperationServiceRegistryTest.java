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

import junit.framework.TestCase;

import org.waveprotocol.box.server.robots.operations.DoNothingService;
import org.waveprotocol.box.server.robots.operations.OperationService;

/**
 * Unit tests for {@link AbstractOperationServiceRegistry}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationServiceRegistryTest extends TestCase {

  private static class OperationServiceRegistryImpl extends AbstractOperationServiceRegistry {
  }

  private AbstractOperationServiceRegistry operationAccessor;

  @Override
  protected void setUp() {
    operationAccessor = new OperationServiceRegistryImpl();
  }

  public void testGetServiceForThrowsException() throws Exception {
    try {
      operationAccessor.getServiceFor(OperationType.BLIP_CONTINUE_THREAD);
      fail("Expected non registered OperationType to throw exception");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testRegister() throws Exception {
    OperationType operationType = OperationType.BLIP_CONTINUE_THREAD;
    DoNothingService doNothingService = DoNothingService.create();
    operationAccessor.register(operationType, doNothingService);
    OperationService service = operationAccessor.getServiceFor(operationType);
    assertEquals(doNothingService, service);
  }
}
