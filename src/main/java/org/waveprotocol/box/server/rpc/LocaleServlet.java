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

import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.SessionManager;

import com.google.inject.Inject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import javax.inject.Singleton;

/**
 *
 * Servlet allows user to store locale.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
@SuppressWarnings("serial")
@Singleton
public final class LocaleServlet extends HttpServlet {

  private final SessionManager sessionManager;
  private final AccountStore accountStore;
  private final Log LOG = Log.get(LocaleServlet.class);

  @Inject
  public LocaleServlet(SessionManager sessionManager, AccountStore accountStore) {
    this.sessionManager = sessionManager;
    this.accountStore = accountStore;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      ParticipantId participant = sessionManager.getLoggedInUser(req.getSession(false));
      if (participant == null) {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      String locale = URLDecoder.decode(req.getParameter("locale"), "UTF-8");
      AccountData account = accountStore.getAccount(participant);
      HumanAccountData humanAccount;
      if (account != null) {
        humanAccount = account.asHuman();
      } else {
        humanAccount = new HumanAccountDataImpl(participant);
      }
      humanAccount.setLocale(locale);
      accountStore.putAccount(humanAccount);
    } catch (PersistenceException ex) {
      throw new IOException(ex);
    }
  }
}
