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

package com.google.wave.api.oauth.impl;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Objects of this class are stored in Datastore to persist an association
 * between user's wave id + wave id, the user's access token,
 * and the service consumer secret.
 * Holds the request token for later retrieval to exchange it for the access token.
 * 'Detachable' attribute allows object to be altered even after 
 * its Persistence Manager is closed.
 * 
 * @author kimwhite@google.com (Kimberly White)
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "true")
public class OAuthUser {
  
  /** 
   * Datastore key. Consists of the user's wave id and the wave id the robot
   * resides on.
   */
  @SuppressWarnings("unused")
  @PrimaryKey
  @Persistent
  private String nameKey;

  /** OAuth access token. */
  @Persistent
  private String accessToken = null;

  /** OAuth request token. */
  @Persistent
  private String requestToken;
  
  /** 
   * OAuth authorize url. 
   * By default passed to gadget to open pop-up window.
   */
  @Persistent
  private String authUrl = null;
  
  /** The token secret used to sign requests. */
  @Persistent
  private String tokenSecret = null;
  
  /**
   * Creates a new profile with the user id and OAuth request token.
   * 
   * @param userId wave creator's id and wave id.
   * @param requestToken the OAuth request token.
   */
  public OAuthUser(String userId, String requestToken) {
    this.nameKey = userId;
    this.requestToken = requestToken;
  }

  /**
   * Adds the user's access token to the OAuth profile.
   * 
   * @param accessToken the OAuth access token.
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
  
  /**
   * Returns the access token.
   * 
   * @return the access token.
   */
  public String getAccessToken() {
    return accessToken;
  }
  
  /**
   * Returns the request token.
   * 
   * @return the request token.
   */
  public String getRequestToken() {
    return requestToken;
  }
  
  /**
   * Sets the authorize url.
   * 
   * @param authUrl the url to the service provider authorize page.
   */
  public void setAuthUrl(String authUrl) {
    this.authUrl = authUrl;
  }

  /**
   * Returns the authorize url.
   * 
   * @return the url to the service provider authorize page.
   */
  public String getAuthUrl() {
    return authUrl;
  }

  /**
   * Sets the oauth_token_secret parameter.
   * 
   * @param tokenSecret Token used to sign requests for protected resources.
   */
  public void setTokenSecret(String tokenSecret) {
    this.tokenSecret = tokenSecret;
  }

  /**
   * Returns the oauth_token_secret parameter.
   * 
   * @return the oauth_token_secret parameter
   */
  public String getTokenSecret() {
    return tokenSecret;
  }
}
