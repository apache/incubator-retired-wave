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
import static org.waveprotocol.wave.model.testing.ExtraAsserts.assertStructureEquivalent;

import org.waveprotocol.wave.model.conversation.Conversation.Anchor;
import org.waveprotocol.wave.model.conversation.testing.BlipTestUtils;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.MutableDocument.Action;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Conversation tests for the wavelet-based conversation.
 *
 * These tests mostly check the conversation interprets and generates the
 * correct manifest schema.
 *
 * @author anorth@google.com (Alex North)
 */

public class WaveletBasedConversationTest extends ConversationTestBase {

  private IdGenerator idGenerator;
  private ObservableWaveView waveView;
  private WaveBasedConversationView conversationView;
  private WaveletBasedConversation target;
  private ObservableMutableDocument<?, ?, ?> manifestDoc;

  private WaveletBasedConversationBlip.Listener blipListener;
  private WaveletBasedConversationThread.Listener threadListener;

  @Override
  protected void setUp() throws Exception {
    idGenerator = FakeIdGenerator.create();
    waveView = ConversationTestUtils.createWaveView(idGenerator);
    conversationView = WaveBasedConversationView.create(waveView, idGenerator);
    target = makeConversation();
    manifestDoc = WaveletBasedConversation.getManifestDocument(target.getWavelet());

    blipListener = mock(WaveletBasedConversationBlip.Listener.class);
    threadListener = mock(WaveletBasedConversationThread.Listener.class);

    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  protected WaveletBasedConversation makeConversation() {
    return conversationView.createConversation();
  }

  @Override
  protected WaveletBasedConversation mirrorConversation(ObservableConversation toMirror) {
    WaveletBasedConversation backer = (WaveletBasedConversation) toMirror;
    ObservableDocument backerManifestDoc = WaveletBasedConversation.getManifestDocument(
        backer.getWavelet());
    ObservableManifest manifest = backerManifestDoc.with(
        new ObservableMutableDocument.Method<ObservableManifest>() {
          @Override
          public <N, E extends N, T extends N> ObservableManifest exec(
              ObservableMutableDocument<N, E, T> doc) {
            E top = DocHelper.expectAndGetFirstTopLevelElement(doc,
                DocumentBasedManifest.MANIFEST_TOP_TAG);
            return DocumentBasedManifest.createOnExisting(DefaultDocumentEventRouter.create(doc),
                top);
          }
        });
    return WaveletBasedConversation.create(conversationView, backer.getWavelet(), manifest,
        idGenerator);
  }

  @Override
  protected void assertBlipValid(ConversationBlip blip) {
    ((WaveletBasedConversationBlip)blip).checkIsUsable();
  }

  @Override
  protected void assertBlipInvalid(ConversationBlip blip) {
    try {
      ((WaveletBasedConversationBlip)blip).checkIsUsable();
      fail("Expected blip to be invalid");
    } catch (IllegalStateException expected) {
    }
  }

  @Override
  protected void assertThreadInvalid(ConversationThread thread) {
    try {
      ((WaveletBasedConversationThread)thread).checkIsUsable();
      fail("Expected thread to be invalid");
    } catch (IllegalStateException expected) {
    }
  }

  @Override
  protected void assertThreadValid(ConversationThread thread) {
    ((WaveletBasedConversationThread)thread).checkIsUsable();
  }

  //
  // Initialization
  //

  /**
   * Tests that an empty wavelet is recognised as having no conversation
   * structure.
   */
  public void testNewWaveletHasNoConversation() {
    Wavelet wavelet = waveView.createWavelet();
    assertFalse(WaveletBasedConversation.waveletHasConversation(wavelet));
  }

  /**
   * Tests that a wavelet with an initialised manifest is recognised as having
   * conversation structure.
   */
  public void testWaveletWithManifestHasConversation() {
    assertTrue(WaveletBasedConversation.waveletHasConversation(target.getWavelet()));
  }

  /**
   * Tests that makeWaveletConversational does so.
   */
  public void testHackMakeWaveletConversationalMakesConversation() {
    Wavelet wavelet = waveView.createWavelet();
    WaveletBasedConversation.makeWaveletConversational(wavelet);
    assertTrue(WaveletBasedConversation.waveletHasConversation(wavelet));
  }

  /**
   * Tests that WaveletBasedConversation does not die if an additional
   * conversation element is added to the manifest dynamically.
   */
  public void testDynamicAdditionOfExtraConversationElementDoesNotFail() {
    // target is currently listening to the manifest, so we only need to poke it.
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        doc.createChildElement(doc.getDocumentElement(), DocumentBasedManifest.MANIFEST_TOP_TAG,
            Collections.<String, String>emptyMap());
      }
    });
  }

  /**
   * Tests that WaveletBasedConversation does not die if it is loaded on a
   * manifest document with multiple conversation elements.
   */
  public void testMultipleConversationElementsDoesNotPreventLoad() {
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        doc.createChildElement(doc.getDocumentElement(), DocumentBasedManifest.MANIFEST_TOP_TAG,
            Collections.<String, String>emptyMap());
      }
    });

    // Manifest now has multiple elements.  Re-load conversation view.
    conversationView = WaveBasedConversationView.create(waveView, idGenerator);
  }

  //
  // Anchors
  //

  /**
   * Tests that an empty manifest document means no anchor.
   */
  public void testEmptyManifestHasNoAnchor() {
    assertFalse(target.hasAnchor());
  }

  /**
   * Tests that a wavelet with a manifest but no anchor attributes is not
   * anchored.
   */
  public void testMissingAnchorAttributesMeansNoAnchor() {
    setManifestAttribute(manifestDoc, "sort", "a-value");
    assertFalse(target.hasAnchor());
  }

  /**
   * Tests that setting an anchor updates the conversation manifest correctly.
   */
  public void testSetAnchorUpdatesManifest() {
    // Anchor target(wavelet1) in alternate (wavelet2).
    WaveletBasedConversation conversation2 = makeConversation();
    populate(conversation2);
    ConversationBlip firstBlip = getFirstBlip(conversation2);
    Anchor anchor = conversation2.createAnchor(firstBlip);
    target.setAnchor(anchor);
    assertEquals(WaveletBasedConversation.idFor(conversation2.getWavelet().getId()),
        getManifestAttribute(manifestDoc, "anchorWavelet"));
    assertEquals(firstBlip.getId(), getManifestAttribute(manifestDoc, "anchorBlip"));
  }

  /**
   * Tests that setting a null anchor updates the manifest and makes the wavelet
   * un-anchored.
   */
  public void testClearAnchorClearsManifest() {
    WaveletBasedConversation conversation2 = makeConversation();
    populate(conversation2);
    Anchor anchor = conversation2.createAnchor(getFirstBlip(conversation2));
    target.setAnchor(anchor);
    target.setAnchor(null);
    assertNull(getManifestAttribute(manifestDoc, "anchorWavelet"));
    assertNull(getManifestAttribute(manifestDoc, "anchorBlip"));
  }

  //
  // Threads and blips.
  //

  public void testAbstractBlipIdMatchesConcreteBlipId() {
    populate(target);
    WaveletBasedConversationBlip convBlip = target.getRootThread().getFirstBlip();
    assertEquals(convBlip.getId(), convBlip.getBlip().getId());
  }

  /**
   * Tests that the wavelet-based conversation reads meta-data from the
   * underlying wavelet structures. This test will go away when meta-data is
   * stored in the conversation documents.
   */
  public void testConversationBlipMetadataMatchesWavelet() {
    populate(target);
    ConversationBlip convBlip = target.getRootThread().getFirstBlip();
    Wavelet wavelet = target.getWavelet();
    Blip blip = wavelet.getBlip(convBlip.getId());

    assertEquals(blip.getId(), convBlip.getId());
    assertEquals(blip.getLastModifiedVersion().longValue(), convBlip.getLastModifiedVersion());
    assertEquals(blip.getLastModifiedTime().longValue(), convBlip.getLastModifiedTime());
    assertEquals(blip.getAuthorId(), convBlip.getAuthorId());
    assertEquals(blip.getContributorIds(), convBlip.getContributorIds());
  }

  public void testAppendBlipsInRootThreadUpdatesManifest() {
    WaveletBasedConversationBlip first = target.getRootThread().appendBlip();
    assertManifestXml("<blip id=\"" + first.getId() + "\"></blip>");

    WaveletBasedConversationBlip second = target.getRootThread().appendBlip();
    assertManifestXml("<blip id=\"" + first.getId() + "\"></blip>" +
        "<blip id=\"" + second.getId() + "\"></blip>");
    assertMirrorConversationEquivalent();
  }

  public void testAppendNonInlineRepliesUpdatesManifest() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    WaveletBasedConversationThread firstReply = blip.addReplyThread();
    WaveletBasedConversationBlip firstReplyBlip = firstReply.appendBlip();
    assertManifestXml("<blip id=\"" + blip.getId() + "\">" +
        "<thread id=\"" + firstReply.getId() + "\">" +
          "<blip id=\"" + firstReplyBlip.getId() + "\"></blip>" +
        "</thread>" +
        "</blip>");
    WaveletBasedConversationThread secondReply = blip.addReplyThread();
    WaveletBasedConversationBlip secondReplyBlip = secondReply.appendBlip();
    assertManifestXml("<blip id=\"" + blip.getId() + "\">" +
        "<thread id=\"" + firstReply.getId() + "\">" +
          "<blip id=\"" + firstReplyBlip.getId() + "\"></blip>" +
        "</thread>" +
        "<thread id=\"" + secondReply.getId() + "\">" +
          "<blip id=\"" + secondReplyBlip.getId() + "\"></blip>" +
        "</thread>" +
        "</blip>");
    assertMirrorConversationEquivalent();
  }

  public void testAppendBlipsInReplyThreadsUpdatesManifest() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    WaveletBasedConversationThread reply = blip.addReplyThread();
    WaveletBasedConversationBlip firstReplyBlip = reply.appendBlip();
    WaveletBasedConversationBlip secondReplyBlip = reply.appendBlip();

    assertManifestXml("<blip id=\"" + blip.getId() + "\">" +
        "<thread id=\"" + reply.getId() + "\">" +
        "<blip id=\"" + firstReplyBlip.getId() + "\"></blip>" +
        "<blip id=\"" + secondReplyBlip.getId() + "\"></blip>" +
        "</thread>" +
        "</blip>");
    assertMirrorConversationEquivalent();
  }

  public void testAppendInlineReplyUpdatesManifest() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    WaveletBasedConversationThread reply = blip.addReplyThread(locateAfterLineElement(
        blip.getContent()));
    WaveletBasedConversationBlip replyBlip = reply.appendBlip();
    assertManifestXml("<blip id=\"" + blip.getId() + "\">" +
        "<thread id=\"" + reply.getId() + "\" inline=\"true\">" +
          "<blip id=\"" + replyBlip.getId() + "\"></blip>" +
        "</thread>" +
        "</blip>");
    assertEquals(Blips.INITIAL_HEAD +
        "<body><line></line><reply id=\"" + reply.getId() + "\"></reply></body>",
        XmlStringBuilder.innerXml(blip.getContent()).toString());
    assertMirrorConversationEquivalent();
  }

  public void testDeleteBlipNoRepliesUpdatesManifest() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    blip.delete();
    assertManifestXml("");
    assertStructureEquivalent(XmlStringBuilder.createEmpty(), blip.getContent());
    assertMirrorConversationEquivalent();
  }

  public void testDeleteBlipWithInlineReplyUpdatesManifest() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    WaveletBasedConversationThread reply = blip.addReplyThread(
        BlipTestUtils.getBodyPosition(blip) + 3);
    WaveletBasedConversationBlip replyBlip = reply.appendBlip();

    blip.delete();
    // The first blip is gone, and the inline reply and its blip are gone too.
    // Both blips' content is gone.
    assertManifestXml("");
    assertStructureEquivalent(XmlStringBuilder.createEmpty(), blip.getContent());
    assertStructureEquivalent(XmlStringBuilder.createEmpty(), replyBlip.getContent());
    assertMirrorConversationEquivalent();
  }

  // Bug 2220263.
  public void testDeleteLastBlipInThreadRemovesThread() {
    ConversationBlip rootBlip = target.getRootThread().appendBlip();
    ConversationThread topThread = rootBlip.addReplyThread();
    ConversationBlip topBlip = topThread.appendBlip();

    ConversationThread firstReply = topBlip.addReplyThread();
    firstReply.appendBlip().delete();

    assertNull(topBlip.getReplyThread(firstReply.getId()));
    assertManifestXml("<blip id=\"" + rootBlip.getId() + "\">" +
        "<thread id=\"" + topThread.getId() + "\">" +
        "<blip id=\"" + topBlip.getId() + "\"></blip>" +
        "</thread>" +
        "</blip>");
  }

  /**
   * Test that producing a complex conversation structure programatically
   * results in the expected manifest, and that that manifest is parsed
   * back into an equivalent conversation structure.
   */
  public void testComplexManifestProducesEquivalentConversation() {
    // Set up wavelet structure. The root wavelet has just a single blip.
    // The second (target) wavelet is a private reply to the root. It has
    // two blips in the root thread. The first has two reply threads with one
    // blip each. The first reply is inline, with an anchor in the content
    // of the root thread's first blip.
    WaveletBasedConversation rootConv = makeConversation();
    WaveletBasedConversationBlip rootWaveletRootBlip = rootConv.getRootThread().appendBlip();

    target.setAnchor(rootConv.createAnchor(rootConv.getRootThread().getFirstBlip()));
    WaveletBasedConversationBlip firstBlip = target.getRootThread().appendBlip();
    WaveletBasedConversationBlip secondBlip = target.getRootThread().appendBlip();

    WaveletBasedConversationThread firstReply = firstBlip.addReplyThread();
    WaveletBasedConversationBlip firstReplyFirstBlip = firstReply.appendBlip();
    final WaveletBasedConversationThread secondReply = firstBlip.addReplyThread(
        BlipTestUtils.getBodyPosition(firstBlip) + 1);
    WaveletBasedConversationBlip secondReplyFirstBlip = secondReply.appendBlip();

    firstBlip.getContent().with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        int location = locateAfterLineElement(doc);
        Point<N> point = doc.locate(location);
        E element = doc.createElement(point, "reply",
            Collections.singletonMap("id", secondReply.getId()));
      }
    });

    // Clear attributes on the manifest since we're not interested in testing those.
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        E top = DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
        doc.setElementAttribute(top, "anchorBlip", null);
        doc.setElementAttribute(top, "anchorWavelet", null);
      }
    });

    assertManifestXml(
        "<blip id=\"" + firstBlip.getId() + "\">"
        + "<thread id=\"" + firstReply.getId() + "\">"
          + "<blip id=\"" + firstReplyFirstBlip.getId() + "\"></blip>"
        + "</thread>"
        + "<thread id=\"" + secondReply.getId() + "\" inline=\"true\">"
          + "<blip id=\"" + secondReplyFirstBlip.getId() + "\"></blip>"
        + "</thread>"
        + "</blip>"
      + "<blip id=\"" + secondBlip.getId() + "\"></blip>");
    assertMirrorConversationEquivalent();
  }

  /**
   * Tests that empty threads are not ignored.
   */
  public void testCreateWithEmptyManifestThreadNotIgnored() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread thread = blip.addReplyThread();

    WaveletBasedConversation another = mirrorConversation(target);
    assertNotNull(another.getRootThread().getFirstBlip());
    assertTrue(another.getRootThread().getFirstBlip().getReplyThreads().iterator().hasNext());
  }

  /**
   * Test that a conversation can be created on a manifest that contains blips
   * that are not backed by the wavelet.
   */
  public void testBlipMissingFromWavelet() {
    WaveletBasedConversation empty = target;
    WaveletBasedConversation nonEmpty = makeConversation();
    nonEmpty.getRootThread().appendBlip();
    WaveletBasedConversation conversation = WaveletBasedConversation.create(conversationView,
        empty.getWavelet(), nonEmpty.getManifest(), idGenerator);
    assertNull(conversation.getRootThread().getFirstBlip());
    assertEquals(Collections.emptyList(), getBlipList(conversation.getRootThread()));
  }

  /**
   * Test that we can cope with blips being added to the manifest but not to the
   * wavelet.
   */
  public void testAddingBlipMissingFromWavelet() {
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        N rootThreadNode = doc.getFirstChild(doc.getDocumentElement());
        E rootThread = doc.asElement(rootThreadNode);
        doc.createChildElement(rootThread, "blip", Collections.singletonMap(
            "id", idGenerator.newBlipId()));
      }
    });
    assertNull(target.getRootThread().getFirstBlip());
    assertEquals(Collections.emptyList(), getBlipList(target.getRootThread()));
  }

  /**
   * Test that iterating a thread whose manifest contains blips not backed by the
   * wavelet skips those blips.
   */
  public void testMissingBlipIteration() {
    WaveletBasedConversationThread thread = target.getRootThread();
    WaveletBasedConversationBlip first = thread.appendBlip();
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        N rootThreadNode = doc.getFirstChild(doc.getDocumentElement());
        E rootThread = doc.asElement(rootThreadNode);
        doc.createChildElement(rootThread, "blip", Collections.singletonMap(
            "id", idGenerator.newBlipId()));
      }
    });
    WaveletBasedConversationBlip third = thread.appendBlip();
    assertEquals(3, CollectionUtils.newArrayList(thread.getManifestThread().getBlips()).size());
    assertEquals(Arrays.asList(first, third), getBlipList(thread));
  }

  // Bug 2268864.
  public void testObsoleteThreadThenRestoreRemoveBlipDoesntDie() {
    WaveletBasedConversationBlip first = target.getRootThread().appendBlip();
    WaveletBasedConversationThread willBecomeEmpty = first.addReplyThread();
    ConversationBlip toggleBlip = willBecomeEmpty.appendBlip();
    String toggleBlipId = toggleBlip.getId();

    // Make the thread empty by remotely removing its blip.
    ManifestBlip manifestRootBlip = target.getManifest().getRootThread().getBlip(0);
    ManifestThread manifestReply = manifestRootBlip.getReply(0);
    manifestReply.removeBlip(manifestReply.getBlip(0));

    assertThreadValid(willBecomeEmpty);

    // Re-add then remove the blip, as can happen in playback.
    manifestReply.appendBlip(toggleBlipId);
    manifestReply.removeBlip(manifestReply.getBlip(0));
  }

  public void testRemoveRestoreThreadAfterObsoleteThreadDoesntDie() {
    WaveletBasedConversationBlip first = target.getRootThread().appendBlip();
    WaveletBasedConversationThread reply11 = first.addReplyThread();
    ConversationBlip blip1 = reply11.appendBlip();
    String blip1Id = blip1.getId();

    WaveletBasedConversationThread reply2 = first.addReplyThread();

    // Make first thread empty by removing its blip.
    ManifestBlip manifestRootBlip = target.getManifest().getRootThread().getBlip(0);
    ManifestThread manifestReply1 = manifestRootBlip.getReply(0);
    manifestReply1.removeBlip(manifestReply1.getBlip(0));
    assertThreadValid(reply11);

    // Remove and re-add second thread, as can happen in playback.
    String thread2Id = reply2.getId();
    ManifestThread manifestReply2 = manifestRootBlip.getReply(1);
    manifestRootBlip.removeReply(manifestReply2);
    manifestRootBlip.appendReply(thread2Id, false);

    assertThreadInvalid(reply2);
  }

  //
  // Non-interface methods.
  //

  public void testGetBlipRetrievesBlip() {
    WaveletBasedConversationBlip blip = target.getRootThread().appendBlip();
    WaveletBasedConversationThread reply = blip.addReplyThread();
    WaveletBasedConversationBlip replyBlip = reply.appendBlip();

    assertSame(blip, target.getBlip(blip.getId()));
    assertSame(replyBlip, target.getBlip(replyBlip.getId()));
    assertNull(target.getBlip("foobar"));
  }

  //
  // Concurrent behaviour.
  //

  public void testConcrrentDeletionOfFinalBlipsLeavesEmptyThread() {
    WaveletBasedConversationBlip first = target.getRootThread().appendBlip();
    WaveletBasedConversationThread replyThread = first.addReplyThread();
    WaveletBasedConversationBlip b1 = replyThread.appendBlip();
    WaveletBasedConversationBlip b2 = replyThread.appendBlip();

    // Locally delete b1, remotely delete b2.
    b1.delete();

    replyThread.addListener(threadListener);
    b2.addListener(blipListener);
    target.addListener(convListener);

    remoteRemoveBlip(b2);
    // Expect blip deletion events and it to be invalid.
    verify(blipListener).onDeleted();
    verify(convListener).onBlipDeleted(b2);
    assertBlipInvalid(b2);
    assertThreadValid(replyThread);
    assertEquals(Arrays.asList(replyThread), CollectionUtils.newArrayList(first.getReplyThreads()));

    // The manifest now has a thread with no blips.
    assertEquals(1, first.getManifestBlip().numReplies());
    assertEquals(0, first.getManifestBlip().getReply(0).numBlips());

    // Still there after the next mutation.
    ObservableConversationBlip second = target.getRootThread().appendBlip();
    assertThreadValid(replyThread);
    assertEquals(Arrays.asList(replyThread), CollectionUtils.newArrayList(first.getReplyThreads()));
    verify(convListener).onBlipAdded(second);
    verifyNoMoreInteractions(blipListener, threadListener, convListener);
  }

  public void testConcurrentDeletionOfFinalThreadsLeavesEmptyBlip() {
    WaveletBasedConversationBlip first = target.getRootThread().appendBlip();
    WaveletBasedConversationThread t1 = first.addReplyThread();
    WaveletBasedConversationBlip t1b = t1.appendBlip();
    WaveletBasedConversationThread t2 = first.addReplyThread();
    WaveletBasedConversationBlip t2b = t2.appendBlip();

    // Locally delete t1, remotely delete t2.
    t1b.delete();

    first.addListener(blipListener);
    t2.addListener(threadListener);
    target.addListener(convListener);

    remoteRemoveBlip(t2b);
    remoteRemoveThread(t2);
    // Expect thread t2 deletion events and it to be invalid.
    verify(threadListener).onDeleted();
    verify(convListener).onBlipDeleted(t2b);
    verify(convListener).onThreadDeleted(t2);
    assertBlipInvalid(t2b);
    assertThreadInvalid(t2);
    assertBlipValid(first);
    assertNotNull(target.getRootThread().getFirstBlip());

    // The manifest now has an empty blip.
    assertEquals(0, first.getManifestBlip().numReplies());

    // Still there after next write.
    WaveletBasedConversationBlip second = target.getRootThread().appendBlip();
    assertBlipValid(first);
    verify(convListener).onBlipAdded(second);
    verifyNoMoreInteractions(blipListener, threadListener, convListener);
  }

  /**
   * Sets an attribute value on the conversation element in a manifest document.
   *
   * @param doc manifest document
   * @param attribute attribute to set
   * @param value value to set
   */
  private static void setManifestAttribute(MutableDocument<?, ?, ?> doc, final String attribute,
      final String value) {
    doc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        E top = DocHelper.expectAndGetFirstTopLevelElement(
            doc, DocumentBasedManifest.MANIFEST_TOP_TAG);
        doc.setElementAttribute(top, attribute, value);

      }
    });
  }

  /**
   * Gets an attribute value from the conversation element in a manifest
   * document.
   *
   * @param doc manifest document
   * @param attribute attribute to get
   * @return value of {@code attribute}
   */
  private static <N> String getManifestAttribute(MutableDocument<N, ?, ?> doc, String attribute) {
    // NOTE(user): two-stage generic unfunging is required for Sun's compiler.
    return getManifestAttributeInner(doc, attribute);
  }

  private static <E> String getManifestAttributeInner(MutableDocument<? super E, E, ?> doc,
      String attribute) {
    E top = DocHelper.expectAndGetFirstTopLevelElement(doc, DocumentBasedManifest.MANIFEST_TOP_TAG);
    return doc.getAttribute(top, attribute);
  }

  /**
   * Removes a blip from the manifest, as though a remote client had done so.
   */
  private void remoteRemoveBlip(WaveletBasedConversationBlip blip) {
    ManifestThread parentThread = blip.getThread().getManifestThread();
    ManifestBlip manifestBlip = blip.getManifestBlip();
    parentThread.removeBlip(manifestBlip);
  }

  /**
   * Removes a thread from the manifest, as though a remote client had done so.
   */
  private void remoteRemoveThread(WaveletBasedConversationThread thread) {
    ManifestBlip parentBlip = thread.getParentBlip().getManifestBlip();
    ManifestThread manifestThread = thread.getManifestThread();
    parentBlip.removeReply(manifestThread);
  }

  /**
   * Asserts that the manifest content within the "conversation" tag matches an
   * expected string.
   */
  private void assertManifestXml(final String expected) {
    manifestDoc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        XmlStringBuilder exp = XmlStringBuilder.createFromXmlString(expected);
        assertStructureEquivalent(exp.wrap("conversation"), doc);
      }
    });
  }

  /**
   * Asserts that a new conversation model built on top of the target
   * conversation's substrate matches the structure of that.
   */
  private void assertMirrorConversationEquivalent() {
    Conversation copy = mirrorConversation(target);
    assertThreadsEquivalent(target.getRootThread(), copy.getRootThread());
  }

  private static void assertThreadsEquivalent(ConversationThread expected,
      ConversationThread actual) {
    assertEquals("Mismatched id in constructed conversation thread",
        expected.getId(), actual.getId());
    assertEquals("Mismatched first blip in constructed conversation thread",
        expected.getFirstBlip() == null, actual.getFirstBlip() == null);
    Iterator<? extends ConversationBlip> expectedBlips = expected.getBlips().iterator();
    Iterator<? extends ConversationBlip> actualBlips = actual.getBlips().iterator();
    while (expectedBlips.hasNext()) {
      assertTrue("Missing blip in reconstructed conversation", actualBlips.hasNext());
      ConversationBlip expectedBlip = expectedBlips.next();
      ConversationBlip actualBlip = actualBlips.next();
      assertBlipsEquivalent(expectedBlip, actualBlip);
    }
    assertFalse("Extra blip in reconstructed conversation", actualBlips.hasNext());
  }

  private static void assertBlipsEquivalent(ConversationBlip expected,
      ConversationBlip actual) {
    assertEquals("Mismatched id in constructed conversation blip",
        expected.getId(), actual.getId());
    assertEquals("Mismatched author in constructed conversation blip",
        expected.getAuthorId(), actual.getAuthorId());
    assertEquals("Mismatched timestamp in constructed conversation blip",
        expected.getLastModifiedTime(), actual.getLastModifiedTime());
    assertEquals("Mismatched contributors in constructed conversation blip",
        expected.getContributorIds(), actual.getContributorIds());
    Iterator<? extends ConversationThread> expectedThreads =
      expected.getReplyThreads().iterator();
    Iterator<? extends ConversationThread> actualThreads = actual.getReplyThreads().iterator();
    while (expectedThreads.hasNext()) {
      assertTrue("Missing thread in reconstructed conversation", actualThreads.hasNext());
      ConversationThread expectedThread = expectedThreads.next();
      ConversationThread actualThread = actualThreads.next();
      assertThreadsEquivalent(expectedThread, actualThread);
    }
    assertFalse("Extra thread in reconstructed conversation", actualThreads.hasNext());
  }
}
