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

import com.google.wave.api.SearchResult;

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * A provider of search results. SearchProviders can be queried, and reply with a set of
 * ReadableWaveletData objects which match the query.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public interface SearchProvider {
  /**
   * Run a search query.
   * 
   * @param user the user executing the query
   * @param query the query string
   * @param startAt The offset in the results to return
   * @param numResults The number of results from startAt to return
   * @return the search result with digests which match the specified query
   */
  SearchResult search(
      ParticipantId user, String query, int startAt, int numResults);
}
