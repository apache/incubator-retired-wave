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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.box.server.authentication.SessionManager;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A servlet for signing out the user.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
@SuppressWarnings("serial")
@Singleton
public class SignOutServlet extends HttpServlet {
  private final SessionManager sessionManager;

  @Inject
  public SignOutServlet(SessionManager sessionManager) {
    Preconditions.checkNotNull(sessionManager, "Session manager is null");
    this.sessionManager = sessionManager;
  }

  /**
   * On GET, sign the user out and redirect them to the redirect URL.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    HttpSession session = req.getSession(false);
    sessionManager.logout(session);

    String redirectUrl = req.getParameter("r");
    if (redirectUrl != null && redirectUrl.startsWith("/")) {
      resp.sendRedirect(redirectUrl);
    } else {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/html");
      resp.getWriter().print("<html><body>Logged out.</body></html>");
    }
  }
}
