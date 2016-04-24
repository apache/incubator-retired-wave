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

package org.waveprotocol.box.webclient.common.communication.callback;

/**
 * A callback for a task which may either succeed or fail.
 *
 * Exactly one of {@link #onSuccess(Object)} or {@link #onFailure(Object)}
 * must be invoked exactly once by receivers of a callback.
 *
 * @param <R> type of a successful response
 * @param <F> type of a failure response
 * @author anorth@google.com (Alex North)
 */
public interface SimpleCallback<R, F> {
  /**
   * Called when the task succeeds.
   *
   * @param response successful response object for this callback
   */
  public void onSuccess(R response);

  /**
   * Called when the task fails.
   *
   * @param reason failure reason for this callback
   */
  public void onFailure(F reason);
}
