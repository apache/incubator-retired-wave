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
 * A callback which may be called multiple times with a stream of responses.
 *
 * {@link #onUpdate(Object)} may be called any number of times before either
 * {@link #onSuccess(Object)} or {@link #onFailure(Object)}.
 *
 * @param <R> type of a successful response
 * @param <U> type of an update response
 * @param <F> type of a failure response
 * @author anorth@google.com (Alex North)
 */
public interface StreamingCallback<R, U, F> extends SimpleCallback<R, F> {
  /**
   * Called when an update arrives.
   *
   * @param update update object to return for this callback
   */
  public void onUpdate(U update);

}
