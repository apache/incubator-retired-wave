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

package org.waveprotocol.box.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.server.persistence.lucene.FSIndexDirectory;
import org.waveprotocol.box.server.persistence.lucene.IndexDirectory;
import org.waveprotocol.box.server.waveserver.*;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class SearchModule extends AbstractModule {

  private final String searchType;
  private final String indexDirectory;

  @Inject
  public SearchModule(Config config) {
    this.searchType = config.getString("core.search_type");
    this.indexDirectory = config.getString("core.index_directory");
  }

  @Override
  public void configure() {
    if ("lucene".equals(searchType)) {
      bind(SearchProvider.class).to(SimpleSearchProviderImpl.class).in(Singleton.class);
      bind(PerUserWaveViewProvider.class).to(LucenePerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(PerUserWaveViewBus.Listener.class).to(LucenePerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(PerUserWaveViewHandler.class).to(LucenePerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(IndexDirectory.class).to(FSIndexDirectory.class);
      if (!FileUtils.isDirExistsAndNonEmpty(indexDirectory)) {
        bind(WaveIndexer.class).to(LuceneWaveIndexerImpl.class);
      } else {
        bind(WaveIndexer.class).to(NoOpWaveIndexerImpl.class);
      }
    } else if ("solr".equals(searchType)) {
      bind(SearchProvider.class).to(SolrSearchProviderImpl.class).in(Singleton.class);
      /*-
       * (Frank R.) binds to class with dummy methods just because it's required by
       * org.waveprotocol.box.server.ServerMain.initializeSearch(Injector, WaveBus)
       */
      bind(PerUserWaveViewBus.Listener.class).to(SolrWaveIndexerImpl.class).in(Singleton.class);
      bind(WaveIndexer.class).to(SolrWaveIndexerImpl.class).in(Singleton.class);
    } else if ("memory".equals(searchType)) {
      bind(SearchProvider.class).to(SimpleSearchProviderImpl.class).in(Singleton.class);
      bind(PerUserWaveViewProvider.class).to(MemoryPerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(PerUserWaveViewBus.Listener.class).to(MemoryPerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(PerUserWaveViewHandler.class).to(MemoryPerUserWaveViewHandlerImpl.class).in(
          Singleton.class);
      bind(WaveIndexer.class).to(MemoryWaveIndexerImpl.class).in(Singleton.class);
    } else {
      throw new RuntimeException("Unknown search type: " + searchType);
    }
  }
}
