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

import static org.waveprotocol.box.server.waveserver.IndexFieldType.LMT;
import static org.waveprotocol.box.server.waveserver.IndexFieldType.WAVEID;
import static org.waveprotocol.box.server.waveserver.IndexFieldType.WAVELETID;
import static org.waveprotocol.box.server.waveserver.IndexFieldType.WITH;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NRTManager;
import org.apache.lucene.search.NRTManagerReopenThread;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.SearcherWarmer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.Version;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.lucene.IndexDirectory;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lucene based implementation of {@link PerUserWaveViewHandler}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@Singleton
public class LucenePerUserWaveViewHandlerImpl implements PerUserWaveViewHandler, Closeable {

  private static class WaveSearchWarmer implements SearcherWarmer {

    private final ParticipantId sharedDomainParticipantId;

    WaveSearchWarmer(String waveDomain) {
      sharedDomainParticipantId = ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(waveDomain);
    }

    @Override
    public void warm(IndexSearcher searcher) throws IOException {
      // TODO (Yuri Z): Run some diverse searches, searching against all
      // fields.

      BooleanQuery participantQuery = new BooleanQuery();
      participantQuery.add(
          new TermQuery(new Term(WITH.toString(), sharedDomainParticipantId.getAddress())),
          Occur.SHOULD);
      searcher.search(participantQuery, MAX_WAVES);
    }
  }

  private static final Logger LOG = Logger.getLogger(LucenePerUserWaveViewHandlerImpl.class
      .getName());

  // TODO (Yuri Z.): Inject executor.
  private static final Executor executor = Executors.newSingleThreadExecutor();

  private static final Version LUCENE_VERSION = Version.LUCENE_35;

  /** The results will be returned in the ascending order according to last modified time. */
  private static Sort LMT_ASC_SORT = new Sort(new SortField("title", SortField.LONG));

  /** Minimum time until a new reader can be opened. */
  private static final double MIN_STALE_SEC = 0.025;

  /** Maximum time until a new reader must be opened. */
  private static final double MAX_STALE_SEC = 1.0;

  /** Defines the maximum number of waves returned by the search. */
  private static final int MAX_WAVES = 10000;

  private final StandardAnalyzer analyzer;
  private final TextCollator textCollator;
  private final IndexWriter indexWriter;
  private final NRTManager nrtManager;
  private final NRTManagerReopenThread nrtManagerReopenThread;
  private final ReadableWaveletDataProvider waveletProvider;
  private boolean isClosed = false;

