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

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.Set;

/**
 * A wrapper around primitive supplement to allow for dummy POJO versions
 * to sit in place of the presentation model when the usePresentationModel
 * flag is disabled and to suppress all private gadget state requests when
 * the usePrivateGadgetStates flag is disabled.
 *
 */
public final class PartitioningPrimitiveSupplement implements ObservablePrimitiveSupplement {

  private final ObservablePrimitiveSupplement realPrimitive;
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /**
   * The primitive supplement to use for presentation actions. When the
   * usePresentationModel client flag is disabled, this is reset every
   * time stopPartitioning is called, otherwise it just refers to the
   * same object as realPrimitive. Non-null.
   */
  private ObservablePrimitiveSupplement presentationPrimitive;

  private final boolean usePrivateGadgetStates;

  public PartitioningPrimitiveSupplement (ObservablePrimitiveSupplement realPrimitive,
      boolean usePresentationModel, boolean usePrivateGadgetStates) {
    this.realPrimitive = realPrimitive;
    this.usePrivateGadgetStates = usePrivateGadgetStates;
    presentationPrimitive = !usePresentationModel ? new PrimitiveSupplementImpl() : realPrimitive;
  }

  @Override
  public void addFolder(int id) {
    realPrimitive.addFolder(id);
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    realPrimitive.addWantedEvaluation(evaluation);
  }

  @Override
  public void archiveAtVersion(WaveletId waveletId, int waveletVersion) {
     realPrimitive.archiveAtVersion(waveletId, waveletVersion);
  }

  @Override
  public void clearArchiveState() {
     realPrimitive.clearArchiveState();
  }

  @Override
  public void clearBlipReadState(WaveletId waveletId, String blipId) {
    realPrimitive.clearBlipReadState(waveletId, blipId);
  }

  @Override
  public void clearFollow() {
    realPrimitive.clearFollow();
  }

  @Override
  public void clearPendingNotification() {
    realPrimitive.clearPendingNotification();
  }

  @Override
  public void clearReadState() {
    realPrimitive.clearReadState();
  }

  @Override
  public void follow() {
    realPrimitive.follow();
  }

  @Override
  public int getArchiveWaveletVersion(WaveletId waveletId) {
    return realPrimitive.getArchiveWaveletVersion(waveletId);
  }

  @Override
  public Iterable<WaveletId> getArchiveWavelets() {
    return realPrimitive.getArchiveWavelets();
  }

  @Override
  public Iterable<Integer> getFolders() {
    return realPrimitive.getFolders();
  }

  @Override
  public Boolean getFollowed() {
    return realPrimitive.getFollowed();
  }

  @Override
  public int getLastReadBlipVersion(WaveletId waveletId, String blipId) {
    return realPrimitive.getLastReadBlipVersion(waveletId, blipId);
  }

  @Override
  public int getLastReadParticipantsVersion(WaveletId waveletId) {
    return realPrimitive.getLastReadParticipantsVersion(waveletId);
  }

  @Override
  public int getLastReadTagsVersion(WaveletId waveletId) {
    return realPrimitive.getLastReadTagsVersion(waveletId);
  }

  @Override
  public int getLastReadWaveletVersion(WaveletId waveletId) {
    return realPrimitive.getLastReadWaveletVersion(waveletId);
  }

  @Override
  public boolean getPendingNotification() {
    return realPrimitive.getPendingNotification();
  }

  @Override
  public Iterable<String> getReadBlips(WaveletId waveletId) {
    return realPrimitive.getReadBlips(waveletId);
  }

  @Override
  public Iterable<WaveletId> getReadWavelets() {
    return realPrimitive.getReadWavelets();
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId waveletId) {
    return realPrimitive.getSeenVersion(waveletId);
  }

  @Override
  public int getNotifiedVersion(WaveletId waveletId) {
    return realPrimitive.getNotifiedVersion(waveletId);
  }

