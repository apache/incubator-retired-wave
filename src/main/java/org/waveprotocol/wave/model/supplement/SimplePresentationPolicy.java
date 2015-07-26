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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.conversation.ConversationThread;

/**
 * A simple {@link PresentationPolicy}.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class SimplePresentationPolicy implements PresentationPolicy {

  private final ThreadState defaultState;

  /**
   * Creates a simple presentation policy with a default thread state to return
   * in {@link #getThreadState} when none is given.
   *
   * @param defaultState the default thread state
   */
  public SimplePresentationPolicy(ThreadState defaultState) {
    this.defaultState = defaultState;
  }

  @Override
  public ThreadState expand(ConversationThread thread, ThreadState current) {
    return ThreadState.EXPANDED;
  }

  @Override
  public ThreadState collapse(ConversationThread thread, ThreadState current) {
    return ThreadState.COLLAPSED;
  }

  @Override
  public ThreadState getThreadState(ConversationThread thread, ThreadState specified) {
    return (specified != null) ? specified : defaultState;
  }
}
