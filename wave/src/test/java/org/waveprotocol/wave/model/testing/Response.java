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

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

/**
 * A simple answer for providing type-safe generic containers as return
 * values to Mockito stubbing calls.
 *
 * Java doesn't resolve the wildcard generic here:
 * <pre>
 * interface Container {
 *   Collection<? extends Foo> getFoos();
 * }
 *
 * public void testFooContainer() {
 *   Container c = mock(Container.class);
 *   when(c.getFoos()).thenReturn(Arrays.asList(a, b));
 * }
 * </pre>
 *
 * Instead, try:
 * <pre>
 * public void testFooContainer() {
 *   Container c = mock(Container.class);
 *   when(c.getFoos()).thenAnswer(Response.of(Arrays.asList(a, b)));
 * }
 * </pre>
 * or
 * <pre>
 * public void testFooContainer() {
 *   Container c = mock(Container.class);
 *   when(c.getFoos()).thenAnswer(Response.ofList(a, b));
 * }
 * </pre>
 *
 * Note that {@code Mockito.doReturn()} does work, but is unnatural.
 * <pre>
 * public void testFooContainer() {
 *   Container c = mock(Container.class);
 *   doReturn(Arrays.asList(a, b)).when(c.getFoos());
 * }
 * </pre>
 *
 * @author anorth@google.com (Alex North)
 */
public final class Response {
  /**
   * Creates a response which returns a value.
   *
   * @param response the value to return
   */
  public static <T> ResponseAnswer<T> of(T response) {
    return new ResponseAnswer<T>(response);
  }

  /**
   * Creates a response which returns a list of values.
   */
  @SuppressWarnings("unchecked")
  public static <T> ResponseAnswer<List<T>> ofList(T... responses) {
    return new ResponseAnswer<List<T>>(Arrays.asList(responses));
  }

  /**
   * An answer which simply returns a response value.
   *
   * @param <T> type of the response
   */
  public static final class ResponseAnswer<T> implements Answer<T> {
    private final T response;

    ResponseAnswer(T response) {
      this.response = response;
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
      return response;
    }
  }

  private Response() {
  }
}
