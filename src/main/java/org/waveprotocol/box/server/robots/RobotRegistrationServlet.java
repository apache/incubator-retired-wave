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

package org.waveprotocol.box.server.robots;

import com.google.common.base.Strings;
import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.typesafe.config.Config;
import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.gxp.RobotRegistrationPage;
import org.waveprotocol.box.server.gxp.RobotRegistrationSuccessPage;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for Robot Registration.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
@SuppressWarnings("serial")
@Singleton
public class RobotRegistrationServlet extends HttpServlet {

  private static final String CREATE_PATH = "/create";

  private static final Log LOG = Log.get(RobotRegistrationServlet.class);

  private final RobotRegistrar robotRegistrar;
  private final String domain;
  private final String analyticsAccount;

  @Inject
  private RobotRegistrationServlet(@Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) String domain,
      RobotRegistrar robotRegistrar,
      Config config) {
    this.robotRegistrar = robotRegistrar;
    this.domain = domain;
    this.analyticsAccount = config.getString("administration.analytics_account");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (CREATE_PATH.equals(pathInfo)) {
      doRegisterGet(req, resp, "");
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (CREATE_PATH.equals(pathInfo)) {
      doRegisterPost(req, resp);
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Handles GET request for the register page.
   *
   * @param message non-null but optional message to show on the page
   */
  private void doRegisterGet(HttpServletRequest req, HttpServletResponse resp, String message)
      throws IOException {
    RobotRegistrationPage.write(resp.getWriter(), new GxpContext(req.getLocale()), domain, message,
        analyticsAccount);
    resp.setContentType("text/html");
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Handles POST request for the register page.
   */
  private void doRegisterPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = req.getParameter("username");
    String location = req.getParameter("location");

    if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(location)) {
      doRegisterGet(req, resp, "Please complete all fields.");
      return;
    }

    ParticipantId id;
    try {
      id = ParticipantId.of(username + "@" + domain);
    } catch (InvalidParticipantAddress e) {
      doRegisterGet(req, resp, "Invalid username specified, use alphanumeric characters only.");
      return;
    }

    RobotAccountData robotAccount;
    try{
      robotAccount = robotRegistrar.registerNew(id, location);
    } catch (RobotRegistrationException e) {
      doRegisterGet(req, resp, e.getMessage());
      return;
    } catch (PersistenceException e) {
      LOG.severe("Failed to retrieve account data for " + id, e);
      doRegisterGet(req, resp, "Failed to retrieve account data for " + id.getAddress());
      return;
    }
    onRegisterSuccess(req, resp, robotAccount);
  }

  /**
   * Shows the page that signals that a robot was successfully registered a
   * robot. It will show the robot's token and token secret to use for the
   * Active API.
   *
   * @param robotAccount the newly registered robot account.
   */
  private void onRegisterSuccess(HttpServletRequest req, HttpServletResponse resp,
      RobotAccountData robotAccount) throws IOException {
    RobotRegistrationSuccessPage.write(resp.getWriter(), new GxpContext(req.getLocale()),
        robotAccount.getId().getAddress(), robotAccount.getConsumerSecret(), analyticsAccount);
    resp.setContentType("text/html");
    resp.setStatus(HttpServletResponse.SC_OK);
  }
}