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

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.ObservableBasicSet;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableMonotonicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicSet;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBoolean;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedMonotonicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedMonotonicValue;
import org.waveprotocol.wave.model.conversation.InboxState;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletIdSerializer;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.util.ValueUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionSerializer;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Set;

/**
 * Implementation of the supplement ADT that uses a wavelet as the underlying
 * data structure.
 *
 * The wavelet uses separate documents for each of:
 * <ul>
 * <li>read-state</li>
 * <li>folder state</li>
 * <li>archiving state</li>
 * <li>muted state</li>
 * <li>abuse</li>
 * <li>seen-state and pending-notifications state</li>
 * </ul>
 *
 * The read state is tracked in an element per wavelet. That element tracks blip
 * read-versions, the participants read-version, and the wavelet-override
 * version, using the {@link DocumentBasedMonotonicMap} and
 * {@link DocumentBasedMonotonicValue} embeddings. Below is an example state of
 * the read-state document.
 *
 * <pre>
 *   &lt;data&gt;
 *     &lt;wavelet i=&quot;example.org!conv+root&quot;&gt;
 *       &lt;all v=&quot;25&quot;/&gt;
 *       &lt;participants v=&quot;12&quot;/&gt;
 *       &lt;blip i=&quot;8fJd77*2&quot; v=&quot;7&quot;/&gt;
 *       &lt;all v=&quot;28&quot;/&gt;
 *       &lt;blip i=&quot;8fJd77*7&quot; v=&quot;38&quot;/&gt;
 *       &lt;blip i=&quot;8fJd77*7&quot; v=&quot;11&quot;/&gt;
 *     &lt;/wavelet&gt;
 *     &lt;wavelet i=&quot;conversation/dRwppo8*34&quot;&gt;
 *       &lt;blip i=&quot;dRwppo8*35&quot; v=&quot;4&quot;/&gt;
 *       &lt;blip i=&quot;dRwppo8*36&quot; v=&quot;15&quot;/&gt;
 *     &lt;/wavelet&gt;
 *   &lt;/data&gt;
 * </pre>
 *
 * The interpretation of that state, as provided by
 * {@link DocumentBasedMonotonicMap} and {@link DocumentBasedMonotonicValue},
 * is:
 * <ul>
 * <li>wavelet {@code example.org!conv+root} has a wavelet read-version of 28, a
 * participants read-version of 12, a read version of 7 for blip {@code
 * 8fJd77*2}, and a read version of 38 for blip {@code 8fJd77*7}.</li>
 * <li>wavelet {@code conversation/dRwppo8*34} has a read version of 4 for blip
 * {@code dRwppo8*35} and a read version of 15 for blip {@code dRwppo8*36}.</li>
 * </ul>
 *
 * The folder state is tracked in an element per folder, according to the
 * {@link DocumentBasedBasicSet} embedding. An example folder state:
 *
 * <pre>
 *   &lt;data&gt;
 *     &lt;folder i=&quot;3&quot;/&gt;
 *     &lt;folder i=&quot;12&quot;/&gt;
 *     &lt;folder i=&quot;12&quot;/&gt;
 *   &lt;/data&gt;
 * </pre>
 *
 * The interpretation of that state is that the wave is in folders 3 and 12.
 *
 * The archiving state is tracked in an element per wavelet, with a map of
 * wavelet ids and versions. When a wave is archived, all the versions of its
 * conversation wavelets are saved in the archiving document.
 *
 * An example archiving state:
 *
 * <pre>
 *   &lt;data&gt;
 *     &lt;archive i=&quot;example.org!conv+root&quot; v=&quot;48&quot; /&gt;
 *     &lt;archive i=&quot;conversation/dRwppo8*34&quot; v=&quot;15&quot; /&gt;
 *   &lt;/data&gt;
 * </pre>
 *
 * The interpretation of that state is that the wave is
 * {@link InboxState#ARCHIVE archived} as long as its wavelet versions don't go
 * above 48 for "root" and 15 for "dRwppo8*34".
 *
 * The muted state is reflected by two boolean values, stored in two separate
 * documents. TODO(hearnden/flopiano): improve the handling of mute and clear to
 * be less wasteful.
 *
 * Example:
 *
 * <pre>
 *   &lt;data muted=&quot;true&quot; /&gt;
 * </pre>
 *
 * Abuse State is managed by the {@link DocumentBasedAbuseStore} class.
 *
 */
