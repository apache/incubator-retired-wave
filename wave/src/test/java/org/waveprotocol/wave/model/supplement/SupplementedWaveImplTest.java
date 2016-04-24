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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.conversation.testing.FakeConversationView;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.schema.account.AccountSchemas;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.schema.supplement.UserDataSchemas;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link SupplementedWaveImpl}.
 *
 */

public final class SupplementedWaveImplTest extends TestCase {

  /**
   * Stub for a wave view, exposing versioned wavelets, and allowing version
   * increments.
   */
  private static class StubSupplementWaveView implements SupplementWaveView {
    private final Map<WaveletId, Long> versions = CollectionUtils.newHashMap();
    private final Map<WaveletId, Map<String, Long>> blipVersions = CollectionUtils.newHashMap();
    private boolean isExplicitParticipant;

    @Override
    public long getVersion(WaveletId id) {
      return versions.get(id);
    }

    @Override
    public HashedVersion getSignature(WaveletId id) {
      return HashedVersion.unsigned(0);
    }

    @Override
    public Iterable<WaveletId> getWavelets() {
      return versions.keySet();
    }

    @Override
    public Map<String, Long> getBlipVersions(WaveletId id) {
      return blipVersions.get(id);
    }

    @Override
    public boolean isExplicitParticipant() {
      return isExplicitParticipant;
    }

    public void put(WaveletId id, long version) {
      versions.put(id, version);
    }

    public void putBlipVersion(WaveletId id, String blipId, long version) {
      Map<String, Long> waveletBlipVersions = blipVersions.get(id);
      if (waveletBlipVersions == null) {
        waveletBlipVersions = CollectionUtils.newHashMap();
        blipVersions.put(id, waveletBlipVersions);
      }
      waveletBlipVersions.put(blipId, version);
    }

    /** Increments the version of a wavelet. */
    public void touch(WaveletId id) {
      if (versions.containsKey(id)) {
        versions.put(id, versions.get(id) + 1);
      }
    }
  }

  private static final SchemaCollection schemas = new SchemaCollection();
  static {
    schemas.add(new AccountSchemas());
    schemas.add(new ConversationSchemas());
    schemas.add(new UserDataSchemas());
  }

  private static final WaveletId ROOT;
  private static final String ROOT_BLIP1;

  private static final WaveletId W1;
  private static final String W1_BLIP1;
  private static final long ROOT_VERSION = 30;
  private static final long ROOT_BLIP1_VERSION = 28;

  private static final long W1_VERSION = 20;
  private static final long W1_BLIP1_VERSION = 15;

  private final static Map<WaveletId, Long> EXPECTED_ARCHIVE_VERSIONS;

  static {
    IdGenerator generator = FakeIdGenerator.create();
    ROOT = generator.newConversationRootWaveletId();
    W1 = generator.newConversationWaveletId();
    ROOT_BLIP1 = generator.newBlipId();
    W1_BLIP1 = generator.newBlipId();
    EXPECTED_ARCHIVE_VERSIONS = CollectionUtils.newHashMap();
    EXPECTED_ARCHIVE_VERSIONS.put(ROOT, ROOT_VERSION);
    EXPECTED_ARCHIVE_VERSIONS.put(W1, W1_VERSION);
  }

  private StubSupplementWaveView wave;
  private PrimitiveSupplement substrate;
  private SupplementedWave supplement;

  @Override
  protected void setUp() throws Exception {
    //
    // Initial state is:
    //  * a wave with two wavelets in view, ROOT and W1, at particular versions.
    //  * an empty supplement.
    //

    substrate = new PrimitiveSupplementImpl();
    wave = new StubSupplementWaveView();

    wave.put(ROOT, ROOT_VERSION);
    wave.put(W1, W1_VERSION);
    wave.putBlipVersion(ROOT, ROOT_BLIP1, ROOT_BLIP1_VERSION);
    wave.putBlipVersion(W1, W1_BLIP1, W1_BLIP1_VERSION);

    supplement = SupplementedWaveImpl.create(substrate, wave, DefaultFollow.ALWAYS);
  }

