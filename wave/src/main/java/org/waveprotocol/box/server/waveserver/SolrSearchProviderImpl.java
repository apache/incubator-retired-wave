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

import com.google.common.base.Function;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.wave.api.SearchResult;
import com.typesafe.config.Config;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.waveprotocol.box.stat.Timed;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Search provider that offers full text search
 *
 * @author Frank R. <renfeng.cn@gmail.com>
 * @author Yuri Zelikov <yurize@apache.com>
 */
public class SolrSearchProviderImpl extends AbstractSearchProviderImpl {

  private static final Log LOG = Log.get(SolrSearchProviderImpl.class);

  private static final String WORD_START = "(\\b|^)";
  private static final Pattern IN_PATTERN = Pattern.compile("\\bin:\\S*");
  private static final Pattern WITH_PATTERN = Pattern.compile("\\bwith:\\S*");

  public static final int ROWS = 10;

  public static final String ID = "id";
  public static final String WAVE_ID = "waveId_s";
  public static final String WAVELET_ID = "waveletId_s";
  public static final String DOC_NAME = "docName_s";
  public static final String LMT = "lmt_l";
  public static final String WITH = "with_ss";
  public static final String WITH_FUZZY = "with_txt";
  public static final String CREATOR = "creator_t";
  public static final String TEXT = "text_t";
  public static final String IN = "in_ss";

  private final String solrBaseUrl;

  /*-
   * http://wiki.apache.org/solr/CommonQueryParameters#q
   */
  public static final String Q = WAVE_ID + ":[* TO *]"
      + " AND " + WAVELET_ID + ":[* TO *]"
      + " AND " + DOC_NAME + ":[* TO *]"
      + " AND " + LMT + ":[* TO *]"
      + " AND " + WITH + ":[* TO *]"
      + " AND " + WITH_FUZZY + ":[* TO *]"
      + " AND " + CREATOR + ":[* TO *]";

  private static final String FILTER_QUERY_PREFIX = "{!lucene q.op=AND df=" + TEXT + "}" //
      + WITH + ":";

  private final static Function<InputStreamReader, JsonArray> extractDocsJsonFunction =
      new Function<InputStreamReader, JsonArray>() {

    @Override
    public JsonArray apply(InputStreamReader inputStreamResponse) {
      return extractDocsJson(inputStreamResponse);
    }};

  @Inject
  public SolrSearchProviderImpl(WaveDigester digester, WaveMap waveMap, Config config) {
    super(config.getString("core.wave_server_domain"), digester, waveMap);
    solrBaseUrl = config.getString("core.solr_base_url");
  }

  @Timed
  @Override
  public SearchResult search(final ParticipantId user, String query, int startAt, int numResults) {

    LOG.fine("Search query '" + query + "' from user: " + user + " [" + startAt + ", "
        + ((startAt + numResults) - 1) + "]");

    // Maybe should be changed in case other folders in addition to 'inbox' are
    // added.
    final boolean isAllQuery = isAllQuery(query);

    LinkedHashMultimap<WaveId, WaveletId> currentUserWavesView = LinkedHashMultimap.create();

    if (numResults > 0) {

      int start = startAt;
      int rows = Math.max(numResults, ROWS);

      /*-
       * "fq" stands for Filter Query. see
       * http://wiki.apache.org/solr/CommonQueryParameters#fq
       */
      String fq = buildFilterQuery(query, isAllQuery, user.getAddress(), sharedDomainParticipantId);

      try {
        while (true) {
          String solrQuery = buildCurrentSolrQuery(start, rows, fq);

          JsonArray docsJson = sendSearchRequest(solrQuery, extractDocsJsonFunction);

          addSearchResultsToCurrentWaveView(currentUserWavesView, docsJson);
          if (docsJson.size() < rows) {
            break;
          }
          start += rows;
        }
      } catch (Exception e) {
        LOG.warning("Failed to execute query: " + query);
        LOG.warning(e.getMessage());
        return digester.generateSearchResult(user, query, null);
      }
    }

    ensureWavesHaveUserDataWavelet(currentUserWavesView, user);

    LinkedHashMap<WaveId, WaveViewData> results =
        createResults(user, isAllQuery, currentUserWavesView);

    Collection<WaveViewData> searchResult =
        computeSearchResult(user, startAt, numResults, Lists.newArrayList(results.values()));
    LOG.info("Search response to '" + query + "': " + searchResult.size() + " results, user: "
        + user);

    return digester.generateSearchResult(user, query, searchResult);
  }

