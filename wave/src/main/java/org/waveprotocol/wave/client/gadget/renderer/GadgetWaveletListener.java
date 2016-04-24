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

package org.waveprotocol.wave.client.gadget.renderer;


import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Gadget wavelet listener interface to relay participant/contributor event
 * notifications to the gadget widget.
 *
 */
public interface GadgetWaveletListener {
  /**
   * Notifies a gadget that a participant has been added to its wavelet.
   *
   * @param participant participant that was added
   */
  void onParticipantAdded(ParticipantId participant);

  /**
   * Notifies a gadget that a participant has been removed from its wavelet.
   *
   * @param participant participant that was removed
   */
  void onParticipantRemoved(ParticipantId participant);

  /**
   * Notifies a gadget that a contributor has been added to its blip.
   *
   * @param contributor contributor that was added
   */
  void onBlipContributorAdded(ParticipantId contributor);

  /**
   * Notifies a gadget that a contributor has been removed from its blip.
   *
   * @param contributor contributor that was removed
   */
  void onBlipContributorRemoved(ParticipantId contributor);
}
