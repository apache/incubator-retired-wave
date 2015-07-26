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

package org.waveprotocol.wave.model.waveref;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * A WaveRef refers to a particular wave, and optionally to a more specific
 * position inside it, i.e. to a wavelet and blip inside it.
 *
 * @author meade@google.com <Edwina Mead>
 */
public final class WaveRef {

  /**
   * Provides a waveref.
   */
  public interface Provider {
    WaveRef getWaveRef();
  }

  // Factory methods.
  public static WaveRef of(WaveId waveId) {
    Preconditions.checkNotNull(waveId, "Null wave Id");
    return new WaveRef(waveId, null, null);
  }

  public static WaveRef of(WaveId waveId, WaveletId waveletId) {
    Preconditions.checkNotNull(waveId, "Null wave Id");
    Preconditions.checkNotNull(waveletId, "Null wavelet Id");
    return new WaveRef(waveId, waveletId, null);
  }

  public static WaveRef of(WaveId waveId, WaveletId waveletId, String documentId) {
    Preconditions.checkNotNull(waveId, "Null wave Id");
    Preconditions.checkNotNull(waveletId, "Null wavelet Id");
    Preconditions.checkNotNull(documentId, "Null blip Id");
    return new WaveRef(waveId, waveletId, documentId);
  }

  private final WaveId waveId;

  /** Optional waveletId within the wave specified by waveId. May be null. */
  private final WaveletId waveletId;

  /**
   * The documentId within the wavelet specified by waveletId. May be null.
   * Should always be null if waveletId is null.
   */
  private final String documentId;

  private WaveRef(WaveId waveId, WaveletId wavelet, String documentId) {
    Preconditions.checkNotNull(waveId, "Waveref must have non-null wave-id");
    this.waveId = waveId;
    this.waveletId = wavelet;
    if (wavelet != null) {
      this.documentId = documentId;
    } else {
      this.documentId = null;
    }
  }

  /**
   * @return The WaveId contained in this waveref. Never null.
   */
  public WaveId getWaveId() {
    return waveId;
  }

  /**
   * @return The WaveletId contained in this waveref. Could be null if not
   *         specified.
   */
  public WaveletId getWaveletId() {
    return waveletId;
  }

  /**
   * @return the document Id contained in this waveref. Could be null if not
   *         specified. Should always be null if getWaveletId returned null.
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * @return true if the waveref has a waveletId specified, false if
   *         getWaveletId would return null.
   */
  public boolean hasWaveletId() {
    return (waveletId != null);
  }

  /**
   * @return true if the waveref has a documentId specified, false if
   *         getDocumentId would return null.
   */
  public boolean hasDocumentId() {
    return (documentId != null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WaveRef)) {
      return false;
    }
    WaveRef other = (WaveRef) obj;

    return (ValueUtils.equal(waveId, other.getWaveId()) &&
        ValueUtils.equal(waveletId, other.getWaveletId()) &&
        ValueUtils.equal(documentId, other.getDocumentId()));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + waveId.hashCode();
    if (waveletId != null) {
      result = prime * result + waveletId.hashCode();
    }
    if (documentId != null) {
      result = prime * result + documentId.hashCode();
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("[WaveRef:");
    if (waveId != null) {
      result.append(waveId.toString());
    }
    if (waveletId != null) {
      result.append(waveletId.toString());
    }
    if (documentId != null) {
      result.append("[documentId:");
      result.append(documentId);
      result.append("]");
    }
    return result.append("]").toString();
  }
}
