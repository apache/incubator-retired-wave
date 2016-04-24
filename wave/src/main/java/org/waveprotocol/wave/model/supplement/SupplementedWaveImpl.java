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

import org.waveprotocol.wave.model.conversation.BlipIterators;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical implementation {@link SupplementedWave}.
 *
 */
public class SupplementedWaveImpl implements SupplementedWave {
  /**
   * Defines the predicate for whether a wave is followed by default.
   */
  public interface DefaultFollow {
    /**
     * @return the default follow state of a wave for the supplement owner.
     */
    boolean isFollowed(SupplementWaveView wave);

    /**
     * Follows a wave by default if and only if the viewer is an explicit
     * participant.
     */
    final DefaultFollow WHEN_PARTICIPANT = new DefaultFollow() {
      @Override
      public boolean isFollowed(SupplementWaveView wave) {
        return wave.isExplicitParticipant();
      }
    };

    /**
     * Follows a wave always by default.
     */
    final DefaultFollow ALWAYS = new DefaultFollow() {
      @Override
      public boolean isFollowed(SupplementWaveView wave) {
        return true;
      }
    };
  }

  /**
   * HACK(user): The fred RPC that performs the inbox() action is expressed
   * as a moveToFolder with a special folder id. moveToFolder() has special
   * behaviour to interpret this.
   */
  /* VisibleForTesting */
  static final int INBOX_FOLDER = 1;

  /**
   * The "All" folder.  moveToFolder(ALL) has special extra semantics.
   */
  /* VisibleForTesting */
  static final int ALL_FOLDER = 3;

  /**
   * The "Trash" folder. Waves with no participants are to be purged from trash
   * after a period.
   */
  /* VisibleForTesting */
  public static final int TRASH_FOLDER = 8;

  /**
   * Adapts a wave-model wave view to the {@link SupplementedWave} interface.
   */
  static class WaveViewAdapter implements SupplementWaveView {
    private final ConversationView view;
    private final ParticipantId viewer;

    public WaveViewAdapter(ConversationView view, ParticipantId viewer) {
      this.view = view;
      this.viewer = viewer;
    }

    @Override
    public Iterable<WaveletId> getWavelets() {
      List<WaveletId> ids = new ArrayList<WaveletId>();
      for (Conversation c : view.getConversations()) {
        ids.add(WaveletBasedConversation.widFor(c.getId()));
      }
      return ids;
    }

    @Override
    public HashedVersion getSignature(WaveletId id) {
      Conversation c = view.getConversation(WaveletBasedConversation.idFor(id));
      return (null != c) ?
          ((WaveletBasedConversation) c).getWavelet().getHashedVersion()
          : HashedVersion.unsigned(0);
    }

    @Override
    public long getVersion(WaveletId id) {
      Conversation c = view.getConversation(WaveletBasedConversation.idFor(id));
      return c != null ?
          // TODO(user): Once bug 2820511 is fixed, get rid of the cast.
          ((WaveletBasedConversation) c).getWavelet().getVersion()
          : PrimitiveSupplement.NO_VERSION;
    }

    @Override
    public Map<String, Long> getBlipVersions(WaveletId id) {
      Conversation c = view.getConversation(WaveletBasedConversation.idFor(id));
      Map<String, Long> blipVersions = new HashMap<String, Long>();
      for (ConversationBlip blip : BlipIterators.breadthFirst(c)) {
        blipVersions.put(blip.getId(), blip.getLastModifiedVersion());
      }
      return blipVersions;
    }

