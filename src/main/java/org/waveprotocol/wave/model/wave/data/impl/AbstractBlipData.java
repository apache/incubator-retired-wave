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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

/**
 * Provides a skeleton implementation of a primitive blip except
 * contributors.
 *
 */
public abstract class AbstractBlipData implements BlipData {
  /** This blip's identifier */
  private final String id;

  /** The wavelet in which this blip appears. */
  private final AbstractWaveletData<?> wavelet;

  /** The XML content of this blip. */
  private final DocumentOperationSink content;

  /** The id of the author of this blip. */
  private final ParticipantId author;

  /** The epoch time of the last modification to this blip. */
  private long lastModifiedTime;

  /** The wavelet version of the last modification to this blip. */
  private long lastModifiedVersion;

  /**
   * Creates a blip.
   *
   * @param id the id of this blip
   * @param wavelet the wavelet containing this blip
   * @param author the author of this blip
   * @param content XML document of this blip
   * @param lastModifiedTime the last modified time of this blip
   * @param lastModifiedVersion the last modified version of this blip
   */
  protected AbstractBlipData(String id, AbstractWaveletData<?> wavelet, ParticipantId author,
      DocumentOperationSink content, long lastModifiedTime,
      long lastModifiedVersion) {
    this.content = content;
    this.id = id;
    this.wavelet = wavelet;
    this.author = author;
    this.lastModifiedTime = lastModifiedTime;
    this.lastModifiedVersion = lastModifiedVersion;
  }

  //
  // Accessors
  //

  @Override
  final public String getId() {
    return id;
  }

  @Override
  final public ParticipantId getAuthor() {
    return author;
  }

  @Override
  final public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  final public long getLastModifiedVersion() {
    return lastModifiedVersion;
  }

  @Override
  final public DocumentOperationSink getContent() {
    return content;
  }

  @Override
  final public AbstractWaveletData<?> getWavelet() {
    return wavelet;
  }

  //
  // Mutators
  //

  /**
   * {@inheritDoc}
   *
   * Tells the wavelet to notify its listeners that this blip has been submitted.
   */
  @Override
  final public void submit() {
    wavelet.getListenerManager().onBlipDataSubmitted(wavelet, this);
  }

  @Override
  final public long setLastModifiedTime(long newTime) {
    if (newTime == lastModifiedTime) {
      return newTime;
    }

    Long oldLastModifiedTime = lastModifiedTime;
    lastModifiedTime = newTime;
    wavelet.getListenerManager().onBlipDataTimestampModified(
        wavelet, this, oldLastModifiedTime, newTime);
    return oldLastModifiedTime;
  }

  @Override
  final public long setLastModifiedVersion(long newVersion) {
    if (newVersion == lastModifiedVersion) {
      return newVersion;
    }
    Long oldVersion = lastModifiedVersion;
    lastModifiedVersion = newVersion;
    wavelet.getListenerManager().onBlipDataVersionModified(wavelet, this, oldVersion, newVersion);
    return oldVersion;
  }

  @Override
  final public String toString() {
    return "Blip state = " +
        "[id:" + id + "] " +
        "[author: " + author + "] " +
        "[contributors: " + getContributors() + "] " +
        "[lastModifiedVersion:" + lastModifiedVersion + "] " +
        "[lastModifiedTime:" + lastModifiedTime + "]";
  }

  @Deprecated
  @Override
  final public void onRemoteContentModified() {
    wavelet.getListenerManager().onRemoteBlipDataContentModified(wavelet, this);
  }

  final protected void fireContributorAdded(ParticipantId contributor) {
    wavelet.getListenerManager().onBlipDataContributorAdded(wavelet, this, contributor);
  }

  final protected void fireContributorRemoved(ParticipantId contributor) {
    wavelet.getListenerManager().onBlipDataContributorRemoved(wavelet, this, contributor);
  }
}
