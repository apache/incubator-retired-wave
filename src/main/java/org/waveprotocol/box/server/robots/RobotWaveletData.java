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

package org.waveprotocol.box.server.robots;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.CapturingOperationSink;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a Wavelet opened by the Robot API. It gathers operations by
 * possibly different participants and can offer up these operations as a list
 * of deltas.
 *
 * <p>
 * TODO(ljvderijk): Possible limitation of this class occurs when a robot wants
 * to execute operations for multiple users in the same request where operations
 * from different users depend on each other. The code will apply each operation
 * to the snapshot given upon construction and not the most up to date one.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotWaveletData {

  private final ReadableWaveletData snapshot;

  private final HashedVersion snapshotVersion;

  /**
   * {@link LinkedHashMap} that maps participants to their
   * {@link CapturingOperationSink}. This allows us to return deltas ordered by
   * the insertion of participants into this map.
   */
  private final LinkedHashMap<ParticipantId, CapturingOperationSink<WaveletOperation>> sinkMap =
      Maps.newLinkedHashMap();

  private final Map<ParticipantId, OpBasedWavelet> waveletMap = Maps.newHashMap();

  /**
   * Constructs a new {@link RobotWaveletData}. The given
   * {@link ReadableWaveletData} will be copied by the constructor.
   *
   * @param snapshot the base {@link ReadableWaveletData} from which
   *        {@link OpBasedWavelet} are created.
   * @param committedVersion the committed version of the given snapshot, used
   *        to generate deltas.
   */
  public RobotWaveletData(ReadableWaveletData snapshot, HashedVersion committedVersion) {
    this.snapshot = WaveletDataUtil.copyWavelet(snapshot);
    this.snapshotVersion = committedVersion;
  }

  /**
   * Returns the name of this wavelet.
   */
  public WaveletName getWaveletName() {
    return WaveletDataUtil.waveletNameOf(snapshot);
  }

  /**
   * Returns an {@link OpBasedWavelet} on which operations can be performed. The
   * operations are collected by this RobotWavelet and can be returned in the
   * form of deltas by calling getDeltas(). This method will store a reference
   * to the {@link OpBasedWavelet} for each unique author given.
   *
   * @param opAuthor the author of the operations performed on the returned
   *        wavelet.
   */
  public OpBasedWavelet getOpBasedWavelet(ParticipantId opAuthor) {
    if (waveletMap.containsKey(opAuthor)) {
      return waveletMap.get(opAuthor);
    }

    // Every wavelet needs another document factory because we need to set the
    // wavelet after the OpBasedWavelet has been created to inject the
    // CapturingOperationSink.
    // TODO(ljvderijk): Proper schemas need to be enforced here.

    DocumentFactory<?> docFactory =
        ObservablePluggableMutableDocument.createFactory(SchemaCollection.empty());

    ObservableWaveletData perAuthorWavelet =
        WaveletDataImpl.Factory.create(docFactory).create(snapshot);

    SilentOperationSink<WaveletOperation> executor =
        SilentOperationSink.Executor.<WaveletOperation, WaveletData>build(perAuthorWavelet);
    // Build sink that gathers these ops
    CapturingOperationSink<WaveletOperation> output =
        new CapturingOperationSink<WaveletOperation>();

    BasicWaveletOperationContextFactory contextFactory =
        new BasicWaveletOperationContextFactory(opAuthor);
    OpBasedWavelet w =
        new OpBasedWavelet(perAuthorWavelet.getWaveId(), perAuthorWavelet, contextFactory,
            ParticipationHelper.DEFAULT, executor, output);

    // Store the new sink and wavelet
    sinkMap.put(opAuthor, output);
    waveletMap.put(opAuthor, w);

    return w;
  }

  /**
   * Returns a list of deltas for all the operations performed on this wavelet
   * in order of the participants passed into getOpBasedWavelet(). The deltas
   * apply to the version given during construction of the
   * {@link RobotWaveletData}.
   */
  public List<WaveletDelta> getDeltas() {
    List<WaveletDelta> deltas = Lists.newArrayList();

    for (Entry<ParticipantId, CapturingOperationSink<WaveletOperation>> entry :
        sinkMap.entrySet()) {
      ParticipantId author = entry.getKey();
      List<WaveletOperation> ops = entry.getValue().getOps();

      if (ops.isEmpty()) {
        // No ops to generate delta for
        continue;
      }
      WaveletDelta delta = new WaveletDelta(author, snapshotVersion, ops);
      deltas.add(delta);
    }
    return deltas;
  }
}
