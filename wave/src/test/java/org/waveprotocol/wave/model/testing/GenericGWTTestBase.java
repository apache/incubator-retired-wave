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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * The base class for running a model-related test case as a GWT test case.
 *
 * A {@link GenericGWTTestBase} contains a {@link GenericTestBase}, to which it forwards all
 * relevant testing methods.  This base class holds the reference to the
 * contained test, and forwards {@link GWTTestCase#gwtSetUp()} and
 * {@link GWTTestCase#gwtTearDown()} to it.
 *
 * To run a vanilla JUnit test case as a GWTTestCase, simply write the JUnit
 * test as an extension of {@link GenericTestBase}, and create a parallel extension
 * of this class that wraps an instance of the plain test case, and forwards
 * all test methods to it.
 *
 * @param <T> wrapped test case class
 */
public abstract class GenericGWTTestBase<T extends GenericTestBase<?>> extends GWTTestCase {
  /** The wrapped vanilla test case. */
  protected final T target;

  /**
   * The default constructor.
   */
  protected GenericGWTTestBase(T target) {
    this.target = target;
  }

  /**
   * Forwards to wrapped test's {@link GenericTestBase#setUp()}.
   */
  @Override
  protected void gwtSetUp() throws Exception {
    target.setUp();
  }

  /**
   * Forwards to wrapped test's {@link GenericTestBase#tearDown()}.
   */
  @Override
  protected void gwtTearDown() throws Exception {
    target.tearDown();
  }

  /**
   * Specifies a module to use when running this test case. The returned
   * module must cause the source for this class to be included.
   *
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.model.tests";
  }

}