  private void createEmptyWave() {
    substrate = new PrimitiveSupplementImpl();
    wave = new StubSupplementWaveView();
    supplement = SupplementedWaveImpl.create(substrate, wave, DefaultFollow.ALWAYS);
  }

  /**
   * Sets up the test supplement with a real wave model behind it.
   */
  private WaveletBasedConversation setUpWithWaveModel() {
    FakeConversationView view = FakeConversationView.builder().with(schemas).build();
    WaveletBasedConversation conversation = view.createRoot();
    ParticipantId viewer = new ParticipantId("nobody@nowhere.com");
    supplement = SupplementedWaveImpl.create(substrate, view, viewer, DefaultFollow.ALWAYS);
    return conversation;
  }

  private void addTags(Wavelet wavelet) {
    wavelet.getDocument(IdUtil.TAGS_DOC_ID);
  }

  //
  // Environment tests.
  //

  public void testIncorrectExternalWaveViewThrowsException() {
    // Make view return a non-conversational id, and test that supplement actions throw exceptions.
    wave.put(FakeIdGenerator.create().newUserDataWaveletId("foo@bar.com"), 20);

    try {
      supplement.archive();
      fail();
    } catch (RuntimeException e) {
      // expected
    }

    try {
      supplement.markAsRead();
      fail();
    } catch (RuntimeException e) {
      // expected
    }
  }

  //
  // Read/unread tests.
  //

  public void testNewBlipIsUnread() {
    WaveletBasedConversation c = setUpWithWaveModel();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    assertTrue(supplement.isUnread(b));
  }

  public void testMarkBlipAsReadAffectsBlipReadState() {
    WaveletBasedConversation c = setUpWithWaveModel();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markAsRead(b);
    assertFalse(supplement.isUnread(b));
  }

  public void testMarkBlipIsIdempotent() {
    // Use real wave-model view.
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markAsRead(b);
    int blipReadVersion = substrate.getLastReadBlipVersion(w.getId(), b.getId());
    int waveletVersion = (int) w.getVersion();
    assertEquals(waveletVersion, blipReadVersion);

    // Do something to increase wavelet version without increasing blip last-modified version.
    t.appendBlip();
    assert w.getVersion() > waveletVersion : "test wave model did not increase version";

    // Test that marking blip as read again has no effect.
    supplement.markAsRead(b);
    long newBlipReadVersion = substrate.getLastReadBlipVersion(w.getId(), b.getId());
    assertEquals(blipReadVersion, (int) newBlipReadVersion);
  }

  public void testMarkingBlipAsReadAfterWaveUnreadMarksAtWaveletVersion() {
    // Mark blip as read, then mark wavelet as unread, then mark blip as read
    // again, and test that it is marked at wavelet version. There is no real
    // design reason to test this use case; this is just here because it was a
    // specific case that was failing before.
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markAsRead(b);
    supplement.markAsUnread();

    // Do something to increase wavelet version without increasing blip last-modified version.
    t.appendBlip();

    // Mark as read again, test that it's marked at wavelet version.
    supplement.markAsRead(b);
    int blipReadVersion = substrate.getLastReadBlipVersion(w.getId(), b.getId());
    assertEquals(blipReadVersion, (int) w.getVersion());
  }

  public void testMarkingBlipAsReadAfterBlipModifiedMarksAtWaveletVersion() {
    // Mark blip as read, then mark wavelet as unread, then mark blip as read
    // again, and test that it is marked at wavelet version. There is no real
    // design reason to test this use case; this is just here because it was a
    // specific case that was failing before.
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markAsRead(b);
    supplement.markAsUnread();

    // Increase both last-modified blip version and wavelet version (but latter more than former).
    b.getContent().appendXml(Blips.INITIAL_CONTENT);
    t.appendBlip();

    // Mark as read again, test that it's marked at wavelet version, not blip last-modified version.
    supplement.markAsRead(b);
    long blipReadVersion = substrate.getLastReadBlipVersion(w.getId(), b.getId());
    assertEquals(blipReadVersion, (int) w.getVersion());
  }

  //
  // Wave state tests (inbox / archive / mute).
  //

