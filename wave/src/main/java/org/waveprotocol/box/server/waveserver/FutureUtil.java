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

package org.waveprotocol.box.server.waveserver;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utility methods for futures.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class FutureUtil {

  /**
   * Wraps an unexpected exception as a {@link RuntimeException}.
   */
  public static class UnexpectedExceptionFromFuture extends RuntimeException {
    public UnexpectedExceptionFromFuture(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Gets the result of the given future or propagates any exception of
   * the expected type. That is, in the case of an {@link ExecutionException}),
   * if the cause has the expected type, it is unwrapped and thrown directly.
   * 
   * @param <V> the result type of the future
   * @param <X> exception type expected to be wrapped
   * @param future future to check
   * @param expectedCauseClass class of exception expected to be wrapped
   * @return the result of the future
   * @throws X if the future throws an {@link ExecutionException} wrapping an
   *         exception of type X
   * @throws UnexpectedExceptionFromFuture wrapping any cause of
   *         an {@link ExecutionException} not of type X
   * @throws InterruptedException if the current thread was interrupted
   * @throws CancellationException if the future was cancelled
   */
  public static <V, X extends Exception> V getResultOrPropagateException(Future<V> future,
      Class<X> expectedCauseClass) throws X, InterruptedException {
    try {
      return future.get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (expectedCauseClass.isInstance(cause)) {
        throw expectedCauseClass.cast(cause);
      } else {
        throw new UnexpectedExceptionFromFuture(cause);
      }
    }
  }

  private FutureUtil() { } // prevent instantiation
}
