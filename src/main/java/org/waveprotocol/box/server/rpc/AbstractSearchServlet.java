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

package org.waveprotocol.box.server.rpc;

import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationQueue;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.SearchResult;
import com.google.wave.api.data.converter.EventDataConverterManager;

import org.waveprotocol.box.search.SearchProto.SearchRequest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A base for servlets that provide search functionality.
 *
 * @author yurize@apache.org (Yuri Z.)
 */
@SuppressWarnings("serial")
public abstract class AbstractSearchServlet extends HttpServlet {

  private static final Log LOG = Log.get(AbstractSearchServlet.class);

  private static String DEFAULT_QUERY = "";
  private static String DEFAULT_NUMRESULTS = "100";

  protected final ConversationUtil conversationUtil;
  protected final EventDataConverterManager converterManager;
  protected final WaveletProvider waveletProvider;
  protected final SessionManager sessionManager;
  protected final OperationServiceRegistry operationRegistry;


  private static String getParameter(HttpServletRequest req, String paramName, String defaultValue) {
    String param = req.getParameter(paramName);
    param = param == null ? defaultValue : param;
    return param;
  }

  /**
   * Constructor.
   */
  public AbstractSearchServlet(ConversationUtil conversationUtil,
      EventDataConverterManager converterManager, WaveletProvider waveletProvider,
      SessionManager sessionManager, OperationServiceRegistry operationRegistry) {
    this.conversationUtil = conversationUtil;
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.sessionManager = sessionManager;
    this.operationRegistry = operationRegistry;
  }

  /**
   * Extracts search query params from request.
   *
   * @param req the request.
   * @param response the response.
   * @return the SearchRequest with query data.
   */
  public static SearchRequest parseSearchRequest(HttpServletRequest req,
      HttpServletResponse response) {

    String query = getParameter(req, "query", DEFAULT_QUERY);
    String index = getParameter(req, "index", "0");
    String numResults = getParameter(req, "numResults", DEFAULT_NUMRESULTS);
    SearchRequest searchRequest =
        SearchRequest.newBuilder().setQuery(query).setIndex(Integer.parseInt(index))
            .setNumResults(Integer.parseInt(numResults)).build();
    return searchRequest;
  }

  /**
   * Performs search using Data API.
   */
  protected SearchResult performSearch(SearchRequest searchRequest, ParticipantId user) {
    OperationQueue opQueue = new OperationQueue();
    opQueue.search(searchRequest.getQuery(), searchRequest.getIndex(),
        searchRequest.getNumResults());
    OperationContextImpl context =
        new OperationContextImpl(waveletProvider,
            converterManager.getEventDataConverter(ProtocolVersion.DEFAULT), conversationUtil);
    LOG.fine(
        "Performing query: " + searchRequest.getQuery() + " [" + searchRequest.getIndex() + ", "
            + (searchRequest.getIndex() + searchRequest.getNumResults()) + "]");
    OperationRequest operationRequest = opQueue.getPendingOperations().get(0);
    String opId = operationRequest.getId();
    OperationUtil.executeOperation(operationRequest, operationRegistry, context, user);
    JsonRpcResponse jsonRpcResponse = context.getResponses().get(opId);
    SearchResult searchResult =
        (SearchResult) jsonRpcResponse.getData().get(ParamsProperty.SEARCH_RESULTS);
    return searchResult;
  }
}