  private void assertIsInbox() {
    assertTrue(supplement.isInbox());
    assertFalse(supplement.isArchived());
    assertFalse(supplement.isMute());
  }

  private void assertIsArchived() {
    assertFalse(supplement.isInbox());
    assertTrue(supplement.isArchived());
    assertFalse(supplement.isMute());
  }

  private void assertIsUnfollowed() {
    assertFalse(supplement.isInbox());
    assertFalse(supplement.isArchived());
    assertTrue(supplement.isMute());
  }

  public void testUnmutedUnarchivedIsInbox() {
    assertIsInbox();
  }

  public void testEmptyWaveState() {
    createEmptyWave();
    assertIsArchived();
  }

  public void testUnmutedSomeArchivedIsInbox() {
    substrate.archiveAtVersion(ROOT, (int) ROOT_VERSION);
    assertIsInbox();
  }

  public void testUnmutedArchivedIsNotInbox() {
    supplement.archive();
    assertIsArchived();
  }

  public void testUnmutedRemoteArchivedIsNotInbox() {
    substrate.archiveAtVersion(ROOT, (int) ROOT_VERSION);
    substrate.archiveAtVersion(W1, (int) W1_VERSION);
    assertIsArchived();
  }

  public void testUnfollowedIsNotInbox() {
    supplement.unfollow();
    assertIsUnfollowed();
  }

  public void testRemoteUnfollowedIsNotInbox() {
    substrate.unfollow();
    assertIsUnfollowed();
  }

  public void testArchiveArchivesAllWavelets() {
    supplement.archive();
    assertSubstrateArchiveVersionsEquals(EXPECTED_ARCHIVE_VERSIONS);
    assertIsArchived();
  }

  public void testChangeAfterArchiveIsInbox() {
    supplement.archive();
    wave.touch(ROOT);
    assertIsInbox();
  }

  public void testChangeAfterUnfollowIsNotInbox() {
    supplement.unfollow();
    wave.touch(ROOT);
    assertIsUnfollowed();
  }

  public void testUnfollowUnfollowsSubstrate() {
    supplement.unfollow();
    assertEquals(Boolean.FALSE, substrate.getFollowed());
    assertIsUnfollowed();
  }

  public void testMuteClearsArchiveVersions() {
    supplement.archive();
    supplement.mute();
    assertSubstrateArchiveVersionsEquals(Collections.<WaveletId, Long>emptyMap());
  }

  public void testArchiveDoesNotClearMuteNorSetsArchiveVersions() {
    supplement.unfollow();
    supplement.archive();
    assertEquals(Boolean.FALSE, substrate.getFollowed());
    assertSubstrateArchiveVersionsEquals(Collections.<WaveletId, Long> emptyMap());
  }

  //
  // Folder tests.
  //

  public void testMoveToFolderArchives() {
    supplement.moveToFolder(100);
    assertSubstrateArchiveVersionsEquals(EXPECTED_ARCHIVE_VERSIONS);
  }

  public void testMoveToFolderRemovesExistingFolders() {
    substrate.addFolder(100);
    substrate.addFolder(101);

    supplement.moveToFolder(102);

    assertEquals(supplement.getFolders(), Collections.singleton(102));
    assertSubstrateFoldersEquals(Collections.singleton(102));
  }

  public void testMoveToFolderIsNotInbox() {
    supplement.moveToFolder(100);
    assertFalse(supplement.isInbox());
  }

  public void testMoveToFolderPreservesUnfollow() {
    supplement.unfollow();
    supplement.moveToFolder(100);
    assertEquals(Boolean.FALSE, substrate.getFollowed());
  }

  public void testMoveToInboxFolderMovesToInbox() {
    supplement.moveToFolder(SupplementedWaveImpl.INBOX_FOLDER);
    assertTrue(supplement.isInbox());
    assertSubstrateFoldersEquals(Collections.<Integer>emptySet());
  }

  public void testMoveToInboxFolderRemovesExistingFolders() {
    supplement.moveToFolder(100);
    supplement.moveToFolder(SupplementedWaveImpl.INBOX_FOLDER);
    assertEquals(Collections.emptySet(), supplement.getFolders());
  }

