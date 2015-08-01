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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.waveprotocol.box.server.util.OAuthUtil;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Container to handle request and access tokens for the Data Api.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
@Singleton
public final class DataApiTokenContainer {

  private static final Log LOG = Log.get(DataApiTokenContainer.class);

  /** Length of a token in number of characters */
  private static final int TOKEN_LENGTH = 48;

  /** Number of minutes a request token is valid */
  private static final int REQUEST_TOKEN_EXPIRATION = 10;

  /**
   * Number of minutes an access token is valid, public so this can be shown to
   * the user
   */
  public static final int ACCESS_TOKEN_EXPIRATION = 60;
  public static final String USER_PROPERTY_NAME = "user";

  /** Map containing unauthorized accessors indexed on their request token */
  private ConcurrentMap<String, OAuthAccessor> requestTokenAccessors;

  /** Map containing the authorized accessors indexed on the access token */
  private ConcurrentMap<String, OAuthAccessor> accessTokenAccessors;

  /** Used to generate OAuth tokens */
  private final TokenGenerator tokenGenerator;

  @Inject
  @VisibleForTesting
  DataApiTokenContainer(TokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;

    requestTokenAccessors =
        CacheBuilder.newBuilder().expireAfterWrite(REQUEST_TOKEN_EXPIRATION, TimeUnit.MINUTES)
        .<String, OAuthAccessor>build().asMap();
    accessTokenAccessors =
        CacheBuilder.newBuilder().expireAfterWrite(ACCESS_TOKEN_EXPIRATION, TimeUnit.MINUTES)
        .<String, OAuthAccessor>build().asMap();
  }

  /**
   * Gets the {@link OAuthAccessor} that is identified by the given request
   * token. Any changes made to the accessor's fields, except the consumer, will
   * not be reflected in this container.
   *
   * @param requestToken the request token used for identification.
   * @throws OAuthProblemException if the token does not map to an accessor.
   */
  public OAuthAccessor getRequestTokenAccessor(String requestToken) throws OAuthProblemException {
    OAuthAccessor accessor = requestTokenAccessors.get(requestToken);
    if (accessor == null) {
      OAuthProblemException exception =
          OAuthUtil.newOAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
      exception.setParameter(OAuth.OAUTH_TOKEN, requestToken);
      throw exception;
    }
    return accessor.clone();
  }

  /**
   * Gets the authorized {@link OAuthAccessor} that is identified by the given
   * access token. Any changes made to the accessor's fields, except the
   * consumer, will not be reflected in this container.
   *
   * @param accessToken the access token used for identification.
   * @throws OAuthProblemException if the token does not map to an accessor.
   */
  public OAuthAccessor getAccessTokenAccessor(String accessToken) throws OAuthProblemException {
    OAuthAccessor accessor = accessTokenAccessors.get(accessToken);
    if (accessor == null) {
      OAuthProblemException exception =
          OAuthUtil.newOAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
      exception.setParameter(OAuth.OAUTH_TOKEN, accessToken);
      throw exception;
    }
    return accessor.clone();
  }

  /**
   * Generates a new request token for the given {@link OAuthConsumer}.
   *
   * @param consumer the consumer to generate the token for.
   */
  public OAuthAccessor generateRequestToken(OAuthConsumer consumer) {
    Preconditions.checkNotNull(consumer, "Consumer must not be null");

    // Accessor can be generated up front with a token secret that does not need
    // to be unique.
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    accessor.tokenSecret = generateToken();

    do {
      accessor.requestToken = generateToken();
    } while (requestTokenAccessors.putIfAbsent(accessor.requestToken, accessor) != null);

    return accessor.clone();
  }

  /**
   * Authorizes a request token to be exchanged for an access token.
   *
   * @param requestToken the request token used for identification.
   * @param user the user that has authorized the token.
   * @throws OAuthProblemException if the request token does not map to an
   *         accessor or if the token was already used.
   */
  public OAuthAccessor authorizeRequestToken(String requestToken, ParticipantId user)
      throws OAuthProblemException {
    Preconditions.checkNotNull(user, "User must not be null");

    OAuthAccessor accessor = getRequestTokenAccessor(requestToken);

    if (accessor.getProperty(USER_PROPERTY_NAME) != null) {
      throw OAuthUtil.newOAuthProblemException(OAuth.Problems.TOKEN_USED);
    }

    accessor.setProperty(USER_PROPERTY_NAME, user);
    requestTokenAccessors.put(requestToken, accessor);

    LOG.info("Authorized request token for " + user);
    return accessor.clone();
  }

  /**
   * Rejects authorization of a request token.
   *
   * @param requestToken the request token used for identification.
   * @throws OAuthProblemException if the request token does not map to an
   *         accessor or if the token was already used.
   */
  public void rejectRequestToken(String requestToken) throws OAuthProblemException {
    OAuthAccessor accessor = getRequestTokenAccessor(requestToken);

    if (accessor.getProperty(USER_PROPERTY_NAME) != null) {
      throw OAuthUtil.newOAuthProblemException(OAuth.Problems.TOKEN_USED);
    }

    // Can't use remove(String, OAuthAccessor) since equals is not defined.
    requestTokenAccessors.remove(requestToken);
    LOG.info("Rejected request token " + requestToken);
  }

  /**
   * Authorize the {@link OAuthAccessor} by generating a new access token and
   * token secret.
   *
   * @param requestToken the requestToken used for identifying the accessor that
   *        needs to be authorized.
   * @return a new {@link OAuthAccessor} with the access token and token secret
   *         set.
   * @throws OAuthProblemException if the request token in the accessor is not
   *         known.
   */
  public OAuthAccessor generateAccessToken(String requestToken) throws OAuthProblemException {
    OAuthAccessor accessor = getRequestTokenAccessor(requestToken);

    if (accessor.getProperty(USER_PROPERTY_NAME) == null) {
      // User has not given the consumer permission yet.
      throw OAuthUtil.newOAuthProblemException(OAuth.Problems.PERMISSION_UNKNOWN);
    }

    // Token secret does not need to unique so can be generated now.
    accessor.tokenSecret = generateToken();

    do {
      accessor.accessToken = generateToken();
    } while (accessTokenAccessors.putIfAbsent(accessor.accessToken, accessor) != null);
    requestTokenAccessors.remove(accessor.requestToken);

    LOG.info("Generated access token for " + accessor.getProperty(USER_PROPERTY_NAME));
    return accessor.clone();
  }

  /**
   * Generates an OAuth token.
   */
  private String generateToken() {
    return tokenGenerator.generateToken(TOKEN_LENGTH);
  }
}
