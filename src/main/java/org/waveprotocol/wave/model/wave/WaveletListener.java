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

package org.waveprotocol.wave.model.wave;


import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * A Listener that hears events from the wavelet.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface WaveletListener {
  /**
   * Notifies this listener that a participant has been added.
   *
   * @param wavelet the wavelet that's changed
   * @param participant  participant that was added
   */
  void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant);

  /**
   * Notifies this listener that a participant has been removed.
   *
   * @param wavelet the wavelet that's changed
   * @param participant  participant that was removed
   */
  void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant);

  /**
   * Called whenever the last modified time for the wavelet is changed.
   */
  void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime);

  /**
   * Notifies this listener that a new blip has been added to the wavelet.
   *
   * @param wavelet the wavelet that changed
   * @param blip  blip that was added
   */
  void onBlipAdded(ObservableWavelet wavelet, Blip blip);

  /**
   * Notifies this listener that a blip has been removed from the wavelet.
   *
   * @param wavelet the wavelet that changed
   * @param blip  blip that was removed
   */
  void onBlipRemoved(ObservableWavelet wavelet, Blip blip);

  /**
   * Notifies this listener that a blip has been submitted.
   *
   * @param wavelet the wavelet that changed
   * @param blip  blip that was submitted
   */
  void onBlipSubmitted(ObservableWavelet wavelet, Blip blip);

  /**
   * Notifies this listener that a blip's timestamp has changed.
   *
   * @param wavelet the wavelet that changed
   * @param blip     modified blip
   * @param oldTime  old timestamp
   * @param newTime  new timestamp
   */
  void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime, long newTime);

  /**
   * Notifies this listener that a blip's version has changed.
   *
   * @param wavelet the wavelet that changed
   * @param blip     modified blip
   * @param oldVersion  old version
   * @param newVersion  new version
   */
  void onBlipVersionModified(ObservableWavelet wavelet,
      Blip blip, Long oldVersion, Long newVersion);

  /**
   * Notifies this listener that a contributor has been added to a blip.
   *
   * @param wavelet the wavelet that changed
   * @param blip         blip that was modified
   * @param contributor  contributor that was added
   */
  void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip, ParticipantId contributor);

  /**
   * Notifies this listener that a contributor has been removed from a blip.
   *
   * @param wavelet the wavelet that changed
   * @param blip         blip that was modified
   * @param contributor  contributor that was removed
   */
  void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip, ParticipantId contributor);

  /**
   * Notifies this listener that the wavelet version has changed.
   *
   * @param wavelet the wavelet that changed
   * @param oldVersion  old version
   * @param newVersion  new version
   */
  void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion);

  /**
   * Notifies this listener that the wavelet hashed version has changed.
   *
   * @param wavelet the wavelet that changed
   * @param oldHashedVersion  old version
   * @param newHashedVersion  new version
   */
  void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion);

  /**
   * Notifies this listener that the content of the blip was modified.
   * HACK(zdwang, hearnden): We don't really need to care that a modification is remote
   * if the editor can guarantee that it does not generate any ops after submit.
   *
   * @param wavelet the wavelet that changed
   * @param blip         blip that was modified
   *
   * @deprecated do not use this method, it will be removed, with no equivalent
   *             to replace it
   */
  @Deprecated
  void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip);
}
