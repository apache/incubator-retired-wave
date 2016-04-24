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
package org.waveprotocol.box.server.stat;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.waveprotocol.box.stat.RequestScope;
import org.waveprotocol.box.stat.SessionContext;
import org.waveprotocol.box.stat.Timing;


/**
 * Filter that executes the http servlet request in the request scope.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@Singleton
@SuppressWarnings("rawtypes")
public class RequestScopeFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (Timing.isEnabled()) {
      Timing.enterScope();
      final HttpSession session = ((HttpServletRequest)request).getSession();
      final ParticipantId loggedInUser = (ParticipantId)session.getAttribute(SessionManager.USER_FIELD);
      Timing.setScopeValue(SessionContext.class, new SessionContext() {
        @Override
        public boolean isAuthenticated() {
          return session != null;
        }

        @Override
        public String getSessionKey() {
          return session.getId();
        }

        @Override
        public ParticipantId getParticipantId() {
          return loggedInUser;
        }

        @Override
        public RequestScope.Value clone() {
          return this;
        }
      });
    }

    try {
      chain.doFilter(request, response);
    } finally {
      Timing.exitScope();
    }
  }

  @Override
  public void init(FilterConfig config) {
  }

  @Override
  public void destroy() {
  }
}