  public void testInboxMovesToInbox() {
    supplement.inbox();
    assertIsInbox();
  }

  public void testInboxClearsArchiveState() {
    supplement.archive();
    supplement.inbox();

    assertSubstrateArchiveVersionsEquals(Collections.<WaveletId, Long>emptyMap());
  }

  public void testMoveToFolderAllArchivesAndClearsFolderState() {
    supplement.moveToFolder(100);
    supplement.moveToFolder(SupplementedWaveImpl.ALL_FOLDER);
    assertIsArchived();
    assertEquals(Collections.emptySet(), supplement.getFolders());
    assertSubstrateArchiveVersionsEquals(EXPECTED_ARCHIVE_VERSIONS);
    assertSubstrateFoldersEquals(Collections.<Integer>emptySet());
  }

  public void testMoveToFolderAllPreservesMute() {
    supplement.moveToFolder(100);
    supplement.unfollow();
    supplement.moveToFolder(SupplementedWaveImpl.ALL_FOLDER);
    assertEquals(Boolean.FALSE, substrate.getFollowed());
  }

  public void testMutePreservesFolders() {
    supplement.moveToFolder(100);
    supplement.mute();
    assertEquals(Collections.singleton(100), supplement.getFolders());
  }

  public void testParticipantReadState() {
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();

    assertFalse(supplement.haveParticipantsEverBeenRead(w));
    assertTrue(supplement.isParticipantsUnread(w));

    supplement.markParticipantAsRead(w);
    assertTrue(supplement.haveParticipantsEverBeenRead(w));
    assertFalse(supplement.isParticipantsUnread(w));
  }

  public void testParticipantReadStateWithBlip() {
    WaveletBasedConversation c = setUpWithWaveModel();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markParticipantAsRead(c.getWavelet());
  }

  public void testParticipantReadStateAffectedByWaveletReadState() {
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();

    assertFalse(supplement.haveParticipantsEverBeenRead(w));
    assertTrue(supplement.isParticipantsUnread(w));

    supplement.markAsRead();
    assertTrue(supplement.haveParticipantsEverBeenRead(w));
    assertFalse(supplement.isParticipantsUnread(w));
  }