public final class WaveletBasedSupplement implements ObservablePrimitiveSupplement {

  public static final String READSTATE_DOCUMENT = "m/read";
  public static final String PRESENTATION_DOCUMENT = "m/presentation";
  public static final String FOLDERS_DOCUMENT = "m/folder";
  public static final String ARCHIVING_DOCUMENT = "m/archiving";
  public static final String MUTED_DOCUMENT = "m/muted";
  public static final String CLEARED_DOCUMENT = "m/cleared";
  public static final String ABUSE_DOCUMENT = "m/abuse";
  public static final String SEEN_DOCUMENT = "m/seen";
  public static final String GADGETS_DOCUMENT = "m/gadgets";

  public static final String SEEN_VERSION_TAG = "seen";
  public static final String NOTIFIED_VERSION_TAG = "notified";
  public static final String WAVELET_TAG = "wavelet";
  public static final String BLIP_READ_TAG = "blip";
  public static final String PARTICIPANTS_READ_TAG = "participants";
  public static final String TAGS_READ_TAG = "tags";
  public static final String WAVELET_READ_TAG = "all";
  public static final String CONVERSATION_TAG = "conversation";
  public static final String THREAD_TAG = "thread";
  public static final String BLIP_TAG = "blip";
  public static final String ARCHIVE_TAG = "archive";
  public static final String VERSION_ATTR = "v";
  public static final String ID_ATTR = "i";
  public static final String FOLDER_TAG = "folder";
  public static final String MUTED_TAG = "muted";
  public static final String MUTED_ATTR = "muted";
  public static final String CLEARED_TAG = "cleared";
  public static final String CLEARED_ATTR = "cleared";
  public static final String SIGNATURE_ATTR = "signature";
  public static final String STATE_ATTR = "state";
  public static final String NOTIFICATION_TAG = "notification";
  public static final String PENDING_NOTIFICATION_ATTR = "pending";
  public static final String GADGET_TAG = "gadget";
  public static final String PERMISSIONS_ATTR = "p";
  public static final String STATE_TAG = "state";
  public static final String NAME_ATTR = "name";
  public static final String VALUE_ATTR = "value";

  /** Collection of per-wavelet read states. */
  private final WaveletReadStateCollection<?> read;

  /** Collection of per-wavelet collapsed states. */
  private final WaveletThreadStateCollection<?> collapsed;

  /** Folder state, exposed as a set. */
  private final ObservableBasicSet<Integer> folders;

  /** mute state, exposed as a value. */
  private final ObservableBasicValue<Boolean> muted;

  /** Archived state, per wavelet. */
  private final ObservableMonotonicMap<WaveletId, Integer> waveletArchiveVersions;

  /** Last seen version + hash signature of wavelet **/
  private final ObservableBasicMap<WaveletId, HashedVersion> seenVersion;

  /** Last notified version + hash signature of wavelet **/
  private final ObservableBasicMap<WaveletId, Integer> notifiedVersion;

  /** Notification state */
  private final ObservableBasicValue<Boolean> pendingNotification;

  /** Raw wanted evaluation data. */
  private final ObservableAbuseStore abuseStore;

  /** Gadget states. **/
  private final GadgetStateCollection<?> gadgetStates;

