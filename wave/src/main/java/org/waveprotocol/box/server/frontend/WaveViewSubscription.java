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

package org.waveprotocol.box.server.frontend;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client's subscription to a wave view.
 *
 * @author anorth@google.com (Alex North)
 */
final class WaveViewSubscription {

  /**
   * State of a wavelet endpoint.
   */
  private static final class WaveletChannelState {
    /**
     * Resulting versions of deltas submitted on this wavelet for which
     * the outbound delta has not yet been seen.
     */
    public final Collection<Long> submittedEndVersions = Sets.newHashSet();
    /**
     * Resulting version of the most recent outbound delta.
     */
    public HashedVersion lastVersion = null;
    /**
     * Whether a submit request is awaiting a response.
     */
    public boolean hasOutstandingSubmit = false;
    /**
     * Outbound deltas held back while a submit is in-flight.
     */
    public List<TransformedWaveletDelta> heldBackDeltas = Lists.newLinkedList();
  }

  private static final Log LOG = Log.get(WaveViewSubscription.class);

  private final WaveId waveId;
  private final IdFilter waveletIdFilter;
  private final ClientFrontend.OpenListener openListener;
  private final String channelId;
  private final LoadingCache<WaveletId, WaveletChannelState> channels =
      CacheBuilder.newBuilder().build(new CacheLoader<WaveletId, WaveletChannelState>() {
    @Override
    public WaveletChannelState load(WaveletId id) {
      return new WaveletChannelState();
    }
  });

  public WaveViewSubscription(WaveId waveId, IdFilter waveletIdFilter, String channelId,
      ClientFrontend.OpenListener openListener) {
    Preconditions.checkNotNull(waveId, "null wave id");
    Preconditions.checkNotNull(waveletIdFilter, "null filter");
    Preconditions.checkNotNull(openListener, "null listener");
    Preconditions.checkNotNull(channelId, "null channel id");

    this.waveId = waveId;
    this.waveletIdFilter = waveletIdFilter;
    this.channelId = channelId;
    this.openListener = openListener;
  }

  public WaveId getWaveId() {
    return waveId;
  }

  public ClientFrontend.OpenListener getOpenListener() {
    return openListener;
  }

  public String getChannelId() {
    return channelId;
  }

  /**
   * Checks whether the subscription includes a wavelet.
   */
  public boolean includes(WaveletId waveletId) {
    return IdFilter.accepts(waveletIdFilter, waveletId);
  }

  /** This client sent a submit request */
  public synchronized void submitRequest(WaveletName waveletName) {
    // A given client can only have one outstanding submit per wavelet.
    WaveletChannelState state;
    try {
      state = channels.get(waveletName.waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
    Preconditions.checkState(!state.hasOutstandingSubmit,
        "Received overlapping submit requests to subscription %s", this);
    LOG.info("Submit oustandinding on channel " + channelId);
    state.hasOutstandingSubmit = true;
  }

  /**
   * A submit response for the given wavelet and version has been sent to this
   * client.
   */
  public synchronized void submitResponse(WaveletName waveletName, HashedVersion version) {
    Preconditions.checkNotNull(version, "Null delta application version");
    WaveletId waveletId = waveletName.waveletId;
    WaveletChannelState state;
    try {
      state = channels.get(waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
    Preconditions.checkState(state.hasOutstandingSubmit);
    state.submittedEndVersions.add(version.getVersion());
    state.hasOutstandingSubmit = false;
    LOG.info("Submit resolved on channel " + channelId);

    // Forward any queued deltas.
    List<TransformedWaveletDelta> filteredDeltas =  filterOwnDeltas(state.heldBackDeltas, state);
    if (!filteredDeltas.isEmpty()) {
      //
      // Workaround for WAVE-446 (pablojan@apache.org)
      //	
      for (TransformedWaveletDelta delta: filteredDeltas) {
        List<TransformedWaveletDelta> singleDeltaList = new ArrayList<TransformedWaveletDelta>(1);
    	singleDeltaList.add(delta);
        sendUpdate(waveletName, singleDeltaList, null);
	  }

    }
    state.heldBackDeltas.clear();
  }

  /**
   * Sends deltas for this subscription (if appropriate).
   *
   * If the update contains a delta for a wavelet where the delta is actually
   * from this client, the delta is dropped. If there's an outstanding submit
   * request the delta is queued until the submit finishes.
   */
  public synchronized void onUpdate(WaveletName waveletName, DeltaSequence deltas) {
    Preconditions.checkArgument(!deltas.isEmpty());
    WaveletChannelState state;
    try {
      state = channels.get(waveletName.waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
    checkUpdateVersion(waveletName, deltas, state);
    state.lastVersion = deltas.getEndVersion();
    if (state.hasOutstandingSubmit) {
      state.heldBackDeltas.addAll(deltas);
    } else {
      List<TransformedWaveletDelta> filteredDeltas = filterOwnDeltas(deltas, state);
      if (!filteredDeltas.isEmpty()) {
        sendUpdate(waveletName, filteredDeltas, null);
      }
    }
  }

  /**
   * Filters any deltas sent by this client from a list of received deltas.
   *
   * @param deltas received deltas
   * @param state channel state
   * @return deltas, if none are from this client, or a copy with own client's
   *         deltas removed
   */
  private List<TransformedWaveletDelta> filterOwnDeltas(List<TransformedWaveletDelta> deltas,
      WaveletChannelState state) {
    List<TransformedWaveletDelta> filteredDeltas = deltas;
    if (!state.submittedEndVersions.isEmpty()) {
      filteredDeltas = Lists.newArrayList();
      for (TransformedWaveletDelta delta : deltas) {
        long deltaEndVersion = delta.getResultingVersion().getVersion();
        if (!state.submittedEndVersions.remove(deltaEndVersion)) {
          filteredDeltas.add(delta);
        }
      }
    }
    return filteredDeltas;
  }

  /**
   * Sends a commit notice for this subscription.
   */
  public synchronized void onCommit(WaveletName waveletName, HashedVersion committedVersion) {
    sendUpdate(waveletName, ImmutableList.<TransformedWaveletDelta>of(), committedVersion);
  }

  /**
   * Sends an update to the client.
   */
  private void sendUpdate(WaveletName waveletName, List<TransformedWaveletDelta> deltas,
      HashedVersion committedVersion) {
    // Channel id needs to be sent with every message until views can be
    // closed, see bug 128.
    openListener.onUpdate(waveletName, null, deltas, committedVersion, null, channelId);
  }

  /**
   * Checks the update targets the next expected version.
   */
  private void checkUpdateVersion(WaveletName waveletName, DeltaSequence deltas,
      WaveletChannelState state) {
    if (state.lastVersion != null) {
      long expectedVersion = state.lastVersion.getVersion();
      long targetVersion = deltas.getStartVersion();
      Preconditions.checkState(targetVersion == expectedVersion,
          "Subscription expected delta for %s targeting %s, was %s", waveletName, expectedVersion,
          targetVersion);
    }
  }

  @Override
  public String toString() {
    return "[WaveViewSubscription wave: " + waveId + ", channel: " + channelId + "]";
  }
}
