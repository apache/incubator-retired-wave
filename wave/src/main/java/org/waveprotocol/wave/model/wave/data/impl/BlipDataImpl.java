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
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides a skeleton implementation of a primitive blip.
 *
 */
public final class BlipDataImpl extends AbstractBlipData {
  /** The participants who have contributed to this blip's content. */
  private final LinkedHashSet<ParticipantId> contributors;

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
  BlipDataImpl(String id, WaveletDataImpl wavelet, ParticipantId author,
      Collection<ParticipantId> contributors, DocumentOperationSink content, long lastModifiedTime,
      long lastModifiedVersion) {
    super(id, wavelet, author, content, lastModifiedTime, lastModifiedVersion);
    this.contributors = new LinkedHashSet<ParticipantId>(contributors);
  }

  @Override
  public Set<ParticipantId> getContributors() {
    return contributors;
  }

  //
  // Mutators
  //

  @Override
  public void addContributor(ParticipantId participant) {
    if (contributors.add(participant)) {
      fireContributorAdded(participant);
    }
  }

  @Override
  public void removeContributor(ParticipantId participant) {
    boolean removed = contributors.removeAll(Arrays.asList(participant));
    if (removed) {
      fireContributorRemoved(participant);
    }
  }
}
