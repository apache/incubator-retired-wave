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

package org.waveprotocol.wave.model.testing;

import junit.framework.TestCase;

/**
 * Generic base implementation for a test case that tests the behaviour of a
 * single type.  This implementation holds a reference to a factory for
 * creating instances of that interface, and uses that factory to instantiates
 * the instance to test in {@link #setUp()}.
 *
 * @param <T> interface type being tested
 */
public abstract class GenericTestBase<T> extends TestCase {
  /** Factory used to create each wave to be tested. */
  protected final Factory<? extends T> factory;

  // State initialized in setUp()

  /** Target to test. */
  protected T target;

  /**
   * Creates this test case, which runs on the wave-datas created by a factory.
   *
   * @param factory  factory for creating the wave-datas to test
   */
  protected GenericTestBase(Factory<? extends T> factory) {
    this.factory = factory;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation uses the test's factory to creates a test target.
   */
  @Override
  protected void setUp() {
    target = factory.create();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void tearDown() throws Exception {
    // This is only overridden to expose tearDown to GWTTestBase (which should
    // be in GWTTestBase's scope anyway, since it extends TestCase, but for
    // some reason it isn't).
    super.tearDown();
  }
}