  @Inject
  public LucenePerUserWaveViewHandlerImpl(IndexDirectory directory,
      ReadableWaveletDataProvider waveletProvider, TextCollator textCollator,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) final String waveDomain) {
    this.textCollator = textCollator;
    this.waveletProvider = waveletProvider;
    analyzer = new StandardAnalyzer(LUCENE_VERSION);
    try {
      IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, analyzer);
      config.setOpenMode(OpenMode.CREATE_OR_APPEND);
      indexWriter = new IndexWriter(directory.getDirectory(), config);
      nrtManager = new NRTManager(indexWriter, new WaveSearchWarmer(waveDomain));
    } catch (IOException ex) {
      throw new IndexException(ex);
    }

    nrtManagerReopenThread = new NRTManagerReopenThread(nrtManager, MAX_STALE_SEC, MIN_STALE_SEC);
    nrtManagerReopenThread.start();
  }

  /**
   * Closes the handler, releases resources and flushes the recent index changes
   * to persistent storage.
   */
  @Override
  public synchronized void close() {
    if (isClosed) {
      throw new AlreadyClosedException("Already closed");
    }
    isClosed = true;
    try {
      nrtManager.close();
      if (analyzer != null) {
        analyzer.close();
      }
      nrtManagerReopenThread.close();
      indexWriter.close();
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Failed to close the Lucene index", ex);
    }
    LOG.info("Successfully closed the Lucene index...");
  }

  /**
   * Ensures that the index is up to date. Exits quickly if no changes were done
   * to the index.
   *
   * @throws IOException if something goes wrong.
   */
  public void forceReopen() throws IOException {
    nrtManager.maybeReopen(true);
  }

  @Override
  public ListenableFuture<Void> onParticipantAdded(final WaveletName waveletName,
      ParticipantId participant) {
    Preconditions.checkNotNull(waveletName);
    Preconditions.checkNotNull(participant);

    ListenableFutureTask<Void> task = ListenableFutureTask.<Void>create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        ReadableWaveletData waveletData;
        try {
          waveletData = waveletProvider.getReadableWaveletData(waveletName);
          updateIndex(waveletData);
        } catch (WaveServerException e) {
          LOG.log(Level.SEVERE, "Failed to update index for " + waveletName, e);
          throw e;
        }
        return null;
      }
    });
    executor.execute(task);
    return task;
  }

  @Override
  public ListenableFuture<Void> onParticipantRemoved(final WaveletName waveletName,
      final ParticipantId participant) {
    Preconditions.checkNotNull(waveletName);
    Preconditions.checkNotNull(participant);

    ListenableFutureTask<Void> task = ListenableFutureTask.<Void>create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        ReadableWaveletData waveletData;
        try {
          waveletData = waveletProvider.getReadableWaveletData(waveletName);
          try {
            removeParticipantfromIndex(waveletData, participant, nrtManager);
          } catch (CorruptIndexException e) {
            LOG.log(Level.SEVERE, "Failed to update index for " + waveletName, e);
            throw e;
          } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to update index for " + waveletName, e);
            throw e;
          }
        } catch (WaveServerException e) {
          LOG.log(Level.SEVERE, "Failed to update index for " + waveletName, e);
          throw e;
        }
        return null;
      }
    });
    executor.execute(task);
    return task;
  }

  @Override
  public ListenableFuture<Void> onWaveInit(final WaveletName waveletName) {
    Preconditions.checkNotNull(waveletName);

    ListenableFutureTask<Void> task = ListenableFutureTask.<Void>create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        ReadableWaveletData waveletData;
        try {
          waveletData = waveletProvider.getReadableWaveletData(waveletName);
          updateIndex(waveletData);
        } catch (WaveServerException e) {
          LOG.log(Level.SEVERE, "Failed to initialize index for " + waveletName, e);
          throw e;
        }
        return null;
      }
    });
    executor.execute(task);
    return task;
  }

  private void updateIndex(ReadableWaveletData wavelet) throws IndexException {
    Preconditions.checkNotNull(wavelet);
    try {
      // TODO (Yuri Z): Update documents instead of totally removing and adding.
      removeIndex(wavelet, nrtManager);
      addIndex(wavelet, indexWriter, nrtManager, textCollator);
      indexWriter.commit();
    } catch (CorruptIndexException e) {
      throw new IndexException(String.valueOf(wavelet.getWaveletId()), e);
    } catch (IOException e) {
      throw new IndexException(String.valueOf(wavelet.getWaveletId()), e);
    }
  }

  private static void addIndex(ReadableWaveletData wavelet, IndexWriter indexWriter,
      NRTManager nrtManager, TextCollator textCollator) throws CorruptIndexException, IOException {
    Document doc = new Document();
    addWaveletFieldsToIndex(wavelet, textCollator, doc);
    nrtManager.addDocument(doc);
  }

  private static void addWaveletFieldsToIndex(ReadableWaveletData wavelet,
      TextCollator textCollator, Document doc) {
    doc.add(new Field(WAVEID.toString(), wavelet.getWaveId().serialise(), Field.Store.YES,
        Field.Index.NOT_ANALYZED));
    doc.add(new Field(WAVELETID.toString(), wavelet.getWaveletId().serialise(), Field.Store.YES,
        Field.Index.NOT_ANALYZED));
    doc.add(new Field(LMT.toString(), Long.toString(wavelet.getLastModifiedTime()), Field.Store.NO,
        Field.Index.NOT_ANALYZED));
    for (ParticipantId participant : wavelet.getParticipants()) {
      doc.add(new Field(WITH.toString(), participant.toString(), Field.Store.YES,
          Field.Index.NOT_ANALYZED));
    }
  }

  private static void removeIndex(ReadableWaveletData wavelet, NRTManager nrtManager)
      throws CorruptIndexException, IOException {
    BooleanQuery query = new BooleanQuery();
    query.add(new TermQuery(new Term(WAVEID.toString(), wavelet.getWaveId().serialise())),
        BooleanClause.Occur.MUST);
    query.add(new TermQuery(new Term(WAVELETID.toString(), wavelet.getWaveletId().serialise())),
        BooleanClause.Occur.MUST);
    nrtManager.deleteDocuments(query);
  }

  private static void removeParticipantfromIndex(ReadableWaveletData wavelet,
      ParticipantId participant, NRTManager nrtManager) throws CorruptIndexException, IOException {
    BooleanQuery query = new BooleanQuery();
    Term waveIdTerm = new Term(WAVEID.toString(), wavelet.getWaveId().serialise());
    query.add(new TermQuery(waveIdTerm), BooleanClause.Occur.MUST);
    query.add(new TermQuery(new Term(WAVELETID.toString(), wavelet.getWaveletId().serialise())),
        BooleanClause.Occur.MUST);
    SearcherManager searcherManager = nrtManager.getSearcherManager(true);
    IndexSearcher indexSearcher = searcherManager.acquire();
    try {
      TopDocs hints = indexSearcher.search(query, MAX_WAVES);
      for (ScoreDoc hint : hints.scoreDocs) {
        Document document = indexSearcher.doc(hint.doc);
        String[] participantValues = document.getValues(WITH.toString());
        document.removeFields(WITH.toString());
        for (String address : participantValues) {
          if (address.equals(participant.getAddress())) {
            continue;
          }
          document.add(new Field(WITH.toString(), address, Field.Store.YES,
              Field.Index.NOT_ANALYZED));
        }
        nrtManager.updateDocument(waveIdTerm, document);
      }
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to fetch from index " + wavelet.toString(), e);
    } finally {
      try {
        searcherManager.release(indexSearcher);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed to close searcher. ", e);
      }
      indexSearcher = null;
    }
  }


  @Override
  public Multimap<WaveId, WaveletId> retrievePerUserWaveView(ParticipantId user) {
    Preconditions.checkNotNull(user);

    Multimap<WaveId, WaveletId> userWavesViewMap = HashMultimap.create();
    BooleanQuery participantQuery = new BooleanQuery();
    participantQuery.add(new TermQuery(new Term(WITH.toString(), user.getAddress())), Occur.SHOULD);
    SearcherManager searcherManager = nrtManager.getSearcherManager(true);
    IndexSearcher indexSearcher = searcherManager.acquire();
    try {
      TopDocs hints = indexSearcher.search(participantQuery, MAX_WAVES, LMT_ASC_SORT);
      for (ScoreDoc hint : hints.scoreDocs) {
        Document document = indexSearcher.doc(hint.doc);
        WaveId waveId = WaveId.deserialise(document.get(WAVEID.toString()));
        WaveletId waveletId = WaveletId.deserialise(document.get(WAVELETID.toString()));
        userWavesViewMap.put(waveId, waveletId);
      }
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Search failed: " + user, e);
    } finally {
      try {
        searcherManager.release(indexSearcher);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed to close searcher. " + user, e);
      }
      indexSearcher = null;
    }
    return userWavesViewMap;
  }
}