  public void testSeeMarksAsSeenAtVersion() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);

    final HashedVersion signature1 = HashedVersion.of(234L, new byte[] { 1 }),
        signature2 = HashedVersion.of(12828L, new byte[] { 2 });
    WaveletId waveletId1 = WaveletId.of("example.com", "somewaveletid1");
    when(supplementedView.getSignature(waveletId1)).thenReturn(signature1);

    WaveletId waveletId2 = WaveletId.of("example.com", "somewaveletid2");
    when(supplementedView.getSignature(waveletId2)).thenReturn(signature2);

    when(supplementedView.getWavelets()).thenReturn(Arrays.asList(waveletId1, waveletId2));

    SupplementedWaveImpl supplementedWave = new SupplementedWaveImpl(
        primitiveSupplement, supplementedView, DefaultFollow.ALWAYS);

    supplementedWave.see();

    verify(primitiveSupplement).setSeenVersion(eq(waveletId1), eq(signature1));
    verify(primitiveSupplement).setSeenVersion(eq(waveletId2), eq(signature2));

    // simulate the balancing get for the set we just verified above.
    when(primitiveSupplement.getSeenVersion(waveletId1)).thenReturn(signature1);
    when(primitiveSupplement.getSeenVersion(waveletId2)).thenReturn(signature2);

    assertEquals(signature1,  supplementedWave.getSeenVersion(waveletId1));
    assertEquals(signature2,  supplementedWave.getSeenVersion(waveletId2));
  }

  public void testGetSeenVersionReturnsSeenSignature() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);

    WaveletId waveletId1 = WaveletId.of("example.com", "somewaveletid1");
    HashedVersion wavelet1SeenVersion = HashedVersion.of(12312L, new byte[] { 1 });

    when(supplementedView.getWavelets()).thenReturn(Arrays.asList(waveletId1));
    when(primitiveSupplement.getSeenVersion(waveletId1)).thenReturn(
        wavelet1SeenVersion);

    SupplementedWaveImpl supplementedWave = new SupplementedWaveImpl(
        primitiveSupplement, supplementedView, DefaultFollow.ALWAYS);
    assertEquals(wavelet1SeenVersion, supplementedWave.getSeenVersion(waveletId1));
  }

  public void testTrashedIfWaveInTrash() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);
    when(primitiveSupplement.getFolders())
        .thenReturn(Arrays.asList(SupplementedWaveImpl.TRASH_FOLDER));

    assertTrue(new SupplementedWaveImpl(
        primitiveSupplement, supplementedView,DefaultFollow.ALWAYS)
        .isTrashed());
  }

  public void testNotTrashedIfWaveIsNotInTrash() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);
    when(primitiveSupplement.getFolders())
        .thenReturn(Arrays.asList(SupplementedWaveImpl.INBOX_FOLDER));

    assertFalse(new SupplementedWaveImpl(primitiveSupplement, supplementedView,
                                        DefaultFollow.ALWAYS)
        .isTrashed());
  }

  public void testHasBeenSeen() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);

    final HashedVersion signature1 = HashedVersion.of(234L, new byte[] { 1 });
    WaveletId waveletId1 = WaveletId.of("google.com", "somewaveletid1");
    when(supplementedView.getSignature(waveletId1)).thenReturn(signature1);

    when(supplementedView.getWavelets()).thenReturn(Arrays.asList(waveletId1));

    SupplementedWave supplementedWave =
        new SupplementedWaveImpl(primitiveSupplement, supplementedView, DefaultFollow.ALWAYS);

    assertFalse(supplementedWave.hasBeenSeen());

    supplementedWave.see();

    verify(primitiveSupplement).setSeenVersion(eq(waveletId1), eq(signature1));

    when(primitiveSupplement.getSeenVersion(waveletId1)).thenReturn(signature1);

    assertTrue(supplementedWave.hasBeenSeen());
  }

  public void testReadStateAffectedByTagsState() {
    WaveletBasedConversation c = setUpWithWaveModel();
    Wavelet w = c.getWavelet();

    assertTrue(supplement.isTagsUnread(w));

    addTags(w);
    assertTrue(supplement.isTagsUnread(w));

    supplement.markAsRead();
    assertFalse(supplement.isTagsUnread(w));

    supplement.markAsUnread();
    assertTrue(supplement.isTagsUnread(w));

    supplement.markTagsAsRead(w);
    assertFalse(supplement.isTagsUnread(w));
  }

  public void testParticipantReadStateAffectedByWaveletReadStateWithBlip() {
    WaveletBasedConversation c = setUpWithWaveModel();
    ObservableConversationThread t = c.getRootThread();
    ConversationBlip b = t.appendBlip();

    supplement.markAsRead();
  }

  public void testNewWaveHasNoPendingNotification() {
    assertFalse(supplement.hasPendingNotification());
  }

  public void testReportsPendingNotification() {
    substrate.setNotifiedVersion(W1, (int) W1_VERSION);
    assertTrue(supplement.hasPendingNotification());
  }

  public void testSeeingClearsPendingNotification() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);

    final byte[] SIGNATURE = new byte[] { 1 };
    final long VERSION_1 = 234L;
    final long VERSION_2 = 12828L;

    final HashedVersion signature1 = HashedVersion.of(VERSION_1, SIGNATURE),
        signature2 = HashedVersion.of(VERSION_2, SIGNATURE);
    WaveletId waveletId1 = WaveletId.of("example.com", "somewaveletid1");
    when(supplementedView.getSignature(waveletId1)).thenReturn(signature1);

    WaveletId waveletId2 = WaveletId.of("example.com", "somewaveletid2");
    when(supplementedView.getSignature(waveletId2)).thenReturn(signature2);

    when(supplementedView.getWavelets()).thenReturn(Arrays.asList(waveletId1, waveletId2));

    when(primitiveSupplement.getNotifiedWavelets())
        .thenReturn(CollectionUtils.immutableSet(waveletId1, waveletId2));
    when(primitiveSupplement.getSeenVersion(waveletId1))
        .thenReturn(HashedVersion.unsigned(0)).thenReturn(signature1);
    when(primitiveSupplement.getSeenVersion(waveletId2)).thenReturn(signature2);
    when(supplementedView.getVersion(waveletId1)).thenReturn(VERSION_1);
    when(supplementedView.getVersion(waveletId2)).thenReturn(VERSION_2);
    when(primitiveSupplement.getNotifiedVersion(waveletId1)).thenReturn((int) VERSION_1);
    when(primitiveSupplement.getNotifiedVersion(waveletId2)).thenReturn((int) VERSION_2);
    SupplementedWaveImpl supplementedWave = new SupplementedWaveImpl(
        primitiveSupplement, supplementedView, DefaultFollow.ALWAYS);

    supplementedWave.markAsNotified();

    verify(primitiveSupplement).setNotifiedVersion(eq(waveletId1), eq((int) VERSION_1));
    verify(primitiveSupplement).setNotifiedVersion(eq(waveletId2), eq((int) VERSION_2));

    assertTrue(supplementedWave.hasPendingNotification());

    supplementedWave.see();

    // Clears the deprecated pending-notification flag.
    verify(primitiveSupplement).clearPendingNotification();

    assertFalse(supplementedWave.hasPendingNotification());
  }

  public void testSeeingClearsPendingNotification_pendingNotificationFlagSet() {
    PrimitiveSupplement primitiveSupplement = mock(PrimitiveSupplement.class);
    SupplementWaveView supplementedView = mock(SupplementWaveView.class);

    final byte[] SIGNATURE = new byte[] { 1 };
    final long VERSION_1 = 234L;

    final HashedVersion signature1 = HashedVersion.of(VERSION_1, SIGNATURE);
    WaveletId waveletId1 = WaveletId.of("example.com", "somewaveletid1");
    when(supplementedView.getSignature(waveletId1)).thenReturn(signature1);
    when(primitiveSupplement.getNotifiedWavelets())
        .thenReturn(CollectionUtils.<WaveletId>immutableSet())
        .thenReturn(CollectionUtils.immutableSet(waveletId1));

    when(supplementedView.getWavelets()).thenReturn(Arrays.asList(waveletId1));

    // There is a legacy pending notification flag set.
    when(primitiveSupplement.getPendingNotification()).thenReturn(true);

    SupplementedWaveImpl supplementedWave = new SupplementedWaveImpl(
        primitiveSupplement, supplementedView, DefaultFollow.ALWAYS);
    assertTrue(supplementedWave.hasPendingNotification());

    when(primitiveSupplement.getSeenVersion(waveletId1))
        .thenReturn(HashedVersion.unsigned(0)).thenReturn(signature1);
    when(supplementedView.getVersion(waveletId1)).thenReturn(VERSION_1);
    when(primitiveSupplement.getNotifiedVersion(waveletId1)).thenReturn((int) VERSION_1);

    supplementedWave.markAsNotified();

    verify(primitiveSupplement).setNotifiedVersion(eq(waveletId1), eq((int) VERSION_1));

    assertTrue(supplementedWave.hasPendingNotification());

    supplementedWave.see();

    // Clears the deprecated pending-notification flag.
    verify(primitiveSupplement).clearPendingNotification();

    assertFalse(supplementedWave.hasPendingNotification());
  }

  //
  // Test helpers.
  //

  /**
   * Checks the archive versions in the supplement substrate.
   *
   * @param expected  expected archive versions
   */
  private void assertSubstrateArchiveVersionsEquals(Map<WaveletId, Long> expected) {
    Map<WaveletId, Long> actual = CollectionUtils.newHashMap();
    for (WaveletId wid : substrate.getArchiveWavelets()) {
      actual.put(wid, (long)  substrate.getArchiveWaveletVersion(wid));
    }

    assertEquals(expected, actual);
  }

  /**
   * Checks the folders in the supplement substrate.
   *
   * @param expected
   */
  private void assertSubstrateFoldersEquals(Set<Integer> expected) {
    Set<Integer> actual = CollectionUtils.newHashSet();
    for (Integer folder : substrate.getFolders()) {
      actual.add(folder);
    }

    assertEquals(expected, actual);
  }
}
