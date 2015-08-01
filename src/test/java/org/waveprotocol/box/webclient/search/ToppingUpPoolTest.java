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

package org.waveprotocol.box.webclient.search;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ToppingUpPool}.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class ToppingUpPoolTest extends TestCase {

  /**
   * Factory that remembers the objects it creates.
   */
  private static class MockFactory implements ToppingUpPool.Factory<Object> {
    private final List<Object> created = new ArrayList<Object>();

    @Override
    public Object create() {
      Object o = new Object();
      created.add(o);
      return o;
    }

    void assertObjectsCreated(int i) {
      assertEquals(i, created.size());
    }
  }

  /**
   * Top-up size for pool under test.
   */
  private static final int RESERVE_SIZE = 10;

  //
  // Test objects:
  //

  private MockFactory factory;
  private FakeTimerService timer;
  private ToppingUpPool<Object> target;

  @Override
  protected void setUp() {
    factory = new MockFactory();
    timer = new FakeTimerService();
    target = new ToppingUpPool<Object>(timer, factory, RESERVE_SIZE);
  }

  public void testWarmUpOccursAfterConstruction() {
    timer.tick(1);
    factory.assertObjectsCreated(RESERVE_SIZE);
  }

  public void testGetOnInitialStateCreatesAndWarmsUp() {
    target.get();
    factory.assertObjectsCreated(1);

    timer.tick(1);
    factory.assertObjectsCreated(RESERVE_SIZE + 1);
  }

  public void testWildCountZeroInitiallyAndAfterWarmup() {
    assertEquals(0, target.getWildCount());

    timer.tick(1);
    assertEquals(0, target.getWildCount());
  }

  public void testGetIncreasesWildCount() {
    target.get();
    assertEquals(1, target.getWildCount());
    target.get();
    assertEquals(2, target.getWildCount());
  }

  public void testPutDecreasesWildCount() {
    List<Object> os = new ArrayList<Object>();
    int n = 6;

    for (int i = 0; i < n; i++) {
      os.add(target.get());
    }

    timer.tick(1);

    for (int i = 0; i < n; i++) {
      target.recycle(os.remove(0));
      assertEquals(os.size(), target.getWildCount());
    }
  }

  public void testRecycledObjectsAreReused() {
    // Get 10 items, recycle them, then get 10 again, and check that the factory is not invoked.
    List<Object> os = new ArrayList<Object>();
    int n = 10;
    for (int i = 0; i < n; i++) {
      os.add(target.get());
    }

    for (int i = 0; i < n; i++) {
      target.recycle(os.remove(0));
    }

    for (int i = 0; i < n; i++) {
      os.add(target.get());
    }
    factory.assertObjectsCreated(n);
  }
}
