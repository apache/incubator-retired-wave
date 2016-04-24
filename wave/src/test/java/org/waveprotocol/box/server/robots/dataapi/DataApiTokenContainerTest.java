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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Objects;

import junit.framework.TestCase;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;

import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * Unit tests for {@link DataApiTokenContainer}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DataApiTokenContainerTest extends TestCase {

  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final String FAKE_TOKEN = "fake_token";

  private DataApiTokenContainer container;
  private OAuthConsumer consumer;

  @Override
  protected void setUp() throws Exception {
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    when(tokenGenerator.generateToken(anyInt())).thenReturn(FAKE_TOKEN);
    container = new DataApiTokenContainer(tokenGenerator);
    OAuthServiceProvider serviceProvider = new OAuthServiceProvider("", "", "");
    consumer = new OAuthConsumer("", "consumerkey", "consumersecret", serviceProvider);
  }

  public void testGetUnknownRequestTokenAccessorThrowsException() throws Exception {
    try {
      container.getRequestTokenAccessor("unknown");
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testGetUnknownAcessTokenAccessorThrowsException() throws Exception {
    try {
      container.getAccessTokenAccessor("unknown");
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testGenerateRequestToken() throws Exception {
    OAuthAccessor accessor = container.generateRequestToken(consumer);

    assertEquals("Consumer should be retained", consumer, accessor.consumer);
    assertFalse("Request token should be generated", accessor.requestToken.isEmpty());
    assertTrue("Accessor should be in storage",
        areEqual(accessor, container.getRequestTokenAccessor(accessor.requestToken)));
  }

  public void testAuthorizeRequestToken() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);

    OAuthAccessor authorizedRequestToken =
        container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);

    assertEquals("Consumer should be retained", consumer, authorizedRequestToken.consumer);
    assertEquals("User should be equal", ALEX,
        authorizedRequestToken.getProperty(DataApiTokenContainer.USER_PROPERTY_NAME));
    assertTrue("Accessor should be in storage", areEqual(authorizedRequestToken,
        container.getRequestTokenAccessor(authorizedRequestToken.requestToken)));
  }

  public void testAuthorizeAlreadyAuthorizedRequestTokenThrowsException() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);

    container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);
    try {
      container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testRejectRequestToken() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);

    container.rejectRequestToken(unauthorizedRequestToken.requestToken);
    try {
      container.getRequestTokenAccessor(unauthorizedRequestToken.requestToken);
      fail("Retrieving the request token should fail because it was rejected");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testRejectRequestTokenAfterAuthorizationThrowsException() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);

    container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);
    try {
      container.rejectRequestToken(unauthorizedRequestToken.requestToken);
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testRejectUnknownRequestTokenThrowsException() throws Exception {
    try {
      container.rejectRequestToken("unknown");
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testGenerateAccessToken() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);
    OAuthAccessor authorizedRequestToken =
        container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);
    OAuthAccessor accessToken = container.generateAccessToken(authorizedRequestToken.requestToken);

    assertEquals("Consumer should be retained", consumer, accessToken.consumer);
    assertFalse("Access token should be generated", accessToken.accessToken.isEmpty());
    assertFalse("Token secret should be generated", accessToken.tokenSecret.isEmpty());
    assertTrue("Accessor should be in storage",
        areEqual(accessToken, container.getAccessTokenAccessor(accessToken.accessToken)));
  }

  public void testGenerateAccessTokenForUnauthorizedTokenThrowsException() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);

    try {
      container.generateAccessToken(unauthorizedRequestToken.requestToken);
      fail("Expected OAuthProblemException");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testGenerateAccessTokenForUnknownTokenThrowsException() throws Exception {
    try {
      container.generateAccessToken("unknown");
      fail("Expected OAuthProblemException when authorizing an unknown token");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  public void testGenerateAccessTokenForAlreadyAuthorizedTokenThrowsException() throws Exception {
    OAuthAccessor unauthorizedRequestToken = container.generateRequestToken(consumer);
    OAuthAccessor authorizedRequestToken =
        container.authorizeRequestToken(unauthorizedRequestToken.requestToken, ALEX);

    container.generateAccessToken(authorizedRequestToken.requestToken);
    try {
      container.generateAccessToken(authorizedRequestToken.requestToken);
      fail("Expected OAuthProblemException when authorizing an already used token");
    } catch (OAuthProblemException e) {
      // expected
    }
  }

  /** Custom equals method because {@link OAuthAccessor} doesn't define one */
  private boolean areEqual(OAuthAccessor expected, OAuthAccessor actual) {
    return Objects.equal(expected.consumer, actual.consumer)
        && Objects.equal(expected.accessToken, actual.accessToken)
        && Objects.equal(expected.requestToken, actual.requestToken)
        && Objects.equal(expected.tokenSecret, actual.tokenSecret)
        && Objects.equal(expected.getProperty(DataApiTokenContainer.USER_PROPERTY_NAME),
            actual.getProperty(DataApiTokenContainer.USER_PROPERTY_NAME));
  }
}
