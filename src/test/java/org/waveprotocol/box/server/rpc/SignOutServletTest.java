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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.authentication.SessionManager;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SignOutServletTest extends TestCase {
  private SignOutServlet servlet;

  @Mock private SessionManager sessionManager;
  @Mock private HttpServletRequest req;
  @Mock private HttpServletResponse resp;
  @Mock private HttpSession session;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    servlet = new SignOutServlet(sessionManager);

    when(req.getSession(false)).thenReturn(session);
    PrintWriter writer = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(writer);
  }

  public void testUserSignedOut() throws Exception {
    servlet.doGet(req, resp);
    
    verify(sessionManager).logout(session);
    verify(resp).setStatus(HttpServletResponse.SC_OK);
  }

  public void testServletRedirects() throws Exception {
    String redirect_location = "/abc123?nested=query&string";
    when(req.getParameter("r")).thenReturn(redirect_location);

    servlet.doGet(req, resp);

    verify(resp).sendRedirect(redirect_location);
  }

  public void testServletDoesNotRedirectToRemoteUrl() throws Exception {
    String redirect_location = "http://example.com/";
    when(req.getParameter("r")).thenReturn(redirect_location);

    servlet.doGet(req, resp);

    verify(resp, never()).sendRedirect(anyString());
  }

  public void testServletWorksWhenSessionIsMissing() throws Exception {
    when(req.getSession(false)).thenReturn(null);
    
    servlet.doGet(req, resp);
    
    verify(resp).setStatus(HttpServletResponse.SC_OK);
  }
}
