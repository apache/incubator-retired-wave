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

package org.waveprotocol.box.server.robots.dataapi;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.server.HttpRequestMessage;

import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} that serves as the endpoint for the Data Api.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
@SuppressWarnings("serial")
@Singleton
public class DataApiServlet extends BaseApiServlet {

  private static final Log LOG = Log.get(DataApiServlet.class);
  private final DataApiTokenContainer tokenContainer;
  
  @Inject
  public DataApiServlet(RobotSerializer robotSerializer,
      EventDataConverterManager converterManager, WaveletProvider waveletProvider,
      @Named("DataApiRegistry") OperationServiceRegistry operationRegistry,
      ConversationUtil conversationUtil, OAuthValidator validator,
      DataApiTokenContainer tokenContainer) {
    super(robotSerializer, converterManager, waveletProvider, operationRegistry, conversationUtil,
        validator);
    this.tokenContainer = tokenContainer;
  }
  
  /**
   * Entry point for the Data API Calls.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());

    OAuthAccessor accessor;
    try {
      message.requireParameters(OAuth.OAUTH_TOKEN);
      accessor = tokenContainer.getAccessTokenAccessor(message.getParameter(OAuth.OAUTH_TOKEN));
    } catch (OAuthProblemException e) {
      LOG.info("No valid OAuth token present", e);
      // Have to set status here manually, cannot use e.getHttpStatusCode
      // because message.requireParameters doesn't set it in the exception.
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    ParticipantId participant =
        (ParticipantId) accessor.getProperty(DataApiTokenContainer.USER_PROPERTY_NAME);
    
    processOpsRequest(req, resp, message, accessor, participant);
  }
}