  /**
   * This boolean overrides the waveletArchiveVersions whenever a new archive
   * version is added, this boolean is set to false. When the method
   * {@link #clearArchiveState()} is invoked, this value is set to true. This
   * state is not exposed, instead it is used as a way to override the
   * monotonicity of {@link #waveletArchiveVersions}.
   */
  private final ObservableBasicValue<Boolean> archiveCleared;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /** Forwards read-state and thread-state events. */
  private final Listener forwardingListener = new Listener() {

    @Override
    public void onLastReadBlipVersionChanged(WaveletId wid, String bid, int oldVersion,
        int newVersion) {
      triggerOnLastReadBlipVersionChanged(wid, bid, oldVersion, newVersion);
    }

    @Override
    public void onLastReadParticipantsVersionChanged(
        WaveletId wid, int oldVersion, int newVersion) {
      triggerOnLastReadParticipantsVersionChanged(wid, oldVersion, newVersion);
    }

    @Override
    public void onLastReadTagsVersionChanged(WaveletId wid, int oldVersion, int newVersion) {
      triggerOnLastReadTagsVersionChanged(wid, oldVersion, newVersion);
    }

    @Override
    public void onLastReadWaveletVersionChanged(WaveletId wid, int oldVersion, int newVersion) {
      triggerOnLastReadWaveletVersionChanged(wid, oldVersion, newVersion);
    }

    @Override
    public void onThreadStateChanged(WaveletId waveletId, String threadId,
        ThreadState oldState, ThreadState newState) {
      triggerOnThreadStateChanged(waveletId, threadId, oldState, newState);
    }

    @Override
    public void onFollowed() {
    }

    @Override
    public void onUnfollowed() {
    }

    @Override
    public void onFollowCleared() {
    }

    @Override
    public void onArchiveVersionChanged(WaveletId wid, int oldVersion, int newVersion) {
    }

    @Override
    public void onArchiveClearChanged(boolean oldValue, boolean newValue) {
    }

    @Override
    public void onFolderAdded(int newFolder) {
    }

    @Override
    public void onFolderRemoved(int oldFolder) {
    }

    @Override
    public void onWantedEvaluationsChanged(WaveletId waveletId) {
    }

    @Override
    public void onGadgetStateChanged(
        String gadgetId, String key, String oldValue, String newValue) {
      triggerOnGadgetStateChanged(gadgetId, key, oldValue, newValue);
    }
  };

  /**
   * Creates a supplement.
   *
   * @param userData wavelet to hold the supplement state
   */
  private WaveletBasedSupplement(Wavelet userData) {
    folders = fungeCreateFolders(userData.getDocument(FOLDERS_DOCUMENT));
    muted = fungeCreateMuted(userData.getDocument(MUTED_DOCUMENT));
    waveletArchiveVersions = fungeCreateWaveletArchiveState(
        userData.getDocument(ARCHIVING_DOCUMENT));
    archiveCleared = fungeCreateCleared(userData.getDocument(CLEARED_DOCUMENT));
    ObservableDocument readState = userData.getDocument(READSTATE_DOCUMENT);
    read = fungeCreateReadState(readState, forwardingListener);
    collapsed = fungeCreateCollapsedState(
        userData.getDocument(PRESENTATION_DOCUMENT), forwardingListener);
    abuseStore = fungeCreateAbuseStore(userData.getDocument(ABUSE_DOCUMENT));
    seenVersion = fungeCreateSeenVersion(userData.getDocument(SEEN_DOCUMENT));
    notifiedVersion = fungeCreateNotifiedVersion(userData.getDocument(SEEN_DOCUMENT));
    pendingNotification = fungeCreatePendingNotification(
        userData.getDocument(SEEN_DOCUMENT));
    gadgetStates =
        fungeCreateGadgetStates(userData.getDocument(GADGETS_DOCUMENT), forwardingListener);

    hackCleanup(); // Cleanup before installing listeners, so we start from
                   // clean state.
    installListeners();
  }

  private void hackCleanup() {
    // Explicitly remove Inbox and All folder tokens if they are present.
    folders.remove(1);
    folders.remove(3);
  }

