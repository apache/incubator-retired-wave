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

import com.google.gwt.http.client.Request;

/**
 * Implementation of {@link SearchService}.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class RemoteSearchService implements SearchService {
  private static final SearchBuilder DEFAULT_SEARCHER = JsoSearchBuilderImpl.create();
  private final SearchBuilder searcher;

  RemoteSearchService(SearchBuilder searcher) {
    this.searcher = searcher;
  }

  public static RemoteSearchService create() {
    return new RemoteSearchService(DEFAULT_SEARCHER);
  }

  @Override
  public Request search(String query, int index, int numResults, Callback callback) {
    return searcher.newSearch().setQuery(query).setIndex(index).setNumResults(numResults)
        .search(callback);
  }
}
