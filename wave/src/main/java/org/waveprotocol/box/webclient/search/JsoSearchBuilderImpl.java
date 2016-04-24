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

package org.waveprotocol.box.webclient.search;

import com.google.common.base.Preconditions;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;

import org.waveprotocol.box.search.SearchRequest;
import org.waveprotocol.box.search.SearchResponse;
import org.waveprotocol.box.search.SearchResponse.Digest;
import org.waveprotocol.box.search.jso.SearchRequestJsoImpl;
import org.waveprotocol.box.search.jso.SearchResponseJsoImpl;
import org.waveprotocol.box.webclient.search.SearchService.Callback;
import org.waveprotocol.box.webclient.search.SearchService.DigestSnapshot;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Helper class to perform searches.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class JsoSearchBuilderImpl implements SearchBuilder {
  private static final LoggerBundle LOG = new DomLogger("SearchBuilder");

  /** The base search URL. */
  private static final String SEARCH_URL_BASE = "/search";

  /** Holds search request data. */
  private SearchRequest searchRequest;

  private JsoSearchBuilderImpl() {
  }

  /** Static factory method */
  public static SearchBuilder create() {
    return new JsoSearchBuilderImpl();
  }

  @Override
  public SearchBuilder newSearch() {
    searchRequest = SearchRequestJsoImpl.create();
    return this;
  }

  @Override
  public SearchBuilder setQuery(String query) {
    searchRequest.setQuery(query);
    return this;
  }

  @Override
  public SearchBuilder setIndex(int index) {
    searchRequest.setIndex(index);
    return this;
  }

  @Override
  public SearchBuilder setNumResults(int numResults) {
    searchRequest.setNumResults(numResults);
    return this;
  }

  @Override
  public Request search(final Callback callback) {
    Preconditions.checkArgument(searchRequest != null,
        "call SearchBuilder.newSearch method to construct a new query");
    Preconditions.checkArgument(searchRequest.getQuery() != null, "new query should be set");

    String url = getUrl(searchRequest);
    LOG.trace().log("Performing a search query: [Query: ", searchRequest.getQuery(), ", Index: ",
        searchRequest.getIndex(), ", NumResults: ", searchRequest.getNumResults(), "]");

    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);

    requestBuilder.setCallback(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        LOG.trace().log("Search response received: ", response.getText());
        if (response.getStatusCode() != Response.SC_OK) {
          callback.onFailure("Got back status code " + response.getStatusCode());
        } else if (!response.getHeader("Content-Type").startsWith("application/json")) {
          callback.onFailure("Search service did not return json");
        } else {
          SearchResponseJsoImpl searchResponse;
          try {
            searchResponse = JsonMessage.parse(response.getText());
          } catch (JsonException e) {
            callback.onFailure(e.getMessage());
            return;
          }
          List<DigestSnapshot> digestSnapshots =
              SearchBuilderUtils.deserializeSearchResponse(searchResponse);
          callback.onSuccess(searchResponse.getTotalResults(), digestSnapshots);
        }
      }

      @Override
      public void onError(Request request, Throwable exception) {
        LOG.error().log("Search error: ", exception);
        callback.onFailure(exception.getMessage());
      }
    });

    try {
      return requestBuilder.send();
    } catch (RequestException e) {
      callback.onFailure(e.getMessage());
      return null;
    }
  }

  private static class SearchBuilderUtils {
    /**
     * Constructs a list of {@link org.waveprotocol.box.webclient.search.Digest}
     * from {@link SearchResponse}.
     */
    private static List<DigestSnapshot> deserializeSearchResponse(
        SearchResponse searchResponse) {
      List<DigestSnapshot> digestSnapshots =
          CollectionUtils.newArrayList();
      int i = 0;
      for (SearchResponse.Digest digest : searchResponse.getDigests()) {
        DigestSnapshot digestSnapshot = deserializeDigest(digest);
        digestSnapshots.add(i, digestSnapshot);
        i++;
      }
      return digestSnapshots;
    }

    private static DigestSnapshot deserializeDigest(Digest digest) {
      List<ParticipantId> participantIds = CollectionUtils.newArrayList();
      for (String participant : digest.getParticipants()) {
        participantIds.add(ParticipantId.ofUnsafe(participant));
      }
      DigestSnapshot digestSnapshot =
          new DigestSnapshot(digest.getTitle(), digest.getSnippet(), WaveId.deserialise(digest
              .getWaveId()), ParticipantId.ofUnsafe(digest.getAuthor()), participantIds,
              digest.getLastModified(), digest.getUnreadCount(), digest.getBlipCount());
      return digestSnapshot;
    }
  }

  private static String getUrl(SearchRequest searchRequest) {
      String query = URL.encodeQueryString(searchRequest.getQuery());
      String params =
          "?query=" + query +
          "&index=" + searchRequest.getIndex() +
          "&numResults=" + searchRequest.getNumResults();
      return SEARCH_URL_BASE + "/" + params;
  }

  @Override
  public String toString() {
    return "[Query: " + searchRequest.getQuery() + ", Index: " + searchRequest.getIndex()
        + ", NumResults: " + searchRequest.getNumResults() + "]";
  }
}