  /**
   * Installs listeners on the component structures, so that this object can
   * broadcast events.
   */
  private void installListeners() {
    muted.addListener(new ObservableBasicValue.Listener<Boolean>() {
      @Override
      public void onValueChanged(Boolean oldValue, Boolean newValue) {
        if (!ValueUtils.equal(oldValue, newValue)) {
          // Notify based only on new value
          if (newValue == null) {
            triggerOnFollowCleared();
          } else if (newValue) {
            triggerOnUnfollowed();
          } else {
            triggerOnFollowed();
          }
        }
      }
    });

    folders.addListener(new ObservableBasicSet.Listener<Integer>() {
      @Override
      public void onValueAdded(Integer newValue) {
        triggerOnFolderAdded(newValue);
      }

      @Override
      public void onValueRemoved(Integer oldValue) {
        triggerOnFolderRemoved(oldValue);
      }
    });

    archiveCleared.addListener(new ObservableBasicValue.Listener<Boolean>() {
      @Override
      public void onValueChanged(Boolean oldValue, Boolean newValue) {
        if (!ValueUtils.equal(oldValue, newValue)) {
          triggerOnArchiveClearChanged(valueOf(oldValue), valueOf(newValue));
        }
      }
    });

    waveletArchiveVersions.addListener(new ObservableMonotonicMap.Listener<WaveletId, Integer>() {
      @Override
      public void onEntrySet(WaveletId waveletId, Integer oldValue, Integer newValue) {
        triggerOnArchiveVersionChanged(waveletId, valueOf(oldValue), valueOf(newValue));
      }
    });

    abuseStore.addListener(new ObservableAbuseStore.Listener() {
      @Override
      public void onEvaluationAdded(WantedEvaluation newEvaluation) {
        triggerOnWantedEvaluationAdded(newEvaluation);
      }

    });
  }

  private static int valueOf(Integer version) {
    return version != null ? version : NO_VERSION;
  }

  private static boolean valueOf(Boolean value) {
    return value != null ? value : false;
  }

  /**
   * Creates a supplement.
   *
   * @param userData wavelet to hold the supplement state
   */
  public static WaveletBasedSupplement create(Wavelet userData) {
    return new WaveletBasedSupplement(userData);
  }

  @Override
  public void setLastReadBlipVersion(WaveletId waveletId, String blipId, int version) {
    read.setLastReadBlipVersion(waveletId, blipId, version);
  }

  @Override
  public void setLastReadParticipantsVersion(WaveletId waveletId, int version) {
    read.setLastReadParticipantsVersion(waveletId, version);
  }

  @Override
  public void setLastReadTagsVersion(WaveletId waveletId, int version) {
    read.setLastReadTagsVersion(waveletId, version);
  }

  @Override
  public void setLastReadWaveletVersion(WaveletId waveletId, int version) {
    read.setLastReadWaveletVersion(waveletId, version);
  }

  @Override
  public void clearReadState() {
    read.clear();
  }

  @Override
  public void clearBlipReadState(WaveletId waveletId, String blipId) {
    read.clearBlipReadState(waveletId, blipId);
  }

  @Override
  public int getLastReadBlipVersion(WaveletId waveletId, String blipId) {
    return read.getLastReadBlipVersion(waveletId, blipId);
  }

  @Override
  public int getLastReadParticipantsVersion(WaveletId waveletId) {
    return read.getLastReadParticipantsVersion(waveletId);
  }

  @Override
  public int getLastReadTagsVersion(WaveletId waveletId) {
    return read.getLastReadTagsVersion(waveletId);
  }

  @Override
  public int getLastReadWaveletVersion(WaveletId waveletId) {
    return read.getLastReadWaveletVersion(waveletId);
  }

  @Override
  public Iterable<String> getReadBlips(WaveletId waveletId) {
    return read.getReadBlips(waveletId);
  }

  @Override
  public Iterable<WaveletId> getReadWavelets() {
    return read.getReadWavelets();
  }

  //
  // Thread State concerns
  //

  @Override
  public ThreadState getThreadState(WaveletId wid, String threadId) {
    return collapsed.getThreadState(wid, threadId);
  }

  @Override
  public void setThreadState(WaveletId wid, String threadId, ThreadState newState) {
    collapsed.setThreadState(wid, threadId, newState);
  }

  @Override
  public Iterable<String> getStatefulThreads(WaveletId waveletId) {
    return collapsed.getStatefulThreads(waveletId);
  }

  @Override
  public Iterable<WaveletId> getWaveletsWithThreadState() {
    return collapsed.getStatefulWavelets();
  }

  //
  // Folder concerns
  //

  @Override
  public void addFolder(int id) {
    folders.add(id);
  }

  @Override
  public void removeAllFolders() {
    folders.clear();
  }

  @Override
  public void removeFolder(int id) {
    folders.remove(id);
  }

  public Iterable<Integer> getFolders() {
    return folders.getValues();
  }

  @Override
  public boolean isInFolder(int id) {
    return folders.contains(id);
  }

  //
  // Inbox concerns
  //

  private boolean isCleared() {
    Boolean value = archiveCleared.get();
    return value != null ? value : false;
  }

