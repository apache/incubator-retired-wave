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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;

import org.waveprotocol.box.common.Receiver;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * The state of a wavelet, including its delta history. Combines persisted and
 * not-yet-persisted delta history.
 *
 * Implementations of this interface are not thread safe. The callers must
 * serialize all calls to an instance of this interface.
 *
 * @author soren@google.com (Soren Lassen)
 */
interface WaveletState {

  /**
   * @return The wavelet name.
   */
  WaveletName getWaveletName();

  /**
   * @return a snapshot copy of the wavelet state. The snapshot is a reference
   *         the internal state.
   */
  ReadableWaveletData getSnapshot();

  /**
   * @return the current hashed version.
   */
  HashedVersion getCurrentVersion();

  /**
   * @return The last persisted hashed version.
   */
  HashedVersion getLastPersistedVersion();

  /**
   * @return the hashed version at the given version, if the version is at a
   *         delta boundary, otherwise null.
   */
  HashedVersion getHashedVersion(long version);

  /**
   * @return the transformed delta applied at the given version, if it exists,
   *         otherwise null.
   */
  TransformedWaveletDelta getTransformedDelta(HashedVersion beginVersion);

  /**
   * @return the transformed delta with the given resulting version, if it
   *         exists, otherwise null.
   */
  TransformedWaveletDelta getTransformedDeltaByEndVersion(HashedVersion endVersion);

  /**
   * Gets the transformed deltas from the one applied at the given start version
   * until the one resulting in the given end version or receiver will interrupt.
   */
  void getTransformedDeltaHistory(HashedVersion startVersion, HashedVersion endVersion,
      Receiver<TransformedWaveletDelta> receiver);

  /**
   * @return the applied delta applied at the given version, if it exists,
   *         otherwise null.
   */
  ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(HashedVersion beginVersion);

  /**
   * @return the applied delta with the given resulting version, if it exists,
   *         otherwise null.
   */
  ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDeltaByEndVersion(
      HashedVersion endVersion);

  /**
   * Gets the applied deltas from the one applied at the given start version
   * until the one resulting in the given end version or receiver will interrupt.
   */
   void getAppliedDeltaHistory(
      HashedVersion startVersion, HashedVersion endVersion,
      Receiver<ByteStringMessage<ProtocolAppliedWaveletDelta>> receiver);

  /**
   * Appends the delta to the in-memory delta history.
   *
   * <p>
   * The caller must make a subsequent call to {@link #persist(HashedVersion)}
   * to persist the appended delta.
   */
  void appendDelta(WaveletDeltaRecord deltaRecord)
      throws InvalidProtocolBufferException, OperationException;

  /**
   * Initiates persistence of all in-memory deltas up to the one which
   * results in the given version. This call is non-blocking.
   *
   * <p>
   * If the deltas up to the given version are already persisted, this call does
   * nothing and returns a future which is already done.
   *
   * @param version Must be the resulting version of some delta in the delta
   *        history.
   * @return a future which is done when the version is persisted, or the attempt
   *         to persist fails (in which case the future raises an exception).
   */
  ListenableFuture<Void> persist(HashedVersion version);

  /** Flushes persisted delta from memory. */
  void flush(HashedVersion version);

  /**
   * Closes the object. No other methods on the object should be invoked after
   * this class.
   */
  void close() throws IOException;
}
