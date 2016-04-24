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

package org.waveprotocol.box.server.authentication;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.servlet.http.HttpSession;


/**
 * Utility class for managing the session's authentication status.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public interface SessionManager {
  static final String USER_FIELD = "user";

  static final String SIGN_IN_URL = "/auth/signin";


  /**
   * Get the participant id of the currently logged in user from the user's HTTP
   * session.
   *
   *  If the session is null, or if the user is not logged in, this function
   * returns null.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(false);
   * @return the user's participant id, or null if the user is not logged in.
   */
  ParticipantId getLoggedInUser(HttpSession session);

  /**
   * Get account data of the currently logged in user.
   *
   *  If the session is null, or if the user is not logged in, this function
   * returns null.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(false);
   * @return the user's account data, or null if the user is not logged in.
   */
  AccountData getLoggedInAccount(HttpSession session);

  /**
   * Bind the user's participant id to the user's session.
   *
   * This records that a user has been logged in.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(true);
   * @param id the user who has been logged in
   */
  void setLoggedInUser(HttpSession session, ParticipantId id);

  /**
   * Log the user out.
   *
   * If session is null, this function has no effect.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(false);
   */
  void logout(HttpSession session);

  /**
   * Get the relative URL to redirect the user to the login page.
   *
   * @param redirect a url path to redirect the user back to once they have
   *        logged in, or null if the user should not be redirected after
   *        authenticating.
   * @return a url containing the login page.
   */
  String getLoginUrl(String redirect);

  /**
   * Get a user's HttpSession from their session token.
   *
   * @param token the session token. Eg, "JSESSION=abcdef123567890"
   * @return the user's HttpSession, or null if the token is invalid.
   */
  HttpSession getSessionFromToken(String token);
}
