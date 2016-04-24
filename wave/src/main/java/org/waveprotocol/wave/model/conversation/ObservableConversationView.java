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

import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collection;


/**
 * Extends ConversationView to provide events when conversations come into view.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ObservableConversationView extends ConversationView,
    SourcesEvents<ObservableConversationView.Listener> {
  interface Listener {
    /**
     * Notifies this listener that a conversation has been added to the view.
     */
    void onConversationAdded(ObservableConversation conversation);

    /**
     * This conversation has been deleted and should not be used further.
     */
    void onConversationRemoved(ObservableConversation conversation);
  }

  // Covariant specialisations.

  @Override
  Collection<? extends ObservableConversation> getConversations();

  @Override
  ObservableConversation getRoot();

  @Override
  ObservableConversation createRoot();

  @Override
  ObservableConversation getConversation(String id);

  @Override
  ObservableConversation createConversation();
}