  @Override
  public void follow() {
    muted.set(false);
  }

  @Override
  public void unfollow() {
    muted.set(true);
  }

  @Override
  public void clearFollow() {
    muted.set(null);
  }

  @Override
  public Boolean getFollowed() {
    return inverse(muted.get());
  }

  private static Boolean inverse(Boolean b) {
    return b != null ? !b : null;
  }

  @Override
  public int getArchiveWaveletVersion(WaveletId waveletId) {
    if (isCleared()) {
      return NO_VERSION;
    }
    Integer version = waveletArchiveVersions.get(waveletId);
    return version != null ? version : NO_VERSION;
  }

  @Override
  public void archiveAtVersion(WaveletId waveletId, int waveletVersion) {
    waveletArchiveVersions.put(waveletId, waveletVersion);
    if (isCleared()) {
      archiveCleared.set(false);
    }
  }

  @Override
  public void clearArchiveState() {
    // TODO(user)
    // Currently it is not possible to clear a monotonic map, or to remove a
    // document.
    // Instead of actually clearing the archive state, here we set a private
    // flag to true.
    if (!isCleared()) {
      archiveCleared.set(true);
    }
  }

  @Override
  public Set<WaveletId> getSeenWavelets() {
    return seenVersion.keySet();
  }

  @Override
  public Set<WaveletId> getNotifiedWavelets() {
    return notifiedVersion.keySet();
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId waveletId) {
    HashedVersion seenSignature = seenVersion.get(waveletId);

    if (null == seenSignature) {
      return HashedVersion.unsigned(0);
    }
    return seenSignature;
  }

  @Override
  public void setSeenVersion(WaveletId waveletId, HashedVersion signature) {
    seenVersion.put(waveletId, signature);
  }

  @Override
  public void clearSeenVersion(WaveletId waveletId) {
    seenVersion.remove(waveletId);
  }

  @Override
  public Iterable<WaveletId> getArchiveWavelets() {
    return waveletArchiveVersions.keySet();
  }

  //
  // Wanted handling - forward to abuse store.
  //

  @Override
  public Set<WantedEvaluation> getWantedEvaluations() {
    return abuseStore.getWantedEvaluations();
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    abuseStore.addWantedEvaluation(evaluation);
  }

  // Notifications

  @Override
  public boolean getPendingNotification() {
    Boolean pending = pendingNotification.get();
    return pending != null && pending;
  }

  @Override
  public int getNotifiedVersion(WaveletId waveletId) {
    Integer version = notifiedVersion.get(waveletId);
    return version != null ? version : NO_VERSION;
  }

  @Override
  public void setNotifiedVersion(WaveletId waveletId, int signature) {
    notifiedVersion.put(waveletId, signature);
  }

  @Override
  public void clearPendingNotification() {
    pendingNotification.set(null);
  }

