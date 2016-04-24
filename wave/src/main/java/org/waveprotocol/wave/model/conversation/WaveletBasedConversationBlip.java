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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.conversation.WaveletBasedConversation.ComponentHelper;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocument.Action;
import org.waveprotocol.wave.model.document.util.DocIterate;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.opbased.WaveletListenerImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A conversation blip backed by a region of a wavelet's manifest document.
 *
 * NOTE(anorth): at present this in in fact backed by a {@link Blip} but we are
 * migrating all blip meta-data into documents.
 *
 * @author anorth@google.com (Alex North)
 */
final class WaveletBasedConversationBlip implements ObservableConversationBlip,
    SourcesEvents<WaveletBasedConversationBlip.Listener>, ObservableManifestBlip.Listener {

  /**
   * Receives events on a conversation blip.
   */
  interface Listener {
    /**
     * Notifies this listener that a reply thread has been added to this blip.
     *
     * @param reply the new thread
     */
    void onReplyAdded(WaveletBasedConversationThread reply);

    /**
     * Notifies this listener that a reply thread has been added to this blip.
     *
     * @param reply the new thread
     * @param location the location at which the thread is anchored
     */
    void onInlineReplyAdded(WaveletBasedConversationThread reply, int location);

    /**
     * Notifies this listener that the blip was removed from the conversation.
     * No further methods may be called on the blip.
     */
    void onDeleted();

    /**
     * Notifies this listener that a contributor was added to the blip.
     */
    void onContributorAdded(ParticipantId contributor);

    /**
     * Notifies this listener that a contributor was removed from the blip.
     */
    void onContributorRemoved(ParticipantId contributor);

    /**
     * Notifies the listener that the blip was submitted.
     */
    void onSumbitted();

    /**
     * Notifies the listener that the blip timestamp changed.
     */
    void onTimestampChanged(long oldTimestamp, long newTimestamp);
  }


  /**
   * A located reply thread  which specializes the thread type to
   * WaveletBasedConversationThread.
   */
  final class LocatedReplyThread
      extends ConversationBlip.LocatedReplyThread<WaveletBasedConversationThread>
      implements Comparable<LocatedReplyThread> {

    LocatedReplyThread(WaveletBasedConversationThread thread, int location) {
      super(thread, location);
    }

    @Override
    public int compareTo(WaveletBasedConversationBlip.LocatedReplyThread o) {
      if (getLocation() == o.getLocation()) {
        return 0;
      } else if (getLocation() == Blips.INVALID_INLINE_LOCATION) {
        return 1;
      } else if (o.getLocation() == Blips.INVALID_INLINE_LOCATION) {
        return -1;
      }
      return getLocation() - o.getLocation();
    }
  }

  /**
   * Redirects old-style blip events to the conversation listeners.
   */
  private final WaveletListener waveletListener = new WaveletListenerImpl() {
    @Override
    public void onBlipContributorAdded(ObservableWavelet wavelet, Blip eventBlip,
        ParticipantId contributor) {
      if (eventBlip == blip) {
        triggerOnContributorAdded(contributor);
      }
    }

    @Override
    public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip eventBlip,
        ParticipantId contributor) {
      if (eventBlip == blip) {
        triggerOnContributorRemoved(contributor);
      }
    }

    @Override
    public void onBlipSubmitted(ObservableWavelet wavelet, Blip eventBlip) {
      if (eventBlip == blip) {
        triggerOnSubmitted();
      }
    }

    @Override
    public void onBlipTimestampModified(ObservableWavelet wavelet, Blip eventBlip, long oldTime,
        long newTime) {
      if (eventBlip == blip) {
        triggerOnTimestampModified(oldTime, newTime);
      }
    }
  };

  /** Manifest entry for this blip. */
  private final ObservableManifestBlip manifestBlip;

  /** Blip object containing content and metadata. */
  private final Blip blip;

  /** Thread containing this blip. */
  private final WaveletBasedConversationThread parentThread;

  /** Helper for wavelet access. */
  private final ComponentHelper helper;

  /** Replies keyed by id. */
  private final StringMap<WaveletBasedConversationThread> replies = CollectionUtils.createStringMap();

  /** Whether this blip is safe to access. Set false when deleted. */
  private boolean isUsable = true;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  static WaveletBasedConversationBlip create(ObservableManifestBlip manifestBlip, Blip backingBlip,
      WaveletBasedConversationThread thread, ComponentHelper helper) {
    WaveletBasedConversationBlip blip = new WaveletBasedConversationBlip(manifestBlip, backingBlip,
        thread, helper);

    for (ObservableManifestThread reply : manifestBlip.getReplies()) {
      blip.adaptThread(reply);
    }

    manifestBlip.addListener(blip);
    helper.getWaveletEventSource().addListener(blip.waveletListener);
    return blip;
  }

  private WaveletBasedConversationBlip(ObservableManifestBlip manifestBlip, Blip blip,
      WaveletBasedConversationThread thread, ComponentHelper helper) {
    Preconditions.checkNotNull(manifestBlip,
        "WaveletBasedConversationBlip received null manifest blip");
    if (blip == null) {
      Preconditions.nullPointer("WaveletBasedConversationBlip " + manifestBlip.getId()
          + " received null blip");
    }
    this.manifestBlip = manifestBlip;
    this.blip = blip;
    this.helper = helper;
    this.parentThread = thread;
  }

  @Override
  public WaveletBasedConversation getConversation() {
    return helper.getConversation();
  }

  @Override
  public WaveletBasedConversationThread getThread() {
    return parentThread;
  }

 

  @Override
  public Iterable<LocatedReplyThread> locateReplyThreads() {
    // NOTE(anorth): We must recalculate the anchor locations on each
    // call as the document does not provide stable elements. However, we
    // calculate the list of anchor locations on demand.
    Map<String, Integer> replyLocations = null;
    List<LocatedReplyThread> inlineReplyThreads = CollectionUtils.newArrayList();
    for (WaveletBasedConversationThread reply : getReplyThreads()) {
      if (replyLocations == null) {
        replyLocations = findAnchors();
      }
      Integer location = replyLocations.get(reply.getId());
      inlineReplyThreads.add(new LocatedReplyThread(reply,
          (location != null) ? location : Blips.INVALID_INLINE_LOCATION));
    }
    Collections.sort(inlineReplyThreads);
    return Collections.unmodifiableList(inlineReplyThreads);
  }

  /**
   * {@inheritDoc}
   *
   * The 'history of appends' corresponds to the manifest order of replies.
   */
  @Override
  public Iterable<WaveletBasedConversationThread> getReplyThreads() {
    final Iterable<? extends ObservableManifestThread> manifestBlips = manifestBlip.getReplies();
    return new Iterable<WaveletBasedConversationThread>() {
      @Override
      public Iterator<WaveletBasedConversationThread> iterator() {
        return WrapperIterator.create(manifestBlips.iterator(), replies);
      }
    };
  }

  @Override
  public WaveletBasedConversationThread addReplyThread() {
    checkIsUsable();
    String id = helper.createThreadId();
    manifestBlip.appendReply(id, false);
    return replies.get(id);
  }

  @Override
  public WaveletBasedConversationThread addReplyThread(final int location) {
    checkIsUsable();
    final String threadId = helper.createThreadId();
    createInlineReplyAnchor(threadId, location);
    manifestBlip.appendReply(threadId, true);
    return replies.get(threadId);
  }

  @Override
  public Document getContent() {
    return blip.getContent();
  }

  // TODO(anorth): migrate blip metadata to data stored in the blip document.

  @Override
  public ParticipantId getAuthorId() {
    return blip.getAuthorId();
  }

  @Override
  public Set<ParticipantId> getContributorIds() {
    return blip.getContributorIds();
  }

  @Override
  public long getLastModifiedTime() {
    return blip.getLastModifiedTime();
  }

  @Override
  public long getLastModifiedVersion() {
    return blip.getLastModifiedVersion();
  }

  @Override
  public void delete() {
    checkIsUsable();
    Collection<WaveletBasedConversationThread> allReplies = CollectionUtils.createQueue();
    CollectionUtils.copyValuesToJavaCollection(replies, allReplies);
    // Delete reply threads.
    // TODO(anorth): Move this loop to WBCT, where it can delete all the
    // inline reply anchors in one pass.
    for (WaveletBasedConversationThread replyThread : allReplies) {
      deleteThread(replyThread);
    }

    // All replies have been deleted, so remove this empty blip.
    parentThread.deleteBlip(this, true);
  }

  @Override
  public String getId() {
    return manifestBlip.getId();
  }

  @Override
  public boolean isRoot() {
    return parentThread == getConversation().getRootThread() &&
        this == parentThread.getFirstBlip();
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  //
  // ObservableManifestBlip.Listener
  // These methods update local data structures in response to changes in
  // the underlying data, either synchronously in local methods or from
  // remote changes. They don't make further changes to the data.
  //

  @Override
  public void onReplyAdded(ObservableManifestThread thread) {
    WaveletBasedConversationThread convThread = adaptThread(thread);
    triggerOnReplyAdded(convThread);
  }

  @Override
  public void onReplyRemoved(final ObservableManifestThread thread) {
    forgetThread(replies.get(thread.getId()));
  }

  @Override
  public String toString() {
    return "WaveletBasedConversationBlip(id = " + manifestBlip.getId() + ")";
  }

  public Blip getBlip() {
    return blip;
  }

  // TODO(anorth): remove this after porting client to conversation model.
  @Override
  public Blip hackGetRaw() {
    return getBlip();
  }

  /**
   * Deletes the content of this blip's document.
   */
  void clearContent() {
    if (blip.getContent().size() != 0) {
      blip.getContent().emptyElement(blip.getContent().getDocumentElement());
    }
  }

  @Override
  public ObservableConversationThread getReplyThread(String id) {
    return replies.get(id);
  }

  ManifestBlip getManifestBlip() {
    return manifestBlip;
  }

  // Package-private methods for WaveletBasedConversationThread.

  /**
   * Deletes a thread from this blip, deleting that thread's blips.
   */
  void deleteThread(WaveletBasedConversationThread threadToDelete) {
    threadToDelete.deleteBlips();
    manifestBlip.removeReply(threadToDelete.getManifestThread());
    clearInlineReplyAnchor(threadToDelete.getId());
  }

  /**
   * Deletes all threads from this blip.
   *
   * @see WaveletBasedConversationBlip#deleteThread(WaveletBasedConversationThread)
   */
  void deleteThreads() {
    // deleteThread() equivalent is inline here so we can do only one
    // document traversal to remove inline reply anchors.
    List<WaveletBasedConversationThread> threads =
        CollectionUtils.newArrayList(getReplyThreads());
    for (WaveletBasedConversationThread threadToDelete : threads) {
      threadToDelete.deleteBlips();
      manifestBlip.removeReply(threadToDelete.getManifestThread());
    }
    clearAllInlineReplyAnchors();
  }

  /**
   * Invalidates this blip. It may no longer be accessed.
   */
  void invalidate() {
    checkIsUsable();
    manifestBlip.removeListener(this);
    isUsable = false;
  }

  /**
   * Recursively invalidates this blip and its replies.
   */
  void destroy() {
    for (WaveletBasedConversationThread thread : CollectionUtils.valueList(replies)) {
      thread.destroy();
    }
    invalidate();
    listeners.clear();
  }

  /**
   * Checks that this blip is safe to access.
   */
  @VisibleForTesting
  void checkIsUsable() {
    if (!isUsable) {
      Preconditions.illegalState("Deleted blip is not usable: " + this);
    }
  }

  /**
   * Creates a conversation thread backed by a manifest thread and inserts it in
   * {@code replies}.
   */
  private WaveletBasedConversationThread adaptThread(ObservableManifestThread manifestThread) {
    WaveletBasedConversationThread thread =
        WaveletBasedConversationThread.create(manifestThread, this, helper);
    String id = thread.getId();
    replies.put(id, thread);
    return thread;
  }

  /**
   * Removes a thread from the internal list and triggers its deletion event.
   */
  private void forgetThread(WaveletBasedConversationThread threadToRemove) {
    String id = threadToRemove.getId();
    assert replies.containsKey(id);
    replies.remove(id);
    threadToRemove.triggerOnDeleted();
  }

  /**
   * Finds all thread anchor elements in the blip document.
   *
   * @return thread ids and their anchor locations
   */
  private Map<String, Integer> findAnchors() {
    final Map<String, Integer> anchors = CollectionUtils.newHashMap();
    blip.getContent().with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        for (E el : DocIterate.deepElements(doc, doc.getDocumentElement(), null)) {
          if (Blips.THREAD_INLINE_ANCHOR_TAGNAME.equals(doc.getTagName(el))) {
            String threadId = doc.getAttribute(el,
                Blips.THREAD_INLINE_ANCHOR_ID_ATTR);
            if ((threadId != null) && !anchors.containsKey(threadId)) {
              anchors.put(threadId, doc.getLocation(el));
            }
          }
        }
      }
    });
    return anchors;
  }

  /**
   * Inserts an inline reply anchor element in the blip document.
   *
   * @param threadId id of the reply thread
   * @param location location at which to insert anchor
   */
  private void createInlineReplyAnchor(final String threadId, final int location) {
    blip.getContent().with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        Point<N> point = doc.locate(location);
        doc.createElement(point, Blips.THREAD_INLINE_ANCHOR_TAGNAME,
            Collections.singletonMap(Blips.THREAD_INLINE_ANCHOR_ID_ATTR, threadId));
      }
    });
  }

  /**
   * Deletes all inline reply anchor elements for a thread from the blip
   * document.
   *
   * @param threadId id of the anchor(s) to delete
   */
  private void clearInlineReplyAnchor(final String threadId) {
    clearInlineReplyAnchors(CollectionUtils.immutableSet(threadId));
  }

  /**
   * Deletes all inline reply anchor elements for a set of threads from the blip
   * document.
   *
   * @param threadIds ids of the anchors to delete
   */
  private void clearInlineReplyAnchors(final Set<String> threadIds) {
    blip.getContent().with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        List<E> elementsToDelete = CollectionUtils.newArrayList();
        for (E el : DocIterate.deepElements(doc, doc.getDocumentElement(), null)) {
          if (Blips.THREAD_INLINE_ANCHOR_TAGNAME.equals(doc.getTagName(el))) {
            String elId = doc.getAttribute(el, Blips.THREAD_INLINE_ANCHOR_ID_ATTR);
            if (threadIds.contains(elId)) {
              elementsToDelete.add(el);
            }
          }
        }
        // Reverse elements to delete so we always delete bottom up if one
        // contains another (which would be really weird anyway).
        Collections.reverse(elementsToDelete);
        for (E el : elementsToDelete) {
          doc.deleteNode(el);
        }
      }
    });
  }

  /**
   * Deletes all inline reply anchor elements from the blip document.
   */
  private void clearAllInlineReplyAnchors() {
    blip.getContent().with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        List<E> elementsToDelete = CollectionUtils.newArrayList();
        for (E el : DocIterate.deepElements(doc, doc.getDocumentElement(), null)) {
          if (Blips.THREAD_INLINE_ANCHOR_TAGNAME.equals(doc.getTagName(el))) {
            elementsToDelete.add(el);
          }
        }
        // Reverse elements to delete so we always delete bottom up if one
        // contains another (which would be really weird anyway).
        Collections.reverse(elementsToDelete);
        for (E el : elementsToDelete) {
          doc.deleteNode(el);
        }
      }
    });
  }

  private void triggerOnReplyAdded(WaveletBasedConversationThread reply) {
    for (Listener l : listeners) {
      l.onReplyAdded(reply);
    }
  }

  // Package-private for access from WaveletBasedConversationThread.
  void triggerOnDeleted() {
    helper.getWaveletEventSource().removeListener(waveletListener);
    invalidate();
    for (Listener l : listeners) {
      l.onDeleted();
    }
  }

  private void triggerOnContributorAdded(ParticipantId contributor) {
    for (Listener l : listeners) {
      l.onContributorAdded(contributor);
    }
  }

  private void triggerOnContributorRemoved(ParticipantId contributor) {
    for (Listener l : listeners) {
      l.onContributorRemoved(contributor);
    }
  }

  private void triggerOnSubmitted() {
    for (Listener l : listeners) {
      l.onSumbitted();
    }
  }

  private void triggerOnTimestampModified(long oldTimestamp, long newTimestamp) {
    for (Listener l : listeners) {
      l.onTimestampChanged(oldTimestamp, newTimestamp);
    }
  }
}
