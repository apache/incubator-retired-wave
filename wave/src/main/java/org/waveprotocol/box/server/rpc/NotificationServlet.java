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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;
import com.google.wave.api.data.converter.EventDataConverterManager;

import org.waveprotocol.box.search.SearchProto.SearchRequest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides search result digests in JSON format.
 *
 * @author yurize@apache.org (Yuri Z.)
 */
@SuppressWarnings("serial")
@Singleton
public final class NotificationServlet extends AbstractSearchServlet {

  private static final Gson GSON = new Gson();

  @Inject
  public NotificationServlet(SessionManager sessionManager,
      EventDataConverterManager converterManager,
      @Named("DataApiRegistry") OperationServiceRegistry operationRegistry,
      WaveletProvider waveletProvider, ConversationUtil conversationUtil, ProtoSerializer serializer) {
    super(conversationUtil, converterManager, waveletProvider, sessionManager, operationRegistry);
  }

  /**
   * Creates HTTP response to the search query. Main entrypoint for this class.
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
    if (user == null) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    SearchRequest searchRequest = parseSearchRequest(req, response);
    SearchResult searchResult = performSearch(searchRequest, user);
    serializeObjectToServlet(searchResult.getDigests(), response);
  }

  /**
   * Writes the json with search results to Response.
   */
  private void serializeObjectToServlet(List<Digest> digests, HttpServletResponse resp)
      throws IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/html; charset=utf8");
    // This is to make sure the fetched data is fresh - since the w3c spec
    // is rarely respected.
    resp.setHeader("Cache-Control", "no-store");
    resp.getWriter().append(GSON.toJson(digests));
  }
}