    @Override
    public boolean isExplicitParticipant() {
      for (Conversation w : view.getConversations()) {
        if (w.getParticipantIds().contains(viewer)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class CheckingSupplementWaveView implements SupplementWaveView {
    private final SupplementWaveView target;

    private CheckingSupplementWaveView(SupplementWaveView target) {
      this.target = target;
    }

    @Override
    public long getVersion(WaveletId id) {
      return target.getVersion(id);
    }

    @Override
    public HashedVersion getSignature(WaveletId id) {
      return target.getSignature(id);
    }

    @Override
    public Iterable<WaveletId> getWavelets() {
      List<WaveletId> wavelets = new ArrayList<WaveletId>();
      for (WaveletId id : target.getWavelets()) {
        if (!IdUtil.isConversationalId(id)) {
          throw new RuntimeException(
              "Error in view implementation: non-conversational wavelets were returned");
        }
        wavelets.add(id);
      }
      return wavelets;
    }

    @Override
    public Map<String, Long> getBlipVersions(WaveletId id) {
      return target.getBlipVersions(id);
    }

    @Override
    public boolean isExplicitParticipant() {
      return target.isExplicitParticipant();
    }
  }

  private final Supplement supplement;
  private final SupplementWaveView wave;
  private final DefaultFollow followPolicy;

  /**
   * Creates a supplemented wave.
   *
   * @param supplement data-holding substrate
   * @param conversation conversation view to supplement
   * @param viewer account viewing the wave
   * @param followPolicy policy for the default follow state of a wave
   * @return a supplemented wave.
   */
  public static SupplementedWave create(PrimitiveSupplement supplement,
      ConversationView conversation, ParticipantId viewer, DefaultFollow followPolicy) {
    return new SupplementedWaveImpl(supplement, new WaveViewAdapter(conversation, viewer),
        followPolicy);
  }

  /**
   * Creates a supplemented wave.
   *
   * The given SupplementWaveView implementation is untrusted, and is therefore
   * wrapped in a contract-enforcing implementation.
   *
   * @param supplement data-holding substrate
   * @param wave relevant wave state
   * @param followPolicy policy for the default follow state of a wave
   * @return a supplemented wave.
   */
  public static SupplementedWave create(PrimitiveSupplement supplement, SupplementWaveView wave,
      DefaultFollow followPolicy) {
    return new SupplementedWaveImpl(supplement, new CheckingSupplementWaveView(wave), followPolicy);
  }

  protected SupplementedWaveImpl(PrimitiveSupplement supplement, SupplementWaveView wave,
      DefaultFollow followPolicy) {
    this.supplement = new SupplementImpl(supplement);
    this.wave = wave;
    this.followPolicy = followPolicy;
  }

  @Override
  public ThreadState getThreadState(ConversationThread thread) {
    Conversation c = thread.getConversation();
    String id = c.getId();
    return supplement.getThreadState(WaveletBasedConversation.widFor(id), thread.getId());
  }

  @Override
  public boolean isUnread(ConversationBlip blip) {
    Blip raw = blip.hackGetRaw();
    return supplement.isBlipUnread(raw.getWavelet().getId(), raw.getId(), raw
            .getLastModifiedVersion().intValue());
  }

  @Override
  public boolean isParticipantsUnread(Wavelet wavelet) {
    return supplement.isParticipantsUnread(wavelet.getId(), (int) wavelet.getVersion());
  }

  @Override
  public boolean haveParticipantsEverBeenRead(Wavelet wavelet) {
    return supplement.haveParticipantsEverBeenRead(wavelet.getId());
  }

  @Override
  public boolean isTagsUnread(Wavelet wavelet) {
    return supplement.isTagsUnread(wavelet.getId(), (int) wavelet.getVersion());
  }

  @Override
  public void setThreadState(ConversationThread thread, ThreadState state) {
    supplement.setThreadState(WaveletBasedConversation.widFor(
        thread.getConversation().getId()), thread.getId(), state);
  }

  @Override
  public void markAsRead() {
    for (WaveletId id : wave.getWavelets()) {
      supplement.markWaveletAsRead(id, (int) wave.getVersion(id));
    }
  }

  @Override
  public void markParticipantAsRead(Wavelet wavelet) {
    supplement.markParticipantsAsRead(wavelet.getId(), (int) wavelet.getVersion());
  }

  @Override
  public void markTagsAsRead(Wavelet wavelet) {
    supplement.markTagsAsRead(wavelet.getId(), (int) wavelet.getVersion());
  }

  @Override
  public void markAsRead(ConversationBlip b) {
    // Because we use the current wavelet version to mark a blip as read, and
    // because the wavelet version can change independently of that blip, the
    // mark-blip-as-read action is not idempotent. Therefore, to minimise
    // chatter, we do it only for unread blips.
    if (isUnread(b)) {
      Blip raw = b.hackGetRaw();
      Wavelet wavelet = raw.getWavelet();
      supplement.markBlipAsRead(wavelet.getId(), raw.getId(),
          // It is possible that during a VersionUpdateOperatin, the blip version is updated
          // before the wavelet version is updated, hence the max.
          // TODO(user, zdwang) to remove this once the wave model does correct event boundaries.
          (int) Math.max(raw.getLastModifiedVersion(), wavelet.getVersion()));
    }
  }

  @Override
  public void markAsUnread() {
    supplement.markAsUnread();
  }

  @Override
  public void mute() {
    unfollow();
  }

  @Override
  public void follow() {
    supplement.follow();
  }

  @Override
  public void unfollow() {
    supplement.unfollow();
  }

  @Override
  public Set<Integer> getFolders() {
    return supplement.getFolders();
  }

  @Override
  public void moveToFolder(int folderId) {
    switch (folderId) {
      case INBOX_FOLDER:
        inbox();
        break;
      case ALL_FOLDER:
        archive();  // Removes from inbox.
        supplement.removeAllFolders();
        break;
      default:
        archive();
        supplement.moveToFolder(folderId);
        break;
    }
  }

  @Override
  public void inbox() {
    // TODO(user): remove follow() after mute behaviour is no longer being
    // emulated, and replace with preconditions check.
    follow();
    supplement.removeAllFolders();
    supplement.clearArchive();
  }

  @Override
  public void see() {
    for (WaveletId id : wave.getWavelets()) {
      supplement.setSeenVersion(id, wave.getSignature(id));
    }
    supplement.clearPendingNotification();
  }

  @Override
  public void see(Wavelet wavelet) {
    supplement.setSeenVersion(wavelet.getId(), wavelet.getHashedVersion());
    supplement.clearPendingNotification();
  }

  @Override
  public void archive() {
    if (isFollowed()) {
      for (WaveletId id : wave.getWavelets()) {
        supplement.archive(id, (int) wave.getVersion(id));
      }
    } else {
      // Ignore.
      // TODO(user): promote to preconditions check. The archive() action
      // should only be exposed on followed waves.
    }
  }

  @Override
  public boolean isInbox() {
    return isFollowed() && !isArchived();
  }

  @Override
  public boolean isArchived() {
    // Wave is archived iff all wavelets are archived
    for (WaveletId id : wave.getWavelets()) {
      if (!supplement.isArchived(id, (int) wave.getVersion(id))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isMute() {
    return !isFollowed();
  }

  @Override
  public boolean isFollowed() {
    return supplement.isFollowed(followPolicy.isFollowed(wave));
  }

  @Override
  public boolean isTrashed() {
    return supplement.getFolders().contains(TRASH_FOLDER);
  }

  @Override
  public WantedEvaluationSet getWantedEvaluationSet(Wavelet wavelet) {
    return supplement.getWantedEvaluationSet(wavelet.getId());
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    supplement.addWantedEvaluation(evaluation);
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId id) {
    return supplement.getSeenVersion(id);
  }

  @Override
  public boolean hasBeenSeen() {
    for (WaveletId id : wave.getWavelets()) {
      HashedVersion version = supplement.getSeenVersion(id);
      if (version != null && version.getVersion() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasPendingNotification() {
    if (!supplement.hasNotifiedVersion()) {
      // If there has been no use of notified versions, then
      // we fallback to the deprecated pending notification flag.
      return supplement.hasPendingNotification();
    }
    // If there are notified versions, we ignore the deprecated
    // pending notification flag.
    for (WaveletId waveletId : wave.getWavelets()) {
      if (supplement.hasPendingNotification(waveletId)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void markAsNotified() {
    for (WaveletId id : wave.getWavelets()) {
      supplement.markWaveletAsNotified(id, (int) wave.getVersion(id));
    }
  }

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return supplement.getGadgetState(gadgetId);
  }

  @Override
  public String getGadgetStateValue(String gadgetId, String key) {
    return supplement.getGadgetState(gadgetId).get(key);
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    supplement.setGadgetState(gadgetId, key, value);
  }
}
