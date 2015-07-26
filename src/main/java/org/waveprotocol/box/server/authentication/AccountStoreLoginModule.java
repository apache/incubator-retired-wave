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

import com.google.common.base.Preconditions;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * A Simple login module which does username & password authentication against
 * users in a database.
 *
 *  This code is based on the example here:
 * http://java.sun.com/developer/technicalArticles/Security/jaasv2/
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AccountStoreLoginModule implements LoginModule {
  private Subject subject;
  private ParticipantPrincipal principal;
  private CallbackHandler callbackHandler;
  private AccountStore accountStore;

  private final Log LOG = Log.get(AccountStoreLoginModule.class);
  
  enum Status {
    NOT, OK, COMMIT
  }

  private Status status;

  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler,
      Map<String, ?> sharedState, Map<String, ?> options) {
    Preconditions.checkNotNull(callbackHandler, "Callback handler null");

    accountStore = AccountStoreHolder.getAccountStore();

    status = Status.NOT;
    this.subject = subject;
    this.callbackHandler = callbackHandler;
  }

  @Override
  public boolean login() throws LoginException {
    NameCallback nameCallback = new NameCallback("Username");
    PasswordCallback passwordCallback = new PasswordCallback("Password", false);

    Callback callbacks[] = new Callback[] {nameCallback, passwordCallback};

    try {
      callbackHandler.handle(callbacks);
    } catch (java.io.IOException e) {
      throw new LoginException(e.toString());
    } catch (UnsupportedCallbackException e) {
      throw new LoginException("Error: " + e.getCallback().toString());
    }

    boolean success;
    ParticipantId id = null;
    
    String address = nameCallback.getName();
    if (!address.contains(ParticipantId.DOMAIN_PREFIX)) {
      address = address + ParticipantId.DOMAIN_PREFIX + AccountStoreHolder.getDefaultDomain();
    }
    
    try {
      id = ParticipantId.of(address);
      AccountData account = accountStore.getAccount(id);
      char[] password = passwordCallback.getPassword();
      
      if (account == null) {
        // The user doesn't exist. Auth failed.
        success = false;
      } else if (!account.isHuman()) {
        // The account is owned by a robot. Auth failed.
        success = false;
      } else if (password == null) {
        // Null password provided by callback. We require a password (even an empty one).
        success = false;
      } else if (!account.asHuman().getPasswordDigest().verify(password)) {
        // The supplied password doesn't match. Auth failed.
        success = false;
      } else {
        success = true;
      }
    } catch (InvalidParticipantAddress e) {
      // The supplied user address is invalid. Auth failed.
      success = false;
    } catch (PersistenceException e) {
      LOG.severe("Failed to retreive account data for " + id, e);
      throw new LoginException(
          "An unexpected error occured while trying to retrieve account information!");
    }

    // The password is zeroed before it gets GC'ed for memory security.
    passwordCallback.clearPassword();

    if (success) {
      principal = new ParticipantPrincipal(id);
      status = Status.OK;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean commit() {
    if (status == Status.NOT) {
      return false;
    }
    if (subject == null) {
      return false;
    }
    subject.getPrincipals().add(principal);
    status = Status.COMMIT;
    return true;
  }

  @Override
  public boolean abort() {
    if ((subject != null) && (principal != null)) {
      subject.getPrincipals().remove(principal);
    }
    principal = null;
    status = Status.NOT;
    return true;
  }

  @Override
  public boolean logout() {
    subject.getPrincipals().remove(principal);
    status = Status.NOT;
    return true;
  }
}
