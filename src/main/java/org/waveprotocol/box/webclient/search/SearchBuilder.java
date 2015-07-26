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

import org.waveprotocol.box.webclient.search.SearchService.Callback;

/**
 * Interface for a search builder.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public interface SearchBuilder {

  /**
   * Initializes the {@link SearchBuilder} for a new search.
   *
   * @return the {@link SearchBuilder} to allow chaining.
   */
  SearchBuilder newSearch();

  /**
   * @param query the query to execute.
   */
  SearchBuilder setQuery(String query);

  /**
   * @param index the index from which to return results.
   */
  SearchBuilder setIndex(int index);

  /**
   * @param numResults the maximum number of results to return.
   */
  SearchBuilder setNumResults(int numResults);

  /**
   * Performs a full text search on the waves.
   *
   * @param callback the callback through which the search query results are returned.
   * @return the http request
   */
  Request search(final Callback callback);

}
