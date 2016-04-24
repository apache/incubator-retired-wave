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

import com.google.common.collect.Maps;
import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.box.common.SessionConstants;
import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.gxp.TopBar;
import org.waveprotocol.box.server.gxp.WaveClientPage;
import org.waveprotocol.box.server.util.RandomBase64Generator;
import org.waveprotocol.box.server.util.UrlParameters;
import org.waveprotocol.wave.client.util.ClientFlagsBase;
import org.waveprotocol.wave.common.bootstrap.FlagConstants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * The HTTP servlet for serving a wave client along with content generated on
 * the server.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
@SuppressWarnings("serial")
@Singleton
public class WaveClientServlet extends HttpServlet {

  private static final Log LOG = Log.get(WaveClientServlet.class);

  private static final HashMap<String, String> FLAG_MAP = Maps.newHashMap();
  static {
    // __NAME_MAPPING__ is a map of name to obfuscated id
    for (int i = 0; i < FlagConstants.__NAME_MAPPING__.length; i += 2) {
      FLAG_MAP.put(FlagConstants.__NAME_MAPPING__[i], FlagConstants.__NAME_MAPPING__[i + 1]);
    }
  }

  private final String domain;
  private final String analyticsAccount;
  private final SessionManager sessionManager;
  private final String websocketPresentedAddress;

  /**
   * Creates a servlet for the wave client.
   */
  @Inject
  public WaveClientServlet(
      @Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) String domain,
      Config config,
      SessionManager sessionManager) {
    List<String> httpAddresses = config.getStringList("core.http_frontend_addresses");
    String websocketAddress = config.getString("core.http_websocket_public_address");
    String websocketPresentedAddress = config.getString("core.http_websocket_presented_address");
    this.domain = domain;
    String websocketAddress1 = StringUtils.isEmpty(websocketAddress) ? httpAddresses.get(0) : websocketAddress;
    this.websocketPresentedAddress = StringUtils.isEmpty(websocketPresentedAddress) ?
                                             websocketAddress1 : websocketPresentedAddress;
    this.analyticsAccount = config.getString("administration.analytics_account");
    this.sessionManager = sessionManager;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ParticipantId id = sessionManager.getLoggedInUser(request.getSession(false));

    // Eventually, it would be nice to show users who aren't logged in the public waves.
    // However, public waves aren't implemented yet. For now, we'll just redirect users
    // who haven't signed in to the sign in page.
    if (id == null) {
      response.sendRedirect(sessionManager.getLoginUrl("/"));
      return;
    }

    AccountData account = sessionManager.getLoggedInAccount(request.getSession(false));
    if (account != null) {
      String locale = account.asHuman().getLocale();
      if (locale != null) {
        String requestLocale = UrlParameters.getParameters(request.getQueryString()).get("locale");
        if (requestLocale == null) {
          response.sendRedirect(UrlParameters.addParameter(request.getRequestURL().toString(), "locale", locale));
          return;
        }
      }
    }

    String[] parts = id.getAddress().split("@");
    String username = parts[0];
    String userDomain = id.getDomain();

    try {
      WaveClientPage.write(response.getWriter(), new GxpContext(request.getLocale()),
          getSessionJson(request.getSession(false)), getClientFlags(request), websocketPresentedAddress,
          TopBar.getGxpClosure(username, userDomain), analyticsAccount);
    } catch (IOException e) {
      LOG.warning("Failed to write GXP for request " + request, e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  private JSONObject getClientFlags(HttpServletRequest request) {
    try {
      JSONObject ret = new JSONObject();

      Enumeration<?> iter = request.getParameterNames();
      while (iter.hasMoreElements()) {
        String name = (String) iter.nextElement();
        String value = request.getParameter(name);

        if (FLAG_MAP.containsKey(name)) {
          // Set using the correct type of data in the json using reflection
          try {
            Method getter = ClientFlagsBase.class.getMethod(name);
            Class<?> retType = getter.getReturnType();

            if (retType.equals(String.class)) {
              ret.put(FLAG_MAP.get(name), value);
            } else if (retType.equals(Integer.class)) {
              ret.put(FLAG_MAP.get(name), Integer.parseInt(value));
            } else if (retType.equals(Boolean.class)) {
              ret.put(FLAG_MAP.get(name), Boolean.parseBoolean(value));
            } else if (retType.equals(Float.class)) {
              ret.put(FLAG_MAP.get(name), Float.parseFloat(value));
            } else if (retType.equals(Double.class)) {
              ret.put(FLAG_MAP.get(name), Double.parseDouble(value));
            } else {
              // Flag exists, but its type is unknown, so it can not be
              // properly encoded in JSON.
              LOG.warning("Ignoring flag [" + name + "] with unknown return type: " + retType);
            }

            // Ignore the flag on any exception
          } catch (SecurityException | NumberFormatException ignored) {
          } catch (NoSuchMethodException ex) {
            LOG.warning("Failed to find the flag [" + name + "] in ClientFlagsBase.");
          }
        }
      }

      return ret;
    } catch (JSONException ex) {
      LOG.severe("Failed to create flags JSON");
      return new JSONObject();
    }
  }

  private JSONObject getSessionJson(HttpSession session) {
    try {
      ParticipantId user = sessionManager.getLoggedInUser(session);
      String address = (user != null) ? user.getAddress() : null;

      // TODO(zdwang): Figure out a proper session id rather than generating a
      // random number
      String sessionId = (new RandomBase64Generator()).next(10);

      return new JSONObject()
          .put(SessionConstants.DOMAIN, domain)
          .putOpt(SessionConstants.ADDRESS, address)
          .putOpt(SessionConstants.ID_SEED, sessionId);
    } catch (JSONException e) {
      LOG.severe("Failed to create session JSON");
      return new JSONObject();
    }
  }
}
