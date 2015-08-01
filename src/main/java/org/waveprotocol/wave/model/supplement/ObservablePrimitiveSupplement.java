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
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 */
public interface ObservablePrimitiveSupplement extends PrimitiveSupplement,
    SourcesEvents<ObservablePrimitiveSupplement.Listener> {

  public interface Listener {
    /**
     * Notifies this listener that the last-read version of a blip has changed.
     */
    void onLastReadBlipVersionChanged(WaveletId wid, String bid, int oldVersion, int newVersion);

    /**
     * Notifies this listener that the minimum last-read version of all wave
     * parts has changed.
     */
    void onLastReadWaveletVersionChanged(WaveletId wid, int oldVersion, int newVersion);

    /**
     * Notifies this listener that the last-read version of the
     * participants-collection has changed.
     */
    void onLastReadParticipantsVersionChanged(WaveletId wid, int oldVersion, int newVersion);

    /**
     * Notifies this listener that the last-read version of the tags has
     * changed.
     */
    void onLastReadTagsVersionChanged(WaveletId wid, int oldVersion, int newVersion);

    /**
     * Notifies this listener that the followed state has been set to true.
     */
    void onFollowed();

    /**
     * Notifies this listener that the followed state has been set to false.
     */
    void onUnfollowed();

    /**
     * Notifies this listener that the followed state has been cleared.
     */
    void onFollowCleared();

    /**
     * Notifies this listener that last-archived version of a wavelet has
     * changed.
     */
    void onArchiveVersionChanged(WaveletId wid, int oldVersion, int newVersion);

    /**
     * Notifies this listener that archive value has been set.
     */
    // TODO(hearnden/fabio) remove the 'cleared' field from the primitive model
    void onArchiveClearChanged(boolean oldValue, boolean newValue);

    /**
     * Notifies this listener that a folder id has been added.
     */
    void onFolderAdded(int newFolder);

    /**
     * Notifies this listener that a folder id has been removed.
     */
    void onFolderRemoved(int oldFolder);

    /**
     * Notifies this listener that the wanted-evaluations of a wavelet has
     * changed.
     */
    void onWantedEvaluationsChanged(WaveletId wid);

    /**
     * Notifies this listener that a thread's state has been changed
     * ThreadState values shall never be null.
     */
    void onThreadStateChanged(WaveletId wid, String tid,
        ThreadState oldState, ThreadState newState);

    /**
     * Notifies this listener that gadget state has been changed.
     */
    void onGadgetStateChanged(String gadgetId, String key, String oldValue, String newValue);

  }
}
