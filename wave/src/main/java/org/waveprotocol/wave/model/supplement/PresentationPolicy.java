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
 * A set of pluggable strategies for determining presentation behaviour based on
 * user actions and user data.
 *
 */
public interface PresentationPolicy {

  /**
   * Determines the presentation state of a thread according to the policy.
   *
   * @param thread Thread for which thread state is requested
   * @param specified Current thread state in user-data (may be null to
   *        indicate unspecified state)
   * @return the thread state as it should be presented - never null.
   */
  ThreadState getThreadState(ConversationThread thread, ThreadState specified);

  /** @return the state to store in order to expand a thread. */
  ThreadState expand(ConversationThread thread, ThreadState current);

  /** @return the state to store in order to expand a thread. */
  ThreadState collapse(ConversationThread thread, ThreadState current);

  /** A default presentation policy. */
  public final PresentationPolicy DEFAULT = new SimplePresentationPolicy(ThreadState.EXPANDED);
}
