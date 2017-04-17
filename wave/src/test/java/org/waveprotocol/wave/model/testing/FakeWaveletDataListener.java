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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletDataListener;

/**
 * Stub implementation of {@link WaveletDataListener}. Each notification method
 * saves the passed parameters for later inspection by accessors.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FakeWaveletDataListener implements WaveletDataListener {
  /**
   * The last participantId received from
   * {@link #onParticipantAdded(WaveletData, ParticipantId)}
   */
  private ParticipantId participantAdded;

  /**
   * The last participantId received from
   * {@link #onParticipantRemoved(WaveletData, ParticipantId)}
   */
  private ParticipantId participantRemoved;

  /**
   * The last blip received from {@link #onBlipDataAdded(WaveletData, BlipData)}
   */
  private BlipData blipDataAdded;

  /**
   * The last oldTitle received from {@link #onTitleChanged(WaveletData, String, String)}.
   */
  private String oldTitle;

  /**
   * The last newTitle received from {@link #onTitleChanged(WaveletData, String, String)}.
   */
  private String newTitle;

  /**
   * The last blip target received from any other onBlipXxx method.
   */
  private BlipData blipModified;

  /**
   * The last old modified time received by
   * {@link #onLastModifiedTimeChanged(WaveletData, long, long)}
   */
  private long oldLastModifiedTime;

  /**
   * The last new modified time received by
   * {@link #onLastModifiedTimeChanged(WaveletData, long, long)}
   */
  private long newLastModifiedTime;

  /**
   * The last contributor received by
   * {@link #onBlipDataContributorAdded(WaveletData, BlipData, ParticipantId)}
   */
  private ParticipantId blipContributorAdded;

  /**
   * The last contributor received by
   * {@link #onBlipDataContributorRemoved(WaveletData, BlipData, ParticipantId)}
   */
  private ParticipantId blipContributorRemoved;

  /**
   * The last old timestamp received by
   * {@link #onBlipDataTimestampModified(WaveletData, BlipData, long, long)}
   */
  private long blipOldTimestamp;

  /**
   * The last new timestamp received by
   * {@link #onBlipDataTimestampModified(WaveletData, BlipData, long, long)}
   */
  private long blipNewTimestamp;
  /**
   * The last old version received by
   * {@link #onBlipDataVersionModified(WaveletData, BlipData, long, long)}
   */
  private long blipOldVersion;

  /**
   * The last new version received by
   * {@link #onBlipDataVersionModified(WaveletData, BlipData, long, long)}
   */
  private long blipNewVersion;

  private long oldVersion;
  private long newVersion;

  private HashedVersion oldHashedVersion;
  private HashedVersion newHashedVersion;

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participantId) {
    this.participantAdded = participantId;
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participantId) {
    this.participantRemoved = participantId;
  }

  @Override
  public void onLastModifiedTimeChanged(WaveletData waveletData, long oldTime, long newTime) {
    this.oldLastModifiedTime = oldTime;
    this.newLastModifiedTime = newTime;
  }

  @Override
  public void onVersionChanged(WaveletData wavelet, long oldVersion, long newVersion) {
    this.oldVersion = oldVersion;
    this.newVersion = newVersion;
  }

  @Override
  public void onHashedVersionChanged(WaveletData waveletData, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion) {
    this.oldHashedVersion = oldHashedVersion;
    this.newHashedVersion = newHashedVersion;
  }

  @Override
  public void onBlipDataAdded(WaveletData waveletData, BlipData blip) {
    this.blipDataAdded = blip;
  }

  @Override
  public void onBlipDataContributorAdded(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {
    this.blipModified = blip;
    this.blipContributorAdded = contributor;
  }

  @Override
  public void onBlipDataContributorRemoved(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {
    this.blipModified = blip;
    this.blipContributorRemoved = contributor;
  }

  @Override
  public void onBlipDataTimestampModified(
      WaveletData waveletData, BlipData blip, long oldTime, long newTime) {
    this.blipModified = blip;
    this.blipOldTimestamp = oldTime;
    this.blipNewTimestamp = newTime;
  }

  @Override
  public void onBlipDataVersionModified(
      WaveletData waveletData, BlipData blip, long oldVersion, long newVersion) {
    this.blipModified = blip;
    this.blipOldVersion = oldVersion;
    this.blipNewVersion = newVersion;
  }

  @Deprecated
  @Override
  public void onRemoteBlipDataContentModified(WaveletData waveletData, BlipData blip) {
    this.blipModified = blip;
  }

  @Override
  public void onBlipDataSubmitted(WaveletData waveletData, BlipData blip) {
    this.blipModified = blip;
  }

  /**
   * @return the last participantId received by
   *         {@link #onParticipantAdded(WaveletData, ParticipantId)}
   */
  public ParticipantId getParticipantAdded() {
    return participantAdded;
  }

  /**
   * @return the last participantId received by
   *         {@link #onParticipantRemoved(WaveletData, ParticipantId)}
   */
  public ParticipantId getParticipantRemoved() {
    return participantRemoved;
  }

  /**
   * @return the last blip received by {@link #onBlipDataAdded(WaveletData, BlipData)}
   */
  public BlipData getBlipDataAdded() {
    return blipDataAdded;
  }

  /**
   * @return the last blip received by any of the other onBlipDataXxx methods.
   */
  public BlipData getBlipModified() {
    return blipModified;
  }

  /**
   * @return the last newTitle received by
   *         {@link #onTitleChanged(WaveletData, String, String)}.
   */
  public String getNewTitle() {
    return newTitle;
  }

  /**
   * @return the last oldTitle received by
   *         {@link #onTitleChanged(WaveletData, String, String)}.
   */
  public String getOldTitle() {
    return oldTitle;
  }

  /**
   * @return the last old time received by
   *         {@link #onLastModifiedTimeChanged(WaveletData, long, long)}
   */
  public long getOldLastModifiedTime() {
    return oldLastModifiedTime;
  }

  /**
   * @return the last new time received by
   *         {@link #onLastModifiedTimeChanged(WaveletData, long, long)}
   */
  public long getNewLastModifiedTime() {
    return newLastModifiedTime;
  }

  /**
   * @return the last participant received by
   *         {@link #onBlipDataContributorAdded(WaveletData, BlipData, ParticipantId)}
   */
  public ParticipantId getBlipContributorAdded() {
    return blipContributorAdded;
  }

  /**
   * @return the last participant receieved by
   *         {@link #onBlipDataContributorRemoved(WaveletData, BlipData, ParticipantId)}
   */
  public ParticipantId getBlipContributorRemoved() {
    return blipContributorRemoved;
  }

  /**
   * @return the last old timestamp received by
   *         {@link #onBlipDataTimestampModified(WaveletData, BlipData, long, long)}
   */
  public long getBlipOldTimestamp() {
    return blipOldTimestamp;
  }

  /**
   * @return the last new timestamp received by
   *         {@link #onBlipDataTimestampModified(WaveletData, BlipData, long, long)}
   */
  public long getBlipNewTimestamp() {
    return blipNewTimestamp;
  }

  /**
   * @return the last new version received by
   *         {@link #onBlipDataVersionModified(WaveletData, BlipData, long, long)}
   */
  public long getBlipOldVersion() {
    return blipOldVersion;
  }

  /**
   * @return the last old version received by
   *         {@link #onBlipDataVersionModified(WaveletData, BlipData, long, long)}
   */
  public long getBlipNewVersion() {
    return blipNewVersion;
  }

  public long getNewVersion() {
    return newVersion;
  }

  public long getOldVersion() {
    return oldVersion;
  }

  public HashedVersion getNewHashedVersion() {
    return newHashedVersion;
  }

  public HashedVersion getOldHashedVersion() {
    return oldHashedVersion;
  }
}
