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

import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Wave data conversion methods.
 */
public final class DataUtil {

  public static ObservableWaveletData fromCoreWaveletData(
      CoreWaveletData wavelet, HashedVersion version, SchemaProvider schemas) {
    return UnmodifiableWaveletData.FACTORY.create(new CoreWrapperWaveletData(
        wavelet, version, ObservablePluggableMutableDocument.createFactory(schemas)));
  }

  /**
   * {@link BlipData} implementation which has its author set to the first
   * participant on the wavelet, no contributors (empty set), no last modified
   * time (0L), and no last modified version (-1).
   */
  private static final class RawBlipData extends AbstractBlipData {
    /**
     * Creates a blip.
     *
     * @param id the id of this blip
     * @param wavelet the wavelet containing this blip
     * @param content document of this blip
     */
    public RawBlipData(String id, AbstractWaveletData<?> wavelet, DocumentOperationSink content) {
      // no last modified time (0), and no last modified version (NO_VERSION)
      super(id, wavelet, wavelet.getParticipants().iterator().next(), content, 0L, -1);
    }

    @Override
    public Set<ParticipantId> getContributors() {
      return Collections.<ParticipantId>emptySet();
    }

    @Override
    public void addContributor(ParticipantId participant) {
      throw new UnsupportedOperationException("RawBlipData doesn't support addContributor");
    }

    @Override
    public void removeContributor(ParticipantId participant) {
      throw new UnsupportedOperationException("RawBlipData doesn't support removeContributor");
    }
  }

  /**
   * Wraps {@link CoreWaveletData} under the {@link ObservableWaveletData} interface.
   * Instances are immutable, i.e., all mutation methods throw
   * {@link UnsupportedOperationException}.
   */
  private static final class CoreWrapperWaveletData extends AbstractWaveletData<RawBlipData> {
    private final CoreWaveletData data;
    private final DocumentFactory<?> contentFactory;

    /**
     * Wraps {@code data}.
     *
     * @param data The CoreWaveletData to copy the data from.
     * @param version The wavelet version.
     * @param contentFactory Factory for creating new documents.
     */
    public CoreWrapperWaveletData(CoreWaveletData data, HashedVersion version,
        DocumentFactory<?> contentFactory) {
      // A random participant as creator (get(0)), no creation time (0),
      // and no last modified time (0)
      super(data.getWaveletName().waveletId, data.getParticipants().get(0), 0L,
          version.getVersion(), version, 0L, data.getWaveletName().waveId,
          StubDocumentFactory.INSTANCE);
      this.data = data;
      this.contentFactory = contentFactory;
    }

    @Override
    protected Set<ParticipantId> getMutableParticipants() {
      // This set isn't mutable, hence mutation methods will fail.
      return CollectionUtils.immutableSet(data.getParticipants());
    }

    @Override
    protected RawBlipData internalCreateDocument(String docId, ParticipantId author,
        Collection<ParticipantId> contributors, DocumentOperationSink contentSink,
        long lastModifiedTime, long lastModifiedVersion) {
      throw new UnsupportedOperationException(
          "CoreWrapperWaveletData doesn't support document creation");
    }

    @Override
    public RawBlipData getDocument(String documentName) {
      return new RawBlipData(documentName, this,
          contentFactory.create(getWaveletId(), documentName,
              DocOpUtil.asInitialization(data.getDocuments().get(documentName))));
    }

    @Override
    public Set<String> getDocumentIds() {
      return Collections.unmodifiableSet(data.getDocuments().keySet());
    }
  }

  private DataUtil() { } // prevents instantiation
}