  //
  // Gadget states
  //

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return gadgetStates.getGadgetState(gadgetId);
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    gadgetStates.setGadgetState(gadgetId, key, value);
  }

  //
  // Observable aspect
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void triggerOnLastReadBlipVersionChanged(
      WaveletId waveletId, String blipId, int oldVersion, int newVersion) {
    for (Listener listener : listeners) {
      listener.onLastReadBlipVersionChanged(waveletId, blipId, oldVersion, newVersion);
    }
  }

  private void triggerOnLastReadParticipantsVersionChanged(
      WaveletId waveletId, int oldVersion, int newVersion) {
    for (Listener listener : listeners) {
      listener.onLastReadParticipantsVersionChanged(waveletId, oldVersion, newVersion);
    }
  }

  private void triggerOnLastReadTagsVersionChanged(
      WaveletId waveletId, int oldVersion, int newVersion) {
    for (Listener listener : listeners) {
      listener.onLastReadTagsVersionChanged(waveletId, oldVersion, newVersion);
    }
  }

  private void triggerOnLastReadWaveletVersionChanged(
      WaveletId waveletId, int oldVersion, int newVersion) {
    for (Listener listener : listeners) {
      listener.onLastReadWaveletVersionChanged(waveletId, oldVersion, newVersion);
    }
  }

  private void triggerOnFollowed() {
    for (Listener listener : listeners) {
      listener.onFollowed();
    }
  }

  private void triggerOnUnfollowed() {
    for (Listener listener : listeners) {
      listener.onUnfollowed();
    }
  }

  private void triggerOnFollowCleared() {
    for (Listener listener : listeners) {
      listener.onFollowCleared();
    }
  }

  private void triggerOnFolderAdded(int newFolder) {
    for (Listener listener : listeners) {
      listener.onFolderAdded(newFolder);
    }
  }

  private void triggerOnFolderRemoved(int oldFolder) {
    for (Listener listener : listeners) {
      listener.onFolderAdded(oldFolder);
    }
  }

  private void triggerOnArchiveVersionChanged(
      WaveletId waveletId, int oldVersion, int newVersion) {
    for (Listener listener : listeners) {
      listener.onArchiveVersionChanged(waveletId, oldVersion, newVersion);
    }
  }

  private void triggerOnArchiveClearChanged(boolean oldValue, boolean newValue) {
    for (Listener listener : listeners) {
      listener.onArchiveClearChanged(oldValue, newValue);
    }
  }

  private void triggerOnWantedEvaluationAdded(WantedEvaluation newEvaluation) {
    WaveletId waveletId = newEvaluation.getWaveletId();
    if (waveletId == null) {
      return;
    }
    for (Listener listener : listeners) {
      listener.onWantedEvaluationsChanged(waveletId);
    }
  }

  private void triggerOnThreadStateChanged(WaveletId waveletId, String threadId,
      ThreadState oldState, ThreadState newState) {
    for (Listener listener : listeners) {
      listener.onThreadStateChanged(waveletId, threadId, oldState, newState);
    }
  }

  private void triggerOnGadgetStateChanged(
      String gadgetId, String key, String oldValue, String newValue) {
    for (Listener listener : listeners) {
      listener.onGadgetStateChanged(gadgetId, key, oldValue, newValue);
    }
  }

  //
  // Factory methods for component structures.
  //

  /**
   * Exposes a document as a boolean, suitable for holding muted state.
   *
   * @param router router for the muted document
   * @return muted state.
   */
  private static <E> ObservableBasicValue<Boolean> createMuted(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedBoolean.create(router, router.getDocument().getDocumentElement(),
        MUTED_TAG, MUTED_ATTR);
  }

  /**
   * Exposes a document as a boolean, suitable for holding muted state.
   *
   * @param router router for the muted document
   * @return muted state.
   */
  private static <E> ObservableBasicValue<Boolean> createCleared(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedBoolean.create(router, router.getDocument().getDocumentElement(),
        CLEARED_TAG, CLEARED_ATTR);
  }

  /**
   * Exposes a document as an integer set, suitable for holding folder state.
   *
   * @param router router for the folders document
   * @return folder state.
   */
  private static <E> ObservableBasicSet<Integer> createFolders(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedBasicSet.create(router, router.getDocument().getDocumentElement(),
        Serializer.INTEGER, FOLDER_TAG, ID_ATTR);
  }

  /**
   * Exposes a document as a map from wavelet ids to version numbers.
   *
   * @param router router for the archiving document
   * @return archiving state.
   */
  private static <E> ObservableMonotonicMap<WaveletId, Integer> createWaveletArchiveState(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedMonotonicMap.create(router, router.getDocument().getDocumentElement(),
        WaveletIdSerializer.INSTANCE, Serializer.INTEGER, ARCHIVE_TAG, ID_ATTR, VERSION_ATTR);
  }


  /**
   * Exposes a document as a boolean, suitable for holding pending-notification state.
   *
   * @param router router for the notification document
   * @return pending notification state.
   */
  private static <E> ObservableBasicValue<Boolean> createPendingNotification(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedBoolean.create(
        router, router.getDocument().getDocumentElement(), NOTIFICATION_TAG,
        PENDING_NOTIFICATION_ATTR);
  }

  /**
   * Exposes a document as a collection of per-wavelet read-state objects.
   *
   * @param router router for read-state document
   * @return wavelet read-state.
   */
  private static <E> WaveletReadStateCollection<E> createWaveletReadState(
      DocumentEventRouter<? super E, E, ?> router, Listener listener) {
    E container = router.getDocument().getDocumentElement();
    return WaveletReadStateCollection.create(router, container, listener);
  }

  /**
   * Exposes a document as a collection of per-wavelet thread-state objects.
   *
   * @param router router for the thread-state document
   * @return wavelet thread state collection.
   */
  private static <E> WaveletThreadStateCollection<E> createWaveletCollapsedState(
      DocumentEventRouter<? super E, E, ?> router, Listener listener) {
    return WaveletThreadStateCollection.create(router,
        router.getDocument().getDocumentElement(), listener);
  }

  /**
   * Exposes a document as a map of per-wavelet seen-version/seen-hash pairs.
   *
   * @param router router for the seen-state document
   * @return wavelet seen version and hash signature.
   */
  private static <E> ObservableBasicMap<WaveletId, HashedVersion> createWaveletSeenVersion(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedMonotonicMap.create(router, router.getDocument().getDocumentElement(),
        WaveletIdSerializer.INSTANCE, HashedVersionSerializer.INSTANCE, SEEN_VERSION_TAG,
        ID_ATTR, SIGNATURE_ATTR);
  }

  /**
   * Exposes a document as a map of per-wavelet notified-versions.
   *
   * @param router router for the notified-state document
   * @return wavelet notified version.
   */
  private static <E> ObservableBasicMap<WaveletId, Integer> createWaveletNotifiedVersion(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedMonotonicMap.create(router, router.getDocument().getDocumentElement(),
        WaveletIdSerializer.INSTANCE, Serializer.INTEGER, NOTIFIED_VERSION_TAG, ID_ATTR,
        VERSION_ATTR);
  }

  /**
   * Exposes a document as a map of maps of gadget states.
   *
   * @param router router for the gadget state document.
   * @param listener event listener.
   * @return gadget state collection object to access gadgets states by gadget
   *         ID and state name.
   */
  private static <E> GadgetStateCollection<E> createGadgetStatesDoc(
      DocumentEventRouter<? super E, E, ?> router, Listener listener) {
    return GadgetStateCollection.create(router,
        router.getDocument().getDocumentElement(), listener);
  }

  /**
   * Exposes a document as a set of {@link WantedEvaluation}s.
   *
   * @param router abuse document
   * @return all wanted evaluations for the wave.
   */
  private static <E> ObservableAbuseStore createAbuseStore(
      DocumentEventRouter<? super E, E, ?> router) {
    return DocumentBasedAbuseStore.create(router);
  }

  //
  // Funge methods for unfurling generics, required only for Sun JDK compiler.
  //

  private static <N> ObservableBasicSet<Integer> fungeCreateFolders(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createFolders(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableBasicValue<Boolean> fungeCreateMuted(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createMuted(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableBasicValue<Boolean> fungeCreateCleared(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createCleared(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableMonotonicMap<WaveletId, Integer> fungeCreateWaveletArchiveState(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createWaveletArchiveState(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableBasicValue<Boolean> fungeCreatePendingNotification(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createPendingNotification(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> WaveletReadStateCollection<?> fungeCreateReadState(
      ObservableMutableDocument<N, ?, ?> doc, Listener listener) {
    return createWaveletReadState(DefaultDocumentEventRouter.create(doc), listener);
  }

  private static <N> WaveletThreadStateCollection<?> fungeCreateCollapsedState(
      ObservableMutableDocument<N, ?, ?> doc, Listener listener) {
    return createWaveletCollapsedState(DefaultDocumentEventRouter.create(doc), listener);
  }

  private static <N> ObservableBasicMap<WaveletId, HashedVersion> fungeCreateSeenVersion(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createWaveletSeenVersion(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableBasicMap<WaveletId, Integer> fungeCreateNotifiedVersion(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createWaveletNotifiedVersion(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> ObservableAbuseStore fungeCreateAbuseStore(
      ObservableMutableDocument<N, ?, ?> doc) {
    return createAbuseStore(DefaultDocumentEventRouter.create(doc));
  }

  private static <N> GadgetStateCollection<?> fungeCreateGadgetStates(
      ObservableMutableDocument<N, ?, ?> doc, Listener listener) {
    return createGadgetStatesDoc(DefaultDocumentEventRouter.create(doc), listener);
  }

}
