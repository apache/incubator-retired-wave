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

import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.wave.Wavelet;

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Exposes supplementary events on a wave.
 *
 */
public interface ObservableSupplementedWave extends SupplementedWave,
    SourcesEvents<ObservableSupplementedWave.Listener> {

  /**
   * Receiver of supplement events.
   */
  public interface Listener {

    /**
     * Notifies this listener that the thread state of a conversation thread
     * may have changed.
     *
     * @param thread  the thread that has changed
     */
    void onThreadStateChanged(ObservableConversationThread thread);

    /**
     * Notifies this listener that the read state of a blip may have changed.
     *
     * @param blip changed blip
     */
    void onMaybeBlipReadChanged(ObservableConversationBlip blip);

    /**
     * Notifies this listener that the read state of a wavelet's participant
     * collection may have changed.
     *
     * @param wavelet wavelet whose participants collection is affected
     */
    void onMaybeParticipantsReadChanged(Wavelet wavelet);

    /**
     * Notifies this listener that the read state of a wavelet's tags
     * document may have changed.
     *
     * @param wavelet  wavelet whose tags document is affected
     */
    void onMaybeTagsReadChanged(Wavelet wavelet);

    /**
     * Notifies this listener that the inbox state of this wave may have
     * changed.
     */
    void onMaybeInboxStateChanged();

    /**
     * Notifies this listener that the follow state of this wave may have
     * changed.
     */
    void onMaybeFollowStateChanged();

    /**
     * Notifies this listener that some part of this wavelet's read state may
     * have changed.
     */
    void onMaybeWaveletReadChanged();

    /**
     * Notifies this listener that this wave has been added to a folder.
     *
     * @param newFolder new folder
     */
    void onFolderAdded(int newFolder);

    /**
     * Notifies this listener that this wave has been removed from a folder.
     *
     * @param oldFolder old folder
     */
    void onFolderRemoved(int oldFolder);

    /**
     * Notifies this listener that {@link WantedEvaluation}s have changed for a
     * contained wavelet.
     *
     * @param waveletId Identifies the wavelet for that has a new
     *        WantedEvaluation.
     */
    void onWantedEvaluationsChanged(WaveletId waveletId);

    /**
     * Notifies this listener that the gadget state may have changed.
     *
     * @param gadgetId changed gadget
     */
    void onMaybeGadgetStateChanged(String gadgetId);
  }

  /**
   * Vacuous implementation of a listener, intended for extension.
   */
  public static class ListenerImpl implements Listener {
    @Override
    public void onThreadStateChanged(ObservableConversationThread thread) {
    }

    @Override
    public void onFolderAdded(int newFolder) {
    }

    @Override
    public void onFolderRemoved(int oldFolder) {
    }

    @Override
    public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
    }

    @Override
    public void onMaybeInboxStateChanged() {
    }

    @Override
    public void onMaybeFollowStateChanged() {
    }

    @Override
    public void onMaybeParticipantsReadChanged(Wavelet wavelet) {
    }

    @Override
    public void onMaybeTagsReadChanged(Wavelet wavelet) {
    }

    @Override
    public void onMaybeWaveletReadChanged() {
    }

    @Override
    public void onWantedEvaluationsChanged(WaveletId waveletId) {
    }

    @Override
    public void onMaybeGadgetStateChanged(String gadgetId) {
    }
  }
}
