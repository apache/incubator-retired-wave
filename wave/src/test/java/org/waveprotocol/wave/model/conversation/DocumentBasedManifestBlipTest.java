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


import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * @author zdwang@google.com (David Wang)
 */

public class DocumentBasedManifestBlipTest extends TestCase {

  /**
   * Test the delegation is wired up properly
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testWiringUp() {
    // Create mock list and value.
    final ObservableElementList<ObservableManifestThread,
        DocumentBasedManifestBlip.ThreadInitialiser>
            threads = mock(ObservableElementList.class, "threads");
    final BasicValue<String> id = mock(BasicValue.class, "id");

    // Create mock objects to use as return value.
    final ObservableManifestThread thread = mock(ObservableManifestThread.class, "thread");
    final Iterable<ObservableManifestThread> iterator = mock(Iterable.class, "iterator");

    // Listeners that the manifest blip will add.
    ArgumentCaptor<ObservableElementList.Listener> threadsListener =
        ArgumentCaptor.forClass(ObservableElementList.Listener.class);

    // Create a blip to test.
    DocumentBasedManifestBlip blip = new DocumentBasedManifestBlip(threads, id);

    // Expect adding of listeners.
    verify(threads).addListener(threadsListener.capture());

    // Listener to the target blip
    final ObservableManifestBlip.Listener blipListener = mock(
        ObservableManifestBlip.Listener.class, "blipListener");

    blip.addListener(blipListener);

    // Pretend xml list events.
    threadsListener.getValue().onValueAdded(thread);
    threadsListener.getValue().onValueRemoved(thread);

    // Pretends events from xml should cause emit of events on blip listener.
    verify(blipListener).onReplyAdded(same(thread));
    verify(blipListener).onReplyRemoved(same(thread));
    verify(thread).detachListeners();

    when(threads.add(new DocumentBasedManifestBlip.ThreadInitialiser("t1", false))).thenReturn(thread);
    when(threads.add(1, new DocumentBasedManifestBlip.ThreadInitialiser("t2", true))).thenReturn(thread);
    when(threads.get(2)).thenReturn(thread);
    when(threads.getValues()).thenReturn(iterator);
    when(threads.indexOf(same(thread))).thenReturn(1);
    when(threads.remove(same(thread))).thenReturn(true);
    when(threads.size()).thenReturn(2);

    // Exercise the ManifesetBlip interface.
    assertEquals(thread, blip.appendReply("t1", false));
    assertEquals(thread, blip.insertReply(1, "t2", true));
    assertEquals(thread, blip.getReply(2));
    assertEquals(iterator, blip.getReplies());
    assertEquals(1, blip.indexOf(thread));
    assertTrue(blip.removeReply(thread));
    assertEquals(2, blip.numReplies());

    when(id.get()).thenReturn("cc");
    assertEquals("cc", blip.getId());

    // should not emit receive any more events
    blip.removeListener(blipListener);
    threadsListener.getValue().onValueAdded(thread);
    Mockito.verifyNoMoreInteractions(blipListener);
  }

  /**
   * Very basic test to see that we can create the object with a document and use it.
   */
  public void testSimpleUsageWithDocument() {
    ObservableManifestBlip blip = buildBlipElement(ConversationTestUtils.createManifestDocument(),
        "b+1");

    assertEquals("b+1", blip.getId());
    ManifestThread reply = blip.appendReply("t1", false);
    assertNotNull(reply);
    assertEquals("t1", reply.getId());
  }

  private <N> ObservableManifestBlip buildBlipElement(
      ObservableMutableDocument<N, ?, ?> document, String id) {
    return buildBlipElement2(document, id);
  }

  private <E> ObservableManifestBlip buildBlipElement2(
      ObservableMutableDocument<? super E, E, ?> document, String id) {
    E root = DocHelper.getOrCreateFirstTopLevelElement(document, "conversation");
    E container = document.createChildElement(root, "blip",
        CollectionUtils.immutableMap("id", id));
    return DocumentBasedManifestBlip.create(DefaultDocumentEventRouter.create(document),
        container);
  }
}
