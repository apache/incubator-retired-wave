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


import junit.framework.TestCase;

/**
 * Tests for the utilities provided with the [Silent]OperationSink interfaces.
 *
 * @author anorth@google.com (Alex North)
 */

public class SilentOperationSinkTest extends TestCase {
  /**
   * A simple mock data class which can expect an operation, optionally
   * throwing OperationException.
   */
  final class MyData {
    boolean expecting = false;
    boolean failNextOperation = false;

    void expectOperation(boolean fail) {
      expecting = true;
      failNextOperation = fail;
    }

    void operation() throws OperationException {
      assertTrue(expecting);
      expecting = false;
      if (failNextOperation) {
        throw new OperationException("Failed for testing");
      }
    }

    void verify() {
      assertFalse(expecting);
    }
  }

  final class MyOperation implements Operation<MyData> {
    public void apply(MyData target) throws OperationException {
      target.operation();
    }
  }

  private MyData data;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    data = new MyData();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    data.verify();
  }

  /**
   * Tests that the silent operation sink executor applies ops.
   */
  public void testSilentOperationSinkExecutorAppliesOps() {
    SilentOperationSink<MyOperation> sink = 
        SilentOperationSink.Executor.<MyOperation, MyData>build(data);
    data.expectOperation(false);
    sink.consume(new MyOperation());
  }

  /**
   * Tests that the silent operation sink executor rethrows an exception
   * as an operation runtime exception.
   */
  public void testSilentOperationSinkExecutorAdaptsException() {
    SilentOperationSink<MyOperation> sink = 
        SilentOperationSink.Executor.<MyOperation, MyData>build(data);
    data.expectOperation(true);
    try {
      sink.consume(new MyOperation());
      fail("Expected an operation runtime exception");
    } catch (OperationRuntimeException expected) {
    }
  }
}