  @Override
  public Set<WaveletId> getSeenWavelets() {
    return realPrimitive.getSeenWavelets();
  }

  @Override
  public Set<WaveletId> getNotifiedWavelets() {
    return realPrimitive.getNotifiedWavelets();
  }

  @Override
  public Iterable<String> getStatefulThreads(WaveletId waveletId) {
    return presentationPrimitive.getStatefulThreads(waveletId);
  }

  @Override
  public ThreadState getThreadState(WaveletId waveletId, String threadId) {
    return presentationPrimitive.getThreadState(waveletId, threadId);
  }

  @Override
  public Set<WantedEvaluation> getWantedEvaluations() {
    return realPrimitive.getWantedEvaluations();
  }

  @Override
  public Iterable<WaveletId> getWaveletsWithThreadState() {
    return presentationPrimitive.getWaveletsWithThreadState();
  }

  @Override
  public boolean isInFolder(int id) {
    return realPrimitive.isInFolder(id);
  }

  @Override
  public void removeAllFolders() {
    realPrimitive.removeAllFolders();
  }

  @Override
  public void removeFolder(int id) {
    realPrimitive.removeFolder(id);
  }

  @Override
  public void setLastReadBlipVersion(WaveletId waveletId, String blipId, int version) {
    realPrimitive.setLastReadBlipVersion(waveletId, blipId, version);
  }

  @Override
  public void setLastReadParticipantsVersion(WaveletId waveletId, int version) {
    realPrimitive.setLastReadParticipantsVersion(waveletId, version);
  }

  @Override
  public void setLastReadTagsVersion(WaveletId waveletId, int version) {
    realPrimitive.setLastReadTagsVersion(waveletId, version);
  }

  @Override
  public void setLastReadWaveletVersion(WaveletId waveletId, int version) {
    realPrimitive.setLastReadWaveletVersion(waveletId, version);
  }

  @Override
  public void setNotifiedVersion(WaveletId waveletId, int version) {
    realPrimitive.setNotifiedVersion(waveletId, version);
  }

  @Override
  public void setSeenVersion(WaveletId waveletId, HashedVersion signature) {
    realPrimitive.setSeenVersion(waveletId, signature);
  }

  @Override
  public void clearSeenVersion(WaveletId waveletId) {
    realPrimitive.clearSeenVersion(waveletId);
  }

  @Override
  public void setThreadState(WaveletId waveletId, String threadId, ThreadState newState) {
    presentationPrimitive.setThreadState(waveletId, threadId, newState);
  }

  @Override
  public void unfollow() {
    realPrimitive.unfollow();
  }

  @Override
  public void addListener(Listener listener) {
    realPrimitive.addListener(listener);
    listeners.add(listener);
    if (realPrimitive != presentationPrimitive) {
      presentationPrimitive.addListener(listener);
    }
  }

  @Override
  public void removeListener(Listener listener) {
    realPrimitive.removeListener(listener);
    listeners.remove(listener);
    if (realPrimitive != presentationPrimitive) {
      presentationPrimitive.removeListener(listener);
    }
  }

  /**
   * Begins use of POJO versions, if required by client flags.
   */
  public void startPartitioning() {
    // don't need to do anything
  }

  /**
   * Resets POJO supplement, if the client flag is disabled.
   */
  public void stopPartitioning() {
    if (presentationPrimitive != realPrimitive) {
      // If we're filtering presentation state into a pojo, then blast it away
      // and create a new one.
      presentationPrimitive = new PrimitiveSupplementImpl();
      for (Listener listener : listeners) {
        presentationPrimitive.addListener(listener);
      }
    }
  }

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return usePrivateGadgetStates ?
        realPrimitive.getGadgetState(gadgetId) : CollectionUtils.<String> emptyMap();
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    if (usePrivateGadgetStates) {
      realPrimitive.setGadgetState(gadgetId, key, value);
    }
  }
}
