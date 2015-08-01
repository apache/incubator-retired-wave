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

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Defines the abstract data type used to implement a blip's state.
 *
 * In order to modify a blip, or to traverse the wavelet structure surrounding a blip, consider
 * using the {@code OpBasedBlipAdapter} interface instead.
 *
 */
public interface BlipData extends ReadableBlipData {

  /**
   * Notifies this blip that it has been submitted.
   */
  void submit();

  /**
   * Gets the wavelet in which this blip appears.
   *
   * @return the wavelet in which this blip appears.
   */
  @Override
  WaveletData getWavelet();

  /**
   * Adds a contributor to this blip. The new contributor is added to the
   * end of the contributors collection if it is not already present.
   *
   * @param participant contributor to add
   */
  void addContributor(ParticipantId participant);

  /**
   * Removes a contributor from this blip, ensuring it is no longer reflected
   * in the contributors collection.
   *
   * @param participant  contributor to remove
   */
  void removeContributor(ParticipantId participant);

  /**
   * Sets the last-modified time of this blip
   *
   * @param newTime  the new last-modified time
   * @return the old last-modified time.
   */
  long setLastModifiedTime(long newTime);

  /**
   * Sets the last-modified version of this blip.
   *
   * @param newVersion  the new last-modified version
   * @return the old last-modified version.
   */
  long setLastModifiedVersion(long newVersion);

  /**
   * Notifies the BlipData that the content of the document inside the blip have changed.
   */
  void onRemoteContentModified();
}
