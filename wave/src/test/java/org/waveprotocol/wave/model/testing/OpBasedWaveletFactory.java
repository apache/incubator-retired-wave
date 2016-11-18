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

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.SilentOperationSink.Executor;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;

/**
 * Factory for creating {@link OpBasedWavelet} instances suitable for testing.
 *
 */
public final class OpBasedWaveletFactory implements WaveViewImpl.WaveletFactory<OpBasedWavelet>,
    Factory<OpBasedWavelet> {

  /**
   * An operation sink that, on every operation it consumes, fires a version
   * update operation back to the wavelet, then passes the operation along to
   * the next sink. Wavelet versioning is specifically designed to be
   * server-controlled. In a test context, this sink is used to simulate the
   * behaviour of a wavelet server firing back acknowledgements with version
   * updates, in order that tests that mutate wavelets also see version number
   * increase.
   */
  private final static class VersionIncrementingSink implements
      SilentOperationSink<WaveletOperation> {
    private final WaveletData data;
    private final SilentOperationSink<? super WaveletOperation> output;

    public VersionIncrementingSink(WaveletData data,
        SilentOperationSink<? super WaveletOperation> output) {
      this.data = data;
      this.output = output;
    }

    @Override
    public void consume(WaveletOperation op) {
      // Update local version, simulating server response.
      try {
        op.createVersionUpdateOp(1, null).apply(data);
      } catch (OperationException e) {
        throw new OperationRuntimeException("test sink verison update failed", e);
      }

      // Pass to output sink.
      output.consume(op);
    }
  }

  /**
   * Builder, through which a factory can be conveniently configured.
   */
  public final static class Builder {
    private final SchemaProvider schemas;
    private ObservableWaveletData.Factory<?> holderFactory;
    private SilentOperationSink<? super WaveletOperation> sink;
    private ParticipantId author;

    public Builder(SchemaProvider schemas) {
      this.schemas = schemas;
    }

    public Builder with(SilentOperationSink<? super WaveletOperation> sink) {
      this.sink = sink;
      return this;
    }

    public Builder with(ObservableWaveletData.Factory<?> holderFactory) {
      this.holderFactory = holderFactory;
      return this;
    }

    public Builder with(ParticipantId author) {
      this.author = author;
      return this;
    }

    public OpBasedWaveletFactory build() {
      if (holderFactory == null) {
        DocumentFactory<?> docFactory = ObservablePluggableMutableDocument.createFactory(schemas);
        holderFactory = WaveletDataImpl.Factory.create(docFactory);
      }
      if (sink == null) {
        sink = SilentOperationSink.VOID;
      }
      if (author == null) {
        // Old tests expect this.
        author = FAKE_PARTICIPANT;
      }
      return new OpBasedWaveletFactory(holderFactory, sink, author);
    }
  }

  private static final ParticipantId FAKE_PARTICIPANT = new ParticipantId("fake@example.com");

  // Parameters with which to create the OpBasedWavelets.
  private final ObservableWaveletData.Factory<?> holderFactory;
  private final SilentOperationSink<? super WaveletOperation> sink;
  private final ParticipantId author;

  // Testing hacks.
  private MockWaveletOperationContextFactory lastContextFactory;
  private MockParticipationHelper lastAuthoriser;

  /**
   * Creates a factory, which creates op-based waves that adapt wave data
   * holders provided by another factory, sending produced operations to a given
   * sink.
   *
   * @param holderFactory factory for providing wave data holders
   * @param sink sink to which produced operations are sent
   * @param author id to which edits are to be attributed
   */
  private OpBasedWaveletFactory(ObservableWaveletData.Factory<?> holderFactory,
      SilentOperationSink<? super WaveletOperation> sink,
      ParticipantId author) {
    this.holderFactory = holderFactory;
    this.sink = sink;
    this.author = author;
  }

  public static Builder builder(SchemaProvider schemas) {
    return new Builder(schemas);
  }

  @Override
  public OpBasedWavelet create() {
    IdGenerator gen = FakeIdGenerator.create();
    return create(gen.newWaveId(), gen.newConversationWaveletId(), FAKE_PARTICIPANT);
  }

  @Override
  public OpBasedWavelet create(WaveId waveId, WaveletId waveletId, ParticipantId creator) {
    long now = System.currentTimeMillis();
    HashedVersion v0 = HashedVersion.unsigned(0);
    ObservableWaveletData waveData = holderFactory
        .create(new EmptyWaveletSnapshot(waveId, waveletId, creator, v0, now));
    lastContextFactory = new MockWaveletOperationContextFactory().setParticipantId(author);
    lastAuthoriser = new MockParticipationHelper();
    SilentOperationSink<WaveletOperation> executor =
        Executor.<WaveletOperation, WaveletData>build(waveData);
    SilentOperationSink<WaveletOperation> out = new VersionIncrementingSink(waveData, sink);
    return new OpBasedWavelet(waveId, waveData, lastContextFactory, lastAuthoriser, executor, out);
  }

  /**
   * Gets the authoriser provided to help the last {@link OpBasedWavelet} that
   * was created. The result is undefined if no wavelets have been created.
   */
  public MockParticipationHelper getLastAuthoriser() {
    return lastAuthoriser;
  }

  /**
   * Gets the helper provided to the last {@link OpBasedWavelet} that was
   * created. The result is undefined if no wavelets have been created.
   */
  public MockWaveletOperationContextFactory getLastContextFactory() {
    return lastContextFactory;
  }
}
