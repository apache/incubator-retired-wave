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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * @author zdwang@google.com (David Wang)
 */

public class DocumentBasedManifestThreadTest extends TestCase {

  /**
   * Test the delegation is wired up properly
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testWiringUp() {
    // Create mock list and value.
    ObservableElementList<ObservableManifestBlip, String> blips = mock(
        ObservableElementList.class, "blips");
    BasicValue<String> id = mock(ObservableBasicValue.class, "id");
    ObservableBasicValue<Boolean> inlined = mock(ObservableBasicValue.class, "inlined");
    ObservableManifestThread.Listener threadListener = mock(
        ObservableManifestThread.Listener.class, "threadListener");

    // Create mock objects to use as return value
    final ObservableManifestBlip blip = mock(ObservableManifestBlip.class, "blip");
    final Iterable<ObservableManifestBlip> iterator = mock(Iterable.class, "iterator");

    // Listeners that the manifest thread will add.
    ArgumentCaptor<ObservableElementList.Listener> blipsListener =
        ArgumentCaptor.forClass(ObservableElementList.Listener.class);

    // Create a thread to test.
    DocumentBasedManifestThread thread = new DocumentBasedManifestThread(blips, id, inlined);
    thread.addListener(threadListener);
    verify(blips).addListener(blipsListener.capture());

    // Pretend list and value events.
    blipsListener.getValue().onValueAdded(blip);
    blipsListener.getValue().onValueRemoved(blip);

    verify(threadListener).onBlipAdded(same(blip));
    verify(threadListener).onBlipRemoved(same(blip));
    verify(blip).detachListeners();

    when(blips.add("b+1")).thenReturn(blip);
    when(blips.add(1, "b+2")).thenReturn(blip);
    when(blips.get(2)).thenReturn(blip);
    when(blips.getValues()).thenReturn(iterator);
    when(blips.indexOf(same(blip))).thenReturn(1);
    when(blips.remove(same(blip))).thenReturn(true);
    when(blips.size()).thenReturn(2);

    // Exercise the ManifestThread interface.
    assertEquals(blip, thread.appendBlip("b+1"));
    assertEquals(blip, thread.insertBlip(1, "b+2"));
    assertEquals(blip, thread.getBlip(2));
    assertEquals(iterator, thread.getBlips());
    assertEquals(1, thread.indexOf(blip));
    assertTrue(thread.removeBlip(blip));
    assertEquals(2, thread.numBlips());

    when(id.get()).thenReturn("cc");
    when(inlined.get()).thenReturn(false);

    assertEquals("cc", thread.getId());
    assertFalse(thread.isInline());

    // Verify listener can be detached.
    thread.removeListener(threadListener);
    blipsListener.getValue().onValueAdded(blip);
    verifyNoMoreInteractions(threadListener);
  }

  /**
   * Very basic test to see that we can create the object with a document and use it.
   */
  public void testSimpleUsageWithDocument() {
    ObservableManifestThread thread = buildThreadElement(
        ConversationTestUtils.createManifestDocument(), "t1");

    ManifestBlip blip = thread.appendBlip("b+1");
    assertNotNull(blip);
    assertEquals("b+1", blip.getId());
    assertFalse(thread.isInline());
  }

  private <N> ObservableManifestThread buildThreadElement(
      ObservableMutableDocument<N, ?, ?> document, String id) {
    return buildThreadElement2(document, id);
  }

  private <E> ObservableManifestThread buildThreadElement2(
      ObservableMutableDocument<? super E, E, ?> document, String id) {
    E root = DocHelper.getOrCreateFirstTopLevelElement(document, "conversation");
    E blip = document.createChildElement(root, "blip",
        CollectionUtils.immutableMap("id", "b+top"));
    E container = document.createChildElement(blip, "thread",
        CollectionUtils.immutableMap("id", id));
    return DocumentBasedManifestThread.create(DefaultDocumentEventRouter.create(document),
        container);
  }
}
