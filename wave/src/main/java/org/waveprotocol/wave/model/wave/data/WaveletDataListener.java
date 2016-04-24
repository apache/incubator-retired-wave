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

package org.waveprotocol.wave.model.wave.data;


import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A Listener that hears events from the wavelet.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface WaveletDataListener {
  /**
   * Notifies this listener that a participant has been added.
   *
   * @param waveletData the wavelet data which the participant has been added to
   * @param participant  participant that was added
   */
  void onParticipantAdded(WaveletData waveletData, ParticipantId participant);

  /**
   * Notifies this listener that a participant has been removed.
   *
   * @param waveletData the wavelet data which the participant has been removed from
   * @param participant  participant that was removed
   */
  void onParticipantRemoved(WaveletData waveletData, ParticipantId participant);

  /**
   * Notifies this listener that a new blip has been added to the wavelet.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip  blip that was added
   */
  void onBlipDataAdded(WaveletData waveletData, BlipData blip);

  /**
   * Notifies this listener that a blip has been submitted.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip  blip that was submitted
   */
  void onBlipDataSubmitted(WaveletData waveletData, BlipData blip);

  /**
   * Notifies this listener that a contributor has been added to a blip.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip         blip that was modified
   * @param contributor  contributor that was added
   */
  void onBlipDataContributorAdded(
      WaveletData waveletData, BlipData blip, ParticipantId contributor);

  /**
   * Notifies this listener that a contributor has been removed from a blip.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip         blip that was modified
   * @param contributor  contributor that was removed
   */
  void onBlipDataContributorRemoved(
      WaveletData waveletData, BlipData blip, ParticipantId contributor);

  /**
   * Notifies this listener that the last-modified time has changed.
   * @param waveletData TODO
   */
  void onLastModifiedTimeChanged(WaveletData waveletData, long oldTime, long newTime);

  /**
   * Notifies this listener that a blip's timestamp has changed.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip     blip that changed
   * @param oldTime  old timestamp
   * @param newTime  new timestamp
   */
  void onBlipDataTimestampModified(
      WaveletData waveletData, BlipData blip, long oldTime, long newTime);

  /**
   * Notifies this listener that a blip's last-modified version has changed.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip        blip that changed
   * @param oldVersion  old version
   * @param newVersion  new version
   */
  void onBlipDataVersionModified(
      WaveletData waveletData, BlipData blip, long oldVersion, long newVersion);

  /**
   * Notifies this listener that the wavelet's version has changed.
   *
   * @param waveletData the wavelet data that has changed
   * @param oldVersion  old version
   * @param newVersion  new version
   */
  void onVersionChanged(WaveletData waveletData, long oldVersion, long newVersion);

  /**
   * Notifies this listener that the wavelet's hashed version has changed.
   *
   * @param waveletData the wavelet data that has changed
   * @param oldHashedVersion  old version
   * @param newHashedVersion  new version
   */
  void onHashedVersionChanged(WaveletData waveletData, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion);

  /**
   * Notifies this listener that the content of the blip was modified.
   *
   * @param waveletData the wavelet data which has been changed
   * @param blip         blip that was modified
   */
  @Deprecated
  void onRemoteBlipDataContentModified(WaveletData waveletData, BlipData blip);
}