  private LinkedHashMap<WaveId, WaveViewData> createResults(final ParticipantId user,
      final boolean isAllQuery, LinkedHashMultimap<WaveId, WaveletId> currentUserWavesView) {
    Function<ReadableWaveletData, Boolean> matchesFunction =
        new Function<ReadableWaveletData, Boolean>() {

      @Override
      public Boolean apply(ReadableWaveletData wavelet) {
        try {
          return isWaveletMatchesCriteria(wavelet, user, sharedDomainParticipantId, isAllQuery);
        } catch (WaveletStateException e) {
          LOG.warning(
              "Failed to access wavelet "
                  + WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId()), e);
          return false;
        }
      }
    };

    LinkedHashMap<WaveId, WaveViewData> results =
        filterWavesViewBySearchCriteria(matchesFunction, currentUserWavesView);
    if (LOG.isFineLoggable()) {
      for (Map.Entry<WaveId, WaveViewData> e : results.entrySet()) {
        LOG.fine("filtered results contains: " + e.getKey());
      }
    }
    return results;
  }

  private void addSearchResultsToCurrentWaveView(
      LinkedHashMultimap<WaveId, WaveletId> currentUserWavesView, JsonArray docsJson) {
    for (JsonElement aDocsJson : docsJson) {
      JsonObject docJson = aDocsJson.getAsJsonObject();

      WaveId waveId = WaveId.deserialise(docJson.getAsJsonPrimitive(WAVE_ID).getAsString());
      WaveletId waveletId =
              WaveletId.deserialise(docJson.getAsJsonPrimitive(WAVELET_ID).getAsString());
      currentUserWavesView.put(waveId, waveletId);
    }
  }

  private static JsonArray extractDocsJson(InputStreamReader isr) {
    JsonObject json = new JsonParser().parse(isr).getAsJsonObject();
    JsonObject responseJson = json.getAsJsonObject("response");
    return responseJson.getAsJsonArray("docs");
  }

  private String buildCurrentSolrQuery(int start, int rows, String fq) {
    return solrBaseUrl + "/select?wt=json" + "&start=" + start + "&rows="
        + rows + "&sort=" + LMT + "+desc" + "&q=" + Q + "&fq=" + fq;
  }

  private JsonArray sendSearchRequest(String solrQuery,
      Function<InputStreamReader, JsonArray> function) throws IOException {
    JsonArray docsJson;
    GetMethod getMethod = new GetMethod();
    HttpClient httpClient = new HttpClient();
    try {
      getMethod.setURI(new URI(solrQuery, false));
      int statusCode = httpClient.executeMethod(getMethod);
      docsJson = function.apply(new InputStreamReader(getMethod.getResponseBodyAsStream()));
      if (statusCode != HttpStatus.SC_OK) {
        LOG.warning("Failed to execute query: " + solrQuery);
        throw new IOException("Search request status is not OK: " + statusCode);
      }
    } finally {
      getMethod.releaseConnection();
    }
    return docsJson;
  }

  private static boolean isAllQuery(String query) {
    return !IN_PATTERN.matcher(query).find();
  }

  private static String buildUserQuery(String query, ParticipantId sharedDomainParticipantId) {
    return query.replaceAll(WORD_START + TokenQueryType.IN.getToken() + ":", IN + ":")
        .replaceAll(WORD_START + TokenQueryType.WITH.getToken() + ":@",
            WITH + ":" + sharedDomainParticipantId.getAddress())
        .replaceAll(WORD_START + TokenQueryType.WITH.getToken() + ":", WITH_FUZZY + ":")
        .replaceAll(WORD_START + TokenQueryType.CREATOR.getToken() + ":", CREATOR + ":");
  }

  private static String buildFilterQuery(String query, final boolean isAllQuery,
      String addressOfRequiredParticipant, ParticipantId sharedDomainParticipantId) {

    String fq;
    if (isAllQuery) {
      fq =
          FILTER_QUERY_PREFIX + "(" + addressOfRequiredParticipant + " OR "
              + sharedDomainParticipantId + ")";
    } else {
      fq = FILTER_QUERY_PREFIX + addressOfRequiredParticipant;
    }
    if (query.length() > 0) {

      fq += " AND (" + buildUserQuery(query, sharedDomainParticipantId) + ")";
    }
    return fq;
  }
}
