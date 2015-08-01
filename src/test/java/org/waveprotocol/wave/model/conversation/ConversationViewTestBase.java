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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for the conversation view interface.
 *
 * @author anorth@google.com (Alex North)
 */
public abstract class ConversationViewTestBase extends TestCase {

  private ObservableConversationView.Listener listener;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    listener = mock(ObservableConversationView.Listener.class);
  }

  /**
   * Returns the conversation view for testing.
   */
  protected abstract ObservableConversationView getConversationView();

  /**
   * Removes the specified conversation from the underlying conversation view.
   */
  protected abstract void removeConversation(ObservableConversation conv);

  public void testEmptyViewHasNoConversations() {
    assertTrue(getConversationView().getConversations().isEmpty());
  }

  public void testCreatedRootAccessibleByGetRoot() {
    Conversation root = getConversationView().createRoot();
    assertSame(root, getConversationView().getRoot());
  }

  public void testGetRootWithoutRootDoesNotFail() {
    // Empty case
    assertNull(getConversationView().getRoot());

    // ... and with non-root conversations.
    getConversationView().createConversation();
    assertNull(getConversationView().getRoot());
  }

  public void testDuplicateCreateRootFails() {
    getConversationView().createRoot();
    try {
      getConversationView().createRoot();
      fail("Expected IllegalStateException creating duplicate root conversation");
    } catch (IllegalStateException expected) {
    }
  }

  public void testCreateConversations() {
    ObservableConversationView conversationView = getConversationView();
    Conversation conv1 = conversationView.createConversation();
    assertCollectionsEquivalent(Arrays.asList(conv1), conversationView.getConversations());

    Conversation conv2 = conversationView.createConversation();
    assertCollectionsEquivalent(Arrays.asList(conv1, conv2), conversationView.getConversations());
  }

  //
  // ObservableConversationView.
  //

  public void testNewConversationCausesEvent() {
    ObservableConversationView conversationView = getConversationView();
    conversationView.addListener(listener);
    ObservableConversation root = conversationView.createRoot();
    ObservableConversation child = conversationView.createConversation();
    verify(listener).onConversationAdded(root);
    verify(listener).onConversationAdded(child);
    verifyNoMoreInteractions(listener);
  }

  public void testDeleteConversationFromView() {
    ObservableConversationView conversationView = getConversationView();
    ObservableConversation conv = conversationView.createRoot();
    assertCollectionsEquivalent(Arrays.asList(conv), conversationView.getConversations());

    conversationView.addListener(listener);
    conv.delete();

    verify(listener).onConversationRemoved(conv);
    verifyNoMoreInteractions(listener);
    conversationView.getConversations().isEmpty();
  }

  public void testConversationRemovalCausesEvent() {
    ObservableConversationView conversationView = getConversationView();
    conversationView.addListener(listener);
    ObservableConversation conv = conversationView.createRoot();
    verify(listener).onConversationAdded(conv);

    // Now remove the conversation and check that listeners are notified
    removeConversation(conv);
    verify(listener).onConversationRemoved(conv);
    verifyNoMoreInteractions(listener);
  }

  /**
   * Asserts that two collections are contain the same elements.
   */
  private static <T> void assertCollectionsEquivalent(Collection<? extends T> a,
      Collection<? extends T> b) {
    assertEquals(a.size(), b.size());
    assertEquals(CollectionUtils.newHashSet(a), CollectionUtils.newHashSet(b));
  }
}
