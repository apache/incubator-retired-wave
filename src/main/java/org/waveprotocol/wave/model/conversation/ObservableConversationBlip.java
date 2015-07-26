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

package org.waveprotocol.wave.model.conversation;

/**
 * Extends {@link ConversationBlip} to provide events, sourced from the
 * {@link #getConversation() conversation} object.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ObservableConversationBlip extends ConversationBlip {

  // Covariant specialisations

  @Override
  ObservableConversation getConversation();

  @Override
  ObservableConversationThread getThread();

  @Override
  Iterable<? extends ObservableConversationThread> getReplyThreads();

  @Override
  Iterable<? extends LocatedReplyThread<? extends ObservableConversationThread>>
      locateReplyThreads();

  @Override
  ObservableConversationThread getReplyThread(String id);


  @Override
  ObservableConversationThread addReplyThread();

  @Override
  ObservableConversationThread addReplyThread(int location);
}
