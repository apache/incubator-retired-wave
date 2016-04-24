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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.waveprotocol.wave.model.testing.ExtraAsserts.assertStructureEquivalent;

import junit.framework.TestCase;

import org.mockito.InOrder;
import org.waveprotocol.wave.model.conversation.Conversation.Anchor;
import org.waveprotocol.wave.model.conversation.ConversationBlip.LocatedReplyThread;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocument.Action;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocIterate;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.wave.WorthyChangeChecker;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for abstract conversation, thread and blip interfaces.
 *
 * @author anorth@google.com (Alex North)
 */
public abstract class ConversationTestBase extends TestCase {
  private ObservableConversation target;
  private ObservableConversation alternate;

  protected ObservableConversation.Listener convListener;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    target = makeConversation();
    alternate = makeConversation();

    convListener = mock(ObservableConversation.Listener.class);
  }

  /**
   * Creates a new, empty conversation object. All created conversations must be
   * from the same conversation view.
   */
  protected abstract ObservableConversation makeConversation();

  /**
   * Creates a new conversation object backed by the same data as another.
   * Changes made to one conversation should trigger events in the other.
   */
  protected abstract ObservableConversation mirrorConversation(ObservableConversation toMirror);

  /** Checks that a blip is still valid. */
  protected abstract void assertBlipValid(ConversationBlip blip);

  /** Checks that a blip is invalid. */
  protected abstract void assertBlipInvalid(ConversationBlip blip);

  /** Checks that a thread is invalid. */
  protected abstract void assertThreadInvalid(ConversationThread thread);

  /** Checks that a thread is valid. */
  protected abstract void assertThreadValid(ConversationThread thread);

  //
  // Anchoring
  //

  public void testEmptyConversationIsNotAnchored() {
    assertFalse(target.hasAnchor());
  }

  public void testCreateAnchor() {
    populate(alternate);
    ConversationBlip blip = getFirstBlip(alternate);
    Anchor anchor = alternate.createAnchor(blip);
    assertTrue(alternate == anchor.getConversation());
    assertTrue(blip == anchor.getBlip());
  }

  public void testSetAnchor() {
    populate(alternate);
    ConversationBlip blip = getFirstBlip(alternate);
    Anchor anchor = alternate.createAnchor(blip);
    target.setAnchor(anchor);
    assertTrue(target.hasAnchor());
    assertEquals(anchor, target.getAnchor());
  }

  public void testAnchorToSelfFails() {
    populate(target);
    ConversationBlip blip = getFirstBlip(target);
    Anchor anchor = target.createAnchor(blip);
    try {
      target.setAnchor(anchor);
      fail("Expected an IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertFalse(target.hasAnchor());
  }

  public void testClearAnchor() {
    populate(alternate);
    ConversationBlip blip = getFirstBlip(alternate);
    Anchor anchor = alternate.createAnchor(blip);
    target.setAnchor(anchor);
    target.setAnchor(null);
    assertFalse(target.hasAnchor());
  }

  // Regression test for a bug where the manifest was forgotten after
  // any element removed.
  public void testAnchorStillAccessibleAfterBlipAdded() {
    populate(alternate);
    ConversationBlip blip = getFirstBlip(alternate);
    Anchor anchor = alternate.createAnchor(blip);
    target.setAnchor(anchor);

    target.getRootThread().appendBlip();
    target.getRootThread().getFirstBlip().delete();
    assertTrue(target.hasAnchor());
    assertEquals(anchor, target.getAnchor());
  }

  //
  // Participants.
  //

  public void testAddedParticipantIsRetreived() {
    ParticipantId creator = target.getParticipantIds().iterator().next();
    ParticipantId fake = new ParticipantId("bill@foo.com");
    target.addParticipant(fake);

    assertEquals(Arrays.asList(creator, fake),
        CollectionUtils.newArrayList(target.getParticipantIds()));
  }

  public void testRemovedParticipantNoLongerRetrieved() {
    ParticipantId creator = target.getParticipantIds().iterator().next();
    ParticipantId fake = new ParticipantId("bill@foo.com");
    target.addParticipant(fake);
    target.removeParticipant(fake);

    assertEquals(Collections.singletonList(creator),
        CollectionUtils.newArrayList(target.getParticipantIds()));
  }

  public void testParticipantsAreASet() {
    ParticipantId creator = target.getParticipantIds().iterator().next();
    ParticipantId fake1 = new ParticipantId("joe");
    ParticipantId fake2 = new ParticipantId("bill");
    List<ParticipantId> participants = CollectionUtils.newArrayList(creator, fake1, fake2);

    target.addParticipant(fake1);
    target.addParticipant(fake2);
    assertEquals(participants, CollectionUtils.newArrayList(target.getParticipantIds()));

    target.addParticipant(fake2);
    assertEquals(participants, CollectionUtils.newArrayList(target.getParticipantIds()));
  }

  //
  // Threads and blips
  //

  public void testEmptyRootThreadHasNoBlips() {
    assertNotNull(target.getRootThread());
    assertSame(target, target.getRootThread().getConversation());
    assertNull(target.getRootThread().getFirstBlip());
    assertNull(target.getRootThread().getParentBlip());
  }

  public void testAppendBlipAppendsBlipsToThread() {
    ConversationThread thread = target.getRootThread();
    ConversationBlip b1 = thread.appendBlip();
    ConversationBlip b2 = thread.appendBlip();
    ConversationBlip b3 = thread.appendBlip();

    assertSame(b1, thread.getFirstBlip());
    assertSame(target, b1.getConversation());
    assertSame(thread, b1.getThread());
    assertSame(b1, target.getBlip(b1.getId()));
    assertEquals(Arrays.asList(b1, b2, b3), getBlipList(thread));
  }

  public void testInsetBlipBeforeFirstBlipCreatesNewFirstBlip() {
    ConversationThread thread = target.getRootThread();
    ConversationBlip oldFirst = thread.appendBlip();
    ConversationBlip newFirst = thread.insertBlip(oldFirst);

    assertSame(thread.getFirstBlip(), newFirst);
    assertSame(newFirst, target.getBlip(newFirst.getId()));
    assertSame(oldFirst, target.getBlip(oldFirst.getId()));
    assertEquals(Arrays.asList(newFirst, oldFirst), getBlipList(thread));
  }

  public void testInsertBlipBetweenBlipsInserts() {
    ConversationThread thread = target.getRootThread();
    ConversationBlip first = thread.appendBlip();
    ConversationBlip last = thread.appendBlip();
    ConversationBlip middle = thread.insertBlip(last);

    assertEquals(Arrays.asList(first, middle, last), getBlipList(thread));
  }

  public void testAppendRepliesAppendsRepliesToBlip() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread t1 = blip.addReplyThread();
    // Append blips to get a new ID for the next thread.
    t1.appendBlip();
    ConversationThread t2 = blip.addReplyThread();
    t2.appendBlip();
    ConversationThread t3 = blip.addReplyThread();
    t3.appendBlip();

    assertSame(blip, t1.getParentBlip());
    assertEquals(Arrays.asList(t1, t2, t3), CollectionUtils.newArrayList(blip.getReplyThreads()));
    assertThreadChildrenConsistent(blip);
  }

  public void testAppendInlineReplyCreatesInlineThread() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    int location = locateAfterLineElement(doc);
    ConversationThread thread = blip.addReplyThread(location);

    assertSame(blip, thread.getParentBlip());
    assertEquals(Collections.singletonList(LocatedReplyThread.of(thread, location)),
        blip.locateReplyThreads());
    assertThreadChildrenConsistent(blip);
  }

  public void testInlineReplyWithMultipleAnchorsUsesFirst() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    final int location = locateAfterLineElement(doc);
    ConversationThread thread = blip.addReplyThread(location);

    // Duplicate the anchor.
    doc.with(new Action() {
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        E anchor = Point.elementAfter(doc, doc.locate(location));
        E anchorParent = doc.getParentElement(anchor);
        doc.createChildElement(anchorParent, doc.getTagName(anchor),
            doc.getAttributes(anchor));
      }
    });

    assertEquals(Collections.singletonList(LocatedReplyThread.of(thread, location)),
        blip.locateReplyThreads());
  }

  public void testInlineReplyPointUpdatesWithDocContent() {
    final ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    doc.with(new Action() {
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        Point<N> startText = doc.locate(locateAfterLineElement(doc));
        doc.insertText(startText, "cd");
        // Insert reply between c|d.
        N bodyNode = DocHelper.getElementWithTagName(doc, Blips.BODY_TAGNAME);
        N textNode = doc.getFirstChild(bodyNode);
        textNode = doc.getNextSibling(textNode);
        int replyLocation = doc.getLocation(Point.inText(textNode, 1));
        blip.addReplyThread(replyLocation);

        // Insert text to give abc|d
        startText = Point.before(doc, textNode);
        doc.insertText(startText, "ab");

        int newLocation = blip.locateReplyThreads().iterator().next().getLocation();
        assertEquals(replyLocation + 2, newLocation);
      }
    });
  }

  public void testInlineReplyWithDeletedAnchorHasInvalidLocation() {
    final ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    doc.with(new Action() {
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        Point<N> startText = doc.locate(locateAfterLineElement(doc));
        doc.insertText(startText, "cd");
        // Insert reply between c|d.
        N bodyNode = DocHelper.getElementWithTagName(doc, Blips.BODY_TAGNAME);
        N textNode = doc.getFirstChild(bodyNode);
        textNode = doc.getNextSibling(textNode);
        int replyLocation = doc.getLocation(Point.inText(textNode, 1));
        ConversationThread replyThread = blip.addReplyThread(replyLocation);

        // Delete text and anchor.
        doc.deleteRange(Point.before(doc, textNode),
            Point.inElement(bodyNode, null));
        int newLocation = blip.locateReplyThreads().iterator().next().getLocation();
        assertEquals(Blips.INVALID_INLINE_LOCATION, newLocation);
      }
    });
  }

  public void testInlineRepliesInLocationOrder() {
    final ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    doc.with(new Action() {
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        Point<N> startText = doc.locate(locateAfterLineElement(doc));
        int replyLocation = doc.getLocation(startText);
        ConversationThread t1 = blip.addReplyThread(replyLocation);
        t1.appendBlip();
        // In front of t1.
        ConversationThread t2 = blip.addReplyThread(replyLocation);
        t2.appendBlip();
        // In front of the others.
        ConversationThread t3 = blip.addReplyThread(replyLocation);
        t3.appendBlip();

        // Delete t3's anchor.
        E anchorToDelete = Point.elementAfter(doc, doc.locate(replyLocation));
        doc.deleteNode(anchorToDelete);

        List<LocatedReplyThread<ConversationThread>> expected =
            new ArrayList<LocatedReplyThread<ConversationThread>>();
        expected.add(LocatedReplyThread.of(t2, replyLocation));
        expected.add(LocatedReplyThread.of(t1, replyLocation + 2));
        expected.add(LocatedReplyThread.of(t3, Blips.INVALID_INLINE_LOCATION));
        List<LocatedReplyThread<? extends ConversationThread>> threads =
            CollectionUtils.newArrayList(blip.locateReplyThreads());
        assertEquals(expected, threads);
      }
    });
  }

  public void testDeleteSingleRootThreadBlipRemovesIt() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    blip.delete();
    assertNull(target.getRootThread().getFirstBlip());
    assertBlipInvalid(blip);
  }

  public void testDeleteSingleNonRootThreadBlipRemovesIt() {
    ConversationThread thread = target.getRootThread().appendBlip().addReplyThread();
    ConversationBlip unDeleted = thread.appendBlip();
    ConversationBlip blip = thread.appendBlip();
    blip.delete();
    assertEquals(Arrays.asList(unDeleted), getBlipList(thread));
    assertBlipInvalid(blip);
  }

  public void testCanAppendAfterDeletingOnlyRootThreadBlip() {
    ConversationBlip first = target.getRootThread().appendBlip();
    first.delete();
    ConversationBlip second = target.getRootThread().appendBlip();
    assertBlipInvalid(first);
    assertBlipValid(second);
  }

  public void testCanAppendAfterDeletingRootThreadReplies() {
    ConversationBlip first = target.getRootThread().appendBlip();
    ConversationBlip second = target.getRootThread().appendBlip();
    ConversationThread reply = first.addReplyThread();
    reply.appendBlip();

    second.delete();
    first.delete();

    ConversationBlip newFirst = target.getRootThread().appendBlip();
    assertBlipValid(newFirst);
  }

  public void testDeleteBlipInThreadLeavesSiblings() {
    ConversationBlip b1 = target.getRootThread().appendBlip();
    ConversationBlip b2 = target.getRootThread().appendBlip();
    ConversationBlip b3 = target.getRootThread().appendBlip();

    b2.delete();
    assertEquals(Arrays.asList(b1, b3), getBlipList(target.getRootThread()));

    b1.delete();
    assertEquals(Arrays.asList(b3), getBlipList(target.getRootThread()));
  }

  public void testDeleteBlipWithInlineReplyDeletesReply() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    ConversationThread reply = blip.addReplyThread(locateAfterLineElement(doc));
    ConversationBlip replyBlip = reply.appendBlip();

    blip.delete();
    assertNull(target.getRootThread().getFirstBlip());
    assertThreadInvalid(reply);
    assertBlipInvalid(replyBlip);
  }

  public void testDeleteBlipWithManyRepliesDeletesReplies() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    MutableDocument<?, ?, ?> doc = blip.getContent();
    ConversationThread reply1 = blip.addReplyThread();
    // Append blips to get a new ID for the next thread.
    reply1.appendBlip();
    ConversationThread inlineReply1 = blip.addReplyThread(locateAfterLineElement(doc));
    inlineReply1.appendBlip();
    ConversationThread reply2 = blip.addReplyThread();
    reply2.appendBlip();
    ConversationThread inlineReply2 = blip.addReplyThread(locateAfterLineElement(doc));
    inlineReply2.appendBlip();

    blip.delete();
    assertNull(target.getRootThread().getFirstBlip());
    assertBlipInvalid(blip);
    assertThreadInvalid(reply1);
    assertThreadInvalid(reply2);
    assertThreadInvalid(inlineReply1);
    assertThreadInvalid(inlineReply2);
  }

  public void testDeletedConversationIsUnusable() {
    target.delete();
    assertConversationAccessible(target);
    assertConversationUnusable(target);
  }

  public void testDeleteConversationInvalidatesBlips() {
    target.getRootThread();
    ObservableConversationBlip blip1 = target.getRootThread().appendBlip();
    ObservableConversationBlip blip2 = target.getRootThread().appendBlip();
    target.addListener(convListener);
    target.delete();
    assertConversationUnusable(target);
    assertBlipInvalid(blip1);
    assertBlipInvalid(blip2);
    verify(convListener).onBlipDeleted(blip1);
    verify(convListener).onBlipDeleted(blip2);
  }

  public void testDeleteConversationInvalidatesNonRootThreads() {
    ObservableConversationBlip outerBlip = target.getRootThread().appendBlip();
    ObservableConversationThread inlineThread =
      outerBlip.addReplyThread(locateAfterLineElement(outerBlip.getContent()));
    ObservableConversationBlip innerBlip = inlineThread.appendBlip();
    target.addListener(convListener);
    target.delete();
    assertBlipInvalid(outerBlip);
    assertBlipInvalid(innerBlip);
    assertThreadInvalid(inlineThread);
  }

  public void testDeleteConversationEvents() {
    ObservableConversationBlip outerBlip = target.getRootThread().appendBlip();
    ObservableConversationThread inlineThread =
      outerBlip.addReplyThread(locateAfterLineElement(outerBlip.getContent()));
    ObservableConversationBlip innerBlip = inlineThread.appendBlip();
    target.addListener(convListener);
    target.delete();
    assertBlipInvalid(outerBlip);
    assertBlipInvalid(innerBlip);
    assertThreadInvalid(inlineThread);
    verify(convListener).onBlipDeleted(innerBlip);
    verify(convListener).onThreadDeleted(inlineThread);
    verify(convListener).onBlipDeleted(outerBlip);
    verifyNoMoreInteractions(convListener);
  }

  /**
   * Tests that non-inline replies to an inline reply are deleted
   * completely when the inline reply's parent blip is deleted. No
   * tombstones remain.
   */
  public void testDeleteBlipDeletesRepliesToInlineReply() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread inlineReply = blip.addReplyThread(locateAfterLineElement(
        blip.getContent()));
    ConversationBlip inlineReplyBlip = inlineReply.appendBlip();
    ConversationThread nonInlineReplyToReply = inlineReplyBlip.addReplyThread();
    ConversationBlip nonInlineReplyBlip = nonInlineReplyToReply.appendBlip();

    blip.delete();
    assertNull(target.getRootThread().getFirstBlip());
    assertBlipInvalid(nonInlineReplyBlip);
    assertThreadInvalid(nonInlineReplyToReply);
    assertBlipInvalid(inlineReplyBlip);
    assertThreadInvalid(inlineReply);
  }

  public void testDeleteLastBlipInNonRootThreadDeletesThread() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread replyThread = blip.addReplyThread();
    ConversationBlip replyBlip = replyThread.appendBlip();

    replyBlip.delete();
    assertFalse(blip.getReplyThreads().iterator().hasNext());
    assertThreadChildrenConsistent(blip);
    assertThreadInvalid(replyThread);
  }

  // Bug 2220263.
  public void testCanReplyAfterDeletingReplyThread() {
    ConversationThread topThread = target.getRootThread().appendBlip().addReplyThread();
    ConversationBlip topBlip = topThread.appendBlip();
    // Add two reply threads. Delete the second (by deleting its blip).
    ConversationThread firstReply = topBlip.addReplyThread();
    firstReply.appendBlip();
    ConversationThread secondReply = topBlip.addReplyThread();
    secondReply.appendBlip().delete();
    // Reply again. This used to throw IndexOutOfBounds.
    ConversationThread replacementReply = topBlip.addReplyThread();
    ConversationBlip replacementBlip = replacementReply.appendBlip();

    assertBlipValid(replacementBlip);
    assertEquals(Arrays.asList(firstReply, replacementReply),
        CollectionUtils.newArrayList(topBlip.getReplyThreads()));
  }

  public void testDeleteInlineReplyDeletesAnchor() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    XmlStringBuilder xmlBefore = XmlStringBuilder.innerXml(blip.getContent());
    ConversationThread inlineReply = blip.addReplyThread(locateAfterLineElement(
        blip.getContent()));
    ConversationBlip inlineReplyBlip = inlineReply.appendBlip();

    inlineReplyBlip.delete();
    assertBlipInvalid(inlineReplyBlip);
    assertThreadInvalid(inlineReply);
    assertStructureEquivalent(xmlBefore, blip.getContent());
  }

  public void testDeleteRootThreadRemovesAllBlips() {
    ConversationThread rootThread = target.getRootThread();
    ConversationBlip first = rootThread.appendBlip();
    ConversationBlip second = rootThread.appendBlip();

    rootThread.delete();
    assertBlipInvalid(first);
    assertBlipInvalid(second);
    assertEquals(CollectionUtils.newArrayList(), getBlipList(rootThread));
    assertThreadValid(rootThread);
  }

  public void testDeleteNonRootThreadRemovesAllBlipsAndThread() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread replyThread = blip.addReplyThread();
    ConversationBlip replyBlip1 = replyThread.appendBlip();
    ConversationBlip replyBlip2 = replyThread.appendBlip();

    replyThread.delete();
    assertFalse(blip.getReplyThreads().iterator().hasNext());
    assertThreadChildrenConsistent(blip);
    assertBlipInvalid(replyBlip1);
    assertBlipInvalid(replyBlip2);
    assertThreadInvalid(replyThread);
  }

  public void testDeleteEmptyThread() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread replyThread = blip.addReplyThread();

    replyThread.delete();
    assertFalse(blip.getReplyThreads().iterator().hasNext());
    assertThreadChildrenConsistent(blip);
    assertThreadInvalid(replyThread);
  }

  /**
   * Tests that methods which access the state of a blip without changing it
   * are correct after blip deletion.
   */
  public void testBlipCanBeAccessedAfterDeletion() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    blip.delete();

    assertBlipInvalid(blip);
    assertBlipAccessible(blip);

    assertEquals(target.getRootThread(), blip.getThread());
    assertEquals(Collections.emptyList(), getBlipList(target.getRootThread()));
    assertEquals(Collections.emptyList(), getAllReplyList(blip));
  }

  /**
   * Tests that methods which access the state of a blip (this time with a
   * child thread) without changing it are correct after blip deletion.
   */
  public void testBlipWithThreadCanBeAccessedAfterDeletion() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread thread = blip.addReplyThread();
    blip.delete();

    assertBlipInvalid(blip);
    assertBlipAccessible(blip);

    assertEquals(target.getRootThread(), blip.getThread());
    assertEquals(Collections.emptyList(), getBlipList(target.getRootThread()));
    assertEquals(blip, thread.getParentBlip());
    assertEquals(Collections.emptyList(), getAllReplyList(blip));
  }

  /**
   * Tests that methods which access the state of a thread without changing it
   * are correct after thread deletion.
   */
  public void testThreadCanBeAccessedAfterDeletion() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    ConversationThread thread = blip.addReplyThread();
    ConversationBlip replyBlip = thread.appendBlip();
    thread.delete();

    assertBlipInvalid(replyBlip);
    assertBlipAccessible(replyBlip);
    assertThreadInvalid(thread);
    assertThreadAccessible(thread);

    assertEquals(blip, thread.getParentBlip());
    assertFalse(blip.getReplyThreads().iterator().hasNext());
    assertEquals(thread, replyBlip.getThread());
    assertEquals(Collections.emptyList(), getBlipList(thread));
  }

  //
  // Tests for ObservableConversation.
  //

  public void testSetAnchorEventsAreFired() {
    populate(alternate);
    ObservableConversation.AnchorListener listener =
        mock(ObservableConversation.AnchorListener.class);
    target.addListener(listener);
    Anchor anchor1 = alternate.createAnchor(getFirstBlip(alternate));

    // Set anchor from null.
    target.setAnchor(anchor1);
    verify(listener).onAnchorChanged(null, anchor1);

    // Change anchor to different blip.
    Anchor anchor11 = alternate.createAnchor(alternate.getRootThread().getFirstBlip()
        .getReplyThreads().iterator().next().getFirstBlip());
    target.setAnchor(anchor11);
    verify(listener).onAnchorChanged(anchor1, anchor11);

    // Change anchor to different wavelet.
    ObservableConversation alternate2 = makeConversation();
    populate(alternate2);
    Anchor anchor2 = alternate2.createAnchor(getFirstBlip(alternate2));
    target.setAnchor(anchor2);
    verify(listener).onAnchorChanged(anchor11, anchor2);

    // Set anchor to null.
    target.setAnchor(null);
    verify(listener).onAnchorChanged(anchor2, null);

    // Remove listener.
    target.removeListener(listener);
    target.setAnchor(anchor1);
    verifyNoMoreInteractions(listener);
  }

  // These methods test that local modifications cause events via the
  // blip and thread listeners. They test that modifications to the underlying
  // data cause events via the conversation listener on a mirror conversation.

  public void testParticipantChangesFireEvents() {
    ParticipantId p1 = new ParticipantId("someone@example.com");
    ParticipantId p2 = new ParticipantId("else@example.com");
    ObservableConversation mirror = mirrorConversation(target);
    mirror.addListener(convListener);

    target.addParticipant(p1);
    target.addParticipant(p2);

    verify(convListener).onParticipantAdded(p1);
    verify(convListener).onParticipantAdded(p2);

    target.addParticipant(p1);
    verifyNoMoreInteractions(convListener);

    target.removeParticipant(p2);
    verify(convListener).onParticipantRemoved(p2);
  }

  public void testThreadAppendInsertBlipFiresEvent() {
    ObservableConversation mirror = mirrorConversation(target);
    mirror.addListener(convListener);

    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();
    verify(convListener).onBlipAdded(b1mirror);

    target.getRootThread().insertBlip(b1);
    ObservableConversationBlip b2mirror = mirror.getRootThread().getFirstBlip();
    verify(convListener).onBlipAdded(b2mirror);

    allowBlipTimestampChanged(convListener);
    verifyNoMoreInteractions(convListener);
  }

  public void testThreadRemovalFiresEvent() {
    ObservableConversation mirror = mirrorConversation(target);
    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationThread t1 = b1.addReplyThread();
    ObservableConversationThread t1mirror = mirror.getRootThread().getFirstBlip()
        .getReplyThreads().iterator().next();

    t1.appendBlip();
    ObservableConversationBlip b3mirror = t1mirror.getFirstBlip();

    mirror.addListener(convListener);

    // Trigger thread deletion.
    t1.delete();
    verify(convListener).onBlipDeleted(b3mirror);
    verify(convListener).onThreadDeleted(t1mirror);

    allowBlipTimestampChanged(convListener);
    verifyNoMoreInteractions(convListener);
  }

  public void testRootThreadRemovalDoesntFireEvent() {
    ObservableConversation mirror = mirrorConversation(target);
    target.getRootThread().appendBlip();
    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();

    mirror.addListener(convListener);

    // Trigger thread deletion.
    target.getRootThread().delete();
    verify(convListener).onBlipDeleted(b1mirror);

    allowBlipTimestampChanged(convListener);
    verifyNoMoreInteractions(convListener);
  }

  public void testBlipAppendReplyFiresEvent() {
    ObservableConversation mirror = mirrorConversation(target);
    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();

    mirror.addListener(convListener);

    b1.addReplyThread();
    ObservableConversationThread t1mirror = b1mirror.getReplyThreads().iterator().next();
    verify(convListener).onThreadAdded(t1mirror);

    verifyNoMoreInteractions(convListener);
  }

  public void testBlipRemovalFiresEvent() {
    ObservableConversation mirror = mirrorConversation(target);
    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();

    mirror.addListener(convListener);

    b1.delete();
    verify(convListener).onBlipDeleted(b1mirror);

    allowBlipTimestampChanged(convListener);
    verifyNoMoreInteractions(convListener);
  }

  public void testCompoundEventsFireBottomUp() {
    ObservableConversation mirror = mirrorConversation(target);

    // Build tall structure.
    // rootThread
    // |- b1 (deleted)
    //    |- t1
    //       |- b2
    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationThread t1 = b1.addReplyThread();
    ObservableConversationBlip b2 = t1.appendBlip();

    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();
    ObservableConversationThread t1mirror = b1mirror.getReplyThreads().iterator().next();
    ObservableConversationBlip b2mirror = t1mirror.getFirstBlip();

    mirror.addListener(convListener);

    // Trigger cascading deletion.
    b1.delete();

    // Timestamp changed events may have also occurred on the blip listeners.
    // Mockito doesn't support atMost on inOrder verifications, hence we cannot
    // verify those events then verifyNoMoreInteractions on the blip listeners.
    // TODO(anorth): verifyNoMoreInteractions when the CWM injects a clock.

    InOrder order = inOrder(convListener);
    order.verify(convListener).onBlipDeleted(b2mirror);
    order.verify(convListener).onThreadDeleted(t1mirror);
    order.verify(convListener).onBlipDeleted(b1mirror);

    allowBlipTimestampChanged(convListener);
    verifyNoMoreInteractions(convListener);
  }

  public void testRemovedListenersReceiveNoEvents() {
    ObservableConversation mirror = mirrorConversation(target);
    ObservableConversationBlip b1 = target.getRootThread().appendBlip();
    ObservableConversationThread t1 = b1.addReplyThread();
    ObservableConversationBlip b2 = t1.appendBlip();

    ObservableConversationBlip b1mirror = mirror.getRootThread().getFirstBlip();
    ObservableConversationThread t1mirror = b1mirror.getReplyThreads().iterator().next();
    t1mirror.getFirstBlip();

    mirror.addListener(convListener);

    mirror.removeListener(convListener);

    b1.delete();

    verifyNoMoreInteractions(convListener);
  }

  //
  // Data documents
  //

  public void testCanGetDataDocument() {
    MutableDocument<?, ?, ?> doc = target.getDataDocument("some-doc-id");
    assertNotNull(doc);
  }

  public void testCannotGetBlipAsDataDocument() {
    ConversationBlip blip = target.getRootThread().appendBlip();
    try {
      target.getDataDocument(blip.getId());
      fail("Expected an exception fetching a blip document as a data doc");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCannotGetManifestAsDataDocument() {
    try {
      target.getDataDocument("conversation");
      fail("Expected an exception fetching manifest as a data doc");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testWorthynessConstant() {
    assertEquals(Blips.THREAD_INLINE_ANCHOR_TAGNAME,
        WorthyChangeChecker.THREAD_INLINE_ANCHOR_TAGNAME);
  }

  protected static ConversationBlip getFirstBlip(Conversation conv) {
    return conv.getRootThread().getFirstBlip();
  }

  /**
   * Appends a blip to the root thread, and adds a reply to that blip with one
   * blip.
   */
  protected static void populate(Conversation conv) {
    ConversationBlip blip = conv.getRootThread().appendBlip();
    blip.addReplyThread().appendBlip();
  }

  protected static <N> int locateAfterLineElement(MutableDocument<N, ?, ?> doc) {
    return locateAfterLineElementInner(doc);
  }

  private static <N, E extends N, T extends N> int locateAfterLineElementInner(
      MutableDocument<N, E, T> doc) {
    for (E el : DocIterate.deepElementsReverse(doc, doc.getDocumentElement(), null)) {
      if (LineContainers.isLineContainer(doc, el)) {
        Point<N> point = Point.inElement((N) el, null);
        return doc.getLocation(point);
      }
    }

    LineContainers.appendLine(doc, XmlStringBuilder.createEmpty());
    return locateAfterLineElement(doc);
  }

  /**
   * Convenience function that returns the blips in a thread as a List.
   */
  protected static List<ConversationBlip> getBlipList(ConversationThread thread) {
    return CollectionUtils.newArrayList(thread.getBlips());
  }

  /**
   * Convenience function that returns all reply threads to a blip as a List.
   */
  protected static List<ConversationThread> getAllReplyList(ConversationBlip blip) {
    return CollectionUtils.newArrayList(blip.getReplyThreads());
  }

  /**
   * Verifies any number of method invocations on a mock.
   */
  protected static <T> T allow(T mock) {
    return verify(mock, atMost(Integer.MAX_VALUE));
  }

  /**
   * Allows any invocations of onBlipTimestampChanged on a mock.
   */
  protected static void allowBlipTimestampChanged(ObservableConversation.Listener mock) {
    allow(mock).onBlipTimestampChanged(any(ObservableConversationBlip.class), anyLong(), anyLong());
  }

  /**
   * Checks that the set of all reply threads of a blip is the same as the union
   * of the inline reply and non-inline reply threads.
   */
  private static void assertThreadChildrenConsistent(ConversationBlip blip) {
    Set<ConversationThread> allChildren =
        new HashSet<ConversationThread>();
    for (ConversationThread thread : blip.getReplyThreads()) {
      assertFalse(allChildren.contains(thread));
      allChildren.add(thread);
    }
    for (ConversationThread child : blip.getReplyThreads()) {
      assertTrue(allChildren.contains(child));
      allChildren.remove(child);
    }
    // make sure they are exactly equals
    assertEquals(0, allChildren.size());
  }

  /**
   * Checks that a conversation is unusable by attempting mutation.
   */
  protected static void assertConversationUnusable(Conversation conversation) {
    try {
      conversation.setAnchor(null);
      fail("Expected conversation to be unusable");
    } catch (IllegalStateException expected) {
    }

    try {
      conversation.getRootThread().appendBlip();
      fail("Expected conversation items to be unusable");
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * Checks that a conversation is accessible by examining some state.
   */
  protected static void assertConversationAccessible(Conversation conversation) {
    conversation.getAnchor();
    assertThreadAccessible(conversation.getRootThread());
  }

  /**
   * Asserts that the state-querying methods on a blip can be called.
   */
  protected static void assertBlipAccessible(ConversationBlip blip) {
    blip.getReplyThreads();
    blip.getAuthorId();
    blip.getContent();
    blip.getContributorIds();
    blip.getConversation();
    blip.getId();
    blip.locateReplyThreads();
    blip.getLastModifiedTime();
    blip.getLastModifiedVersion();
    blip.getReplyThreads();
    blip.getThread();
    blip.hackGetRaw();
    blip.isRoot();
  }

  /**
   * Asserts that the state-querying methods on a thread can be called.
   */
  protected static void assertThreadAccessible(ConversationThread thread) {
    thread.getBlips();
    thread.getConversation();
    thread.getFirstBlip();
    thread.getId();
    thread.getParentBlip();
  }
}
