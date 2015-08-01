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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.SearchResult;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.waveserver.SearchProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;
import java.util.Map;

/**
 * {@link OperationService} for the "search" operation.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SearchService implements OperationService {

  /**
   * The number of search results to return if not defined in the request.
   * Defined in the spec.
   */
  private static final int DEFAULT_NUMBER_SEARCH_RESULTS = 10;

  private final SearchProvider searchProvider;

  @Inject
  public SearchService(SearchProvider searchProvider) {
    this.searchProvider = searchProvider;
  }

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    String query = OperationUtil.getRequiredParameter(operation, ParamsProperty.QUERY);
    int index = OperationUtil.getOptionalParameter(operation, ParamsProperty.INDEX, 0);
    int numResults = OperationUtil.getOptionalParameter(
        operation, ParamsProperty.NUM_RESULTS, DEFAULT_NUMBER_SEARCH_RESULTS);

    SearchResult result = search(participant, query, index, numResults);

    Map<ParamsProperty, Object> data =
        ImmutableMap.<ParamsProperty, Object> of(ParamsProperty.SEARCH_RESULTS, result);
    context.constructResponse(operation, data);
  }

  // Note that this search implementation is only of prototype quality.
  private SearchResult search(
      ParticipantId participant, String query, int startAt, int numResults) {
    return searchProvider.search(participant, query, startAt, numResults);
  }
}
