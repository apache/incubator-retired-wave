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

package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import com.google.common.base.Function;

/**
 * Interface for a container class for a Wavelet's current state as well as its
 * delta history. Local and remote wavelet interfaces inherit from this one.
 */
interface WaveletContainer {

  /**
   * Manufactures wavelet containers.
   *
   * @param <T> type manufactured by this factory.
   */
  interface Factory<T extends WaveletContainer> {
    /**
     * @return a new wavelet container with the given wavelet name
     */
    T create(WaveletNotificationSubscriber notifiee, WaveletName waveletName, String waveDomain);
  }

  /** Returns the name of the wavelet. */
  WaveletName getWaveletName();

  /** Returns a snapshot copy of the wavelet state. */
  ObservableWaveletData copyWaveletData() throws WaveletStateException;

  /** Returns a snapshot of the wavelet state, last committed version. */
  CommittedWaveletSnapshot getSnapshot() throws WaveletStateException;

  /**
   * Provides read access to the inner state of the {@link WaveletContainer}.
   *
   * @param <T> the return type of the method.
   * @param function the function to apply on the {@link ReadableWaveletData}.
   * @return the output of the function.
   * @throws WaveletStateException if the wavelet is in an unsuitable state.
   */
  <T> T applyFunction(Function<ReadableWaveletData, T> function) throws WaveletStateException;

  /**
   * Retrieve the wavelet history of deltas applied to the wavelet.
   *
   * @param versionStart start version (inclusive), minimum 0.
   * @param versionEnd end version (exclusive).
   * @param receiver the deltas receiver.
   * @throws AccessControlException if {@code versionStart} or
   *         {@code versionEnd} are not in the wavelet history.
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         retrieving history.
   */
  void requestHistory(HashedVersion versionStart, HashedVersion versionEnd,
      Receiver<ByteStringMessage<ProtocolAppliedWaveletDelta>> receiver)
      throws AccessControlException, WaveletStateException;

  /**
   * Retrieve the wavelet history of deltas applied to the wavelet, with
   * additional safety check that
   *
   * @param versionStart start version (inclusive), minimum 0.
   * @param versionEnd end version (exclusive).
   * @param receiver the deltas receiver.
   * @throws AccessControlException if {@code versionStart} or
   *         {@code versionEnd} are not in the wavelet history.
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         retrieving history.
   */
  void requestTransformedHistory(HashedVersion versionStart, HashedVersion versionEnd,
      Receiver<TransformedWaveletDelta> receiver)
      throws AccessControlException, WaveletStateException;

  /**
   * @param participantId id of participant attempting to gain access to
   *        wavelet, or null if the user isn't logged in.
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         checking permissions.
   * @return true if the participant is a participant on the wavelet or if the
   *         wavelet is empty or if a shared domain participant is participant
   *         on the wavelet.
   */
  boolean checkAccessPermission(ParticipantId participantId) throws WaveletStateException;

  /**
   * The Last Committed Version returns when the local or remote wave server
   * committed the wavelet.
   *
   * @throws WaveletStateException if the wavelet is in a state unsuitable for
   *         getting LCV.
   */
  HashedVersion getLastCommittedVersion() throws WaveletStateException;

  /**
   * @return true if the participant id is a current participant of the wavelet.
   *          Each invocation acquires and releases the lock.
   */
  boolean hasParticipant(ParticipantId participant) throws WaveletStateException;

  /**
   * @return the wavelet creator. This method doesn't acquire
   *         {@link WaveletContainer} lock since wavelet creator cannot change
   *         after wavelet creation and therefore it is save to concurrently
   *         read this property without lock.
   */
  ParticipantId getCreator();

  /**
   * This method doesn't acquire {@link WaveletContainer} lock since shared
   * domain participant cannot change and therefore it is safe to concurrently
   * read this property without lock.
   *
   * @return the shared domain participant.
   */
  public ParticipantId getSharedDomainParticipant();

  /**
   * @return true if the wavelet is at version zero, i.e., has no delta history
   */
  boolean isEmpty() throws WaveletStateException;

}
