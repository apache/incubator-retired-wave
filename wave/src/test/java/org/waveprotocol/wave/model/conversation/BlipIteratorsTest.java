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

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.testing.BlipTestUtils;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for conversation iterators.
 *
 * @author anorth@google.com (Alex North)
 */

public class BlipIteratorsTest extends TestCase {

  private Conversation conversation;
  private ConversationBlip b1;
  private ConversationBlip b1t1b1;
  private ConversationBlip b1t1b2;
  private ConversationBlip b1t2b1;
  private ConversationBlip b2_d;
  private ConversationBlip b3;
  private ConversationBlip b3t1b1;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    conversation = buildConversation();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Tests that blipsBreathFirst makes breadth-first traversal and skips
   * deleted blips.
   */
  public void testBlipsBreadthFirst() {
    List<ConversationBlip> actual = CollectionUtils.newArrayList(
        BlipIterators.breadthFirst(conversation));
    assertEquals(Arrays.asList(b1, b3, b1t1b1, b1t1b2, b1t2b1, b3t1b1), actual);
  }

  private Conversation buildConversation() {
    IdGenerator idGenerator = FakeIdGenerator.create();
    ObservableWaveView waveView = ConversationTestUtils.createWaveView(idGenerator);
    ConversationView convView = WaveBasedConversationView.create(waveView, idGenerator);
    Conversation conv = convView.createRoot();
    b1 = conv.getRootThread().appendBlip();
    ConversationThread b1t1 = b1.addReplyThread();
    b1t1b1 = b1t1.appendBlip();
    b1t1b2 = b1t1.appendBlip();
    ConversationThread b1t2 = b1.addReplyThread(
        BlipTestUtils.getBodyPosition(b1) + 3);
    b1t2b1 = b1t1.appendBlip();

    b2_d = conv.getRootThread().appendBlip();
    ConversationThread b2t1 = b2_d.addReplyThread();
    ConversationBlip b2t1b1 = b2t1.appendBlip();
    b2_d.delete();

    b3 = conv.getRootThread().appendBlip();
    ConversationThread b3t1 = b3.addReplyThread();
    b3t1b1 = b3t1.appendBlip();

    return conv;
  }

}
