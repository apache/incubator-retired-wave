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

package org.waveprotocol.box.server.robots.active;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;

import junit.framework.TestCase;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;

import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Unit tests for the {@link ActiveApiServlet}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class ActiveApiServletTest extends TestCase {

  private static final ParticipantId ROBOT = ParticipantId.ofUnsafe("robot@example.com");
  private static final ParticipantId UNKNOWN = ParticipantId.ofUnsafe("unknown@example.com");

  private RobotSerializer robotSerializer;
  private OperationServiceRegistry operationRegistry;
  private OAuthValidator validator;
  private HttpServletResponse resp;
  private HttpServletRequest req;
  private StringWriter outputWriter;
  private ActiveApiServlet servlet;

  @Override
  protected void setUp() throws Exception {
    robotSerializer = mock(RobotSerializer.class);
    operationRegistry = mock(OperationServiceRegistry.class);
    validator = mock(OAuthValidator.class);

    EventDataConverterManager converterManager = mock(EventDataConverterManager.class);
    WaveletProvider waveletProvider = mock(WaveletProvider.class);
    ConversationUtil conversationUtil = mock(ConversationUtil.class);
    OAuthServiceProvider oAuthServiceProvider = mock(OAuthServiceProvider.class);
    AccountStore accountStore = mock(AccountStore.class);

    when(accountStore.getAccount(ROBOT)).thenReturn(
        new RobotAccountDataImpl(ROBOT, "", "secret", null, true));

    req = mock(HttpServletRequest.class);
    when(req.getRequestURL()).thenReturn(new StringBuffer("www.example.com/robot"));
    when(req.getHeaderNames()).thenReturn(
        convertRawEnumerationToGeneric(new StringTokenizer("Authorization")));
    when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

    resp = mock(HttpServletResponse.class);
    outputWriter = new StringWriter();
    when(resp.getWriter()).thenReturn(new PrintWriter(outputWriter));

    servlet =
        new ActiveApiServlet(robotSerializer, converterManager, waveletProvider, operationRegistry,
            conversationUtil, oAuthServiceProvider, validator, accountStore);
  }

  public void testDoPostExecutesAndWritesResponse() throws Exception {
    when(req.getHeaders("Authorization")).thenReturn(convertRawEnumerationToGeneric(
        generateOAuthHeader(ROBOT.getAddress())));

    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);
    List<OperationRequest> operations = Collections.singletonList(operation);
    when(robotSerializer.deserializeOperations(anyString())).thenReturn(operations);
    String responseValue = "response value";
    when(robotSerializer.serialize(any(), any(Type.class), any(ProtocolVersion.class))).thenReturn(
        responseValue);

    OperationService service = mock(OperationService.class);
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    servlet.doPost(req, resp);

    verify(operationRegistry).getServiceFor(any(OperationType.class));
    verify(service).execute(eq(operation), any(OperationContext.class), eq(ROBOT));
    verify(resp).setStatus(HttpServletResponse.SC_OK);
    assertEquals("Response should have been written into the servlet", responseValue,
        outputWriter.toString());
  }

  public void testDoPostUnauthorizedWhenParticipantInvalid() throws Exception {
    when(req.getHeaders("Authorization")).thenReturn(
        convertRawEnumerationToGeneric(generateOAuthHeader("invalid#$example.com")));

    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }


  public void testDoPostUnauthorizedWhenParticipantUnknown() throws Exception {
    when(req.getHeaders("Authorization")).thenReturn(
        convertRawEnumerationToGeneric(generateOAuthHeader(UNKNOWN.getAddress())));

    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  public void testDoPostUnauthorizedWhenValidationFails() throws Exception {
    when(req.getHeaders("Authorization")).thenReturn(
        convertRawEnumerationToGeneric(generateOAuthHeader(ROBOT.getAddress())));
    doThrow(new OAuthException("")).when(validator).validateMessage(
        any(OAuthMessage.class), any(OAuthAccessor.class));

    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private StringTokenizer generateOAuthHeader(String address) {
    return new StringTokenizer("OAuth " + OAuth.OAUTH_CONSUMER_KEY + "=\"" + address + "\"", "");
  }
  
  /**
   * Converts a {@link StringTokenizer} into an @{link {@link Enumeration
   * <String>}.
   * 
   * @author jeden17@gmail.com (Antonio Bello)
   */
  private Enumeration<String> convertRawEnumerationToGeneric(final StringTokenizer tokens) {
    return new Enumeration<String>() {
      @Override
      public String nextElement() {
        return (String) tokens.nextElement();
      }

      @Override
      public boolean hasMoreElements() {
        return tokens.hasMoreElements();
      }
    };
  }
}
